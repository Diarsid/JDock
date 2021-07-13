package diarsid.jdock.json;

import java.util.HashMap;
import java.util.Map;

import diarsid.jdock.model.DockPosition;

import static diarsid.jdock.model.DockPosition.BOTTOM;
import static diarsid.jdock.model.DockPosition.LEFT;
import static diarsid.jdock.model.DockPosition.RIGHT;
import static diarsid.jdock.model.DockPosition.TOP;

public class DocksJson {

    private ItemJson[] top;
    private ItemJson[] right;
    private ItemJson[] left;
    private ItemJson[] bottom;

    public ItemJson[] getTop() {
        return top;
    }

    void setTop(ItemJson[] top) {
        this.top = top;
    }

    public ItemJson[] getRight() {
        return right;
    }

    void setRight(ItemJson[] right) {
        this.right = right;
    }

    public ItemJson[] getLeft() {
        return left;
    }

    void setLeft(ItemJson[] left) {
        this.left = left;
    }

    public ItemJson[] getBottom() {
        return bottom;
    }

    void setBottom(ItemJson[] bottom) {
        this.bottom = bottom;
    }

    public ItemJson[] get(DockPosition position) {
        switch ( position ) {
            case TOP: return top;
            case RIGHT: return right;
            case BOTTOM: return bottom;
            case LEFT: return left;
            default: throw position.unsupported();
        }
    }

    public Map<DockPosition, ItemJson[]> all() {
        return Map.of(
                TOP, top,
                RIGHT, right,
                BOTTOM, bottom,
                LEFT, left);
    }

    public Map<DockPosition, ItemJson[]> allNonEmpty() {
        Map<DockPosition, ItemJson[]> nonEmpty = new HashMap<>();

        all()
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue().length > 0)
                .forEach(entry -> nonEmpty.put(entry.getKey(), entry.getValue()));

        return nonEmpty;
    }
}
