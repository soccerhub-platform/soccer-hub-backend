package kz.edu.soccerhub.common.domain.model;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class Client {
    private UUID id;
    private UUID userId;
    private String firstName;
    private String lastName;
    private String phone;
    private LocalDateTime createdAt;
}
