package dev.cwhead.GravesX.manager;

import me.croabeast.scheduler.GlobalScheduler;
import me.croabeast.scheduler.GlobalTask;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * Fire-and-forget scheduler wrapper around {@link GlobalScheduler}.
 */
public final class SchedulerManager {

    /**
     * Owning plugin instance.
     */
    private final Plugin plugin;

    /**
     * Backing scheduler implementation for the current runtime.
     */
    private final GlobalScheduler scheduler;

    /**
     * Returnable API that provides {@link Task} handles.
     */
    private final Returnable returnable;

    /**
     * Creates a scheduler manager for the given plugin.
     *
     * @param plugin owning plugin
     */
    public SchedulerManager(@NotNull Plugin plugin) {
        this.plugin = plugin;
        this.scheduler = GlobalScheduler.getScheduler(plugin);
        this.returnable = new Returnable(this, scheduler);
    }

    /**
     * Returns the backing {@link GlobalScheduler}.
     *
     * @return scheduler instance
     */
    public @NotNull GlobalScheduler getScheduler() {
        return scheduler;
    }

    /**
     * Returns the owning {@link Plugin}.
     *
     * @return plugin instance
     */
    public @NotNull Plugin getPlugin() {
        return plugin;
    }

    /**
     * Returns the handle-returning API.
     *
     * @return returnable helper
     */
    public @NotNull Returnable returnable() {
        return returnable;
    }

    /**
     * Checks if the current thread is the global scheduler thread for this runtime.
     *
     * @return true if on global thread, otherwise false
     */
    public boolean isGlobalThread() {
        return scheduler.isGlobalThread();
    }

    /**
     * Checks if the current thread is the tick/primary thread.
     *
     * @return true if on tick thread, otherwise false
     */
    public boolean isTickThread() {
        return scheduler.isTickThread();
    }

    /**
     * Checks if the current thread owns the specified entity's region.
     *
     * @param entity entity to check
     * @return true if the current thread owns the entity region, otherwise false
     */
    public boolean isEntityThread(@NotNull Entity entity) {
        return scheduler.isEntityThread(entity);
    }

    /**
     * Checks if the current thread owns the region containing the specified location.
     *
     * @param location location to check (world must be non-null)
     * @return true if the current thread owns the region, otherwise false
     */
    public boolean isRegionThread(@NotNull Location location) {
        return scheduler.isRegionThread(location);
    }

    /**
     * Calls a task on the global/main thread and returns a {@link Future}.
     *
     * @param task callable to execute
     * @param <T>  result type
     * @return future result
     */
    public <T> @NotNull Future<T> callSyncMethod(@NotNull Callable<T> task) {
        return scheduler.callSyncMethod(task);
    }

    /**
     * Schedules a task for the next tick (global/main).
     *
     * @param runnable task to run
     */
    public void runTask(@NotNull Runnable runnable) {
        scheduler.runTask(runnable);
    }

    /**
     * Schedules a task for the next tick (legacy signature).
     *
     * @param plugin   plugin (legacy parameter)
     * @param runnable task to run
     * @deprecated Use {@link #runTask(Runnable)}.
     */
    @Deprecated
    public void runTask(@NotNull Plugin plugin, @NotNull Runnable runnable) {
        scheduler.runTask(plugin, runnable);
    }

    /**
     * Schedules a delayed task (global/main).
     *
     * @param runnable task to run
     * @param delay    delay in ticks
     */
    public void runTaskLater(@NotNull Runnable runnable, long delay) {
        scheduler.runTaskLater(runnable, delay);
    }

    /**
     * Schedules a delayed task (legacy signature).
     *
     * @param plugin   plugin (legacy parameter)
     * @param runnable task to run
     * @param delay    delay in ticks
     * @deprecated Use {@link #runTaskLater(Runnable, long)}.
     */
    @Deprecated
    public void runTaskLater(@NotNull Plugin plugin, @NotNull Runnable runnable, long delay) {
        scheduler.runTaskLater(plugin, runnable, delay);
    }

