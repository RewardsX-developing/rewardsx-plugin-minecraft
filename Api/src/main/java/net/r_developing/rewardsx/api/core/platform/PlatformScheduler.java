package net.r_developing.rewardsx.api.core.platform;

/**
 * Platform-independent abstraction for task scheduling.
 *
 * <p>Provides methods to schedule tasks on different threads and at different times.
 * Implementations wrap platform-specific schedulers:
 *   - Bukkit.getScheduler() for Spigot
 *   - Velocity's SchedulerService for Velocity
 *   - BungeeCord's ExecutorService for BungeeCord
 *
 * <p>Key distinction: sync tasks run on the main server thread (safe for API calls);
 * async tasks run on thread pool threads (safe for I/O, blocking operations).
 *
 * <p>Used throughout the codebase for:
 *   - Polling the backend (async repeating)
 *   - Executing reward commands on the main thread
 *   - Loading translations in parallel
 *   - Delaying version checks
 */
public interface PlatformScheduler {
    /**
     * Schedules a task to run repeatedly on an async thread.
     *
     * <p>Essential for non-blocking operations like HTTP calls to the backend.
     * The task runs after an initial delay, then repeats at fixed intervals.
     *
     * <p>Used by RewardPollingTask and Fetcher to poll for updates without blocking
     * the main server thread. Each iteration runs independently on a thread pool.
     *
     * <p>All blocking I/O (API calls, database queries) should use this method
     * to avoid freezing the server.
     *
     * @param task         the runnable to execute repeatedly
     * @param delayTicks   initial delay before first execution (in server ticks; 20 ticks = 1 sec)
     * @param periodTicks  interval between executions (in server ticks)
     */
    void runAsyncRepeatingTask(Runnable task, long delayTicks, long periodTicks);

    /**
     * Schedules a task to run on the main server thread as soon as possible.
     *
     * <p>Safe for all server API calls (opening inventories, modifying player data,
     * spawning entities, etc.). Use this when you need to touch the server world
     * or player state from an async context.
     *
     * <p>Used by Buy.confirm() to execute reward commands after the backend confirms
     * a purchase. The confirmation callback runs async, but commands need the main thread.
     *
     * @param task the runnable to execute on the main thread
     */
    void runSync(Runnable task);

    /**
     * Schedules a task to run on an async (thread pool) thread immediately.
     *
     * <p>Non-blocking - returns immediately without waiting for execution.
     * The task starts as soon as a thread is available.
     *
     * <p>Safe for I/O (HTTP, database, file operations) but NOT safe for server API.
     * If you need server API, call runSync() from within the async task when needed.
     *
     * <p>Used by Messager to load translations in parallel, and by Config to reload
     * files without blocking the main thread.
     *
     * @param task the runnable to execute async
     */
    void runAsync(Runnable task);

    /**
     * Schedules a task to run on an async thread after a delay.
     *
     * <p>Non-blocking - returns immediately. The task runs once after the delay expires.
     * Similar to runAsync() but with a delay (useful for retries, backoff, etc.).
     *
     * <p>Used for things like delayed update checks or retry logic.
     *
     * @param task       the runnable to execute
     * @param delayTicks the delay in server ticks before execution (20 ticks = 1 sec)
     */
    void runTaskLater(Runnable task, long delayTicks);

    /**
     * Cancels all scheduled tasks.
     *
     * <p>Called on plugin disable to clean up. Stops all repeating tasks, pending
     * delayed tasks, and async work in progress.
     *
     * <p>Implementation may cancel platform-specific task IDs or shut down thread pools,
     * depending on how the platform scheduler works.
     */
    void cancelAll();
}