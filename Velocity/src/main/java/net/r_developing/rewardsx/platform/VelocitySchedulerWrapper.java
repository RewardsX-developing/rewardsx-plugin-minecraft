package net.r_developing.rewardsx.platform;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import net.r_developing.rewardsx.Plugin;
import net.r_developing.rewardsx.api.core.platform.PlatformScheduler;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class VelocitySchedulerWrapper implements PlatformScheduler {
    private final Plugin plugin;
    private final ProxyServer server;
    private final Set<ScheduledTask> runningTasks = ConcurrentHashMap.newKeySet();

    public VelocitySchedulerWrapper(Plugin plugin, ProxyServer server) {
        this.plugin = plugin;
        this.server = server;
    }

    @Override
    public void runAsyncRepeatingTask(Runnable task, long delayTicks, long periodTicks) {
        long delayMillis = delayTicks * 50L;
        long periodMillis = periodTicks * 50L;

        ScheduledTask scheduledTask = server.getScheduler()
                .buildTask(plugin, task)
                .delay(delayMillis, TimeUnit.MILLISECONDS)
                .repeat(periodMillis, TimeUnit.MILLISECONDS)
                .schedule();

        runningTasks.add(scheduledTask);
    }

    @Override
    public void runAsync(Runnable task) {
        ScheduledTask[] taskRef = new ScheduledTask[1];

        taskRef[0] = server.getScheduler()
                .buildTask(plugin, () -> {
                    try {
                        task.run();
                    } finally {
                        if (taskRef[0] != null) {
                            runningTasks.remove(taskRef[0]);
                        }
                    }
                })
                .schedule();

        if (taskRef[0] != null) {
            runningTasks.add(taskRef[0]);
        }
    }

    @Override
    public void runSync(Runnable task) {
        runAsync(task);
    }

    @Override
    public void runTaskLater(Runnable task, long delayTicks) {
        long delayMillis = delayTicks * 50L;
        ScheduledTask[] taskRef = new ScheduledTask[1];

        taskRef[0] = server.getScheduler()
                .buildTask(plugin, () -> {
                    try {
                        task.run();
                    } finally {
                        if (taskRef[0] != null) {
                            runningTasks.remove(taskRef[0]);
                        }
                    }
                })
                .delay(delayMillis, TimeUnit.MILLISECONDS)
                .schedule();

        if (taskRef[0] != null) {
            runningTasks.add(taskRef[0]);
        }
    }

    public void cancelAll() {
        for (ScheduledTask task : runningTasks) {
            task.cancel();
        }
        runningTasks.clear();
    }
}