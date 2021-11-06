package diarsid.jdock.jfx;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
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

        Menu fullScreenMode = new Menu("full screen mode");
//        fullScreenMode.setOnAction(event -> dock.app.toggleFullScreen());
        MenuItem enable = new MenuItem("enable");
        MenuItem disable = new MenuItem("disable");
        MenuItem enable15Mins = new MenuItem("enable on 15 minutes");

        fullScreenMode.getItems().setAll(enable, enable15Mins, disable);


        this.getItems().setAll(reload, fullScreenMode, exit);

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
