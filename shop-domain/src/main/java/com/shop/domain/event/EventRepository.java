package com.shop.domain.event;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    /**
     * 활성화된 이벤트 조회 (상품 ID로)
     */
    @Query("SELECT e FROM Event e WHERE e.item.id = :itemId AND e.status = 'ACTIVE'")
    Optional<Event> findActiveEventByItemId(@Param("itemId") Long itemId);

    /**
     * ⚠️ Phase 1-2에서 비관적 락 적용할 메서드
     * 일단은 기본 조회만
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Event e WHERE e.id = :eventId")
    Optional<Event> findByIdForUpdate(@Param("eventId") Long eventId);

    @Query("SELECT e FROM Event e JOIN FETCH e.item WHERE e.id = :id")
    Optional<Event> findByIdWithItem(@Param("id") Long id);
}