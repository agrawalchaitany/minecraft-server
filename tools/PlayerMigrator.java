import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

// Moves player data between offline UUIDs (v3, derived from the name) and online UUIDs (v4, Mojang accounts).
public class PlayerMigrator {
    static final int CHANGES_PENDING = 3;

    private static final Pattern UUID_RE =
        Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    private static final Pattern JSON_OBJECT = Pattern.compile("\\{[^{}]*}");
    private static final Pattern JSON_UUID = Pattern.compile("\"uuid\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern JSON_NAME = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
    private static final List<String> PLAYER_LISTS = List.of("ops.json", "whitelist.json", "banned-players.json");

    private final Path data;
    private final boolean toOnline;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final Map<String, Optional<String>> onlineIds = new HashMap<>();

    private PlayerMigrator(Path data, boolean toOnline) {
        this.data = data;
        this.toOnline = toOnline;
    }

    /** args: <dataDir> <online|offline> [--apply]; returns the process exit code. */
    static int run(String[] args) throws Exception {
        if (args.length < 2 || !Set.of("online", "offline").contains(args[1])) {
            System.err.println("usage: McTool migrate-players <dataDir> <online|offline> [--apply]");
            return 2;
        }
        boolean apply = args.length > 2 && args[2].equals("--apply");
        return new PlayerMigrator(Path.of(args[0]), args[1].equals("online")).migrate(apply);
    }

    private int migrate(boolean apply) throws Exception {
        List<Path> dirs = playerDirs();
        Set<String> ids = new TreeSet<>();
        for (Path dir : dirs) for (Path f : list(dir)) {
            String id = idOf(f);
            if (id != null) ids.add(id);
        }
        Map<String, String> names = knownNames(dirs, ids);

        int wantVersion = toOnline ? 4 : 3;
        Map<String, String> moves = new LinkedHashMap<>();
        for (String id : ids) {
            if (UUID.fromString(id).version() == wantVersion) continue;
            String name = names.get(id);
            if (name == null) {
                log("skip " + id + ": username unknown");
                continue;
            }
            Optional<String> target = toOnline ? onlineId(name) : Optional.of(offlineId(name));
            if (target.isEmpty()) {
                log("keep " + name + ": no official account with this name, data stays under its offline id");
                continue;
            }
            if (!target.get().equals(id)) {
                moves.put(id, target.get());
                log((apply ? "move " : "will move ") + name + ": " + id + " -> " + target.get());
            }
        }

        if (moves.isEmpty()) return 0;
        if (!apply) return CHANGES_PENDING;

        Path conflicts = data.resolve("player-migration-conflicts")
            .resolve(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));
        for (Path dir : dirs) for (Path f : list(dir)) {
            String id = idOf(f);
            if (id == null || !moves.containsKey(id)) continue;
            Path target = dir.resolve(f.getFileName().toString().replace(id, moves.get(id)));
            if (Files.exists(target)) {
                boolean sourceNewer = mtime(f).compareTo(mtime(target)) > 0;
                Path loser = sourceNewer ? target : f;
                Path aside = conflicts.resolve(data.relativize(loser));
                Files.createDirectories(aside.getParent());
                Files.move(loser, aside);
                log("conflict: kept newer " + (sourceNewer ? f : target).getFileName() + ", moved other to " + data.relativize(aside));
                if (!sourceNewer) continue;
            }
            Files.move(f, target);
        }
        for (String list : PLAYER_LISTS) {
            Path p = data.resolve(list);
            if (!Files.exists(p)) continue;
            String json = Files.readString(p);
            for (var m : moves.entrySet()) json = json.replace(m.getKey(), m.getValue());
            Files.writeString(p, json);
        }
        // Stale cached name->id pairs would make commands like /op resolve to the old id.
        Files.deleteIfExists(data.resolve("usercache.json"));
        log("migrated " + moves.size() + " player(s)");
        return 0;
    }

    private List<Path> playerDirs() throws IOException {
        Path world = data.resolve(levelName());
        return Stream.of("players/data", "players/stats", "players/advancements", "playerdata", "stats", "advancements")
            .map(world::resolve).filter(Files::isDirectory).toList();
    }

