package com.baseshop.board.service;

import com.baseshop.board.entity.BoardViewCount;
import com.baseshop.board.repository.BoardViewCountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BoardViewCountService {

    private final BoardViewCountRepository boardViewCountRepository;

    @Transactional
    public void updateViewCount(Long boardId, Long viewCount){
        int result = boardViewCountRepository.updateViewCount(boardId, viewCount);
        if(result == 0){
            boardViewCountRepository.findById(boardId).ifPresentOrElse(ignored -> { },
                    () -> boardViewCountRepository.save(BoardViewCount.init(boardId, viewCount))
            );
        }
    }
}
