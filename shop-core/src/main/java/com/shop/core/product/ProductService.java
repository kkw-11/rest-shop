package com.shop.core.product;

import com.shop.common.exception.CustomException;
import com.shop.common.exception.ErrorCode;
import com.shop.domain.item.Item;
import com.shop.domain.item.ItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {
    private final ItemRepository itemRepository;

    /**
     * 상품 목록 조회(페이징)
     * @param pageable
     * @return
     */
    public Page<Item> getProducts(Pageable pageable) {
        return itemRepository.findAll(pageable);
    }

    /**
     * 상품 상세조회
     * @param id
     * @return
     */
    public Item getProduct(Long id) {
        return itemRepository.findById(id).orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
    }
}