package com.promtior.booking.infrastructure.persistence;

import com.promtior.booking.domain.Booking;
import com.promtior.booking.domain.BookingRange;
import com.promtior.booking.domain.Room;
import com.promtior.booking.domain.TimeSlot;
import com.promtior.booking.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Fila de la tabla {@code booking}. {@code roomId} y {@code ownerUsername} son columnas planas, no
 * relaciones JPA a {@code room}/{@code app_user}: ver ADR 0004.
 */
@Entity
@Table(name = "booking")
public class BookingJpaEntity {

  @Id private UUID id;

  @Column(nullable = false)
  private String title;

  @Column(name = "attendee_count", nullable = false)
  private int attendeeCount;

  @Column(name = "owner_username", nullable = false)
  private String ownerUsername;

  @Column(name = "room_id", nullable = false)
  private String roomId;

  @Column(name = "range_start", nullable = false)
  private LocalDateTime rangeStart;

  @Column(name = "range_end", nullable = false)
  private LocalDateTime rangeEnd;

  protected BookingJpaEntity() {}

  private BookingJpaEntity(
      UUID id,
      String title,
      int attendeeCount,
      String ownerUsername,
      String roomId,
      LocalDateTime rangeStart,
      LocalDateTime rangeEnd) {
    this.id = id;
    this.title = title;
    this.attendeeCount = attendeeCount;
    this.ownerUsername = ownerUsername;
    this.roomId = roomId;
    this.rangeStart = rangeStart;
    this.rangeEnd = rangeEnd;
  }

  static BookingJpaEntity fromDomain(Booking booking) {
    return new BookingJpaEntity(
        UUID.randomUUID(),
        booking.title(),
        booking.attendeeCount(),
        booking.owner().username(),
        booking.room().name(),
        booking.range().start().start(),
        booking.range().end().end());
  }

  Booking toDomain() {
    BookingRange range =
        BookingRange.between(new TimeSlot(rangeStart), new TimeSlot(rangeEnd.minusMinutes(30)));
    return new Booking(title, attendeeCount, new User(ownerUsername), Room.valueOf(roomId), range);
  }
}
