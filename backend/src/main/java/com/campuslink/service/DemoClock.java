package com.campuslink.service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

@Component
public class DemoClock {

  private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

  public String nowTime() {
    return LocalTime.now().format(TIME_FORMAT);
  }
}
