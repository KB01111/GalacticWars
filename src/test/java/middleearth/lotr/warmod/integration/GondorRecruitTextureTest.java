package middleearth.lotr.warmod.integration;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.imageio.ImageIO;

public final class GondorRecruitTextureTest {
    private static final Path TEXTURE_PATH = Path.of(
            "src/main/resources/assets/kingdomwarsmiddleearth/textures/entity/gondor_recruit.png");
    private static final String HUMAN_ERA_INFANTRYMAN_SHA256 =
            "620bd2f3667ada9afebbd579ec6feb833b12c270feb2c90c1f947b3f67e53e5f";

    private GondorRecruitTextureTest() {
    }

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        assertEquals(HUMAN_ERA_INFANTRYMAN_SHA256, sha256(TEXTURE_PATH), "texture provenance hash");

        BufferedImage texture = ImageIO.read(TEXTURE_PATH.toFile());

        assertEquals(64, texture.getWidth(), "texture width");
        assertEquals(64, texture.getHeight(), "texture height");

        assertOpaque(texture, 8, 8, 8, 8, "head front");
        assertOpaque(texture, 20, 20, 8, 12, "body front");
        assertOpaque(texture, 44, 20, 4, 12, "right arm front");
        assertOpaque(texture, 36, 52, 4, 12, "left arm front");
        assertOpaque(texture, 4, 20, 4, 12, "right leg front");
        assertOpaque(texture, 20, 52, 4, 12, "left leg front");

        System.out.println("GondorRecruitTextureTest passed");
    }

    private static void assertOpaque(BufferedImage texture, int x, int y, int width, int height, String label) {
        for (int ix = x; ix < x + width; ix++) {
            for (int iy = y; iy < y + height; iy++) {
                int alpha = (texture.getRGB(ix, iy) >>> 24) & 0xFF;
                if (alpha == 0) {
                    throw new AssertionError(label + " has transparent pixel at " + ix + "," + iy);
                }
            }
        }
    }

    private static void assertEquals(int expected, int actual, String label) {
        if (expected != actual) {
            throw new AssertionError(label + " expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void assertEquals(String expected, String actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static String sha256(Path path) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(Files.readAllBytes(path));
        StringBuilder result = new StringBuilder(hash.length * 2);
        for (byte value : hash) {
            result.append(String.format("%02x", value & 0xFF));
        }
        return result.toString();
    }
}
