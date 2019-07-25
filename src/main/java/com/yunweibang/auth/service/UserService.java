package com.yunweibang.auth.service;

import com.google.common.base.Strings;
import com.yunweibang.auth.common.JsonResponse;
import com.yunweibang.auth.model.*;
import com.yunweibang.auth.utils.BCrypt;
import com.yunweibang.auth.utils.IPUtils;
import com.yunweibang.auth.utils.IPZone;
import com.yunweibang.auth.utils.ToolUtil;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

public class UserService {
    private static Logger logger = LoggerFactory.getLogger(UserService.class);

    public Map<String, Object> loadSetting(Integer id) {
        QueryRunner qr = new QueryRunner(com.yunweibang.auth.utils.JdbcUtils.getDs());
        String sql = "select * from notifications where id=" + id;
        List<Map<String, Object>> settings = null;
        try {
            settings = qr.query(sql, new MapListHandler());
        } catch (SQLException e) {
            logger.error(" select  notifications error", e);
        }

        return settings.isEmpty() ? null : settings.get(0);
    }

    private Notifications getNotifications() {
        Map<String, Object> notify = loadSetting(1);
        if (Objects.nonNull(notify) && (Boolean) notify.get("status")) {
            Notifications notifications = new Notifications();
            notifications.setHost(notify.get("host").toString());
            notifications.setUser(notify.get("user").toString());
            notifications.setPass(notify.get("pass").toString());
            notifications.setPort(Integer.parseInt(notify.get("port").toString()));
            notifications.setEnableSsl((Boolean) notify.get("enable_ssl"));
            notifications.setEnableTls((Boolean) notify.get("enable_tls"));
            notifications.setContentSign((String) notify.get("content_sign"));
            notifications.setTitleSign((String) notify.get("title_sign"));
            return notifications;
        }
        return null;
    }

