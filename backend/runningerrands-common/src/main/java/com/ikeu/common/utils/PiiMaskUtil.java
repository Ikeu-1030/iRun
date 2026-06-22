package com.ikeu.common.utils;

/**
 * PII 脱敏工具类，对手机号、姓名、学号等个人信息进行部分隐藏。
 * 管理端非超管角色查看用户数据时使用。
 *
 * @author ikeu
 * @since 2026/06/21
 */
public final class PiiMaskUtil {

    private PiiMaskUtil() {}

    /** 手机号脱敏：保留前3后4，中间4位替换为**** */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone == null ? null : phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /** 姓名脱敏：保留首字，其余替换为* */
    public static String maskRealName(String name) {
        if (name == null || name.isEmpty()) return name;
        if (name.length() == 1) return "*";
        StringBuilder sb = new StringBuilder();
        sb.append(name.charAt(0));
        for (int i = 1; i < name.length(); i++) sb.append('*');
        return sb.toString();
    }

    /** 学号脱敏：保留后3位，其余替换为**** */
    public static String maskStudentId(String studentId) {
        if (studentId == null || studentId.length() < 4) return studentId == null ? null : studentId;
        return "****" + studentId.substring(studentId.length() - 3);
    }
}
