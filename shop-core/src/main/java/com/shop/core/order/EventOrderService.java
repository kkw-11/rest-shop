package com.shop.core.order;

import com.shop.common.exception.CustomException;
import com.shop.common.exception.ErrorCode;
import com.shop.domain.event.Event;
import com.shop.domain.event.EventParticipant;
import com.shop.domain.event.EventParticipantRepository;
import com.shop.domain.event.EventRepository;
import com.shop.domain.member.Member;
import com.shop.domain.member.MemberRepository;
import com.shop.domain.order.Order;
import com.shop.domain.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventOrderService {
    private final EventRepository eventRepository;
    private final MemberRepository memberRepository;
    private final OrderRepository orderRepository;
    private final EventParticipantRepository participantRepository;

    /**
     * 이벤트 주문 생성(비관적 락)
     */
    @Transactional
    public Long createOrderWithPessimisticLock(Long eventId, Long memberId) {
        // 1.조회
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        Event event = eventRepository.findByIdForUpdate(eventId).orElseThrow(() -> new IllegalArgumentException("이벤트를 찾을 수 없습니다."));

        // 2. 이벤트 검증
        event.validateEventPeriodAndStatus();

        // 3. 중복 주문 체크
        validateDuplicateOrder(eventId, memberId);

        // 4. 선착순 인원 체크 (COUNT 쿼리)
        validateMaxParticipants(event);

        // 5. 주문 생성 (Item 재고 차감 포함)
        Order order = Order.createEventOrder(member, event);
        orderRepository.save(order);

        // 6. 참여자 기록
        EventParticipant participant = EventParticipant.create(event, member, order);
        participantRepository.save(participant);

        log.info("이벤트 주문 생성 완료. orderId={}, eventId={}, memberId={}", order.getId(), eventId, member.getId());

        return order.getId();
    }


    /**
     * 이벤트 주문 생성 (Redis 분산락 적용)
     * @param eventId
     * @param memberId
     * @return
     */
    @Transactional
    public Long createOrder(Long eventId, Long memberId) {
        // 1. 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("이벤트를 찾을 수 없습니다."));

        // 2. 중복 주문 체크
        validateDuplicateOrder(eventId, memberId);

        // 3. 이벤트 검증 (도메인)
        event.validateEventPeriodAndStatus();

        // 4. 선착순 인원 체크 (COUNT 쿼리)
        validateMaxParticipants(event);

        // 5. 주문 생성 (Item 재고 차감)
        Order order = Order.createEventOrder(member, event);
        orderRepository.save(order);

        // 6. 참여자 기록
        EventParticipant participant = EventParticipant.create(event, member, order);
        participantRepository.save(participant);

        log.info("이벤트 주문 생성 완료. orderId={}, eventId={}, memberId={}",
                order.getId(), eventId, memberId);

        return order.getId();
    }

    /**
     * 중복주문 검증
     * @param eventId
     * @param memberId
     */
    private void validateDuplicateOrder(Long eventId, Long memberId) {
        if(participantRepository.existsByEventIdAndMemberId(eventId, memberId)){
            throw new IllegalStateException("이미 주문한 이벤트입니다.");
        }
    }


    /**
     * 선착순 인원 체크
     * @param event
     */
    private void validateMaxParticipants(Event event) {
        long participantCount = participantRepository.countByEventId(event.getId());
        if (participantCount >= event.getMaxParticipants()) {
            throw new IllegalStateException(
                    "선착순 마감되었습니다. (마감 인원: " + event.getMaxParticipants() + "명)"
            );
        }
    }
}
