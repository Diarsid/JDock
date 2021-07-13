package diarsid.jdock.json;

public class ConfigJson {

    private SettingsJson settings;
    private DocksJson docks;

    public SettingsJson getSettings() {
        return settings;
    }

    void setSettings(SettingsJson settings) {
        this.settings = settings;
    }

    public DocksJson getDocks() {
        return docks;
    }

    void setDocks(DocksJson docks) {
        this.docks = docks;
    }
}
