package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.mpa.MpaStorage;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/mpa")
public class MpaController {
    private final MpaStorage mpaStorage;

    @GetMapping
    public List<Mpa> getAll() {
        return mpaStorage.getAll();
    }

    @GetMapping("/{id}")
    public Mpa getById(@PathVariable Integer id) {
        return mpaStorage.getById(id)
                .orElseThrow(() ->
                        new NotFoundException("MPA с id=" + id + " не найден"));
    }
}
