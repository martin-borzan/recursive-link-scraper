package me.borzan.linkscraper.application;

import me.borzan.linkscraper.util.ArgsUtils;
import me.borzan.linkscraper.util.LoggingUtils;
import me.borzan.linkscraper.util.WebUtils;
import me.borzan.linkscraper.web.Hyperlink;
import me.borzan.linkscraper.web.WebScraperService;

import java.net.URI;
import java.util.List;
import java.util.logging.Level;

public class LinkScraperApplication {
    public static void main(String[] args) {
        LoggingUtils.setRootLogLevel(Level.SEVERE);

        URI websiteUri = ArgsUtils.tryParseWebsiteFromArgs(args);

        List<Hyperlink> allRecursiveDomainUris = WebScraperService.visitDomain(websiteUri);

        System.out.printf("Found %d unique URL-label pairs (including the initial url) for the domain '%s' when visiting from '%s'%n",
                allRecursiveDomainUris.size(), WebUtils.extractDomain(websiteUri), websiteUri);
        System.out.println("Here they are, sorted by label in the format <label> (<URL>):");
        allRecursiveDomainUris.stream()
                .map(Hyperlink::toString)
                .forEach(System.out::println);
    }
}