package halisoncode;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Issue #198: both loaders name the same mod icon, and it is a square PNG in the mod's resources. */
class ModIconTest {
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

    private static Path root() {
        Path dir = Path.of("").toAbsolutePath();
        while (!Files.exists(dir.resolve("settings.gradle.kts"))) dir = dir.getParent();
        return dir;
    }

    @Test
    void bothLoadersNameTheSameSquarePngIcon() throws IOException {
        String fabric = JsonParser.parseString(Files.readString(root().resolve("fabric/src/main/resources/fabric.mod.json"),
                StandardCharsets.UTF_8)).getAsJsonObject().get("icon").getAsString();
        Matcher logo = Pattern.compile("(?m)^logoFile\\s*=\\s*\"([^\"]+)\"").matcher(Files.readString(
                root().resolve("neoforge/src/main/resources/META-INF/neoforge.mods.toml"), StandardCharsets.UTF_8));
        assertTrue(logo.find(), "no logoFile in neoforge.mods.toml");
        assertEquals(fabric, logo.group(1));
        try (InputStream in = ModIconTest.class.getResourceAsStream("/" + fabric)) {
            assertNotNull(in, fabric + " is not in the mod's resources");
            byte[] head = in.readNBytes(24);
            byte[] signature = new byte[8];
            System.arraycopy(head, 0, signature, 0, 8);
            assertArrayEquals(PNG, signature, fabric + " is not a PNG");
            int width = ((head[16] & 0xFF) << 24) | ((head[17] & 0xFF) << 16) | ((head[18] & 0xFF) << 8) | (head[19] & 0xFF);
            int height = ((head[20] & 0xFF) << 24) | ((head[21] & 0xFF) << 16) | ((head[22] & 0xFF) << 8) | (head[23] & 0xFF);
            assertEquals(width, height, "the icon is not square");
        }
    }
}
