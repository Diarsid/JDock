package diarsid.jdock.jfx;

import java.awt.MouseInfo;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import javafx.animation.Animation;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import diarsid.jdock.app.DockApp;
import diarsid.jdock.json.ConfigJson;
import diarsid.jdock.json.DockMove;
import diarsid.jdock.json.ItemJson;
import diarsid.jdock.model.DockPosition;
import diarsid.jdock.model.DockSession;
import diarsid.support.filesystem.FileInvoker;
import diarsid.support.filesystem.InvokeException;
import diarsid.support.javafx.StageAlwaysOnTopKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javafx.css.PseudoClass.getPseudoClass;

import static diarsid.jdock.json.DockMove.SMOOTH;
import static diarsid.jdock.model.DockOrientation.VERTICAL;

public class Dock {

    private static final Logger log = LoggerFactory.getLogger(Dock.class);

    public static final DockMove DEFAULT_MOVE = SMOOTH;
    public static final double DEFAULT_SHOW_TIME = 0.1;
    public static final double DEFAULT_HIDE_TIME = 0.08;

    private final Stage stageForDock;
    private final Stage stageForFold;
    private final Scene sceneForDock;
    private final Scene sceneForFold;
    private final HBox dockPadding;
    private final Pane dock;
    private final Label fold;
    private final StageAlwaysOnTopKeeper onTopKeeper;
    public final DockApp app;
    public final DockPosition position;
    public final DockSession session;
    public final ContextMenuForDock contextMenuDock;
    public final ContextMenuForFold contextMenuFold;
    private DockMove move;
    private Animation showSmoothly;
    private Animation hideSmoothly;

    public Dock(DockPosition position, DockApp app) {
        this.position = position;
        this.app = app;

        this.stageForDock = new Stage();
        this.stageForDock.initStyle(StageStyle.TRANSPARENT);
        this.stageForDock.setAlwaysOnTop(true);
        this.stageForDock.setMinWidth(1);
        this.stageForDock.setMinHeight(1);
        this.stageForDock.setResizable(true);
        this.stageForDock.initOwner(app.hiddenStages.newHiddenStageFor(this.stageForDock));

        this.stageForFold = new Stage();
        this.stageForFold.initStyle(StageStyle.TRANSPARENT);
        this.stageForFold.setAlwaysOnTop(true);
        this.stageForFold.setMinWidth(1);
        this.stageForFold.setMinHeight(1);
        this.stageForFold.setResizable(true);
        this.stageForFold.initOwner(app.hiddenStages.newHiddenStageFor(this.stageForFold));

        Pane dockPane;
        this.fold = new Label();
        this.fold.getStyleClass().add("fold");
        this.fold.pseudoClassStateChanged(getPseudoClass(position.name().toLowerCase()), true);

        if ( this.position.dockOrientation == VERTICAL ) {
            VBox dockPaneVBox = new VBox();
            dockPaneVBox.getStyleClass().add("dock");
            dockPane = dockPaneVBox;
        }
        else {
            HBox dockPaneHBox = new HBox();
            dockPaneHBox.getStyleClass().add("dock");
            dockPane = dockPaneHBox;
        }

        this.dock = dockPane;
        this.dock.setVisible(false);
        this.dock.pseudoClassStateChanged(getPseudoClass(position.name().toLowerCase()), true);

        this.dockPadding = new HBox();
        this.dockPadding.getChildren().add(this.dock);
        this.dockPadding.getStyleClass().add("dock-margin");
        this.dockPadding.pseudoClassStateChanged(getPseudoClass(position.name().toLowerCase()), true);
        this.dockPadding.setAlignment(Pos.CENTER);

        this.fold.setVisible(true);

        this.session = new DockSession(position, app.namedThreadSource, this::showDock, this::tryHideDock, this::canFinishSession);

        this.fold.setOnMouseEntered(this::foldTouched);
        this.fold.setOnMouseMoved(this::foldTouched);

        this.dock.setOnMouseEntered(event -> {
            session.touch();
        });

        this.dock.setOnMouseMoved(event -> {
            session.touch();
        });

        this.sceneForDock = new Scene(this.dockPadding);
        this.sceneForFold = new Scene(this.fold);

        if ( this.position.dockOrientation == VERTICAL ) {
            this.fold.prefHeightProperty().bind(this.dockPadding.heightProperty());
        }
        else {
            this.fold.minWidthProperty().bind(this.dockPadding.widthProperty());
        }

        String name = position.name().toLowerCase();
        this.onTopKeeper = new StageAlwaysOnTopKeeper(name, this.stageForDock, app.namedThreadSource.namedThreadFactory(name), 1, SECONDS);
        this.onTopKeeper.startWork();

        this.contextMenuDock = new ContextMenuForDock(this);
        this.contextMenuDock.setAutoHide(true);

        this.dock.setOnMousePressed(event -> {
            if ( event.isPrimaryButtonDown() ) {
                this.contextMenuDock.hide();
            }
            else if ( event.isSecondaryButtonDown() ) {
                this.contextMenuDock.show(this.dock, event.getScreenX(), event.getScreenY());
            }
        });

        this.contextMenuFold = new ContextMenuForFold(this);
        this.contextMenuFold.setAutoHide(true);

        this.fold.setOnMousePressed(event -> {
            if ( event.isPrimaryButtonDown() ) {
                this.contextMenuFold.hide();
            }
            else if ( event.isSecondaryButtonDown() ) {
                this.fold.requestFocus();
                this.contextMenuFold.requestFocus();
                this.contextMenuFold.show(this.dock, event.getScreenX(), event.getScreenY());
            }
        });

        this.configure();
    }

