package me.borzan.linkscraper.web.hyperlink;

import me.borzan.linkscraper.web.WebScraperService;
import me.borzan.linkscraper.web.WebScraperWorker;

import java.net.URI;

/*
 * This service's purpose is to facilitate recursive scraping and limit the recursion by time.
 * An implementation that limits the recursion by depth or by number of results for example would be trivial to adapt in a similar manner.
 *
 * It is final as it serves a specific purpose, with the same reasoning as to why the PropagatingHyperlinkWebScraperWorker is final.
 * I could not think of a reason to extend this class instead of extending the abstract WebScraperService
 */
public final class PropagatingHyperlinkWebScraperService extends WebScraperService<HyperlinkWebScrapingResult> {
    private final int maxScrapingDurationMinutes;

    public PropagatingHyperlinkWebScraperService(int connectionTimeoutSeconds, int maxScrapingPropagationDurationMinutes) {
        super(connectionTimeoutSeconds);
        this.maxScrapingDurationMinutes = maxScrapingPropagationDurationMinutes;
    }

    WebScraperWorker<HyperlinkWebScrapingResult> createNewRootScraperForHyperlink(Hyperlink hyperlink) {
        return new PropagatingHyperlinkWebScraperWorker(hyperlink, this, this.maxScrapingDurationMinutes);
    }

    @Override
    protected WebScraperWorker<HyperlinkWebScrapingResult> createScraperForUri(URI websiteUri) {
        return createNewRootScraperForHyperlink(new Hyperlink(websiteUri, ""));
    }
}
