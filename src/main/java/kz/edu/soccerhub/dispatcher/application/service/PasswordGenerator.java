package kz.edu.soccerhub.dispatcher.application.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Locale;

@Component
public class PasswordGenerator {

    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = UPPER.toLowerCase(Locale.ROOT);
    private static final String DIGITS = "0123456789";
    private static final String SYMBOLS = "!@#$%^&*()_-+=<>?";
    private static final String ALL = UPPER + LOWER + DIGITS + SYMBOLS;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int DEFAULT_LENGTH = 12;

    public String generate() {
        return generate(DEFAULT_LENGTH);
    }

    public String generate(int length) {
        if (length < 6) {
            throw new IllegalArgumentException("Password length should be at least 6 characters.");
        }

        StringBuilder password = new StringBuilder(length);

        // Ensure at least one of each type
        password.append(randomChar(UPPER));
        password.append(randomChar(LOWER));
        password.append(randomChar(DIGITS));
        password.append(randomChar(SYMBOLS));

        // Fill the rest
        for (int i = 4; i < length; i++) {
            password.append(randomChar(ALL));
        }

        // Shuffle characters
        return shuffle(password.toString());
    }

    private char randomChar(String chars) {
        return chars.charAt(RANDOM.nextInt(chars.length()));
    }

    private String shuffle(String input) {
        char[] chars = input.toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char tmp = chars[i];
            chars[i] = chars[j];
            chars[j] = tmp;
        }
        return new String(chars);
    }
}