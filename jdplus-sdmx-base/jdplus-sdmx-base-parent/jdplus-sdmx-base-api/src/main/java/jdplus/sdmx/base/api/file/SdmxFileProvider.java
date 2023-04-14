/*
 * Copyright 2015 National Bank of Belgium
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
package jdplus.sdmx.base.api.file;

import jdplus.toolkit.base.api.timeseries.TsProvider;
import jdplus.sdmx.base.api.HasSdmxProperties;
import jdplus.toolkit.base.tsp.*;
import jdplus.toolkit.base.tsp.cube.CubeConnection;
import jdplus.toolkit.base.tsp.cube.CubeSupport;
import jdplus.toolkit.base.tsp.stream.HasTsStream;
import jdplus.toolkit.base.tsp.stream.TsStreamAsProvider;
import jdplus.toolkit.base.tsp.util.ResourcePool;
import internal.sdmx.base.api.SdmxCubeConnection;
import internal.sdmx.base.api.SdmxCubeItems;
import internal.sdmx.base.api.SdmxPropertiesSupport;
import nbbrd.io.Resource;
import nbbrd.service.ServiceProvider;
import sdmxdl.Connection;
import sdmxdl.DataflowRef;
import sdmxdl.file.SdmxFileManager;
import sdmxdl.file.SdmxFileSource;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * @author Philippe Charles
 * @since 2.2.0
 */
@ServiceProvider(TsProvider.class)
public final class SdmxFileProvider implements FileLoader<SdmxFileBean>, HasSdmxProperties<SdmxFileManager> {

    public static final String NAME = "sdmx-file";

    @lombok.experimental.Delegate
    private final HasSdmxProperties<SdmxFileManager> properties;

    @lombok.experimental.Delegate
    private final HasDataSourceMutableList mutableListSupport;

    @lombok.experimental.Delegate
    private final HasDataMoniker monikerSupport;

    @lombok.experimental.Delegate
    private final HasDataSourceBean<SdmxFileBean> beanSupport;

    @lombok.experimental.Delegate
    private final HasFilePaths filePathSupport;

    @lombok.experimental.Delegate(excludes = {HasTsStream.class, HasDataDisplayName.class})
    private final CubeSupport cubeSupport;

    @lombok.experimental.Delegate
    private final TsProvider tsSupport;

    public SdmxFileProvider() {
        ResourcePool<CubeConnection> pool = CubeSupport.newConnectionPool();
        SdmxFileParam param = new SdmxFileParam.V1();

        this.properties = SdmxPropertiesSupport.of(SdmxFileManager::ofServiceLoader, pool::clear);
        this.mutableListSupport = HasDataSourceMutableList.of(NAME, pool::remove);
        this.monikerSupport = HasDataMoniker.usingUri(NAME);
        this.beanSupport = HasDataSourceBean.of(NAME, param, param.getVersion());
        this.filePathSupport = HasFilePaths.of(pool::clear);
        this.cubeSupport = CubeSupport.of(NAME, pool.asFactory(o -> openConnection(o, properties, filePathSupport, param)), param::getCubeIdParam);
        this.tsSupport = TsStreamAsProvider.of(NAME, cubeSupport, monikerSupport, pool::clear);
    }

    @Override
    public String getDisplayName() {
        return "SDMX Files";
    }

    @Override
    public String getFileDescription() {
        return "SDMX file";
    }

    @Override
    public boolean accept(File pathname) {
        return pathname.getName().toLowerCase(Locale.ROOT).endsWith(".xml");
    }

    @Override
    public String getDisplayName(DataSource dataSource) throws IllegalArgumentException {
        return getSourceLabel(decodeBean(dataSource));
    }

    @Override
    public String getDisplayName(DataSet dataSet) throws IllegalArgumentException {
        return cubeSupport.getDisplayName(dataSet);
    }

    @Override
    public String getDisplayName(IOException exception) throws IllegalArgumentException {
        return cubeSupport.getDisplayName(exception);
    }

    @Override
    public String getDisplayNodeName(DataSet dataSet) throws IllegalArgumentException {
        return cubeSupport.getDisplayNodeName(dataSet);
    }

    private static CubeConnection openConnection(DataSource dataSource, HasSdmxProperties<SdmxFileManager> properties, HasFilePaths paths, SdmxFileParam param) throws IOException {
        SdmxFileBean bean = param.get(dataSource);
        SdmxFileSource files = SdmxCubeItems.resolveFileSet(paths, bean);

        DataflowRef flow = files.asDataflowRef();

        Connection conn = properties.getSdmxManager().getConnection(files);
        try {
            return SdmxCubeConnection.of(conn, flow, bean.getDimensions(), bean.getLabelAttribute(), getSourceLabel(bean));
        } catch (IOException ex) {
            Resource.ensureClosed(ex, conn);
            throw ex;
        }
    }

    private static String getSourceLabel(SdmxFileBean bean) {
        return bean.getFile().getPath();
    }
}
