package com.shop.api.auth;

import com.shop.api.auth.dto.*;
import com.shop.common.constant.ResponseMessage;
import com.shop.common.dto.ApiResponse;
import com.shop.core.auth.AuthService;
import com.shop.core.member.MemberService;
import com.shop.core.member.dto.CreateMemberCommand;
import com.shop.domain.member.Member;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "인증 API", description = "회원가입, 로그인, 토큰 관리 API")
public class AuthController {

    private final MemberService memberService;
    private final AuthService authService;

    /**
     * 회원가입
     */
    @Operation(summary = "회원가입", description = "이메일, 비밀번호, 이름, 주소를 입력하여 회원가입합니다.")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Long>> register(@RequestBody RegisterRequest request) {
        // DTO → Command 변환
        CreateMemberCommand command = CreateMemberCommand.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .name(request.getName())
                .address(request.getAddress())
                .build();

        // 회원가입
        Long memberId = memberService.register(command);

        log.info("회원가입 API 호출 완료: memberId={}", memberId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(ResponseMessage.MEMBER_REGISTERED.getMessage(), memberId));
    }

    /**
     * 로그인
     */
    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인하여 Access Token과 Refresh Token을 발급받습니다.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@RequestBody LoginRequest request) {
        // 로그인 처리
        AuthService.TokenInfo tokenInfo = authService.login(
                request.getEmail(),
                request.getPassword()
        );

        // TokenResponse 생성
        TokenResponse response = TokenResponse.of(
                tokenInfo.getAccessToken(),
                tokenInfo.getRefreshToken()
        );

        log.info("로그인 API 호출 완료: email={}", request.getEmail());

        return ResponseEntity.ok(ApiResponse.success("로그인에 성공했습니다.", response));
    }

    /**
     * Access Token 재발급
     */
    @Operation(summary = "토큰 갱신", description = "Refresh Token으로 새로운 Access Token을 발급받습니다.")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(@RequestBody TokenRefreshRequest request) {
        // Access Token 재발급
        AuthService.TokenInfo tokenInfo = authService.refresh(request.getRefreshToken());

        // TokenResponse 생성
        TokenResponse response = TokenResponse.of(
                tokenInfo.getAccessToken(),
                tokenInfo.getRefreshToken()
        );

        log.info("토큰 갱신 API 호출 완료");

        return ResponseEntity.ok(ApiResponse.success("토큰이 갱신되었습니다.", response));
    }

    /**
     * 로그아웃
     */
    @Operation(summary = "로그아웃", description = "로그아웃하여 Refresh Token을 무효화합니다.")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        // SecurityContext에서 인증된 사용자 ID 추출
        Long memberId = getCurrentMemberId();

        authService.logout(memberId);

        log.info("로그아웃 API 호출 완료: memberId={}", memberId);

        return ResponseEntity.ok(ApiResponse.success("로그아웃되었습니다.", null));
    }

    /**
     * 내 정보 조회
     */
    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 정보를 조회합니다.")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberResponse>> getMyInfo() {
        // SecurityContext에서 인증된 사용자 ID 추출
        Long memberId = getCurrentMemberId();

        Member member = memberService.findById(memberId);
        MemberResponse response = MemberResponse.from(member);

        log.info("내 정보 조회 API 호출 완료: memberId={}", memberId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * SecurityContext에서 현재 인증된 사용자 ID 추출
     */
    private Long getCurrentMemberId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("인증되지 않은 사용자입니다.");
        }

        // JwtAuthenticationFilter에서 principal을 memberId로 설정했음
        return (Long) authentication.getPrincipal();
    }
}
