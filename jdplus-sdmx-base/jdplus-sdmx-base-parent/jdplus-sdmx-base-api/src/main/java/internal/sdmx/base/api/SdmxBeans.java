package internal.sdmx.base.api;

import jdplus.sdmx.base.api.file.SdmxFileBean;
import jdplus.sdmx.base.api.file.SdmxFileProvider;
import jdplus.sdmx.base.api.web.SdmxWebBean;
import jdplus.sdmx.base.api.web.SdmxWebProvider;
import sdmxdl.DatabaseRef;
import sdmxdl.FlowRef;
import sdmxdl.file.FileSource;
import sdmxdl.web.WebSource;

import java.io.FileNotFoundException;
import java.util.Optional;

public final class SdmxBeans {

    private SdmxBeans() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static WebSource getWebSourceOrNull(SdmxWebBean bean, SdmxWebProvider provider) {
        return provider.getSdmxManager().getSources().get(bean.getSource());
    }

    public static DatabaseRef getDatabase(SdmxWebBean ignore) {
        return DatabaseRef.NO_DATABASE;
    }

    public static DatabaseRef getDatabase(SdmxFileBean ignore) {
        return DatabaseRef.NO_DATABASE;
    }

    public static FlowRef getFlowRefOrNull(SdmxWebBean bean) {
        try {
            return FlowRef.parse(bean.getFlow());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public static Optional<FileSource> getFileSource(SdmxFileBean bean, SdmxFileProvider provider) {
        try {
            return Optional.of(SdmxCubeItems.resolveFileSet(provider, bean));
        } catch (FileNotFoundException ex) {
            return Optional.empty();
        }
    }
}
