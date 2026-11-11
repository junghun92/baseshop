package com.baseshop.admin.product.service;

import com.baseshop.admin.product.dto.*;
import com.baseshop.admin.product.repository.AdminProductQueryRepository;
import com.baseshop.common.dto.PagingResDto;
import com.baseshop.common.file.FileStore;
import com.baseshop.product.dto.*;
import com.baseshop.product.entity.Product;
import com.baseshop.product.repository.ProductRepository;
import com.baseshop.product.service.ProductFileService;
import com.baseshop.product.type.ProductStatusType;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminProductService {

    private final ProductRepository productRepository;
    private final AdminProductQueryRepository adminProductQueryRepository;
    private final ProductFileService productFileService;
    private final FileStore fileStore;

    @Transactional
    public Long create(AdminProductCreateReqDto adminProductCreateReqDto) {
        Product product = Product.builder()
                .name(adminProductCreateReqDto.getName())
                .description(adminProductCreateReqDto.getDescription())
                .price(adminProductCreateReqDto.getPrice())
                .quantity(adminProductCreateReqDto.getQuantity())
                .productStatusType(adminProductCreateReqDto.getProductStatusType())
                .build();

        return productRepository.save(product).getId();
    }

    @Transactional
    public Long update(Long id, AdminProductUpdateReqDto adminProductUpdateReqDto) {
        Product product = productRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("등록 된 상품이 없습니다."));
        product.changeProduct(
                adminProductUpdateReqDto.getName(),
                adminProductUpdateReqDto.getDescription(),
                adminProductUpdateReqDto.getPrice(),
                adminProductUpdateReqDto.getProductStatusType());

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
    public AdminProductDetailResDto getProductDetail(Long id) {
        Product product = productRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("등록 된 상품이 없습니다."));
        String imageUrl = productFileService.getProductImageUrl(id);

        return AdminProductDetailResDto.builder()
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
    public PagingResDto<AdminProductListResDto> getProductList(AdminProductListReqDto adminProductListReqDto) {
        Page<ProductListQueryDto> productList = adminProductQueryRepository.findProductList(adminProductListReqDto);

        return new PagingResDto<>(productList.map(dto -> {
            String storeFilePath = dto.getStoreFilePath();
            String imageUrl = storeFilePath != null ? fileStore.getUrl(storeFilePath) : null;
            return new AdminProductListResDto(dto.getProduct(), imageUrl);
        }));
    }

    @Transactional
    public Long updateQuantity(Long id, QuantityUpdateReqDto quantityUpdateReqDto) {
        Product product = productRepository.findByIdForUpdate(id).orElseThrow(() -> new EntityNotFoundException("등록 된 상품이 없습니다."));

        product.changeQuantity(quantityUpdateReqDto.getChangeQuantity());

        return product.getId();
    }
}
