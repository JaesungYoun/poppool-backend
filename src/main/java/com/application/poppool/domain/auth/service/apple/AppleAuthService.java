package com.application.poppool.domain.auth.service.apple;

import com.application.poppool.domain.auth.dto.info.ApplePublicKeys;
import com.application.poppool.domain.auth.dto.request.AppleLoginRequest;
import com.application.poppool.domain.auth.dto.response.LoginResponse;
import com.application.poppool.domain.auth.enums.SocialType;
import com.application.poppool.domain.token.entity.RefreshTokenEntity;
import com.application.poppool.domain.token.repository.RefreshTokenRepository;
import com.application.poppool.domain.user.repository.UserRepository;
import com.application.poppool.global.exception.BadRequestException;
import com.application.poppool.global.exception.ErrorCode;
import com.application.poppool.global.jwt.JwtService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppleAuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AppleAuthFeignClient appleAuthFeignClient;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${oauth.apple.auth-url}")
    private String appleAuthUrl;

    /**
     * 애플 로그인
     *
     * @param appleLoginRequest
     * @param response
     * @return
     */
    @Transactional
    public LoginResponse appleLogin(AppleLoginRequest appleLoginRequest, HttpServletResponse response) {

        // 1. 애플 ID 토큰 검증 및 애플 사용자 고유 식별자 추출
        String appleIdTokenSub = validateAppleIdTokenAndExtractSub(appleLoginRequest.getIdToken());

        if (appleIdTokenSub == null) {
            throw new BadRequestException(ErrorCode.AUTHENTICATION_FAIL_EXCEPTION);
        }

        String userId = appleIdTokenSub + SocialType.APPLE.getSocialSuffix();
        boolean isRegisteredUser = true;
        boolean isTemporaryToken = false;

        /**
         * 기존 회원이 아닌 경우, 회원가입 대상 -> isUser 구분 값 false 설정 및 임시토큰 설정
         */
        if (!userRepository.findByUserId(userId).isPresent()) {
            isRegisteredUser = false;
            isTemporaryToken = true;
        }

        // 로그인 응답
        LoginResponse loginResponse = jwtService.createJwtToken(userId, isTemporaryToken);
        // 헤더에 토큰 싣기
        jwtService.setHeaderAccessToken(response, loginResponse.getAccessToken());
        jwtService.setHeaderRefreshToken(response, loginResponse.getRefreshToken());

        // 리프레쉬 토큰 엔티티 생성
        RefreshTokenEntity refreshToken = RefreshTokenEntity.builder()
                .userId(userId)
                .token(loginResponse.getRefreshToken())
                .build();

        // 리프레쉬 토큰 저장
        refreshTokenRepository.save(refreshToken);

        return LoginResponse.builder()
                .userId(userId)
                .grantType(loginResponse.getGrantType())
                .accessToken(loginResponse.getAccessToken())
                .accessTokenExpiresAt(loginResponse.getAccessTokenExpiresAt())
                .refreshToken(loginResponse.getRefreshToken())
                .refreshTokenExpiresAt(loginResponse.getRefreshTokenExpiresAt())
                .socialType(SocialType.APPLE)
                .isRegisteredUser(isRegisteredUser)
                .build();

    }

    /**
     * 애플 ID 토큰 검증 및 애플 고유 식별자 추출
     *
     * @param idToken
     * @return
     */
    public String validateAppleIdTokenAndExtractSub(String idToken) {
        try {

            // 1. API 요청을 통해 애플 공개 키 가져오기
            List<ApplePublicKeys.Key> keys = this.getApplePublicKeys();


            // 2. idToken 디코딩
            JsonNode decodedIdToken = this.decodeIdToken(idToken);
            String kid = decodedIdToken != null ? decodedIdToken.get("kid").asText() : null;

            // 3. ID 토큰의 kid (Key ID)와 매칭되는 공개 키 찾기
            ApplePublicKeys.Key matchingKey = this.getMatchingKey(keys, kid);

            // 4. 공개 키 생성
            PublicKey publicKey = this.generatePublicKey(matchingKey);

            // 5. setSigningKey(publicKey)를 통해 검증 및 claim 생성
            Claims claims = this.createClaim(publicKey, idToken);

            // 추가 검증 로직 (예: issuer, audience 등)
            if (!appleAuthUrl.equals(claims.getIssuer())) {
                throw new IllegalArgumentException("Invalid issuer");
            }

            return claims.getSubject();
        } catch (JsonProcessingException e) {
            return null;
        } catch (NoSuchAlgorithmException e) {
            return null;
        } catch (InvalidKeySpecException e) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 1. Apple PublicKey 요청
     *
     * @return
     */
    private List<ApplePublicKeys.Key> getApplePublicKeys() {
        ApplePublicKeys applePublicKeys = appleAuthFeignClient.getAppleAuthPublicKey();
        List<ApplePublicKeys.Key> keys = applePublicKeys.getKeys();
        return keys;
    }

    /**
     * 2. Id Token 디코딩
     *
     * @param idToken
     * @return
     * @throws JsonProcessingException
     */
    private JsonNode decodeIdToken(String idToken) throws JsonProcessingException {
        String[] parts = idToken.split("\\.");
        String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));
        JsonNode decodedIdToken = new ObjectMapper().readTree(headerJson);
        return decodedIdToken;
    }

    /**
     * 3. ID 토큰의 kid (Key ID)와 매칭되는 공개 키 찾기
     *
     * @param keys
     * @param kid
     * @return
     */
    private ApplePublicKeys.Key getMatchingKey(List<ApplePublicKeys.Key> keys, String kid) {
        return keys.stream()
                .filter(key -> key.getKid().equals(kid))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No matching key found"));
    }

    /**
     * 4. 공개 키 생성
     *
     * @param matchingKey
     * @return
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    private PublicKey generatePublicKey(ApplePublicKeys.Key matchingKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] nBytes = Base64.getUrlDecoder().decode(matchingKey.getN());
        byte[] eBytes = Base64.getUrlDecoder().decode(matchingKey.getE());
        return KeyFactory.getInstance("RSA")
                .generatePublic(new RSAPublicKeySpec(new BigInteger(1, nBytes), new BigInteger(1, eBytes)));
    }

    /**
     * 5. setSigningKey(publicKey)를 통해 검증 및 claim 생성
     *
     * @param publicKey
     * @param idToken
     * @return
     */
    private Claims createClaim(PublicKey publicKey, String idToken) {
        return Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build()
                .parseClaimsJws(idToken)
                .getBody();
    }

}
