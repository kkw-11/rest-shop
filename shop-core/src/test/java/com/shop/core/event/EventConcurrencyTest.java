package com.shop.core.event;

import com.shop.common.constant.EventStatus;
import com.shop.common.constant.ItemSellStatus;
import com.shop.domain.event.Event;
import com.shop.domain.event.EventRepository;
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

/**
 * 이벤트 동시성 제어 테스트
 * - Domain 테스트가 아니라 Core(Service) 레이어 테스트
 * - 올바른 의존성 방향: Core → Domain
 */
@SpringBootTest
@ActiveProfiles("test")
class EventConcurrencyTest {

    @Autowired
    private EventService eventService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ItemRepository itemRepository;

    private Event testEvent;
    private Item testItem;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
        itemRepository.deleteAll();

        testItem = new Item();
        testItem.setItemNm("테스트 상품");
        testItem.setPrice(10000);
        testItem.setStockNumber(1000);
        testItem.setItemDetail("테스트 상품입니다.");
        testItem.setItemSellStatus(ItemSellStatus.SELL);
        itemRepository.save(testItem);

        testEvent = Event.createEvent(
            "신년 특가 이벤트",
            testItem,
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().plusDays(7),
            50,
            100,
            1
        );
        testEvent.activate();
        eventRepository.save(testEvent);
    }

    @Test
    @DisplayName("동시성 문제 재현: 100명이 동시에 주문하면 재고가 정확하게 차감되지 않는다")
    void concurrency_problem_reproduction() throws InterruptedException {
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    eventService.decreaseStockWithoutLock(testEvent.getId(), 1);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        Event result = eventRepository.findById(testEvent.getId()).orElseThrow();

        System.out.println("\n===== 동시성 문제 재현 결과 =====");
        System.out.println("성공 요청: " + successCount.get());
        System.out.println("실패 요청: " + failCount.get());
        System.out.println("예상 재고: 0");
        System.out.println("실제 재고: " + result.getRemainingStock());
        System.out.println("유실된 재고: " + result.getRemainingStock());
        System.out.println("================================\n");

        assertThat(result.getRemainingStock()).isNotEqualTo(0);
    }

    @Test
    @DisplayName("비관적 락 적용: 100명이 동시에 주문해도 재고가 정확하게 차감된다")
    void pessimistic_lock_solution() throws InterruptedException {
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    eventService.decreaseStockWithPessimisticLock(testEvent.getId(), 1);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        Event result = eventRepository.findById(testEvent.getId()).orElseThrow();
        
        System.out.println("\n===== 비관적 락 적용 결과 =====");
        System.out.println("성공 요청: " + successCount.get());
        System.out.println("실패 요청: " + failCount.get());
        System.out.println("예상 재고: 0");
        System.out.println("실제 재고: " + result.getRemainingStock());
        System.out.println("이벤트 상태: " + result.getStatus());
        System.out.println("==============================\n");

        assertThat(result.getRemainingStock()).isEqualTo(0);
        assertThat(result.getStatus()).isEqualTo(EventStatus.SOLD_OUT);
        assertThat(successCount.get()).isEqualTo(100);
    }

    @Test
    @DisplayName("성능 비교: 동시성 제어 없음 vs 비관적 락")
    void performance_comparison() throws InterruptedException {
        int threadCount = 100;

        // 1. 동시성 제어 없음
        long startTime1 = System.currentTimeMillis();
        runTest(threadCount, false);
        long duration1 = System.currentTimeMillis() - startTime1;

        // 데이터 초기화
        setUp();

        // 2. 비관적 락
        long startTime2 = System.currentTimeMillis();
        runTest(threadCount, true);
        long duration2 = System.currentTimeMillis() - startTime2;

        System.out.println("\n===== 성능 비교 =====");
        System.out.println("동시성 제어 없음: " + duration1 + "ms");
        System.out.println("비관적 락: " + duration2 + "ms");
        System.out.println("차이: " + (duration2 - duration1) + "ms");
        System.out.println("비율: " + String.format("%.2f", (double)duration2/duration1) + "x");
        System.out.println("====================\n");
    }

    private void runTest(int threadCount, boolean useLock) throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    if (useLock) {
                        eventService.decreaseStockWithPessimisticLock(testEvent.getId(), 1);
                    } else {
                        eventService.decreaseStockWithoutLock(testEvent.getId(), 1);
                    }
                } catch (Exception e) {
                    // ignore
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();
    }

}
