package com.carwash.exception;

// 슬롯 점유/확정 충돌 — 다른 트랜잭션이 선점(낙관락 영향행수 0) 시 발생 → 409 (require 7.3)
public class SlotConflictException extends RuntimeException {

    public SlotConflictException(String message) {
        super(message);
    }
}
