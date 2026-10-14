package com.baseshop.product.service;

import com.baseshop.common.dto.PagingResDto;
import com.baseshop.common.file.FileStore;
import com.baseshop.product.dto.*;
import com.baseshop.product.entity.Product;
import com.baseshop.product.repository.ProductQueryRepository;
import com.baseshop.product.repository.ProductRepository;
import com.baseshop.product.type.ProductStatusType;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
