package com.carwash.controller;

import com.carwash.dto.BayResponse;
import com.carwash.service.BayService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 베이 조회 — 전체(카탈로그 캐시용)
@RestController
@RequestMapping("/api/bays")
public class BayController {

    private final BayService bayService;

    public BayController(BayService bayService) {
        this.bayService = bayService;
    }

    @GetMapping
    public List<BayResponse> bays() {
        return bayService.findAll();
    }
}
