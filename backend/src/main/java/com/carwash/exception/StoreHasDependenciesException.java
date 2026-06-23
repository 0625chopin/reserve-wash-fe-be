package com.carwash.exception;

// 매장 삭제 무결성(v2.4) — 연관 데이터(예약/후기/매니저/슬롯)가 있는 매장 삭제 시도.
//   GlobalExceptionHandler가 409 Conflict(STORE_HAS_DEPENDENCIES)로 매핑한다. 소프트 비활성 미채택.
public class StoreHasDependenciesException extends RuntimeException {

    public StoreHasDependenciesException(String message) {
        super(message);
    }
}
