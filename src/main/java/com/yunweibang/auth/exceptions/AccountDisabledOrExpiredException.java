package com.yunweibang.auth.exceptions;


import javax.security.auth.login.LoginException;

public class AccountDisabledOrExpiredException extends LoginException  {


	private static final long serialVersionUID = 1L;

	public AccountDisabledOrExpiredException() {
		super();

	}

	public AccountDisabledOrExpiredException(String msg) {
		super(msg);

	}
    
}
