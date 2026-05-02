package com.streama.utils;

import com.alibaba.fastjson2.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class JsonUtils {
    private static final Logger logger = LoggerFactory.getLogger(JsonUtils.class);

    public static String convertObj2Json(Object obj) {
        return JSON.toJSONString(obj);
    }

    public static <T> T convertJson2Obj(String json, Class<T> clazz) {
        return JSON.parseObject(json, clazz);
    }

    public static <T> List<T> convertJsonArray2List(String json, Class<T> clazz) {
        return JSON.parseArray(json, clazz);
    }
}
