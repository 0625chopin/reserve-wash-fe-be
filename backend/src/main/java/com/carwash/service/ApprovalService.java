package com.carwash.service;

import com.carwash.domain.ManagerDayoff;
import com.carwash.domain.StoreHoliday;
import com.carwash.domain.enums.ApprovalStatus;
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

// 휴일/휴무 결재 워크플로우 (require 8장) — 상태값 변경 방식(워크플로우 엔진 미사용).
//   휴무: 2단계 승인(L1 최고매니저 → L2 관리자 → CONFIRMED). 휴일: 1단계 승인(관리자 → CONFIRMED).
//   불가능한 단계 전이는 도메인 메서드가 IllegalStateException(→ GlobalExceptionHandler 409).
@Service
public class ApprovalService {

    private final ManagerDayoffMapper dayoffMapper;
    private final StoreHolidayMapper holidayMapper;

    public ApprovalService(ManagerDayoffMapper dayoffMapper, StoreHolidayMapper holidayMapper) {
        this.dayoffMapper = dayoffMapper;
        this.holidayMapper = holidayMapper;
    }

    // ── 매니저 휴무 결재 ─────────────────────────────────────────────
    @Transactional
    public DayoffApprovalResponse submitDayoff(DayoffRequest req) {
        ManagerDayoff dayoff = ManagerDayoff.builder()
                .managerId(req.managerId())
                .date(req.date())
                .type(req.type())
                .status(ApprovalStatus.SUBMITTED)
                .build();
        dayoffMapper.insert(dayoff);   // useGeneratedKeys → id 채워짐
        return DayoffApprovalResponse.from(dayoff);
    }

    @Transactional
    public void approveDayoffL1(Long id) {
        ManagerDayoff dayoff = loadDayoff(id);
        dayoff.approveL1();   // SUBMITTED → APPROVED_L1 (불가 전이 시 예외)
        dayoffMapper.updateStatus(dayoff.getId(), dayoff.getStatus().name());
    }

    @Transactional
    public void approveDayoffL2(Long id) {
        ManagerDayoff dayoff = loadDayoff(id);
        dayoff.approveL2();   // APPROVED_L1 → CONFIRMED → 카탈로그(Manager.dayoffs)에 노출되어 슬롯 비활성 반영
        dayoffMapper.updateStatus(dayoff.getId(), dayoff.getStatus().name());
    }

    @Transactional
    public void rejectDayoff(Long id) {
        ManagerDayoff dayoff = loadDayoff(id);
        dayoff.reject();
        dayoffMapper.updateStatus(dayoff.getId(), dayoff.getStatus().name());
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
