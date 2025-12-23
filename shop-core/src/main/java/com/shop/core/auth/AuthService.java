package com.shop.core.auth;

import com.shop.common.exception.CustomException;
import com.shop.common.exception.ErrorCode;
import com.shop.core.security.JwtTokenProvider;
import com.shop.domain.auth.RefreshToken;
import com.shop.domain.auth.RefreshTokenRepository;
import com.shop.domain.member.Member;
import com.shop.domain.member.MemberRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;


    /**
     * 로그인
     * @param email
     * @param password
     * @return
     */
    @Transactional
    public TokenInfo login(String email, String password) {
        //회원 조회
        Member member = memberRepository.findByEmail(email).orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        //비밀 번호 검증
        if (!passwordEncoder.matches(password, member.getPassword())) {
            throw new IllegalArgumentException(ErrorCode.INVALID_PASSWORD.getMessage());
        }

        //Access Token 생성
        String accessToken = jwtTokenProvider.createAccessToken(member.getId(), member.getEmail());

        //Refresh Token 생성
        String refreshTokenValue = jwtTokenProvider.createRefreshToken();

        //기존 RefreshToken 있다면 삭제
        refreshTokenRepository.findByMember_Id(member.getId()).ifPresent(refreshTokenRepository::delete);

        RefreshToken refreshtoken = RefreshToken.builder()
                .token(refreshTokenValue)
                .member(member)
                .expireDate(LocalDateTime.now().plusDays(14))
                .build();

        refreshTokenRepository.save(refreshtoken);

        log.info("로그인 성공 email: {}", email);

        return new TokenInfo(accessToken, refreshTokenValue);

    }


    /**
     * Access Token 재발급
     */
    @Transactional
    public TokenInfo refreshToken(String refreshTokenValue) {
        // RefreshToken 조회
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue).orElseThrow(() -> new IllegalArgumentException(ErrorCode.INVALID_TOKEN.getMessage()));

        //만료 확인
        if(refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new IllegalArgumentException(ErrorCode.EXPIRED_TOKEN.getMessage());
        }

        // 새 Access Token 발급
        Member member = refreshToken.getMember();
        String newAccessToken = jwtTokenProvider.createAccessToken(member.getId(), member.getEmail());

        log.info("AccessToken 재발급:  memberId = {}", member.getEmail());

        return new TokenInfo(newAccessToken, refreshTokenValue);
    }

    /**
     * 로그아웃
     */
    @Transactional
    public void logout(Long memberId) {
        refreshTokenRepository.findByMember_Id(memberId)
                .ifPresent(refreshToken -> {
                    refreshTokenRepository.delete(refreshToken);
                    log.info("로그아웃 완료: memberId={}", memberId);
                });
    }

    /**
     * 토큰 정보를 담는 내부 클래스
     */
    @RequiredArgsConstructor
    @Getter
    public static class TokenInfo {
        private final String accessToken;
        private final String refreshToken;
    }
}
