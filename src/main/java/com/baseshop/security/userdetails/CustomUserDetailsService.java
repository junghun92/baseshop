package com.baseshop.security.userdetails;

import com.baseshop.member.dto.MemberDetailResDto;
import com.baseshop.member.entity.Member;
import com.baseshop.member.repository.MemberRepository;
import com.baseshop.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberService memberService;

    /**
     * 유저정보 -> AuthenticationProvicer (검증)
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        MemberDetailResDto member = memberService.getMemberByUsername(username);

        return CustomUserDetails.builder()
                .memberId(member.getId())
                .username(member.getUsername())
                .password(member.getPassword())
                .memberRoleType(member.getMemberRoleType())
                .build();
    }
}
