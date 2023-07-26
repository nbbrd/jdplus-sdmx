package jdplus.sdmx.desktop.plugin.web;

import internal.sdmx.desktop.plugin.CustomNetwork;
import internal.sdmx.desktop.plugin.SdmxIcons;
import standalone_sdmxdl.internal.util.WebCachingLoader;
import jdplus.toolkit.base.tsp.util.PropertyHandler;
import jdplus.toolkit.desktop.plugin.notification.MessageUtil;
import jdplus.toolkit.desktop.plugin.properties.NodePropertySetBuilder;
import jdplus.toolkit.desktop.plugin.util.Persistence;
import nbbrd.design.MightBeGenerated;
import standalone_sdmxdl.nbbrd.io.text.Parser;
import org.openide.awt.NotificationDisplayer;
import org.openide.awt.StatusDisplayer;
import org.openide.nodes.Sheet;
import sdmxdl.Languages;
import standalone_sdmxdl.sdmxdl.format.xml.XmlWebSource;
import standalone_sdmxdl.sdmxdl.provider.web.SingleNetworkingSupport;
import sdmxdl.web.SdmxWebManager;
import sdmxdl.web.SdmxWebSource;
import sdmxdl.web.spi.Networking;
import sdmxdl.web.spi.WebCaching;

import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@lombok.Data
public class SdmxWebConfiguration {

    private static final String SOURCES_PROPERTY = "sources";
    private static final File DEFAULT_SOURCES = null;
    private File sources = DEFAULT_SOURCES;

    private static final String LANGUAGES_PROPERTY = "languages";
    private static final String DEFAULT_LANGUAGES = null;
    private String languages = DEFAULT_LANGUAGES;

    private static final String CURL_BACKEND_PROPERTY = "curlBackend";
    private static final boolean DEFAULT_CURL_BACKEND = false;
    private boolean curlBackend = DEFAULT_CURL_BACKEND;

    private static final String NO_CACHE_PROPERTY = "noCache";
    private static final boolean DEFAULT_NO_CACHE = false;
    private boolean noCache = DEFAULT_NO_CACHE;

    private static final String AUTO_PROXY_PROPERTY = "autoProxy";
    private static final boolean DEFAULT_AUTO_PROXY = false;
    private boolean autoProxy = DEFAULT_AUTO_PROXY;

    private static final String NO_DEFAULT_SSL_PROPERTY = "noDefaultSSL";
    private static final boolean DEFAULT_NO_DEFAULT_SSL = false;
    private boolean noDefaultSSL = DEFAULT_NO_DEFAULT_SSL;

    private static final String NO_SYSTEM_SSL_PROPERTY = "noSystemSSL";
    private static final boolean DEFAULT_NO_SYSTEM_SSL = false;
    private boolean noSystemSSL = DEFAULT_NO_SYSTEM_SSL;

    private static final String DISPLAY_CODES_PROPERTY = "displayCodes";
    private static final boolean DEFAULT_DISPLAY_CODES = false;
    private boolean displayCodes = DEFAULT_DISPLAY_CODES;

    @MightBeGenerated
    public static SdmxWebConfiguration copyOf(SdmxWebConfiguration bean) {
        SdmxWebConfiguration result = new SdmxWebConfiguration();
        result.sources = bean.sources;
        result.languages = bean.languages;
        result.curlBackend = bean.curlBackend;
        result.noCache = bean.noCache;
        result.autoProxy = bean.autoProxy;
        result.noDefaultSSL = bean.noDefaultSSL;
        result.noSystemSSL = bean.noSystemSSL;
        result.displayCodes = bean.displayCodes;
        return result;
    }

    public SdmxWebManager toSdmxWebManager() {
        return SdmxWebManager.ofServiceLoader()
                .toBuilder()
                .onEvent(this::reportEvent)
                .onError(this::reportError)
                .caching(toCaching())
                .networking(toNetworking())
                .customSources(toSources())
                .build();
    }

    public Languages toLanguages() {
        return Parser.of(Languages::parse)
                .parseValue(languages)
                .orElse(Languages.ANY);
    }

    private void reportEvent(SdmxWebSource source, String marker, CharSequence message) {
        StatusDisplayer.getDefault().setStatusText(message.toString());
    }

    private void reportError(SdmxWebSource source, String marker, CharSequence message, IOException error) {
        NotificationDisplayer.getDefault().notify(message.toString(), SdmxIcons.getDefaultIcon(), "", null);
    }

    private WebCaching toCaching() {
        if (noCache) {
            return WebCaching.noOp();
        }
        return WebCachingLoader.load();
    }

