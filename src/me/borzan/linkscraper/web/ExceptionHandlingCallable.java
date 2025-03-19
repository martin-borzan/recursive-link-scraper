package me.borzan.linkscraper.web;

import java.util.concurrent.Callable;

/*
 * A callable that is forced to deal with its own exceptions.
 */
public interface ExceptionHandlingCallable<T> extends Callable<T> {
    T handleExecutionException(Exception exception);
}
