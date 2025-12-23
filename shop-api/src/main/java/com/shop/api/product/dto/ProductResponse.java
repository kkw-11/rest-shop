package com.shop.api.product.dto;


import com.shop.common.constant.ItemSellStatus;
import com.shop.domain.item.Item;
import com.shop.domain.item.ItemImg;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 상품 정보 응답
 */
@Getter
@Builder
@AllArgsConstructor
public class ProductResponse {
    private Long id;
    private String itemNm;
    private Integer price;
    private Integer stockNumber;
    private String itemDetail;
    private ItemSellStatus itemSellStatus;
    private List<ProductImageResponse> images;

    public static ProductResponse from(Item item) {
        return ProductResponse.builder()
                .id(item.getId())
                .itemNm(item.getItemNm())
                .price(item.getPrice())
                .stockNumber(item.getStockNumber())
                .itemDetail(item.getItemDetail())
                .itemSellStatus(item.getItemSellStatus())
                .images(item.getItemImgs().stream().map(ProductImageResponse::from).collect(Collectors.toList()))
                .build();
    }


    @Getter
    @Builder
    @AllArgsConstructor
    private static class ProductImageResponse {
        private Long id;
        private String imgNm;
        private String imgUrl;
        private String repImgYn;

        public static ProductImageResponse from(ItemImg itemImg) {
            return ProductImageResponse.builder()
                    .id(itemImg.getId())
                    .imgNm(itemImg.getImgName())
                    .imgUrl(itemImg.getImgUrl())
                    .repImgYn(itemImg.getRepImgYn())
                    .build();
        }
    }
}