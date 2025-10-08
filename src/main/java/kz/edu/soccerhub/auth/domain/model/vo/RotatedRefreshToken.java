package kz.edu.soccerhub.auth.domain.model.vo;

import kz.edu.soccerhub.auth.domain.model.AppUserEntity;

public record RotatedRefreshToken(
        AppUserEntity user,
        String newRefreshToken) {}