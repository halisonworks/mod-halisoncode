package halisoncode.client;

import halisoncode.HalisonCode;
import halisoncode.net.RelayPayloads;
import halisoncode.platform.Platform;
import halisoncode.robot.RobotEffects;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.SoundType;

/**
 * Issues #81 and #82: the particles and sounds for what the owner's robot does. {@code /halisoncode effects off} turns
 * them off on this computer; the choice is kept in config/halisoncode-client.properties.
 */
public final class RobotEffectsClient {
    private static final String FILE = "halisoncode-client.properties";
    private static final String KEY = "robotEffects";
    private static Boolean on;
    /** Each effect the client showed; the client GameTest puts its own here. */
    public static Consumer<RelayPayloads.Effect> shown = e -> { };

    private RobotEffectsClient() {
    }

    public static boolean on() {
        if (on == null) on = !"false".equals(load().getProperty(KEY));
        return on;
    }

    public static void set(boolean value) {
        on = value;
        Properties props = load();
        props.setProperty(KEY, Boolean.toString(value));
        try (Writer out = Files.newBufferedWriter(file())) {
            props.store(out, "Halison Code, this computer only");
        } catch (IOException e) {
            HalisonCode.LOG.warn("could not save {}: {}", FILE, e.toString());
        }
    }

    private static Path file() {
        return Platform.INSTANCE.configDir().resolve(FILE);
    }

    private static Properties load() {
        Properties props = new Properties();
        if (Files.exists(file())) {
            try (Reader in = Files.newBufferedReader(file())) {
                props.load(in);
            } catch (IOException e) {
                HalisonCode.LOG.warn("could not read {}: {}", FILE, e.toString());
            }
        }
        return props;
    }

    /** On the render thread. */
    public static void receive(Minecraft client, RelayPayloads.Effect effect) {
        ClientLevel level = client.level;
        if (level == null || !on()) return;
        BlockPos pos = effect.pos();
        double x = pos.getX() + 0.5, y = pos.getY(), z = pos.getZ() + 0.5;
        switch (effect.kind()) {
            case RobotEffects.MOVE -> {
                // A footstep on the block below, and a small puff at the feet.
                SoundType ground = level.getBlockState(pos.below()).getSoundType();
                level.playLocalSound(x, y, z, ground.getStepSound(), SoundSource.NEUTRAL, ground.getVolume() * 0.3f, ground.getPitch(), false);
                for (int i = 0; i < 3; i++) level.addParticle(ParticleTypes.CLOUD, x + (i - 1) * 0.2, y + 0.05, z, 0, 0.01, 0);
            }
            case RobotEffects.ERROR -> {
                level.playLocalSound(x, y + 1, z, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.NEUTRAL, 0.8f, 0.6f, false);
                for (int i = 0; i < 8; i++) level.addParticle(ParticleTypes.SMOKE, x + Math.sin(i) * 0.3, y + 1.2 + i * 0.05, z + Math.cos(i) * 0.3, 0, 0.03, 0);
            }
            case RobotEffects.INSPECT -> outline(level, pos);
            default -> {
                return;
            }
        }
        shown.accept(effect);
    }

    // The 12 edges of the block, 5 points on each.
    private static void outline(ClientLevel level, BlockPos pos) {
        for (int axis = 0; axis < 3; axis++) {
            for (int a = 0; a <= 1; a++) {
                for (int b = 0; b <= 1; b++) {
                    for (int i = 0; i <= 4; i++) {
                        double t = i / 4.0;
                        double[] p = axis == 0 ? new double[] {t, a, b} : axis == 1 ? new double[] {a, t, b} : new double[] {a, b, t};
                        level.addParticle(ParticleTypes.HAPPY_VILLAGER, pos.getX() + p[0], pos.getY() + p[1], pos.getZ() + p[2], 0, 0, 0);
                    }
                }
            }
        }
    }
}
