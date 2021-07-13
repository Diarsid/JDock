package diarsid.jdock.jfx;

import javafx.stage.Screen;

public class Util {

    public static double screenWidth() {
        return Screen.getPrimary().getBounds().getWidth();
    }

    public static double screenHeight() {
        return Screen.getPrimary().getBounds().getHeight();
    }
}
