package com.baseshop.order.service;

import com.baseshop.common.dto.PagingResDto;
import com.baseshop.common.file.FileStore;
import com.baseshop.member.entity.Member;
import com.baseshop.member.repository.MemberRepository;
import com.baseshop.order.dto.*;
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
import com.baseshop.product.entity.ProductFile;
import com.baseshop.product.repository.ProductFileRepository;
import com.baseshop.product.repository.ProductRepository;
import com.baseshop.product.service.ProductService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final MemberRepository memberRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final ProductFileRepository productFileRepository;
    private final OrderQueryRepository orderQueryRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final ProductService productService;
    private final FileStore fileStore;

    @Transactional
    public OrderCreateResDto create(Long memberId, OrderCreateReqDto orderCreateReqDto) {
        List<OrderCreateReqDto.OrderItemCreateReqDto> items = orderCreateReqDto.getItems();

        // 상품ID 순서로 정렬 (데드락 방지)
        items.sort(Comparator.comparing(OrderCreateReqDto.OrderItemCreateReqDto::getProductId));

        // 회원조회
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 회원입니다."));

        // 주문 생성 (주문대기)
        Order order = Order.createOrder(member);

        List<OrderItem> orderItemList = new ArrayList<>();

        for (OrderCreateReqDto.OrderItemCreateReqDto item : items) {
            Product product = productRepository.findByIdForUpdate(item.getProductId()).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 상품입니다."));

            // 주문상품 생성, 상품 재고차감 (변경 감지로 UPDATE)
            OrderItem orderItem = order.addOrderItem(product, item.getQuantity());
            orderItemList.add(orderItem);
        }

        orderRepository.save(order); // 주문저장
        orderItemRepository.saveAll(orderItemList); // 주문상품 저장

        // 결제 준비상태 저장
        Payment payment = paymentRepository.save(Payment.createReady(order));

        return OrderCreateResDto.builder()
                .orderId(order.getId())
                .portonePaymentId(payment.getPortonePaymentId())
                .totalAmount(payment.getAmount())
                .orderName(order.getOrderName())
                .build();
    }

    @Transactional(readOnly = true)
    public PagingResDto<OrderListResDto> getOrderList(Long memberId, OrderListReqDto orderListReqDto) {
        // 주문 리스트
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

    @Transactional(readOnly = true)
    public OrderSheetResDto getOrderSheet(OrderSheetReqDto orderSheetReqDto) {
        List<OrderSheetReqDto.OrderSheetItemReqDto> items = orderSheetReqDto.getItems();

        // 상품번호 추출
        List<Long> productIdList = items.stream()
                .map(OrderSheetReqDto.OrderSheetItemReqDto::getProductId)
                .toList();
        
        // 전체 상품조회 -> Map<상품ID, 상품>
        List<Product> productList = productRepository.findAllById(productIdList);
        Map<Long, Product> productMap = productList.stream()
                .collect(Collectors.toMap(Product::getId, product -> product));
        
        // 전체 상품 이미지 조회 -> Map<상품ID, 상품파일>
        List<ProductFile> productFileList = productFileRepository.findByProductIdInAndDelYn(productIdList, "N");
        Map<Long, ProductFile> productFileMap = productFileList.stream()
                .collect(Collectors.toMap(p -> p.getProduct().getId(), productFile -> productFile));

        // 주문서 정보 조회
        OrderSheetResDto orderSheetResDto = new OrderSheetResDto();

        for (OrderSheetReqDto.OrderSheetItemReqDto item : items) {
            Product product = productMap.get(item.getProductId());
            if(product == null){
                throw new EntityNotFoundException("상품 정보가 없습니다.");
            }

            ProductFile productFile = productFileMap.get(item.getProductId());
            String imageUrl = productFile != null ? fileStore.getUrl(productFile.getUploadFile().getStoreFilePath()) : null;

            orderSheetResDto.addItem(OrderSheetResDto.OrderSheetItemResDto.builder()
                    .productId(product.getId())
                    .name(product.getName())
                    .imageUrl(imageUrl)
                    .price(product.getPrice())
                    .quantity(item.getQuantity())
                    .build());
        }

        return orderSheetResDto;
    }
}
