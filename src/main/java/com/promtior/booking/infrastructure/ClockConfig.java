package com.promtior.booking.infrastructure;

import com.promtior.booking.domain.BookingRange;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Ver ADR 0003: la zona de oficina es la que rige el reloj de producción. */
@Configuration
class ClockConfig {

  @Bean
  Clock clock() {
    return Clock.system(BookingRange.OFFICE_ZONE);
  }
}
