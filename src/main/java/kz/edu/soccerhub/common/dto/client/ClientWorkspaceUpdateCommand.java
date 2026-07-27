package kz.edu.soccerhub.common.dto.client;

import kz.edu.soccerhub.client.domain.enums.ClientSource;
import java.util.UUID;

public record ClientWorkspaceUpdateCommand(
        UUID clientId,
        String firstName,
        String lastName,
        String phone,
        String email,
        ClientSource source,
        String sourceDetails,
        String comments
) {
    public ClientWorkspaceUpdateCommand(UUID clientId, String firstName, String lastName, String phone,
                                        String email, String source, String comments) {
        this(clientId, firstName, lastName, phone, email, parseSource(source), null, comments);
    }

    private static ClientSource parseSource(String value) {
        if (value == null || value.isBlank()) return null;
        try { return ClientSource.valueOf(value.trim().toUpperCase()); }
        catch (IllegalArgumentException ignored) { return ClientSource.OTHER; }
    }
}
