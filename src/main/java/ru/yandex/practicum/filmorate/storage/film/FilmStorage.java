
package ru.yandex.practicum.filmorate.storage.film;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

// Интерфейс для хранения и управления фильмами
public interface FilmStorage {

    // Создаёт новый фильм
    Film create(Film film);

    // Обновляет существующий фильм
    Film update(Film film);

    // Возвращает фильм по идентификатору
    Optional<Film> getById(Integer id);

    // Возвращает все фильмы
    Collection<Film> getAll();

    // Удаляет фильм по идентификатору
    void delete(Integer id);

    // Добавляет лайк фильму
    default void addLike(Integer filmId, Integer userId) {
        getById(filmId).ifPresent(film -> film.getLikes().add(userId));
    }

    // Удаляет лайк у фильма
    default void removeLike(Integer filmId, Integer userId) {
        getById(filmId).ifPresent(film -> film.getLikes().remove(userId));
    }

    // Возвращает популярные фильмы по количеству лайков
    default List<Film> getPopular(Integer count) {
        return getAll().stream()
                .sorted(Comparator.comparingInt(
                        (Film film) -> film.getLikes().size()
                ).reversed())
                .limit(count)
                .toList();
    }

    // Возвращает фильмы режиссёра с сортировкой по году или лайкам
    List<Film> getByDirector(Integer directorId, String sortBy);
}
