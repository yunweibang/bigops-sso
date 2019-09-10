package com.yunweibang.auth.exceptions;

import javax.security.auth.login.LoginException;

public class LDAPSearchMoreUserException extends LoginException {

	private static final long serialVersionUID = 1L;

	public LDAPSearchMoreUserException() {
		super();

	}

	public LDAPSearchMoreUserException(String msg) {
		super(msg);

	}

}
