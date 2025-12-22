package com.shop.core.security;

import com.shop.domain.member.Member;
import com.shop.domain.member.MemberRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 인증 필터
 * 모든 HTTP 요청에서 JWT 토큰을 검증하고 인증 정보를 SecurityContext에 저장
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
        private final JwtTokenProvider jwtTokenProvider;
        private final MemberRepository memberRepository;

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
            try {
                // 1. Request Header에서 JWT 토큰 추출
                log.info("Request Header Authorization: {}", request.getHeader("Authorization"));
                String token = extractTokenFromRequest(request);

                // 2. 토큰이 있고, 유효하면 인증정보 설정
                if(token != null && jwtTokenProvider.validateToken(token)) {
                    // 토큰에서 memberId 추출
                    Long memberId = jwtTokenProvider.getMemberId(token);
                    String email = jwtTokenProvider.getEmail(token);
                    Member member = memberRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException(email));

                    //회원 권한 설정
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(memberId, null, List.of(new SimpleGrantedAuthority("ROLE_" + member.getRole().USER.name())));

                    //Security 인증정보 저장
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.debug("JWT 인증 성공: memberId = {}, email = {} ", memberId, email);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            //다음 필터로 진행
            filterChain.doFilter(request, response);
    }

    /**
     * Request Header에서 Bearer Token 추출
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if(StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);//"Bearer " 제거
        }

        return null;
    }
}
