package com.vagdedes.spartan.utils.java;

import lombok.experimental.UtilityClass;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

@UtilityClass
public class TryIgnore {

    public static ThrowableHandler throwableHandler = Throwable::printStackTrace;
    public static <T> T unchecked(SupplierThrows<T> supplier) {
        try {
            return supplier.get();
        } catch(Exception e) {
            doThrow0(e);
            throw new AssertionError();
        }
    }
    public static void unchecked(RunnableThrows runnable) {
        try {
            runnable.run();
        } catch(Exception e) {
            doThrow0(e);
            throw new AssertionError();
        }
    }
    public static <T> Predicate<T> unchecked(PredicateThrows<T> predicate) {
        return t -> {
            try {
                return predicate.test(t);
            } catch(Exception e) {
                doThrow0(e);
                throw new AssertionError();
            }
        };
    }

    public static <T> T ignore(SupplierThrows<T> supplier, T def) {
        try {
            return supplier.get();
        } catch(Throwable e) {
            throwableHandler.handle(e);
            return def;
        }
    }
    public static Optional<Throwable> ignore(RunnableThrows runnable) {
        try {
            runnable.run();
        } catch(Throwable e) {
            throwableHandler.handle(e);
            return Optional.of(e);
        }
        return Optional.empty();
    }

    public static Optional<Throwable> ignore(RunnableThrows runnable, Consumer<Throwable> consumer) {
        try {
            runnable.run();
        } catch(Throwable e) {
            consumer.accept(e);
            throwableHandler.handle(e);
            return Optional.of(e);
        }
        return Optional.empty();
    }

    public static <T> Predicate<T> ignore(PredicateThrows<T> predicate, boolean def) {
        return t -> {
            try {
                return predicate.test(t);
            } catch(Throwable e) {
                throwableHandler.handle(e);
                return def;
            }
        };
    }

    @SuppressWarnings("unchecked")
    public static <E extends Throwable> void doThrow0(Throwable e) throws E {
        throw (E) e;
    }

    public interface SupplierThrows<T> {

        T get() throws Exception;
    }

    public interface RunnableThrows {

        void run() throws Exception;
    }

    public interface PredicateThrows<T> {

        boolean test(T val) throws Exception;
    }

    public interface ThrowableHandler {

        void handle(Throwable throwable);
    }
}