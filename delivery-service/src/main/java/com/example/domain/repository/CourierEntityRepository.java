package com.example.domain.repository;

import com.example.domain.CourierEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CourierEntityRepository extends JpaRepository<CourierEntity, Long> {
    Optional<CourierEntity> findFirstByAvailableTrue();
}