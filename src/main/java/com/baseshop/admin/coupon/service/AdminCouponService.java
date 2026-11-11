package com.baseshop.admin.coupon.service;

import com.baseshop.admin.coupon.dto.*;
import com.baseshop.admin.coupon.repository.AdminCouponQueryRepository;
import com.baseshop.common.dto.PagingResDto;
import com.baseshop.coupon.entity.Coupon;
import com.baseshop.coupon.repository.CouponRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminCouponService {

    private final CouponRepository couponRepository;
    private final AdminCouponQueryRepository adminCouponQueryRepository;

    @Transactional
    public Long create(AdminCouponCreateReqDto adminCouponCreateReqDto){
        validateIssuePeriod(adminCouponCreateReqDto.getIssueStartDate(), adminCouponCreateReqDto.getIssueEndDate());

        Coupon coupon = Coupon.builder()
                .name(adminCouponCreateReqDto.getName())
                .discountAmount(adminCouponCreateReqDto.getDiscountAmount())
                .minOrderAmount(adminCouponCreateReqDto.getMinOrderAmount())
                .totalQuantity(adminCouponCreateReqDto.getTotalQuantity())
                .issueStartDate(adminCouponCreateReqDto.getIssueStartDate())
                .issueEndDate(adminCouponCreateReqDto.getIssueEndDate())
                .validDays(adminCouponCreateReqDto.getValidDays())
                .build();

        return  couponRepository.save(coupon).getId();
    }

    @Transactional
    public Long update(Long id, AdminCouponUpdateReqDto adminCouponUpdateReqDto) {
        validateIssuePeriod(adminCouponUpdateReqDto.getIssueStartDate(), adminCouponUpdateReqDto.getIssueEndDate());

        Coupon coupon = couponRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("해당 쿠폰이 존재하지 않습니다."));

        coupon.changeCoupon(
                adminCouponUpdateReqDto.getName(),
                adminCouponUpdateReqDto.getDiscountAmount(),
                adminCouponUpdateReqDto.getMinOrderAmount(),
                adminCouponUpdateReqDto.getTotalQuantity(),
                adminCouponUpdateReqDto.getIssueStartDate(),
                adminCouponUpdateReqDto.getIssueEndDate(),
                adminCouponUpdateReqDto.getValidDays(),
                LocalDateTime.now()
        );

        return coupon.getId();
    }

    @Transactional(readOnly = true)
    public PagingResDto<AdminCouponListResDto> getCouponList(AdminCouponListReqDto adminCouponListReqDto) {
        Page<Coupon> couponList = adminCouponQueryRepository.findCouponList(adminCouponListReqDto);

        return new PagingResDto<>(couponList.map(AdminCouponListResDto::from));
    }

    @Transactional(readOnly = true)
    public AdminCouponDetailResDto getCouponDetail(Long id) {
        Coupon coupon = couponRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("해당 쿠폰이 존재하지 않습니다."));
        return AdminCouponDetailResDto.from(coupon);
    }

    @Transactional
    public Long delete(Long id) {
        Coupon coupon = couponRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("해당 쿠폰이 존재하지 않습니다."));

        coupon.stop();

        return coupon.getId();
    }

    /** 쿠폰 발급기간 검증 : 시작일이 종료일보다 이전 */
    private void validateIssuePeriod(LocalDateTime issueStartDate, LocalDateTime issueEndDate) {
        if(!issueStartDate.isBefore(issueEndDate)){
            throw new IllegalArgumentException("발급 시작일은 발급 종료일보다 이전이어야 합니다.");
        }
    }

}
