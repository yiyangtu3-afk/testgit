package com.campuslink.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  private final RequestLogInterceptor requestLogInterceptor;

  public WebConfig(RequestLogInterceptor requestLogInterceptor) {
    this.requestLogInterceptor = requestLogInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(requestLogInterceptor)
        .addPathPatterns("/api/**");
  }
}
