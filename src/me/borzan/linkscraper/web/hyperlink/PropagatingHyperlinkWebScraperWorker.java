package me.borzan.linkscraper.web.hyperlink;

import me.borzan.linkscraper.web.WebScraperWorker;
import me.borzan.linkscraper.util.UriUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/*
 * This class is final because it is a specific implementation with a specific intent.
 * Any desired changes to the behavior of this class should warrant a separate WebScraperWorker child class.
 */
public final class PropagatingHyperlinkWebScraperWorker extends WebScraperWorker<HyperlinkWebScrapingResult> {
    /*
     * This should be replaced with a DOM parser library, `org.w3c.dom` is too strict with modern html responses
     * Also, this does not handle relative links, although it would be possible to implement that as well,
     * but I hope this is enough as a proof of concept since a DOM parser would be the proper way to go anyway.
     */
    private static final Pattern ABSOLUTE_HYPERLINK_REGEX =
            Pattern.compile("<a\\s+[^>]*href=\"(https?[^\"\\s]*)\"[^>]*>(.*?)</a>", Pattern.CASE_INSENSITIVE);

    private final Map<URI, Set<String>> knownUris;
    private final Hyperlink hyperlink;

    private Instant scrapingStartTime;
    private final Duration maxPropagationDuration;

    private final Logger logger;

    public PropagatingHyperlinkWebScraperWorker(Hyperlink hyperlink, PropagatingHyperlinkWebScraperService service, int maxPropagationDurationMinutes) {
        this(new ConcurrentHashMap<>(), hyperlink, service, null, maxPropagationDurationMinutes);
    }

    private PropagatingHyperlinkWebScraperWorker(Map<URI, Set<String>> knownUris, Hyperlink hyperlink, PropagatingHyperlinkWebScraperService service, Instant scrapingStartTime, long maxScrapingPropagationDurationMinutes) {
        super(service);
        validateRequiredParameters(hyperlink, service, maxScrapingPropagationDurationMinutes);

        this.knownUris = knownUris;
        this.hyperlink = hyperlink;

        this.scrapingStartTime = scrapingStartTime;
        this.maxPropagationDuration = Duration.ofMinutes(maxScrapingPropagationDurationMinutes);

        this.logger = Logger.getLogger(this.getClass().getName());
    }

    /*
     * This factory method is intended to be used for recursive propagation, hence it's private as the initial state of its resulting worker depends on the caller worker.
     */
    private PropagatingHyperlinkWebScraperWorker propagateToNewScraperForHyperlink(Hyperlink hyperlink) {
        Instant scrapingEndTime = Instant.now();
        if(Duration.between(this.scrapingStartTime, scrapingEndTime).compareTo(this.maxPropagationDuration) > 0) {
            // return no new scraper worker as the max duration of minutes has passed
            logger.log(Level.INFO, "The maximum duration of %d minutes is up. Propagation stopped.".formatted(maxPropagationDuration.toMinutes()));
            return null;
        }
        return new PropagatingHyperlinkWebScraperWorker(this.knownUris, hyperlink, (PropagatingHyperlinkWebScraperService) this.webScraperService, this.scrapingStartTime, this.maxPropagationDuration.toMinutes());
    }

    @Override
    public HyperlinkWebScrapingResult call() {
        if(this.scrapingStartTime == null) {
            this.scrapingStartTime = Instant.now();
        }

        HttpResponse<String> response = visitUri();
        String responseBody = extractBodyWithoutNewLines(response);
        Set<Hyperlink> urisFound = parseHyperlinksFromResponse(responseBody);

        return new HyperlinkWebScrapingResult(propagateLinkScraping(urisFound));
    }

