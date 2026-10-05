package com.example.domain.Address;

import com.example.domain.Customer.CustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AddressEntityRepository extends JpaRepository<AddressEntity, Long> {
    Optional<AddressEntity> findByIdAndCustomer(Long id, CustomerEntity customer);
}