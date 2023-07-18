package jdplus.sdmx.desktop.plugin.web.actions;

import ec.util.desktop.Desktop;
import ec.util.desktop.DesktopManager;
import jdplus.sdmx.base.api.web.SdmxWebProvider;
import jdplus.toolkit.base.tsp.DataSource;
import jdplus.toolkit.desktop.plugin.TsManager;
import jdplus.toolkit.desktop.plugin.actions.AbilityNodeAction;
import jdplus.toolkit.desktop.plugin.actions.Actions;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;
import org.openide.util.actions.Presenter;
import sdmxdl.web.SdmxWebSource;

import javax.swing.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.stream.Stream;

import static jdplus.toolkit.desktop.plugin.tsproviders.TsProviderNodes.SOURCE_ACTION_PATH;

@ActionID(category = "Edit", id = OpenWebsiteAction.ID)
@ActionRegistration(displayName = "#CTL_OpenWebsiteAction", lazy = false)
@Messages("CTL_OpenWebsiteAction=Open web site")
@ActionReference(path = SOURCE_ACTION_PATH, position = 720, separatorBefore = 700, id = @ActionID(category = "Edit", id = OpenWebsiteAction.ID))
public final class OpenWebsiteAction extends AbilityNodeAction<DataSource> implements Presenter.Popup {

    static final String ID = "jdplus.sdmx.desktop.plugin.web.actions.OpenWebsiteAction";

    public OpenWebsiteAction() {
        super(DataSource.class, true);
    }

    @Override
    public JMenuItem getPopupPresenter() {
        return Actions.hideWhenDisabled(new JMenuItem(this));
    }

    @Override
    protected void performAction(Stream<DataSource> items) {
        items.forEach(item -> {
            try {
                DesktopManager.get().browse(getWebsite(item).toURI());
            } catch (IOException | URISyntaxException ex) {
                Exceptions.printStackTrace(ex);
            }
        });
    }

    @Override
    protected boolean enable(Stream<DataSource> items) {
        return DesktopManager.get().isSupported(Desktop.Action.BROWSE)
                && items.anyMatch(item -> getWebsite(item) != null);
    }

    private URL getWebsite(DataSource dataSource) {
        return TsManager.get()
                .getProvider(SdmxWebProvider.class, dataSource)
                .map(provider -> getWebsite(provider, dataSource))
                .orElse(null);
    }

    private URL getWebsite(SdmxWebProvider provider, DataSource dataSource) {
        SdmxWebSource source = provider.getSdmxManager().getSources().get(provider.decodeBean(dataSource).getSource());
        return source != null ? source.getWebsite() : null;
    }

    @Override
    public String getName() {
        return Bundle.CTL_OpenWebsiteAction();
    }
}
