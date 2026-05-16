package com.healthagent.common;

/**
 * 用户意图类型枚举
 */
public enum IntentType {

    /**
     * 查询保单
     */
    QUERY_POLICY("query_policy", "查询保单"),

    /**
     * 体检预约
     */
    BOOK_EXAMINATION("book_examination", "体检预约"),

    /**
     * 查询预约记录
     */
    QUERY_BOOKING("query_booking", "查询预约记录"),

    /**
     * 健康咨询
     */
    HEALTH_CONSULTATION("health_consultation", "健康咨询"),

    /**
     * 一般对话
     */
    GENERAL_CONVERSATION("general_conversation", "一般对话");

    private final String code;
    private final String desc;

    IntentType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static IntentType fromCode(String code) {
        if (code == null) {
            return GENERAL_CONVERSATION;
        }
        for (IntentType type : values()) {
            if (type.code.equalsIgnoreCase(code) || code.contains(type.code)) {
                return type;
            }
        }
        return GENERAL_CONVERSATION;
    }
}