    public JsonResponse<Object> sendTestEmail(HttpServletRequest request, SendEmailDTO dto) {
        String ip = IPUtils.getIpAddr(request);
        Notifications notifications = getNotifications();
        notifications.setSendTo(dto.getEmail());
        String validateCode = ToolUtil.getRandomNum(6);
        String title = "忘记密码";
        String msg = "验证码为" + validateCode + "，请在2分钟内回填。";
        if (Objects.nonNull(notifications)) {
            if (sendMailConfig(notifications, title, msg)) {
                com.yunweibang.auth.utils.TxQueryRunner tx = new com.yunweibang.auth.utils.TxQueryRunner();
                String sql = " insert into sso_find_pwd(account,email,validate_code,create_time,code_expire_time,is_success)  values(?,?,?,?,?,?) ";
                Date d = new Date();
                Calendar c = Calendar.getInstance();
                c.add(Calendar.MINUTE, 2);
                Object[] params = new Object[]{dto.getAccount(), dto.getEmail(), validateCode, d, c.getTime(), 0};
                try {
                    tx.update(sql, params);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return new JsonResponse<Object>(200, "发送邮件成功", null);
            } else {
                insertFaillog(dto.getAccount(), ip, "找密,发送邮件失败");
                return new JsonResponse<Object>(401, "发送邮件失败", null);
            }
        } else {
            insertFaillog(dto.getAccount(), ip, "找密,发送邮件失败");
            return new JsonResponse<Object>(401, "发送邮件失败", null);
        }

    }

    public JsonResponse<Object> editPasswd(String token, String password1, String password2, HttpServletRequest request) {
        String ip = IPUtils.getIpAddr(request);

        if (StringUtils.isBlank(password1) || StringUtils.isBlank(password2)) {
            return new JsonResponse<Object>(503, "密码不能为空", null);
        }
        if (!Pattern.matches("^(?![0-9]+$)(?![a-zA-Z]+$)(?![_~@#\\$]+$)[0-9A-Za-z_~@#\\$]{6,16}$", password1)) {
            return new JsonResponse<Object>(501, "长度为6-16个字符，必须包含字母、数字、符号中至少2种", null);
        }
        if (!password1.equals(password2)) {
            return new JsonResponse<Object>(502, "两次输入的密码不一致", null);
        }
        QueryRunner qr = new QueryRunner(com.yunweibang.auth.utils.JdbcUtils.getDs());
        String sql = "select * from sso_find_pwd where token= ? ";
        Object[] params = new Object[]{token};
        Map map = null;
        try {
            map = qr.query(sql, new MapHandler(), params);
            if (map != null && map.containsKey("id")) {
                logger.info("editPasswd select token success");
                com.yunweibang.auth.utils.TxQueryRunner tx = new com.yunweibang.auth.utils.TxQueryRunner();
                String updatepasssql = "update ri_user set pass=? ,mtime=? where account=? and email=?";
                String md5pwd = BCrypt.hashpw(password1, BCrypt.gensalt());
                Object[] pdatepassparams = new Object[]{md5pwd, new Date(), map.get("account").toString(),
                        map.get("email").toString()};
                try {
                    int result = tx.update(updatepasssql, pdatepassparams);
                    if (result > 0) {
                        insertSuccesslog(map.get("account").toString(), ip);
                        return new JsonResponse<Object>(200, "修改密码成功", null);
                    }
                } catch (SQLException e) {
                    logger.error(" editPasswd error", e);
                }

            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
        return new JsonResponse<Object>(401, "修改密码失败", null);
    }

    public JsonResponse<Object> validateEmail(ValidateEmailDTO dto, HttpServletRequest request) {
        String ip = IPUtils.getIpAddr(request);
        QueryRunner qr = new QueryRunner(com.yunweibang.auth.utils.JdbcUtils.getDs());
        String sql = "select * from sso_find_pwd where account=? and email=? and validate_code= ?";
        Object[] params = new Object[]{dto.getAccount(), dto.getEmail(), dto.getCode()};
        Map map = null;
        try {
            map = qr.query(sql, new MapHandler(), params);
            if (map != null && map.size() > 0 && map.containsKey("id")) {
                SimpleDateFormat sdfTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                try {

                    if (new Date().getTime() > sdfTime.parse(map.get("code_expire_time").toString()).getTime()) {
                        insertFaillog(dto.getAccount(), ip, "找密,验证码已失效");
                        return new JsonResponse<Object>(401, "验证码已失效", null);
                    }

                } catch (ParseException e) {
                    e.printStackTrace();
                }

            } else {
                insertFaillog(dto.getAccount(), ip, "找密,邮箱验证失败");
                return new JsonResponse<Object>(401, "邮箱验证失败", null);
            }

        } catch (SQLException e) {
            logger.error("select sql validateEmail error", e);
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        com.yunweibang.auth.utils.TxQueryRunner tx = new com.yunweibang.auth.utils.TxQueryRunner();
        String updatesql = "update sso_find_pwd set token=? , token_expire_time=? ,modify_time=? ,validate_code=?,code_expire_time=? where id= ?";
        Calendar c = Calendar.getInstance();
        c.add(Calendar.MINUTE, 2);
        Object[] updateparam = new Object[]{token, c.getTime(), new Date(), null, null, Integer.parseInt(map.get("id").toString())};
        try {
            int result = tx.update(updatesql, updateparam);
            if (result > 0) {
                return new JsonResponse<Object>(200, "邮箱验证成功", token);
            }
        } catch (SQLException e) {
            logger.error(" editPasswd error", e);
        }
        insertFaillog(dto.getAccount(), ip, "找密,邮箱验证失败");
        return new JsonResponse<Object>(401, "邮箱验证失败", null);
    }

    public void clearPassRecord(String token) {
        com.yunweibang.auth.utils.TxQueryRunner tx = new com.yunweibang.auth.utils.TxQueryRunner();

        String updatesql = "update sso_find_pwd set token=? ,is_success=? ,modify_time=?  where token=?";

        Object[] updateparam = new Object[]{null, 1, new Date(), token};
        try {
            int result = tx.update(updatesql, updateparam);
            if (result > 0) {
                logger.info("clearPassRecord result=" + result);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public JsonResponse<Object> validateUserInfo(HttpServletRequest request, ValidateUserInfoDTO dto) {
        String ip = IPUtils.getIpAddr(request);
        String kaptcha = dto.getImageCode().trim();
        String sessionImageCode = (String) request.getSession()
                .getAttribute(com.google.code.kaptcha.Constants.KAPTCHA_SESSION_KEY);
        kaptcha = kaptcha.toLowerCase();
        sessionImageCode = sessionImageCode.toLowerCase();
        if (ToolUtil.isEmpty(kaptcha) || !kaptcha.equals(sessionImageCode)) {
            insertFaillog(dto.getAccount(), ip, "找密,图形验证码错误");

            return new JsonResponse<Object>(401, "图形验证码错误", null);
        }

        QueryRunner qr = new QueryRunner(com.yunweibang.auth.utils.JdbcUtils.getDs());
        String sql = "select * from ri_user where account= ? and email = ? ";
        Object[] params = new Object[]{dto.getAccount(), dto.getEmail()};
        Map map = null;
        try {
            map = qr.query(sql, new MapHandler(), params);
            if (map != null && map.size() > 0 && map.containsKey("id")) {
                logger.info("前去发邮件");
                return new JsonResponse<Object>(200, "验证成功", null);
            } else {
                insertFaillog(dto.getAccount(), ip, "找密,用户名或邮箱不存在");
                return new JsonResponse<Object>(401, "用户名或邮箱不存在", null);
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public boolean isLdapAuthType() {
        QueryRunner qr = new QueryRunner(com.yunweibang.auth.utils.JdbcUtils.getDs());
        String sql = "select * from sso_auth where type='ldap'";
        Map map = null;
        try {
            map = qr.query(sql, new MapHandler());
            if (map != null && map.containsKey("ldap_server")) {
                boolean flag = (Boolean) map.get("is_used");
                return flag;
            } else {
                return false;
            }

        } catch (SQLException e) {
            logger.error("select sql getAuthType error", e);
        }

        return false;
    }

    public void insertFaillog(String username, String ip, String content) {
        String address = getCityAddress(ip);
        com.yunweibang.auth.utils.TxQueryRunner tx = new com.yunweibang.auth.utils.TxQueryRunner();
        String logsql = " insert into log_login(account,status,operation,content,ip,city,ctime)  values(?,?,?,?,?,?,?) ";
        Date d = new Date();
        Object[] logparams = new Object[]{username, com.yunweibang.auth.utils.Constants.LOGIN_STATUS_FAIL, com.yunweibang.auth.utils.Constants.FIND_PASSWORD, content, ip, address, d};
        try {
            tx.update(logsql, logparams);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void insertSuccesslog(String username, String ip) {
        String address = getCityAddress(ip);
        com.yunweibang.auth.utils.TxQueryRunner tx = new com.yunweibang.auth.utils.TxQueryRunner();
        String logsql = " insert into log_login(account,status,operation,content,ip,city,ctime)  values(?,?,?,?,?,?,?) ";
        Date d = new Date();
        Object[] logparams = new Object[]{username, com.yunweibang.auth.utils.Constants.LOGIN_STATUS_SUCCESS, com.yunweibang.auth.utils.Constants.FIND_PASSWORD, "找回密码成功", ip, address, d};

        try {
            int logresult = tx.update(logsql, logparams);
            if (logresult <= 0) {
                throw new RuntimeException("插入登录日志失败");
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public String getCityAddress(String ip) {
        com.yunweibang.auth.utils.QQWry qqwry = null;
        try {
            qqwry = new com.yunweibang.auth.utils.QQWry();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        final IPZone zone = qqwry.findIP(ip);
        return zone.getMainInfo();
    }

    public void sendMessageToUser(String userName, String title, String msg) {
        QueryRunner qr = new QueryRunner(com.yunweibang.auth.utils.JdbcUtils.getDs());
        String sql = "select * from ri_user where account= ? limit 1";
        Object[] params = new Object[]{userName};
        Map map = null;
        try {
            map = qr.query(sql, new MapHandler(), params);
            if (map != null && StringUtils.isNotBlank((String) map.get("email"))) {
                Notifications notifications = getNotifications();
                notifications.setSendTo((String) map.get("email"));
                sendMailConfig(notifications, title, msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
          /*  if (map != null && map.get("dingtalk") != null && StringUtils.isNotBlank(map.get("dingtalk") + "")) {
                Map<String, Object> notify = loadSetting(2);
                if (notify.get("status") != null && Integer.parseInt(notify.get("status").toString()) == 1) {
                    logger.info("sendMessageToUser 前去发dingtalk dingtalk=" + map.get("dingtalk"));
                    Notifications notifications = new Notifications();
                    notifications.setHost(notify.get("host").toString());
                    notifications.setUser(notify.get("user").toString());
                    notifications.setPass(notify.get("pass").toString());
                    notifications.setCorpId(notify.get("corp_id").toString());
                    notifications.setSendTo(map.get("dingtalk").toString());
                    testDingDingConfig(notifications);
                }
            }
            if (map != null && map.get("wechat") != null && StringUtils.isNotBlank(map.get("wechat") + "")) {
                Map<String, Object> notify = loadSetting(3);
                if (Integer.parseInt(notify.get("status").toString()) == 1) {
                    logger.info("sendMessageToUser 前去发wechat wechat=" + map.get("wechat"));
                    Notifications notifications = new Notifications();
                    notifications.setUser(notify.get("user").toString());
                    notifications.setPass(notify.get("pass").toString());
                    notifications.setCorpId(notify.get("corp_id").toString());
                    notifications.setSendTo(map.get("wechat").toString());
                    testWechatConfig(notifications);
                }
            }*/

    }

    public boolean sendMailConfig(Notifications notifications, String title, String msg) {
        if (Strings.isNullOrEmpty(notifications.getSendTo())) {
            return false;
        }
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setDefaultEncoding("UTF-8");
        sender.setHost(notifications.getHost());
        sender.setPort(notifications.getPort());
        sender.setUsername(notifications.getUser());
        sender.setPassword(notifications.getPass());

        Properties javaMailProperties = new Properties();
        javaMailProperties.put("mail.smtp.auth", "true");
        javaMailProperties.put("mail.smtp.connectiontimeout", 300);
        if (notifications.getEnableSsl()) {
            javaMailProperties.put("mail.smtp.ssl.enable", true);
        }
        if (notifications.getEnableTls()) {
            javaMailProperties.put("mail.smtp.starttls.enable", true);
        }
        sender.setJavaMailProperties(javaMailProperties);
        try {
            SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
            simpleMailMessage.setTo(notifications.getSendTo());
            simpleMailMessage.setFrom(notifications.getUser());
            String titleSign = "";
            String contentSign = "";
            if (Objects.nonNull(notifications.getTitleSign())) {
                titleSign = notifications.getTitleSign();
            }
            if (Objects.nonNull(notifications.getContentSign())) {
                contentSign = com.yunweibang.auth.utils.Constants.LINE_SEPARATOR + com.yunweibang.auth.utils.Constants.LINE_SEPARATOR + notifications.getContentSign();
            }
            simpleMailMessage.setSubject(titleSign + title);
            simpleMailMessage.setText(msg + contentSign);
            sender.send(simpleMailMessage);
            return true;
        } catch (MailException ex) {
            try {
                throw new Exception(ex);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public boolean testWechatConfig(Notifications notifications) {
        String msg = "密码输错次数过多,请5分钟后重试!";
        try {
            return com.yunweibang.auth.utils.WeiXinUtils.sendTextMsg(notifications.getUser(), notifications.getPass(), notifications.getSendTo(),
                    Integer.parseInt(notifications.getCorpId()), msg);
        } catch (Exception ex) {
            try {
                throw new Exception(ex);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public boolean testDingDingConfig(Notifications notifications) {
        String msg = "密码输错次数过多,请5分钟后重试!";
        try {
            return com.yunweibang.auth.utils.WeiXinUtils.sendTextMsg(notifications.getCorpId(), notifications.getPass(), notifications.getSendTo(),
                    Integer.parseInt(notifications.getUser()), msg);
        } catch (Exception ex) {
            try {
                throw new Exception(ex);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public JsonResponse<Object> saveAccountRegistry(AccountRegistryDTO dto, HttpServletRequest request) {
        String kaptcha = dto.getImageCode().trim();
        String sessionImageCode = (String) request.getSession()
                .getAttribute(com.google.code.kaptcha.Constants.KAPTCHA_SESSION_KEY);
        kaptcha = kaptcha.toLowerCase();
        sessionImageCode = sessionImageCode.toLowerCase();
        if (ToolUtil.isEmpty(kaptcha) || !kaptcha.equals(sessionImageCode)) {
            return new JsonResponse<Object>(401, "图形验证码错误", null);
        }
        if (!Pattern.matches("^(?![0-9]+$)(?![a-zA-Z]+$)(?![_~@#\\$]+$)[0-9A-Za-z_~@#\\$]{6,16}$", dto.getPass())) {
            return new JsonResponse<Object>(501, "密码格式错误", null);
        }
        com.yunweibang.auth.utils.TxQueryRunner qr = new com.yunweibang.auth.utils.TxQueryRunner();
        String sql = "select id,account from ri_user where account= ? ";
        Object[] params = new Object[]{dto.getAccount()};
        Map map = null;
        try {
            map = qr.query(sql, new MapHandler(), params);
            if (map != null && map.size() > 0) {
                return new JsonResponse<Object>(402, "账户已存在", null);
            } else {
                insertUserAccount(dto, qr);
                insertMsgBox(dto, qr);
                //TODO 发邮件 新用户xxx已注册成功，等待激活
                sendRegisterMessageToAdmin(dto.getAccount());
                String title = "注册用户成功";
                String msg = "欢迎使用比格自动化运维平台，请等待管理员审批。";
                sendMessageToUser(dto.getAccount(), title, msg);
                return new JsonResponse<Object>(200, "注册成功", com.yunweibang.auth.utils.JdbcUtils.getDomainUrl());

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new JsonResponse<Object>(403, "注册失败", null);
    }

    private void insertMsgBox(AccountRegistryDTO dto, com.yunweibang.auth.utils.TxQueryRunner tx) {
        String sql = "insert into msg_box(receive_user,title,content,type,send_time,read_status,send_user,delete_status)  values(?,?,?,?,?,?,?,?)";

        String title = "审批新用户" + dto.getAccount() + "通知";
        String msg = "新用户" + dto.getAccount() + "已注册成功，等待您激活。请到资源->用户->常用管理->待激活用户，进行激活";
        Object[] params = new Object[]{"admin",
                title,
                msg,
                1, new Date(), 0, "系统", 0};
        try {
            tx.update(sql, params);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void insertUserAccount(AccountRegistryDTO dto, com.yunweibang.auth.utils.TxQueryRunner tx) {
        String sql = "insert into ri_user(name,account,pass,email,etime,status)  values(?,?,?,?,?,?)";
        String saltpwd = BCrypt.hashpw(dto.getPass(), BCrypt.gensalt());
        Object[] params = new Object[]{dto.getName(), dto.getAccount(), saltpwd, dto.getEmail(), "9999-01-01 01:01:01", "待激活"};
        try {
            tx.update(sql, params);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void sendRegisterMessageToAdmin(String userName) {
        QueryRunner qr = new QueryRunner(com.yunweibang.auth.utils.JdbcUtils.getDs());
        String sql = "select email from ri_user where account= ? limit 1";
        Object[] params = new Object[]{"admin"};
        Map map = null;
        try {
            map = qr.query(sql, new MapHandler(), params);
            if (map != null && StringUtils.isNotBlank((String) map.get("email"))) {
                Notifications notifications = getNotifications();

                if (Objects.nonNull(notifications)) {
                    notifications.setSendTo((String) map.get("email"));
                    String title = "审批新用户" + userName + "通知";
                    String msg = "新用户" + userName + "已注册成功，等待您激活。请到资源->用户->常用管理->待激活用户，进行激活";
                    sendMailConfig(notifications, title, msg);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public JsonResponse<Object> getHomeUrl() {
        return new JsonResponse<Object>(com.yunweibang.auth.utils.JdbcUtils.getDomainUrl());
    }
}
