package com.yunweibang.auth.exceptions;


import javax.security.auth.login.LoginException;


public class TooManyCountException extends LoginException  {

	
	private static final long serialVersionUID = 1L;

	public TooManyCountException() {
		super();

	}

	public TooManyCountException(String msg) {
		super(msg);

	}
    
}
