package com.shop.domain.order;

import com.shop.domain.common.BaseEntity;
import com.shop.domain.event.Event;
import com.shop.domain.item.Item;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "order_item")
@Getter @Setter
public class OrderItem extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    private int orderPrice;

    @Column(name = "quantity")
    private Integer quantity;

    public static  OrderItem createOrderItem(Item item, int quantity) {
        OrderItem orderItem = new OrderItem();
        orderItem.setItem(item);
        orderItem.setQuantity(quantity);
        orderItem.setOrderPrice(item.getPrice());

        item.removeStock(quantity);
        return orderItem;
    }

    /**
     * 이벤트 주문 상품 생성
     * @param event
     * @param quantity
     * @return
     */
    public static OrderItem createOrderItem(Item item,Event event, int quantity) {
        OrderItem orderItem = new OrderItem();
        orderItem.setItem(item);
        orderItem.setQuantity(quantity);
        orderItem.setOrderPrice(event.calculateDiscountPrice(item.getPrice()));

        item.removeStock(quantity);

        return orderItem;
    }

    public int getTotalPrice() {
        return orderPrice * quantity;
    }

    public void cancel() {
        this.getItem().addStock(quantity);
    }
}
