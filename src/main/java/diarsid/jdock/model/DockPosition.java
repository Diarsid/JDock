package diarsid.jdock.model;

import java.util.List;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.stage.Stage;
import javafx.util.Duration;

import diarsid.jdock.jfx.Item;
import diarsid.support.objects.CommonEnum;

import static diarsid.jdock.jfx.Util.screenHeight;
import static diarsid.jdock.jfx.Util.screenWidth;
import static diarsid.jdock.model.DockOrientation.HORIZONTAL;
import static diarsid.jdock.model.DockOrientation.VERTICAL;
import static diarsid.jdock.model.DockPosition.MoveDirection.HIDING;
import static diarsid.jdock.model.DockPosition.MoveDirection.SHOWING;

public enum DockPosition implements CommonEnum<DockPosition> {

    TOP(
            /* shown X */ (stage) -> (screenWidth() / 2 - stage.widthProperty().get() / 2),
            /* shown Y */ (stage) -> 0,
            /* hidden X */ (stage) -> (screenWidth() / 2 - stage.widthProperty().get() / 2),
            /* hidden Y */ (stage) -> ( - stage.heightProperty().get()),
            HORIZONTAL),

    RIGHT(
            /* shown X */ (stage) -> (screenWidth() - stage.widthProperty().get()),
            /* shown Y */ (stage) -> (screenHeight() / 2 - stage.heightProperty().get() / 2),
            /* hidden X */ (stage) -> (screenWidth()),
            /* hidden Y */ (stage) -> (screenHeight() / 2 - stage.heightProperty().get() / 2),
            VERTICAL),

    BOTTOM(
            /* shown X */ (stage) -> (screenWidth() / 2 - stage.widthProperty().get() / 2),
            /* shown Y */ (stage) -> (screenHeight() - stage.heightProperty().get()),
            /* hidden X */ (stage) -> (screenWidth() / 2 - stage.widthProperty().get() / 2),
            /* hidden Y */ (stage) -> (screenHeight() + stage.heightProperty().get()),
            HORIZONTAL),

    LEFT(
            /* shown X */ (stage) -> (0),
            /* shown Y */ (stage) -> (screenHeight() / 2 - stage.heightProperty().get() / 2),
            /* hidden X */ (stage) -> ( - stage.widthProperty().get()),
            /* hidden Y */ (stage) -> (screenHeight() / 2 - stage.heightProperty().get() / 2),
            VERTICAL);

    private interface AxisValueGetter {

        double getOf(Stage stage);
    }

    private interface AxisValueChange {

        void accept(double axisValue);
    }

    enum MoveDirection {
        HIDING, SHOWING
    }

    private final AxisValueGetter shownX;
    private final AxisValueGetter shownY;
    private final AxisValueGetter hiddenX;
    private final AxisValueGetter hiddenY;

    public final DockOrientation dockOrientation;

    DockPosition(
            AxisValueGetter shownX,
            AxisValueGetter shownY,
            AxisValueGetter hiddenX,
            AxisValueGetter hiddenY,
            DockOrientation dockOrientation) {
        this.shownX = shownX;
        this.shownY = shownY;
        this.hiddenX = hiddenX;
        this.hiddenY = hiddenY;
        this.dockOrientation = dockOrientation;
    }

    public double getShownXFor(Stage stage) {
        return this.shownX.getOf(stage);
    }

    public double getShownYFor(Stage stage) {
        return this.shownY.getOf(stage);
    }

    public double getHiddenXFor(Stage stage) {
        return this.hiddenX.getOf(stage);
    }

    public double getHiddenYFor(Stage stage) {
        return this.hiddenY.getOf(stage);
    }

    public void assignShownXY(Stage stage) {
        stage.setX(this.shownX.getOf(stage));
        stage.setY(this.shownY.getOf(stage));
    }

    public void assignHiddenXY(Stage stage) {
        stage.setX(this.hiddenX.getOf(stage));
        stage.setY(this.hiddenY.getOf(stage));
    }

    public Animation createShowingAnimation(Stage stage, double seconds, Runnable onFinished) {
        return createAnimationFor(stage, SHOWING, seconds, onFinished);
    }

    public Animation createHidingAnimation(Stage stage, double seconds, Runnable onFinished) {
        return createAnimationFor(stage, HIDING, seconds, onFinished);
    }

    private Animation createAnimationFor(
            Stage stage,
            MoveDirection direction,
            double seconds,
            Runnable onFinished) {
        Timeline showing = new Timeline();
        Duration duration = Duration.seconds(seconds);

        double axisInitialValue;
        double axisTargetValue;
        AxisValueChange axisValueChange;
        if ( this.dockOrientation.equalTo(VERTICAL) ) {
            axisValueChange = changedX -> stage.setX(changedX);

            if ( direction == SHOWING ) {
                axisInitialValue = this.hiddenX.getOf(stage);
                axisTargetValue = this.shownX.getOf(stage);
            }
            else {
                axisInitialValue = this.shownX.getOf(stage);
                axisTargetValue = this.hiddenX.getOf(stage);
            }
        }
        else {
            axisValueChange = changedY -> stage.setY(changedY);

            if ( direction == SHOWING ) {
                axisInitialValue = this.hiddenY.getOf(stage);
                axisTargetValue = this.shownY.getOf(stage);
            }
            else {
                axisInitialValue = this.shownY.getOf(stage);
                axisTargetValue = this.hiddenY.getOf(stage);
            }
        }

        Interpolator interpolator;
        if ( direction == SHOWING ) {
            interpolator = Interpolator.EASE_IN;
        }
        else {
            interpolator = Interpolator.EASE_OUT;
        }

        DoubleProperty xProperty = new SimpleDoubleProperty(axisInitialValue);

        ChangeListener<? super Number> xPropertyChange = (prop, oldV, newV) -> {
            axisValueChange.accept(newV.doubleValue());
        };

        xProperty.addListener(xPropertyChange);

        KeyValue key = new KeyValue(xProperty, axisTargetValue, interpolator);
        KeyFrame finalPosition = new KeyFrame(duration, key);
        showing.getKeyFrames().add(finalPosition);

        showing.setOnFinished(event -> {
            xProperty.removeListener(xPropertyChange);
            xProperty.set(axisInitialValue);
            xProperty.addListener(xPropertyChange);
            onFinished.run();
        });

        return showing;
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
