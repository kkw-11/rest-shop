package com.shop.core.event;

import com.shop.common.constant.EventStatus;
import com.shop.common.constant.ItemSellStatus;
import com.shop.core.order.EventOrderFacade;
import com.shop.core.order.EventOrderService;
import com.shop.domain.event.Event;
import com.shop.domain.event.EventParticipantRepository;
import com.shop.domain.event.EventRepository;
import com.shop.domain.item.Item;
import com.shop.domain.item.ItemRepository;
import com.shop.domain.member.Member;
import com.shop.domain.member.MemberRepository;
import com.shop.domain.order.OrderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 이벤트 동시성 제어 테스트
 * Phase 4: Event 전용 재고 기반 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
class EventConcurrencyTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private EventOrderService eventOrderService;

    @Autowired
    private EventOrderFacade eventOrderFacade;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EventParticipantRepository participantRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private Event testEvent;
    private Item testItem;
    private List<Member> testMembers;

    @BeforeEach
    @Transactional
    void setUp() {
        // TransactionTemplate 제거!

        // 데이터 정리
        participantRepository.deleteAll();
        orderRepository.deleteAll();
        eventRepository.deleteAll();
        memberRepository.deleteAll();
        itemRepository.deleteAll();

        // Item 생성
        testItem = new Item();
        testItem.setItemNm("테스트 상품");
        testItem.setPrice(10000);
        testItem.setStockNumber(1000);
        testItem.setItemDetail("테스트 상품입니다.");
        testItem.setItemSellStatus(ItemSellStatus.SELL);
        itemRepository.save(testItem);

        // Event 생성
        testEvent = Event.createEvent(
                "선착순 이벤트",
                testItem,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(7),
                50,
                100,
                1
        );
        testEvent.activate();
        eventRepository.save(testEvent);

        // Members 생성
        testMembers = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Member member = Member.createMember(
                    "test" + i + "@test.com",
                    "테스트" + i,
                    "password",
                    "test"
            );
            memberRepository.save(member);
            testMembers.add(member);
        }
    }

    @AfterEach
    void tearDown() {
        participantRepository.deleteAll();
        orderRepository.deleteAll();
        eventRepository.deleteAll();
        memberRepository.deleteAll();
        itemRepository.deleteAll();
    }

    @Test
    @DisplayName("동시성 문제 재현: 100명이 동시에 주문하면 재고가 정확하게 차감되지 않는다")
    void concurrency_problem_reproduction() throws InterruptedException {
        // Given
        int threadCount = 100;
        int initialEventStock = testEvent.getRemainingStock();  // 100
        int initialItemStock = testItem.getStockNumber();       // 1000

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When: 100명 동시 주문 (락 없이)
        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executorService.submit(() -> {
                try {
                    eventOrderService.createOrder(
                            testEvent.getId(),
                            testMembers.get(idx).getId()
                    );
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

        // Then
        Event result = eventRepository.findById(testEvent.getId()).orElseThrow();
        Item itemResult = itemRepository.findById(testItem.getId()).orElseThrow();

        System.out.println("\n===== 동시성 문제 재현 결과 =====");
        System.out.println("성공 요청: " + successCount.get());
        System.out.println("실패 요청: " + failCount.get());
        System.out.println("--- Event 재고 ---");
        System.out.println("  예상 재고: 0");
        System.out.println("  실제 재고: " + result.getRemainingStock());
        System.out.println("  유실 재고: " + result.getRemainingStock());
        System.out.println("--- Item 재고 ---");
        System.out.println("  초기 재고: " + initialItemStock);
        System.out.println("  현재 재고: " + itemResult.getStockNumber());
        System.out.println("  변화: " + (itemResult.getStockNumber() == initialItemStock ? "없음 ✅" : "있음 ❌"));
        System.out.println("================================\n");

        // Event 재고가 정확하지 않음
        assertThat(result.getRemainingStock()).isNotEqualTo(0);
        // Item 재고는 변하지 않음
        assertThat(itemResult.getStockNumber()).isEqualTo(initialItemStock);
    }

    @Test
    @DisplayName("비관적 락 적용: 100명이 동시에 주문해도 재고가 정확하게 차감된다")
    void pessimistic_lock_solution() throws InterruptedException {
        // Given
        int threadCount = 100;
        int initialEventStock = testEvent.getRemainingStock();  // 100
        int initialItemStock = testItem.getStockNumber();       // 1000

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When: 100명 동시 주문 (비관적 락)
        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executorService.submit(() -> {
                try {
                    eventOrderService.createOrderWithPessimisticLock(
                            testEvent.getId(),
                            testMembers.get(idx).getId()
                    );
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

        // Then
        Event result = eventRepository.findById(testEvent.getId()).orElseThrow();
        Item itemResult = itemRepository.findById(testItem.getId()).orElseThrow();

        System.out.println("\n===== 비관적 락 적용 결과 =====");
        System.out.println("성공 요청: " + successCount.get());
        System.out.println("실패 요청: " + failCount.get());
        System.out.println("--- Event 재고 ---");
        System.out.println("  예상 재고: 0");
        System.out.println("  실제 재고: " + result.getRemainingStock());
        System.out.println("  유실 재고: 0 ✅");
        System.out.println("--- Item 재고 ---");
        System.out.println("  초기 재고: " + initialItemStock);
        System.out.println("  현재 재고: " + itemResult.getStockNumber());
        System.out.println("  변화: " + (itemResult.getStockNumber() == initialItemStock ? "없음 ✅" : "있음 ❌"));
        System.out.println("--- 결과 ---");
        System.out.println("  이벤트 상태: " + result.getStatus());
        System.out.println("  주문 수: " + orderRepository.count());
        System.out.println("  참여자 수: " + participantRepository.count());
        System.out.println("==============================\n");

        // 검증
        assertThat(result.getRemainingStock()).isEqualTo(0);
        assertThat(result.getStatus()).isEqualTo(EventStatus.SOLD_OUT);
        assertThat(itemResult.getStockNumber()).isEqualTo(initialItemStock);
        assertThat(successCount.get()).isEqualTo(100);
        assertThat(orderRepository.count()).isEqualTo(100);
    }

    @Test
    @DisplayName("Redis 분산 락 적용: 100명이 동시에 주문해도 재고가 정확하게 차감된다")
    void redis_lock_solution() throws InterruptedException {
        // Given
        int threadCount = 100;
        int initialEventStock = testEvent.getRemainingStock();  // 100
        int initialItemStock = testItem.getStockNumber();       // 1000

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When: 100명 동시 주문 (Redis 분산 락)
        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executorService.submit(() -> {
                try {
                    eventOrderFacade.createOrderWithRedisLock(
                            testEvent.getId(),
                            testMembers.get(idx).getId()
                    );
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

        // Then
        Event result = eventRepository.findById(testEvent.getId()).orElseThrow();
        Item itemResult = itemRepository.findById(testItem.getId()).orElseThrow();

        System.out.println("\n===== Redis 분산 락 적용 결과 =====");
        System.out.println("성공 요청: " + successCount.get());
        System.out.println("실패 요청: " + failCount.get());
        System.out.println("--- Event 재고 ---");
        System.out.println("  예상 재고: 0");
        System.out.println("  실제 재고: " + result.getRemainingStock());
        System.out.println("  유실 재고: 0 ✅");
        System.out.println("--- Item 재고 ---");
        System.out.println("  초기 재고: " + initialItemStock);
        System.out.println("  현재 재고: " + itemResult.getStockNumber());
        System.out.println("  변화: " + (itemResult.getStockNumber() == initialItemStock ? "없음 ✅" : "있음 ❌"));
        System.out.println("--- 결과 ---");
        System.out.println("  이벤트 상태: " + result.getStatus());
        System.out.println("  주문 수: " + orderRepository.count());
        System.out.println("  참여자 수: " + participantRepository.count());
        System.out.println("===================================\n");

        // 검증
        assertThat(result.getRemainingStock()).isEqualTo(0);
        assertThat(result.getStatus()).isEqualTo(EventStatus.SOLD_OUT);
        assertThat(itemResult.getStockNumber()).isEqualTo(initialItemStock);
        assertThat(successCount.get()).isEqualTo(100);
        assertThat(orderRepository.count()).isEqualTo(100);
    }
}
