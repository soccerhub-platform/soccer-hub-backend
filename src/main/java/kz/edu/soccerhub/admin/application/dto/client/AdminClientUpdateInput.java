package kz.edu.soccerhub.admin.application.dto.client;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import kz.edu.soccerhub.client.domain.enums.ClientSource;

public record AdminClientUpdateInput(
        @NotBlank String firstName,
        String lastName,
        String phone,
        @Email String email,
        ClientSource source,
        @Size(max = 500) String sourceDetails,
        String comments
) {
    public AdminClientUpdateInput(String firstName, String lastName, String phone, String email,
                                  String source, String comments) {
        this(firstName, lastName, phone, email, parseSource(source), null, comments);
    }

    private static ClientSource parseSource(String value) {
        if (value == null || value.isBlank()) return null;
        try { return ClientSource.valueOf(value.trim().toUpperCase()); }
        catch (IllegalArgumentException ignored) { return ClientSource.OTHER; }
    }
}
