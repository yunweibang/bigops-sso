package com.yunweibang.auth.conf;

import com.yunweibang.auth.utils.DisableSSLCertificateCheckUtil;
import com.yunweibang.auth.utils.JdbcUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapHandler;
import org.apereo.cas.services.RegexRegisteredService;
import org.apereo.cas.services.RegisteredService.LogoutType;
import org.apereo.cas.services.ReturnAllAttributeReleasePolicy;
import org.apereo.cas.services.ServicesManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.Map;

@Component
@Order(1)
public class DisableSSLCertificateRunner implements CommandLineRunner {
    private static Logger logger = LoggerFactory.getLogger(DisableSSLCertificateRunner.class);
    @Autowired
    @Qualifier("servicesManager")
    private ServicesManager servicesManager;

    @Value("${home.url}")
    private String homeUrl;

    @Override
    public void run(String... args) throws Exception {
        DisableSSLCertificateCheckUtil.disableChecks();
        System.setProperty("com.sun.jndi.ldap.object.disableEndpointIdentification", "true");
        logger.info("sso init app url start to initialize ...");
        QueryRunner qr = new QueryRunner(JdbcUtils.getDs());
        String sql = "select id from regexregisteredservice where serviceId=? limit 1 ";
        String serviceId = "^" + homeUrl + ".*";
        Object[] params = new Object[]{serviceId};
        Map map = null;
        map = qr.query(sql, new MapHandler(), params);
        if (map != null && map.containsKey("id")) {
            logger.info("sso init app url exists ......");
        } else {
            RegexRegisteredService service = new RegexRegisteredService();
            ReturnAllAttributeReleasePolicy re = new ReturnAllAttributeReleasePolicy();
            service.setServiceId(serviceId);
            service.setAttributeReleasePolicy(re);
            // 将name统一设置为servicesId
            service.setName("bigops");
            // 单点登出
            service.setLogoutUrl(new URL(homeUrl + "/api/auth/quit"));
            service.setTheme("default");
            service.setLogoutType(LogoutType.BACK_CHANNEL);
            servicesManager.save(service);
            servicesManager.load();
            logger.info(" sso init app url end add success ......");
        }
    }
}

