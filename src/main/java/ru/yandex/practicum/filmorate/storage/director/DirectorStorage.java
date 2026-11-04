package ru.yandex.practicum.filmorate.storage.director;

import ru.yandex.practicum.filmorate.model.Director;
import java.util.List;
import java.util.Optional;

// Интерфейс для хранения и управления режиссёрами
public interface DirectorStorage {

    // Возвращает список всех режиссёров
    List<Director> getAll();

    // Возвращает режиссёра по его идентификатору
    Optional<Director> getById(Integer id);

    // Создаёт и сохраняет нового режиссёра
    Director create(Director director);

    // Обновляет информацию о существующем режиссёре
    Director update(Director director);

    // Удаляет режиссёра по его идентификатору
    void delete(Integer id);
}
