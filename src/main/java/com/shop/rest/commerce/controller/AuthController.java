package com.shop.rest.commerce.controller;

import com.shop.rest.commerce.constant.ResponseMessage;
import com.shop.rest.commerce.dto.ApiResponse;
import com.shop.rest.commerce.dto.MemberResponse;
import com.shop.rest.commerce.entity.Member;
import com.shop.rest.commerce.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final MemberService memberService;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<MemberResponse>> register(@RequestBody Member member) {
        // 비밀번호 암호화
        member.setPassword(passwordEncoder.encode(member.getPassword()));

        // 회원 저장
        Member savedMember = memberService.saveMember(member);

        // DTO 변환
        MemberResponse response = MemberResponse.from(savedMember);

        // 201 Created 상태코드와 함께 응답
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(ResponseMessage.MEMBER_REGISTERED.getMessage(), response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MemberResponse>> getMemberById(@PathVariable Long id) {
        Member member = memberService.findById(id);
        MemberResponse response = MemberResponse.from(member);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<MemberResponse>> getMemberByEmail(@RequestParam String email) {
        Member member = memberService.findByEmail(email);
        MemberResponse response = MemberResponse.from(member);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}