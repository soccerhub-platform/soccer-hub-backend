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
import java.awt.geom.AffineTransform;
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

                int storedWidth = reader.getWidth(0);
                int storedHeight = reader.getHeight(0);
                validateDimensions(storedWidth, storedHeight);

                BufferedImage image = reader.read(0);
                if (image == null) {
                    throw invalidImage();
                }

                BufferedImage orientedImage = applyExifOrientation(
                        image,
                        readExifOrientation(bytes)
                );

                return new DecodedImage(
                        orientedImage,
                        orientedImage.getWidth(),
                        orientedImage.getHeight(),
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

    private int readExifOrientation(byte[] bytes) {
        if (bytes.length < 4 || unsigned(bytes[0]) != 0xFF || unsigned(bytes[1]) != 0xD8) {
            return 1;
        }

        int offset = 2;
        while (offset + 4 <= bytes.length) {
            if (unsigned(bytes[offset]) != 0xFF) {
                break;
            }

            int marker = unsigned(bytes[offset + 1]);
            if (marker == 0xDA || marker == 0xD9) {
                break;
            }

            int segmentLength = readUnsignedShort(bytes, offset + 2, false);
            if (segmentLength < 2 || offset + 2 + segmentLength > bytes.length) {
                break;
            }

            int segmentStart = offset + 4;
            if (marker == 0xE1 && hasExifHeader(bytes, segmentStart, segmentLength - 2)) {
                int orientation = readTiffOrientation(bytes, segmentStart + 6, offset + 2 + segmentLength);
                if (orientation >= 1 && orientation <= 8) {
                    return orientation;
                }
            }

            offset += 2 + segmentLength;
        }

        return 1;
    }

    private boolean hasExifHeader(byte[] bytes, int offset, int available) {
        return available >= 14
                && bytes[offset] == 'E'
                && bytes[offset + 1] == 'x'
                && bytes[offset + 2] == 'i'
                && bytes[offset + 3] == 'f'
                && bytes[offset + 4] == 0
                && bytes[offset + 5] == 0;
    }

    private int readTiffOrientation(byte[] bytes, int tiffStart, int segmentEnd) {
        if (tiffStart + 8 > segmentEnd) {
            return 1;
        }

        boolean littleEndian;
        if (bytes[tiffStart] == 'I' && bytes[tiffStart + 1] == 'I') {
            littleEndian = true;
        } else if (bytes[tiffStart] == 'M' && bytes[tiffStart + 1] == 'M') {
            littleEndian = false;
        } else {
            return 1;
        }

        if (readUnsignedShort(bytes, tiffStart + 2, littleEndian) != 42) {
            return 1;
        }

        long firstIfdOffset = readUnsignedInt(bytes, tiffStart + 4, littleEndian);
        long ifdPosition = tiffStart + firstIfdOffset;
        if (firstIfdOffset < 0 || ifdPosition < tiffStart || ifdPosition + 2 > segmentEnd) {
            return 1;
        }

        int entryCount = readUnsignedShort(bytes, (int) ifdPosition, littleEndian);
        int entryOffset = (int) ifdPosition + 2;
        for (int index = 0; index < entryCount; index++) {
            int entry = entryOffset + index * 12;
            if (entry + 12 > segmentEnd) {
                return 1;
            }

            int tag = readUnsignedShort(bytes, entry, littleEndian);
            if (tag == 0x0112) {
                int type = readUnsignedShort(bytes, entry + 2, littleEndian);
                long count = readUnsignedInt(bytes, entry + 4, littleEndian);
                if (type == 3 && count >= 1) {
                    return readUnsignedShort(bytes, entry + 8, littleEndian);
                }
                return 1;
            }
        }

        return 1;
    }

    private int readUnsignedShort(byte[] bytes, int offset, boolean littleEndian) {
        if (offset < 0 || offset + 2 > bytes.length) {
            return 0;
        }
        int first = unsigned(bytes[offset]);
        int second = unsigned(bytes[offset + 1]);
        return littleEndian ? first | (second << 8) : (first << 8) | second;
    }

    private long readUnsignedInt(byte[] bytes, int offset, boolean littleEndian) {
        if (offset < 0 || offset + 4 > bytes.length) {
            return -1;
        }
        if (littleEndian) {
            return unsigned(bytes[offset])
                    | ((long) unsigned(bytes[offset + 1]) << 8)
                    | ((long) unsigned(bytes[offset + 2]) << 16)
                    | ((long) unsigned(bytes[offset + 3]) << 24);
        }
        return ((long) unsigned(bytes[offset]) << 24)
                | ((long) unsigned(bytes[offset + 1]) << 16)
                | ((long) unsigned(bytes[offset + 2]) << 8)
                | unsigned(bytes[offset + 3]);
    }

    private int unsigned(byte value) {
        return value & 0xFF;
    }

    private BufferedImage applyExifOrientation(BufferedImage source, int orientation) {
        if (orientation <= 1 || orientation > 8) {
            return source;
        }

        int width = source.getWidth();
        int height = source.getHeight();
        boolean swapsDimensions = orientation >= 5;
        BufferedImage target = new BufferedImage(
                swapsDimensions ? height : width,
                swapsDimensions ? width : height,
                source.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB
        );

        AffineTransform transform = switch (orientation) {
            case 2 -> new AffineTransform(-1, 0, 0, 1, width, 0);
            case 3 -> new AffineTransform(-1, 0, 0, -1, width, height);
            case 4 -> new AffineTransform(1, 0, 0, -1, 0, height);
            case 5 -> new AffineTransform(0, 1, 1, 0, 0, 0);
            case 6 -> new AffineTransform(0, 1, -1, 0, height, 0);
            case 7 -> new AffineTransform(0, -1, -1, 0, height, width);
            case 8 -> new AffineTransform(0, -1, 1, 0, 0, width);
            default -> new AffineTransform();
        };

        Graphics2D graphics = target.createGraphics();
        try {
            configureQuality(graphics);
            graphics.drawImage(source, transform, null);
        } finally {
            graphics.dispose();
        }
        return target;
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
