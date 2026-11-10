package com.collaboffice.user.service;

import com.collaboffice.user.dto.TokenPairDTO;

public interface TokenService {

    TokenPairDTO generateTokenPair(Long userId);

    String refreshAccessToken(String refreshToken);

    void revokeToken(String accessToken);

    boolean validateRefreshToken(String refreshToken);
}
