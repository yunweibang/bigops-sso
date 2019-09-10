package com.yunweibang.auth.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yunweibang.auth.exceptions.LDAPSearchMoreUserException;

public class LDAPUtil {

	private static final String FACTORY = "com.sun.jndi.ldap.LdapCtxFactory";
	private static LdapContext ctx = null;
	private static final Control[] connCtls = null;

	private static Logger logger = LoggerFactory.getLogger(LDAPUtil.class);

	// 登录ldap
	public static boolean user_connect(String basedn, String username, String userpass, String url) {

		logger.info("ldap connect url:" + url);

		boolean flag = false;

		if (!"/".equals(url.substring(url.length() - 2, url.length() - 1))) {
			url = url + "/";
		}

		Hashtable<String, String> env = new Hashtable<String, String>();
		env.put(Context.INITIAL_CONTEXT_FACTORY, FACTORY);
		env.put(Context.PROVIDER_URL, url + basedn);
		env.put(Context.SECURITY_AUTHENTICATION, "simple");

		env.put(Context.SECURITY_PRINCIPAL, username); // 账号
		env.put(Context.SECURITY_CREDENTIALS, userpass); // 密码
		env.put("java.naming.ldap.attributes.binary", "objectGUID");
		env.put("java.naming.ldap.attributes.binary", "objectSid");
		try {
			ctx = new InitialLdapContext(env, connCtls);
			flag = true;
		} catch (javax.naming.AuthenticationException e) {
			logger.info("user connect fail {}", e.getMessage());
		} catch (Exception e) {
			logger.error("user connect error {}", e.getMessage());
		}

		return flag;
	}

	// 关闭连接
	public static void closeContext() {

		if (ctx != null) {
			try {
				ctx.close();
			} catch (NamingException e) {
				e.printStackTrace();
			}

		}
	}

	// 通过 filter 寻找用户信息
	public static Map<String, Object> getUser(String name, String filter) throws LDAPSearchMoreUserException {

		logger.info("enter getUser()");

		Map<String, Object> map = new HashMap<>();

		int flag = 0;

		try {

			SearchControls constraints = new SearchControls();
			constraints.setSearchScope(SearchControls.SUBTREE_SCOPE);

			NamingEnumeration<SearchResult> en = ctx.search("", filter + "=" + name, constraints);

			if (en == null || !en.hasMoreElements()) {

				logger.info("not found user");
			}
			// maybe more than one element

			while (en != null && en.hasMoreElements()) {

				Object obj = en.nextElement();

				if (obj instanceof SearchResult) {

					flag = flag + 1;

					SearchResult si = (SearchResult) obj;

					map.put("dn", si.getNameInNamespace());

					Attributes ab = si.getAttributes();

					NamingEnumeration<? extends Attribute> ne = ab.getAll();

					while (ne.hasMore()) {
						Attribute a = ne.next();
						try {
							if ("objectGUID".equals(a.getID()) && a.get() != null) {
								byte[] GUID = objectToByteArray(a.get());
								String strGUID = "";
								String byteGUID = "";
								// Convert the GUID into string using the byte format
								for (int c = 0; c < GUID.length; c++) {
									byteGUID = byteGUID + "\\" + AddLeadingZero((int) GUID[c] & 0xFF);
								}
								strGUID = "{";
								strGUID = strGUID + AddLeadingZero((int) GUID[3] & 0xFF);
								strGUID = strGUID + AddLeadingZero((int) GUID[2] & 0xFF);
								strGUID = strGUID + AddLeadingZero((int) GUID[1] & 0xFF);
								strGUID = strGUID + AddLeadingZero((int) GUID[0] & 0xFF);
								strGUID = strGUID + "-";
								strGUID = strGUID + AddLeadingZero((int) GUID[5] & 0xFF);
								strGUID = strGUID + AddLeadingZero((int) GUID[4] & 0xFF);
								strGUID = strGUID + "-";
								strGUID = strGUID + AddLeadingZero((int) GUID[7] & 0xFF);
								strGUID = strGUID + AddLeadingZero((int) GUID[6] & 0xFF);
								strGUID = strGUID + "-";
								strGUID = strGUID + AddLeadingZero((int) GUID[8] & 0xFF);
								strGUID = strGUID + AddLeadingZero((int) GUID[9] & 0xFF);
								strGUID = strGUID + "-";
								strGUID = strGUID + AddLeadingZero((int) GUID[10] & 0xFF);
								strGUID = strGUID + AddLeadingZero((int) GUID[11] & 0xFF);
								strGUID = strGUID + AddLeadingZero((int) GUID[12] & 0xFF);
								strGUID = strGUID + AddLeadingZero((int) GUID[13] & 0xFF);
								strGUID = strGUID + AddLeadingZero((int) GUID[14] & 0xFF);
								strGUID = strGUID + AddLeadingZero((int) GUID[15] & 0xFF);
								strGUID = strGUID + "}";
								map.put(a.getID(), strGUID);
							} else if ("objectSid".equals(a.getID()) && a.get() != null) {

								// byte[] SID = objectToByteArray(a.get());
								byte[] SID = (byte[]) a.get();
								// byte[]SID=object.toString().getBytes();//不可以用这种转换类型，有损耗
								StringBuilder strSID = new StringBuilder("S-");
								strSID.append(SID[0]).append('-');
								// bytes[2..7]:
								StringBuilder tmpBuff = new StringBuilder();
								for (int t = 2; t <= 7; t++) {
									String hexString = Integer.toHexString(SID[t] & 0xFF);
									tmpBuff.append(hexString);
								}
								strSID.append(Long.parseLong(tmpBuff.toString(), 16));
								// bytes[1]:thesubauthoritiescount
								int count = SID[1];
								// bytes[8..end]:thesubauthorities(theseareIntegers-notice
								// theendian)
								for (int i = 0; i < count; i++) {
									int currSubAuthOffset = i * 4;
									tmpBuff.setLength(0);
									tmpBuff.append(String.format("%02X%02X%02X%02X",
											(SID[11 + currSubAuthOffset] & 0xFF), (SID[10 + currSubAuthOffset] & 0xFF),
											(SID[9 + currSubAuthOffset] & 0xFF), (SID[8 + currSubAuthOffset] & 0xFF)));
									strSID.append('-').append(Long.parseLong(tmpBuff.toString(), 16));
								}
								map.put(a.getID(), strSID.toString());
							} else {
								map.put(a.getID(), a.get());
							}
						} catch (Exception e) {
							e.printStackTrace();
						} finally {
							closeContext();
						}

					}
				}
			}
		} catch (Exception e) {

			logger.error("getUser error {}", e.getMessage());
		}

		if (flag > 1) {
			throw new LDAPSearchMoreUserException();
		}

		return map;
	}

