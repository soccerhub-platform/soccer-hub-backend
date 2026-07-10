package kz.edu.soccerhub.media.application.image;

import kz.edu.soccerhub.media.domain.exception.MediaValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class AvatarImageProcessor {

    private static final Set<String> SUPPORTED_FORMATS = Set.of(
            "jpeg",
            "jpg",
            "png",
            "webp"
    );

    private final AvatarImageProperties properties;

    public ProcessedAvatar process(MultipartFile file) {
        validateFile(file);

        byte[] bytes = readBytes(file);
        DecodedImage decodedImage = decode(bytes);
        ProcessedImage original = encode(
                toOutputCompatibleImage(decodedImage.image()),
                decodedImage.width(),
                decodedImage.height()
        );

        return new ProcessedAvatar(
                decodedImage.mimeType(),
                file.getSize(),
                decodedImage.width(),
                decodedImage.height(),
                original,
                resizeToSquare(decodedImage.image(), properties.thumbSize()),
                resizeToSquare(decodedImage.image(), properties.mediumSize())
        );
    }

    private void validateFile(MultipartFile file) {
        if (file == null) {
            throw new MediaValidationException(
                    "Media file is required",
                    "EMPTY_MEDIA_FILE"
            );
        }

        if (file.isEmpty() || file.getSize() == 0) {
            throw new MediaValidationException(
                    "Media file must not be empty",
                    "EMPTY_MEDIA_FILE"
            );
        }

        if (file.getSize() > properties.maxUploadSizeBytes()) {
            throw new MediaValidationException(
                    "Media file is too large",
                    "MEDIA_FILE_TOO_LARGE"
            );
        }
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();

        } catch (IOException exception) {
            throw new MediaValidationException(
                    "Failed to read media file",
                    "INVALID_IMAGE_FILE"
            );
        }
    }

    private DecodedImage decode(byte[] bytes) {
        try (
                ImageInputStream imageInputStream = ImageIO.createImageInputStream(
                        new ByteArrayInputStream(bytes)
                )
        ) {
            if (imageInputStream == null) {
                throw invalidImage();
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStream);
            if (!readers.hasNext()) {
                throw new MediaValidationException(
                        "Unsupported image type",
                        "UNSUPPORTED_MEDIA_TYPE"
                );
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInputStream, true, true);

                String formatName = normalizeFormat(reader.getFormatName());
                if (!SUPPORTED_FORMATS.contains(formatName)) {
                    throw new MediaValidationException(
                            "Unsupported image type",
                            "UNSUPPORTED_MEDIA_TYPE"
                    );
                }

                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                validateDimensions(width, height);

                BufferedImage image = reader.read(0);
                if (image == null) {
                    throw invalidImage();
                }

                return new DecodedImage(
                        image,
                        width,
                        height,
                        toMimeType(formatName)
                );

            } finally {
                reader.dispose();
            }

        } catch (MediaValidationException exception) {
            throw exception;

        } catch (IOException | RuntimeException exception) {
            throw invalidImage();
        }
    }

    private void validateDimensions(
            int width,
            int height
    ) {
        if (
                width <= 0
                        || height <= 0
                        || width > properties.maxWidth()
                        || height > properties.maxHeight()
                        || Math.multiplyFull(width, height) > properties.maxPixels()
        ) {
            throw new MediaValidationException(
                    "Image dimensions exceed configured limits",
                    "IMAGE_DIMENSIONS_EXCEEDED"
            );
        }
    }

    private ProcessedImage resizeToSquare(
            BufferedImage source,
            int size
    ) {
        BufferedImage target = new BufferedImage(
                size,
                size,
                outputImageType()
        );
        Graphics2D graphics = target.createGraphics();

        try {
            configureQuality(graphics);

            double scale = Math.max(
                    size / (double) source.getWidth(),
                    size / (double) source.getHeight()
            );
            int scaledWidth = (int) Math.ceil(source.getWidth() * scale);
            int scaledHeight = (int) Math.ceil(source.getHeight() * scale);
            int x = (size - scaledWidth) / 2;
            int y = (size - scaledHeight) / 2;

            if (usesJpegOutput()) {
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, size, size);
            }

            graphics.drawImage(
                    source,
                    x,
                    y,
                    scaledWidth,
                    scaledHeight,
                    null
            );

        } finally {
            graphics.dispose();
        }

        return encode(target, size, size);
    }

    private BufferedImage toOutputCompatibleImage(BufferedImage source) {
        if (!usesJpegOutput()) {
            return source;
        }

        BufferedImage target = new BufferedImage(
                source.getWidth(),
                source.getHeight(),
                BufferedImage.TYPE_INT_RGB
        );
        Graphics2D graphics = target.createGraphics();

        try {
            configureQuality(graphics);
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, target.getWidth(), target.getHeight());
            graphics.drawImage(source, 0, 0, null);
            return target;

        } finally {
            graphics.dispose();
        }
    }

    private ProcessedImage encode(
            BufferedImage image,
            int width,
            int height
    ) {
        String outputFormat = normalizeFormat(properties.outputFormat());
        if (!ImageIO.getImageWritersByFormatName(outputFormat).hasNext()) {
            throw new MediaValidationException(
                    "Image output format is not supported",
                    "IMAGE_PROCESSING_FAILED"
            );
        }

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            boolean written = ImageIO.write(
                    image,
                    outputFormat,
                    outputStream
            );

            if (!written) {
                throw new MediaValidationException(
                        "Image processing failed",
                        "IMAGE_PROCESSING_FAILED"
                );
            }

            return new ProcessedImage(
                    outputStream.toByteArray(),
                    properties.outputMimeType(),
                    extension(outputFormat),
                    width,
                    height
            );

        } catch (IOException exception) {
            throw new MediaValidationException(
                    "Image processing failed",
                    "IMAGE_PROCESSING_FAILED"
            );
        }
    }

    private void configureQuality(Graphics2D graphics) {
        graphics.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC
        );
        graphics.setRenderingHint(
                RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY
        );
        graphics.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );
    }

    private int outputImageType() {
        return usesJpegOutput()
                ? BufferedImage.TYPE_INT_RGB
                : BufferedImage.TYPE_INT_ARGB;
    }

    private boolean usesJpegOutput() {
        String format = normalizeFormat(properties.outputFormat());
        return "jpg".equals(format) || "jpeg".equals(format);
    }

    private String normalizeFormat(String format) {
        if (format == null) {
            return "";
        }
        return format.trim().toLowerCase(Locale.ROOT);
    }

    private String toMimeType(String formatName) {
        return switch (formatName) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }

    private String extension(String formatName) {
        return "jpeg".equals(formatName) ? "jpg" : formatName;
    }

    private MediaValidationException invalidImage() {
        return new MediaValidationException(
                "Invalid image file",
                "INVALID_IMAGE_FILE"
        );
    }

    private record DecodedImage(
            BufferedImage image,
            int width,
            int height,
            String mimeType
    ) {
    }
}
