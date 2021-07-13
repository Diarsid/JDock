package diarsid.jdock.jfx;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.Effect;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;

import diarsid.jdock.app.Main;

import static java.util.Objects.isNull;

public final class ItemIcon {

    public final Dock dock;
    public final Item item;
    private final ImageView icon;
    public final Label iconLabel;
    public final transient List<Process> process;
    public final Consumer<ItemIcon> invocationCallback;

    public ItemIcon(Dock dock, Item item, Consumer<ItemIcon> invocationCallback) {
        this.dock = dock;
        this.item = item;
        Image image;
        if ( isNull(item.image) ) {
            image = dock.app.imageExtractor.getFrom(new File(item.target));
        }
        else {
            image = new Image("file:" + this.item.image.toString(), false);
        }
        ColorAdjust brighter = new ColorAdjust();
        ColorAdjust darker = new ColorAdjust();
        brighter.setBrightness(dock.app.config.get().getSettings().getIconHoverBrighter());
        darker.setBrightness(dock.app.config.get().getSettings().getIconPressDarker());
        this.icon = new ImageView();
        double iconSize = dock.app.config.get().getSettings().getIconSize();
        this.icon.setFitHeight(iconSize);
        this.icon.setFitHeight(iconSize);
        this.icon.setPreserveRatio(true);
        this.icon.setImage(image);
        this.iconLabel = new Label();
        this.iconLabel.getStyleClass().add("dock-icon");
        this.iconLabel.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        this.iconLabel.setGraphic(this.icon);
        this.iconLabel.setTooltip(new Tooltip(this.item.name));

        this.iconLabel.hoverProperty().addListener(((observable, oldValue, newValue) -> {
            if ( (! oldValue) && newValue ) {
                this.icon.setEffect(brighter);
            }
            else {
                this.icon.setEffect(null);
            }
        }));

        this.iconLabel.setOnMouseMoved(event -> {
            Effect effect = this.icon.getEffect();
            if ( isNull(effect) ) {
                this.icon.setEffect(brighter);
            }
        });

        this.iconLabel.setOnMousePressed(event -> {
            this.icon.setEffect(darker);
            this.click(event);
        });
        this.iconLabel.setOnMouseReleased(event -> {
            this.icon.setEffect(brighter);
        });

        this.process = new ArrayList<>();
        this.invocationCallback = invocationCallback;
    }

    void click(MouseEvent event) {
        if ( event.isPrimaryButtonDown() ) {
            this.invocationCallback.accept(this);
        }
        event.consume();
    }
}
