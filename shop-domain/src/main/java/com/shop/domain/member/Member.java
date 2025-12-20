package com.shop.domain.member;

import com.shop.common.constant.Role;
import com.shop.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "member")
@Getter
@NoArgsConstructor(access =  AccessLevel.PROTECTED)
@ToString
public class Member extends BaseEntity {
    @Id
    @Column(name="member_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true)
    private String email;

    private String password;

    private String address;

    @Enumerated(EnumType.STRING)
    private Role role;

    /**
     * 정적 팩토리 메서드 - 신규회원 생성
     * @param email
     * @param encodedPassword
     * @param name
     * @param address
     * @return
     */

    public static Member createMember(
            String email,
            String encodedPassword,
            String name,
            String address
    ) {
        Member member = new Member();
        member.email = email;
        member.password = encodedPassword;
        member.name = name;
        member.address = address;
        member.role = Role.USER; //비즈니스 규칙 신규회원은 기본적으로 USER 권한
        return member;
    }
}
