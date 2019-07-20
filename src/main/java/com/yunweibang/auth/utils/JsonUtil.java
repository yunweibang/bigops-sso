package com.yunweibang.auth.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;




/**
 *  JSON工具
 *  @author lpp
 *  @since 2017-08-22 15:19:11
 */
public class JsonUtil {

    private static Logger logger = LoggerFactory.getLogger(JsonUtil.class);

    private static final SerializerFeature[] features = {SerializerFeature.WriteMapNullValue, // 输出空置字段
            SerializerFeature.WriteNullListAsEmpty, // list字段如果为null，输出为[]，而不是null
            SerializerFeature.WriteNullNumberAsZero, // 数值字段如果为null，输出为0，而不是null
            SerializerFeature.WriteNullBooleanAsFalse, // Boolean字段如果为null，输出为false，而不是null
            SerializerFeature.WriteNullStringAsEmpty // 字符类型字段如果为null，输出为""，而不是null
    };

    /**
     * 对象转成json字符串
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        return JSON.toJSONString(obj);
    }

    /**
     * 对象转成带null的json字符串
     */
    public static String toJsonWithNull(Object obj) {
        if (obj == null) {
            return null;
        }
        return JSON.toJSONString(obj, SerializerFeature.WriteMapNullValue);
    }

    /**
     * json字符串转成对象
     */
    public static Object fromJson(String text) {
        return JSON.parse(text);
    }

    /**
     * json字符串转成对象
     */
    public static <T> T fromJson(String text, Class<T> clazz) {
        return JSON.parseObject(text, clazz);
    }


}
