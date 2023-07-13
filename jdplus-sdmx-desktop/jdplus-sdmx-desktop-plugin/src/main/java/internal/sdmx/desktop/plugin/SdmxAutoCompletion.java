/*
 * Copyright 2017 National Bank of Belgium
 *
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved
 * by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */
package internal.sdmx.desktop.plugin;

import ec.util.completion.AutoCompletionSource;
import ec.util.completion.ExtAutoCompletionSource;
import ec.util.completion.swing.CustomListCellRenderer;
import jdplus.sdmx.base.api.web.SdmxWebProvider;
import jdplus.toolkit.desktop.plugin.TsManager;
import lombok.NonNull;
import nbbrd.desktop.favicon.DomainName;
import nbbrd.desktop.favicon.FaviconRef;
import nbbrd.desktop.favicon.FaviconSupport;
import nbbrd.desktop.favicon.URLConnectionFactory;
import nbbrd.io.text.Parser;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import sdmxdl.*;
import sdmxdl.web.SdmxWebManager;
import sdmxdl.web.SdmxWebSource;
import sdmxdl.web.spi.Network;
import sdmxdl.web.spi.Networking;
import sdmxdl.web.spi.SSLFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.swing.*;
import java.io.IOException;
import java.net.Proxy;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static ec.util.completion.AutoCompletionSource.Behavior.*;
import static java.util.stream.Collectors.toList;

/**
 * @author Philippe Charles
 */
public abstract class SdmxAutoCompletion {

    public abstract AutoCompletionSource getSource();

    public abstract ListCellRenderer getRenderer();

    public static SdmxAutoCompletion onWebSource(SdmxWebManager manager, Languages languages) {
        return new WebSourceCompletion(manager, languages);
    }

    public static <S extends SdmxSource> SdmxAutoCompletion onDataflow(SdmxManager<S> manager, Languages languages, Supplier<S> source, ConcurrentMap cache) {
        return new DataflowCompletion<>(manager, languages, source, cache);
    }

    public static <S extends SdmxSource> SdmxAutoCompletion onDimension(SdmxManager<S> manager, Languages languages, Supplier<S> source, Supplier<DataflowRef> flowRef, ConcurrentMap cache) {
        return new DimensionCompletion<>(manager, languages, source, flowRef, cache);
    }

    public static <S extends SdmxSource> SdmxAutoCompletion onAttribute(SdmxManager<S> manager, Languages languages, Supplier<S> source, Supplier<DataflowRef> flowRef, ConcurrentMap cache) {
        return new AttributeCompletion<>(manager, languages, source, flowRef, cache);
    }

    @lombok.AllArgsConstructor
    private static final class WebSourceCompletion extends SdmxAutoCompletion {

        @lombok.NonNull
        private final SdmxWebManager manager;

        private final @NonNull Languages languages;

        @Override
        public AutoCompletionSource getSource() {
            return ExtAutoCompletionSource
                    .builder(this::load)
                    .behavior(SYNC)
                    .postProcessor(this::filterAndSort)
                    .valueToString(SdmxWebSource::getId)
                    .build();
        }

        @Override
        public ListCellRenderer getRenderer() {
            return new CustomListCellRenderer<SdmxWebSource>() {
                @Override
                protected String getValueAsString(SdmxWebSource value) {
                    return value.getId() + ": " + languages.select(value.getNames());
                }

                @Override
                protected Icon toIcon(String term, JList list, SdmxWebSource value, int index, boolean isSelected, boolean cellHasFocus) {
                    return getFavicon(value.getWebsite(), list::repaint);
                }
            };
        }

        private List<SdmxWebSource> load(String term) {
            return manager
                    .getSources()
                    .values()
                    .stream()
                    .filter(source -> !source.isAlias())
                    .collect(toList());
        }

        private List<SdmxWebSource> filterAndSort(List<SdmxWebSource> list, String term) {
            return list.stream().filter(getFilter(term)).collect(toList());
        }

        private Predicate<SdmxWebSource> getFilter(String term) {
            Predicate<String> filter = ExtAutoCompletionSource.basicFilter(term);
            return value -> filter.test(languages.select(value.getNames()))
                    || filter.test(value.getId())
                    || value.getAliases().stream().anyMatch(filter);
        }
    }

