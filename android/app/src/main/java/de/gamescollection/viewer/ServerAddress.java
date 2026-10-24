package de.gamescollection.viewer;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/** Origin validation shared by setup, top-level navigation and resource loading. */
final class ServerAddress {
    private ServerAddress() {}

    static String normalize(String input, boolean allowHttp) {
        String value = input.trim();
        if (value.isEmpty()) throw new IllegalArgumentException("Bitte gib eine Serveradresse ein.");
        if (!value.contains("://")) value = (allowHttp ? "http://" : "https://") + value;
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
            if (!scheme.equals("https") && !scheme.equals("http")) {
                throw new IllegalArgumentException("Bitte verwende eine HTTP- oder HTTPS-Adresse.");
            }
            if (scheme.equals("http") && !allowHttp) {
                throw new IllegalArgumentException("Aktiviere für diese Adresse ausdrücklich HTTP.");
            }
            if (uri.getHost() == null || uri.getUserInfo() != null || uri.getPort() > 65535
                    || uri.getRawAuthority().endsWith(":")) {
                throw new IllegalArgumentException("Die Serveradresse ist ungültig. Zugangsdaten gehören in die Anmeldemaske.");
            }
            if (uri.getQuery() != null || uri.getFragment() != null
                    || (uri.getPath() != null && !uri.getPath().isEmpty() && !uri.getPath().equals("/"))) {
                throw new IllegalArgumentException("Bitte gib nur die Serveradresse mit optionalem Port ein, ohne Unterpfad.");
            }
            int port = uri.getPort();
            if (port == (scheme.equals("https") ? 443 : 80)) port = -1;
            return new URI(scheme, null, uri.getHost().toLowerCase(Locale.ROOT), port, "/", null, null).toASCIIString();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Die Serveradresse ist ungültig.");
        }
    }

    static boolean sameOrigin(String server, String target) {
        try {
            URI base = new URI(server);
            URI uri = new URI(target);
            return uri.getUserInfo() == null && base.getScheme().equalsIgnoreCase(uri.getScheme())
                    && base.getHost().equalsIgnoreCase(uri.getHost()) && port(base) == port(uri);
        } catch (URISyntaxException | NullPointerException e) {
            return false;
        }
    }

    private static int port(URI uri) {
        return uri.getPort() == -1 ? ("https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80) : uri.getPort();
    }
}