    private HttpResponse<String> visitUri() {
        logger.log(Level.INFO, "%s - # of known uris: %6d - uri: '%s' - label: '%s'".formatted(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd:hh-mm-ss")), knownUris.size(), hyperlink.getUri(), hyperlink.getLabel()));

        HttpResponse<String> response = null;
        try {
            response = webScraperService.sendHttpRequest(hyperlink.getUri());
        } catch (InterruptedException | IOException e) {
            logger.log(Level.SEVERE, ("An error (%s) occurred when visiting '%s': %s. " +
                    "Link is marked as visited, but it will not propagate the search.")
                    .formatted(e.getClass().getSimpleName(), hyperlink.getUri(), e.getMessage()));
        }
        return response;
    }

    private String extractBodyWithoutNewLines(HttpResponse<String> response) {
        /*
         * Ideally, this would be done with some sort of DOM parser library.
         * I tried parsing the responses into a DocumentBuilder via a DocumentBuilderFactory using the hyperlink uris,
         * however the `org.w3c.dom` API is too strict and can't handle unassigned html attributes, which most websites seem to deploy
         *
         * I have to admit here, my knowledge of HTML and what options the JVM has to interact with it is limited as I've never had to do
         * this sort of parsing in pure JVM before, so maybe there is another way I have not come across while researching this issue.
         */

        if (response == null) {
            return null;
        }

        String responseStringToParse = response.body();
        if (responseStringToParse == null) {
            responseStringToParse = "";
        }
        /*
         * I have omitted error handling of the response here.
         * One could parse the status code and decide what to do in each case. For this sort of demo application, I did not deem it a necessity.
         *
         * Regarding redirects, the scraper will rely on the http client to handle redirects as configured.
         * Just looking at the location header and blindly redirecting seems like a security risk upon closer inspection.
         * Especially when a redirect happens from https to http.
         * 
         * This also means, the content of the redirection links will be counted as the content of the original link. 
         * This might not be the desired behavior but it seems arbitrary to me which rule to follow. 
         * Perhaps both should be counted so as not to visit the link potentially twice but this again seems to devolve into too much complexity for the demo application.
         */

        // flatten response to one line so the regex can match originally-multi-line <a> tags
        return responseStringToParse.replaceAll("[\n\r]", "");
    }

    /*
     * Just a regex matcher parsing the response body looking for hyperlink html tags.
     */
    private Set<Hyperlink> parseHyperlinksFromResponse(String responseBody) {
        Matcher regexMatcher = ABSOLUTE_HYPERLINK_REGEX.matcher(responseBody);

        Set<Hyperlink> parsedLinks = new HashSet<>();
        while (regexMatcher.find()) {
            String hrefMatch = regexMatcher.group(1).trim();
            String labelMatch = regexMatcher.group(2).replaceAll("\\s+", " ").trim();

            URI parsedUri = UriUtils.tryParseUri(hrefMatch);
            if (parsedUri != null) {
                parsedLinks.add(new Hyperlink(parsedUri, labelMatch));
            }
        }

        return parsedLinks;
    }

    /*
     * this method will wait for all created scrapers to finish and then collect all hyperlinks found by all of them
     */
    private List<Hyperlink> propagateLinkScraping(Set<Hyperlink> urisFound) {
        var newUrisFound = urisFound
                .stream()
                .filter(hyperlink::sharesDomainWith)
                .filter(this::isNew)
                .toList();

        var propagatedUris = newUrisFound
                .stream()
                .map(this::propagateToNewScraperForHyperlink)
                .filter(Objects::nonNull)
                .map(PropagatingHyperlinkWebScraperWorker::scrape)
                .map(HyperlinkWebScrapingResult::result)
                .flatMap(Collection::stream);

        return Stream.concat(newUrisFound.stream(), propagatedUris).toList();
    }

    private boolean isNew(Hyperlink hyperlink) {
        synchronized (knownUris) {
            if(knownUris.isEmpty()) {
                knownUris.computeIfAbsent(this.hyperlink.getUri(), uri -> new HashSet<>()).add(this.hyperlink.getLabel());
            }
            if (knownUris.containsKey(hyperlink.getUri())) {
                // The url could have multiple different labels
                knownUris.get(hyperlink.getUri()).add(hyperlink.getLabel());
                // Prevent visiting the URI again
                return false;
            }
            knownUris.computeIfAbsent(hyperlink.getUri(), uri -> new HashSet<>()).add(hyperlink.getLabel());
            return true;
        }
    }

    private void validateRequiredParameters(Hyperlink hyperlink, PropagatingHyperlinkWebScraperService service, long maxScrapingPropagationDurationMinutes) {
        if(service == null) {
            throw new IllegalStateException("WebScraperService is null. Cannot send web requests or schedule workers without it.");
        }
        if(hyperlink == null) {
            throw new IllegalStateException("Hyperlink is null. Cannot scrape web address 'null'.");
        }
        if(maxScrapingPropagationDurationMinutes < 0) {
            throw new IllegalStateException("Time duration must not be negative.");
        }
    }

    @Override
    public HyperlinkWebScrapingResult handleExecutionException(Exception exception) {
        logger.log(Level.SEVERE, ("An error occurred during multi-thread execution: %s. " +
                "Resulting list might be incomplete.").formatted(exception.getMessage()));

        return new HyperlinkWebScrapingResult(Collections.emptyList());
    }
}