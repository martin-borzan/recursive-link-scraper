package me.borzan.linkscraper.web;

import java.util.concurrent.ExecutionException;

/*
 * This is a callable framework that should be extended and implemented for each custom use-case, analogous to, and in conjunction with the WebScraperService
 * It is supposed to handle the execution exceptions that may be thrown by the Future::get method to give the implementation a choice in how to handle failures.
 */
public abstract class WebScraperWorker<T extends WebScrapingResult<?>> implements ExceptionHandlingCallable<T> {
    protected final WebScraperService<T> webScraperService;

    protected WebScraperWorker(WebScraperService<T> webScraperService) {
        if(webScraperService == null) {
            throw new IllegalArgumentException("The WebScraperService must not be null");
        }

        this.webScraperService = webScraperService;
    }

    public final T scrape() {
        try {
            return this.webScraperService.scheduleWorkerAndWaitForResult(this);
        } catch (InterruptedException | ExecutionException e) {
            return this.handleExecutionException(e);
        }
    }
}