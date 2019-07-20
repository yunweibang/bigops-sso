/*
 * Copyright 2018 www.yunweibang.com Inc. All rights reserved.
 */
package com.yunweibang.auth.conf;

import com.yunweibang.auth.common.JsonResponse;
import com.yunweibang.auth.utils.CookieUtil;
import com.yunweibang.auth.utils.JdbcUtils;
import com.yunweibang.auth.utils.JsonUtil;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapHandler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class ResetPassTokenInterceptor implements HandlerInterceptor {
    private static Logger logger = LoggerFactory.getLogger(ResetPassTokenInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        logger.info("ResetPassTokenInterceptor   enter ==========================");
        //没有cookies 返回401
        logger.info("ResetPassTokenInterceptor   requesturl =" + request.getRequestURI());

        String authToken = CookieUtil.getCookieValue(request, "tmp_token");
        logger.info("CookieUtil.getCookieValue   authToken=" + authToken);

        if (StringUtils.isBlank(authToken)) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(JsonUtil.toJson(new JsonResponse<>(401, "token为空", null)));
            return false;
        }

        //有cookie 
        QueryRunner qr = new QueryRunner(JdbcUtils.getDs());
        String sql = "select * from sso_find_pwd where token= ? ";
        Object[] params = new Object[]{authToken};
        Map map = null;
        try {
            map = qr.query(sql, new MapHandler(), params);
            if (map != null && map.size() > 0 && map.containsKey("id")) {
                logger.info("ResetPassTokenInterceptor select token success");
                SimpleDateFormat sdfTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                try {

                    if (new Date().getTime() > sdfTime.parse(map.get("token_expire_time").toString()).getTime()) {
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write(JsonUtil.toJson(new JsonResponse<>(401, "token令牌已过期", null)));
                        return false;
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }

            } else {
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(JsonUtil.toJson(new JsonResponse<>(401, "token令牌无效", null)));
                return false;
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {

    }

}
