package ru.yandex.practicum.filmorate.storage;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.director.DirectorDbStorage;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@Import({DirectorDbStorage.class, FilmDbStorage.class})
class DirectorDbStorageTest {
    @Autowired
    private DirectorDbStorage directors;
    @Autowired
    private FilmDbStorage films;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldCreateUpdateAndDeleteDirector() {
        Director director = directors.create(new Director(null, "First"));
        assertThat(director.getId()).isPositive();
        assertThat(directors.getById(director.getId())).isPresent();
        director.setName("Updated");
        assertThat(directors.update(director).getName()).isEqualTo("Updated");
        directors.delete(director.getId());
        assertThat(directors.getById(director.getId())).isEmpty();
    }

    @Test
    void shouldPersistFilmDirectorsAndSortByYearAndLikes() {
        Director director = directors.create(new Director(null, "Director"));
        Film old = createFilm("Old", 2000, director);
        Film recent = createFilm("Recent", 2020, director);
        assertThat(films.getById(old.getId()).orElseThrow().getDirectors())
                .containsExactly(director);
        jdbcTemplate.update("INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)",
                "director-test@example.com", "director-viewer", "Viewer", LocalDate.of(1990, 1, 1));
        Integer viewerId = jdbcTemplate.queryForObject(
                "SELECT user_id FROM users WHERE email = ?", Integer.class, "director-test@example.com");
        films.addLike(recent.getId(), viewerId);

        assertThat(films.getByDirector(director.getId(), "year"))
                .extracting(Film::getId).containsExactly(old.getId(), recent.getId());
        assertThat(films.getByDirector(director.getId(), "likes"))
                .extracting(Film::getId).containsExactly(recent.getId(), old.getId());
        old.setDirectors(new LinkedHashSet<>());
        films.update(old);
        assertThat(films.getByDirector(director.getId(), "year"))
                .extracting(Film::getId).containsExactly(recent.getId());
    }

    private Film createFilm(String name, int year, Director director) {
        Film film = new Film();
        film.setName(name);
        film.setDescription(name);
        film.setReleaseDate(LocalDate.of(year, 1, 1));
        film.setDuration(120);
        film.setMpa(new Mpa(1, "G"));
        film.setDirectors(new LinkedHashSet<>(List.of(director)));
        return films.create(film);
    }
}
