package com.campuslink.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RequestLogInterceptor implements HandlerInterceptor {

  private static final Logger log = LoggerFactory.getLogger(RequestLogInterceptor.class);
  private static final String START_TIME_ATTRIBUTE = "campusLinkRequestStartTime";

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    request.setAttribute(START_TIME_ATTRIBUTE, System.currentTimeMillis());
    log.info("[HTTP] --> {} {}", request.getMethod(), request.getRequestURI());
    return true;
  }

  @Override
  public void afterCompletion(
      HttpServletRequest request,
      HttpServletResponse response,
      Object handler,
      Exception ex) {
    long startedAt = (long) request.getAttribute(START_TIME_ATTRIBUTE);
    long durationMs = System.currentTimeMillis() - startedAt;
    if (ex == null) {
      log.info(
          "[HTTP] <-- {} {} status={} duration={}ms",
          request.getMethod(),
          request.getRequestURI(),
          response.getStatus(),
          durationMs);
      return;
    }
    log.warn(
        "[HTTP] <-- {} {} status={} duration={}ms error={}",
        request.getMethod(),
        request.getRequestURI(),
        response.getStatus(),
        durationMs,
        ex.getMessage());
  }
}
