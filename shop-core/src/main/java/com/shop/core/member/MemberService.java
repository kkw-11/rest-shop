package com.shop.core.member;

import com.shop.common.exception.CustomException;
import com.shop.common.exception.ErrorCode;
import com.shop.core.member.dto.CreateMemberCommand;
import com.shop.domain.member.Member;
import com.shop.domain.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService implements UserDetailsService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("회원을 찾을 수 없습니다: " + email));

        return User.builder()
                .username(member.getEmail())
                .password(member.getPassword())
                .roles(member.getRole().name())
                .build();
    }

    /**
     * 회원가입
     */
    @Transactional
    public Long register(CreateMemberCommand command) {
        validateDuplicateEmail(command.getEmail());

        // 비밀번호 암호화 (인프라 계층)
        String encodedPassword = passwordEncoder.encode(command.getPassword());

        // 도메인 로직 (Entity 팩토리 메서드 사용)
        Member member = Member.createMember(
                command.getEmail(),
                encodedPassword,
                command.getName(),
                command.getAddress()
        );

        Member savedMember = memberRepository.save(member);
        log.info("회원가입 완료: email={}", command.getEmail());
        
        return savedMember.getId();
    }

    @Transactional
    public Member saveMember(Member member) {
        validateDuplicateMember(member);
        return memberRepository.save(member);
    }

    public Member findById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }

    public Member findByEmail(String email) {
        return memberRepository.findByEmail(email).orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }

    public List<Member> findAllMembers() {
        return memberRepository.findAll();
    }

    public boolean existsByEmail(String email) {
        return memberRepository.existsByEmail(email);
    }

    @Transactional
    public void deleteMember(Long id) {
        memberRepository.deleteById(id);
    }

    /**
     * 이메일 중복 검증
     */
    private void validateDuplicateEmail(String email) {
        if (memberRepository.existsByEmail(email)) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }
    }

    /**
     * 회원 중복 검증 (Member 객체용)
     */
    private void validateDuplicateMember(Member member) {
        validateDuplicateEmail(member.getEmail());
    }
}
