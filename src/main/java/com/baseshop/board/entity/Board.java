package com.baseshop.board.entity;

import com.baseshop.board.type.BoardType;
import com.baseshop.common.entity.BaseEntity;
import com.baseshop.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Board extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_id")
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Enumerated(EnumType.STRING)
    private BoardType boardType;

    private String title;

    private String content;

    public void changeBoard(BoardType boardType, String title, String content) {
        this.boardType = boardType;
        this.title = title;
        this.content = content;
    }

    @Builder
    public Board(Member member, BoardType boardType, String title, String content) {
        this.member = member;
        this.boardType = boardType;
        this.title = title;
        this.content = content;
    }

}
