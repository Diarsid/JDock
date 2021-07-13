package diarsid.jdock.model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import javafx.application.Platform;

import diarsid.support.concurrency.threads.NamedThreadSource;
import diarsid.support.objects.references.Possible;

import static java.util.Objects.nonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import static diarsid.support.concurrency.threads.ThreadsUtil.shutdownAndWait;
import static diarsid.support.objects.references.References.simplePossibleButEmpty;

public class DockSession {

    private final Runnable onActivation;
    private final Runnable onDeactivation;
    private final Supplier<Boolean> canDeactivate;
    private final ReadWriteLock lock;
    private final Possible<Future> deactivation;
    private final List<String> blocks;
    private final ScheduledExecutorService async;

    public DockSession(
            DockPosition position,
            NamedThreadSource namedThreadSource,
            Runnable onActivation,
            Runnable onDeactivation,
            Supplier<Boolean> canDeactivate) {
        this.onActivation = onActivation;
        this.onDeactivation = onDeactivation;
        this.canDeactivate = canDeactivate;
        this.lock = new ReentrantReadWriteLock();
        this.deactivation = simplePossibleButEmpty();
        this.blocks = new ArrayList<>();
        this.async = namedThreadSource.newNamedScheduledExecutorService("dock-session-" + position.name().toLowerCase(), 1);
    }

    public boolean isActive() {
        lock.readLock().lock();
        try {
            return deactivation.isPresent();
        }
        finally {
            lock.readLock().unlock();
        }
    }

    public void touch() {
        lock.writeLock().lock();
        try {
            if ( blocks.isEmpty() ) {
                doTouch();
            }
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    private void doTouch() {
        if ( deactivation.isPresent() ) {
            prolongActivity();
        }
        else {
            activate();
        }
    }

    public void block(String name) {
        lock.writeLock().lock();
        try {
            Future oldDeactivation = deactivation.orThrow();
            oldDeactivation.cancel(true);
            blocks.add(name);
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    public void unblock(String name) {
        lock.writeLock().lock();
        try {
            boolean removed = blocks.remove(name);
            if ( ! removed ) {
                throw new IllegalArgumentException();
            }
            if ( blocks.isEmpty() ) {
                doTouch();
            }
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    public void dispose() {
        lock.writeLock().lock();
        try {
            deactivation.ifPresent(future -> future.cancel(true));
            shutdownAndWait(async);
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    private void activate() {
        onActivation.run();
        Future newDeactivation = async.schedule(this::tryDeactivate, 300, MILLISECONDS);
        Future oldDeactivation = deactivation.resetTo(newDeactivation);
        if ( nonNull(oldDeactivation) ) {
            throw new IllegalStateException();
        }
    }

    private void prolongActivity() {
        Future oldDeactivation = deactivation.resetTo(async.schedule(this::tryDeactivate, 300, MILLISECONDS));
        if ( nonNull(oldDeactivation) ) {
            oldDeactivation.cancel(true);
        }
    }

    private void tryDeactivate() {
        lock.writeLock().lock();
        try {
            boolean canDeactivate = this.canDeactivate.get();
            if ( canDeactivate ) {
                System.out.println("session : finish");
                Platform.runLater(this.onDeactivation);
                deactivation.nullify();
            }
            else {
                System.out.println("session : prolong");
                deactivation.resetTo(async.schedule(this::tryDeactivate, 300, MILLISECONDS));
            }
        }
        finally {
            lock.writeLock().unlock();
        }
    }
}
