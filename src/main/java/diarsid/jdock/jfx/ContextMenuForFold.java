package diarsid.jdock.jfx;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

public class ContextMenuForFold extends ContextMenu {

    private final Dock dock;

    public ContextMenuForFold(Dock dock) {
        this.dock = dock;

        MenuItem exit = new MenuItem("exit");
        exit.setOnAction(event -> dock.app.exit());

        MenuItem reload = new MenuItem("reload settings");
        reload.setOnAction(event -> dock.app.reload());

        MenuItem fullScreenMode = new MenuItem("disable full screen mode");
        fullScreenMode.setOnAction(event -> dock.app.toggleFullScreen());

        this.getItems().setAll(reload, fullScreenMode, exit);
    }
}
