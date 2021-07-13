package diarsid.jdock.app;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import javafx.application.Platform;

import diarsid.jdock.jfx.Dock;
import diarsid.jdock.json.ConfigError;
import diarsid.jdock.json.ConfigJson;
import diarsid.jdock.json.ConfigJsonReader;
import diarsid.jdock.model.DockPosition;
import diarsid.support.concurrency.threads.NamedThreadSource;
import diarsid.support.filesystem.FileInvoker;
import diarsid.support.javafx.FilesNativeIconImageExtractor;
import diarsid.support.javafx.HiddenStages;
import diarsid.support.javafx.PlatformStartup;
import diarsid.support.objects.CommonEnum;
import diarsid.support.objects.Either;
import diarsid.support.objects.references.Possible;
import diarsid.support.objects.references.PresentProperty;
import diarsid.support.objects.references.References;

import static java.util.concurrent.CompletableFuture.runAsync;

import static diarsid.jdock.app.DockApp.ExitBehavior.SHUTDOWN_JAVA_PROCESS;
import static diarsid.jdock.model.DockPosition.values;
import static diarsid.support.objects.Either.Side.LEFT;

public class DockApp {

    public static enum ExitBehavior implements CommonEnum<ExitBehavior> {
        SHUTDOWN_JAVA_PROCESS,
        HIDE_JAVAFX_COMPONENTS
    }

    public final HiddenStages hiddenStages;
    private final Map<DockPosition, Possible<Dock>> docks;
    private final ConfigJsonReader configReader;
    public final NamedThreadSource namedThreadSource;
    public final FileInvoker fileInvoker;
    public final FilesNativeIconImageExtractor imageExtractor;
    public final PresentProperty<ConfigJson> config;
    public final PresentProperty<ExitBehavior> exitBehavior;

    public DockApp(ConfigJsonReader configReader) {
        PlatformStartup.await();
        this.configReader = configReader;
        this.config = References.presentPropertyOf(this.configReader.get().left, "Config");
        this.exitBehavior = References.presentPropertyOf(SHUTDOWN_JAVA_PROCESS, "Exit_behavior");
        this.hiddenStages = new HiddenStages();
        this.docks = new HashMap<>();
        this.namedThreadSource = new NamedThreadSource("diarsid.jdock");
        this.fileInvoker = new FileInvoker();
        this.imageExtractor = new FilesNativeIconImageExtractor();

        for ( DockPosition position : values() ) {
            this.docks.put(position, References.simplePossibleButEmpty());
        }

        this.createDocks();

        this.config.listen((oldConfig, newConfig) -> {
            this.reconfigureDocks();
        });

        Platform.setImplicitExit(false);
    }

    private void createDocks() {
        var nonEmptyDocs = this.config.get().getDocks().allNonEmpty();
        if ( nonEmptyDocs.isEmpty() ) {
            throw new IllegalStateException("There are no filled docks!");
        }

        Platform.runLater(() -> {
            this.config.get().getDocks().allNonEmpty().entrySet().forEach(positionAndItems -> {
                DockPosition position = positionAndItems.getKey();
                Dock newDock = new Dock(position, this);
                this.docks.get(position).resetTo(newDock);
            });
        });
    }

    private void reconfigureDocks() {
        Platform.runLater(() -> {
            this.docks
                    .values()
                    .stream()
                    .map(dockPossible -> dockPossible.or(null))
                    .filter(Objects::nonNull)
                    .forEach(Dock::reconfigure);

            this.config
                    .get()
                    .getDocks()
                    .allNonEmpty()
                    .forEach((position, itemJsons) -> {
                        Possible<Dock> dock = this.docks.get(position);
                        if ( dock.isNotPresent() ) {
                            Dock newDock = new Dock(position, this);
                            dock.resetTo(newDock);
                        }
                    });
        });
    }

    public void exit(ExitBehavior exitBehavior) {
        switch ( exitBehavior ) {
            case SHUTDOWN_JAVA_PROCESS:
                this.shutdown();
                break;
            case HIDE_JAVAFX_COMPONENTS:
                this.hide();
                break;
            default:
                throw exitBehavior.unsupported();

        }
        namedThreadSource.closeThreads();
    }

    public void exit() {
        this.exit(this.exitBehavior.get());
    }

    private void shutdown() {
        System.exit(0);
    }

    private void hide() {
        CountDownLatch hiding = new CountDownLatch(1);

        Platform.runLater(() -> {
            this.docks
                    .values()
                    .stream()
                    .map(dockPossible -> dockPossible.or(null))
                    .filter(Objects::nonNull)
                    .forEach(Dock::deactivate);
            hiding.countDown();
        });

        try {
            hiding.await();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void reload() {
        runAsync(() -> {
            Either<ConfigJson, ConfigError> configOrError = this.configReader.get();
            if ( configOrError.side.equalTo(LEFT) ) {
                this.config.resetTo(configOrError.left);
            }
            else {
                System.out.println(configOrError.right.message);
            }
        });
    }

//    public void create(DockPosition position, DockSettings settings) {
//        Platform.runLater(() -> this.docks.get(position).ifNotPresentResetTo(new Dock(position, settings, this)));
//    }
}
