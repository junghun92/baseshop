package com.baseshop.coupon.service;

import com.baseshop.common.exception.BusinessException;
import com.baseshop.common.exception.ErrorCode;
import com.baseshop.coupon.dto.IssuedCouponListResDto;
import com.baseshop.coupon.entity.IssuedCoupon;
import com.baseshop.coupon.repository.IssuedCouponRepository;
import com.baseshop.coupon.type.IssuedCouponStatusType;
import com.baseshop.order.entity.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    /**
     * 주문 : 쿠폰 사용 -> 할인 금액 반환
     * - 주문 생성 트랜잭션 안에서만 호출 (MANDATORY)
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public long updateIssuedCouponUse(Long memberId, Long issuedCouponId, Order order) {
        LocalDateTime now = LocalDateTime.now();

        // 1. 내 받은 쿠폰 조회 (회원 ID 조건 = 내 쿠폰 확인)
        IssuedCoupon issuedCoupon = issuedCouponRepository.findByIdAndMemberId(issuedCouponId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ISSUED_COUPON_NOT_FOUND));

        // 2. 이 주문에 쓸 수 있는지 (사용 가능 상태, 만료 전, 최소 주문 금액)
        issuedCoupon.validateUsable(order.getTotalPrice(), now);

        // 3. 사용 처리 : ISSUED 일 때만 USED 로
        int updated = issuedCouponRepository.updateIssuedCouponUse(
                issuedCouponId, order, now, IssuedCouponStatusType.USED, IssuedCouponStatusType.ISSUED);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.ISSUED_COUPON_NOT_USABLE);
        }

        // 4. 할인 금액
        return issuedCoupon.getCoupon().getDiscountAmount();
    }

    /**
     * 결제 실패·취소·만료 : 이 주문에 쓴 쿠폰 복구
     * - 재고 복구와 같은 트랜잭션 안에서만 호출 (MANDATORY)
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void updateIssuedCouponRestore(Long orderId) {
        // 이 주문에 쓴 쿠폰 조회 -> 복구 (쿠폰 안 쓴 주문이면 빈 목록이라 아무 일 없음)
        issuedCouponRepository.findAllByOrderId(orderId)
                .forEach(IssuedCoupon::restore);
    }
}
