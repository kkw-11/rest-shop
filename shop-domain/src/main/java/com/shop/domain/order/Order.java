package com.shop.domain.order;

import com.shop.common.constant.OrderStatus;
import com.shop.domain.common.BaseEntity;
import com.shop.domain.event.Event;
import com.shop.domain.member.Member;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
public class Order extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    private LocalDateTime orderDate; //주문일

    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus; //주문상태

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItem> orderItems = new ArrayList<>();

    public void addOrderItem(OrderItem orderItem) {
        orderItems.add(orderItem);
        orderItem.setOrder(this);//외래키 세팅
    }

    public static Order createOrder(Member member, List<OrderItem> orderItemList) {
        Order order = new Order();

        //연관관계 세팅
        order.member = member;
        for(OrderItem orderItem : orderItemList) {
            order.addOrderItem(orderItem);
        }

        order.orderStatus = OrderStatus.ORDER;
        order.orderDate = LocalDateTime.now();
        return order;
    }

    public static Order createEventOrder(Member member, Event event) {
        Order order = new Order();
        order.member = member;
        order.orderStatus = OrderStatus.ORDER;
        order.orderDate = LocalDateTime.now();

        OrderItem orderItem = OrderItem.createEventOrderItem(event.getItem(), event, event.getMaxPurchasePerUser());
        order.addOrderItem(orderItem);//연관관계 세팅, 외래키

        return order;
    }

    public int getTotalPrice() {
        int totalPrice = 0;
        for(OrderItem orderItem : orderItems) {
            totalPrice += orderItem.getTotalPrice();
        }
        return totalPrice;
    }

    public void cancelOrder(){
        this.orderStatus = OrderStatus.CANCEL;
        orderItems.stream().forEach(orderItem -> orderItem.cancel());
    }
}
