package com.yunweibang.auth.exceptions;


import javax.security.auth.login.LoginException;

public class DynamicCodeIsNullException extends LoginException  {


	private static final long serialVersionUID = 1L;

	public DynamicCodeIsNullException() {
		super();

	}

	public DynamicCodeIsNullException(String msg) {
		super(msg);

	}
    
}
