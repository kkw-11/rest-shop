package com.shop.api.auth.dto;

import com.shop.domain.member.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberResponse {
    private Long id;
    private String email;
    private String name;
    private LocalDateTime createdAt;

    // Entity -> DTO 변환
    public static MemberResponse from(Member member) {
        return builder()
                .id(member.getId())
                .email(member.getEmail())
                .name(member.getName())
                .createdAt(member.getRegTime())
                .build();
    }
}