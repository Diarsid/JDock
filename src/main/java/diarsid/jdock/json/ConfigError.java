package diarsid.jdock.json;

public class ConfigError {

    public final String message;
    public final Exception cause;

    public ConfigError(Exception e) {
        this.message = e.getMessage();
        this.cause = e;
    }
}
