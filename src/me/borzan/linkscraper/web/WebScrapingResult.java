package me.borzan.linkscraper.web;

/*
 * A marker/wrapper interface to facilitate implementing Scraper logic that returns custom results.
 */
public interface WebScrapingResult<T> {
    T result();
}
