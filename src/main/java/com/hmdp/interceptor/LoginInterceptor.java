package com.hmdp.interceptor;

import com.hmdp.utils.UserHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 判断是否需要做拦截
        if(UserHolder.getUser() == null){
            // 没有用户，需要拦截
            response.setStatus(401);
            return false;
        }
        // 有用户，则放行
        return true;
    }

}
