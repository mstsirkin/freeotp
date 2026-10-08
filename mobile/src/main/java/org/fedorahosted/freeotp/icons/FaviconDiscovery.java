/* SPDX-License-Identifier: Apache-2.0 */
package org.fedorahosted.freeotp.icons;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Website icon discovery, independent of Android for regression tests. */
public final class FaviconDiscovery {
    private static final Pattern TAG = Pattern.compile("<(link|base)\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern ATTRIBUTE = Pattern.compile(
            "([\\w-]+)\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s>]+))");

    public static URI website(String input) {
        String value = input.trim();
        if (!value.contains("://")) value = "https://" + value;
        URI uri = URI.create(value);
        if (!isWeb(uri) || uri.getUserInfo() != null) throw new IllegalArgumentException("Invalid website URL");
        return uri;
    }

    public static boolean isWeb(URI uri) {
        return uri.getHost() != null && ("https".equalsIgnoreCase(uri.getScheme())
                || "http".equalsIgnoreCase(uri.getScheme()));
    }

    private static String attribute(String tag, String key) {
        Matcher attributes = ATTRIBUTE.matcher(tag);
        while (attributes.find()) {
            if (key.equalsIgnoreCase(attributes.group(1))) {
                String value = attributes.group(2);
                if (value == null) value = attributes.group(3);
                if (value == null) value = attributes.group(4);
                return value.replace("&amp;", "&").replace("&#38;", "&");
            }
        }
        return "";
    }

    public static List<URI> candidates(URI page, String html) {
        // Ignore commented-out tags. Honor the first base URL, including when after a link.
        html = html.replaceAll("(?s)<!--.*?-->", "");
        URI base = page;
        Matcher tags = TAG.matcher(html);
        while (tags.find()) {
            if (!tags.group(1).equalsIgnoreCase("base")) continue;
            try {
                URI candidate = page.resolve(attribute(tags.group(), "href"));
                if (isWeb(candidate)) base = candidate;
            } catch (IllegalArgumentException ignored) { }
            break;
        }
        LinkedHashSet<URI> icons = new LinkedHashSet<>();
        tags.reset();
        while (tags.find() && icons.size() < 8) {
            if (!tags.group(1).equalsIgnoreCase("link")) continue;
            String rel = attribute(tags.group(), "rel").toLowerCase(Locale.ROOT);
            if (!rel.matches(".*(?:^|\\s)(?:icon|apple-touch-icon|apple-touch-icon-precomposed)(?:\\s|$).*")) continue;
            String href = attribute(tags.group(), "href");
            if (href.isEmpty()) continue;
            try {
                URI icon = base.resolve(href);
                if (isWeb(icon) && icon.getUserInfo() == null) icons.add(icon);
            } catch (IllegalArgumentException ignored) { }
        }
        icons.add(page.resolve("/favicon.ico"));
        return new ArrayList<>(icons);
    }
}
