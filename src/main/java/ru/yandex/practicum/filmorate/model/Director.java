package ru.yandex.practicum.filmorate.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
// Модель режиссёра фильма
public class Director {

    // Уникальный идентификатор режиссёра
    private Integer id;

    // Имя режиссёра
    private String name;
}
