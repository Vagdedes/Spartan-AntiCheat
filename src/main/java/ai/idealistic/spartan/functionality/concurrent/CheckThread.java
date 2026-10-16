package ai.idealistic.spartan.functionality.concurrent;

import ai.idealistic.spartan.Register;
import ai.idealistic.spartan.abstraction.protocol.PlayerProtocol;
import ai.idealistic.spartan.functionality.server.MultiVersion;
import ai.idealistic.spartan.functionality.server.PluginBase;
import ai.idealistic.spartan.utils.java.OverflowMap;
import ai.idealistic.spartan.utils.java.TryIgnore;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;
import org.bukkit.Bukkit;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Function;

@Log
@UtilityClass
public class CheckThread {

    private static boolean debug = false;
    private static final char INNER_CLASS_SEPARATOR_CHAR = '$';
    public static int STOP_WATCH_TIME_MILLIS = 750;
    @Getter
    private static final ScheduledExecutorService scheduler = MultiVersion.folia
            ? null
            : Executors.newSingleThreadScheduledExecutor(
            new ThreadFactoryBuilder().setNameFormat(Register.pluginName + " Thread %d").build()
    );
    private static final Map<String, Boolean> processedErrors = new OverflowMap<>(
            new ConcurrentHashMap<>(),
            128
    );

    public static void shutdown() {
        if (!MultiVersion.folia) {
            TryIgnore.ignore(scheduler::shutdownNow);
        }
    }

    public static Future<?> run(PlayerProtocol p, Runnable runnable) {
        if (MultiVersion.folia) {
            PluginBase.runTask(p, new DecoratedRunnable(runnable));
            return null;
        } else {
            return scheduler.submit(new DecoratedRunnable(runnable));
        }
    }

    public static ScheduledFuture<?> later(Runnable runnable, long delay) {
        DecoratedRunnable decorated = new DecoratedRunnable(runnable);

        if (MultiVersion.folia) {
            Bukkit.getAsyncScheduler().runDelayed(Register.plugin, task -> decorated.run(), delay, TimeUnit.MILLISECONDS);
            return null;
        } else {
            return scheduler.schedule(decorated, delay, TimeUnit.MILLISECONDS);
        }
    }

    public static ScheduledFuture<?> timer(Runnable runnable, long delay, long period) {
        DecoratedRunnable decorated = new DecoratedRunnable(runnable);

        if (MultiVersion.folia) {
            PluginBase.runRepeatingTask(decorated, delay, period);
            return null;
        } else {
            return scheduler.scheduleAtFixedRate(decorated, delay, period, TimeUnit.MILLISECONDS);
        }
    }

    public static void cancel(ScheduledFuture<?> timer) {
        try {
            if (timer != null) {
                timer.cancel(true);
            }
        } catch (Exception ignored) {
        }
    }

    @ToString
    public static class DecoratedRunnable implements Runnable {
        @Setter
        private static Function<Runnable, Runnable> hotfixDecorator = runnable -> runnable;

        private final Runnable originalRunnable;
        private final Runnable decoratedRunnable;

        public DecoratedRunnable(Runnable originalRunnable) {
            this.originalRunnable = originalRunnable;
            this.decoratedRunnable = hotfixDecorator.apply(originalRunnable);
        }

        @Override
        public void run() {
            long start = System.currentTimeMillis();
            try {
                decoratedRunnable.run();
            } catch (Throwable e) {
                if (debug || processedErrors.putIfAbsent(String.valueOf(e.getMessage()), true) == null) {
                    log.severe("Error during execution of asynchronous task:");
                    e.printStackTrace();
                }
                // Rethrowing would cancel periodic tasks, so only fatal errors are propagated
                if (e instanceof Error) {
                    throw (Error) e;
                }
            } finally {
                if (debug) {
                    long after = System.currentTimeMillis() - start;
                    if (after > STOP_WATCH_TIME_MILLIS) {
                        String l = CheckThread.toString(originalRunnable);
                        if (l.length() > 26) l = l.substring(0, 26) + "...";
                        log.warning("Busy task " + l + ", it was performed " + after + "ms.");
                    }
                }
            }
        }
    }

    private static String toString(Object object) {
        if (object == null) {
            return "null";
        }

        Class<?> clazz = object.getClass();
        StringBuilder sb = new StringBuilder(clazz.getSimpleName() + "{");

        Field[] fields = clazz.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            field.setAccessible(true);

            try {
                if (field.getName().indexOf(INNER_CLASS_SEPARATOR_CHAR) != -1) {
                    sb.append(field.getName()).append("=");
                    Object value = field.get(object);
                    sb.append(value == null ? "null" : value.toString());
                }
            } catch (IllegalAccessException e) {
                sb.append(field.getName()).append("=<access denied>");
            }

            if (i < fields.length - 1) {
                sb.append(", ");
            }
        }

        sb.append("}");
        return sb.toString();
    }
}
