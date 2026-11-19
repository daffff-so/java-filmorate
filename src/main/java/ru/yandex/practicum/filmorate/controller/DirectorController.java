package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.service.DirectorService;

import java.util.List;

@RestController
@RequestMapping("/directors")
@RequiredArgsConstructor
// Класс DirectorController
public class DirectorController {

    // Сервис для управления режиссёрами
    private final DirectorService service;

    @GetMapping
    // Получает список всех режиссёров
    public List<Director> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    // Получает режиссёра по его идентификатору
    public Director getById(@PathVariable Integer id) {
        return service.getById(id);
    }

    @PostMapping
    // Создаёт нового режиссёра
    public Director create(@RequestBody Director director) {
        return service.create(director);
    }

    @PutMapping
    // Обновляет данные существующего режиссёра
    public Director update(@RequestBody Director director) {
        return service.update(director);
    }

    @DeleteMapping("/{id}")
    // Удаляет режиссёра по его идентификатору
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}
