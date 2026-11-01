package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.film.InMemoryFilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;
import ru.yandex.practicum.filmorate.storage.user.InMemoryUserStorage;

import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.genre.GenreStorage;
import ru.yandex.practicum.filmorate.storage.mpa.MpaStorage;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class FilmControllerTest {
    private FilmController controller;

    @BeforeEach
    void setUp() {
        FilmStorage filmStorage = new InMemoryFilmStorage();
        UserStorage userStorage = new InMemoryUserStorage();
        GenreStorage genreStorage = mock(GenreStorage.class);
        MpaStorage mpaStorage = mock(MpaStorage.class);

        when(mpaStorage.getById(1))
                .thenReturn(Optional.of(new Mpa(1, "G")));

        when(genreStorage.getByIds(anyCollection()))
                .thenAnswer(invocation -> {
                    Collection<?> ids = invocation.getArgument(0);
                    return ids.stream()
                            .map(id -> new Genre((Integer) id, "Genre " + id))
                            .toList();
                });

        FilmService filmService = new FilmService(
                filmStorage,
                userStorage,
                genreStorage,
                mpaStorage
        );

        controller = new FilmController(filmService);
    }

    @Test
    void shouldCreateValidFilm() {

        Film film = new Film();
        film.setName("Avatar");
        film.setDescription("Good film");
        film.setReleaseDate(LocalDate.of(2009, 12, 10));
        film.setDuration(160);

        film.setMpa(new Mpa(1, "G"));

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

    @Test
    void shouldReturnPopularFilmsFilteredByGenreAndYear() {
        Film a = createFilm(LocalDate.of(2000, 6, 1), 1);
        createFilm(LocalDate.of(2000, 6, 1), 2);
        createFilm(LocalDate.of(2001, 6, 1), 1);

        List<Film> filtered = controller.getPopularFilms(10, 1, 2000);

        assertEquals(1, filtered.size());
        assertEquals(a.getId(), filtered.getFirst().getId());
        assertEquals(3, controller.getPopularFilms(10, null, null).size());
    }

    @Test
    void shouldRejectNonPositiveCountWithFilters() {
        assertThrows(ValidationException.class,
                () -> controller.getPopularFilms(0, 1, 2000));
        assertThrows(ValidationException.class,
                () -> controller.getPopularFilms(-1, null, 2000));
    }

    @Test
    void shouldTreatNullCountAsDefaultTen() {
        for (int i = 0; i < 11; i++) {
            createFilm(LocalDate.of(2000, 6, 1));
        }

        assertEquals(10, controller.getPopularFilms(null, null, 2000).size());
    }

    private Film createFilm(LocalDate releaseDate, Integer... genreIds) {
        Film film = new Film();
        film.setName("Film");
        film.setDescription("Description");
        film.setReleaseDate(releaseDate);
        film.setDuration(100);
        film.setMpa(new Mpa(1, "G"));

        LinkedHashSet<Genre> genres = new LinkedHashSet<>();
        for (Integer genreId : genreIds) {
            genres.add(new Genre(genreId, null));
        }
        film.setGenres(genres);

        return controller.createFilm(film);
    }
}