    /**
     * Schedules a repeating task (global/main).
     *
     * @param runnable task to run
     * @param delay    initial delay in ticks
     * @param period   period in ticks
     */
    public void runTaskTimer(@NotNull Runnable runnable, long delay, long period) {
        scheduler.runTaskTimer(runnable, delay, period);
    }

    /**
     * Schedules a repeating task (legacy signature).
     *
     * @param plugin   plugin (legacy parameter)
     * @param runnable task to run
     * @param delay    initial delay in ticks
     * @param period   period in ticks
     * @deprecated Use {@link #runTaskTimer(Runnable, long, long)}.
     */
    @Deprecated
    public void runTaskTimer(@NotNull Plugin plugin, @NotNull Runnable runnable, long delay, long period) {
        scheduler.runTaskTimer(plugin, runnable, delay, period);
    }

    /**
     * Schedules a task on the region that owns the provided location.
     *
     * @param location location context
     * @param runnable task to run
     */
    public void runTask(@NotNull Location location, @NotNull Runnable runnable) {
        scheduler.runTask(location, runnable);
    }

    /**
     * Schedules a delayed task on the region that owns the provided location.
     *
     * @param location location context
     * @param runnable task to run
     * @param delay    delay in ticks
     */
    public void runTaskLater(@NotNull Location location, @NotNull Runnable runnable, long delay) {
        scheduler.runTaskLater(location, runnable, delay);
    }

    /**
     * Schedules a repeating task on the region that owns the provided location.
     *
     * @param location location context
     * @param runnable task to run
     * @param delay    initial delay in ticks
     * @param period   period in ticks
     */
    public void runTaskTimer(@NotNull Location location, @NotNull Runnable runnable, long delay, long period) {
        scheduler.runTaskTimer(location, runnable, delay, period);
    }

    /**
     * Schedules a task on the region that owns the provided entity.
     *
     * @param entity   entity context
     * @param runnable task to run
     */
    public void runTask(@NotNull Entity entity, @NotNull Runnable runnable) {
        scheduler.runTask(entity, runnable);
    }

    /**
     * Schedules a delayed task on the region that owns the provided entity.
     *
     * @param entity   entity context
     * @param runnable task to run
     * @param delay    delay in ticks
     */
    public void runTaskLater(@NotNull Entity entity, @NotNull Runnable runnable, long delay) {
        scheduler.runTaskLater(entity, runnable, delay);
    }

    /**
     * Schedules a repeating task on the region that owns the provided entity.
     *
     * @param entity   entity context
     * @param runnable task to run
     * @param delay    initial delay in ticks
     * @param period   period in ticks
     */
    public void runTaskTimer(@NotNull Entity entity, @NotNull Runnable runnable, long delay, long period) {
        scheduler.runTaskTimer(entity, runnable, delay, period);
    }

    /**
     * Schedules an async task immediately.
     *
     * @param runnable task to run
     */
    public void runTaskAsynchronously(@NotNull Runnable runnable) {
        scheduler.runTaskAsynchronously(runnable);
    }

    /**
     * Schedules an async task immediately (legacy signature).
     *
     * @param plugin   plugin (legacy parameter)
     * @param runnable task to run
     * @deprecated Use {@link #runTaskAsynchronously(Runnable)}.
     */
    @Deprecated
    public void runTaskAsynchronously(@NotNull Plugin plugin, @NotNull Runnable runnable) {
        scheduler.runTaskAsynchronously(plugin, runnable);
    }

    /**
     * Schedules an async task after a delay.
     *
     * @param runnable task to run
     * @param delay    delay in ticks
     */
    public void runTaskLaterAsynchronously(@NotNull Runnable runnable, long delay) {
        scheduler.runTaskLaterAsynchronously(runnable, delay);
    }