    @lombok.AllArgsConstructor
    private static final class DataflowCompletion<S extends SdmxSource> extends SdmxAutoCompletion {

        @lombok.NonNull
        private final SdmxManager<S> manager;

        private final @NonNull Languages languages;

        @lombok.NonNull
        private final Supplier<S> source;

        @lombok.NonNull
        private final ConcurrentMap cache;

        @Override
        public AutoCompletionSource getSource() {
            return ExtAutoCompletionSource
                    .builder(this::load)
                    .behavior(this::getBehavior)
                    .postProcessor(this::filterAndSort)
                    .valueToString(o -> o.getRef().toString())
                    .cache(cache, this::getCacheKey, SYNC)
                    .build();
        }

        @Override
        public ListCellRenderer getRenderer() {
            return CustomListCellRenderer.<Dataflow>of(flow -> flow.getRef() + "<br><i>" + flow.getName(), flow -> flow.getRef().toString());
        }

        private List<Dataflow> load(String term) throws Exception {
            try (Connection c = manager.getConnection(source.get(), languages)) {
                return new ArrayList<>(c.getFlows());
            }
        }

        private AutoCompletionSource.Behavior getBehavior(String term) {
            return source.get() != null ? ASYNC : NONE;
        }

        private List<Dataflow> filterAndSort(List<Dataflow> values, String term) {
            Predicate<String> filter = ExtAutoCompletionSource.basicFilter(term);
            return values.stream()
                    .filter(o -> filter.test(o.getName()) || filter.test(o.getRef().getId()) || filter.test(o.getDescription()))
                    .sorted(Comparator.comparing(Dataflow::getName))
                    .collect(toList());
        }

        private String getCacheKey(String term) {
            return "Dataflow" + source.get() + languages;
        }
    }

    @lombok.AllArgsConstructor
    private static final class DimensionCompletion<S extends SdmxSource> extends SdmxAutoCompletion {

        @lombok.NonNull
        private final SdmxManager<S> manager;

        private final @NonNull Languages languages;

        @lombok.NonNull
        private final Supplier<S> source;

        @lombok.NonNull
        private final Supplier<DataflowRef> flowRef;

        @lombok.NonNull
        private final ConcurrentMap cache;

        @Override
        public AutoCompletionSource getSource() {
            return ExtAutoCompletionSource
                    .builder(this::load)
                    .behavior(this::getBehavior)
                    .postProcessor(this::filterAndSort)
                    .valueToString(Dimension::getId)
                    .cache(cache, this::getCacheKey, SYNC)
                    .build();
        }

        @Override
        public ListCellRenderer getRenderer() {
            return CustomListCellRenderer.of(Dimension::getId, Dimension::getName);
        }

        private List<Dimension> load(String term) throws Exception {
            try (Connection c = manager.getConnection(source.get(), languages)) {
                return new ArrayList<>(c.getStructure(flowRef.get()).getDimensions());
            }
        }

        private AutoCompletionSource.Behavior getBehavior(String term) {
            return source.get() != null && flowRef.get() != null ? ASYNC : NONE;
        }

        private List<Dimension> filterAndSort(List<Dimension> values, String term) {
            Predicate<String> filter = ExtAutoCompletionSource.basicFilter(term);
            return values.stream()
                    .filter(o -> filter.test(o.getId()) || filter.test(o.getName()) || filter.test(String.valueOf(o.getPosition())))
                    .sorted(Comparator.comparing(Dimension::getId))
                    .collect(toList());
        }

        private String getCacheKey(String term) {
            return "Dimension" + source.get() + flowRef.get() + languages;
        }
    }

    @lombok.AllArgsConstructor
    private static final class AttributeCompletion<S extends SdmxSource> extends SdmxAutoCompletion {

        @lombok.NonNull
        private final SdmxManager<S> manager;

        private final @NonNull Languages languages;

        @lombok.NonNull
        private final Supplier<S> source;

        @lombok.NonNull
        private final Supplier<DataflowRef> flowRef;

        @lombok.NonNull
        private final ConcurrentMap cache;

