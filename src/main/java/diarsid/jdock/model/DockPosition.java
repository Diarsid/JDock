package diarsid.jdock.model;

import java.util.List;
import java.util.function.Consumer;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import diarsid.jdock.jfx.Item;
import diarsid.support.objects.CommonEnum;

import static diarsid.jdock.jfx.Util.screenHeight;
import static diarsid.jdock.jfx.Util.screenWidth;
import static diarsid.jdock.model.DockOrientation.HORIZONTAL;
import static diarsid.jdock.model.DockOrientation.VERTICAL;

public enum DockPosition implements CommonEnum<DockPosition> {

    TOP(
            (box, dock, fold) -> {
                box.getChildren().addAll(fold, dock);
            },
            (stage) -> {
                stage.setX(screenWidth() / 2 - stage.widthProperty().get() / 2);
                stage.setY(0);
            },
            HORIZONTAL),

    RIGHT(
            (box, dock, fold) -> {
                box.getChildren().addAll(dock, fold);
            },
            (stage) -> {
                stage.setX(screenWidth() - stage.widthProperty().get());
                stage.setY(screenHeight() / 2 - stage.heightProperty().get() / 2);
            },
            VERTICAL),

    BOTTOM(
            (box, dock, fold) -> {
                box.getChildren().addAll(dock, fold);
            },
            (stage) -> {
                stage.setX(screenWidth() / 2 - stage.widthProperty().get() / 2);
                stage.setY(screenHeight() - stage.heightProperty().get());
            },
            HORIZONTAL),

    LEFT(
            (box, dock, fold) -> {
                box.getChildren().addAll(fold, dock);
            },
            (stage) -> {
                stage.setX(0);
                stage.setY(screenHeight() / 2 - stage.heightProperty().get() / 2);
            },
            VERTICAL);

    static interface Arrangement {
        void arrange(Pane box, HBox dockPadding, Label dockFold);
    }

    private final Consumer<Stage> stageXYAssignment;
    private final Arrangement arrangement;
    public final DockOrientation dockOrientation;

    DockPosition(
            Arrangement arrangement,
            Consumer<Stage> stageXYAssignment,
            DockOrientation dockOrientation) {
        this.stageXYAssignment = stageXYAssignment;
        this.arrangement = arrangement;
        this.dockOrientation = dockOrientation;
    }

    public void assignXY(Stage stage) {
        this.stageXYAssignment.accept(stage);
    }

    public void arrangeInCorrectOrder(Pane box, HBox dockPadding, Label dockFold) {
        this.arrangement.arrange(box, dockPadding, dockFold);
    }

    public void mustBeParentOfAll(Item item) {
        if ( this != item.position ) {
            throw new IllegalArgumentException();
        }
    }

    public void mustBeParentOfAll(List<Item> items) {
        for ( Item item : items ) {
            this.mustBeParentOfAll(item);
        }
    }

}
