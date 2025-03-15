package me.borzan.linkscraper.util;

import java.net.URI;

public class ArgsUtils {
    public static URI tryParseWebsiteFromArgs(String[] args) {
        if (args.length != 1) {
            printUsagePrompt();
            System.exit(1);
        }

        URI parsedUri = UriUtils.tryParseUri(args[0]);
        if (parsedUri == null) {
            System.exit(1);
        }

        return parsedUri;
    }

    private static void printUsagePrompt() {
        System.out.println("""
                     Run this application with one argument.
                     Example Usage:
                         java -jar ArgsParser.jar <your-website-url>
                """);
    }
}
