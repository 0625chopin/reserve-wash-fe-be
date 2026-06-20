package com.carwash.concurrency;

import static org.assertj.core.api.Assertions.assertThat;

import com.carwash.domain.enums.CarType;
import com.carwash.domain.enums.ServiceType;
import com.carwash.dto.ConfirmRequest;
import com.carwash.service.ReservationService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

// 동시성 통합 테스트 (require 7장·6.1) — 동일 슬롯 동시 확정 중 정확히 1건만 성공
//   슬롯 행은 희소 → 동시 INSERT를 uk_slot UNIQUE가 1건으로 직렬화(최종 방어선) + 낙관락 보조.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ReservationConcurrencyTest {

    @Autowired
    private ReservationService reservationService;

    @Test
    void 동시_확정_N건_중_정확히_1건만_성공() throws InterruptedException {
        int threads = 16;
        // 전용 미점유 슬롯(다른 테스트와 키 충돌 없음)
        ConfirmRequest req = new ConfirmRequest(
                "store2", "store2-A1", "2026-09-09", "10:00",
                null, CarType.SMALL, ServiceType.EXT, 12000);

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger conflict = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();              // 동시 출발
                    reservationService.confirm("user1", req);
                    success.incrementAndGet();
                } catch (Exception e) {
                    conflict.incrementAndGet(); // SlotConflict / DataIntegrityViolation 등
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();
        start.countDown();
        done.await();
        pool.shutdown();

        // 정확히 1건 성공, 나머지는 충돌(409 대상)
        assertThat(success.get()).isEqualTo(1);
        assertThat(conflict.get()).isEqualTo(threads - 1);
    }
}
