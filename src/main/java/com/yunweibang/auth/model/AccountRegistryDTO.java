package com.yunweibang.auth.model;



import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;

public class AccountRegistryDTO {


	 @NotBlank(message="请输入姓名")
	 private String name;
	 @NotBlank(message="请输入账户名")
	 private String account;
	 @NotBlank(message="请输入密码")
	 private String pass;
	 
	 @NotBlank(message="请输入email")
	 @Email
	 private String email;
	 
	 @NotBlank(message="请输入图形验证码")
	 private String imageCode;

	 public String getPass() {
		return pass;
	}

	 public void setPass(String pass) {
		this.pass = pass;
	}


	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

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

	public String getImageCode() {
		return imageCode;
	}

	public void setImageCode(String imageCode) {
		this.imageCode = imageCode;
	}
}
