package me.borzan.linkscraper.util;

import java.net.URI;
import java.util.Arrays;

public class WebUtils {
    public static String extractDomain(URI uri) {
        if (uri.getHost() == null) {
            // This should not happen for valid website URIs so there is no worry if
            // the entire URI is returned, it's a malformed domain ether way I think
            return uri.toString();
        }

        // I feel like there must be a better way to parse the domain, but I couldn't come up with one.
        String[] domainParts = uri.getHost().split("\\.");
        return Arrays.stream(domainParts)
                .skip(Math.max(0, domainParts.length - 2))
                .reduce((secondLevelDomain, topLevelDomain) -> secondLevelDomain + "." + topLevelDomain)
                .orElse(uri.getHost());
    }
}