    private Networking toNetworking() {
        CustomNetwork x = CustomNetwork
                .builder()
                .curlBackend(curlBackend)
                .autoProxy(autoProxy)
                .defaultTrustMaterial(!noDefaultSSL)
                .systemTrustMaterial(!noSystemSSL)
                .build();
        return SingleNetworkingSupport
                .builder()
                .id("DRY")
                .proxySelector(x::getProxySelector)
                .sslFactory(x::getSSLFactory)
                .urlConnectionFactory(x::getURLConnectionFactory)
                .build();
    }

    private List<SdmxWebSource> toSources() {
        if (sources != null && sources.exists()) {
            try {
                return XmlWebSource.getParser().parseFile(sources);
            } catch (IOException ex) {
                MessageUtil.showException("Cannot load custom sources", ex);
            }
        }
        return Collections.emptyList();
    }

    Sheet toSheet() {
        Sheet result = new Sheet();
        NodePropertySetBuilder b = new NodePropertySetBuilder();

        b.withFile()
                .select(this, SOURCES_PROPERTY)
                .display("Sources")
                .description("File that provides data source definitions")
                .filterForSwing(new FileNameExtensionFilter("XML file", "xml"))
                .directories(false)
                .add();
        b.withAutoCompletion()
                .select(this, LANGUAGES_PROPERTY)
                .servicePath(Locale.class.getName())
                .separator(",")
                .display("Languages")
                .description("Language priority list")
                .add();
        result.put(b.build());

        b.reset("Network");
        b.withBoolean()
                .select(this, CURL_BACKEND_PROPERTY)
                .display("Curl backend")
                .description("Use curl backend instead of JDK")
                .add();
        b.withBoolean()
                .select(this, NO_CACHE_PROPERTY)
                .display("No cache")
                .description("Disable caching")
                .add();
        b.withBoolean()
                .select(this, AUTO_PROXY_PROPERTY)
                .display("Auto proxy")
                .description("Enable automatic proxy detection")
                .add();
        b.withBoolean()
                .select(this, NO_DEFAULT_SSL_PROPERTY)
                .display("No default SSL")
                .description("Disable default truststore")
                .add();
        b.withBoolean()
                .select(this, NO_SYSTEM_SSL_PROPERTY)
                .display("No system SSL")
                .description("Disable system truststore")
                .add();
        result.put(b.build());

        return result;
    }

    @MightBeGenerated
    static final Persistence<SdmxWebConfiguration> PERSISTENCE = Persistence
            .builderOf(SdmxWebConfiguration.class)
            .name("INSTANCE")
            .version("20230717")
            .with(PropertyHandler.onFile(SOURCES_PROPERTY, DEFAULT_SOURCES), SdmxWebConfiguration::getSources, SdmxWebConfiguration::setSources)
            .with(PropertyHandler.onString(LANGUAGES_PROPERTY, DEFAULT_LANGUAGES), SdmxWebConfiguration::getLanguages, SdmxWebConfiguration::setLanguages)
            .with(PropertyHandler.onBoolean(CURL_BACKEND_PROPERTY, DEFAULT_CURL_BACKEND), SdmxWebConfiguration::isCurlBackend, SdmxWebConfiguration::setCurlBackend)
            .with(PropertyHandler.onBoolean(NO_CACHE_PROPERTY, DEFAULT_NO_CACHE), SdmxWebConfiguration::isNoCache, SdmxWebConfiguration::setNoCache)
            .with(PropertyHandler.onBoolean(AUTO_PROXY_PROPERTY, DEFAULT_AUTO_PROXY), SdmxWebConfiguration::isAutoProxy, SdmxWebConfiguration::setAutoProxy)
            .with(PropertyHandler.onBoolean(NO_DEFAULT_SSL_PROPERTY, DEFAULT_NO_DEFAULT_SSL), SdmxWebConfiguration::isNoDefaultSSL, SdmxWebConfiguration::setNoDefaultSSL)
            .with(PropertyHandler.onBoolean(NO_SYSTEM_SSL_PROPERTY, DEFAULT_NO_SYSTEM_SSL), SdmxWebConfiguration::isNoSystemSSL, SdmxWebConfiguration::setNoSystemSSL)
            .with(PropertyHandler.onBoolean(DISPLAY_CODES_PROPERTY, DEFAULT_DISPLAY_CODES), SdmxWebConfiguration::isDisplayCodes, SdmxWebConfiguration::setDisplayCodes)
            .build();
}
