package com.yunweibang.auth.conf;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
@ComponentScan("com.yunweibang.auth")
@PropertySource(value = {"file:/opt/bigops/config/bigops.properties"})
public class MySSOConfig extends WebMvcConfigurerAdapter {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new ResetPassTokenInterceptor()).addPathPatterns("/user/editPasswd", "/user/password/resetPass");
        super.addInterceptors(registry);
    }

    @Bean
    public FilterRegistrationBean filterCors() {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(new CorsFilter());
        registration.setName("corsFilter");
        // 设定匹配的路径
        registration.addUrlPatterns("/*");
        registration.setOrder(1);
        return registration;
    }

}
