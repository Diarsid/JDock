package diarsid.jdock.json;

public class SettingsJson {

    int iconSize;
    int foldThick;
    double iconHoverBrighter;
    double iconPressDarker;

    public double getIconSize() {
        return iconSize;
    }

    void setIconSize(int iconSize) {
        this.iconSize = iconSize;
    }

    public int getFoldThick() {
        return foldThick;
    }

    void setFoldThick(int foldThick) {
        this.foldThick = foldThick;
    }

    public double getIconHoverBrighter() {
        return iconHoverBrighter;
    }

    void setIconHoverBrighter(double iconHoverBrighter) {
        this.iconHoverBrighter = iconHoverBrighter;
    }

    public double getIconPressDarker() {
        return iconPressDarker;
    }

    void setIconPressDarker(double iconPressDarker) {
        this.iconPressDarker = iconPressDarker;
    }
}
