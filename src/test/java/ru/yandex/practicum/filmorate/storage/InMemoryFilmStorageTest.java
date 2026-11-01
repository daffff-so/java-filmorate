package ru.yandex.practicum.filmorate.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.film.InMemoryFilmStorage;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryFilmStorageTest {
    private InMemoryFilmStorage filmStorage;

    @BeforeEach
    void setUp() {
        filmStorage = new InMemoryFilmStorage();
    }

    @Test
    void shouldFilterByGenreAndYearInMemory() {
        Film a = createFilm(LocalDate.of(2000, 6, 1), Set.of(1), 1);
        Film b = createFilm(LocalDate.of(2000, 6, 1), Set.of(1, 2, 3), 2);
        Film c = createFilm(LocalDate.of(2001, 6, 1), Set.of(1, 2), 1);
        Film d = createFilm(LocalDate.of(2000, 6, 1), Set.of(1, 2), 1, 2);

        assertThat(filmStorage.getPopular(10, 1, 2000))
                .extracting(Film::getId)
                .containsExactly(d.getId(), a.getId());

        assertThat(filmStorage.getPopular(10, 1, null))
                .extracting(Film::getId)
                .containsExactly(c.getId(), d.getId(), a.getId());

        assertThat(filmStorage.getPopular(1, null, 2000))
                .extracting(Film::getId)
                .containsExactly(b.getId());
    }

    @Test
    void shouldOrderEqualLikesByIdInMemory() {
        for (int i = 0; i < 17; i++) {
            Film film = createFilm(LocalDate.of(2000, 6, 1), Set.of(1));
            if (film.getId() != 2 && film.getId() != 17) {
                filmStorage.delete(film.getId());
            }
        }

        assertThat(filmStorage.getPopular(10, null, null))
                .extracting(Film::getId)
                .containsExactly(2, 17);
    }

    @Test
    void shouldFilterByYearBoundariesInMemory() {
        createFilm(LocalDate.of(1999, 12, 31), Set.of());
        Film jan1 = createFilm(LocalDate.of(2000, 1, 1), Set.of());
        Film dec31 = createFilm(LocalDate.of(2000, 12, 31), Set.of());
        createFilm(LocalDate.of(2001, 1, 1), Set.of());

        assertThat(filmStorage.getPopular(10, null, 2000))
                .extracting(Film::getId)
                .containsExactly(jan1.getId(), dec31.getId());
    }

    @Test
    void shouldReturnEmptyInMemoryForUnknownGenre() {
        createFilm(LocalDate.of(2000, 6, 1), Set.of(1), 1);

        assertThat(filmStorage.getPopular(10, 999, null)).isEmpty();
        assertThat(filmStorage.getPopular(10, null, 1990)).isEmpty();
    }

    @Test
    void shouldNotFailOnFilmWithNullGenresInMemory() {
        Film film = createFilm(LocalDate.of(2000, 6, 1), Set.of());
        film.setGenres(null);

        assertThat(filmStorage.getPopular(10, 1, null)).isEmpty();
        assertThat(filmStorage.getPopular(10, null, null))
                .extracting(Film::getId)
                .containsExactly(film.getId());
    }

    private Film createFilm(
            LocalDate releaseDate,
            Set<Integer> likes,
            Integer... genreIds
    ) {
        Film film = new Film();
        film.setName("Film");
        film.setReleaseDate(releaseDate);
        film.setDuration(100);
        film.setLikes(new HashSet<>(likes));

        LinkedHashSet<Genre> genres = new LinkedHashSet<>();
        for (Integer genreId : genreIds) {
            genres.add(new Genre(genreId, null));
        }
        film.setGenres(genres);

        return filmStorage.create(film);
    }
}
