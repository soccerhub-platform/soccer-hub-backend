package kz.edu.soccerhub.media.api;

import kz.edu.soccerhub.media.application.access.MediaAccessService;
import kz.edu.soccerhub.media.application.access.MediaContent;
import kz.edu.soccerhub.media.domain.enums.MediaVariant;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaContentController {

    private final MediaAccessService mediaAccessService;

    @GetMapping("/{assetId}/content")
    public ResponseEntity<InputStreamResource> content(
            @PathVariable UUID assetId,
            @RequestParam String variant,
            @RequestParam String token
    ) {
        MediaContent content = mediaAccessService.loadContent(assetId, MediaVariant.fromApiValue(variant), token);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + content.fileName() + "\"")
                .cacheControl(CacheControl.maxAge(15, TimeUnit.MINUTES).cachePrivate())
                .body(new InputStreamResource(content.inputStream()));
    }
}
