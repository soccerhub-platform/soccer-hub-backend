package kz.edu.soccerhub.media.application.image;

import kz.edu.soccerhub.media.domain.exception.MediaValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

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
}
