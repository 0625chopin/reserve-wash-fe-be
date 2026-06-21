package com.carwash.mapper;

import com.carwash.domain.NotificationLog;
import org.apache.ibatis.annotations.Mapper;

// 알림 발송 이력 매퍼 (Phase 9) — 발송 시도/스킵 기록(추적성). id는 useGeneratedKeys로 채움.
@Mapper
public interface NotificationLogMapper {

    int insert(NotificationLog log);
}
