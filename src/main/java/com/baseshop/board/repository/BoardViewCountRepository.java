package com.baseshop.board.repository;

import com.baseshop.board.entity.BoardViewCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BoardViewCountRepository extends JpaRepository<BoardViewCount, Long> {

    @Modifying
    @Query("update BoardViewCount b set b.viewCount = :viewCount where b.boardId = :boardId and b.viewCount < :viewCount")
    int updateViewCount(@Param("boardId") Long boardId, @Param("viewCount") Long viewCount);
}
