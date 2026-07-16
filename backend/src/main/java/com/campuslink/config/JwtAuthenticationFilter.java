package com.campuslink.config;

import com.campuslink.entity.DemoEntities.UserEntity;
import com.campuslink.service.AuthTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final AuthTokenService authTokenService;
  private final ObjectMapper objectMapper;

  public JwtAuthenticationFilter(AuthTokenService authTokenService, ObjectMapper objectMapper) {
    this.authTokenService = authTokenService;
    this.objectMapper = objectMapper;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return (!path.startsWith("/api/") && !path.startsWith("/actuator/"))
        || path.startsWith("/api/auth/")
        || "/api/database/health".equals(path)
        || "/actuator/health".equals(path);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (authorization == null || authorization.isBlank()) {
      filterChain.doFilter(request, response);
      return;
    }
    try {
      UserEntity user = authTokenService.requireUser(authorization);
      var authentication = new UsernamePasswordAuthenticationToken(
          user,
          null,
          user.role().contains("管理员")
              ? List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
              : List.of(new SimpleGrantedAuthority("ROLE_USER")));
      authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
      SecurityContextHolder.getContext().setAuthentication(authentication);
      filterChain.doFilter(request, response);
    } catch (SecurityException exception) {
      SecurityContextHolder.clearContext();
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      objectMapper.writeValue(response.getOutputStream(), Map.of("message", exception.getMessage()));
    } finally {
      SecurityContextHolder.clearContext();
    }
  }
}