    private String levelName() throws IOException {
        Path props = data.resolve("server.properties");
        if (Files.exists(props)) {
            for (String line : Files.readAllLines(props)) {
                if (line.startsWith("level-name=") && line.length() > 11) return line.substring(11).trim();
            }
        }
        return "world";
    }

    private Map<String, String> knownNames(List<Path> dirs, Set<String> listedIds) throws IOException {
        Map<String, String> names = new HashMap<>();
        for (String file : Stream.concat(Stream.of("usercache.json"), PLAYER_LISTS.stream()).toList()) {
            Path p = data.resolve(file);
            if (!Files.exists(p)) continue;
            Matcher obj = JSON_OBJECT.matcher(Files.readString(p));
            while (obj.find()) {
                Matcher u = JSON_UUID.matcher(obj.group()), n = JSON_NAME.matcher(obj.group());
                if (u.find() && n.find() && UUID_RE.matcher(u.group(1)).matches()) {
                    names.put(u.group(1), n.group(1));
                    if (!file.equals("usercache.json")) listedIds.add(u.group(1));
                }
            }
        }
        // The name saved inside the player's own data file is the most reliable source.
        for (Path dir : dirs) for (Path f : list(dir)) {
            String id = idOf(f);
            if (id != null && f.getFileName().toString().endsWith(".dat")) {
                String name = lastKnownName(f);
                if (name != null) names.put(id, name);
            }
        }
        return names;
    }

    private static String lastKnownName(Path dat) {
        try (InputStream in = new GZIPInputStream(Files.newInputStream(dat))) {
            byte[] b = in.readAllBytes();
            byte[] key = "lastKnownName".getBytes(StandardCharsets.UTF_8);
            outer:
            for (int i = 0; i + key.length + 2 <= b.length; i++) {
                for (int j = 0; j < key.length; j++) if (b[i + j] != key[j]) continue outer;
                int at = i + key.length, len = ((b[at] & 0xFF) << 8) | (b[at + 1] & 0xFF);
                if (at + 2 + len <= b.length) return new String(b, at + 2, len, StandardCharsets.UTF_8);
            }
        } catch (IOException ignored) {
        }
        return null;
    }

    static String offlineId(String name) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8)).toString();
    }

    private Optional<String> onlineId(String name) throws IOException, InterruptedException {
        if (onlineIds.containsKey(name)) return onlineIds.get(name);
        URI uri = URI.create("https://api.mojang.com/users/profiles/minecraft/" + URLEncoder.encode(name, StandardCharsets.UTF_8));
        HttpRequest req = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(15)).GET().build();
        for (int attempt = 1; ; attempt++) {
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            int code = res.statusCode();
            if (code == 200) {
                Matcher m = Pattern.compile("\"id\"\\s*:\\s*\"([0-9a-f]{32})\"").matcher(res.body());
                if (!m.find()) throw new IOException("unexpected Mojang response for " + name);
                String h = m.group(1);
                String id = h.substring(0, 8) + "-" + h.substring(8, 12) + "-" + h.substring(12, 16) + "-"
                    + h.substring(16, 20) + "-" + h.substring(20);
                onlineIds.put(name, Optional.of(id));
                return onlineIds.get(name);
            }
            if (code == 204 || code == 404) {
                onlineIds.put(name, Optional.empty());
                return Optional.empty();
            }
            if (code == 429 && attempt < 5) {
                Thread.sleep(15_000L * attempt);
                continue;
            }
            throw new IOException("Mojang lookup for " + name + " failed with HTTP " + code);
        }
    }

    private static List<Path> list(Path dir) throws IOException {
        try (Stream<Path> s = Files.list(dir)) {
            return s.filter(Files::isRegularFile).toList();
        }
    }

    private static String idOf(Path f) {
        Matcher m = UUID_RE.matcher(f.getFileName().toString());
        return m.lookingAt() ? m.group() : null;
    }

    private static FileTime mtime(Path p) throws IOException {
        return Files.getLastModifiedTime(p);
    }

    private static void log(String msg) {
        System.out.println("[migrate-players] " + msg);
    }
}
