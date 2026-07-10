package kz.edu.soccerhub.media.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import kz.edu.soccerhub.common.domain.model.AbstractAuditableEntity;
import kz.edu.soccerhub.media.domain.enums.MediaKind;
import kz.edu.soccerhub.media.domain.enums.MediaOwnerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "media_asset")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaAsset extends AbstractAuditableEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false)
    private MediaOwnerType ownerType;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false)
    private MediaKind kind;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "mime_type", nullable = false)
    private String mimeType;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "original_storage_key", nullable = false, length = 1024)
    private String originalStorageKey;

    @Column(name = "thumb_storage_key", length = 1024)
    private String thumbStorageKey;

    @Column(name = "medium_storage_key", length = 1024)
    private String mediumStorageKey;

    @Column(name = "deleted_by")
    private UUID deletedBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public boolean isActive() {
        return deletedAt == null;
    }

    public void markDeleted(
            UUID actorId,
            LocalDateTime deletedAt
    ) {
        this.deletedBy = actorId;
        this.deletedAt = deletedAt;
    }

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }

        validate();
    }

    @PreUpdate
    void preUpdate() {
        validate();
    }

    private void validate() {
        requirePresent(ownerType, "Owner type");
        requirePresent(ownerId, "Owner id");
        requirePresent(kind, "Media kind");
        requireText(fileName, "File name");
        requireText(mimeType, "Mime type");
        requireText(originalStorageKey, "Original storage key");

        if (sizeBytes == null || sizeBytes < 0) {
            throw new IllegalArgumentException(
                    "Size bytes must not be negative"
            );
        }

        if (width != null && width <= 0) {
            throw new IllegalArgumentException(
                    "Width must be positive"
            );
        }

        if (height != null && height <= 0) {
            throw new IllegalArgumentException(
                    "Height must be positive"
            );
        }
    }

    private void requirePresent(
            Object value,
            String fieldName
    ) {
        if (value == null) {
            throw new IllegalArgumentException(
                    fieldName + " must not be null"
            );
        }
    }

    private void requireText(
            String value,
            String fieldName
    ) {
        if (
                value == null
                        || value.isBlank()
        ) {
            throw new IllegalArgumentException(
                    fieldName + " must not be blank"
            );
        }
    }
}
