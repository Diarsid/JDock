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

import diarsid.support.javafx.FilesNativeIconImageExtractor;
import diarsid.support.objects.references.Possible;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import static diarsid.support.objects.references.References.simplePossibleButEmpty;

public final class ItemIcon {

    private final ImageView icon;
    private final ColorAdjust brighter;
    private final ColorAdjust darker;
    private final Possible<Effect> cssEffect;
    private boolean isHovered;
    public final Dock dock;
    public final Item item;
    public final Label iconLabel;
    public final transient List<Process> process;
    public final Consumer<ItemIcon> invocationCallback;

    public ItemIcon(Dock dock, Item item, Consumer<ItemIcon> invocationCallback) {
        this.dock = dock;
        this.item = item;
        Image image;
        if ( isNull(item.image) ) {
            image = dock.app.imageExtractor.getFrom(
                    new File(item.target),
                    FilesNativeIconImageExtractor.PathCache.USE,
                    FilesNativeIconImageExtractor.ExtensionCache.NO_USE);
        }
        else {
            image = new Image("file:" + this.item.image.toString(), false);
        }
        this.brighter = new ColorAdjust();
        this.darker = new ColorAdjust();
        this.brighter.setBrightness(dock.app.config.get().getSettings().getIconHoverBrighter());
        this.darker.setBrightness(dock.app.config.get().getSettings().getIconPressDarker());

        this.icon = new ImageView();
        double iconSize = dock.app.config.get().getSettings().getIconSize();
        this.icon.setFitHeight(iconSize);
        this.icon.setFitHeight(iconSize);
        this.icon.setPreserveRatio(true);
        this.icon.setImage(image);
        this.icon.getStyleClass().add("icon");
        this.iconLabel = new Label();
        this.iconLabel.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        this.iconLabel.setGraphic(this.icon);
        this.iconLabel.setTooltip(new Tooltip(this.item.name));

        this.cssEffect = simplePossibleButEmpty();
        this.isHovered = false;

        this.iconLabel.hoverProperty().addListener(((observable, oldValue, newValue) -> {
            this.getInitialEffectAtFirstRun();
            if ( (! oldValue) && newValue ) {
                this.icon.setEffect(brighter);
            }
            else {
                this.icon.setEffect(this.cssEffect.or(null));
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

    private synchronized void getInitialEffectAtFirstRun() {
        if ( ! this.isHovered ) {
            this.isHovered = true;
            Effect initialEffect =  this.icon.getEffect();
            if ( nonNull(initialEffect) ) {
                this.cssEffect.resetTo(initialEffect);
                this.brighter.setInput(initialEffect);
                this.darker.setInput(initialEffect);
            }
        }
    }
}
