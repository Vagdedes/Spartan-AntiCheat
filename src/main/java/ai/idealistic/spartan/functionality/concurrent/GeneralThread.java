package ai.idealistic.spartan.functionality.concurrent;

import lombok.SneakyThrows;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GeneralThread {

    // Methods

    private static volatile boolean enabled = true;
    private static final Map<String, Boolean> threads = new ConcurrentHashMap<>();

    public static void disable() {
        enabled = false;
        CheckThread.shutdown();
    }

    public static boolean isKnown(Thread thread) {
        return threads.containsKey(thread.getName());
    }

    // Object

    public static class ThreadPool {

        private final Thread thread;
        private final List<Runnable> runnables;
        private volatile boolean pause;

        public ThreadPool(long refreshRateInMilliseconds) {
            pause = false;
            runnables = Collections.synchronizedList(new LinkedList<>());
            thread = new Thread(() -> threadPoolInit(refreshRateInMilliseconds));
            threads.put(thread.getName(), true);
            thread.start();
        }

        @SneakyThrows
        private void threadPoolInit(long refreshRateInMilliseconds) {
            while (enabled) {
                if (pause) {
                    Thread.sleep(refreshRateInMilliseconds);
                } else {
                    Runnable runnable;
                    synchronized (runnables) {
                        runnable = runnables.isEmpty() ? null : runnables.remove(0);
                    }
                    if (runnable != null) {
                        try {
                            runnable.run();
                        } catch (Throwable e) {
                            // A failing task must not terminate the worker, otherwise every later task would never run
                            e.printStackTrace();
                        }
                    } else {
                        Thread.sleep(refreshRateInMilliseconds);
                    }
                }
            }
            runnables.clear();
            threads.remove(Thread.currentThread().getName());
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
                if (!runnables.isEmpty()) return false;
                runnables.add(runnable);
                return true;
            }
        }

        public boolean execute(Runnable runnable) {
            synchronized (runnables) {
                return runnables.add(runnable);
            }
        }

        public boolean executeIfUnknownThreadElseHere(Runnable runnable) {
            if (isKnown(Thread.currentThread())) {
                runnable.run();
                return true;
            } else {
                synchronized (runnables) {
                    return runnables.add(runnable);
                }
            }
        }

        public boolean executeWithPriority(Runnable runnable) {
            synchronized (runnables) {
                if (runnables.isEmpty()) {
                    return runnables.add(runnable);
                } else {
                    Collection<Runnable> runnables = new ArrayList<>(this.runnables.size() + 1);
                    runnables.add(runnable);
                    runnables.addAll(this.runnables);
                    this.runnables.clear();
                    return this.runnables.addAll(runnables);
                }
            }
        }

        public boolean pause() {
            boolean pause = this.pause;
            this.pause = true;
            return !pause;
        }

        public boolean resume() {
            boolean pause = this.pause;
            this.pause = false;
            return pause;
        }

        public long getID() {
            return this.thread.getId();
        }
    }
}
