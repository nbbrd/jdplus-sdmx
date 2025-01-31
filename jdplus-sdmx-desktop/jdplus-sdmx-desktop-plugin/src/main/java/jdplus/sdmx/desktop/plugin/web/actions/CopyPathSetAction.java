package jdplus.sdmx.desktop.plugin.web.actions;

import internal.sdmx.desktop.plugin.CatalogRef;
import internal.sdmx.desktop.plugin.OnDemandMenuBuilder;
import internal.sdmx.desktop.plugin.SdmxCommand;
import internal.sdmx.desktop.plugin.SdmxURI;
import jdplus.sdmx.base.api.web.SdmxWebBean;
import jdplus.sdmx.base.api.web.SdmxWebProvider;
import jdplus.toolkit.base.tsp.DataSet;
import jdplus.toolkit.base.tsp.DataSource;
import jdplus.toolkit.desktop.plugin.TsManager;
import jdplus.toolkit.desktop.plugin.actions.AbilityNodeAction;
import jdplus.toolkit.desktop.plugin.actions.Actions;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;
import org.openide.util.actions.Presenter;
import sdmxdl.Connection;
import sdmxdl.FlowRef;
import sdmxdl.Key;
import sdmxdl.Structure;

import javax.swing.*;
import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;

import static jdplus.toolkit.desktop.plugin.tsproviders.TsProviderNodes.COLLECTION_ACTION_PATH;
import static jdplus.toolkit.desktop.plugin.tsproviders.TsProviderNodes.SERIES_ACTION_PATH;

@ActionID(category = "Edit", id = CopyPathSetAction.ID)
@ActionRegistration(displayName = "#CTL_CopyPathSetAction", lazy = false)
@Messages("CTL_CopyPathSetAction=Copy Path/Reference...")
@ActionReferences({
        @ActionReference(path = COLLECTION_ACTION_PATH, position = 422, id = @ActionID(category = "Edit", id = CopyPathSetAction.ID)),
        @ActionReference(path = SERIES_ACTION_PATH, position = 422, id = @ActionID(category = "Edit", id = CopyPathSetAction.ID))
})
public final class CopyPathSetAction extends AbilityNodeAction<DataSet> implements Presenter.Popup {

    static final String ID = "jdplus.sdmx.desktop.plugin.web.actions.CopyPathSetAction";

    public CopyPathSetAction() {
        super(DataSet.class, true);
    }

    @Override
    public JMenuItem getPopupPresenter() {
        return Actions.hideWhenDisabled(new JMenuItem(this));
    }

    @Override
    protected void performAction(Stream<DataSet> items) {
        DataSet item = single(items).orElseThrow(NoSuchElementException::new);
        SdmxWebProvider provider = providerOf(item.getDataSource()).orElseThrow(NoSuchElementException::new);
        SdmxWebBean bean = provider.decodeBean(item.getDataSource());
        FlowRef flowRef = FlowRef.parse(bean.getFlow());
        Key key = getKey(provider, bean.getSource(), flowRef, item);
        CatalogRef catalog = CatalogRef.NO_CATALOG;
        new OnDemandMenuBuilder()
                .copyToClipboard("SDMX-DL URI", SdmxURI.dataSetURI(bean.getSource(), flowRef, key, catalog))
                .copyToClipboard("Source", bean.getSource())
                .copyToClipboard("Flow", flowRef.toString())
                .copyToClipboard("Key", key.toString())
                .addSeparator()
                .copyToClipboard("Fetch data command", SdmxCommand.fetchData(catalog, bean.getSource(), flowRef.toString(), key))
                .copyToClipboard("Fetch meta command", SdmxCommand.fetchMeta(catalog, bean.getSource(), flowRef.toString(), key))
                .copyToClipboard("Fetch keys command", SdmxCommand.fetchKeys(catalog, bean.getSource(), flowRef.toString(), key))
                .showMenuAsPopup(null);
    }

    @Override
    protected boolean enable(Stream<DataSet> items) {
        Optional<DataSet> item = single(items);
        return item.isPresent() && providerOf(item.orElseThrow().getDataSource()).isPresent();
    }

    @Override
    public String getName() {
        return Bundle.CTL_CopyPathSetAction();
    }

    private static Optional<SdmxWebProvider> providerOf(DataSource dataSource) {
        return TsManager.get().getProvider(SdmxWebProvider.class, dataSource);
    }

    private static <T> Optional<T> single(Stream<T> items) {
        List<T> list = items.toList();
        return list.size() == 1 ? Optional.of(list.get(0)) : Optional.empty();
    }

    private static Key getKey(SdmxWebProvider provider, String source, FlowRef flowRef, DataSet dataSet) {
        try (Connection connection = provider.getSdmxManager().getConnection(source, provider.getLanguages())) {
            Structure structure = connection.getStructure(flowRef);
            Key.Builder result = Key.builder(structure);
            structure.getDimensions().forEach(dimension -> result.put(dimension.getId(), dataSet.getParameter(dimension.getId())));
            return result.build();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
