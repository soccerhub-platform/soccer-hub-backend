package kz.edu.soccerhub.admin.application.dto.client;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kz.edu.soccerhub.client.domain.enums.ClientSource;

import java.util.UUID;

public record AdminClientCreateInput(
        @NotNull UUID branchId,
        @NotBlank String firstName,
        String lastName,
        String phone,
        @Email String email,
        ClientSource source,
        @Size(max = 500) String sourceDetails,
        String comments
) {
    public AdminClientCreateInput(UUID branchId, String firstName, String lastName, String phone,
                                  String email, String source, String comments) {
        this(branchId, firstName, lastName, phone, email, parseSource(source), null, comments);
    }

    private static ClientSource parseSource(String value) {
        if (value == null || value.isBlank()) return null;
        try { return ClientSource.valueOf(value.trim().toUpperCase()); }
        catch (IllegalArgumentException ignored) { return ClientSource.OTHER; }
    }
}
