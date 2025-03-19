package me.borzan.linkscraper.web.hyperlink;

import java.net.URI;
import java.util.Arrays;
import java.util.Objects;

public final class Hyperlink implements Comparable<Hyperlink> {
    private final URI uri;
    private final String label;
    private final String domain;

    public Hyperlink(URI uri, String label) {
        this.uri = uri;
        this.label = label;
        this.domain = extractDomain();
    }

    @Override
    public int compareTo(Hyperlink other) {
        return this.label.compareTo(other.label);
    }

    @Override
    public String toString() {
        return ("'%s' ('%s')").formatted(label.isBlank() ? "<no label>" : label, uri.toString());
    }

    public boolean sharesDomainWith(Hyperlink other) {
        return Objects.equals(this.domain, other.domain);
    }

    private String extractDomain() {
        if (this.uri.getHost() == null) {
            return uri.toString();
        }

        String[] domainParts = uri.getHost().split("\\.");
        return Arrays.stream(domainParts)
                .skip(Math.max(0, domainParts.length - 2))
                .reduce((secondLevelDomain, topLevelDomain) -> secondLevelDomain + "." + topLevelDomain)
                .orElse(uri.getHost());
    }

    public URI getUri() {
        return this.uri;
    }

    public String getLabel() {
        return this.label;
    }
}