    /**
     * Schedules an async task after a delay (legacy signature).
     *
     * @param plugin   plugin (legacy parameter)
     * @param runnable task to run
     * @param delay    delay in ticks
     * @deprecated Use {@link #runTaskLaterAsynchronously(Runnable, long)}.
     */
    @Deprecated
    public void runTaskLaterAsynchronously(@NotNull Plugin plugin, @NotNull Runnable runnable, long delay) {
        scheduler.runTaskLaterAsynchronously(plugin, runnable, delay);
    }

    /**
     * Schedules a repeating async task.
     *
     * @param runnable task to run
     * @param delay    initial delay in ticks
     * @param period   period in ticks
     */
    public void runTaskTimerAsynchronously(@NotNull Runnable runnable, long delay, long period) {
        scheduler.runTaskTimerAsynchronously(runnable, delay, period);
    }

    /**
     * Schedules a repeating async task (legacy signature).
     *
     * @param plugin   plugin (legacy parameter)
     * @param runnable task to run
     * @param delay    initial delay in ticks
     * @param period   period in ticks
     * @deprecated Use {@link #runTaskTimerAsynchronously(Runnable, long, long)}.
     */
    @Deprecated
    public void runTaskTimerAsynchronously(@NotNull Plugin plugin, @NotNull Runnable runnable, long delay, long period) {
        scheduler.runTaskTimerAsynchronously(plugin, runnable, delay, period);
    }

    /**
     * Legacy alias for {@link #runTask(Runnable)}.
     *
     * @param runnable task to run
     * @deprecated Use {@link #runTask(Runnable)}.
     */
    @Deprecated
    public void scheduleSyncDelayedTask(@NotNull Runnable runnable) {
        scheduler.scheduleSyncDelayedTask(runnable);
    }

    /**
     * Legacy alias for {@link #runTaskLater(Runnable, long)}.
     *
     * @param runnable task to run
     * @param delay    delay in ticks
     * @deprecated Use {@link #runTaskLater(Runnable, long)}.
     */
    @Deprecated
    public void scheduleSyncDelayedTask(@NotNull Runnable runnable, long delay) {
        scheduler.scheduleSyncDelayedTask(runnable, delay);
    }

    /**
     * Legacy alias for {@link #runTaskTimer(Runnable, long, long)}.
     *
     * @param runnable task to run
     * @param delay    initial delay in ticks
     * @param period   period in ticks
     * @deprecated Use {@link #runTaskTimer(Runnable, long, long)}.
     */
    @Deprecated
    public void scheduleSyncRepeatingTask(@NotNull Runnable runnable, long delay, long period) {
        scheduler.scheduleSyncRepeatingTask(runnable, delay, period);
    }

    /**
     * Executes a task on the global/main thread.
     *
     * @param runnable task to run
     */
    public void execute(@NotNull Runnable runnable) {
        scheduler.execute(runnable);
    }

    /**
     * Executes a task on the region that owns the provided location.
     *
     * @param location location context
     * @param runnable task to run
     */
    public void execute(@NotNull Location location, @NotNull Runnable runnable) {
        scheduler.execute(location, runnable);
    }

    /**
     * Executes a task on the region that owns the provided entity.
     *
     * @param entity   entity context
     * @param runnable task to run
     */
    public void execute(@NotNull Entity entity, @NotNull Runnable runnable) {
        scheduler.execute(entity, runnable);
    }

    /**
     * Cancels a task handle if present.
     *
     * @param task task handle (nullable)
     */
    public void cancel(@Nullable GlobalTask task) {
        if (task != null) task.cancel();
    }

    /**
     * Cancels a task by id.
     *
     * @param taskId task id
     */
    public void cancel(int taskId) {
        scheduler.cancel(taskId);
    }

    /**
     * Cancels all tasks scheduled by the owning plugin.
     */
    public void cancelAll() {
        scheduler.cancelAll();
    }

    /**
     * Cancels all tasks scheduled by the specified plugin.
     *
     * @param plugin target plugin
     */
    public void cancelAll(@NotNull Plugin plugin) {
        scheduler.cancelAll(plugin);
    }

    /**
     * Managed task wrapper that retains the owning {@link SchedulerManager}.
     */
    public static final class Task {

