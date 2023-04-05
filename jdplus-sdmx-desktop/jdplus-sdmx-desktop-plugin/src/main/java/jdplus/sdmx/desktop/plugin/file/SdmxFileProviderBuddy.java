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
package jdplus.sdmx.desktop.plugin.file;

import jdplus.toolkit.desktop.plugin.Config;
import jdplus.toolkit.desktop.plugin.Persistable;
import jdplus.toolkit.desktop.plugin.TsManager;
import jdplus.toolkit.desktop.plugin.actions.Configurable;
import jdplus.toolkit.desktop.plugin.properties.PropertySheetDialogBuilder;
import jdplus.toolkit.desktop.plugin.tsproviders.DataSourceProviderBuddy;
import jdplus.sdmx.base.api.file.SdmxFileBean;
import jdplus.sdmx.base.api.file.SdmxFileProvider;
import internal.sdmx.desktop.plugin.SdmxAutoCompletion;
import java.awt.Image;
import java.util.Optional;
import nbbrd.design.DirectImpl;
import nbbrd.service.ServiceProvider;
import org.openide.nodes.Sheet;

/**
 *
 * @author Philippe Charles
 */
@DirectImpl
@ServiceProvider(DataSourceProviderBuddy.class)
public final class SdmxFileProviderBuddy implements DataSourceProviderBuddy, Configurable, Persistable {

    private SdmxFileConfiguration configuration;

    public SdmxFileProviderBuddy() {
        this.configuration = new SdmxFileConfiguration();
        updateProvider();
    }

    private void updateProvider() {
        lookupProvider().ifPresent(provider -> provider.setSdmxManager(configuration.toSdmxFileManager()));
    }

    @Override
    public String getProviderName() {
        return SdmxFileProvider.NAME;
    }

    @Override
    public Image getIconOrNull(int type, boolean opened) {
        return SdmxAutoCompletion.getDefaultIcon().getImage();
    }

    @Override
    public Sheet getSheetOfBeanOrNull(Object bean) {
        return bean instanceof SdmxFileBean ? getSheetOrNull((SdmxFileBean) bean) : null;
    }

    @Override
    public void configure() {
        SdmxFileConfiguration editable = SdmxFileConfiguration.copyOf(configuration);
        PropertySheetDialogBuilder editor = new PropertySheetDialogBuilder()
                .title("Configure " + lookupProvider().map(SdmxFileProvider::getDisplayName).orElse(""))
                .icon(SdmxAutoCompletion.getDefaultIcon());
        if (editor.editSheet(editable.toSheet())) {
            configuration = editable;
            updateProvider();
        }
    }

    @Override
    public Config getConfig() {
        return SdmxFileConfiguration.PERSISTENCE.loadConfig(configuration);
    }

    @Override
    public void setConfig(Config config) throws IllegalArgumentException {
        SdmxFileConfiguration.PERSISTENCE.storeConfig(configuration, config);
        updateProvider();
    }

    private Sheet getSheetOrNull(SdmxFileBean bean) {
        return lookupProvider().map(provider -> SdmxFileBeanSupport.newSheet(bean, provider)).orElse(null);
    }

    private static Optional<SdmxFileProvider> lookupProvider() {
        return TsManager.get().getProvider(SdmxFileProvider.class);
    }
}
