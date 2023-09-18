package me.vagdedes.spartan.objects.system;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class Threads {

    // Methods

    private static boolean enabled = true;

    public static void disable() {
        enabled = false;
    }

    // Object

    public static class ThreadPool {

        private final List<Runnable> runnables;

        public ThreadPool(long refreshRateInMilliseconds) {
            runnables = Collections.synchronizedList(new LinkedList<>());
            new Thread(() -> {
                while (enabled) {
                    Runnable runnable;

                    synchronized (runnables) {
                        runnable = runnables.isEmpty() ? null : runnables.remove(0);
                    }

                    if (runnable != null) {
                        runnable.run();
                    } else {
                        try {
                            Thread.sleep(refreshRateInMilliseconds);
                        } catch (Exception ignored) {
                        }
                    }
                }
                runnables.clear();
            }).start();
        }

        public boolean executeIfFreeElseHere(Runnable runnable) {
            synchronized (runnables) {
                if (runnables.isEmpty()) {
                    return runnables.add(runnable);
                } else {
                    runnable.run();
                    return false;
                }
            }
        }

        public boolean executeIfFree(Runnable runnable) {
            synchronized (runnables) {
                return runnables.isEmpty() && runnables.add(runnable);
            }
        }

        public boolean execute(Runnable runnable) {
            synchronized (runnables) {
                return runnables.add(runnable);
            }
        }

    }
}
