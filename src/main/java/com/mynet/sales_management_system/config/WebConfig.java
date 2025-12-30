// WebConfig.java - 웹 설정
package com.mynet.sales_management_system.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 웹 설정
 * - 뷰 컨트롤러 매핑
 * - 정적 리소스 설정
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 정적 리소스 핸들러 등록
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/");
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/");
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/");
    }

}