package ru.yandex.practicum.filmorate.storage.film;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public interface FilmStorage {
    Film create(Film film);

    Film update(Film film);

    Optional<Film> getById(Integer id);

    Collection<Film> getAll();

    void delete(Integer id);

    default void addLike(Integer filmId, Integer userId) {
        getById(filmId).ifPresent(film -> film.getLikes().add(userId));
    }

    default void removeLike(Integer filmId, Integer userId) {
        getById(filmId).ifPresent(film -> film.getLikes().remove(userId));
    }

    default List<Film> getPopular(Integer count) {
        return getAll().stream()
                .sorted(Comparator.comparingInt(
                        (Film film) -> film.getLikes().size()
                ).reversed())
                .limit(count)
                .toList();
    }

    default List<Film> getCommonFilms(Integer userId, Integer friendId) {
        return getAll().stream()
                .filter(film -> film.getLikes().contains(userId)
                        && film.getLikes().contains(friendId))
                .sorted(Comparator.comparingInt((Film film) -> film.getLikes().size())
                        .reversed()
                        .thenComparing(Film::getId))
                .toList();
    }
}