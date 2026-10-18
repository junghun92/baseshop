package com.baseshop.cart.repository;

import com.baseshop.cart.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByMemberIdAndProductId (Long memberId, Long ProductId);

    @Modifying
    @Query("delete from Cart c where c.member.id = :memberId and c.product.id in :productIdList")
    void deleteByMemberIdAndProductIdList(@Param("memberId") Long memberId, @Param("productIdList") List<Long> productIdList);
}
