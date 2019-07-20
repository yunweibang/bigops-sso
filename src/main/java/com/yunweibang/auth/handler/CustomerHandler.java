package com.yunweibang.auth.handler;

import com.alibaba.fastjson.JSONObject;
import com.yunweibang.auth.exceptions.AccountDisabledOrExpiredException;
import com.yunweibang.auth.exceptions.TooManyCountException;
import com.yunweibang.auth.service.UserService;
import com.yunweibang.auth.utils.*;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ArrayListHandler;
import org.apache.commons.dbutils.handlers.MapHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.apache.commons.lang3.StringUtils;
import org.apereo.cas.authentication.*;
import org.apereo.cas.authentication.exceptions.AccountDisabledException;
import org.apereo.cas.authentication.handler.support.AbstractPreAndPostProcessingAuthenticationHandler;
import org.apereo.cas.authentication.principal.PrincipalFactory;
import org.apereo.cas.services.ServicesManager;
import org.apereo.inspektr.common.web.ClientInfo;
import org.apereo.inspektr.common.web.ClientInfoHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.AccountLockedException;
import javax.security.auth.login.FailedLoginException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class CustomerHandler extends AbstractPreAndPostProcessingAuthenticationHandler {

    private static Logger logger = LoggerFactory.getLogger(CustomerHandler.class);
    public static final String MYSQL_LOGIN = "mysql";
    public static final String LDAP_LOGIN = "ldap";
    public static final String LDAPS_LOGIN = "ldaps";

    public CustomerHandler(String name, ServicesManager servicesManager, PrincipalFactory principalFactory,
                           Integer order) {
        super(name, servicesManager, principalFactory, order);
    }

    @SuppressWarnings({"deprecation", "unused"})
    protected AuthenticationHandlerExecutionResult doAuthentication(Credential credential)
            throws GeneralSecurityException, PreventedException {
        UsernamePasswordCredential usernamePasswordCredentia = (UsernamePasswordCredential) credential;

        String username = usernamePasswordCredentia.getUsername();
        String password = usernamePasswordCredentia.getPassword();
        String address = null;
        ClientInfo clientInfo = ClientInfoHolder.getClientInfo();
        logger.info("clientInfo=" + JsonUtil.toJson(clientInfo));
        TxQueryRunner tx = new TxQueryRunner();
        String logsql = " insert into log_login(account,status,operation,content,login_type,ip,city,ctime)  values(?,?,?,?,?,?,?,?) ";
        address = getCityAddress(clientInfo, address);
        if (StringUtils.isBlank(username)) {
            throw new AccountDisabledException();
        } else if (StringUtils.isBlank(password)) {
            throw new AccountLockedException();

        } else {
            QueryRunner qr = new QueryRunner(JdbcUtils.getDs());
            String logloginsql = "select ctime from log_login where ctime > ? and status=? and account =? order by ctime desc ";
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY) - 6);
            Date lastdate = calendar.getTime();
            Object[] logloginparams = new Object[]{lastdate, Constants.LOGIN_STATUS_FAIL, username};
            List<Map<String, Object>> counts = null;
            try {
                counts = qr.query(logloginsql, logloginparams, new MapListHandler());
                if (counts != null) {
                    SimpleDateFormat sdfTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    boolean flag = counts.size() >= 5 && new Date().getTime() - sdfTime.parse(counts.get(0).get("ctime").toString()).getTime() < 10 * 60 * 1000;
                    logger.info("loglogin flag= {}", flag);
                    if (flag) {
                        UserService userservice = new UserService();
                        String title = "安全警告通知";
                        String msg = "已连续5次输错登录密码，账号已被锁定10分钟。";
                        userservice.sendMessageToUser(username, title, msg);
                        throw new TooManyCountException();
                    }
                }
            } catch (SQLException e2) {
                e2.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }
            String pwd = BCrypt.hashpw(password, BCrypt.gensalt());

            Map<String, Object> mapldap = null;

            // 查询sso auth
            String sqlldap = "select * from sso_auth where type = 'ldap'";

            try {
                mapldap = qr.query(sqlldap, new MapHandler());
            } catch (SQLException e1) {
                logger.error(e1.getMessage());
            }

            boolean flagldap = (Boolean) mapldap.get("is_used");// 判断是否启用ldap认证
            boolean flagssl = (Boolean) mapldap.get("use_ssl");// 判断是否启用ldaps认证

            logger.info("//==========ldap state is :" + flagldap);
            logger.info("//==========ssl ldap state is :" + flagssl);
            boolean isldaps = false;
            try {

                if (!"admin".equals(username) && flagldap) {

                    // 获取连接ldap信息
                    String ldapServer = (String) mapldap.get("ldap_server");
                    String ldapPort = (String) mapldap.get("ldap_port");
                    String filter = (String) mapldap.get("user_filter");
                    String baseDn = (String) mapldap.get("base_dn");
                    String mapMysql = (String) mapldap.get("map_mysql");
                    String bindUser = (String) mapldap.get("bind_user");
                    String bindPass = (String) mapldap.get("bind_pass");
                    String userOu = (String) mapldap.get("user_ou");

                    String url = "ldap://" + ldapServer + ":" + ldapPort;

                    if (flagssl) { // 启用 ldapssl
                        isldaps = true;
                        url = "ldaps://" + ldapServer + ":" + ldapPort;
                    }

                    Map<String, Object> mapmysql = JSONObject.parseObject(mapMysql); // mapMysql 转化成map

                    Set<String> setmysql = mapmysql.keySet(); // 获取map的键

                    boolean adminConnstate = false; // admin 登录状态
                    boolean userConnstate = false; // user 登录状态

                    Map<String, Object> map = new HashMap<>();

                    if (flagssl) { // 启用 ldapssl

                        adminConnstate = LDAPUtil.admin_sslconnect(baseDn, bindUser, bindPass, url);
                        logger.info("//==========admin ldap ssl connect state is :" + adminConnstate);
                    } else {

                        adminConnstate = LDAPUtil.admin_connect(baseDn, bindUser, bindPass, url);
                        logger.info("//==========admin ldap connect state is :" + adminConnstate);
                    }

                    if (adminConnstate) { // 管理员登录成功

                        map = LDAPUtil.getUser(username, filter); // 管理员获取登录用户的信息

                        LDAPUtil.closeContext();
                        if (map == null || map.isEmpty()) {
                            logger.info("LDAPUtil.getUser is empty");
                            if (isldaps) {
                                insertfailurelog(username + "(未知)", clientInfo, "用户不存在,登录失败", logsql, tx, address, LDAPS_LOGIN);
                            } else {
                                insertfailurelog(username + "(未知)", clientInfo, "用户不存在,登录失败", logsql, tx, address, LDAP_LOGIN);
                            }
                            throw new FailedLoginException();
                        }
                        String userdn = "";
                        String userbaseDn = ""; // 获取用户完整baseDn

                        String[] dnArray = null;

                        if (map.get("entryDN") != null) {

                            userdn = (String) map.get("entryDN");

                            dnArray = userdn.split(",");
                        }

                        if (map.get("distinguishedName") != null) {

                            userdn = (String) map.get("distinguishedName");

                            dnArray = userdn.split(",");
                        }

                        for (int i = 0; i < dnArray.length; i++) {

                            if (!dnArray[i].contains(filter)) {
                                userbaseDn = userbaseDn + dnArray[i] + ",";
                            }

                        }
                        userbaseDn = userbaseDn.substring(0, userbaseDn.length() - 1);

                        //验证是否在ou范围内，不是则不让登陆
                        boolean ouFlag = true;

                        if (userOu.contains("||")) {

                            String[] userOus = userOu.split("\\|\\|");

                            for (String ou : userOus) {

                                ouFlag = true;

                                String distinguishedName = map.get("distinguishedName").toString().toLowerCase();
                                ou = ou.toLowerCase();

                                distinguishedName = distinguishedName.replaceAll(" ", "");
                                ou = ou.replaceAll(" ", "");

                                if (ou.length() > distinguishedName.length()) {

                                    ouFlag = false;
                                }

                                int j = distinguishedName.length() - 1;

                                for (int i = ou.length() - 1; i >= 0; i--) {

                                    if (distinguishedName.charAt(j) != ou.charAt(i)) {

                                        ouFlag = false;
                                    }

                                    j--;
                                }

                                if (ouFlag) {

                                    break;
                                }

                            }

                        } else {

                            String distinguishedName = map.get("distinguishedName").toString().toLowerCase();
                            userOu = userOu.toLowerCase();

                            distinguishedName = distinguishedName.replaceAll(" ", "");
                            userOu = userOu.replaceAll(" ", "");

                            if (userOu.length() > distinguishedName.length()) {

                                ouFlag = false;
                            }

                            int j = distinguishedName.length() - 1;

                            for (int i = userOu.length() - 1; i >= 0; i--) {

                                if (distinguishedName.charAt(j) != userOu.charAt(i)) {

                                    ouFlag = false;
                                }

                                j--;
                            }

                        }

                        if (!ouFlag) {

                            logger.error("user 不在 userOu的范围内，禁止登录！  user:" + map.get("distinguishedName") + "; userOu:" + userOu);
                            throw new RuntimeException("user 不在 userOu的范围内，禁止登录！  user:" + map.get("distinguishedName") + "; userOu:" + userOu);
                        }

                        if (flagssl) { // 启用 ldapssl

                            userConnstate = LDAPUtil.user_sslconnect(userbaseDn, userdn, password, url); // 用户登录 ldapssl
                            logger.info("//==========user ldap ssl connect state is :" + userConnstate);
                        } else {

                            userConnstate = LDAPUtil.user_connect(userbaseDn, userdn, password, url); // 用户登录 ldap
                            logger.info("//==========user ldap connect state is :" + userConnstate);
                        }

                        if (userConnstate) { // 用户登录成功

                            LDAPUtil.closeContext(); // 关闭连接

                            // 查询用户
                            String sqluser = "select account from ri_user where account = '" + username + "'";

                            Map<String, Object> mapuser = qr.query(sqluser, new MapHandler());

                            // 查询表字段
                            String sqlcolumn = "select COLUMN_NAME from INFORMATION_SCHEMA.Columns where table_name='ri_user' and table_schema='bigops'";

                            List<Object[]> list = qr.query(sqlcolumn, new ArrayListHandler());

                            Map<String, Object> mapdb = new HashMap<>();

                            for (int i = 0; i < list.size(); i++) { // 将映射mysql键值都存到List中
                                for (String s : setmysql) {
                                    if (list.get(i)[0].equals(mapmysql.get(s))) {
                                        mapdb.put(String.valueOf(mapmysql.get(s)), map.get(s));
                                    }
                                }
                            }
                            List<Object> params = new ArrayList<Object>();

                            Set<String> setupdate = mapdb.keySet();

                            List<MessageDescriptor> mlist = new ArrayList<>();

                            if (mapuser != null && mapuser.size() > 0) { // 判断 用户在mysql中存在

                                String sqlupdate = "update ri_user set ";

                                for (String s : setupdate) {
                                    if (mapdb.get(s) != null && !"".equals(String.valueOf(mapdb.get(s)))) {
                                        sqlupdate = sqlupdate + s + "=?,";
                                        params.add(mapdb.get(s));
                                    }
                                }

                                sqlupdate = sqlupdate + "pass =? where account=?";
                                params.add(pwd);
                                params.add(username);

                                if (qr.update(sqlupdate, params.toArray()) > 0) {
                                    if (isldaps) {
                                        insertldapSuccesslog(clientInfo, username, address, logsql, tx, LDAPS_LOGIN, Constants.LOGIN_SUCCESS);
                                    } else {
                                        insertldapSuccesslog(clientInfo, username, address, logsql, tx, LDAP_LOGIN, Constants.LOGIN_SUCCESS);
                                    }
                                    return createHandlerResult(credential,
                                            this.principalFactory.createPrincipal(username, mapdb), mlist);
                                }
                            } else { // 不存在

                                String sqlinsert = "insert into ri_user (";
                                boolean accountExpiresFlag = true;
                                for (String s : setupdate) {
                                    if (mapdb.get(s) != null && !"".equals(String.valueOf(mapdb.get(s)))) {
                                        sqlinsert = sqlinsert + s + ",";
                                        if ("etime".equals(s)) {
                                            params.add(Constants.FT.format(DateUtil.fromDnetToJdate(mapdb.get(s).toString())));
                                        } else {
                                            params.add(mapdb.get(s));
                                        }
                                    }
                                    if ("etime".equals(s)) {
                                        accountExpiresFlag = false;
                                    }
                                }

                                if (accountExpiresFlag) {
                                    sqlinsert = sqlinsert + "etime,";
                                    params.add(Constants.FT.parse("9999-01-01 01:01:01"));
                                }
                                sqlinsert = sqlinsert + "pass,status) values (";
                                for (String s : setupdate) {
                                    if (mapdb.get(s) != null && !"".equals(String.valueOf(mapdb.get(s)))) {
                                        sqlinsert = sqlinsert + "?,";
                                    }
                                }
                                if (accountExpiresFlag) {
                                    sqlinsert = sqlinsert + "?,";
                                }

                                sqlinsert = sqlinsert + "?,?)";
                                params.add(pwd);
                                params.add("启用");

                                if (qr.update(sqlinsert, params.toArray()) > 0) {
                                    if (isldaps) {
                                        insertldapSuccesslog(clientInfo, username, address, logsql, tx, LDAPS_LOGIN, Constants.LOGIN_SUCCESS);
                                    } else {
                                        insertldapSuccesslog(clientInfo, username, address, logsql, tx, LDAP_LOGIN, Constants.LOGIN_SUCCESS);
                                    }
                                    logger.info("// ldap================" + username + JsonUtil.toJson(mapdb));
                                    return createHandlerResult(credential,
                                            this.principalFactory.createPrincipal(username, mapdb), mlist);
                                }
                            }

                        } else {

                            logger.info("//========user connect faild ");
                            if (isldaps) {
                                insertfailurelog(username, clientInfo, Constants.LOGIN_FAIL, logsql, tx, address, LDAPS_LOGIN);
                            } else {
                                insertfailurelog(username, clientInfo, Constants.LOGIN_FAIL, logsql, tx, address, LDAP_LOGIN);
                            }
                            throw new FailedLoginException();
                        }

                    } else {

                        logger.info("//========admin connect faild ");
                        if (isldaps) {
                            insertfailurelog(username, clientInfo, Constants.LOGIN_FAIL, logsql, tx, address, LDAPS_LOGIN);
                        } else {
                            insertfailurelog(username, clientInfo, Constants.LOGIN_FAIL, logsql, tx, address, LDAP_LOGIN);
                        }
                        throw new FailedLoginException();
                    }

                }
            } catch (Exception e) {
                logger.error("sql erroe :" + e.toString());
            }

            String usersql = "select * from ri_user where account = ?";
            Object[] userparams = new Object[]{username};
            Map<String, Object> usermap = null;
            try {
                usermap = qr.query(usersql, new MapHandler(), userparams);
                if (usermap != null && usermap.containsKey("id")) {
                    System.out.println("usermap=" + usermap);
                    Date etime = null;

                    if (usermap.get("etime") != null) {
                        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                        etime = format.parse(usermap.get("etime").toString());
                    }
                    if (!"启用".equals((String) usermap.get("status")) || (etime != null && etime.getTime() < new Date().getTime())) {
                        throw new AccountDisabledOrExpiredException();
                    }
                    if (BCrypt.checkpw(password, String.valueOf(usermap.get("pass")))) {

                        Map<String, Object> result = new HashMap<String, Object>();

                        Set<String> sets = (Set<String>) usermap.keySet();

                        Set<String> principalAttributesets = PrincipalAttributeUtils.getPrincipalAttributes();

                        boolean b = sets.containsAll(principalAttributesets);

                        if (b) {
                            for (String value : principalAttributesets) {
                                result.put(value, usermap.get(value));
                            }
                        } else {
                            throw new RuntimeException(" cas.principal.attributes  属性错误");
                        }
                        logger.info("result  attributes" + JsonUtil.toJson(result));
                        Date d = new Date();

                        Object[] logparams = new Object[]{usermap.get("account").toString(), Constants.LOGIN_STATUS_SUCCESS, Constants.LOGIN, Constants.LOGIN_SUCCESS, MYSQL_LOGIN,
                                clientInfo.getClientIpAddress(), address, d};
                        try {
                            int logresult = tx.update(logsql, logparams);
                            if (logresult <= 0) {
                                throw new RuntimeException("插入登录日志失败");
                            }
                        } catch (Exception ex) {
                            throw new RuntimeException(ex);
                        }
                        // 允许登录，并且通过this.principalFactory.createPrincipal 来返回用户属性
                        List<MessageDescriptor> list = new ArrayList<>();
                        return createHandlerResult(credential, this.principalFactory.createPrincipal(username, result),
                                list);
                    }
                    insertfailurelog(username, clientInfo, Constants.LOGIN_FAIL, logsql, tx, address, MYSQL_LOGIN);
                    throw new FailedLoginException();
                } else {
                    insertfailurelog(username + "(未知)", clientInfo, Constants.LOGIN_FAIL, logsql, tx, address, MYSQL_LOGIN);
                    throw new FailedLoginException();
                }
            } catch (AccountDisabledOrExpiredException e1) {
                e1.printStackTrace();
                throw new AccountDisabledOrExpiredException();
            } catch (FailedLoginException e2) {
                e2.printStackTrace();
                throw new FailedLoginException();
            } catch (Exception e) {
                e.printStackTrace();
            }
            insertfailurelog(username, clientInfo, Constants.LOGIN_FAIL, logsql, tx, address, "未知");
            throw new FailedLoginException();

        }

    }

    public boolean supports(Credential credential) {
        return credential instanceof UsernamePasswordCredential;
    }

    public String getCityAddress(ClientInfo clientInfo, String address) {
        QQWry qqwry = null;
        try {
            qqwry = new QQWry();
        } catch (IOException e) {
            e.printStackTrace();
            return address;
        }

        final IPZone zone = qqwry.findIP(clientInfo.getClientIpAddress());
        return zone.getMainInfo();
    }

    public void insertfailurelog(String username, ClientInfo clientInfo, String content, String logsql, TxQueryRunner tx,
                                 String address, String loginType) {
        Date d = new Date();
        address = getCityAddress(clientInfo, address);
        Object[] logparams = new Object[]{username, Constants.LOGIN_STATUS_FAIL, Constants.LOGIN, content, loginType, clientInfo.getClientIpAddress(), address, d};
        try {
            tx.update(logsql, logparams);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void insertldapSuccesslog(ClientInfo clientInfo, String username, String address, String logsql,
                                     TxQueryRunner tx, String loginType, String content) {
        Date d = new Date();
        Object[] logparams = new Object[]{username, Constants.LOGIN_STATUS_SUCCESS, Constants.LOGIN, content, loginType, clientInfo.getClientIpAddress(), address, d};

        try {
            int logresult = tx.update(logsql, logparams);
            if (logresult <= 0) {
                throw new RuntimeException("插入登录日志失败");
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

}
