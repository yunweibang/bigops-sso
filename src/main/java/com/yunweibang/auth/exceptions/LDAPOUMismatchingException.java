package com.yunweibang.auth.exceptions;

import javax.security.auth.login.LoginException;

public class LDAPOUMismatchingException extends LoginException {

	private static final long serialVersionUID = 1L;

	public LDAPOUMismatchingException() {
		super();

	}

	public LDAPOUMismatchingException(String msg) {
		super(msg);

	}

}
