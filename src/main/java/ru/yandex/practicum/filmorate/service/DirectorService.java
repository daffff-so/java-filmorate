package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.storage.director.DirectorStorage;

import java.util.List;

@Service
@RequiredArgsConstructor
// Сервис для управления режиссёрами
public class DirectorService {

    // Хранилище данных о режиссёрах
    private final DirectorStorage storage;

    // Возвращает список всех режиссёров
    public List<Director> getAll() {
        return storage.getAll();
    }

    // Возвращает режиссёра по идентификатору
    public Director getById(Integer id) {
        return storage.getById(id).orElseThrow(() ->
                new NotFoundException("Режиссёр с id=" + id + " не найден"));
    }

    // Создаёт нового режиссёра после проверки данных
    public Director create(Director director) {
        validate(director);
        return storage.create(director);
    }

    // Обновляет данные существующего режиссёра
    public Director update(Director director) {
        validate(director);
        if (director.getId() == null) {
            throw new ValidationException("Не указан id режиссёра");
        }
        getById(director.getId());
        return storage.update(director);
    }

    // Удаляет режиссёра по идентификатору
    public void delete(Integer id) {
        getById(id);
        storage.delete(id);
    }

    // Проверяет корректность данных режиссёра
    private void validate(Director director) {
        if (director == null || director.getName() == null || director.getName().isBlank()) {
            throw new ValidationException("Имя режиссёра не может быть пустым");
        }
    }
}
