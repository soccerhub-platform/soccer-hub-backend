package kz.edu.soccerhub.dispacher.domain.model;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "dispatcher_club")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DispatcherClub {

    @EmbeddedId
    private DispatcherClubId id;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DispatcherClubId implements Serializable {
        private UUID dispatcherId;
        private UUID clubId;
    }

}
