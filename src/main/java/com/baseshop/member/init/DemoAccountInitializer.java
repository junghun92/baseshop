package com.baseshop.member.init;

import com.baseshop.member.entity.Member;
import com.baseshop.member.repository.MemberRepository;
import com.baseshop.member.type.MemberRoleType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DemoAccountInitializer implements ApplicationRunner {

    private static final String DEMO_PASSWORD = "demo1234!";

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;


    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        createDemoMember("demo_user", "demo_user@baseshop.com", MemberRoleType.USER);
        createDemoMember("demo_admin", "demo_admin@baseshop.com", MemberRoleType.ADMIN);
    }

    private void createDemoMember(String username, String email, MemberRoleType role) {
        if (memberRepository.existsByUsername(username)) return;
        Member member = Member.builder()
                .username(username)
                .password(passwordEncoder.encode(DEMO_PASSWORD))
                .email(email)
                .memberRoleType(role)
                .build();
        memberRepository.save(member);
    }
}
