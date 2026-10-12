package com.baseshop.order.service;

import com.baseshop.board.dto.BoardListResDto;
import com.baseshop.cart.repository.CartRepository;
import com.baseshop.common.dto.PagingResDto;
import com.baseshop.member.entity.Member;
import com.baseshop.member.repository.MemberRepository;
import com.baseshop.order.dto.OrderCreateReqDto;
import com.baseshop.order.dto.OrderItemCreateReqDto;
import com.baseshop.order.dto.OrderListReqDto;
import com.baseshop.order.dto.OrderListResDto;
import com.baseshop.order.entity.Order;
import com.baseshop.order.entity.OrderItem;
import com.baseshop.order.repository.OrderItemRepository;
import com.baseshop.order.repository.OrderQueryRepository;
import com.baseshop.order.repository.OrderRepository;
import com.baseshop.order.type.OrderStatusType;
import com.baseshop.product.entity.Product;
import com.baseshop.product.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final MemberRepository memberRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;
    private final OrderQueryRepository orderQueryRepository;

    @Transactional
    public Long create(Long memberId, OrderCreateReqDto orderCreateReqDto) {
        List<OrderItemCreateReqDto> items = orderCreateReqDto.getItems();

        // 1. 요청 검증
        if(items == null || items.isEmpty()){
            throw new IllegalArgumentException("주문할 상품이 없습니다.");
        }

        // 2. 회원조회
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 회원입니다."));

        // 3. 주문 저장
        long totalPrice = 0L; // 총 주문금액 (주문상품 저장 후, update 처리)
        Order orderBuilder = Order.builder()
                .member(member)
                .totalPrice(0L)
                .orderStatusType(OrderStatusType.ORDER)
                .build();

        Order savedOrder = orderRepository.save(orderBuilder);

        // 4. 주문상품 저장 (재고 차감 포함)
        List<OrderItem> orderItemList = new ArrayList<>();
        List<Long> cartIdList = new ArrayList<>();

        // 상품ID 순서로 정렬 (재고 동시성 이슈, 데드락 방지)
        items.sort(Comparator.comparing(OrderItemCreateReqDto::getProductId));

        for (OrderItemCreateReqDto item : items) {
            Product product = productRepository.findByIdForUpdate(item.getProductId()).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 상품입니다."));

            // 주문상품 생성, 상품 재고차감 (변경 감지로 UPDATE)
            OrderItem orderItem = OrderItem.createOrderItem(savedOrder, product, item.getQuantity());
            orderItemList.add(orderItem);

            totalPrice += orderItem.getTotalPrice(); // 총 주문금액

            // 장바구니에 존재하는 상품체크
            if (item.getCartId() != null) {
                cartIdList.add(item.getCartId());
            }
        }

        orderItemRepository.saveAll(orderItemList);

        // 5. 총 주문금액 (변경 감지로 UPDATE)
        savedOrder.changeTotalPrice(totalPrice);

        // 6. 장바구니 삭제
        if(!cartIdList.isEmpty()){
            cartRepository.deleteByIdListAndMemberId(cartIdList, memberId);
        }

        return savedOrder.getId();
    }

    @Transactional(readOnly = true)
    public PagingResDto<OrderListResDto> getOrderList(Long memberId, OrderListReqDto orderListReqDto) {
        // 주문 리스트
        Page<Order> orderList = orderQueryRepository.findOrderList(memberId, orderListReqDto);

        return new PagingResDto<>(orderList.map(OrderListResDto::new));
    }

    @Transactional
    public Long cancel(Long memberId, Long id) {
        // 주문 row 락 (중복 취소 방지)
        Order order = orderRepository.findByIdForUpdate(id).orElseThrow(() -> new EntityNotFoundException("해당 주문내역이 없습니다."));

        if(!order.getMember().getId().equals(memberId)){
            throw new AccessDeniedException("본인의 주문만 취소할 수 있습니다.");
        }

        // 상태변경 - CANCEL
        order.cancel();

        // 재고 복구 : 주문할 때랑 똑같이 상품ID 순서로 락 걸고 처리
        List<OrderItem> orderItemList = order.getOrderItemList();
        orderItemList.sort(Comparator.comparing(orderItem -> orderItem.getProduct().getId()));

        for (OrderItem orderItem : orderItemList) {
            Product product = productRepository.findByIdForUpdate(orderItem.getProduct().getId())
                    .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 상품입니다."));

            product.addQuantity(orderItem.getQuantity());
        }

        return order.getId();
    }
}
