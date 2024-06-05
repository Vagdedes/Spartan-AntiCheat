package com.vagdedes.spartan.abstraction.data;

import java.util.*;

public class Clicks {

    private final Collection<Long> clicks;

    public Clicks() {
        this.clicks = Collections.synchronizedList(new LinkedList<>());
    }

    public void calculate() {
        long time = System.currentTimeMillis();

        synchronized (this.clicks) {
            this.remove();
            this.clicks.add(time);
        }
    }

    public int getCount() {
        return this.getRawData().size();
    }

    private void remove() {
        Iterator<Long> iterator = this.clicks.iterator();

        while (iterator.hasNext()) {
            if (System.currentTimeMillis() - iterator.next() > 1_000L) {
                iterator.remove();
            } else {
                break;
            }
        }
    }

    private Collection<Long> getRawData() {
        if (!this.clicks.isEmpty()) {
            synchronized (this.clicks) {
                this.remove();
            }
            return this.clicks;
        } else {
            return new ArrayList<>(0);
        }
    }

    public Collection<Long> getData() {
        if (!this.clicks.isEmpty()) {
            synchronized (this.clicks) {
                this.remove();
            }
            return new ArrayList<>(this.clicks);
        } else {
            return new ArrayList<>(0);
        }
    }

}
