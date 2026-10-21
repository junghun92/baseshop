package com.baseshop.order.facade;

import com.baseshop.member.entity.Member;
import com.baseshop.member.service.MemberService;
import com.baseshop.order.dto.OrderCreateReqDto;
import com.baseshop.order.dto.OrderCreateResDto;
import com.baseshop.order.dto.OrderSheetReqDto;
import com.baseshop.order.dto.OrderSheetResDto;
import com.baseshop.order.entity.Order;
import com.baseshop.order.service.OrderService;
import com.baseshop.payment.entity.Payment;
import com.baseshop.payment.service.PaymentService;
import com.baseshop.portone.PortOneClient;
import com.baseshop.portone.dto.PortOneCancelReqDto;
import com.baseshop.portone.dto.PortOneCancelResDto;
import com.baseshop.portone.type.CancelReasonType;
import com.baseshop.portone.type.CancelRequesterType;
import com.baseshop.product.entity.Product;
import com.baseshop.product.service.ProductFileService;
import com.baseshop.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Component
public class OrderFacade {

    private final MemberService memberService;
    private final ProductService productService;
    private final ProductFileService productFileService;
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final PortOneClient portOneClient;
    private final OrderProcessor orderProcessor;

    /** 주문생성 */
    @Transactional
    public OrderCreateResDto create(Long memberId, OrderCreateReqDto orderCreateReqDto) {
        List<OrderCreateReqDto.OrderItemCreateReqDto> itemList = orderCreateReqDto.getItemList();

        // 1. 회원조회
        Member member = memberService.getMember(memberId);

        // 2. 상품 락 조회
        List<Long> productIdList = itemList.stream()
                .map(OrderCreateReqDto.OrderItemCreateReqDto::getProductId)
                .toList();
        Map<Long, Product> productMap = productService.getProductMapForUpdate(productIdList);
        
        // 3. 주문 대기상태 저장 (주문상품 저장 + 재고 차감)
        Order order = orderService.create(member, itemList, productMap);
        
        // 4. 결제 준비상태 저장
        Payment payment = paymentService.createReady(order);

        return OrderCreateResDto.builder()
                .orderId(order.getId())
                .portonePaymentId(payment.getPortonePaymentId())
                .totalAmount(payment.getAmount())
                .orderName(order.getOrderName())
                .build();
    }

    /** 주문서 조회 */
    @Transactional(readOnly = true)
    public OrderSheetResDto getOrderSheet(OrderSheetReqDto orderSheetReqDto){
        List<OrderSheetReqDto.OrderSheetItemReqDto> itemList = orderSheetReqDto.getItemList();
        
        // 상품번호 추출
        List<Long> productIdList = itemList.stream()
                .map(OrderSheetReqDto.OrderSheetItemReqDto::getProductId)
                .toList();
        
        // 1. 상품 조회
        Map<Long, Product> productMap = productService.getProductMap(productIdList);
        
        // 2. 상품 이미지 조회
        Map<Long, String> productImageUrlMap = productFileService.getProductImageUrlMap(productIdList);
        
        // 3. 주문서 생성
        return OrderSheetResDto.of(itemList, productMap, productImageUrlMap);
    }

    /** 주문취소 */
    public Long cancel(Long memberId, Long orderId) {
        // 1. 취소 가능 확인
        String portonePaymentId = paymentService.getCancelablePaymentId(memberId, orderId);

        // 2. 포트원 취소 요청
        PortOneCancelReqDto portOneCancelReqDto = new PortOneCancelReqDto(
                CancelReasonType.CUSTOMER_REQUEST.getDescription(), CancelRequesterType.CUSTOMER);
        PortOneCancelResDto.Cancellation cancellation = portOneClient.cancel(portonePaymentId, portOneCancelReqDto);

        // 3. DB 반영
        try {
            orderProcessor.updateCancel(portonePaymentId, CancelRequesterType.CUSTOMER, cancellation);
        } catch (Exception e) {
            // 포트원 취소는 완료
            // DB 반영만 실패 -> 웹훅에서 처리 됨
            log.error("취소 완료 후 DB 반영 실패 orderId={}, portonePaymentId={}", orderId, portonePaymentId, e);
            throw new IllegalArgumentException("취소 완료되었습니다. 주문 상태는 잠시 후 반영됩니다.");
        }

        return orderId;
    }
}
