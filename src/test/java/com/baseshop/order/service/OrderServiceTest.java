package com.baseshop.order.service;

import com.baseshop.member.entity.Member;
import com.baseshop.member.repository.MemberRepository;
import com.baseshop.member.type.MemberRoleType;
import com.baseshop.order.dto.OrderCreateReqDto;
import com.baseshop.order.entity.Order;
import com.baseshop.order.facade.OrderFacade;
import com.baseshop.product.entity.Product;
import com.baseshop.product.repository.ProductRepository;
import com.baseshop.product.type.ProductStatusType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
class OrderServiceTest {

    @Autowired
    OrderService orderService;
    @Autowired
    OrderFacade orderFacade;
    @Autowired
    ProductRepository productRepository;
    @Autowired
    MemberRepository memberRepository;

    @Test
    void 동시주문100건_재고100개_재고0() throws InterruptedException {
        //given
        Member member = memberRepository.save(Member.builder()
                .username("product-Quantity-test-" + System.nanoTime())
                .password("test")
                .email("stock" + System.nanoTime() + "@test.com")
                .memberRoleType(MemberRoleType.USER)
                .build());

        Product product = productRepository.save(Product.builder()
                .name("동시성 테스트 상품")
                .description("테스트")
                .price(1000)
                .quantity(100)
                .productStatusType(ProductStatusType.SALE)
                .build());

        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(32);// 32개 스레드가 동시에
        CountDownLatch latch = new CountDownLatch(threadCount); // 100개 다 끈날 떄까지 대기용
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        //when
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    orderFacade.create(member.getId(), createOrderReq(product.getId(), 1));
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executor.shutdown();

        //then
        Product result = productRepository.findById(product.getId()).orElseThrow();
        System.out.println("성공: " + successCount + " 실패: " + failCount + " 남은재고: " + result.getQuantity());

        assertThat(result.getQuantity()).isEqualTo(100 - successCount.get());
        assertThat(result.getQuantity()).isEqualTo(0);
    }

    @Test
    void 동시주문30건_재고10개_10건만성공() throws InterruptedException {
        // given
        Member member = memberRepository.save(Member.builder()
                .username("test" + System.nanoTime())
                .password("1234")
                .email("test199@test.com")
                .memberRoleType(MemberRoleType.USER)
                .build());

        Product product = productRepository.save(Product.builder()
                .name("동시주문30건_재고10개_10건만성공")
                .description("테스트")
                .price(1000)
                .quantity(10)
                .productStatusType(ProductStatusType.SALE)
                .build());

        int threadCount = 30;
        ExecutorService executor = Executors.newFixedThreadPool(30);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        // when - 30건 동시 주문
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    orderFacade.create(member.getId(), createOrderReq(product.getId(), 1));
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executor.shutdown();

        // then - successCount == 10, failCount == 20, 남은재고 == 0 (음수 X)
        Product result = productRepository.findById(product.getId()).orElseThrow();
        System.out.println("성공: "+ successCount.get() + " 실패: " + failCount.get() + " 재고: " + result.getQuantity());

        assertThat(successCount.get()).isEqualTo(10);
        assertThat(failCount.get()).isEqualTo(20);
        assertThat(result.getQuantity()).isEqualTo(0);
    }

    private OrderCreateReqDto createOrderReq(Long productId, int quantity) {
        OrderCreateReqDto.OrderItemCreateReqDto item = new OrderCreateReqDto.OrderItemCreateReqDto();
        item.setProductId(productId);
        item.setQuantity(quantity);

        OrderCreateReqDto req = new OrderCreateReqDto();
        req.setItemList(new ArrayList<>(List.of(item)));
        return req;
    }

}