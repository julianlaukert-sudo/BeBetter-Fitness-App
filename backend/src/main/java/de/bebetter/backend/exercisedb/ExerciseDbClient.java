package de.bebetter.backend.exercisedb;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PreDestroy;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
// KI-Log Nr. 12
@Component
public class ExerciseDbClient {

    private static final Duration TTL = Duration.ofHours(6);
    private static final int PAGE_SIZE = 25;
    private static final int MAX_PAGES = 4;
    private static final long MIN_GAP_NANOS = 500_000_000L;   // 500 ms zwischen Aufrufen

    private record Entry<T>(T value, Instant until) {}

    private final Map<String, Entry<CatalogExercise>> details = new ConcurrentHashMap<>();
    private final Map<String, Entry<List<CatalogExercise>>> lists = new ConcurrentHashMap<>();
    private final List<RestClient> clients;
    private final Clock clock;
    private Instant blockedUntil = Instant.EPOCH;
    private long lastRequest;
    private final ExecutorService workers = Executors.newFixedThreadPool(5);

    public ExerciseDbClient(
            @Value("${EXERCISEDB_URLS:https://oss.exercisedb.dev/api/v1}") String urls,
            RestClient.Builder builder, Clock clock) {
        this.clock = clock;
        clients = Arrays.stream(urls.split(",")).map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(url -> builder.clone().baseUrl(url).build()).toList();
        if (clients.isEmpty()) throw new IllegalArgumentException("ExerciseDB URL is missing.");
    }

    @PreDestroy
    public void close() { workers.shutdownNow(); }

    private boolean fresh(Entry<?> e) {
        return e != null && e.until().isAfter(clock.instant());
    }

    /** Einzelne Übung (Cache 6 h). */
    public CatalogExercise exercise(String id) {
        var cached = details.get(id);
        if (fresh(cached)) return cached.value();
        JsonNode body = call(c -> c.get().uri("/exercises/{id}", id)
                .retrieve().body(JsonNode.class));
        CatalogExercise value = read(body.get("data"));
        details.put(id, new Entry<>(value, clock.instant().plus(TTL)));
        return value;
    }

    /** Liste pro Körperteil, höchstens 4 Seiten, IDs dedupliziert. */
    public List<CatalogExercise> byBodyPart(String part) {
        var cached = lists.get(part);
        if (fresh(cached)) return cached.value();
        var result = new LinkedHashMap<String, CatalogExercise>();
        var cursors = new HashSet<String>();
        String cursor = null;
        for (int page = 0; page < MAX_PAGES; page++) {
            String after = cursor;
            JsonNode body = call(c -> c.get().uri(u -> {
                u.path("/exercises").queryParam("bodyParts", part).queryParam("limit", PAGE_SIZE);
                if (after != null) u.queryParam("after", after);
                return u.build();
            }).retrieve().body(JsonNode.class));
            JsonNode data = body.get("data");
            if (!data.isArray()) throw new ExerciseDbException("Invalid exercise list.");
            for (JsonNode item : data) {
                CatalogExercise value = read(item);
                result.putIfAbsent(value.exerciseId(), value);
            }
            String next = body.path("meta").path("nextCursor").asText("");
            if (data.isEmpty() || next.isBlank() || !cursors.add(next)) break;
            cursor = next;
        }
        var values = List.copyOf(result.values());
        Instant until = clock.instant().plus(TTL);
        values.forEach(u -> details.put(u.exerciseId(), new Entry<>(u, until))); // füllt Detailcache
        lists.put(part, new Entry<>(values, until));
        return values;
    }

    /** Kandidaten für mehrere Körperteile – nacheinander geladen. */
    public Map<String, List<CatalogExercise>> candidates(Collection<String> parts) {
        var result = new LinkedHashMap<String, List<CatalogExercise>>();
        for (String part : parts) result.put(part, byBodyPart(part));
        return result;
    }

    /** Details für viele IDs (max. 5 Worker). Fehlende Details werden einfach weggelassen. */
    public Map<String, CatalogExercise> exercises(Collection<String> ids) {
        var pending = new LinkedHashMap<String, CompletableFuture<CatalogExercise>>();
        new HashSet<>(ids).forEach(id -> pending.put(id,
                CompletableFuture.supplyAsync(() -> exercise(id), workers)));
        var result = new LinkedHashMap<String, CatalogExercise>();
        pending.forEach((id, f) -> {
            try { result.put(id, f.join()); }
            catch (CompletionException ex) { /* Vorgaben bleiben ohne Details sichtbar. */ }
        });
        return result;
    }

    private synchronized JsonNode call(Function<RestClient, JsonNode> action) {
        if (clock.instant().isBefore(blockedUntil))
            throw new ExerciseDbException("ExerciseDB rate limit. Please try later.");
        RuntimeException last = null;
        for (RestClient client : clients) {
            try {
                long wait = MIN_GAP_NANOS - (System.nanoTime() - lastRequest);
                if (wait > 0) TimeUnit.NANOSECONDS.sleep(wait);
                lastRequest = System.nanoTime();
                JsonNode body = action.apply(client);
                if (body == null || !body.hasNonNull("data")
                        || (body.has("success") && !body.path("success").asBoolean()))
                    throw new IllegalStateException("Invalid ExerciseDB response.");
                return body;
            } catch (HttpClientErrorException.TooManyRequests ex) {
                // Rate Limit wird NICHT über die Ersatzadresse umgangen
                var headers = ex.getResponseHeaders();
                blockedUntil = RetryAfterPolicy.until(
                        headers == null ? null : headers.getFirst("Retry-After"),
                        clock.instant());
                throw new ExerciseDbException("ExerciseDB rate limit. Please try later.", ex);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new ExerciseDbException("ExerciseDB request interrupted.", ex);
            } catch (RuntimeException ex) {
                if (last != null) ex.addSuppressed(last);
                last = ex;
            }
        }
        throw new ExerciseDbException("Exercises could not be loaded.", last);
    }

    private static CatalogExercise read(JsonNode n) {
        if (n == null || !n.isObject() || n.path("exerciseId").asText("").isBlank()
                || n.path("name").asText("").isBlank())
            throw new ExerciseDbException("Invalid exercise.");
        return new CatalogExercise(n.path("exerciseId").asText(), n.path("name").asText(),
                strings(n, "bodyParts"), strings(n, "targetMuscles"), strings(n, "equipments"),
                n.path("gifUrl").asText(null));
    }

    private static List<String> strings(JsonNode n, String field) {
        var result = new ArrayList<String>();
        if (n.path(field).isArray()) n.path(field).forEach(x -> result.add(x.asText()));
        return List.copyOf(result);
    }
}