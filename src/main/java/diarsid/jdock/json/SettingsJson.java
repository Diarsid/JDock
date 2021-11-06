package diarsid.jdock.json;

public class SettingsJson {

    int iconSize;
    int foldThick;
    double iconHoverBrighter;
    double iconPressDarker;
    double showTime;
    double hideTime;
    DockMove move;

    public double getIconSize() {
        return iconSize;
    }

    public int getFoldThick() {
        return foldThick;
    }

    public double getIconHoverBrighter() {
        return iconHoverBrighter;
    }

    public double getIconPressDarker() {
        return iconPressDarker;
    }

    public double getShowTime() {
        return showTime;
    }

    public double getHideTime() {
        return hideTime;
    }

    public DockMove getMove() {
        return move;
    }

    void setIconSize(int iconSize) {
        this.iconSize = iconSize;
    }

    void setFoldThick(int foldThick) {
        this.foldThick = foldThick;
    }

    void setIconHoverBrighter(double iconHoverBrighter) {
        this.iconHoverBrighter = iconHoverBrighter;
    }

    void setIconPressDarker(double iconPressDarker) {
        this.iconPressDarker = iconPressDarker;
    }

    void setShowTime(double showTime) {
        this.showTime = showTime;
    }

    void setHideTime(double hideTime) {
        this.hideTime = hideTime;
    }

    void setMove(DockMove move) {
        this.move = move;
    }
}