        /**
         * Owning manager for this task wrapper.
         */
        private final SchedulerManager manager;

        /**
         * Backing scheduler task handle.
         */
        private final GlobalTask task;

        /**
         * Creates a managed task wrapper.
         *
         * @param manager owning manager
         * @param task    backing task handle
         */
        private Task(@NotNull SchedulerManager manager, @NotNull GlobalTask task) {
            this.manager = manager;
            this.task = task;
        }

        /**
         * Returns the owning {@link SchedulerManager}.
         *
         * @return scheduler manager
         */
        public @NotNull SchedulerManager getSchedulerManager() {
            return manager;
        }

        /**
         * Returns the raw {@link GlobalTask} handle.
         *
         * @return task handle
         */
        public @NotNull GlobalTask getHandle() {
            return task;
        }

        /**
         * Returns the task id.
         *
         * @return task id
         */
        public int getTaskId() {
            return task.getTaskId();
        }

        /**
         * Cancels this task.
         */
        public void cancel() {
            task.cancel();
        }

        /**
         * Checks if this task is cancelled.
         *
         * @return true if cancelled, otherwise false
         */
        public boolean isCancelled() {
            return task.isCancelled();
        }

        /**
         * Returns the plugin that scheduled this task.
         *
         * @return owning plugin
         */
        public @NotNull Plugin getPlugin() {
            return task.getPlugin();
        }

        /**
         * Checks if this task is currently running.
         *
         * @return true if running, otherwise false
         */
        public boolean isRunning() {
            return task.isRunning();
        }

        /**
         * Checks if this task is repeating.
         *
         * @return true if repeating, otherwise false
         */
        public boolean isRepeating() {
            return task.isRepeating();
        }
    }

    /**
     * Returnable scheduler API for when a task handle is needed.
     */
    public static final class Returnable {

        /**
         * Owning manager used to attach context to returned {@link Task}s.
         */
        private final SchedulerManager manager;

        /**
         * Backing scheduler instance.
         */
        private final GlobalScheduler scheduler;

        /**
         * Creates a returnable wrapper.
         *
         * @param manager   owning manager
         * @param scheduler backing scheduler
         */
        private Returnable(@NotNull SchedulerManager manager, @NotNull GlobalScheduler scheduler) {
            this.manager = manager;
            this.scheduler = scheduler;
        }

        /**
         * Calls a task on the global/main thread and returns a {@link Future}.
         *
         * @param task callable to execute
         * @param <T>  result type
         * @return future result
         */
        public <T> @NotNull Future<T> callSyncMethod(@NotNull Callable<T> task) {
            return scheduler.callSyncMethod(task);
        }

        /**
         * Wraps a {@link GlobalTask} with a reference back to the owning {@link SchedulerManager}.
         *
         * @param task task handle
         * @return managed task
         */
        public @NotNull Task wrap(@NotNull GlobalTask task) {
            return new Task(manager, task);
        }

        /**
         * Wraps a {@link GlobalTask} if present.
         *
         * @param task task handle (nullable)
         * @return managed task (or null)
         */
        public @Nullable Task wrapNullable(@Nullable GlobalTask task) {
            return task == null ? null : new Task(manager, task);
        }

        /**
         * Schedules a task for the next tick (global/main).
         *
         * @param runnable task to run
         * @return managed task handle
         */
        public @NotNull Task runTask(@NotNull Runnable runnable) {
            return wrap(scheduler.runTask(runnable));
        }

        /**
         * Schedules a task for the next tick (legacy signature).
         *
         * @param plugin   plugin (legacy parameter)
         * @param runnable task to run
         * @return managed task handle
         * @deprecated Use {@link #runTask(Runnable)}.
         */
        @Deprecated
        public @NotNull Task runTask(@NotNull Plugin plugin, @NotNull Runnable runnable) {
            return wrap(scheduler.runTask(plugin, runnable));
        }

        /**
         * Schedules a delayed task (global/main).
         *
         * @param runnable task to run
         * @param delay    delay in ticks
         * @return managed task handle
         */
        public @NotNull Task runTaskLater(@NotNull Runnable runnable, long delay) {
            return wrap(scheduler.runTaskLater(runnable, delay));
        }

