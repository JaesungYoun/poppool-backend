package com.application.poppool.domain.user.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class GetMyPageResponse {

    private String nickname;
    private String profileImage;
    private String instagramId;
    private List<PopUpInfo> popUpInfoList;
    private boolean isLogin;

    @Getter
    @Builder
    public static class PopUpInfo {
        private Long popUpStoreId;
        private String popUpStoreName;
    }

}
