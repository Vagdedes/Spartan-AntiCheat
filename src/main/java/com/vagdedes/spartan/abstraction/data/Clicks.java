package com.vagdedes.spartan.abstraction.data;

import com.vagdedes.spartan.functionality.server.TPS;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

public class Clicks {

    private final Collection<Long> clicks;
    private final Cooldowns cooldowns;

    public Clicks() {
        this.clicks = new LinkedList<>();
        this.cooldowns = new Cooldowns(null);
    }

    public void calculate() {
        this.clicks.add(System.currentTimeMillis());
    }

    public int getCount() {
        return this.getRawData().size();
    }

    public boolean canDistributeInformation() {
        if (cooldowns.canDo("")) {
            cooldowns.add("", (int) TPS.maximum);
            return true;
        } else {
            return false;
        }
    }

    private void remove() {
        long time = System.currentTimeMillis();
        Iterator<Long> iterator = this.clicks.iterator();

        while (iterator.hasNext()) {
            if (time - iterator.next() > 1_000L) {
                iterator.remove();
            } else {
                break;
            }
        }
    }

    private Collection<Long> getRawData() {
        if (!this.clicks.isEmpty()) {
            this.remove();
            return this.clicks;
        } else {
            return new ArrayList<>(0);
        }
    }

    public Collection<Long> getData() {
        if (!this.clicks.isEmpty()) {
            this.remove();
            return new ArrayList<>(this.clicks);
        } else {
            return new ArrayList<>(0);
        }
    }

}
