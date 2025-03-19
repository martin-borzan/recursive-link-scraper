package me.borzan.linkscraper.web;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
 * This class is typed such that any inherited class can decide what a scraping result should look like.
 * As the PropagatingHyperlinkWebScraperService class demonstrates, it can be customized to for example extract a List
 * of Hyperlinks. Another possible use-case could have been for example to extract all <img> tags from a website.
 *
 * This class only assumes a WebScraperWorker thread class facilitates the scraping, and provides an http client
 * and executor service to schedule the worker and send web requests as the worker thread sees fit.
 *
 * The executor service does not shut down internally, so that it can be re-used for multiple different scraper workers.
 */
public abstract class WebScraperService<T extends WebScrapingResult<?>> {
    protected final HttpClient httpClient;
    protected final ExecutorService executorService;

    public WebScraperService(HttpClient httpClient, ExecutorService executorService) {
        if(httpClient == null) {
            throw new IllegalStateException("HttpClient is null. Cannot create web scrapers without it.");
        }
        if(executorService == null) {
            throw new IllegalStateException("ExecutorService is null. Cannot schedule web scrapers without it.");
        }

        this.httpClient = httpClient;
        this.executorService = executorService;
    }

    public WebScraperService(ExecutorService executorService, int connectionTimeoutSeconds) {
        this(HttpClient
                .newBuilder()
                .executor(executorService)
                .connectTimeout(Duration.ofSeconds(connectionTimeoutSeconds))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build(),
            executorService);
    }

    public WebScraperService(int connectionTimeoutSeconds) {
        this(Executors.newVirtualThreadPerTaskExecutor(), connectionTimeoutSeconds);
    }

    public final T scrapeUri(URI websiteUri) {
        if(executorService.isShutdown()) {
            throw new IllegalStateException("ExecutorService is shut down. Cannot re-use this WebScraperService.");
        }
        return createScraperForUri(websiteUri).scrape();
    }

    public final HttpResponse<String> sendHttpRequest(URI uri) throws IOException, InterruptedException {
        return httpClient.send(HttpRequest.newBuilder().uri(uri).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    public final T scheduleWorkerAndWaitForResult(WebScraperWorker<T> worker) throws ExecutionException, InterruptedException {
        return this.executorService.submit(worker).get();
    }

    protected abstract WebScraperWorker<T> createScraperForUri(URI websiteUri);
}
