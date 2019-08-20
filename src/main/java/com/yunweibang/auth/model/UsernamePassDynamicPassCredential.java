package com.yunweibang.auth.model;

import org.apereo.cas.authentication.UsernamePasswordCredential;

public class UsernamePassDynamicPassCredential extends UsernamePasswordCredential {
    private String code;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
