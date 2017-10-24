package com.commune.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;

public class Util {
    //生成一个特定长度的随机字符串
    public static String generateRandomString(int length) {
        if (length <=0) return "";

        StringBuilder sb = new StringBuilder(length);
        for (int i=0; i<length; i++) {
            char c = (char)(' ' + Math.random()*95);
            sb.append(c);
        }
        return sb.toString();
    }

    //生成SHA-1
    public static String getSHA1(String str){
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
            messageDigest.update(str.getBytes());
            byte[] bytes = messageDigest.digest();

            int length = bytes.length;
            StringBuilder sb = new StringBuilder();
            for (byte b: bytes) {
                String hexString = Integer.toHexString(0xff & b);
                if (hexString.length() == 1) sb.append("0");
                sb.append(hexString);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
            return str;
        }
    }

    public static String getHumanReadableFileLength (long length) {
        double len = length;

        int i;
        String[] units = {"字节", "KB", "MB", "GB"};
        for (i=0; i<4; i++) {
            if (len/1024 <= 1) break;
            len/=1024;
        }
        DecimalFormat format = new DecimalFormat("#.##");
        if (i==0)
            return String.valueOf(length) + " " + units[i];
        else
            return String.format("%.1f", len) + " " + units[i];
    }

}
