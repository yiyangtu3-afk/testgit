package com.campuslink.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RequestLogInterceptor implements HandlerInterceptor {

  private static final Logger log = LoggerFactory.getLogger(RequestLogInterceptor.class);
  private static final String START_TIME_ATTRIBUTE = "campusLinkRequestStartTime";

  private final MeterRegistry meterRegistry;

  public RequestLogInterceptor(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

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
    recordRequestMetric(request, response, durationMs);
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

  private void recordRequestMetric(
      HttpServletRequest request,
      HttpServletResponse response,
      long durationMs) {
    Timer.builder("campuslink.http.requests")
        .description("CampusLink API request duration")
        .tags(
            "method", request.getMethod(),
            "route", routeTemplate(request),
            "status", String.valueOf(response.getStatus()))
        .register(meterRegistry)
        .record(durationMs, TimeUnit.MILLISECONDS);
  }

  private String routeTemplate(HttpServletRequest request) {
    Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
    return pattern instanceof String value ? value : "unmatched";
  }
}
