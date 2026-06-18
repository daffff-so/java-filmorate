package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FilmService {
    private final FilmStorage filmStorage;
    private final UserStorage userStorage;

    public Film addLike(Integer filmId, Integer userId) {
        Film film = filmStorage.getById(filmId);
        userStorage.getById(userId);

        film.getLikes().add(userId);
        return film;
    }

    public Film removeLike(Integer filmId, Integer userId) {
        Film film = filmStorage.getById(filmId);
        userStorage.getById(userId);

        film.getLikes().remove(userId);
        return film;
    }

    public List<Film> getPopularFilms(Integer count) {
        int limit = count == null ? 10 : count;

        return filmStorage.getAll().stream()
                .sorted(Comparator.comparingInt((Film film) -> film.getLikes().size()).reversed())
                .limit(limit)
                .toList();
    }
}