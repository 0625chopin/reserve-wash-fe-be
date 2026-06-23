package com.carwash.dto;

// 전 매장 매출 비중 집계 응답 (v2.4) — 매장별 COMPLETED 예약 금액 합산.
//   상위 5개 + ETC 합산·비중(%) 가공은 FE(buildSalesSlices)에서 수행 → 서버는 매장별 합계만 반환.
public record SalesByStoreResponse(String storeId, String storeName, long amount) {}
