package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class FilmControllerTest {
    private final FilmController controller = new FilmController();

    @Test
    void shouldCreateValidFilm() {
        Film film = new Film();
        film.setName("Avatar");
        film.setDescription("Good film");
        film.setReleaseDate(LocalDate.of(2009, 12, 10));
        film.setDuration(160);

        Film createdFilm = controller.createFilm(film);

        assertNotNull(createdFilm.getId());
        assertEquals("Avatar", createdFilm.getName());
    }

    @Test
    void shouldNotCreateFilmWithEmptyName() {
        Film film = new Film();
        film.setName("");
        film.setDescription("Good film");
        film.setReleaseDate(LocalDate.of(2009, 12, 10));
        film.setDuration(160);

        assertThrows(ValidationException.class, () -> controller.createFilm(film));
    }

    @Test
    void shouldNotCreateFilmWithLongDescription() {
        Film film = new Film();
        film.setName("Avatar");
        film.setDescription("a".repeat(201));
        film.setReleaseDate(LocalDate.of(2009, 12, 10));
        film.setDuration(160);

        assertThrows(ValidationException.class, () -> controller.createFilm(film));
    }

    @Test
    void shouldNotCreateFilmWithReleaseDateBeforeCinemaBirthday() {
        Film film = new Film();
        film.setName("Old film");
        film.setDescription("Too old");
        film.setReleaseDate(LocalDate.of(1895, 12, 27));
        film.setDuration(100);

        assertThrows(ValidationException.class, () -> controller.createFilm(film));
    }

    @Test
    void shouldNotCreateFilmWithNegativeDuration() {
        Film film = new Film();
        film.setName("Avatar");
        film.setDescription("Good film");
        film.setReleaseDate(LocalDate.of(2009, 12, 10));
        film.setDuration(-1);

        assertThrows(ValidationException.class, () -> controller.createFilm(film));
    }
}