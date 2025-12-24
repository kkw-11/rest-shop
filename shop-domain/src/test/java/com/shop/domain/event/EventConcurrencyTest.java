package com.shop.domain.event;

import com.shop.common.constant.EventStatus;
import com.shop.common.constant.ItemSellStatus;
import com.shop.domain.item.Item;
import com.shop.domain.item.ItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class EventConcurrencyTest {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ItemRepository itemRepository;

    private Event testEvent;
    private Item testItem;

    @BeforeEach
    void setUp() {
        // 테스트용 상품 생성
        testItem = new Item();
        testItem.setItemNm("테스트 상품");
        testItem.setPrice(10000);
        testItem.setStockNumber(1000);
        testItem.setItemDetail("테스트 상품입니다.");
        testItem.setItemSellStatus(ItemSellStatus.SELL);
        itemRepository.save(testItem);

        // 테스트용 이벤트 생성 (선착순 100명, 50% 할인)
        testEvent = Event.createEvent(
                "신년 특가 이벤트",
                testItem,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(7),
                50, // 50% 할인
                100, // 선착순 100개
                1  // 1인 1개
        );
        testEvent.activate(); // 이벤트 활성화
        eventRepository.save(testEvent);
    }

    @Test
    @DisplayName("동시성 문제 재현: 100명이 동시에 주문하면 재고가 정확하게 차감되지 않는다")
    void concurrency_problem_reproduction() throws InterruptedException {
        // given
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when: 100명이 동시에 1개씩 주문 시도
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    Event event = eventRepository.findById(testEvent.getId()).orElseThrow();

                    // 재고 확인 후 차감 (동시성 제어 없음)
                    if (event.getRemainingStock() > 0) {
                        Thread.sleep(10); // ⚠️ 의도적인 지연으로 동시성 문제 악화
                        event.decreaseStock(1);
                        eventRepository.save(event);
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then: 문제 확인
        Event result = eventRepository.findById(testEvent.getId()).orElseThrow();

        System.out.println("===== 동시성 문제 재현 결과 =====");
        System.out.println("성공 요청: " + successCount.get());
        System.out.println("실패 요청: " + failCount.get());
        System.out.println("예상 재고: 0");
        System.out.println("실제 재고: " + result.getRemainingStock());
        System.out.println("===============================");

        // ⚠️ 이 테스트는 실패할 것임 (동시성 문제)
        // 기대값: remainingStock = 0
        // 실제값: remainingStock > 0 (예: 30~50 남음)
        assertThat(result.getRemainingStock())
                .as("동시성 제어가 없으면 재고가 정확하게 차감되지 않는다")
                .isNotEqualTo(0); // 일부러 실패하는 테스트
    }

    @Test
    @DisplayName("비관적 락 적용: 100명이 동시에 주문해도 재고가 정확하게 차감된다")
    void pessimistic_lock_solution() throws InterruptedException {
        // given
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when: 100명이 동시에 1개씩 주문 시도 (비관적 락 사용)
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    // ⚠️ 이번엔 findByIdForUpdate() 사용 (비관적 락)
                    Event event = eventRepository.findByIdForUpdate(testEvent.getId()).orElseThrow();

                    if (event.getRemainingStock() > 0) {
                        Thread.sleep(10); // 동일한 지연 조건
                        event.decreaseStock(1);
                        eventRepository.save(event);
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then: 정확한 재고 확인
        Event result = eventRepository.findById(testEvent.getId()).orElseThrow();

        System.out.println("===== 비관적 락 적용 결과 =====");
        System.out.println("성공 요청: " + successCount.get());
        System.out.println("실패 요청: " + failCount.get());
        System.out.println("예상 재고: 0");
        System.out.println("실제 재고: " + result.getRemainingStock());
        System.out.println("이벤트 상태: " + result.getStatus());
        System.out.println("===============================");

        // ✅ 이번엔 성공해야 함
        assertThat(result.getRemainingStock()).isEqualTo(0);
        assertThat(result.getStatus()).isEqualTo(EventStatus.SOLD_OUT);
        assertThat(successCount.get()).isEqualTo(100);
        assertThat(failCount.get()).isEqualTo(0);
    }

    @Test
    @DisplayName("성능 비교: 비관적 락 vs 동시성 제어 없음")
    void performance_comparison() throws InterruptedException {
        // 1. 동시성 제어 없음
        long startTime1 = System.currentTimeMillis();
        concurrency_problem_reproduction();
        long duration1 = System.currentTimeMillis() - startTime1;

        // 2. 비관적 락
        setUp(); // 데이터 초기화
        long startTime2 = System.currentTimeMillis();
        pessimistic_lock_solution();
        long duration2 = System.currentTimeMillis() - startTime2;

        System.out.println("===== 성능 비교 =====");
        System.out.println("동시성 제어 없음: " + duration1 + "ms");
        System.out.println("비관적 락: " + duration2 + "ms");
        System.out.println("차이: " + (duration2 - duration1) + "ms");
        System.out.println("==================");
    }
}