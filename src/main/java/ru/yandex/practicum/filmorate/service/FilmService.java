package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.genre.GenreStorage;
import ru.yandex.practicum.filmorate.storage.mpa.MpaStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class FilmService {
    private static final LocalDate MIN_RELEASE_DATE =
            LocalDate.of(1895, 12, 28);
    private static final int MAX_DESCRIPTION_LENGTH = 200;

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final GenreStorage genreStorage;
    private final MpaStorage mpaStorage;

    public FilmService(
            @Qualifier("filmDbStorage") FilmStorage filmStorage,
            @Qualifier("userDbStorage") UserStorage userStorage,
            GenreStorage genreStorage,
            MpaStorage mpaStorage
    ) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.genreStorage = genreStorage;
        this.mpaStorage = mpaStorage;
    }

    public Film createFilm(Film film) {
        validateFilm(film);
        validateReferences(film);
        return filmStorage.create(film);
    }

    public Film updateFilm(Film film) {
        validateFilm(film);
        getFilmById(film.getId());
        validateReferences(film);

        return filmStorage.update(film);
    }

    public Collection<Film> getAllFilms() {
        return filmStorage.getAll();
    }

    public Film getFilmById(Integer id) {
        return filmStorage.getById(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Фильм с id=" + id + " не найден"
                        ));
    }

    public Film addLike(Integer filmId, Integer userId) {
        getFilmById(filmId);
        getUserById(userId);

        filmStorage.addLike(filmId, userId);
        return getFilmById(filmId);
    }

    public Film removeLike(Integer filmId, Integer userId) {
        getFilmById(filmId);
        getUserById(userId);

        filmStorage.removeLike(filmId, userId);
        return getFilmById(filmId);
    }

    public List<Film> getPopularFilms(Integer count) {
        if (count == null) {
            count = 10;
        }

        if (count <= 0) {
            throw new ValidationException(
                    "Параметр count должен быть больше 0"
            );
        }

        return filmStorage.getPopular(count);
    }

    private User getUserById(Integer id) {
        return userStorage.getById(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Пользователь с id=" + id + " не найден"
                        ));
    }

    private void validateFilm(Film film) {
        if (film == null) {
            log.warn("Ошибка валидации фильма: тело запроса пустое");
            throw new ValidationException(
                    "Фильм не может быть null"
            );
        }

        if (film.getName() == null || film.getName().isBlank()) {
            log.warn("Ошибка валидации фильма: пустое название");
            throw new ValidationException(
                    "Название фильма не может быть пустым"
            );
        }

        if (film.getDescription() != null
                && film.getDescription().length()
                > MAX_DESCRIPTION_LENGTH) {
            log.warn(
                    "Ошибка валидации фильма: описание длиннее 200 символов"
            );
            throw new ValidationException(
                    "Описание фильма не может быть длиннее 200 символов"
            );
        }

        if (film.getReleaseDate() == null
                || film.getReleaseDate().isBefore(MIN_RELEASE_DATE)) {
            log.warn("Ошибка валидации фильма: некорректная дата релиза");
            throw new ValidationException(
                    "Дата релиза не может быть раньше 28 декабря 1895 года"
            );
        }

        if (film.getDuration() == null || film.getDuration() <= 0) {
            log.warn(
                    "Ошибка валидации фильма: некорректная продолжительность"
            );
            throw new ValidationException(
                    "Продолжительность фильма должна быть положительным числом"
            );
        }
    }

    private void validateReferences(Film film) {
        if (film.getMpa() == null || film.getMpa().getId() == null) {
            throw new NotFoundException("Рейтинг MPA не указан");
        }

        Mpa mpa = mpaStorage.getById(film.getMpa().getId())
                .orElseThrow(() ->
                        new NotFoundException(
                                "Рейтинг MPA с id="
                                        + film.getMpa().getId()
                                        + " не найден"
                        ));

        film.setMpa(mpa);

        if (film.getGenres() == null || film.getGenres().isEmpty()) {
            film.setGenres(new LinkedHashSet<>());
            return;
        }

        boolean containsInvalidGenre = film.getGenres().stream()
                .anyMatch(genre ->
                        genre == null || genre.getId() == null);

        if (containsInvalidGenre) {
            throw new NotFoundException(
                    "Указан жанр без идентификатора"
            );
        }

        Set<Integer> genreIds = film.getGenres().stream()
                .map(Genre::getId)
                .collect(
                        java.util.stream.Collectors.toCollection(
                                LinkedHashSet::new
                        )
                );

        List<Genre> existingGenres =
                genreStorage.getByIds(genreIds);

        if (existingGenres.size() != genreIds.size()) {
            throw new NotFoundException(
                    "Один или несколько жанров не найдены"
            );
        }

        film.setGenres(new LinkedHashSet<>(existingGenres));
    }
}