package com.baseshop.member.service;

import com.baseshop.member.dto.MemberCreateReqDto;
import com.baseshop.member.dto.MemberDetailResDto;
import com.baseshop.member.entity.Member;
import com.baseshop.member.repository.MemberRepository;
import com.baseshop.member.type.MemberRoleType;
import lombok.RequiredArgsConstructor;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Long create(MemberCreateReqDto memberCreateReqDto){

        if(existsByUsername(memberCreateReqDto.getUsername())){
            throw new IllegalArgumentException("이미 사용중인 아이디 입니다.");
        }

        String email = memberCreateReqDto.getEmail().trim().toLowerCase();

        if(!EmailValidator.getInstance().isValid(email)){
            throw new IllegalArgumentException("이메일 형식이 올바르지 않습니다.");
        }

        if(existsByEmail(email)){
            throw new IllegalArgumentException("이미 사용중인 이메일 입니다.");
        }

        Member member = Member.builder()
                .username(memberCreateReqDto.getUsername())
                .password(passwordEncoder.encode(memberCreateReqDto.getPassword()))
                .email(email)
                .memberRoleType(MemberRoleType.USER)
                .build();

        return memberRepository.save(member).getId();
    }
    
    // 아이디 중복확인
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username){
        return memberRepository.existsByUsername(username);
    }

    // 이메일 중복확인
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email){
        return memberRepository.existsByEmail(email);
    }

    @Transactional(readOnly = true)
    public MemberDetailResDto getMemberByUsername(String username) {
        Member member = memberRepository.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("회원 정보가 없습니다."));

        return MemberDetailResDto.builder()
                .id(member.getId())
                .username(member.getUsername())
                .password(member.getPassword())
                .memberRoleType(member.getMemberRoleType())
                .build();
    }


}
