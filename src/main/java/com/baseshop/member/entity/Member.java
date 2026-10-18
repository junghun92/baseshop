package com.baseshop.member.entity;

import com.baseshop.common.entity.BaseEntity;
import com.baseshop.member.type.MemberRoleType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "members")
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, columnDefinition = "varchar(20)")
    @Enumerated(EnumType.STRING)
    private MemberRoleType memberRoleType;

    @Builder
    public Member(String username, String password, String email, MemberRoleType memberRoleType) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.memberRoleType = memberRoleType;
    }
}
