package com.carwash.controller;

import com.carwash.dto.PriceResponse;
import com.carwash.service.PriceService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 가격 조회 — 전체 매트릭스(카탈로그 캐시용, require 10.3)
@RestController
@RequestMapping("/api/prices")
public class PriceController {

    private final PriceService priceService;

    public PriceController(PriceService priceService) {
        this.priceService = priceService;
    }

    @GetMapping
    public List<PriceResponse> prices() {
        return priceService.findAll();
    }
}
