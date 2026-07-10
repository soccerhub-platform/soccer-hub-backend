package kz.edu.soccerhub.media.domain.enums;

import kz.edu.soccerhub.common.exception.BadRequestException;

import java.util.Arrays;
import java.util.Locale;

public enum MediaVariant {
    ORIGINAL,
    THUMB,
    MEDIUM;

    public String apiValue() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static MediaVariant fromApiValue(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("Media variant is required");
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(variant -> variant.name().equals(normalized))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Unsupported media variant", value));
    }
}
