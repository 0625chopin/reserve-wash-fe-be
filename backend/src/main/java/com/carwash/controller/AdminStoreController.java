package com.carwash.controller;

import com.carwash.dto.AdminStoreRequest;
import com.carwash.dto.AdminStoreResponse;
import com.carwash.service.AdminStoreService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// 관리자 매장 등록/수정/삭제 (v2.4) — /api/admin/** 는 SecurityConfig 경로 인가로 ADMIN 한정.
//   GET은 승인/미승인 포함 전체(FO GET /api/stores는 승인 매장만 반환하는 것과 구분).
@RestController
@RequestMapping("/api/admin/stores")
public class AdminStoreController {

    private final AdminStoreService adminStoreService;

    public AdminStoreController(AdminStoreService adminStoreService) {
        this.adminStoreService = adminStoreService;
    }

    @GetMapping
    public List<AdminStoreResponse> list() {
        return adminStoreService.list();
    }

    @GetMapping("/{id}")
    public AdminStoreResponse get(@PathVariable String id) {
        return adminStoreService.get(id);
    }

    // 등록 — 🔒 approved 미입력 시 false(미승인)로 생성
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminStoreResponse create(@RequestBody AdminStoreRequest req) {
        return adminStoreService.create(req);
    }

    @PutMapping("/{id}")
    public AdminStoreResponse update(@PathVariable String id, @RequestBody AdminStoreRequest req) {
        return adminStoreService.update(id, req);
    }

    // 삭제 — 🔒 연관 데이터 존재 시 409(STORE_HAS_DEPENDENCIES)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        adminStoreService.delete(id);
    }
}
