package me.borzan.linkscraper.web;

import me.borzan.linkscraper.util.UriUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class WebScraperService {
    private static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(5);
    private static final String EMPTY_STRING = "";

    private static final Map<URI, Set<String>> KNOWN_URIS = new HashMap<>();
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newVirtualThreadPerTaskExecutor();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .executor(EXECUTOR_SERVICE)
            .connectTimeout(CONNECTION_TIMEOUT)
            .build();

    public static List<Hyperlink> visitDomain(URI websiteUri) {
        KNOWN_URIS.computeIfAbsent(websiteUri, uri -> new HashSet<>()).add(EMPTY_STRING);
        new ScraperThread(websiteUri).runWithExceptionHandling();
        EXECUTOR_SERVICE.shutdown();

        return KNOWN_URIS.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream()
                        .map(linkLabel -> new Hyperlink(entry.getKey(), linkLabel)))
                .sorted()
                .toList();
    }

    private record ScraperThread(Hyperlink hyperlink) implements Runnable {
        private static final Logger LOGGER = Logger.getLogger(ScraperThread.class.getName());
        private static final Pattern HYPERLINK_REGEX =
                Pattern.compile("<a\\s+[^>]*href=\"(https?[^\"\\s]*)\"[^>]*>(.*?)</a>", Pattern.CASE_INSENSITIVE);

        public ScraperThread(URI websiteUri) {
            this(new Hyperlink(websiteUri, EMPTY_STRING));
        }

        @Override
        public void run() {
            HttpResponse<String> response = visitUri();
            String responseBody = extractBody(response);
            Set<Hyperlink> urisFound = parseHyperlinksFromResponse(responseBody);
            propagateLinkScraping(urisFound);
        }

        private HttpResponse<String> visitUri() {
            // Uncomment for crude logging (be aware that knownUris.size() is called without critical section so it might not be up to date when printed):
            LOGGER.log(Level.INFO, "%s - # of known uris: %6d - uri: '%s' - label: '%s'".formatted(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd:hh-mm-ss")), KNOWN_URIS.size(), hyperlink.uri(), hyperlink.label()));

            HttpRequest request = HttpRequest.newBuilder().uri(hyperlink.uri()).GET().build();
            HttpResponse<String> response = null;
            try {
                response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (InterruptedException | IOException e) {
                LOGGER.log(Level.SEVERE, ("An error (%s) occurred when visiting '%s': %s. " +
                        "Link is marked as visited, but it will not propagate the search.")
                        .formatted(e.getClass().getSimpleName(), hyperlink.uri(), e.getMessage()));
            }
            return response;
        }

        private String extractBody(HttpResponse<String> response) {
            if (response == null) {
                return EMPTY_STRING;
            }

            String responseStringToParse = response.body();
            if (responseStringToParse == null) {
                responseStringToParse = EMPTY_STRING;
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                // Parsing every error seems excessive so just checking for a relocation header should be good enough here
                String additionalStringToParse = response.headers().map()
                        .getOrDefault("Location", Collections.emptyList()).stream()
                        .map(ScraperThread::mockHtmlLink)
                        .collect(Collectors.joining());
                responseStringToParse += additionalStringToParse;
            }
            return responseStringToParse;
        }

        private void propagateLinkScraping(Set<Hyperlink> urisFound) {
            urisFound
                    .parallelStream()
                    .filter(hyperlink::sharesDomainWith)
                    .filter(ScraperThread::isUnknownUri)
                    .map(ScraperThread::new)
                    .forEach(ScraperThread::runWithExceptionHandling);
        }

        private void runWithExceptionHandling() {
            try {
                EXECUTOR_SERVICE.submit(this).get();
            } catch (InterruptedException | ExecutionException e) {
                LOGGER.log(Level.SEVERE, ("An error occurred during multi-thread execution: %s. " +
                        "Resulting list might be incomplete.").formatted(e.getMessage()));
            }
        }

        private static Set<Hyperlink> parseHyperlinksFromResponse(String responseBody) {
            Matcher regexMatcher = HYPERLINK_REGEX.matcher(responseBody);

            Set<Hyperlink> parsedLinks = new HashSet<>();
            while (regexMatcher.find()) {
                String hrefMatch = regexMatcher.group(1).trim();
                String labelMatch = regexMatcher.group(2);

                URI parsedUri = UriUtils.tryParseUri(hrefMatch);
                if (parsedUri != null) {
                    parsedLinks.add(new Hyperlink(parsedUri, labelMatch));
                }
            }

            return parsedLinks;
        }

        private static boolean isUnknownUri(Hyperlink hyperlink) {
            synchronized (KNOWN_URIS) {
                if (KNOWN_URIS.containsKey(hyperlink.uri())) {
                    // The url could have multiple different labels
                    KNOWN_URIS.get(hyperlink.uri()).add(hyperlink.label());
                    // Prevent visiting the URI again
                    return false;
                }
                KNOWN_URIS.computeIfAbsent(hyperlink.uri(), uri -> new HashSet<>()).add(hyperlink.label());
                return true;
            }
        }

        private static String mockHtmlLink(String uriString) {
            return "<a href=\"%s\"></a>".formatted(uriString);
        }
    }
}
