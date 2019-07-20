package com.yunweibang.auth.model;



import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;

public class ValidateEmailDTO {
	
	

	@NotBlank(message="请输入账户名")
	 private String account;
	 
	 @NotBlank(message="请输入email")
	 @Email
	 private String email;
	 
	 @NotBlank(message="请输入邮箱验证码code")
	 private String code;
	 
	public String getAccount() {
		return account;
	}
	public void setAccount(String account) {
		this.account = account;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	
	 
	 
}
