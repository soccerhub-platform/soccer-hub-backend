package kz.edu.soccerhub.media.application.image;

import kz.edu.soccerhub.media.domain.exception.MediaValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AvatarImageProcessorTest {

    static {
        System.setProperty("java.awt.headless", "true");
    }

    private final AvatarImageProcessor processor = new AvatarImageProcessor(
            new AvatarImageProperties(
                    5_242_880,
                    10_000,
                    10_000,
                    40_000_000,
                    96,
                    320,
                    "jpg",
                    "image/jpeg"
            )
    );

    @Test
    void processShouldCreateExactSquareVersions() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                imageBytes(1200, 1600, "png")
        );

        ProcessedAvatar avatar = processor.process(file);

        assertEquals("image/png", avatar.originalMimeType());
        assertEquals(1200, avatar.originalWidth());
        assertEquals(1600, avatar.originalHeight());
        assertEquals(96, avatar.thumb().width());
        assertEquals(96, avatar.thumb().height());
        assertEquals(320, avatar.medium().width());
        assertEquals(320, avatar.medium().height());
        assertEquals("image/jpeg", avatar.original().mimeType());
    }

    @Test
    void processShouldRejectFakeImage() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.jpg",
                "image/jpeg",
                "not-an-image".getBytes()
        );

        assertThrows(
                MediaValidationException.class,
                () -> processor.process(file)
        );
    }

    @Test
    void processShouldApplyExifOrientationBeforeCreatingVariants() throws Exception {
        byte[] jpeg = splitColorImageBytes(120, 80);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "phone-photo.jpg",
                "image/jpeg",
                withExifOrientation(jpeg, 6)
        );

        ProcessedAvatar avatar = processor.process(file);

        assertEquals(80, avatar.originalWidth());
        assertEquals(120, avatar.originalHeight());

        BufferedImage normalized = ImageIO.read(
                new java.io.ByteArrayInputStream(avatar.original().content())
        );
        Color top = new Color(normalized.getRGB(40, 20));
        Color bottom = new Color(normalized.getRGB(40, 100));
        org.junit.jupiter.api.Assertions.assertTrue(top.getRed() > top.getBlue());
        org.junit.jupiter.api.Assertions.assertTrue(bottom.getBlue() > bottom.getRed());
    }

    private byte[] imageBytes(
            int width,
            int height,
            String format
    ) throws Exception {
        BufferedImage image = new BufferedImage(
                width,
                height,
                BufferedImage.TYPE_INT_RGB
        );
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.GREEN);
            graphics.fillRect(0, 0, width, height);

        } finally {
            graphics.dispose();
        }

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(image, format, outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] splitColorImageBytes(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.RED);
            graphics.fillRect(0, 0, width / 2, height);
            graphics.setColor(Color.BLUE);
            graphics.fillRect(width / 2, 0, width - width / 2, height);
        } finally {
            graphics.dispose();
        }

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(image, "jpg", outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] withExifOrientation(byte[] jpeg, int orientation) {
        byte[] exifSegment = new byte[] {
                (byte) 0xFF, (byte) 0xE1, 0x00, 0x22,
                'E', 'x', 'i', 'f', 0x00, 0x00,
                'I', 'I', 0x2A, 0x00,
                0x08, 0x00, 0x00, 0x00,
                0x01, 0x00,
                0x12, 0x01,
                0x03, 0x00,
                0x01, 0x00, 0x00, 0x00,
                (byte) orientation, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00
        };

        byte[] result = Arrays.copyOf(jpeg, jpeg.length + exifSegment.length);
        System.arraycopy(exifSegment, 0, result, 2, exifSegment.length);
        System.arraycopy(jpeg, 2, result, 2 + exifSegment.length, jpeg.length - 2);
        return result;
    }
}
