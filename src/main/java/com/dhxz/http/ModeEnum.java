package com.dhxz.http;

import org.omg.CORBA.PRIVATE_MEMBER;

/**
 * @author 10066610
 * @desaciption todo
 */
public enum  ModeEnum {
    DAILY("日排行","daily");
    
    private String message;
    private String value;
    
    public String getMessage() {
        return message;
    }
    
    public String getValue() {
        return value;
    }
    
    ModeEnum(String message, String value) {
        this.message = message;
        this.value = value;
    }
}
