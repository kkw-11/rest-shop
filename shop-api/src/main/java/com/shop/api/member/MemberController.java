package com.shop.api.member;

import com.shop.api.member.dto.MemberResponse;
import com.shop.common.dto.ApiResponse;
import com.shop.common.exception.CustomException;
import com.shop.common.exception.ErrorCode;
import com.shop.core.member.MemberService;
import com.shop.domain.member.Member;
import com.shop.domain.member.MemberRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@Tag(name = "회원 API", description = "회원 정보 조회 및 관리")
public class MemberController {
    private final MemberService memberService;
    private final MemberRepository memberRepository;

    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 정보를 조회 합니다.")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberResponse>> getMyInfo() {
        // SecurityContext에서 인증된 사용자 ID 추출
        Long memberId = getCurrentMemberId();

        Member member = memberService.findById(memberId);
        MemberResponse response = MemberResponse.from(member);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private Long getCurrentMemberId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if(authentication == null || !authentication.isAuthenticated()) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        return (Long)authentication.getPrincipal();
    }
}
