package com.streama.utils;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;

public class StringTools {
    public static boolean isEmpty(String str) {
        if (null == str || str.isEmpty() || "null".equals(str) || "\u0000".equals(str)) {
            return true;
        } else return str.trim().isEmpty();
    }

    public static String upperCaseFirstLetter(String field) {
        if(isEmpty(field)) {
            return field;
        }
        return field.substring(0, 1).toUpperCase() + field.substring(1);
    }

    public static String getRandomString(Integer length) {
        return RandomStringUtils.random(length, true, true);
    }

    public static String getRandomNumber(Integer length) {
        return RandomStringUtils.random(length, false, true);
    }

    public static String encodeByMd5(String originString) {
        return StringTools.isEmpty(originString) ? null : DigestUtils.md5Hex(originString);
    }

    public static boolean pathIsOk(String path) {
        if(StringTools.isEmpty(path)) {
            return true;
        }
        if(path.contains("../") || path.contains("..\\")) {
            return false;
        }
        return true;
    }

    public static String getFileSuffix(String fileName) {
        if(StringTools.isEmpty(fileName) || !fileName.contains(".")) {
            return null;
        }
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        return suffix;
    }
}
