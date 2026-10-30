import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Reads the YAML subset used by plugin.yml / paper-plugin.yml: nested maps, block and inline lists, scalars.
final class Yaml {
    private record Line(int indent, String text) {}

    private final List<Line> lines = new ArrayList<>();
    private int pos;

    private Yaml(String text) {
        for (String raw : text.replace("\t", "  ").split("\r?\n")) {
            String t = stripComment(raw);
            if (t.isBlank() || t.trim().equals("---")) continue;
            int indent = 0;
            while (indent < t.length() && t.charAt(indent) == ' ') indent++;
            lines.add(new Line(indent, t.trim()));
        }
    }

    static Map<String, Object> parse(String text) {
        Yaml y = new Yaml(text);
        Object root = y.lines.isEmpty() ? null : y.block(y.lines.get(0).indent());
        return Json.obj(root) != null ? Json.obj(root) : Map.of();
    }

    private Object block(int indent) {
        return lines.get(pos).text().startsWith("-") ? list(indent) : map(indent);
    }

    private Map<String, Object> map(int indent) {
        Map<String, Object> m = new LinkedHashMap<>();
        while (pos < lines.size() && lines.get(pos).indent() == indent && !lines.get(pos).text().startsWith("- ")) {
            String t = lines.get(pos++).text();
            int colon = keyColon(t);
            if (colon < 0) continue;
            String key = unquote(t.substring(0, colon).trim());
            String value = t.substring(colon + 1).trim();
            if (value.startsWith("|") || value.startsWith(">")) {
                while (pos < lines.size() && lines.get(pos).indent() > indent) pos++;
                m.put(key, "");
            } else if (!value.isEmpty()) {
                m.put(key, scalar(value));
            } else if (pos < lines.size() && (lines.get(pos).indent() > indent
                    || (lines.get(pos).indent() == indent && lines.get(pos).text().startsWith("- ")))) {
                m.put(key, block(lines.get(pos).indent()));
            } else {
                m.put(key, null);
            }
        }
        return m;
    }

    private List<Object> list(int indent) {
        List<Object> l = new ArrayList<>();
        while (pos < lines.size() && lines.get(pos).indent() == indent && lines.get(pos).text().startsWith("-")) {
            String item = lines.get(pos++).text().substring(1).trim();
            if (!item.isEmpty()) l.add(scalar(item));
            else if (pos < lines.size() && lines.get(pos).indent() > indent) l.add(block(lines.get(pos).indent()));
        }
        return l;
    }

    private static Object scalar(String v) {
        if (v.startsWith("[") && v.endsWith("]")) {
            List<Object> l = new ArrayList<>();
            for (String part : v.substring(1, v.length() - 1).split(",")) if (!part.isBlank()) l.add(unquote(part.trim()));
            return l;
        }
        return unquote(v);
    }

    private static String unquote(String v) {
        if (v.length() >= 2 && (v.startsWith("\"") && v.endsWith("\"") || v.startsWith("'") && v.endsWith("'"))) {
            return v.substring(1, v.length() - 1);
        }
        return v;
    }

    private static int keyColon(String t) {
        boolean quoted = false;
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (c == '"' || c == '\'') quoted = !quoted;
            if (c == ':' && !quoted && (i + 1 == t.length() || t.charAt(i + 1) == ' ')) return i;
        }
        return -1;
    }

    private static String stripComment(String s) {
        boolean quoted = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\'') quoted = !quoted;
            if (c == '#' && !quoted && (i == 0 || s.charAt(i - 1) == ' ')) return s.substring(0, i);
        }
        return s;
    }
}
