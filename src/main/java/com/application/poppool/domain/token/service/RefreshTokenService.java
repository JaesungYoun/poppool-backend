package com.application.poppool.domain.token.service;

import com.application.poppool.domain.token.entity.RefreshTokenEntity;
import com.application.poppool.domain.token.repository.RefreshTokenRepository;
import com.application.poppool.global.exception.BadRequestException;
import com.application.poppool.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * 요청한 유저의 RefreshToken이 맞는지, 그리고 db 테이블에 있는 토큰이랑 동일한지 유효성 검증
     * @param userId
     * @param refreshToken
     * @return
     */
    public boolean isUserRefreshTokenValid(String userId, String refreshToken) {

        // userId가 없으면 예외처리 -> 이것도 유효하지 않은 것으로 간주해야함
        RefreshTokenEntity storedRefreshToken = refreshTokenRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException(ErrorCode.DATA_VALIDATION_ERROR));

        // 같은지 비교
        return storedRefreshToken.equals(refreshToken);
    }

    /**
     * 새로운 RefreshToken 발급하면서 db 테이블에도 새로운 RefreshToken으로 대체 (RTR)
     * @param userId
     * @param newRefreshToken
     */
    @Transactional
    public void replaceRefreshToken(String userId, String newRefreshToken) {
        refreshTokenRepository.findByUserId(userId)
                .ifPresent(token -> {token.updateToken(newRefreshToken);
                refreshTokenRepository.save(token);
                });

    }

    /**
     * 로그아웃 -> RefreshToken 삭제
     * @param userId
     */
    @Transactional
    public void deleteRefreshToken(String userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

}
