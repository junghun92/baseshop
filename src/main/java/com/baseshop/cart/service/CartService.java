package com.baseshop.cart.service;

import com.baseshop.board.dto.BoardListResDto;
import com.baseshop.cart.dto.CartCreateReqDto;
import com.baseshop.cart.dto.CartListReqDto;
import com.baseshop.cart.dto.CartListResDto;
import com.baseshop.cart.dto.CartUpdateReqDto;
import com.baseshop.cart.entity.Cart;
import com.baseshop.cart.repository.CartQueryRepository;
import com.baseshop.cart.repository.CartRepository;
import com.baseshop.common.dto.PagingResDto;
import com.baseshop.member.entity.Member;
import com.baseshop.member.repository.MemberRepository;
import com.baseshop.product.entity.Product;
import com.baseshop.product.repository.ProductRepository;
import com.baseshop.product.type.ProductStatusType;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartQueryRepository cartQueryRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;

    @Transactional
    public Long create(Long memberId, CartCreateReqDto cartCreateReqDto) {
        Product product = productRepository.findById(cartCreateReqDto.getProductId()).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 상품입니다."));

        product.validateOnSale();

        // 이미 담긴 상품이면 수량만 증가
        Optional<Cart> savedCart = cartRepository.findByMemberIdAndProductId(memberId, product.getId());
        if(savedCart.isPresent()){
            Cart cart = savedCart.get();
            cart.addQuantity(cartCreateReqDto.getQuantity());
            return cart.getId();
        }

        // 처음 담긴 상품이면 신규등록
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 회원입니다."));

        Cart cart = Cart.builder()
                .member(member)
                .product(product)
                .quantity(cartCreateReqDto.getQuantity())
                .build();

        return cartRepository.save(cart).getId();
    }

    @Transactional
    public Long update(Long id, CartUpdateReqDto cartUpdateReqDto, Long memberId) {
        Cart cart = cartRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("장바구니를 찾을 수 없습니다."));

        if(!cart.getMember().getId().equals(memberId)){
            throw new AccessDeniedException("본인이 등록한 장바구니만 수정할 수 있습니다.");
        }

        cart.changeQuantity(cartUpdateReqDto.getQuantity());

        return cart.getId();
    }

    @Transactional
    public Long delete(Long id, Long memberId) {
        Cart cart = cartRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("장바구니를 찾을 수 없습니다."));

        if(!cart.getMember().getId().equals(memberId)){
            throw new AccessDeniedException("본인이 등록한 장바구니만 삭제할 수 있습니다.");
        }

        cartRepository.delete(cart);
        return id;
    }

    @Transactional(readOnly = true)
    public PagingResDto<CartListResDto> getCartList(Long memberId, CartListReqDto cartListReqDto) {
        Page<Cart> cartList = cartQueryRepository.findCartList(memberId, cartListReqDto);

        return new PagingResDto<>(cartList.map(CartListResDto::new));
    }
}
