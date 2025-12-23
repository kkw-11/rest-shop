package com.shop.api.product;

import com.shop.api.product.dto.ProductResponse;
import com.shop.common.dto.ApiResponse;
import com.shop.core.product.ProductService;
import com.shop.domain.item.Item;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "상품 API", description = "상품 조회 API")
public class ProductController {

    private final ProductService productService;

    /**
     * 상품 목록 조회 (페이징)
     */
   @Operation(summary = "상품 목록 조회", description = "상품 목록을 페이징하여 조회합니다.")
   @GetMapping
   public ResponseEntity<ApiResponse<Page<ProductResponse>>> getProducts(@PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
       Page<Item> items = productService.getProducts(pageable);
       Page<ProductResponse> response = items.map(ProductResponse::from);

       log.info("상품 목록 조회 API 호출 완료, page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
       return ResponseEntity.ok(ApiResponse.success(response));
   }

    /**
     * 상품 상세 조회
     */
    @Operation(summary = "상품 상세 조회", description = "상품 ID로 상품 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(@PathVariable Long id) {
        Item item = productService.getProduct(id);
        ProductResponse response = ProductResponse.from(item);

        log.info("상품 상세 조회 API 호출 완료: itemId={}", id);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