        /**
         * Schedules a delayed task (legacy signature).
         *
         * @param plugin   plugin (legacy parameter)
         * @param runnable task to run
         * @param delay    delay in ticks
         * @return managed task handle
         * @deprecated Use {@link #runTaskLater(Runnable, long)}.
         */
        @Deprecated
        public @NotNull Task runTaskLater(@NotNull Plugin plugin, @NotNull Runnable runnable, long delay) {
            return wrap(scheduler.runTaskLater(plugin, runnable, delay));
        }

        /**
         * Schedules a repeating task (global/main).
         *
         * @param runnable task to run
         * @param delay    initial delay in ticks
         * @param period   period in ticks
         * @return managed task handle
         */
        public @NotNull Task runTaskTimer(@NotNull Runnable runnable, long delay, long period) {
            return wrap(scheduler.runTaskTimer(runnable, delay, period));
        }

        /**
         * Schedules a repeating task (legacy signature).
         *
         * @param plugin   plugin (legacy parameter)
         * @param runnable task to run
         * @param delay    initial delay in ticks
         * @param period   period in ticks
         * @return managed task handle
         * @deprecated Use {@link #runTaskTimer(Runnable, long, long)}.
         */
        @Deprecated
        public @NotNull Task runTaskTimer(@NotNull Plugin plugin, @NotNull Runnable runnable, long delay, long period) {
            return wrap(scheduler.runTaskTimer(plugin, runnable, delay, period));
        }

        /**
         * Schedules a task on the region that owns the provided location.
         *
         * @param location location context
         * @param runnable task to run
         * @return managed task handle
         */
        public @NotNull Task runTask(@NotNull Location location, @NotNull Runnable runnable) {
            return wrap(scheduler.runTask(location, runnable));
        }

        /**
         * Schedules a delayed task on the region that owns the provided location.
         *
         * @param location location context
         * @param runnable task to run
         * @param delay    delay in ticks
         * @return managed task handle
         */
        public @NotNull Task runTaskLater(@NotNull Location location, @NotNull Runnable runnable, long delay) {
            return wrap(scheduler.runTaskLater(location, runnable, delay));
        }

        /**
         * Schedules a repeating task on the region that owns the provided location.
         *
         * @param location location context
         * @param runnable task to run
         * @param delay    initial delay in ticks
         * @param period   period in ticks
         * @return managed task handle
         */
        public @NotNull Task runTaskTimer(@NotNull Location location, @NotNull Runnable runnable, long delay, long period) {
            return wrap(scheduler.runTaskTimer(location, runnable, delay, period));
        }

        /**
         * Schedules a task on the region that owns the provided entity.
         *
         * @param entity   entity context
         * @param runnable task to run
         * @return managed task handle
         */
        public @NotNull Task runTask(@NotNull Entity entity, @NotNull Runnable runnable) {
            return wrap(scheduler.runTask(entity, runnable));
        }

        /**
         * Schedules a delayed task on the region that owns the provided entity.
         *
         * @param entity   entity context
         * @param runnable task to run
         * @param delay    delay in ticks
         * @return managed task handle
         */
        public @NotNull Task runTaskLater(@NotNull Entity entity, @NotNull Runnable runnable, long delay) {
            return wrap(scheduler.runTaskLater(entity, runnable, delay));
        }

        /**
         * Schedules a repeating task on the region that owns the provided entity.
         *
         * @param entity   entity context
         * @param runnable task to run
         * @param delay    initial delay in ticks
         * @param period   period in ticks
         * @return managed task handle
         */
        public @NotNull Task runTaskTimer(@NotNull Entity entity, @NotNull Runnable runnable, long delay, long period) {
            return wrap(scheduler.runTaskTimer(entity, runnable, delay, period));
        }

