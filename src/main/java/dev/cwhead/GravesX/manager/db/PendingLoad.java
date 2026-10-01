package dev.cwhead.GravesX.manager.db;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/**
 * Tracks a database load, including tasks queued by other loading tasks.
 */
public final class PendingLoad {
    private final CompletableFuture<Void> completion = new CompletableFuture<>();
    private int pending = 1;
    private Throwable failure;

    public CompletionStage<Void> completion() {
        return completion.minimalCompletionStage();
    }

    public synchronized boolean isDone() {
        return pending == 0;
    }

    public synchronized void fail(Throwable error) {
        if (failure == null)
            failure = error;
    }

    /**
     *  Registers before dispatch so even immediate or nested execution is accounted for.
     */
    public void submit(Consumer<Runnable> scheduler, Runnable task) {
        synchronized (this) {
            if (pending == 0)
                throw new IllegalStateException("Database load already finished");

            pending++;
        }
        try {
            scheduler.accept(() -> {
                try {
                    task.run();
                } catch (Throwable error) {
                    fail(error);
                } finally {
                    finish();
                }
            });
        } catch (Throwable error) {
            fail(error);
            finish();
        }
    }

    /**
     * Releases one reservation; failures are reported only once all work has finished.
     */
    public void finish() {
        Throwable error;

        synchronized (this) {
            if (pending <= 0)
                throw new IllegalStateException("Unbalanced load completion");
            if (--pending != 0)
                return;
            error = failure;
        }

        if (error == null) {
            completion.complete(null);
        } else {
            completion.completeExceptionally(error);
        }
    }
}
