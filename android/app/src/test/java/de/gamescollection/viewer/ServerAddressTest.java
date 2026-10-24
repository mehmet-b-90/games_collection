package de.gamescollection.viewer;

import org.junit.Test;
import static org.junit.Assert.*;

public class ServerAddressTest {
    @Test public void normalizesHttpsAndDefaultPorts() {
        assertEquals("https://games.example.org/", ServerAddress.normalize(" Games.Example.org ", false));
        assertEquals("https://games.example.org/", ServerAddress.normalize("https://games.example.org:443/", false));
    }

    @Test public void permitsExplicitHttpAndIpv6() {
        assertEquals("http://192.168.1.20:7000/", ServerAddress.normalize("http://192.168.1.20:7000", true));
        assertEquals("https://[::1]:7000/", ServerAddress.normalize("https://[::1]:7000", false));
    }

    @Test public void infersProtocolFromHttpPermission() {
        assertEquals("https://192.168.1.20:7000/", ServerAddress.normalize("192.168.1.20:7000", false));
        assertEquals("http://192.168.1.20:7000/", ServerAddress.normalize(" 192.168.1.20:7000 ", true));
        assertEquals("http://localhost/", ServerAddress.normalize("localhost:80", true));
        assertEquals("http://[::1]:7000/", ServerAddress.normalize("[::1]:7000", true));
        assertEquals("https://games.example.org/", ServerAddress.normalize("https://games.example.org", true));
    }

    @Test public void rejectsUnsafeOrUnsupportedAddresses() {
        String[] invalid = {"", "http://games.example.org", "file:///tmp/test", "javascript:alert(1)",
                "https://user:password@games.example.org", "https://games.example.org/login",
                "https://games.example.org?token=secret", "https://games.example.org/#test",
                "https://games.example.org:99999", "https://games.example.org:"};
        for (String input : invalid) {
            assertThrows(input, IllegalArgumentException.class, () -> ServerAddress.normalize(input, false));
        }
    }

    @Test public void confinesNavigationToExactOrigin() {
        String base = "https://games.example.org/";
        assertTrue(ServerAddress.sameOrigin(base, "https://games.example.org:443/login?next=/"));
        assertFalse(ServerAddress.sameOrigin(base, "https://games.example.org.evil.test/"));
        assertFalse(ServerAddress.sameOrigin(base, "https://games.example.org:7000/"));
        assertFalse(ServerAddress.sameOrigin(base, "http://games.example.org/"));
        assertFalse(ServerAddress.sameOrigin(base, "https://user@games.example.org/"));
        assertFalse(ServerAddress.sameOrigin(base, "javascript:alert(1)"));
        assertFalse(ServerAddress.sameOrigin(base, "/relative"));
    }
}
