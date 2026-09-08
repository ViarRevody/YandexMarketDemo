package com.example.domain.Restaurant;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantEntityRepository extends JpaRepository<RestaurantEntity, Long> {
}