package com.yunweibang.auth.exceptions;

import javax.security.auth.login.LoginException;

public class LDAPNotSearchUserException extends LoginException {

	private static final long serialVersionUID = 1L;

	public LDAPNotSearchUserException() {
		super();

	}

	public LDAPNotSearchUserException(String msg) {
		super(msg);

	}

}
