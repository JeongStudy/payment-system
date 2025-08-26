package com.system.payment.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public final class TimeUtils {
  private TimeUtils() {}

  private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Seoul");

  public static Instant toInstant(LocalDateTime ldt) {
    return ldt.atZone(DEFAULT_ZONE).toInstant();
  }

  public static Instant toInstant(LocalDateTime ldt, ZoneId zone) {
    return ldt.atZone(zone).toInstant();
  }
}