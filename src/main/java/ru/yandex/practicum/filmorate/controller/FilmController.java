package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/films")
public class FilmController {
    private final FilmStorage filmStorage;
    private final FilmService filmService;

    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);
    private static final int MAX_DESCRIPTION_LENGTH = 200;

    @PostMapping
    public Film createFilm(@RequestBody Film film) {
        validateFilm(film);

        Film createdFilm = filmStorage.create(film);
        log.info("Добавлен фильм: {}", createdFilm);

        return createdFilm;
    }

    @PutMapping
    public Film updateFilm(@RequestBody Film film) {
        validateFilm(film);

        Film updatedFilm = filmStorage.update(film);
        log.info("Обновлён фильм: {}", updatedFilm);

        return updatedFilm;
    }

    @GetMapping
    public Collection<Film> getAllFilms() {
        return filmStorage.getAll();
    }

    @GetMapping("/{id}")
    public Film getFilmById(@PathVariable Integer id) {
        return filmStorage.getById(id);
    }

    @PutMapping("/{id}/like/{userId}")
    public Film addLike(@PathVariable Integer id, @PathVariable Integer userId) {
        log.info("Пользователь id={} ставит лайк фильму id={}", userId, id);
        return filmService.addLike(id, userId);
    }

    @DeleteMapping("/{id}/like/{userId}")
    public Film removeLike(@PathVariable Integer id, @PathVariable Integer userId) {
        log.info("Пользователь id={} удаляет лайк у фильма id={}", userId, id);
        return filmService.removeLike(id, userId);
    }

    @GetMapping("/popular")
    public List<Film> getPopularFilms(@RequestParam(defaultValue = "10") Integer count) {
        return filmService.getPopularFilms(count);
    }

    private void validateFilm(Film film) {
        if (film == null) {
            log.warn("Ошибка валидации фильма: тело запроса пустое");
            throw new ValidationException("Фильм не может быть null");
        }

        if (film.getName() == null || film.getName().isBlank()) {
            log.warn("Ошибка валидации фильма: пустое название");
            throw new ValidationException("Название фильма не может быть пустым");
        }

        if (film.getDescription() != null && film.getDescription().length() > MAX_DESCRIPTION_LENGTH) {
            log.warn("Ошибка валидации фильма: описание длиннее 200 символов");
            throw new ValidationException("Описание фильма не может быть длиннее 200 символов");
        }

        if (film.getReleaseDate() == null || film.getReleaseDate().isBefore(MIN_RELEASE_DATE)) {
            log.warn("Ошибка валидации фильма: некорректная дата релиза");
            throw new ValidationException("Дата релиза не может быть раньше 28 декабря 1895 года");
        }

        if (film.getDuration() == null || film.getDuration() <= 0) {
            log.warn("Ошибка валидации фильма: некорректная продолжительность");
            throw new ValidationException("Продолжительность фильма должна быть положительным числом");
        }
    }
}