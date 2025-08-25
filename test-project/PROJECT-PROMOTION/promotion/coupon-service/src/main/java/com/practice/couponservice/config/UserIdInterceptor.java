package com.practice.couponservice.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class UserIdInterceptor implements HandlerInterceptor {

    private static final String USER_ID_HEADER = "X-USER-ID";
    private static final ThreadLocal<Long> currentUserId = new ThreadLocal<>();

    // 핸들러 메서드 실행 직전에 호출
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String userIdStr = request.getHeader(USER_ID_HEADER);

        if(userIdStr == null || userIdStr.isEmpty()) {
            throw new IllegalStateException("X-USER-ID header is required");
        }

        try{
            currentUserId.set(Long.parseLong(userIdStr)); // 원래 Long인 UserId를 X-USER-ID헤더의 value로 저장할때 String으로 저장해놨기때문에 다시 Long타입으로 바꿔서 저장
            return true;
        }catch (NumberFormatException e){
            throw new IllegalStateException("X-USER-ID header is invalid");
        }
    }

    // 뷰 렌더링까지 완료된 뒤 호출
    // 인터셉터의 preHandle이 true를 반환했을 경우에만 afterCompletion이 보장됨
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 같은 스레드가 재사용될 수 있으므로, 메모리 누수를 막기위해  remove 해줘야한다
        currentUserId.remove();
    }

    public static Long getCurrentUserId() {
        Long userId = currentUserId.get();
        if(userId == null) {
            throw new IllegalStateException("User ID not found in current context");
        }
        return userId;
    }
}
