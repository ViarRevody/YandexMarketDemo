package com.example.domain;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "couriers")
public class CourierEntity {

    @Id
    @SequenceGenerator(name = "couriers_seq_gen", sequenceName = "couriers_seq", allocationSize = 50)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "couriers_seq_gen")
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "phone", nullable = false, unique = true)
    private String phone;

    @Column(name = "available", nullable = false)
    @Builder.Default
    private Boolean available = true;
}