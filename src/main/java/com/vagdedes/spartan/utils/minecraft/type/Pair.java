package com.vagdedes.spartan.utils.minecraft.type;

public final class Pair<X, Y> {
    private X x;
    private Y y;

    public Pair(X x, Y y) {
        this.x = x;
        this.y = y;
    }

    public X getX() {
        return this.x;
    }

    public Y getY() {
        return this.y;
    }

    public void setX(X x) {
        this.x = x;
    }

    public void setY(Y y) {
        this.y = y;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof Pair)) return false;
        final Pair<?, ?> other = (Pair<?, ?>) o;
        final Object this$x = this.getX();
        final Object other$x = other.getX();
        if (this$x == null ? other$x != null : !this$x.equals(other$x)) return false;
        final Object this$y = this.getY();
        final Object other$y = other.getY();
        if (this$y == null ? other$y != null : !this$y.equals(other$y)) return false;
        return true;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $x = this.getX();
        result = result * PRIME + ($x == null ? 43 : $x.hashCode());
        final Object $y = this.getY();
        result = result * PRIME + ($y == null ? 43 : $y.hashCode());
        return result;
    }

    public String toString() {
        return "Pair(x=" + this.getX() + ", y=" + this.getY() + ")";
    }
}