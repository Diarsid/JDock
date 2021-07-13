package diarsid.jdock.jfx;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.stage.WindowEvent;

public class ContextMenuForDock extends ContextMenu {

    public static final String BLOCK_NAME = "CONTEXT_MENU_DOCK";

    private final Dock dock;

    public ContextMenuForDock(Dock dock) {
        this.dock = dock;
        MenuItem exit = new MenuItem("exit");
        exit.setOnAction(event -> dock.app.exit());
        MenuItem reload = new MenuItem("reload settings");
        reload.setOnAction(event -> dock.app.reload());
        this.getItems().setAll(reload, exit);

        this.setOnShowing(this::onShow);
        this.setOnHiding(this::onHide);
    }

    private void onShow(WindowEvent event) {
        this.dock.session.block(BLOCK_NAME);
    }

    private void onHide(WindowEvent event) {
        this.dock.session.unblock(BLOCK_NAME);
    }
}
