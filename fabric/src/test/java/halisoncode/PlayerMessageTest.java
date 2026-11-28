package halisoncode;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Issue #278: every message to a player goes through {@code halisoncode.Messages}, with a key in en_us.json. */
class PlayerMessageTest {
    private static final Pattern SENDS = Pattern.compile(
            "Component\\.(literal|translatable\\w*)\\(|\\.(sendFailure|sendSuccess|sendSystemMessage|sendOverlayMessage|displayClientMessage)\\(|SimpleCommandExceptionType|new DynamicCommandExceptionType");
    /** Text that is not Halison Code's own: the program's words, and the name the player gave a robot. The camera HUD draws. */
    private static final List<String> ALLOWED = List.of(
            "player.sendSystemMessage(Component.literal(c.text()))",
            "if (Component.literal(name).equals(getCustomName())) return;",
            "setCustomName(Component.literal(name))",
            "Component.translatable(\"hud.halisoncode.camera\", robot.getDisplayName())");
    private static final Pattern KEY = Pattern.compile("\"((?:commands|message)\\.halisoncode\\.[a-z_.]+)\"");

    private static Path root() {
        Path dir = Path.of("").toAbsolutePath();
        while (!Files.exists(dir.resolve("settings.gradle.kts"))) dir = dir.getParent();
        return dir;
    }

    /** Every Java file of the mod that can talk to a player, by path. */
    private static TreeMap<String, String> sources() throws IOException {
        TreeMap<String, String> sources = new TreeMap<>();
        for (String dir : List.of("common/src/main/java", "common/src/client/java", "fabric/src/main/java", "fabric/src/client/java", "neoforge/src/main/java")) {
            Path path = root().resolve(dir);
            if (!Files.exists(path)) continue;
            try (Stream<Path> files = Files.walk(path)) {
                for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                    sources.put(root().relativize(file).toString().replace('\\', '/'), Files.readString(file));
                }
            }
        }
        assertTrue(sources.containsKey("common/src/main/java/halisoncode/Messages.java"), "sources found: " + sources.keySet());
        return sources;
    }

    private static JsonObject english() throws IOException {
        return lang("en_us");
    }

    private static JsonObject lang(String name) throws IOException {
        try (InputStream in = PlayerMessageTest.class.getResourceAsStream("/assets/halisoncode/lang/" + name + ".json")) {
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    // Issue #447: the robot's own sentences are in en_us.json (%s) and in the editor's simulator (ROBOT_TEXTS, {0}) with
    // the same English, and the editor's Norwegian sim-strings.json translates each of them.
    @Test
    void robotSentencesMatchTheEditor() throws IOException {
        JsonObject english = english();
        String sim = Files.readString(root().resolve("target/sim/simulator.ts"));
        String table = sim.substring(sim.indexOf("ROBOT_TEXTS"), sim.indexOf("};", sim.indexOf("ROBOT_TEXTS")));
        TreeMap<String, String> inSim = new TreeMap<>();
        Matcher m = Pattern.compile("\"(robot\\.halisoncode\\.[a-z_.]+)\": \"((?:[^\"\\\\]|\\\\.)*)\"").matcher(table);
        while (m.find()) inSim.put(m.group(1), m.group(2).replace("\\\"", "\"").replaceAll("\\{\\d+\\}", "%s"));
        TreeMap<String, String> inMod = new TreeMap<>();
        english.keySet().stream().filter(k -> k.startsWith("robot.")).forEach(k -> inMod.put(k, english.get(k).getAsString()));
        assertFalse(inMod.isEmpty(), "robot sentences in en_us.json");
        assertEquals(inMod, inSim, "en_us.json and ROBOT_TEXTS in target/sim/simulator.ts");
        JsonObject nb = JsonParser.parseString(Files.readString(root().resolve("target/locales/nb/halisoncode/sim-strings.json"))).getAsJsonObject();
        List<String> untranslated = inSim.values().stream().map(t -> {
            int[] n = {0};
            return Pattern.compile("%s").matcher(t).replaceAll(r -> "{" + n[0]++ + "}");
        }).filter(t -> !nb.has(t)).toList();
        assertEquals(List.of(), untranslated, "sentences without a Norwegian text in sim-strings.json");
    }

    // Issue #275: a translation has every English key and no other, and the same number of %s in each text, so no
    // argument is lost or shown twice. Minecraft's code for Norwegian Bokmal is no_no (its language list has no
    // nb_no), so a file named nb_no.json is never read.
    @Test
    void norwegianHasEveryKeyWithTheSameArguments() throws IOException {
        assertNull(PlayerMessageTest.class.getResource("/assets/halisoncode/lang/nb_no.json"), "Minecraft reads Norwegian from no_no.json, not nb_no.json");
        JsonObject english = english(), norwegian = lang("no_no");
        assertEquals(new java.util.TreeSet<>(english.keySet()), new java.util.TreeSet<>(norwegian.keySet()), "keys");
        List<String> wrong = english.keySet().stream()
                .filter(k -> english.get(k).getAsString().split("%s", -1).length != norwegian.get(k).getAsString().split("%s", -1).length)
                .sorted().toList();
        assertEquals(List.of(), wrong, "texts with another number of %s");
    }

    @Test
    void onlyMessagesTalksToPlayers() throws IOException {
        List<String> found = new ArrayList<>();
        sources().forEach((file, text) -> {
            if (file.endsWith("/halisoncode/Messages.java")) return;
            for (String line : text.split("\n")) {
                String rest = line;
                for (String allowed : ALLOWED) rest = rest.replace(allowed, "");
                if (SENDS.matcher(rest).find()) found.add(file + ": " + line.strip());
            }
        });
        assertEquals(List.of(), found, "send it with halisoncode.Messages and a key in en_us.json");
    }

    @Test
    void everyKeyHasATextAndEveryTextIsUsed() throws IOException {
        JsonObject english = english();
        Set<String> used = new HashSet<>();
        for (String text : sources().values()) {
            Matcher m = KEY.matcher(text);
            while (m.find()) used.add(m.group(1));
        }
        List<String> missing = used.stream().filter(k -> !k.endsWith(".") && !english.has(k)).sorted().toList();
        assertEquals(List.of(), missing, "keys without a text");
        List<String> unused = english.keySet().stream().filter(k -> k.startsWith("commands.") || k.startsWith("message."))
                .filter(k -> !used.contains(k) && !used.contains(k.substring(0, k.lastIndexOf('.') + 1))).sorted().toList();
        assertEquals(List.of(), unused, "texts no code uses");
    }

    @Test
    void textsReadLikeTheGames() throws IOException {
        for (var e : english().entrySet()) {
            String text = e.getValue().getAsString();
            // What a robot says (issue #447) is a sentence of its own, with a full stop, as it always was.
            if (!e.getKey().startsWith("robot.")) assertFalse(text.endsWith("."), e.getKey() + ": the game's messages have no full stop at the end");
            assertFalse(Pattern.compile("agent\\.|[A-Z]+_[A-Z]+|\\b(UNLOADED|STOPPED|PROTECTED)\\b").matcher(text).find(), e.getKey() + ": no internal codes: " + text);
            assertFalse(Pattern.compile("%(?!s)").matcher(text).find(), e.getKey() + ": only %s works in a translation");
        }
    }
}
