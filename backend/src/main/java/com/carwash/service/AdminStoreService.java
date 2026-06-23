package com.carwash.service;

import com.carwash.domain.Bay;
import com.carwash.domain.Store;
import com.carwash.dto.AdminStoreRequest;
import com.carwash.dto.AdminStoreResponse;
import com.carwash.exception.StoreHasDependenciesException;
import com.carwash.mapper.BayMapper;
import com.carwash.mapper.ManagerMapper;
import com.carwash.mapper.ReservationMapper;
import com.carwash.mapper.ReviewMapper;
import com.carwash.mapper.SlotMapper;
import com.carwash.mapper.StoreMapper;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// 관리자 매장 CRUD (v2.4) — /api/admin/** 경로 인가(ADMIN)로 보호.
//   🔒 정책: 신규 등록 매장 기본 approved=false(미승인), 연관 데이터 존재 시 삭제 차단(409).
//   베이(bay)는 매장 구성요소라 매장과 함께 생성/교체/삭제한다(삭제 의존성에는 미포함).
@Service
@Transactional
public class AdminStoreService {

    private final StoreMapper storeMapper;
    private final BayMapper bayMapper;
    private final ReservationMapper reservationMapper;
    private final ReviewMapper reviewMapper;
    private final ManagerMapper managerMapper;
    private final SlotMapper slotMapper;

    public AdminStoreService(
            StoreMapper storeMapper,
            BayMapper bayMapper,
            ReservationMapper reservationMapper,
            ReviewMapper reviewMapper,
            ManagerMapper managerMapper,
            SlotMapper slotMapper) {
        this.storeMapper = storeMapper;
        this.bayMapper = bayMapper;
        this.reservationMapper = reservationMapper;
        this.reviewMapper = reviewMapper;
        this.managerMapper = managerMapper;
        this.slotMapper = slotMapper;
    }

    // 관리자용 전체 목록 — 승인/미승인 모두 포함(FO GET /api/stores는 승인만)
    @Transactional(readOnly = true)
    public List<AdminStoreResponse> list() {
        return storeMapper.findAll().stream()
                .map(s -> AdminStoreResponse.from(s, bayMapper.findByStore(s.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminStoreResponse get(String id) {
        Store store = requireStore(id);
        return AdminStoreResponse.from(store, bayMapper.findByStore(id));
    }

    // 등록 — 🔒 approved 미입력 시 false(미승인)로 생성. 베이 동반 insert.
    public AdminStoreResponse create(AdminStoreRequest req) {
        String id = "store-" + UUID.randomUUID().toString().substring(0, 8);
        Store store = Store.builder()
                .id(id)
                .name(req.name())
                .bayCount(req.bayCount())
                .approved(req.approvedOrDefault())
                .build();
        storeMapper.insert(store);
        insertBays(id, req);
        return AdminStoreResponse.from(store, bayMapper.findByStore(id));
    }

    // 수정 — 매장 필드 갱신 + 베이 전체 교체(deleteByStore 후 재insert)
    public AdminStoreResponse update(String id, AdminStoreRequest req) {
        requireStore(id);
        Store store = Store.builder()
                .id(id)
                .name(req.name())
                .bayCount(req.bayCount())
                .approved(req.approvedOrDefault())
                .build();
        storeMapper.update(store);
        bayMapper.deleteByStore(id);
        insertBays(id, req);
        return AdminStoreResponse.from(store, bayMapper.findByStore(id));
    }

    // 삭제 — 🔒 연관 데이터(예약/후기/매니저/슬롯) 1건이라도 있으면 409로 차단(소프트 비활성 미채택).
    //   베이는 매장 구성요소라 의존성에서 제외하고 매장과 함께 물리 삭제한다.
    public void delete(String id) {
        requireStore(id);
        long deps = reservationMapper.countByStore(id)
                + reviewMapper.countByStore(id)
                + managerMapper.countByStore(id)
                + slotMapper.countByStore(id);
        if (deps > 0) {
            throw new StoreHasDependenciesException(
                    "연관 데이터(예약/후기/매니저/슬롯)가 있어 매장을 삭제할 수 없습니다.");
        }
        bayMapper.deleteByStore(id);
        storeMapper.deleteById(id);
    }

    private void insertBays(String storeId, AdminStoreRequest req) {
        for (AdminStoreRequest.BayInput b : req.baysOrEmpty()) {
            bayMapper.insert(Bay.builder()
                    .id(storeId + "-" + b.code())
                    .storeId(storeId)
                    .code(b.code())
                    .size(b.size())
                    .build());
        }
    }

    private Store requireStore(String id) {
        Store store = storeMapper.findById(id);
        if (store == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "매장을 찾을 수 없습니다.");
        }
        return store;
    }
}
