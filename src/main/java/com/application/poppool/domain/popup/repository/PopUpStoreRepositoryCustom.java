package com.application.poppool.domain.popup.repository;

import com.application.poppool.domain.category.enums.Category;
import com.application.poppool.domain.home.dto.response.GetHomeInfoResponse;
import com.application.poppool.domain.user.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface PopUpStoreRepositoryCustom {

    List<Category> getUserInterestCategoryList(String userId);
    Page<GetHomeInfoResponse.CustomPopUpStore> getCustomPopUpStoreList(UserEntity user, Pageable pageable);
    Page<GetHomeInfoResponse.PopularPopUpStore> getPopularPopUpStoreList(Pageable pageable);
    Page<GetHomeInfoResponse.NewPopUpStore> getNewPopUpStoreList(LocalDateTime currentDate, Pageable pageable);

}
