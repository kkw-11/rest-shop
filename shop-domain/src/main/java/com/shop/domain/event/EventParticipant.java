package com.shop.domain.event;

import com.shop.domain.common.BaseTimeEntity;
import com.shop.domain.member.Member;
import com.shop.domain.order.Order;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 이벤트 참여 이력 (1인 1개 제한 검증용)
 */
@Entity
@Table(
        name = "event_participant",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_event_member",
                        columnNames = {"event_id", "member_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventParticipant extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "participant_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    public static EventParticipant create(Event event, Member member, Order order) {
        EventParticipant participant = new EventParticipant();
        participant.event = event;
        participant.member = member;
        participant.order = order;
        return participant;
    }
}