	// ssl登录ldap
	public static boolean user_sslconnect(String basedn, String username, String userpass, String url) {

		logger.info("ldap connect url:" + url);

		boolean flag = false;

		if (!"/".equals(url.substring(url.length() - 2, url.length() - 1))) {
			url = url + "/";
		}

		Hashtable<String, String> env = new Hashtable<String, String>();
		env.put(Context.INITIAL_CONTEXT_FACTORY, FACTORY);
		env.put(Context.PROVIDER_URL, url + basedn);
		env.put(Context.SECURITY_AUTHENTICATION, "simple");

		env.put("java.naming.ldap.factory.socket", "com.yunweibang.bigops.util.LTSSSLSocketFactory"); // ssl忽略certificate（证书）验证
		env.put(Context.SECURITY_PROTOCOL, "ssl"); // 启用 ldap ssl 验证

		env.put(Context.SECURITY_PRINCIPAL, username); // 账号
		env.put(Context.SECURITY_CREDENTIALS, userpass); // 密码
		env.put("java.naming.ldap.attributes.binary", "objectGUID");
		env.put("java.naming.ldap.attributes.binary", "objectSid");
		try {
			System.setProperty("com.sun.jndi.ldap.object.disableEndpointIdentification", "true");
			ctx = new InitialLdapContext(env, connCtls);
			System.out.println("连接成功");
			flag = true;
		} catch (javax.naming.AuthenticationException e) {
			System.out.println("连接失败");
			logger.info("user connect fail {}", e.getMessage());
		} catch (Exception e) {
			System.out.println("连接异常");
			logger.error("user connect error {}", e.getMessage());
		}

		return flag;
	}

	public static String AddLeadingZero(int k) {
		return (k <= 0xF) ? "0" + Integer.toHexString(k) : Integer.toHexString(k);
	}

	public static byte[] objectToByteArray(Object obj) {
		byte[] bytes = null;
		ByteArrayOutputStream byteArrayOutputStream = null;
		ObjectOutputStream objectOutputStream = null;
		try {
			byteArrayOutputStream = new ByteArrayOutputStream();
			objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
			objectOutputStream.writeObject(obj);
			objectOutputStream.flush();
			bytes = byteArrayOutputStream.toByteArray();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (objectOutputStream != null) {
				try {
					objectOutputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (byteArrayOutputStream != null) {
				try {
					byteArrayOutputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return bytes;
	}

}
