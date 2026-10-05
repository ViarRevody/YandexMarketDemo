package com.example.domain.Category;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "categories")
public class CategoryEntity {

    @Id
    @SequenceGenerator(name = "categories_seq_gen", sequenceName = "categories_seq", allocationSize = 50)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "categories_seq_gen")
    private Long id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;
}