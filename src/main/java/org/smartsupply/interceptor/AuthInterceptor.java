package org.smartsupply.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.smartsupply.model.entity.User;
import org.smartsupply.service.implementation.SessionManager;
import org.smartsupply.service.implementation.UserContext;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final SessionManager sessionManager;
    private final UserContext userContext;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String sessionId = request.getHeader("Session-Id");

        if (sessionId != null) {
            User user = sessionManager.getUserFromSession(sessionId);
            if (user != null) {
                userContext.setCurrentUser(user);
            }
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        userContext.clear();
    }
}
