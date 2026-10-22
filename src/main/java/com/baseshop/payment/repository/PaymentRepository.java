package com.baseshop.payment.repository;

import com.baseshop.payment.entity.Payment;
import com.baseshop.payment.type.PaymentStatusType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.portonePaymentId = :portonePaymentId")
    Optional<Payment> findByPortonePaymentIdForUpdate(String portonePaymentId);

    @Query("select p from Payment p join fetch p.order where p.portonePaymentId = :portonePaymentId")
    Optional<Payment> findWithOrderByPortonePaymentId(String portonePaymentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.order.id = :orderId")
    Optional<Payment> findByOrderIdForUpdate(Long orderId);

    Optional<Payment> findByOrderId(Long orderId);

    /** 만료 대상 결제 ID (상태 + 생성일 기준, 오래된 순) */
    @Query("select p.portonePaymentId from Payment p " +
            "where p.paymentStatusType = :status and p.regDate < :threshold " +
            "order by p.id")
    List<String> findExpiredPortonePaymentIdList(@Param("status") PaymentStatusType status,
                                              @Param("threshold") LocalDateTime threshold,
                                              Pageable pageable);
}
