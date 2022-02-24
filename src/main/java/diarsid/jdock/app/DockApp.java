package diarsid.jdock.app;

import java.security.cert.Extension;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import javafx.application.Platform;

import diarsid.files.Extensions;
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
    private final AtomicBoolean fullScreenMode;
    public final NamedThreadSource namedThreadSource;
    public final FileInvoker fileInvoker;
    public final FilesNativeIconImageExtractor imageExtractor;
    public final PresentProperty<ConfigJson> config;
    public final PresentProperty<ExitBehavior> exitBehavior;

    public DockApp(ConfigJsonReader configReader) {
        PlatformStartup.await();
        this.configReader = configReader;
        this.fullScreenMode = new AtomicBoolean(false);
        this.config = References.presentPropertyOf(
                this.configReader
                        .get()
                        .leftOrThrow(error -> new IllegalStateException(error.message)),
                "Config");
        this.exitBehavior = References.presentPropertyOf(SHUTDOWN_JAVA_PROCESS, "Exit_behavior");
        this.hiddenStages = new HiddenStages();
        this.docks = new HashMap<>();
        this.namedThreadSource = new NamedThreadSource("diarsid.jdock");
        this.fileInvoker = new FileInvoker();
        Extensions extensions = new Extensions();
        this.imageExtractor = new FilesNativeIconImageExtractor(extensions);

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
            this.allDocks().forEach(Dock::reconfigure);

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

    private Stream<Dock> allDocks() {
        return this.docks
                .values()
                .stream()
                .filter(Possible::isPresent)
                .map(Possible::orThrow);
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

    public void toggleFullScreen() {
        boolean prevFullScreenMode;
        boolean nextFullScreenMode;
        synchronized ( this.fullScreenMode ) {
            prevFullScreenMode= this.fullScreenMode.get();
            nextFullScreenMode = ! prevFullScreenMode;
            this.fullScreenMode.set(nextFullScreenMode);
        }
        if ( nextFullScreenMode ) {
            this.allDocks().forEach(Dock::fullScreenModeOn);
        }
        else {
            this.allDocks().forEach(Dock::fullScreenModeOff);
        }
    }

    public boolean isFullScreenModeOn() {
        return this.fullScreenMode.get();
    }

    public boolean isFullScreenModeOff() {
        return ! this.fullScreenMode.get();
    }

}
