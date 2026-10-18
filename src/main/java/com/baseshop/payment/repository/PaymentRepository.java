package com.baseshop.payment.repository;

import com.baseshop.payment.entity.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.portonePaymentId = :portonePaymentId")
    Optional<Payment> findByPortonePaymentIdForUpdate(String portonePaymentId);

    Optional<Payment> findByPortonePaymentId(String portonePaymentId);

    /** 주문 ID로 결제를 조회하며 락을 건다. (주문/결제 상태 변경은 결제 행 락으로 직렬화) */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.order.id = :orderId")
    Optional<Payment> findByOrderIdForUpdate(Long orderId);
}
