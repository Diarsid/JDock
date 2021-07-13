package diarsid.jdock.app;

import diarsid.jdock.json.ConfigJsonReader;

public class Main {

    public static void main(String[] args) throws Exception {
        ConfigJsonReader configJsonReader = new ConfigJsonReader("./jdock-config.json");
        DockApp dockApp = new DockApp(configJsonReader);
    }
}
