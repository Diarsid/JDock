package diarsid.jdock.jfx;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

import diarsid.jdock.json.ItemJson;
import diarsid.jdock.model.DockPosition;

import static java.util.Objects.isNull;

public final class Item implements Comparable<Item> {

//    public static final class Changing {
//        final DockPosition position;
//        int index;
//        Path image;
//        String name;
//        String target;
//        Type type;
//
//        private Changing(Item item) {
//            this.position = item.position;
//            this.index = item.index;
//            this.image = item.image;
//            this.name = item.name;
//            this.target = item.target;
//            this.type = item.type;
//        }
//
//        public Item done() {
//            return new Item(position, index, image, name, target, type);
//        }
//
//        public Changing index(int index) {
//            this.index = index;
//            return this;
//        }
//
//        public Changing image(Path image) {
//            this.image = image;
//            return this;
//        }
//
//        public Changing name(String name) {
//            this.name = name;
//            return this;
//        }
//
//        public Changing target(String target) {
//            this.target = target;
//            return this;
//        }
//    }

    public final DockPosition position;
    public final int index;
    public final Path image;
    public final String name;
    public final String target;

    private Item(DockPosition position, int index, Path image, String name, String target) {
        validateIndex(index);
        this.position = position;
        this.index = index;
        this.image = image;
        this.name = name;
        this.target = target;
    }

    public Item(DockPosition position, int index, ItemJson json) {
        validateIndex(index);
        this.position = position;
        this.index = index;
        String iconPath = json.getIcon();
        if ( isNull(iconPath) ) {
            this.image = null;
        }
        else {
            this.image = Paths.get(iconPath);
        }
        this.name = json.getName();
        this.target = json.getTarget();
    }

    @Override
    public int compareTo(Item other) {
        if ( this.index > other.index ) {
            return 1;
        }
        else if ( this.index < other.index ) {
            return -1;
        }
        else {
            return 0;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Item)) return false;
        Item item = (Item) o;
        return index == item.index &&
                position == item.position &&
                image.equals(item.image) &&
                name.equals(item.name) &&
                target.equals(item.target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(position, index, image, name, target);
    }

    @Override
    public String toString() {
        return "Item{" +
                "position=" + position +
                ", index=" + index +
                ", image=" + image +
                ", name='" + name + '\'' +
                ", target='" + target + '\'' +
                '}';
    }

//    public Changing change() {
//        return new Changing(this);
//    }
//
//    public void mustHaveIndex(int otherIndex) {
//        validateIndex(otherIndex);
//    }

//    public static List<List> sqlArgsOf(List<Item> items) {
//        return items.stream().map(Item::sqlArgs).collect(toList());
//    }
    
    public List sqlArgs() {
        return List.of(position, index, image.toString(), name, target);
    }

    private static void validateIndex(int index) {
        if ( index < 0 ) {
            throw new IllegalArgumentException();
        }
    }
}
