package com.baseshop.product.service;

import com.baseshop.common.dto.PagingResDto;
import com.baseshop.common.file.FileStore;
import com.baseshop.order.entity.OrderItem;
import com.baseshop.product.dto.*;
import com.baseshop.product.entity.Product;
import com.baseshop.product.repository.ProductQueryRepository;
import com.baseshop.product.repository.ProductRepository;
import com.baseshop.product.type.ProductStatusType;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductQueryRepository productQueryRepository;
    private final ProductFileService productFileService;
    private final FileStore fileStore;

    @Transactional
    public Long create(ProductCreateReqDto productCreateReqDto) {
        Product product = Product.builder()
                .name(productCreateReqDto.getName())
                .description(productCreateReqDto.getDescription())
                .price(productCreateReqDto.getPrice())
                .quantity(productCreateReqDto.getQuantity())
                .productStatusType(productCreateReqDto.getProductStatusType())
                .build();

        return productRepository.save(product).getId();
    }

    @Transactional
    public Long update(Long id, ProductUpdateReqDto productUpdateReqDto) {
        Product product = productRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("등록 된 상품이 없습니다."));
        product.changeProduct(productUpdateReqDto);

        return product.getId();
    }

    @Transactional
    public Long delete(Long id) {
        Product product = productRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("등록 된 상품이 없습니다."));
        
        // DB는 삭제 X, 상태값만 판매중지로 변경
        product.changeStatus(ProductStatusType.STOP);

        return product.getId();
    }

    @Transactional(readOnly = true)
    public ProductDetailResDto getProductDetail(Long id) {
        Product product = productRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("등록 된 상품이 없습니다."));
        String imageUrl = productFileService.getProductImageUrl(id);

        return ProductDetailResDto.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .quantity(product.getQuantity())
                .productStatusType(product.getProductStatusType())
                .imageUrl(imageUrl)
                .build();
    }

    @Transactional(readOnly = true)
    public PagingResDto<ProductListResDto> getProductList(ProductListReqDto productListReqDto) {
        Page<ProductListQueryDto> productList = productQueryRepository.findProductList(productListReqDto);

        return new PagingResDto<>(productList.map(dto -> {
            String storeFilePath = dto.getStoreFilePath();
            String imageUrl = storeFilePath != null ? fileStore.getUrl(storeFilePath) : null;
            return new ProductListResDto(dto.getProduct(), imageUrl);
        }));
    }

    @Transactional
    public Long updateQuantity(Long id, QuantityUpdateReqDto quantityUpdateReqDto) {
        Product product = productRepository.findByIdForUpdate(id).orElseThrow(() -> new EntityNotFoundException("등록 된 상품이 없습니다."));

        product.changeQuantity(quantityUpdateReqDto.getChangeQuantity());

        return product.getId();
    }

    /**
     * 주문 생성 시 차감한 재고를 복구한다.
     * - 호출하는 쪽의 트랜잭션 안에서 실행된다. (상품 락은 커밋 때 해제)
     * - 주문 생성과 같은 순서(상품ID 오름차순)로 락을 잡아 데드락을 피한다.
     */
    public void updateStockRestore(List<OrderItem> orderItemList) {
        List<OrderItem> sortedOrderItemList = new ArrayList<>(orderItemList);
        sortedOrderItemList.sort(Comparator.comparing(orderItem -> orderItem.getProduct().getId()));

        for (OrderItem orderItem : sortedOrderItemList) {
            Product product = productRepository.findByIdForUpdate(orderItem.getProduct().getId())
                    .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 상품입니다."));
            product.addQuantity(orderItem.getQuantity());
        }
    }

    /** 상품 비관적락 일괄 조회 (재고 변경용) */
    @Transactional(propagation = Propagation.MANDATORY)
    public Map<Long, Product> getProductMapForUpdate(List<Long> productIdList){
        return productRepository.findAllByIdInForUpdate(productIdList).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
    }

    /** 상품 일괄 조회 */
    @Transactional(readOnly = true)
    public Map<Long, Product> getProductMap(List<Long> productIdList) {
        return productRepository.findAllById(productIdList).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
    }
}
