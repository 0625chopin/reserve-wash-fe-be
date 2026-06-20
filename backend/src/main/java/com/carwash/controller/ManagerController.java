package com.carwash.controller;

import com.carwash.dto.ManagerResponse;
import com.carwash.service.ManagerService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 매니저 조회 — 전체(dayoffs 포함, 카탈로그 캐시용)
@RestController
@RequestMapping("/api/managers")
public class ManagerController {

    private final ManagerService managerService;

    public ManagerController(ManagerService managerService) {
        this.managerService = managerService;
    }

    @GetMapping
    public List<ManagerResponse> managers() {
        return managerService.findAll();
    }
}