        @Override
        public AutoCompletionSource getSource() {
            return ExtAutoCompletionSource
                    .builder(o -> load(o))
                    .behavior(this::getBehavior)
                    .postProcessor(this::filterAndSort)
                    .valueToString(Attribute::getId)
                    .cache(cache, this::getCacheKey, SYNC)
                    .build();
        }

        @Override
        public ListCellRenderer getRenderer() {
            return CustomListCellRenderer.of(Attribute::getId, Attribute::getName);
        }

        private List<Attribute> load(String term) throws Exception {
            try (Connection c = manager.getConnection(source.get(), languages)) {
                return new ArrayList<>(c.getStructure(flowRef.get()).getAttributes());
            }
        }

        private AutoCompletionSource.Behavior getBehavior(String term) {
            return source.get() != null && flowRef.get() != null ? ASYNC : NONE;
        }

        private List<Attribute> filterAndSort(List<Attribute> values, String term) {
            Predicate<String> filter = ExtAutoCompletionSource.basicFilter(term);
            return values.stream()
                    .filter(o -> filter.test(o.getId()) || filter.test(o.getName()))
                    .sorted(Comparator.comparing(Attribute::getId))
                    .collect(toList());
        }

        private String getCacheKey(String term) {
            return "Attribute" + source.get() + flowRef.get() + languages;
        }
    }

    public static ImageIcon getDefaultIcon() {
        return ImageUtilities.loadImageIcon("jdplus/sdmx/desktop/plugin/sdmx-logo.png", false);
    }

    public static Icon getFavicon(URL website) {
        return website != null
                ? FAVICONS.getOrDefault(FaviconRef.of(DomainName.of(website), 16), getDefaultIcon())
                : getDefaultIcon();
    }

    public static Icon getFavicon(URL website, Runnable callback) {
        return website != null
                ? FAVICONS.getOrDefault(FaviconRef.of(DomainName.of(website), 16), callback, getDefaultIcon())
                : getDefaultIcon();
    }

    public static final FaviconSupport FAVICONS = FaviconSupport
            .ofServiceLoader()
            .toBuilder()
            .client(new FaviconClientOverCustomNetwork())
            .cache(new HashMap<>())
            //            .cache(IOCacheFactoryLoader.get().ofTtl(Duration.ofHours(1)))
            .build();

    private static final class FaviconClientOverCustomNetwork implements URLConnectionFactory {

        @Override
        public @NonNull URLConnection openConnection(@NonNull URL url) throws IOException {
            Network network = getNetworking().getNetwork(asSource(url));
            Proxy proxy = selectProxy(network, url);
            URLConnection result = network.getURLConnectionFactory().openConnection(url, proxy);
            applyHttps(result, network);
            return result;
        }

        private static SdmxWebSource asSource(URL url) throws IOException {
            try {
                return SdmxWebSource.builder().id("").endpoint(url.toURI()).driver("").build();
            } catch (URISyntaxException ex) {
                throw new IOException(ex);
            }
        }

        private static void applyHttps(URLConnection result, Network network) {
            if (result instanceof HttpsURLConnection) {
                HttpsURLConnection https = (HttpsURLConnection) result;
                SSLFactory sslFactory = network.getSSLFactory();
                https.setHostnameVerifier(sslFactory.getHostnameVerifier());
                https.setSSLSocketFactory(sslFactory.getSSLSocketFactory());
            }
        }

        private static Proxy selectProxy(Network network, URL url) throws IOException {
            try {
                return network.getProxySelector().select(url.toURI()).stream().findFirst().orElse(Proxy.NO_PROXY);
            } catch (URISyntaxException ex) {
                throw new IOException(ex);
            }
        }

        private static Networking getNetworking() {
            return Optional.ofNullable(Lookup.getDefault().lookup(SdmxWebProvider.class))
                    .map(provider -> provider.getSdmxManager().getNetworking())
                    .orElseGet(Networking::getDefault);
        }
    }

    private static Network getNetwork() {
        return TsManager
                .get()
                .getProvider(SdmxWebProvider.class)
                .map(SdmxWebProvider::getSdmxManager)
                .map(SdmxWebManager::getNetworking)
                .map(o -> o.getNetwork(SdmxWebSource.builder().id("").driver("").endpoint(Parser.onURI().parse("http://localhost")).build()))
                .orElse(Network.getDefault());
    }
}
