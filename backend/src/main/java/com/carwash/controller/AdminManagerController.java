package com.carwash.controller;

import com.carwash.dto.AdminCreateManagerRequest;
import com.carwash.dto.ManagerSignupResponse;
import com.carwash.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// 관리자 직접 매니저 등록 (require v1.12 §4.1) — /api/admin/** 는 SecurityConfig 경로 인가(ADMIN)로 보호.
//   생성된 계정은 PENDING_APPROVAL_L2로 '가입 최종 승인'(GET /api/admin/manager-approvals) 목록에 합류해 관리자 최종 승인 후 활성화된다.
@RestController
@RequestMapping("/api/admin/managers")
public class AdminManagerController {

    private final AuthService authService;

    public AdminManagerController(AuthService authService) {
        this.authService = authService;
    }

    // role(MANAGER|STORE_ADMIN) 지정 매니저 등록 — PENDING_APPROVAL_L2 생성. 잘못된 역할/중복 이메일은 400/409.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ManagerSignupResponse create(@Valid @RequestBody AdminCreateManagerRequest req) {
        return authService.adminCreateManager(
                req.email(), req.password(), req.name(), req.storeId(), req.role());
    }
}
