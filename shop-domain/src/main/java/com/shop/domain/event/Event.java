package com.shop.domain.event;

import com.shop.common.constant.EventStatus;
import com.shop.common.exception.CustomException;
import com.shop.common.exception.ErrorCode;
import com.shop.domain.common.BaseEntity;
import com.shop.domain.item.Item;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

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

    @OneToMany(mappedBy = "event")
    private List<EventParticipant> participants;

    /**
     * 할인율 (0-100)
     */
    @Column(nullable = false)
    private Integer discountRate;

    /**
     * 이벤트 전용 재고
     * - 선착순 제어용
     * - Item 재고와 독립적
     */
    @Column(nullable = false)
    private Integer remainingStock;

    @Column(nullable = false)
    private Integer maxParticipants;

    @Column(nullable = false)
    private Integer maxPurchasePerUser; //1인당 최대 구매 수량

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
            Integer remainingStock,
            Integer maxPurchasePerUser
    ) {
        Event event = new Event();
        event.eventName = eventName;
        event.item = item;
        event.startDate = startDate;
        event.endDate = endDate;
        event.discountRate = discountRate;
        event.remainingStock = remainingStock;
        event.maxParticipants = remainingStock;  // 초기값은 재고와 동일
        event.maxPurchasePerUser = maxPurchasePerUser;
        event.status = EventStatus.SCHEDULED;
        return event;
    }

    // ========== 비즈니스 로직 ==========

    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return status == EventStatus.ACTIVE
                && now.isAfter(startDate)
                && now.isBefore(endDate);
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

    public int calculateDiscountPrice(int originalPrice) {
        return originalPrice - calculateDiscountAmount(originalPrice);
    }

    /**
     * 이벤트 주문 처리
     * - Event 재고 차감
     * - Item 재고는 차감 안 함
     */
    public void processOrder(int quantity) {
        validateEventPeriodAndStatus();
        decreaseStock(quantity);
    }

    /**
     * 이벤트 기간 및 상태 검증
     */
    public void validateEventPeriodAndStatus() {
        // 1. 상태 체크
        if (this.status != EventStatus.ACTIVE) {
            throw new IllegalStateException("진행 중인 이벤트가 아닙니다.");
        }

        // 2. 기간 체크
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(this.startDate)) {
            throw new IllegalStateException("이벤트가 아직 시작되지 않았습니다.");
        }

        if (now.isAfter(this.endDate)) {
            throw new IllegalStateException("이벤트가 종료되었습니다.");
        }
    }

    /**
     * 재고 차감
     */
    private void decreaseStock(int quantity) {
        if (this.remainingStock < quantity) {
            throw new CustomException(ErrorCode.OUT_OF_STOCK);
        }

        this.remainingStock -= quantity;

        if (this.remainingStock == 0) {
            this.status = EventStatus.SOLD_OUT;
        }
    }
}
