package com.alibaba.imo.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MyDateUtil {
    public static String dateFormat(Date date) {
        try {
            DateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
            String dateStr = df.format(date);
            return dateStr;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}
