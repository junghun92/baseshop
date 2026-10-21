package com.baseshop.order.service;

import com.baseshop.common.dto.PagingResDto;
import com.baseshop.member.entity.Member;
import com.baseshop.order.dto.OrderCreateReqDto;
import com.baseshop.order.dto.OrderListReqDto;
import com.baseshop.order.dto.OrderListResDto;
import com.baseshop.order.entity.Order;
import com.baseshop.order.entity.OrderItem;
import com.baseshop.order.repository.OrderItemRepository;
import com.baseshop.order.repository.OrderQueryRepository;
import com.baseshop.order.repository.OrderRepository;
import com.baseshop.payment.entity.Payment;
import com.baseshop.payment.repository.PaymentRepository;
import com.baseshop.payment.service.PaymentService;
import com.baseshop.payment.type.PaymentStatusType;
import com.baseshop.portone.type.CancelReasonType;
import com.baseshop.portone.type.CancelRequesterType;
import com.baseshop.product.entity.Product;
import com.baseshop.product.service.ProductService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderQueryRepository orderQueryRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final ProductService productService;

    /** 주문생성 */
    @Transactional
    public Order create(Member member,
                        List<OrderCreateReqDto.OrderItemCreateReqDto> itemList,
                        Map<Long, Product> productMap) {
        // 주문 생성 (주문대기)
        Order order = Order.createPending(member);

        // 주문상품 생성, 상품 재고차감
        List<OrderItem> orderItemList = new ArrayList<>();
        for (OrderCreateReqDto.OrderItemCreateReqDto item : itemList) {
            Product product = productMap.get(item.getProductId());
            OrderItem orderItem = order.addOrderItem(product, item.getQuantity());
            orderItemList.add(orderItem);
        }

        orderRepository.save(order);
        orderItemRepository.saveAll(orderItemList);

        return order;
    }

    /** 주문 리스트 */
    @Transactional(readOnly = true)
    public PagingResDto<OrderListResDto> getOrderList(Long memberId, OrderListReqDto orderListReqDto) {
        Page<Order> orderList = orderQueryRepository.findOrderList(memberId, orderListReqDto);
        return new PagingResDto<>(orderList.map(OrderListResDto::new));
    }

    /**
     * 주문취소
     * - 결제 완료와 웹훅과 같은 결제 행 락을 사용해 동시 처리를 막는다.
     */
    @Transactional
    public Long cancel(Long memberId, Long id) {
        Payment payment = paymentRepository.findByOrderIdForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException("해당 주문내역이 없습니다."));
        Order order = payment.getOrder();

        if(!order.getMember().getId().equals(memberId)){
            throw new AccessDeniedException("본인의 주문만 취소할 수 있습니다.");
        }

        // 결제 완료된 주문만 취소가능
        if (payment.getPaymentStatusType() != PaymentStatusType.PAID) {
            throw new IllegalArgumentException("취소할 수 없는 주문입니다.");
        }

        paymentService.cancel(payment, CancelReasonType.CUSTOMER_REQUEST, CancelRequesterType.CUSTOMER); // 결제취소
        order.cancel(); // 주문취소
        productService.updateStockRestore(order.getOrderItemList()); // 재고복구

        return order.getId();
    }

}
