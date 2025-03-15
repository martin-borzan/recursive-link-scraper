package me.borzan.linkscraper.util;

import java.util.logging.Level;
import java.util.logging.Logger;

public class LoggingUtils {
    public static void setRootLogLevel(Level level) {
        Logger rootLogger = Logger.getLogger("");
        rootLogger.setLevel(level);
    }
}
