package kz.edu.soccerhub.common.domain.model;

import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class Player {
    private UUID id;
    private UUID clientId;
    private LocalDate birthDate;
    private String position;
    private LocalDateTime createdAt;
}