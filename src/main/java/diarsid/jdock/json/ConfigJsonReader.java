package diarsid.jdock.json;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.ObjectMapper;
import diarsid.support.objects.Either;

public class ConfigJsonReader implements Supplier<Either<ConfigJson, ConfigError>> {

    private final ObjectMapper mapper;
    private final File configJsonFile;

    public ConfigJsonReader(String configFilePath) {
        this.mapper = new ObjectMapper();
        Path configFile = Paths.get(configFilePath);
        if ( ! Files.exists(configFile) ) {
            throw new IllegalStateException(configFilePath + " does not exist!");
        }
        if ( ! Files.isReadable(configFile) ) {
            throw new IllegalStateException(configFilePath + " is not readable!");
        }
        this.configJsonFile = configFile.toAbsolutePath().toFile();
    }

    @Override
    public Either<ConfigJson, ConfigError> get() {
        try {
            ConfigJson config = mapper.readValue(configJsonFile, ConfigJson.class);
            return Either.leftOfEither(config);
        }
        catch (Exception e) {
            return Either.rightOfEither(new ConfigError(e));
        }
    }
}
