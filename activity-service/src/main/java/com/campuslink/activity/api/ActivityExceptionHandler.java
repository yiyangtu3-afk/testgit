package com.campuslink.activity.api;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
class ActivityExceptionHandler {
  @ExceptionHandler(MethodArgumentNotValidException.class) @ResponseStatus(HttpStatus.BAD_REQUEST)
  Map<String, String> validation(MethodArgumentNotValidException error) { return Map.of("message", error.getBindingResult().getFieldErrors().stream().findFirst().map(field -> field.getDefaultMessage()).orElse("请求参数不正确")); }
  @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class}) @ResponseStatus(HttpStatus.BAD_REQUEST)
  Map<String, String> badRequest(Exception error) { return Map.of("message", error instanceof HttpMessageNotReadableException ? "请求格式不正确" : error.getMessage()); }
  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<Map<String, String>> status(ResponseStatusException error) {
    return ResponseEntity.status(error.getStatusCode())
        .body(Map.of("message", error.getReason() == null ? "请求失败" : error.getReason()));
  }
}
