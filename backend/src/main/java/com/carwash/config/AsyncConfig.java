package com.carwash.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

// 비동기 실행 설정 (Phase 9 알림) — 메일 발송(EmailSender.send)을 @Async로 분리한다.
//   발송은 본 트랜잭션(가입/예약/결재)과 다른 스레드에서 수행되어, 발송 실패가 도메인 흐름을
//   롤백시키지 않는다. 수신자/본문 해석은 호출자 트랜잭션에서 동기로 끝내고(uncommitted-read 회피),
//   실제 SMTP dispatch만 이 풀에서 비동기로 처리한다.
@Configuration
@EnableAsync
public class AsyncConfig {

    // 메일 발송 전용 스레드 풀 — 요청 처리 스레드를 점유하지 않도록 분리
    @Bean(name = "mailTaskExecutor")
    public Executor mailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);                 // 평상시 유지 스레드 수
        executor.setMaxPoolSize(5);                  // 버스트 시 최대 스레드 수
        executor.setQueueCapacity(100);              // 대기 큐 — 초과 시 호출자 스레드에서 실행
        executor.setThreadNamePrefix("mail-async-"); // 로그 추적용 스레드명
        executor.initialize();
        return executor;
    }
}
