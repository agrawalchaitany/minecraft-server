import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

// Resolves plugins.txt into a compatible set of plugins (with dependencies) from Modrinth and Hangar.
public class PluginManager {
    static final int NOTHING_TO_DO = 3;
    private static final String UA = "self-hosted-paper-docker/1.0";
    private static final String LOADERS = "[\"paper\",\"spigot\",\"bukkit\"]";

    record Entry(String source, String project, String version) {
        String spec() { return source + ":" + project.toLowerCase(Locale.ROOT); }
    }

    /** One installable plugin version. key = source:projectId, stable across slug renames. */
    record Release(String key, String name, String version, String url, String file, String algorithm, String hash,
                   boolean supportsServer, List<Dep> deps) {}

    record Dep(String source, String project, String versionId, boolean incompatible) {}

    record Want(String source, String project, String version, String versionId, String origin) {
        boolean latest() { return version.equals("latest"); }
    }

    record Locked(String key, String version, String file, String hash, String name, String origin) {}

    record PluginInfo(String name, String main, String apiVersion, List<String> depends, List<String> provides) {}

    private final Path manifest, plugins, managed, lockFile, pending;
    private final String mc;
    private final int serverJava = Runtime.version().feature();
    private final HttpClient http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15)).followRedirects(HttpClient.Redirect.NORMAL).build();
    private final List<String> problems = new ArrayList<>();
    private boolean failures;

    // State for one resolution run.
    private Map<String, Locked> lock = Map.of();
    private boolean preferLocked;
    private final Map<String, Release> picks = new LinkedHashMap<>();
    private final Map<String, Path> jarOf = new HashMap<>();
    private final Map<String, PluginInfo> infoOf = new HashMap<>();
    private final Map<String, String> originOf = new HashMap<>();

    private PluginManager(Path manifest, Path plugins, String mc) {
        this.manifest = manifest;
        this.plugins = plugins;
        this.managed = plugins.resolve(".managed");
        this.lockFile = managed.resolve("lock.tsv");
        this.pending = managed.resolve("pending");
        this.mc = mc;
    }

    /** args: <sync|update|status> <plugins.txt> <pluginsDir> <mcVersion> */
    static int run(String[] a) throws Exception {
        if (a.length < 4) {
            System.err.println("usage: McTool plugins <sync|update|status> <plugins.txt> <pluginsDir> <mcVersion>");
            return 2;
        }
        PluginManager pm = new PluginManager(Path.of(a[1]), Path.of(a[2]), a[3]);
        Files.createDirectories(pm.managed);
        if (a[0].equals("status")) return pm.status();
        // Only one install/update at a time (e.g. `make update-plugins` while the scheduled update runs).
        try (var ch = java.nio.channels.FileChannel.open(pm.managed.resolve(".lock"),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             var held = ch.tryLock()) {
            if (held == null) {
                log("another plugin install/update is already running; try again in a minute");
                return 1;
            }
            return switch (a[0]) {
                case "sync" -> pm.sync();
                case "update" -> pm.update();
                default -> 2;
            };
        }
    }

    // At server start: apply staged updates; re-resolve only if plugins.txt, Minecraft or Java changed.
    private int sync() throws Exception {
        applyPending();
        if (!Files.exists(manifest)) {
            log("no " + manifest + " found, leaving plugins unchanged");
            return 0;
        }
        String stamp = stamp();
        boolean filesPresent = readLock(lockFile).values().stream().allMatch(l -> Files.exists(plugins.resolve(l.file())));
        if (stamp.equals(readStamp(lockFile)) && filesPresent) return 0;

        log("plugins.txt, Minecraft or Java changed: resolving plugins for Minecraft " + mc + " on Java " + serverJava);
        if (stage(true) == 0) applyPending();
        if (!failures) writeLock(lockFile, readLock(lockFile), stamp);
        return 0;
    }

    // Any time (server may be running): resolve newest compatible versions and stage them for the next start.
    private int update() throws Exception {
        if (!Files.exists(manifest)) {
            log("ERROR: " + manifest + " not found");
            return 1;
        }
        int rc = stage(false);
        if (rc == NOTHING_TO_DO) log(failures ? "no updates staged (see warnings above)" : "all plugins are up to date");
        else log("updates will be applied on the next server start");
        return rc == NOTHING_TO_DO && failures ? 1 : rc;
    }

    /** Resolves the full plugin set into pending/ (new jars + lock.tsv). Returns 0 if anything changed. */
    private int stage(boolean preferLocked) throws Exception {
        deleteTree(pending);
        Files.createDirectories(pending);
        this.lock = readLock(lockFile);
        this.preferLocked = preferLocked;

        resolve();
        checkSet();

        // When something failed, keep previously installed plugins rather than removing them.
        Map<String, Locked> next = new LinkedHashMap<>();
        for (Release r : picks.values()) {
            next.put(r.key(), new Locked(r.key(), r.version(), r.file(), r.hash(), r.name(), originOf.getOrDefault(r.key(), "listed")));
        }
        if (failures) {
            for (Locked l : lock.values()) {
                Path jar = plugins.resolve(l.file());
                if (!next.containsKey(l.key()) && Files.exists(jar) && unsuitable(pluginInfo(jar), jar) == null) next.put(l.key(), l);
            }
        }

        List<String> changes = new ArrayList<>();
        for (Locked n : next.values()) {
            Locked o = lock.get(n.key());
            if (o == null) changes.add("+ " + n.name() + " " + n.version() + "  (" + n.origin() + ")");
            else if (!o.version().equals(n.version())) changes.add("~ " + n.name() + " " + o.version() + " -> " + n.version());
        }
        for (Locked o : lock.values()) if (!next.containsKey(o.key())) changes.add("- " + o.name() + " " + o.version());
        problems.forEach(p -> log("WARNING: " + p));

        Set<String> wanted = new HashSet<>();
        next.values().forEach(l -> wanted.add(l.file()));
        for (String jar : jars(pending)) if (!wanted.contains(jar)) Files.delete(pending.resolve(jar));

        if (changes.isEmpty()) {
            deleteTree(pending);
            return NOTHING_TO_DO;
        }
        changes.forEach(PluginManager::log);
        writeLock(pending.resolve("lock.tsv"), next, failures ? readStamp(lockFile) : stamp());
        return 0;
    }

    // ---- resolution ----

    private void resolve() throws IOException {
        Deque<Want> queue = new ArrayDeque<>();
        for (Entry e : readManifest()) queue.add(new Want(e.source(), e.project(), e.version(), null, "listed"));

        Map<String, String> incompatible = new LinkedHashMap<>();
        while (!queue.isEmpty()) {
            Want w = queue.poll();
            if (!w.origin().equals("listed") && picks.containsKey(w.source() + ":" + w.project())) continue;
            Release r;
            try {
                r = acquire(w);
            } catch (Exception ex) {
                String msg = ex.getMessage();
                problem((msg.contains(": ") ? msg : w.project() + ": " + msg) + (w.origin().equals("listed") ? "" : " [" + w.origin() + "]"));
                failures = true;
                continue;
            }
            if (picks.containsKey(r.key())) continue;
            if (!r.supportsServer()) problem(r.name() + " " + r.version() + " does not list support for Minecraft " + mc + "; installing it anyway because it was pinned");
            picks.put(r.key(), r);
            originOf.put(r.key(), w.origin());
            for (Dep d : r.deps()) {
                if (d.incompatible()) incompatible.put(d.source() + ":" + d.project(), r.key());
                else queue.add(new Want(d.source(), d.project(), d.versionId() == null ? "latest" : "exact", d.versionId(),
                    "required by " + r.name()));
            }
        }

        for (var e : incompatible.entrySet()) {
            Release a = picks.get(e.getValue()), b = picks.get(e.getKey());
            if (a == null || b == null) continue;
            boolean aListed = originOf.get(a.key()).equals("listed"), bListed = originOf.get(b.key()).equals("listed");
            Release drop = bListed && !aListed ? a : b;
            problem(a.name() + " declares it is incompatible with " + b.name() + "; not installing " + drop.name());
            remove(drop.key());
        }
    }

    private final Map<String, String> failedKeys = new HashMap<>();

    /**
     * Picks the newest version whose actual jar suits this server (plugin descriptor, Minecraft API, Java).
     * Newer plugin versions only ever raise requirements, so a binary search over the history finds the newest
     * compatible one with a handful of downloads.
     */
    private Release acquire(Want w) throws Exception {
        List<Release> cands = candidates(w);
        Release first = cands.get(0);
        if (failedKeys.containsKey(first.key())) throw new IOException(failedKeys.get(first.key()));
        String why = tryRelease(first);
        if (why == null) return first;

        String newest = first.name() + " " + first.version() + " " + why;
        Release found = null;
        if (w.latest() && w.versionId() == null) {
            List<Path> probes = new ArrayList<>();
            int lo = 1, hi = cands.size() - 1;
            while (lo <= hi) {
                int mid = (lo + hi) >>> 1;
                Release c = cands.get(mid);
                if (tryRelease(c) == null) {
                    found = c;
                    probes.add(jarOf.get(c.key()));
                    hi = mid - 1;
                } else {
                    lo = mid + 1;
                }
            }
            if (found != null) {
                Path keep = jarOf.get(found.key());
                for (Path p : probes) if (!p.equals(keep) && p.startsWith(pending)) Files.deleteIfExists(p);
                log(found.name() + ": using " + found.version() + " because " + newest.substring(found.name().length() + 1));
                return found;
            }
        }
        String hint = why.startsWith("needs Java")
            ? "; if your Minecraft version supports it, set JAVA_VERSION=" + why.replaceAll("\\D*(\\d+).*", "$1") + " in .env"
            : "";
        String msg = (w.latest() ? "no version is compatible with this server (newest: " + newest + ")" : newest) + hint;
        failedKeys.put(first.key(), first.name() + ": " + msg);
        throw new IOException(first.name() + ": " + msg);
    }

    /** Downloads and checks one version; registers it and returns null if suitable, otherwise returns why not. */
    private String tryRelease(Release r) throws Exception {
        Path jar = obtain(r);
        PluginInfo pi = pluginInfo(jar);
        String why = unsuitable(pi, jar);
        if (why == null) {
            jarOf.put(r.key(), jar);
            infoOf.put(r.key(), pi);
        } else if (jar.startsWith(pending)) {
            Files.deleteIfExists(jar);
        }
        return why;
    }

    private String unsuitable(PluginInfo pi, Path jar) {
        if (pi == null) return "is not a valid Paper/Bukkit plugin";
        if (pi.apiVersion() != null && VersionResolver.compare(pi.apiVersion(), mc) > 0) {
            return "needs Minecraft " + pi.apiVersion() + " or newer";
        }
        int java = classJava(jar, pi.main());
        if (java > serverJava) return "needs Java " + java + " (this server runs Java " + serverJava + ")";
        return null;
    }

    /** Reuses the installed jar when the version is unchanged, otherwise downloads and verifies it into pending/. */
    private Path obtain(Release r) throws Exception {
        Locked l = lock.get(r.key());
        if (l != null && l.version().equals(r.version()) && Files.exists(plugins.resolve(l.file()))) return plugins.resolve(l.file());
        Path dest = pending.resolve(r.file());
        if (Files.exists(dest)) return dest;
        if (Files.exists(plugins.resolve(r.file())) && lock.values().stream().noneMatch(x -> x.file().equals(r.file()))) {
            throw new IOException(r.file() + " was added to plugins/ by hand; remove it to let plugins.txt manage it");
        }
        Path tmp = download(r, pending);
        Files.move(tmp, dest, StandardCopyOption.REPLACE_EXISTING);
        return dest;
    }

    private void remove(String key) throws IOException {
        picks.remove(key);
        Path jar = jarOf.remove(key);
        infoOf.remove(key);
        if (jar != null && jar.startsWith(pending)) Files.deleteIfExists(jar);
    }

    /** Versions to consider, best first: an exact pin alone, or stable compatible versions (installed one first in sync). */
    private List<Release> candidates(Want w) throws Exception {
        return switch (w.source()) {
            case "modrinth" -> modrinth(w);
            case "hangar" -> hangar(w);
            default -> throw new IOException("unknown source '" + w.source() + "' (use modrinth or hangar)");
        };
    }

    private List<Release> preferInstalled(List<Release> list) {
        if (!preferLocked || list.isEmpty()) return list;
        Locked l = lock.get(list.get(0).key());
        if (l == null) return list;
        List<Release> out = new ArrayList<>(list);
        for (Release r : list) {
            if (r.version().equals(l.version())) {
                out.remove(r);
                out.add(0, r);
            }
        }
        return out;
    }

    private List<Release> modrinth(Want w) throws Exception {
        String p = enc(w.project());
        if (w.versionId() != null) return List.of(modrinthRelease(getJson("https://api.modrinth.com/v2/version/" + enc(w.versionId()))));
        if (!w.latest()) return List.of(modrinthRelease(getJson("https://api.modrinth.com/v2/project/" + p + "/version/" + enc(w.version()))));

        List<Object> all = Json.arr(getJson("https://api.modrinth.com/v2/project/" + p + "/version?loaders=" + enc(LOADERS)
            + "&game_versions=" + enc("[\"" + mc + "\"]")));
        List<Object> stable = all.stream().filter(v -> "release".equals(Json.str(Json.at(v, "version_type")))).toList();
        if (stable.isEmpty()) {
            throw new IOException(all.isEmpty()
                ? "no version supports Minecraft " + mc + " yet"
                : "no stable version supports Minecraft " + mc + " (only beta/alpha); pin one in plugins.txt to use it");
        }
        List<Release> out = new ArrayList<>();
        for (Object v : stable) out.add(modrinthRelease(v));
        return preferInstalled(out);
    }

    private final Map<String, String> modrinthNames = new HashMap<>();

    private Release modrinthRelease(Object v) throws Exception {
        if (Json.at(v, "id") == null) throw new IOException("version not found (check the exact version name on modrinth.com)");
        String projectId = Json.str(Json.at(v, "project_id"));
        if (!modrinthNames.containsKey(projectId)) {
            Object proj = getJson("https://api.modrinth.com/v2/project/" + enc(projectId));
            modrinthNames.put(projectId, Objects.requireNonNullElse(Json.str(Json.at(proj, "title")), projectId));
        }
        List<Object> loaders = Json.arr(Json.at(v, "loaders"));
        if (loaders.stream().noneMatch(l -> List.of("paper", "spigot", "bukkit", "purpur", "folia").contains(Json.str(l)))) {
            throw new IOException("version " + Json.str(Json.at(v, "version_number")) + " is not a Paper/Spigot/Bukkit plugin");
        }
        List<Object> files = Json.arr(Json.at(v, "files"));
        Object file = files.stream().filter(f -> Boolean.TRUE.equals(Json.at(f, "primary"))).findFirst()
            .orElse(files.isEmpty() ? null : files.get(0));
        if (file == null) throw new IOException("version has no files");
        List<Dep> deps = new ArrayList<>();
        for (Object d : Json.arr(Json.at(v, "dependencies"))) {
            String type = Json.str(Json.at(d, "dependency_type"));
            String proj = Json.str(Json.at(d, "project_id"));
            String ver = Json.str(Json.at(d, "version_id"));
            if (proj == null && ver != null) proj = Json.str(Json.at(getJson("https://api.modrinth.com/v2/version/" + enc(ver)), "project_id"));
            if (proj == null) continue;
            if ("required".equals(type)) deps.add(new Dep("modrinth", proj, ver, false));
            else if ("incompatible".equals(type)) deps.add(new Dep("modrinth", proj, null, true));
        }
        boolean supports = Json.arr(Json.at(v, "game_versions")).contains(mc);
        return new Release("modrinth:" + projectId, modrinthNames.get(projectId), Json.str(Json.at(v, "version_number")),
            Json.str(Json.at(file, "url")), Json.str(Json.at(file, "filename")), "SHA-512",
            Json.str(Json.at(file, "hashes", "sha512")), supports, deps);
    }

    private final Map<String, String> hangarNames = new HashMap<>();

    private List<Release> hangar(Want w) throws Exception {
        String base = "https://hangar.papermc.io/api/v1/projects/" + enc(w.project()) + "/versions";
        if (!w.latest()) {
            Object v = getJson(base + "/" + enc(w.version()));
            if (Json.at(v, "name") == null) throw new IOException("version " + w.version() + " not found on Hangar");
            return List.of(hangarRelease(v, w));
        }
        List<Object> result = Json.arr(Json.at(getJson(base + "?limit=25&platform=PAPER&channel=Release&platformVersion="
            + enc(mc)), "result"));
        if (result.isEmpty()) throw new IOException("no stable release supports Minecraft " + mc + " yet");
        List<Release> out = new ArrayList<>();
        for (Object v : result) {
            try {
                out.add(hangarRelease(v, w));
            } catch (IOException skip) {
                // e.g. a version only published as an external link
            }
        }
        if (out.isEmpty()) throw new IOException("only published as external links, so downloads cannot be verified; install it by hand");
        return preferInstalled(out);
    }

    private Release hangarRelease(Object v, Want w) throws Exception {
        Object dl = Json.at(v, "downloads", "PAPER");
        if (dl == null) throw new IOException("version has no Paper download");
        String url = Json.str(Json.at(dl, "downloadUrl"));
        if (url == null) throw new IOException("only published as an external link, so it cannot be verified; download it by hand");

        // Dependencies hosted outside Hangar have no projectId; they are found by plugin name in checkSet().
        List<Dep> deps = new ArrayList<>();
        for (Object d : Json.arr(Json.at(v, "pluginDependencies", "PAPER"))) {
            Object pid = Json.at(d, "projectId");
            if (Boolean.TRUE.equals(Json.at(d, "required")) && pid != null) deps.add(new Dep("hangar", numberString(pid), null, false));
        }
        boolean supports = Json.arr(Json.at(v, "platformDependencies", "PAPER")).contains(mc);
        String projectId = numberString(Json.at(v, "projectId"));
        if (!hangarNames.containsKey(projectId)) {
            Object proj = getJson("https://hangar.papermc.io/api/v1/projects/" + projectId);
            hangarNames.put(projectId, Objects.requireNonNullElse(Json.str(Json.at(proj, "name")), w.project()));
        }
        return new Release("hangar:" + projectId, hangarNames.get(projectId), Json.str(Json.at(v, "name")), url,
            Json.str(Json.at(dl, "fileInfo", "name")), "SHA-256", Json.str(Json.at(dl, "fileInfo", "sha256Hash")), supports, deps);
    }

    // ---- whole-set checks: duplicates, hand-installed copies, and plugin.yml dependencies ----

    private void checkSet() throws IOException {
        Set<String> managedFiles = new HashSet<>();
        lock.values().forEach(l -> managedFiles.add(l.file()));
        Map<String, String> manualNames = new HashMap<>();
        for (String jar : jars(plugins)) {
            if (managedFiles.contains(jar)) continue;
            PluginInfo pi = pluginInfo(plugins.resolve(jar));
            if (pi == null) continue;
            manualNames.put(pi.name().toLowerCase(Locale.ROOT), jar);
            pi.provides().forEach(p -> manualNames.put(p.toLowerCase(Locale.ROOT), jar));
        }

        Set<String> seen = new HashSet<>();
        for (String key : new ArrayList<>(picks.keySet())) {
            PluginInfo pi = infoOf.get(key);
            String id = pi.name().toLowerCase(Locale.ROOT);
            if (manualNames.containsKey(id)) {
                problem(pi.name() + " is already installed by hand (" + manualNames.get(id) + "); not installing a second copy");
                remove(key);
            } else if (!seen.add(id)) {
                problem(pi.name() + " would be installed twice (listed from two sources); keeping the first one");
                remove(key);
            }
        }

        // Dependencies named inside plugin.yml that the plugin sites didn't declare: search for them by name.
        Set<String> unfindable = new HashSet<>();
        for (int round = 0; round < 5; round++) {
            Set<String> have = new HashSet<>(manualNames.keySet());
            for (PluginInfo pi : infoOf.values()) {
                have.add(pi.name().toLowerCase(Locale.ROOT));
                pi.provides().forEach(p -> have.add(p.toLowerCase(Locale.ROOT)));
            }
            Map<String, String> missing = new LinkedHashMap<>();
            for (var e : infoOf.entrySet()) {
                for (String d : e.getValue().depends()) {
                    String id = d.toLowerCase(Locale.ROOT);
                    if (!have.contains(id) && !unfindable.contains(id)) missing.putIfAbsent(d, picks.get(e.getKey()).name());
                }
            }
            if (missing.isEmpty()) return;
            for (var m : missing.entrySet()) {
                if (findByPluginName(m.getKey(), m.getValue())) continue;
                unfindable.add(m.getKey().toLowerCase(Locale.ROOT));
                problem(m.getValue() + " requires the plugin " + m.getKey()
                    + ", which could not be found automatically; add it to plugins.txt (it will not load without it)");
            }
        }
    }

    // Tries Hangar, then Modrinth, for a project named like the plugin; accepted only if the jar's plugin name matches.
    private boolean findByPluginName(String pluginName, String requiredBy) {
        for (String source : List.of("hangar", "modrinth")) {
            String project = source.equals("modrinth") ? pluginName.toLowerCase(Locale.ROOT) : pluginName;
            try {
                Release r = acquire(new Want(source, project, "latest", null, "required by " + requiredBy));
                PluginInfo pi = infoOf.get(r.key());
                boolean matches = pi.name().equalsIgnoreCase(pluginName) || pi.provides().stream().anyMatch(pluginName::equalsIgnoreCase);
                if (matches && !picks.containsKey(r.key())) {
                    picks.put(r.key(), r);
                    originOf.put(r.key(), "required by " + requiredBy);
                    return true;
                }
                if (!picks.containsKey(r.key())) remove(r.key());
            } catch (Exception ignored) {
                // Not on this source; try the next one.
            }
        }
        return false;
    }

    static PluginInfo pluginInfo(Path jar) {
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            ZipEntry paper = zip.getEntry("paper-plugin.yml"), bukkit = zip.getEntry("plugin.yml");
            ZipEntry entry = paper != null ? paper : bukkit;
            if (entry == null) return null;
            Map<String, Object> y = Yaml.parse(new String(zip.getInputStream(entry).readAllBytes(), StandardCharsets.UTF_8));
            String name = Json.str(y.get("name"));
            if (name == null) return null;
            List<String> depends = new ArrayList<>();
            if (paper != null) {
                Map<String, Object> server = Json.obj(Json.at(y, "dependencies", "server"));
                if (server != null) {
                    for (var d : server.entrySet()) {
                        Object required = Json.at(d.getValue(), "required");
                        if (required == null || "true".equals(String.valueOf(required))) depends.add(d.getKey());
                    }
                }
            } else {
                for (Object d : Json.arr(y.get("depend"))) depends.add(String.valueOf(d));
            }
            List<String> provides = new ArrayList<>();
            for (Object p : Json.arr(y.get("provides"))) provides.add(String.valueOf(p));
            return new PluginInfo(name, Json.str(y.get("main")), Json.str(y.get("api-version")), depends, provides);
        } catch (Exception e) {
            return null;
        }
    }

    /** Java version the plugin's main class was compiled for (class file major version - 44). */
    static int classJava(Path jar, String mainClass) {
        if (mainClass == null) return 0;
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            ZipEntry e = zip.getEntry(mainClass.replace('.', '/') + ".class");
            if (e == null) return 0;
            try (InputStream in = zip.getInputStream(e)) {
                byte[] h = in.readNBytes(8);
                return h.length == 8 ? ((h[6] & 0xFF) << 8 | (h[7] & 0xFF)) - 44 : 0;
            }
        } catch (IOException ex) {
            return 0;
        }
    }

    // ---- status / pending ----

    private int status() throws IOException {
        Map<String, Locked> lock = readLock(lockFile);
        System.out.println("Minecraft " + mc + " on Java " + serverJava + " - plugins managed by plugins.txt:");
        if (lock.isEmpty()) System.out.println("  (none)");
        lock.values().forEach(l -> System.out.printf("  %-24s %-22s %s%n", l.name(), l.version(), l.origin()));
        Set<String> managedFiles = new HashSet<>();
        lock.values().forEach(l -> managedFiles.add(l.file()));
        List<String> manual = jars(plugins).stream().filter(f -> !managedFiles.contains(f)).toList();
        System.out.println("Other plugin jars (added by hand or built in):");
        if (manual.isEmpty()) System.out.println("  (none)");
        manual.forEach(f -> System.out.println("  " + f));
        if (Files.exists(pending.resolve("lock.tsv"))) System.out.println("Updates are staged and will apply on the next restart.");
        return 0;
    }

    private void applyPending() throws IOException {
        Path pendingLock = pending.resolve("lock.tsv");
        if (!Files.exists(pendingLock)) return;
        Map<String, Locked> next = readLock(pendingLock);
        Set<String> keep = new HashSet<>();
        next.values().forEach(l -> keep.add(l.file()));
        for (Locked old : readLock(lockFile).values()) {
            if (!keep.contains(old.file())) Files.deleteIfExists(plugins.resolve(old.file()));
        }
        for (String jar : jars(pending)) Files.move(pending.resolve(jar), plugins.resolve(jar), StandardCopyOption.REPLACE_EXISTING);
        writeLock(lockFile, next, readStamp(pendingLock));
        deleteTree(pending);
        log("applied plugin changes");
    }

    // ---- http / files ----

    private Object getJson(String url) throws IOException, InterruptedException {
        HttpResponse<String> res = send(url, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() == 404) throw new IOException("not found on " + URI.create(url).getHost());
        return Json.parse(res.body());
    }

    private <T> HttpResponse<T> send(String url, HttpResponse.BodyHandler<T> handler) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).header("User-Agent", UA)
            .timeout(Duration.ofMinutes(2)).GET().build();
        for (int attempt = 1; ; attempt++) {
            HttpResponse<T> res = http.send(req, handler);
            int code = res.statusCode();
            if (code < 300 || code == 404) return res;
            if ((code == 429 || code >= 500) && attempt < 4) {
                Thread.sleep(5_000L * attempt);
                continue;
            }
            throw new IOException("HTTP " + code + " from " + URI.create(url).getHost());
        }
    }

    private Path download(Release r, Path dir) throws IOException, InterruptedException {
        if (r.url() == null || r.hash() == null || r.file() == null) throw new IOException("incomplete download information");
        if (!r.file().endsWith(".jar") || r.file().contains("/") || r.file().contains("\\") || r.file().startsWith(".")) {
            throw new IOException("refusing unexpected file name " + r.file());
        }
        Path tmp = Files.createTempFile(dir, ".download-", ".part");
        try {
            HttpResponse<Path> res = send(r.url(), HttpResponse.BodyHandlers.ofFile(tmp));
            if (res.statusCode() != 200) throw new IOException("download failed with HTTP " + res.statusCode());
            if (!digest(tmp, r.algorithm()).equalsIgnoreCase(r.hash())) {
                throw new IOException("checksum mismatch for " + r.file() + ", download rejected");
            }
            return tmp;
        } catch (IOException | InterruptedException ex) {
            Files.deleteIfExists(tmp);
            throw ex;
        }
    }

    private static String digest(Path f, String algorithm) throws IOException {
        try (InputStream in = Files.newInputStream(f)) {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] buf = new byte[65536];
            for (int n; (n = in.read(buf)) > 0; ) md.update(buf, 0, n);
            return HexFormat.of().formatHex(md.digest());
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IOException(ex);
        }
    }

    private List<Entry> readManifest() throws IOException {
        List<Entry> entries = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int n = 0;
        for (String raw : Files.readAllLines(manifest)) {
            n++;
            if (n == 1 && raw.startsWith("\uFEFF")) raw = raw.substring(1);
            String line = raw.replaceAll("#.*", "").trim();
            if (line.isEmpty()) continue;
            String[] p = line.split("\\s+");
            if (p.length != 3) {
                problem("plugins.txt line " + n + " ignored, expected '<source> <project> <version|latest>': " + raw.trim());
                continue;
            }
            Entry e = new Entry(p[0].toLowerCase(Locale.ROOT), p[1], p[2]);
            if (seen.add(e.spec())) entries.add(e);
        }
        return entries;
    }

    /** Changes when plugins.txt, the Minecraft version or the server's Java version changes. */
    private String stamp() throws IOException {
        try {
            String input = mc + "\n" + serverJava + "\n" + Files.readString(manifest);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
    }

    private static String readStamp(Path f) throws IOException {
        if (!Files.exists(f)) return "";
        for (String line : Files.readAllLines(f)) if (line.startsWith("#stamp=")) return line.substring(7);
        return "";
    }

    private static Map<String, Locked> readLock(Path f) throws IOException {
        Map<String, Locked> m = new LinkedHashMap<>();
        if (!Files.exists(f)) return m;
        for (String line : Files.readAllLines(f)) {
            if (line.startsWith("#")) continue;
            String[] p = line.split("\t");
            if (p.length == 6) m.put(p[0], new Locked(p[0], p[1], p[2], p[3], p[4], p[5]));
        }
        return m;
    }

    private static void writeLock(Path f, Map<String, Locked> lock, String stamp) throws IOException {
        StringBuilder b = new StringBuilder("#stamp=" + stamp + "\n");
        lock.values().forEach(l -> b.append(String.join("\t", l.key(), l.version(), l.file(), l.hash(), l.name(), l.origin())).append('\n'));
        Path tmp = f.resolveSibling(f.getFileName() + ".tmp");
        Files.writeString(tmp, b);
        Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private static List<String> jars(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) return List.of();
        try (Stream<Path> s = Files.list(dir)) {
            return s.map(p -> p.getFileName().toString()).filter(f -> f.endsWith(".jar")).sorted().toList();
        }
    }

    private static void deleteTree(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        try (Stream<Path> s = Files.walk(dir)) {
            for (Path p : s.sorted(Comparator.reverseOrder()).toList()) Files.delete(p);
        }
    }

    private static String numberString(Object o) {
        return o instanceof Number n ? String.valueOf(n.longValue()) : String.valueOf(o);
    }

    private void problem(String msg) { problems.add(msg); }

    private static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20"); }

    private static void log(String msg) { System.out.println("[plugins] " + msg); }
}
