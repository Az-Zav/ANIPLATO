package com.aniplato.support;

import javafx.application.Platform;
import org.junit.jupiter.api.Assumptions;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/** Runs test code on the JavaFX thread. The toolkit needs a desktop; without one the test is skipped. */
public final class Fx {

    private Fx() { }

    /** Starts the toolkit once; call from @BeforeAll. */
    public static void start() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException alreadyRunning) {
            // started by an earlier test class
        } catch (Throwable noDesktop) {
            Assumptions.abort("JavaFX toolkit unavailable: " + noDesktop);
        }
    }

    public static void run(Runnable action) {
        call(() -> {
            action.run();
            return null;
        });
    }

    public static <T> T call(Supplier<T> action) {
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                result.set(action.get());
            } catch (Throwable t) {
                failure.set(t);
            } finally {
                done.countDown();
            }
        });
        try {
            if (!done.await(20, TimeUnit.SECONDS)) throw new IllegalStateException("The JavaFX thread did not answer");
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
        if (failure.get() != null) throw new IllegalStateException(failure.get());
        return result.get();
    }
}
