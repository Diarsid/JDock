package diarsid.jdock.jfx;

import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import diarsid.jdock.app.DockApp;
import diarsid.jdock.json.ItemJson;
import diarsid.jdock.model.DockPosition;
import diarsid.jdock.model.DockSession;
import diarsid.support.filesystem.FileInvoker;
import diarsid.support.filesystem.InvokeException;
import diarsid.support.javafx.StageAlwaysOnTopKeeper;

import static java.util.concurrent.TimeUnit.SECONDS;
import static javafx.css.PseudoClass.getPseudoClass;

import static diarsid.jdock.model.DockOrientation.VERTICAL;

public class Dock {

    private final Stage stage;
    private final Scene scene;
    private final Pane box;
    private final HBox dockPadding;
    private final Pane dock;
    private final Label dockFold;
    private final StageAlwaysOnTopKeeper onTopKeeper;
    public final DockApp app;
    public final DockPosition position;
    public final DockSession session;
    public final ContextMenuForDock contextMenu;

    public Dock(DockPosition position, DockApp app) {
        this.position = position;
        this.app = app;

        this.stage = new Stage();
        this.stage.initStyle(StageStyle.TRANSPARENT);
        this.stage.setAlwaysOnTop(true);
        this.stage.setMinWidth(1);
        this.stage.setMinHeight(1);
        this.stage.setResizable(true);
        this.stage.initOwner(app.hiddenStages.newHiddenStageFor(this.stage));

        Pane boxPane;
        Pane dockPane;
        this.dockFold = new Label();
        this.dockFold.getStyleClass().add("dock-fold");

        if ( this.position.dockOrientation == VERTICAL ) {
            VBox dockPaneVBox = new VBox();
            dockPaneVBox.getStyleClass().add("dock");
            dockPane = dockPaneVBox;

            HBox boxPaneHBox = new HBox();
            boxPaneHBox.setAlignment(Pos.CENTER);
            boxPaneHBox.setFillHeight(false);
            boxPane = boxPaneHBox;
        }
        else {
            HBox dockPaneHBox = new HBox();
            dockPaneHBox.getStyleClass().add("dock");
            dockPane = dockPaneHBox;

            VBox boxPaneVBox = new VBox();
            boxPaneVBox.setAlignment(Pos.CENTER);
            boxPaneVBox.setFillWidth(false);
            boxPane = boxPaneVBox;
        }

        this.dock = dockPane;
        this.dock.setVisible(false);
        this.dock.pseudoClassStateChanged(getPseudoClass(position.name().toLowerCase()), true);

        this.dockPadding = new HBox();
        this.dockPadding.getChildren().add(this.dock);
        this.dockPadding.getStyleClass().add("dock-margin");
        this.dockPadding.pseudoClassStateChanged(getPseudoClass(position.name().toLowerCase()), true);
        this.dockPadding.setAlignment(Pos.CENTER);

        this.box = boxPane;
        this.box.setStyle("-fx-background-color: transparent; ");

        this.dockFold.setVisible(true);

        this.position.arrangeInCorrectOrder(this.box, this.dockPadding, this.dockFold);

        this.session = new DockSession(position, app.namedThreadSource, this::showDock, this::tryHideDock, this::canFinishSession);

        this.dockFold.setOnMouseEntered(event -> {
            session.touch();
            showDock();
        });

        this.dockFold.setOnMouseMoved(event -> {
            session.touch();
            showDock();
        });

        this.dock.setOnMouseEntered(event -> {
            session.touch();
        });

        this.dock.setOnMouseMoved(event -> {
            session.touch();
        });

        this.scene = new Scene(this.box);

        if ( this.position.dockOrientation == VERTICAL ) {
            this.dockFold.prefHeightProperty().bind(this.dockPadding.heightProperty());
        }
        else {
            this.dockFold.minWidthProperty().bind(this.dockPadding.widthProperty());
        }

        String name = position.name().toLowerCase();
        this.onTopKeeper = new StageAlwaysOnTopKeeper(name, this.stage, app.namedThreadSource.namedThreadFactory(name), 1, SECONDS);
        this.onTopKeeper.startWork();


        this.contextMenu = new ContextMenuForDock(this);
        this.contextMenu.setAutoHide(true);

        this.dock.setOnMousePressed(event -> {
            if ( event.isPrimaryButtonDown() ) {
                this.contextMenu.hide();
            }
            else if ( event.isSecondaryButtonDown() ) {
                this.contextMenu.show(this.dock, event.getScreenX(), event.getScreenY());
            }
        });

        this.configure();
    }

