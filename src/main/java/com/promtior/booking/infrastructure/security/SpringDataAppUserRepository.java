package com.promtior.booking.infrastructure.security;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataAppUserRepository extends JpaRepository<AppUserJpaEntity, String> {}
