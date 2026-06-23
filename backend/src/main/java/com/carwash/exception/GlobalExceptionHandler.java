package com.carwash.exception;

import com.carwash.dto.ErrorResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

// 예약 도메인 예외를 일관 응답으로 매핑 (require 7.3)
//   슬롯 충돌(낙관락/UNIQUE 위반) → 409, 검증 오류 → 400.
//   서비스가 던지는 ResponseStatusException(400/403/404/409)도 본 advice가 직접 처리한다.
//   (Spring 기본 ResponseStatusExceptionResolver는 response.sendError()로 처리하는데,
//    이 sendError가 stateless 보안 체인에서 인증 없는 /error 디스패치를 유발해 의도한 상태/메시지가
//    전부 빈 401로 가려진다 — BUG-3. ResponseEntity를 직접 써서 sendError 경로를 우회한다.)
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 서비스 계층 도메인 검증 예외 — 던진 상태코드·사유를 그대로 노출 (빈 401 마스킹 방지)
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException e) {
        HttpStatusCode status = e.getStatusCode();
        String code = (status instanceof HttpStatus hs) ? hs.name() : "STATUS_" + status.value();
        String message = e.getReason() != null ? e.getReason() : "요청을 처리할 수 없습니다.";
        return ResponseEntity.status(status).body(new ErrorResponse(code, message));
    }

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

    // 매장 삭제 무결성(v2.4) — 연관 데이터(예약/후기/매니저/슬롯) 존재 시 409
    @ExceptionHandler(StoreHasDependenciesException.class)
    public ResponseEntity<ErrorResponse> handleStoreHasDependencies(StoreHasDependenciesException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("STORE_HAS_DEPENDENCIES", e.getMessage()));
    }

    // 불가능한 상태 전이(도메인 가드) — 409. 예: COMPLETED 예약 재취소 (require 11.3)
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTransition(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("INVALID_TRANSITION", e.getMessage()));
    }

    // Bean Validation 오류 — 400
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("VALIDATION_ERROR", "요청 값이 올바르지 않습니다."));
    }
}
