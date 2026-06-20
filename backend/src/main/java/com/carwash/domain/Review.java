package com.carwash.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 후기/평점 — 예약(세차) 완료 사용자만 작성 (require 9.1) — 순수 POJO. FE Review와 무변환 일치
//   id는 문자열(앱이 부여), managerId nullable, createdAt은 ISO 문자열
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    private String id;
    private String reservationId;
    private String userId;
    private String storeId;
    private String managerId;       // nullable
    private int rating;             // 1 ~ 5 정수 (require 9.1)
    private String text;
    private String createdAt;       // ISO 문자열
}
