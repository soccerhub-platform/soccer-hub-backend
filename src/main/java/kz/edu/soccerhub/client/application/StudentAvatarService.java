package kz.edu.soccerhub.client.application;

import kz.edu.soccerhub.common.dto.media.MediaAssetResponse;
import kz.edu.soccerhub.common.dto.media.MediaDownloadUrlResponse;
import kz.edu.soccerhub.common.dto.student.StudentProfileDto;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.AdminBranchAccessPort;
import kz.edu.soccerhub.common.port.AdminPort;
import kz.edu.soccerhub.common.port.ClientPort;
import kz.edu.soccerhub.common.port.MediaAccessPort;
import kz.edu.soccerhub.common.port.MediaAvatarPort;
import kz.edu.soccerhub.media.domain.enums.MediaOwnerType;
import kz.edu.soccerhub.media.domain.enums.MediaVariant;
import kz.edu.soccerhub.media.domain.model.MediaAsset;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudentAvatarService {

    private final ClientPort clientPort;
    private final AdminPort adminPort;
    private final AdminBranchAccessPort adminBranchAccessPort;
    private final MediaAvatarPort mediaAvatarPort;
    private final MediaAccessPort mediaAccessPort;

    public MediaAssetResponse uploadAvatar(
            UUID adminId,
            UUID playerId,
            MultipartFile file
    ) {
        validatePlayerAccess(adminId, playerId);

        MediaAsset avatar = mediaAvatarPort.uploadAvatar(
                MediaOwnerType.PLAYER,
                playerId,
                adminId,
                file
        );

        return mediaAccessPort.toResponse(avatar);
    }

    public void deleteAvatar(
            UUID adminId,
            UUID playerId
    ) {
        validatePlayerAccess(adminId, playerId);

        mediaAvatarPort.deleteAvatar(
                MediaOwnerType.PLAYER,
                playerId,
                adminId
        );
    }

    public MediaDownloadUrlResponse getAvatarDownloadUrl(
            UUID adminId,
            UUID playerId
    ) {
        validatePlayerAccess(adminId, playerId);

        MediaAsset avatar = mediaAvatarPort.findActiveAvatar(
                        MediaOwnerType.PLAYER,
                        playerId
                )
                .orElseThrow(() -> new NotFoundException("Player avatar not found", playerId));

        return new MediaDownloadUrlResponse(
                mediaAccessPort.createContentUrl(avatar, MediaVariant.ORIGINAL)
        );
    }

    private void validatePlayerAccess(
            UUID adminId,
            UUID playerId
    ) {
        if (!adminPort.verifyAdmin(adminId)) {
            throw new NotFoundException("Admin not found", adminId);
        }

        StudentProfileDto profile = clientPort.getStudentProfile(playerId);
        if (!adminBranchAccessPort.verifyAdminBelongsToBranch(adminId, profile.branchId())) {
            throw new BadRequestException(
                    "Admin does not have access to branch",
                    profile.branchId()
            );
        }
    }
}
