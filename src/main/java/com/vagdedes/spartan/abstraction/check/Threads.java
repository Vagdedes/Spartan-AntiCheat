package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.functionality.server.SpartanBukkit;

import java.util.*;

public class Threads {

    // Methods

    private static boolean enabled = true;

    public static void disable() {
        enabled = false;
    }

    // Object

    public static class ThreadPool {

        private final Thread thread;
        private final List<Runnable> runnables;
        private boolean pause;

        public ThreadPool(long refreshRateInMilliseconds) {
            pause = false;
            runnables = Collections.synchronizedList(new LinkedList<>());
            thread = new Thread(() -> {
                while (enabled) {
                    if (pause) {
                        try {
                            Thread.sleep(refreshRateInMilliseconds);
                        } catch (Exception ignored) {
                        }
                    } else {
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
                }
                runnables.clear();
            });
            thread.start();
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

        public boolean executeIfSyncElseHere(Runnable runnable) {
            if (SpartanBukkit.isSynchronised()) {
                synchronized (runnables) {
                    return runnables.add(runnable);
                }
            } else {
                runnable.run();
                return false;
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