    private void configure() {
        this.sceneForDock.setFill(Color.TRANSPARENT);
        this.sceneForDock.getStylesheets().add("file:./jdock-style.css");

        this.sceneForFold.setFill(Color.TRANSPARENT);
        this.sceneForFold.getStylesheets().add("file:./jdock-style.css");

        final ConfigJson config = this.app.config.get();

        List<Label> icons = new ArrayList<>();
        ItemJson[] items = config.getDocks().get(this.position);
        ItemIcon icon;
        Item item;
        for ( int i = 0; i < items.length; i++ )  {
            item = new Item(this.position, i, items[i]);
            icon = new ItemIcon(this, item, this::onInvocation);
            icons.add(icon.iconLabel);
        }

        this.setFoldThick(config.getSettings().getFoldThick());

        this.dock.getChildren().addAll(icons);
        this.stageForDock.setScene(sceneForDock);
        this.stageForDock.sizeToScene();

        this.stageForFold.setScene(sceneForFold);
        this.stageForFold.sizeToScene();

        this.stageForDock.show();
        this.stageForFold.show();

        this.position.assignHiddenXY(this.stageForDock);
        this.position.assignShownXY(this.stageForFold);

        this.move = config.getSettings().getMove();
        if ( isNull(this.move) ) {
            this.move = SMOOTH;
        }

        double showTime = config.getSettings().getShowTime();
        if ( showTime == 0.0 ) {
            showTime = DEFAULT_SHOW_TIME;
        }
        this.showSmoothly = this.position.createShowingAnimation(
                stageForDock,
                showTime,
                () -> {

                });

        double hideTime = config.getSettings().getHideTime();
        if ( hideTime == 0.0 ) {
            hideTime = DEFAULT_HIDE_TIME;
        }
        this.hideSmoothly = this.position.createHidingAnimation(
                stageForDock,
                hideTime,
                () -> {
                    this.setFoldThick(this.app.config.get().getSettings().getFoldThick());
                    this.fold.setVisible(true);
                    this.dock.setVisible(false);
                    Platform.requestNextPulse();
                });
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
        this.stageForDock.hide();
        this.stageForFold.hide();
        this.sceneForDock.getStylesheets().remove("file:./jdock-style.css");
        this.sceneForFold.getStylesheets().remove("file:./jdock-style.css");
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

    private void foldTouched(MouseEvent event) {
        if ( this.app.isFullScreenModeOff() ) {
            this.session.touch();
        }
    }

    public void fullScreenModeOn() {
        this.stageForFold.setAlwaysOnTop(false);
    }

    public void fullScreenModeOff() {
        this.stageForFold.setAlwaysOnTop(true);
    }

    private void onInvocation(ItemIcon itemIcon) {
        this.contextMenuDock.hide();
        this.contextMenuFold.hide();
        try {
            String target = itemIcon.item.target;
            FileInvoker.Invocation invocation = this.app.fileInvoker.invoke(target);
            if ( invocation.fail ) {
                log.info(invocation.name() + " " + target);
            }
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
        this.fold.setVisible(false);
        this.setFoldThick(0);
        this.contextMenuFold.hide();
        Platform.requestNextPulse();
        this.dock.setVisible(true);
        this.stageForDock.sizeToScene();

        if ( this.move == SMOOTH ) {
            this.showSmoothly.playFromStart();
        }
        else {
            this.position.assignShownXY(this.stageForDock);
        }

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
        this.fold.setMinWidth(i);
        this.fold.setMaxWidth(i);
    }

    private void setFoldHeight(int i) {
        this.fold.setMinHeight(i);
        this.fold.setMaxHeight(i);
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
        this.contextMenuDock.hide();
        this.stageForDock.sizeToScene();

        if ( this.move == SMOOTH ) {
            this.hideSmoothly.playFromStart();
        }
        else {
            this.position.assignHiddenXY(this.stageForDock);
        }
    }

    private boolean canFinishSession() {
        double x = stageForDock.getX();
        double y = stageForDock.getY();
        double x2 = x + stageForDock.getWidth();
        double y2 = y + stageForDock.getHeight();
        Point mouse = MouseInfo.getPointerInfo().getLocation();
        double mX = mouse.getX();
        double mY = mouse.getY();

        boolean isHover =
                x <= mX && mX <= x2
                &&
                y <= mY && mY <= y2;

        return ! isHover;
    }

}
