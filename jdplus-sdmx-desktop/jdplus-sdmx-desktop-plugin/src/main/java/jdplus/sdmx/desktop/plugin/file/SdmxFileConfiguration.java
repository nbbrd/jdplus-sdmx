package jdplus.sdmx.desktop.plugin.file;

import jdplus.toolkit.base.tsp.util.PropertyHandler;
import jdplus.toolkit.desktop.plugin.properties.NodePropertySetBuilder;
import jdplus.toolkit.desktop.plugin.util.Caches;
import jdplus.toolkit.desktop.plugin.util.Persistence;
import nbbrd.design.MightBeGenerated;
import org.openide.awt.StatusDisplayer;
import org.openide.nodes.Sheet;
import sdmxdl.EventListener;
import sdmxdl.Languages;
import sdmxdl.file.SdmxFileManager;
import sdmxdl.file.SdmxFileSource;
import sdmxdl.file.spi.FileCaching;
import sdmxdl.format.MemCachingSupport;

import java.util.Locale;

@lombok.Data
public class SdmxFileConfiguration {

    private static final String LANGUAGES_PROPERTY = "languages";
    private static final String DEFAULT_LANGUAGES = null;
    private String languages = DEFAULT_LANGUAGES;

    private static final String NO_CACHE_PROPERTY = "noCache";
    private static final boolean DEFAULT_NO_CACHE = false;
    private boolean noCache = DEFAULT_NO_CACHE;

    @MightBeGenerated
    public static SdmxFileConfiguration copyOf(SdmxFileConfiguration bean) {
        SdmxFileConfiguration result = new SdmxFileConfiguration();
        result.languages = bean.languages;
        result.noCache = bean.noCache;
        return result;
    }

    public SdmxFileManager toSdmxFileManager() {
        return SdmxFileManager.ofServiceLoader()
                .toBuilder()
                .onEvent(toEventListener())
                .caching(toCache())
                .build();
    }

    public Languages toLanguages() throws IllegalArgumentException {
        return languages != null ? Languages.parse(languages) : Languages.ANY;
    }

    private EventListener<? super SdmxFileSource> toEventListener() {
        return (source, marker, message) -> StatusDisplayer.getDefault().setStatusText(message.toString());
    }

    private FileCaching toCache() {
        if (noCache) {
            return FileCaching.noOp();
        }
        return MemCachingSupport
                .builder()
                .id("SHARED_SOFT_MEM")
                .repositoriesOf(Caches.softValuesCacheAsMap())
                .webMonitorsOf(Caches.softValuesCacheAsMap())
                .build();
    }

    Sheet toSheet() {
        Sheet result = new Sheet();
        NodePropertySetBuilder b = new NodePropertySetBuilder();

        b.withAutoCompletion()
                .select(this, LANGUAGES_PROPERTY)
                .servicePath(Locale.class.getName())
                .separator(",")
                .display("Languages")
                .description("Language priority list")
                .add();
        result.put(b.build());

        b.reset("Other");
        b.withBoolean()
                .select(this, NO_CACHE_PROPERTY)
                .display("No cache")
                .description("Disable caching")
                .add();
        result.put(b.build());

        return result;
    }

    @MightBeGenerated
    static final Persistence<SdmxFileConfiguration> PERSISTENCE = Persistence
            .builderOf(SdmxFileConfiguration.class)
            .name("INSTANCE")
            .version("VERSION")
            .with(PropertyHandler.onString(LANGUAGES_PROPERTY, DEFAULT_LANGUAGES), SdmxFileConfiguration::getLanguages, SdmxFileConfiguration::setLanguages)
            .with(PropertyHandler.onBoolean(NO_CACHE_PROPERTY, DEFAULT_NO_CACHE), SdmxFileConfiguration::isNoCache, SdmxFileConfiguration::setNoCache)
            .build();
}
