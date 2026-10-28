package com.example.domain;


import com.example.domain.repository.CourierEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CourierDataInitializer implements CommandLineRunner {

    private final CourierEntityRepository courierRepository;

    @Override
    public void run(String... args) {

        if (courierRepository.count() > 0) {
            return;
        }

        courierRepository.save(
                CourierEntity.builder()
                        .name("Ахмад")
                        .phone("+79990000001")
                        .available(true)
                        .build()
        );

        courierRepository.save(
                CourierEntity.builder()
                        .name("Магомед")
                        .phone("+79990000002")
                        .available(true)
                        .build()
        );

        courierRepository.save(
                CourierEntity.builder()
                        .name("Али")
                        .phone("+79990000003")
                        .available(true)
                        .build()
        );
    }
}