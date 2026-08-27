package com.promtior.booking.domain;

/** Catálogo cerrado de salas de reunión, con su capacidad máxima de asistentes. */
public enum Room {
  A(4),
  B(6),
  C(8),
  D(12),
  E(20);

  private final int capacity;

  Room(int capacity) {
    this.capacity = capacity;
  }

  public int capacity() {
    return capacity;
  }
}
