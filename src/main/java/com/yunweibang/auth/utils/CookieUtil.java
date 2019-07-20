package com.yunweibang.auth.utils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * @author lpp
 */
public class CookieUtil {
    /**
     * 将cookie数组中Cookie对象放入Map中
     *
     * @param cookieArr Cookie对象集合
     * @return Map 数组
     */
    public static Map getCookieMap(Cookie[] cookieArr) {
        Map cookieMap = new HashMap();
        if (cookieArr == null) {
            return cookieMap;
        }
        for (Cookie ck : cookieArr) {
            cookieMap.put(ck.getName(), ck.getValue());
        }
        return cookieMap;
    }

    public static String getCookieValue(HttpServletRequest request, String key) {
        Cookie[] cookieArr = request.getCookies();
        if (cookieArr == null || cookieArr.length <= 0) return null;
        for (Cookie ck : cookieArr) {
            if (key.equals(ck.getName())) return ck.getValue();
        }
        return null;
    }
}
