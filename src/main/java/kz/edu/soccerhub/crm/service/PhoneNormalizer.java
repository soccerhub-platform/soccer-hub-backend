package kz.edu.soccerhub.crm.service;

import kz.edu.soccerhub.common.exception.BadRequestException;

public class PhoneNormalizer {

    public static String normalize(String phone) {

        if (phone == null || phone.isBlank()) {
            throw new BadRequestException("Phone is required");
        }

        String cleaned = phone.replaceAll("[^0-9]", "");

        if (cleaned.startsWith("8")) {
            cleaned = "7" + cleaned.substring(1);
        }

        if (!cleaned.startsWith("7")) {
            throw new BadRequestException("Phone must start with 7");
        }

        if (cleaned.length() != 11) {
            throw new BadRequestException("Invalid phone length");
        }

        return "+" + cleaned;
    }
}