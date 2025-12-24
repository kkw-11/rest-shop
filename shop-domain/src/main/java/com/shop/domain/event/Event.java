package com.shop.domain.event;

import com.shop.common.constant.EventStatus;
import com.shop.domain.common.BaseEntity;
import com.shop.domain.item.Item;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Event extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Long id;

    @Column(nullable = false, length = 100)
    private String eventName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime endDate;

    /**
     * 할인율 (0-100)
     */
    @Column(nullable = false)
    private Integer discountRate;

    /**
     * 이벤트 전용 재고(선착순 수)
     */
    @Column(nullable = false)
    private Integer eventStock;

    /**
     * 남은 이벤트 재고
     */
    @Column(nullable = false)
    private Integer remainingStock;

    /**
     * 1인당 최대 구매 수량
     */
    @Column(nullable = false)
    private Integer maxPurchasePerUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status;

    // ========== 정적 팩토리 메서드 ==========

    public static Event createEvent(
            String eventName,
            Item item,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Integer discountRate,
            Integer eventStock,
            Integer maxPurchasePerUser
    ) {
        Event event = new Event();
        event.eventName = eventName;
        event.item = item;
        event.startDate = startDate;
        event.endDate = endDate;
        event.discountRate = discountRate;
        event.eventStock = eventStock;
        event.remainingStock = eventStock;
        event.maxPurchasePerUser = maxPurchasePerUser;
        event.status = EventStatus.SCHEDULED;
        return event;
    }

    // ========== 비즈니스 로직 ==========

    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return status == EventStatus.ACTIVE
                && now.isAfter(startDate)
                && now.isBefore(endDate)
                && remainingStock > 0;
    }


    public void decreaseStock(int quantity) {
        if (remainingStock < quantity) {
            throw new IllegalStateException(
                    String.format("이벤트 재고 부족. 요청: %d, 남은 재고: %d",
                            quantity, remainingStock)
            );
        }
        remainingStock -= quantity;

        if (remainingStock == 0) {
            this.status = EventStatus.SOLD_OUT;
        }
    }

    public void activate() {
        if (status != EventStatus.SCHEDULED) {
            throw new IllegalStateException("이미 시작되었거나 종료된 이벤트입니다.");
        }
        this.status = EventStatus.ACTIVE;
    }

    public void end() {
        this.status = EventStatus.ENDED;
    }

    public int calculateDiscountAmount(int originalPrice) {
        return (int) (originalPrice * discountRate / 100.0);
    }

    public int calculateFinalPrice(int originalPrice) {
        return originalPrice - calculateDiscountAmount(originalPrice);
    }
}
