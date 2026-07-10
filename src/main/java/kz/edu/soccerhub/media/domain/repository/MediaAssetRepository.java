package kz.edu.soccerhub.media.domain.repository;

import kz.edu.soccerhub.media.domain.enums.MediaKind;
import kz.edu.soccerhub.media.domain.enums.MediaOwnerType;
import kz.edu.soccerhub.media.domain.model.MediaAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MediaAssetRepository extends JpaRepository<MediaAsset, UUID> {

    @Query("""
            select asset
            from MediaAsset asset
            where asset.ownerType = :ownerType
              and asset.ownerId = :ownerId
              and asset.kind = :kind
              and asset.deletedAt is null
            """)
    Optional<MediaAsset> findActiveByOwner(
            MediaOwnerType ownerType,
            UUID ownerId,
            MediaKind kind
    );

    @Query("""
            select asset
            from MediaAsset asset
            where asset.ownerType = :ownerType
              and asset.ownerId in :ownerIds
              and asset.kind = :kind
              and asset.deletedAt is null
            """)
    List<MediaAsset> findAllActiveByOwnerTypeAndOwnerIdInAndKind(
            MediaOwnerType ownerType,
            Collection<UUID> ownerIds,
            MediaKind kind
    );

    @Query("""
            select asset
            from MediaAsset asset
            where asset.ownerType = :ownerType
              and asset.ownerId = :ownerId
              and asset.kind = :kind
              and asset.deletedAt is null
            """)
    List<MediaAsset> findAllActiveByOwner(
            MediaOwnerType ownerType,
            UUID ownerId,
            MediaKind kind
    );
}
