package me.borzan.linkscraper.application;

import me.borzan.linkscraper.util.ArgsUtils;
import me.borzan.linkscraper.util.LoggingUtils;
import me.borzan.linkscraper.web.hyperlink.Hyperlink;
import me.borzan.linkscraper.web.hyperlink.PropagatingHyperlinkWebScraperService;

import java.net.URI;
import java.util.List;
import java.util.logging.Level;

public class LinkScraperApplication {
    private static final int CONNECTION_TIMEOUT_SECONDS = 5;
    private static final int MAX_DURATION_MINUTES = 1;

    public static void main(String[] args) {
        LoggingUtils.setRootLogLevel(Level.INFO); // Set to INFO level for some crude logs.

        URI websiteUri = ArgsUtils.tryParseWebsiteFromArgs(args);

        PropagatingHyperlinkWebScraperService service = new PropagatingHyperlinkWebScraperService(CONNECTION_TIMEOUT_SECONDS, MAX_DURATION_MINUTES);
        List<Hyperlink> allRecursiveDomainUris = service.scrapeUri(websiteUri).result().stream().sorted().toList();

        System.out.printf("Found %d unique URL-label pairs when visiting from '%s'%n", allRecursiveDomainUris.size(), websiteUri);
        System.out.println("Here they are, sorted by label in the format '<label>' ('<URL>'):");
        allRecursiveDomainUris.stream()
                .map(Hyperlink::toString)
                .forEach(System.out::println);
    }
}