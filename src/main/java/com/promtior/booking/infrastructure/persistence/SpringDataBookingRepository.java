package com.promtior.booking.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataBookingRepository extends JpaRepository<BookingJpaEntity, UUID> {

  List<BookingJpaEntity> findByRoomId(String roomId);

  List<BookingJpaEntity> findByOwnerUsername(String ownerUsername);
}
