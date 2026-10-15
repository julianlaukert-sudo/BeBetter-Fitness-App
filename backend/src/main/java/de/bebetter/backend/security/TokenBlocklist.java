package de.bebetter.backend.security;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

// Gesperrte Token-IDs nach Logout, bis zu ihrem Ablauf (wie Referenz).
// Bekannte Grenze: liegt im Speicher dieses Prozesses, nach einem Neustart ist die Liste leer.
@Component
public class TokenBlocklist {

    private final Map<String, Instant> blocked = new ConcurrentHashMap<>();
    private final Clock clock;

    public TokenBlocklist(Clock clock) {
        this.clock = clock;
    }

    public void block(String tokenId, Instant expiresAt) {
        removeExpired();
        if (tokenId != null && expiresAt != null) {
            blocked.put(tokenId, expiresAt);
        }
    }

    public boolean isBlocked(String tokenId) {
        return tokenId != null && blocked.containsKey(tokenId);
    }

    private void removeExpired() {
        Instant now = Instant.now(clock);
        blocked.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
    }
}