package me.borzan.linkscraper.web.hyperlink;

import me.borzan.linkscraper.web.WebScrapingResult;

import java.util.List;

/*
 * A wrapper record for a list of hyperlinks
 */
public record HyperlinkWebScrapingResult(List<Hyperlink> result) implements WebScrapingResult<List<Hyperlink>> {}
