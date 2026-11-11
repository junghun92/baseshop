package com.baseshop.coupon.service;

import com.baseshop.coupon.dto.IssuedCouponListResDto;
import com.baseshop.coupon.repository.IssuedCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IssuedCouponService {

    private final IssuedCouponRepository issuedCouponRepository;

    @Transactional(readOnly = true)
    public List<IssuedCouponListResDto> getIssuedCouponList(Long memberId) {
        return issuedCouponRepository.findByMemberIdOrderByIdDesc(memberId).stream()
                .map(IssuedCouponListResDto::from)
                .toList();
    }
}
