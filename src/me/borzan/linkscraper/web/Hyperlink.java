package me.borzan.linkscraper.web;

import me.borzan.linkscraper.util.WebUtils;

import java.net.URI;
import java.util.Objects;

public record Hyperlink(URI uri, String label) implements Comparable<Hyperlink> {
    @Override
    public int compareTo(Hyperlink other) {
        return this.label.compareTo(other.label);
    }

    @Override
    public String toString() {
        return ("'%s' ('%s')").formatted(label.isBlank() ? "<no label>" : label, uri.toString());
    }

    public boolean sharesDomainWith(Hyperlink other) {
        return Objects.equals(WebUtils.extractDomain(this.uri), WebUtils.extractDomain(other.uri));
    }
}
