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
import com.baseshop.product.entity.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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

}
