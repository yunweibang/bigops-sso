package com.yunweibang.auth.conf;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.util.Properties;

/**
 * kaptcha配置
 *
 * @author lpp

 */
public class KaptchaConfig {

    @Value("${kaptcha.textproducer.font.color}")
    private String fcolor;

    @Value("${kaptcha.textproducer.font.size}")
    private String fsize;

    @Value("${kaptcha.textproducer.font.names}")
    private String fnames;

    @Value("${kaptcha.obscurificator.impl}")
    private String obscurificator;

    @Value("${kaptcha.noise.impl}")
    private String noise;

    @Value("${kaptcha.image.width}")
    private String width;

    @Value("${kaptcha.image.height}")
    private String height;

    @Value("${kaptcha.textproducer.char.length}")
    private String clength;

    @Value("${kaptcha.textproducer.char.space}")
    private String cspace;

    @Value("${kaptcha.background.clear.from}")
    private String from;

    @Value("${kaptcha.background.clear.to}")
    private String to;

    @Bean
    public DefaultKaptcha kaptcha() {
        Properties properties = new Properties();
        properties.put("kaptcha.border", "no");
        properties.put("kaptcha.textproducer.font.color", fcolor);
        properties.put("kaptcha.textproducer.font.size", fsize);
        properties.put("kaptcha.textproducer.font.names", fnames);
        properties.put("kaptcha.obscurificator.impl", obscurificator);
        properties.put("kaptcha.noise.impl", noise);
        properties.put("kaptcha.image.width", width);
        properties.put("kaptcha.image.height", height);
        properties.put("kaptcha.textproducer.char.length", clength);
        properties.put("kaptcha.textproducer.char.space", cspace);
        properties.put("kaptcha.background.clear.from", from);
        properties.put("kaptcha.background.clear.to", to);
        Config config = new Config(properties);
        DefaultKaptcha defaultKaptcha = new DefaultKaptcha();
        defaultKaptcha.setConfig(config);
        return defaultKaptcha;
    }
}
