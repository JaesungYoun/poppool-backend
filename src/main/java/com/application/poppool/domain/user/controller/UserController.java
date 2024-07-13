package com.application.poppool.domain.user.controller;

import com.application.poppool.domain.user.dto.request.CheckedSurveyListRequest;
import com.application.poppool.domain.user.dto.response.GetMyCommentResponse;
import com.application.poppool.domain.user.dto.response.GetMyPageResponse;
import com.application.poppool.domain.user.dto.response.GetWithDrawlSurveyResponse;
import com.application.poppool.domain.user.service.UserService;
import com.application.poppool.global.jwt.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController implements UserControllerDoc {

    private final UserService userService;
    private final JwtService jwtService;

    /**
     * 마이페이지 조회
     *
     * @param userId
     * @return
     */
    @Override
    @GetMapping("/{userId}")
    public ResponseEntity<GetMyPageResponse> getMyPage(@PathVariable String userId) {
        return ResponseEntity.ok(userService.getMyPage(userId));
    }

    /**
     * 내가 쓴 일반 코멘트 조회
     * @param userId
     * @return
     */
    @Override
    @GetMapping("/{userId}/comments")
    public ResponseEntity<GetMyCommentResponse> getMyCommentList(@PathVariable String userId,
                                                                 @PageableDefault(page = 0, size = 10, sort = "createDateTime",direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(userService.getMyCommentList(userId, pageable));
    }


    /**
     * 회원 탈퇴
     * @param userId
     * @param request - 체크된 설문 항목
     */
    @Override
    @PostMapping("/{userId}/delete")
    public void deleteUser(@PathVariable String userId,
                           @RequestBody @Valid CheckedSurveyListRequest request) {
        userService.deleteUser(userId, request);
    }

    /**
     * 설문 항목 리스트
     * @return
     */
    @Override
    @GetMapping("/withdrawl/surveys")
    public ResponseEntity<GetWithDrawlSurveyResponse> getWithDrawlSurvey() {
        return ResponseEntity.ok(userService.getWithDrawlSurvey());
    }

    /**
     * 회원 로그아웃
     * @param request
     */
    @Override
    @PostMapping("/logout")
    public void logout(HttpServletRequest request) {
        String accessToken = jwtService.getAccessToken(request);
        LocalDateTime expiryDateTime = jwtService.getExpiration(accessToken);
        userService.logout(accessToken, expiryDateTime);
    }

}
