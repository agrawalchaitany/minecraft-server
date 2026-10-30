import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

// Resolves a Minecraft version (or "latest") to a Paper release and the Java LTS it needs.
public class VersionResolver {
    private static final String API = "https://fill.papermc.io/v3/projects/paper";
    private static final int[] JAVA_LTS = {17, 21, 25};
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

    /** resolve <mcVersion|latest> [javaVersion|auto]  -> prints MC_VERSION=.. and JAVA_VERSION=.. */
    static int resolve(String[] a) throws Exception {
        String requested = a.length > 0 && !a[0].isBlank() ? a[0] : "latest";
        String javaOverride = a.length > 1 && !a[1].isBlank() ? a[1] : "auto";
        String version = requested.equals("latest") ? latestStable() : requested;

        Object info = get(API + "/versions/" + version);
        if (Json.at(info, "version") == null) {
            System.err.println("Minecraft " + version + " is not available on Paper. Run 'make versions' to see the choices.");
            return 1;
        }
        if (Json.arr(get(API + "/versions/" + version + "/builds")).stream()
                .noneMatch(b -> "STABLE".equals(Json.str(Json.at(b, "channel"))))) {
            System.err.println("Paper has no stable build for Minecraft " + version + " yet.");
            return 1;
        }
        int min = ((Number) Objects.requireNonNullElse(Json.at(info, "version", "java", "version", "minimum"), 17.0)).intValue();
        int java;
        if (javaOverride.equals("auto")) {
            java = Arrays.stream(JAVA_LTS).filter(j -> j >= min).findFirst().orElse(-1);
            if (java < 0) {
                System.err.println("Minecraft " + version + " needs Java " + min + ", which this image does not support yet.");
                return 1;
            }
            if (min < 16) {
                System.err.println("Minecraft " + version + " is too old for this image (Minecraft 1.17 or newer is supported).");
                return 1;
            }
        } else {
            java = Integer.parseInt(javaOverride);
            if (java < min) {
                System.err.println("Minecraft " + version + " needs Java " + min + " or newer, but JAVA_VERSION=" + java);
                return 1;
            }
        }
        System.out.println("MC_VERSION=" + version);
        System.out.println("JAVA_VERSION=" + java);
        return 0;
    }

    /** versions -> table of Minecraft versions available on Paper */
    static int list() throws Exception {
        Object project = get(API);
        Map<String, Object> groups = Json.obj(Json.at(project, "versions"));
        System.out.printf("%-10s %-6s %s%n", "MINECRAFT", "JAVA", "PAPER SUPPORT");
        for (Object group : groups.values()) {
            for (Object v : Json.arr(group)) {
                String id = Json.str(v);
                if (!id.matches("\\d+(\\.\\d+)+")) continue;
                Object info = get(API + "/versions/" + id);
                Number min = (Number) Json.at(info, "version", "java", "version", "minimum");
                if (min == null || min.intValue() < 16) return 0;
                String status = Json.str(Json.at(info, "version", "support", "status"));
                System.out.printf("%-10s %-6s %s%n", id, min.intValue(), status == null ? "" : status.toLowerCase());
            }
        }
        return 0;
    }

    private static String latestStable() throws Exception {
        List<String> all = new ArrayList<>();
        for (Object group : Json.obj(Json.at(get(API), "versions")).values()) {
            for (Object v : Json.arr(group)) if (Json.str(v).matches("\\d+(\\.\\d+)+")) all.add(Json.str(v));
        }
        all.sort(VersionResolver::compare);
        Collections.reverse(all);
        for (String v : all) {
            boolean stable = Json.arr(get(API + "/versions/" + v + "/builds")).stream()
                .anyMatch(b -> "STABLE".equals(Json.str(Json.at(b, "channel"))));
            if (stable) return v;
        }
        throw new IOException("no stable Paper version found");
    }

    /** Numeric dotted-version comparison: 1.21.4 < 1.21.10 < 26.1. */
    static int compare(String a, String b) {
        String[] x = a.split("\\."), y = b.split("\\.");
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            int p = i < x.length ? num(x[i]) : 0, q = i < y.length ? num(y[i]) : 0;
            if (p != q) return Integer.compare(p, q);
        }
        return 0;
    }

    private static int num(String s) {
        String d = s.replaceAll("\\D.*", "");
        return d.isEmpty() ? 0 : Integer.parseInt(d);
    }

    private static Object get(String url) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).header("User-Agent", "self-hosted-paper-docker/1.0")
            .timeout(Duration.ofSeconds(30)).GET().build();
        HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() == 404) return Map.of();
        if (res.statusCode() != 200) throw new IOException("PaperMC API returned HTTP " + res.statusCode());
        return Json.parse(res.body());
    }
}
