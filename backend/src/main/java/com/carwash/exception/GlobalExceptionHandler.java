package com.carwash.exception;

import com.carwash.dto.ErrorResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// 예약 도메인 예외를 일관 응답으로 매핑 (require 7.3)
//   슬롯 충돌(낙관락/UNIQUE 위반) → 409, 검증 오류 → 400.
//   Phase 3 인증의 ResponseStatusException(401/409)은 Spring이 자체 처리하므로 본 advice가 가로채지 않는다.
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 낙관락 충돌(도메인) — 409
    @ExceptionHandler(SlotConflictException.class)
    public ResponseEntity<ErrorResponse> handleSlotConflict(SlotConflictException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("SLOT_CONFLICT", e.getMessage()));
    }

    // 슬롯 UNIQUE 위반(동시 INSERT 최종 방어선) — 500이 아닌 409
    @ExceptionHandler({DuplicateKeyException.class, DataIntegrityViolationException.class})
    public ResponseEntity<ErrorResponse> handleDuplicate(Exception e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("SLOT_CONFLICT", "선택하신 슬롯이 방금 예약되었습니다."));
    }

    // Bean Validation 오류 — 400
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("VALIDATION_ERROR", "요청 값이 올바르지 않습니다."));
    }
}
