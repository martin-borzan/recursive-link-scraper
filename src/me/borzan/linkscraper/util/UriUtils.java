package me.borzan.linkscraper.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UriUtils {
    private static final Logger LOGGER = Logger.getLogger(UriUtils.class.getName());

    public static URI tryParseUri(String input) {
        try {
            return new URI(input);
        } catch (URISyntaxException e) {
            LOGGER.log(Level.SEVERE, "Could not parse a URI from '%s'".formatted(input));
            return null;
        }
    }
}
