package com.yunweibang.auth.exceptions;


import javax.security.auth.login.LoginException;

public class DynamicCodeExpiredException extends LoginException  {


	private static final long serialVersionUID = 1L;

	public DynamicCodeExpiredException() {
		super();

	}

	public DynamicCodeExpiredException(String msg) {
		super(msg);

	}
    
}
