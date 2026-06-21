package com.carwash.service;

import com.carwash.domain.ManagerDayoff;
import com.carwash.domain.StoreHoliday;
import com.carwash.domain.enums.ApprovalStatus;
import com.carwash.domain.enums.DayoffApprovalStatus;
import com.carwash.dto.DayoffApprovalResponse;
import com.carwash.dto.DayoffRequest;
import com.carwash.dto.HolidayApprovalResponse;
import com.carwash.dto.HolidayRequest;
import com.carwash.mapper.ManagerDayoffMapper;
import com.carwash.mapper.StoreHolidayMapper;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// 휴일/휴무 결재 워크플로우 (require v1.7 §8장) — 상태값 변경 방식(워크플로우 엔진 미사용).
//   휴가/반차: 1단계 승인(매장매니저관리자 STORE_ADMIN → APPROVED 종결, 관리자 개입 없음, require §8.2·§8.3).
//   매장 휴일: 1단계 승인(관리자 → CONFIRMED). ⚠️ 휴가/반차(1단계)와 가입(2단계)의 단계 수가 다름에 유의.
//   불가능한 단계 전이는 도메인 메서드가 IllegalStateException(→ GlobalExceptionHandler 409).
@Service
public class ApprovalService {

    private final ManagerDayoffMapper dayoffMapper;
    private final StoreHolidayMapper holidayMapper;
    private final NotificationService notificationService;

    public ApprovalService(
            ManagerDayoffMapper dayoffMapper,
            StoreHolidayMapper holidayMapper,
            NotificationService notificationService) {
        this.dayoffMapper = dayoffMapper;
        this.holidayMapper = holidayMapper;
        this.notificationService = notificationService;
    }

    // ── 매니저 휴가/반차 결재 (1단계, 매장매니저관리자 종결) ──────────────
    @Transactional
    public DayoffApprovalResponse submitDayoff(DayoffRequest req) {
        ManagerDayoff dayoff = ManagerDayoff.builder()
                .managerId(req.managerId())
                .date(req.date())
                .type(req.type())
                .status(DayoffApprovalStatus.SUBMITTED)
                .build();
        dayoffMapper.insert(dayoff);   // useGeneratedKeys → id 채워짐
        return DayoffApprovalResponse.from(dayoff);
    }

    // 휴가/반차 1단계 승인 — 매장매니저관리자(STORE_ADMIN)가 SUBMITTED → APPROVED로 종결(M8).
    //   APPROVED 시 카탈로그(Manager.dayoffs)에 노출되어 슬롯 비활성 반영(require §6.1). 관리자 개입 없음.
    @Transactional
    public void approveDayoff(Long id) {
        ManagerDayoff dayoff = loadDayoff(id);
        dayoff.approve();   // SUBMITTED → APPROVED (불가 전이 시 예외)
        dayoffMapper.updateStatus(dayoff.getId(), dayoff.getStatus().name());
        notificationService.notifyDayoffApprovalResult(dayoff.getManagerId(), "승인");   // 결재 결과 통지(Phase 9)
    }

    @Transactional
    public void rejectDayoff(Long id) {
        ManagerDayoff dayoff = loadDayoff(id);
        dayoff.reject();
        dayoffMapper.updateStatus(dayoff.getId(), dayoff.getStatus().name());
        notificationService.notifyDayoffApprovalResult(dayoff.getManagerId(), "반려");   // 결재 결과 통지(Phase 9)
    }

    @Transactional
    public void resubmitDayoff(Long id) {
        ManagerDayoff dayoff = loadDayoff(id);
        dayoff.resubmit();   // REJECTED → SUBMITTED (재신청)
        dayoffMapper.updateStatus(dayoff.getId(), dayoff.getStatus().name());
    }

    @Transactional(readOnly = true)
    public List<DayoffApprovalResponse> listDayoffsByStore(String storeId) {
        return dayoffMapper.findByStore(storeId).stream().map(DayoffApprovalResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<DayoffApprovalResponse> listAllDayoffs() {
        return dayoffMapper.findAll().stream().map(DayoffApprovalResponse::from).toList();
    }

    private ManagerDayoff loadDayoff(Long id) {
        ManagerDayoff dayoff = dayoffMapper.findById(id);
        if (dayoff == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "휴무 신청을 찾을 수 없습니다.");
        }
        return dayoff;
    }

    // ── 매장 휴일 결재(단일 승인) ───────────────────────────────────
    @Transactional
    public HolidayApprovalResponse submitHoliday(HolidayRequest req) {
        StoreHoliday holiday = StoreHoliday.builder()
                .storeId(req.storeId())
                .date(req.date())
                .status(ApprovalStatus.SUBMITTED)
                .build();
        holidayMapper.insert(holiday);
        return HolidayApprovalResponse.from(holiday);
    }

    @Transactional
    public void approveHoliday(Long id) {
        StoreHoliday holiday = loadHoliday(id);
        holiday.approve();   // SUBMITTED → CONFIRMED
        holidayMapper.updateStatus(holiday.getId(), holiday.getStatus().name());
    }

    @Transactional
    public void rejectHoliday(Long id) {
        StoreHoliday holiday = loadHoliday(id);
        holiday.reject();
        holidayMapper.updateStatus(holiday.getId(), holiday.getStatus().name());
    }

    @Transactional(readOnly = true)
    public List<HolidayApprovalResponse> listHolidays() {
        return holidayMapper.findAll().stream().map(HolidayApprovalResponse::from).toList();
    }

    private StoreHoliday loadHoliday(Long id) {
        StoreHoliday holiday = holidayMapper.findById(id);
        if (holiday == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "휴일 신청을 찾을 수 없습니다.");
        }
        return holiday;
    }
}
