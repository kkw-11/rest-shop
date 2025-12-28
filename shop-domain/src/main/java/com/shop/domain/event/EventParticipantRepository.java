package com.shop.domain.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventParticipantRepository extends JpaRepository<EventParticipant, Long> {

    /**
     * 특정 회원이 해당 이벤트에 이미 참여했는지 확인
     */
    @Query("SELECT COUNT(ep) > 0 FROM EventParticipant ep " +
            "WHERE ep.event.id = :eventId AND ep.member.id = :memberId")
    boolean existsByEventIdAndMemberId(
            @Param("eventId") Long eventId,
            @Param("memberId") Long memberId
    );

    // COUNT 쿼리
    long countByEventId(Long eventId);
}