    private void configure() {
        List<Label> icons = new ArrayList<>();
        ItemJson[] items = this.app.config.get().getDocks().get(this.position);
        ItemIcon icon;
        Item item;
        for ( int i = 0; i < items.length; i++ )  {
            item = new Item(this.position, i, items[i]);
            icon = new ItemIcon(this, item, this::onInvocation);
            icons.add(icon.iconLabel);
        }

        this.setFoldThick(app.config.get().getSettings().getFoldThick());

        this.dock.getChildren().addAll(icons);

        this.scene.setFill(Color.TRANSPARENT);
        this.scene.getStylesheets().add("file:./jdock-style.css");
        this.stage.setScene(scene);
        this.stage.sizeToScene();

        this.stage.show();

        this.position.assignXY(this.stage);
    }

    public void reconfigure() {
        if ( this.app.config.get().getDocks().get(this.position).length == 0 ) {
            this.deactivate();
        }
        else  {
            this.clearAndHide();
            this.configure();
        }
    }

    private void clearAndHide() {
        this.stage.hide();
        this.scene.getStylesheets().remove("file:./jdock-style.css");
        this.dock.getChildren().clear();
    }

    private void pauseAll() {
        this.onTopKeeper.pauseWork();
    }

    private void resumeAll() {
        this.onTopKeeper.startWork();
    }

    public void deactivate() {
        this.clearAndHide();
        this.pauseAll();
    }

    public void activate() {
        this.resumeAll();
        this.configure();
    }

    private void onInvocation(ItemIcon itemIcon) {
        this.contextMenu.hide();
        try {
            String target = itemIcon.item.target;
            FileInvoker.Invocation invocation = this.app.fileInvoker.invoke(target);
        }
        catch (InvokeException e) {
            e.printStackTrace();
        }
    }

    private void showDock() {
        if ( this.dock.visibleProperty().get() ) {
            return;
        }

        Platform.requestNextPulse();
        this.dockFold.setVisible(false);
        this.setFoldThick(0);
        Platform.requestNextPulse();
        this.dock.setVisible(true);
        this.stage.sizeToScene();
        this.position.assignXY(this.stage);
        Platform.requestNextPulse();
    }

    private void setFoldThick(int i) {
        if ( this.position.dockOrientation == VERTICAL ) {
            setFoldWidth(i);
        } else {
            setFoldHeight(i);
        }
    }

    private void setFoldWidth(int i) {
        this.dockFold.setMinWidth(i);
        this.dockFold.setMaxWidth(i);
    }

    private void setFoldHeight(int i) {
        this.dockFold.setMinHeight(i);
        this.dockFold.setMaxHeight(i);
    }

    private void tryHideDock() {
        if ( this.dock.hoverProperty().get() ) {
            this.session.touch();
        }
        else {
            this.hideDock();
        }
    }

    private void hideDock() {
        Platform.requestNextPulse();
        this.dock.setVisible(false);
        this.contextMenu.hide();
        this.dockFold.setVisible(true);
        this.setFoldThick(this.app.config.get().getSettings().getFoldThick());
        this.stage.sizeToScene();
        this.position.assignXY(this.stage);
        Platform.requestNextPulse();
    }

    private boolean canFinishSession() {
        return
                !this.dock.hoverProperty().get() &&
                !this.dockFold.hoverProperty().get() &&
                !this.contextMenu.showingProperty().get();
    }

}