        /**
         * Schedules an async task immediately.
         *
         * @param runnable task to run
         * @return managed task handle
         */
        public @NotNull Task runTaskAsynchronously(@NotNull Runnable runnable) {
            return wrap(scheduler.runTaskAsynchronously(runnable));
        }

        /**
         * Schedules an async task immediately (legacy signature).
         *
         * @param plugin   plugin (legacy parameter)
         * @param runnable task to run
         * @return managed task handle
         * @deprecated Use {@link #runTaskAsynchronously(Runnable)}.
         */
        @Deprecated
        public @NotNull Task runTaskAsynchronously(@NotNull Plugin plugin, @NotNull Runnable runnable) {
            return wrap(scheduler.runTaskAsynchronously(plugin, runnable));
        }

        /**
         * Schedules an async task after a delay.
         *
         * @param runnable task to run
         * @param delay    delay in ticks
         * @return managed task handle
         */
        public @NotNull Task runTaskLaterAsynchronously(@NotNull Runnable runnable, long delay) {
            return wrap(scheduler.runTaskLaterAsynchronously(runnable, delay));
        }

        /**
         * Schedules an async task after a delay (legacy signature).
         *
         * @param plugin   plugin (legacy parameter)
         * @param runnable task to run
         * @param delay    delay in ticks
         * @return managed task handle
         * @deprecated Use {@link #runTaskLaterAsynchronously(Runnable, long)}.
         */
        @Deprecated
        public @NotNull Task runTaskLaterAsynchronously(@NotNull Plugin plugin, @NotNull Runnable runnable, long delay) {
            return wrap(scheduler.runTaskLaterAsynchronously(plugin, runnable, delay));
        }

        /**
         * Schedules a repeating async task.
         *
         * @param runnable task to run
         * @param delay    initial delay in ticks
         * @param period   period in ticks
         * @return managed task handle
         */
        public @NotNull Task runTaskTimerAsynchronously(@NotNull Runnable runnable, long delay, long period) {
            return wrap(scheduler.runTaskTimerAsynchronously(runnable, delay, period));
        }

        /**
         * Schedules a repeating async task (legacy signature).
         *
         * @param plugin   plugin (legacy parameter)
         * @param runnable task to run
         * @param delay    initial delay in ticks
         * @param period   period in ticks
         * @return managed task handle
         * @deprecated Use {@link #runTaskTimerAsynchronously(Runnable, long, long)}.
         */
        @Deprecated
        public @NotNull Task runTaskTimerAsynchronously(@NotNull Plugin plugin, @NotNull Runnable runnable, long delay, long period) {
            return wrap(scheduler.runTaskTimerAsynchronously(plugin, runnable, delay, period));
        }

        /**
         * Legacy alias for {@link #runTask(Runnable)}.
         *
         * @param runnable task to run
         * @return managed task handle
         * @deprecated Use {@link #runTask(Runnable)}.
         */
        @Deprecated
        public @NotNull Task scheduleSyncDelayedTask(@NotNull Runnable runnable) {
            return wrap(scheduler.scheduleSyncDelayedTask(runnable));
        }

        /**
         * Legacy alias for {@link #runTaskLater(Runnable, long)}.
         *
         * @param runnable task to run
         * @param delay    delay in ticks
         * @return managed task handle
         * @deprecated Use {@link #runTaskLater(Runnable, long)}.
         */
        @Deprecated
        public @NotNull Task scheduleSyncDelayedTask(@NotNull Runnable runnable, long delay) {
            return wrap(scheduler.scheduleSyncDelayedTask(runnable, delay));
        }

        /**
         * Legacy alias for {@link #runTaskTimer(Runnable, long, long)}.
         *
         * @param runnable task to run
         * @param delay    initial delay in ticks
         * @param period   period in ticks
         * @return managed task handle
         * @deprecated Use {@link #runTaskTimer(Runnable, long, long)}.
         */
        @Deprecated
        public @NotNull Task scheduleSyncRepeatingTask(@NotNull Runnable runnable, long delay, long period) {
            return wrap(scheduler.scheduleSyncRepeatingTask(runnable, delay, period));
        }
    }
}