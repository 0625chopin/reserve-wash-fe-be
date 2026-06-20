package com.carwash.mapper;

import com.carwash.domain.Review;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

// 후기 매퍼 — Phase 1은 기본 조회/삽입만(후기 API는 Phase 8)
@Mapper
public interface ReviewMapper {

    Review findById(String id);

    List<Review> findByStore(String storeId);

    int insert(Review review);
}
