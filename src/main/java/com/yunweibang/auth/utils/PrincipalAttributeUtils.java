package com.yunweibang.auth.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

@SuppressWarnings("unused")
public class PrincipalAttributeUtils {
    private static final Logger logger = LoggerFactory.getLogger(PrincipalAttributeUtils.class);
    private static Set<String> principalAttributes;

    public static Set<String> getPrincipalAttributes() {
        return principalAttributes;
    }

    public static void setPrincipalAttributes(Set<String> principalAttributes) {
        PrincipalAttributeUtils.principalAttributes = principalAttributes;
    }

    static {
        logger.info("PrincipalAttributeUtils  enter ");
        Properties prop = new Properties();
        String attributes;
        String ssoInitAppUrl;
        Set<String> set;
        try {
            prop.load(new FileReader("/opt/bigops/config/bigops.properties"));
        } catch (FileNotFoundException e) {
            logger.error("PrincipalAttributeUtils error", e);

        } catch (IOException e) {
            logger.error("PrincipalAttributeUtils error", e);
        }

        attributes = prop.getProperty("sso.principal.attributes").trim();
        if (attributes.indexOf(",") != -1) {
            String[] strs = attributes.replaceAll(" ", "").split(",");
            set = new HashSet<String>(Arrays.asList(strs));
        } else {
            set = new HashSet<String>();
            set.add(attributes);
        }
        PrincipalAttributeUtils.setPrincipalAttributes(set);

    }

}
