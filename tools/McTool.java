import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

// Dependency-free Server List Ping (healthcheck) and RCON client.
public class McTool {
    private static final int TIMEOUT_MS = 5000;

    public static void main(String[] args) {
        try {
            if (args.length == 0) usage();
            switch (args[0]) {
                case "ping" -> ping(arg(args, 1, "127.0.0.1"), Integer.parseInt(arg(args, 2, "25565")));
                case "rcon" -> {
                    if (args.length < 4) usage();
                    rcon(args[1], Integer.parseInt(args[2]), args[3], join(args, 4));
                }
                case "resolve" -> System.exit(VersionResolver.resolve(java.util.Arrays.copyOfRange(args, 1, args.length)));
                case "versions" -> System.exit(VersionResolver.list());
                case "plugins" -> System.exit(PluginManager.run(java.util.Arrays.copyOfRange(args, 1, args.length)));
                case "migrate-players" -> System.exit(PlayerMigrator.run(java.util.Arrays.copyOfRange(args, 1, args.length)));
                default -> usage();
            }
        } catch (Exception e) {
            System.err.println("error: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void usage() {
        System.err.println("usage: McTool ping [host] [port] | McTool rcon <host> <port> <password> [command...]");
        System.exit(2);
    }

    private static String arg(String[] a, int i, String def) { return a.length > i ? a[i] : def; }

    private static String join(String[] a, int from) {
        if (a.length <= from) return null;
        return String.join(" ", java.util.Arrays.copyOfRange(a, from, a.length));
    }

    // ---- Server List Ping (protocol state 1) ----

    private static void ping(String host, int port) throws IOException {
        try (Socket s = connect(host, port)) {
            DataOutputStream out = new DataOutputStream(s.getOutputStream());
            DataInputStream in = new DataInputStream(s.getInputStream());

            ByteArrayOutputStream hs = new ByteArrayOutputStream();
            DataOutputStream h = new DataOutputStream(hs);
            writeVarInt(h, 0x00);
            writeVarInt(h, -1);
            byte[] hb = host.getBytes(StandardCharsets.UTF_8);
            writeVarInt(h, hb.length);
            h.write(hb);
            h.writeShort(port);
            writeVarInt(h, 1);

            writeVarInt(out, hs.size());
            out.write(hs.toByteArray());
            writeVarInt(out, 1);
            writeVarInt(out, 0x00);
            out.flush();

            readVarInt(in);
            if (readVarInt(in) != 0x00) throw new IOException("unexpected status packet");
            byte[] json = new byte[readVarInt(in)];
            in.readFully(json);
            System.out.println(new String(json, StandardCharsets.UTF_8));
        }
    }

    private static void writeVarInt(DataOutputStream out, int value) throws IOException {
        while ((value & ~0x7F) != 0) {
            out.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.writeByte(value);
    }

    private static int readVarInt(DataInputStream in) throws IOException {
        int value = 0, shift = 0;
        byte b;
        do {
            b = in.readByte();
            value |= (b & 0x7F) << shift;
            shift += 7;
            if (shift > 35) throw new IOException("VarInt too big");
        } while ((b & 0x80) != 0);
        return value;
    }

    // ---- RCON ----

    private static final int AUTH = 3, EXEC = 2;

    private static void rcon(String host, int port, String password, String command) throws IOException {
        try (Socket s = connect(host, port)) {
            s.setSoTimeout(command == null ? 0 : TIMEOUT_MS);
            OutputStream out = s.getOutputStream();
            DataInputStream in = new DataInputStream(s.getInputStream());

            send(out, 1, AUTH, password);
            Packet p = read(in);
            // Source-style servers send an empty RESPONSE_VALUE (type 0) before the auth reply; Minecraft does not.
            if (p.type == 0) p = read(in);
            if (p.id == -1) throw new IOException("RCON authentication failed");

            if (command != null) {
                System.out.println(exec(out, in, command));
                return;
            }
            BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("> ");
            for (String line; (line = stdin.readLine()) != null; System.out.print("> ")) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if (line.equals("exit") || line.equals("quit")) break;
                System.out.println(exec(out, in, line));
            }
        }
    }

    private static int nextId = 2;

    private static String exec(OutputStream out, DataInputStream in, String command) throws IOException {
        int id = nextId++;
        send(out, id, EXEC, command);
        return read(in).body;
    }

    private record Packet(int id, int type, String body) {}

    private static void send(OutputStream out, int id, int type, String body) throws IOException {
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buf = ByteBuffer.allocate(14 + b.length).order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(10 + b.length).putInt(id).putInt(type).put(b).put((byte) 0).put((byte) 0);
        out.write(buf.array());
        out.flush();
    }

    private static Packet read(DataInputStream in) throws IOException {
        byte[] lenBytes = new byte[4];
        in.readFully(lenBytes);
        int len = ByteBuffer.wrap(lenBytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
        if (len < 10 || len > 1 << 20) throw new IOException("bad RCON packet length " + len);
        byte[] data = new byte[len];
        in.readFully(data);
        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        int id = buf.getInt(), type = buf.getInt();
        return new Packet(id, type, new String(data, 8, len - 10, StandardCharsets.UTF_8));
    }

    private static Socket connect(String host, int port) throws IOException {
        Socket s = new Socket();
        s.connect(new InetSocketAddress(host, port), TIMEOUT_MS);
        s.setSoTimeout(TIMEOUT_MS);
        return s;
    }
}
