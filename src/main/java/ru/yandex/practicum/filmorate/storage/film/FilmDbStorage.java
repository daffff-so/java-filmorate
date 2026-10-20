package ru.yandex.practicum.filmorate.storage.film;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository("filmDbStorage")
@RequiredArgsConstructor
public class FilmDbStorage implements FilmStorage {
    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Film> filmMapper = (rs, rowNum) -> {
        Film film = new Film();
        film.setId(rs.getInt("film_id"));
        film.setName(rs.getString("film_name"));
        film.setDescription(rs.getString("description"));
        film.setReleaseDate(
                rs.getDate("release_date").toLocalDate()
        );
        film.setDuration(rs.getInt("duration"));
        film.setMpa(new Mpa(
                rs.getInt("mpa_id"),
                rs.getString("mpa_name")
        ));
        return film;
    };

    @Override
    @Transactional
    public Film create(Film film) {
        String sql = """
                INSERT INTO films
                    (name, description, release_date, duration, mpa_id)
                VALUES (?, ?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement statement =
                    connection.prepareStatement(
                            sql,
                            Statement.RETURN_GENERATED_KEYS
                    );

            statement.setString(1, film.getName());
            statement.setString(2, film.getDescription());
            statement.setDate(
                    3,
                    Date.valueOf(film.getReleaseDate())
            );
            statement.setInt(4, film.getDuration());
            statement.setInt(5, film.getMpa().getId());

            return statement;
        }, keyHolder);

        film.setId(keyHolder.getKey().intValue());
        saveGenres(film);

        return getById(film.getId()).orElseThrow();
    }

    @Override
    @Transactional
    public Film update(Film film) {
        jdbcTemplate.update("""
                        UPDATE films
                        SET name = ?,
                            description = ?,
                            release_date = ?,
                            duration = ?,
                            mpa_id = ?
                        WHERE film_id = ?
                        """,
                film.getName(),
                film.getDescription(),
                Date.valueOf(film.getReleaseDate()),
                film.getDuration(),
                film.getMpa().getId(),
                film.getId()
        );

        jdbcTemplate.update(
                "DELETE FROM film_genres WHERE film_id = ?",
                film.getId()
        );

        saveGenres(film);

        return getById(film.getId()).orElseThrow();
    }

    @Override
    public Optional<Film> getById(Integer id) {
        List<Film> films = jdbcTemplate.query("""
                SELECT f.film_id,
                       f.name AS film_name,
                       f.description,
                       f.release_date,
                       f.duration,
                       m.mpa_id,
                       m.name AS mpa_name
                FROM films f
                JOIN mpa m ON m.mpa_id = f.mpa_id
                WHERE f.film_id = ?
                """, filmMapper, id);

        if (films.isEmpty()) {
            return Optional.empty();
        }

        Film film = films.getFirst();
        enrichFilms(List.of(film));

        return Optional.of(film);
    }

    @Override
    public Collection<Film> getAll() {
        List<Film> films = jdbcTemplate.query("""
                SELECT f.film_id,
                       f.name AS film_name,
                       f.description,
                       f.release_date,
                       f.duration,
                       m.mpa_id,
                       m.name AS mpa_name
                FROM films f
                JOIN mpa m ON m.mpa_id = f.mpa_id
                ORDER BY f.film_id
                """, filmMapper);

        enrichFilms(films);
        return films;
    }

    @Override
    public void delete(Integer id) {
        jdbcTemplate.update(
                "DELETE FROM films WHERE film_id = ?",
                id
        );
    }

    @Override
    public void addLike(Integer filmId, Integer userId) {
        jdbcTemplate.update("""
                MERGE INTO film_likes (film_id, user_id)
                KEY (film_id, user_id)
                VALUES (?, ?)
                """, filmId, userId);
    }

    @Override
    public void removeLike(Integer filmId, Integer userId) {
        jdbcTemplate.update("""
                DELETE FROM film_likes
                WHERE film_id = ? AND user_id = ?
                """, filmId, userId);
    }

    @Override
    public List<Film> getPopular(Integer count) {
        List<Film> films = jdbcTemplate.query("""
                SELECT f.film_id,
                       f.name AS film_name,
                       f.description,
                       f.release_date,
                       f.duration,
                       m.mpa_id,
                       m.name AS mpa_name
                FROM films f
                JOIN mpa m ON m.mpa_id = f.mpa_id
                LEFT JOIN film_likes fl
                    ON fl.film_id = f.film_id
                GROUP BY f.film_id,
                         f.name,
                         f.description,
                         f.release_date,
                         f.duration,
                         m.mpa_id,
                         m.name
                ORDER BY COUNT(fl.user_id) DESC, f.film_id
                LIMIT ?
                """, filmMapper, count);

        enrichFilms(films);
        return films;
    }

    private void saveGenres(Film film) {
        if (film.getGenres() == null
                || film.getGenres().isEmpty()) {
            return;
        }

        List<Object[]> batchArguments = film.getGenres().stream()
                .map(genre -> new Object[]{
                        film.getId(),
                        genre.getId()
                })
                .toList();

        jdbcTemplate.batchUpdate("""
                INSERT INTO film_genres (film_id, genre_id)
                VALUES (?, ?)
                """, batchArguments);
    }

    private void enrichFilms(List<Film> films) {
        if (films == null || films.isEmpty()) {
            return;
        }

        List<Integer> filmIds = films.stream()
                .map(Film::getId)
                .toList();

        String placeholders = String.join(
                ", ",
                Collections.nCopies(filmIds.size(), "?")
        );

        Map<Integer, LinkedHashSet<Genre>> genresByFilm =
                loadGenres(filmIds, placeholders);

        Map<Integer, LinkedHashSet<Integer>> likesByFilm =
                loadLikes(filmIds, placeholders);

        for (Film film : films) {
            LinkedHashSet<Genre> genres =
                    genresByFilm.get(film.getId());

            LinkedHashSet<Integer> likes =
                    likesByFilm.get(film.getId());

            film.setGenres(
                    genres == null
                            ? new LinkedHashSet<>()
                            : new LinkedHashSet<>(genres)
            );

            film.setLikes(
                    likes == null
                            ? new LinkedHashSet<>()
                            : new LinkedHashSet<>(likes)
            );
        }
    }

    private Map<Integer, LinkedHashSet<Genre>> loadGenres(
            List<Integer> filmIds,
            String placeholders
    ) {
        String sql = """
                SELECT fg.film_id,
                       g.genre_id,
                       g.name
                FROM film_genres fg
                JOIN genres g ON g.genre_id = fg.genre_id
                WHERE fg.film_id IN (%s)
                ORDER BY fg.film_id, g.genre_id
                """.formatted(placeholders);

        List<FilmGenreRow> rows = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new FilmGenreRow(
                        rs.getInt("film_id"),
                        new Genre(
                                rs.getInt("genre_id"),
                                rs.getString("name")
                        )
                ),
                filmIds.toArray()
        );

        Map<Integer, LinkedHashSet<Genre>> result =
                new HashMap<>();

        for (FilmGenreRow row : rows) {
            result.computeIfAbsent(
                    row.filmId(),
                    ignored -> new LinkedHashSet<>()
            ).add(row.genre());
        }

        return result;
    }

    private Map<Integer, LinkedHashSet<Integer>> loadLikes(
            List<Integer> filmIds,
            String placeholders
    ) {
        String sql = """
                SELECT film_id, user_id
                FROM film_likes
                WHERE film_id IN (%s)
                ORDER BY film_id, user_id
                """.formatted(placeholders);

        List<FilmLikeRow> rows = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new FilmLikeRow(
                        rs.getInt("film_id"),
                        rs.getInt("user_id")
                ),
                filmIds.toArray()
        );

        Map<Integer, LinkedHashSet<Integer>> result =
                new HashMap<>();

        for (FilmLikeRow row : rows) {
            result.computeIfAbsent(
                    row.filmId(),
                    ignored -> new LinkedHashSet<>()
            ).add(row.userId());
        }

        return result;
    }

    private record FilmGenreRow(
            Integer filmId,
            Genre genre
    ) {
    }

    private record FilmLikeRow(
            Integer filmId,
            Integer userId
    ) {
    }
}