package ru.yandex.practicum.filmorate.storage.film;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository("filmDbStorage")
@RequiredArgsConstructor
public class FilmDbStorage implements FilmStorage {
    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Film> filmMapper = (rs, rowNum) -> {
        Film film = new Film();
        film.setId(rs.getInt("film_id"));
        film.setName(rs.getString("film_name"));
        film.setDescription(rs.getString("description"));
        film.setReleaseDate(rs.getDate("release_date").toLocalDate());
        film.setDuration(rs.getInt("duration"));
        film.setMpa(new Mpa(rs.getInt("mpa_id"), rs.getString("mpa_name")));
        return film;
    };

    private final RowMapper<Genre> genreMapper = (rs, rowNum) ->
            new Genre(rs.getInt("genre_id"), rs.getString("name"));

    @Override
    @Transactional
    public Film create(Film film) {
        validateReferences(film);

        String sql = """
                INSERT INTO films
                    (name, description, release_date, duration, mpa_id)
                VALUES (?, ?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    sql, Statement.RETURN_GENERATED_KEYS
            );
            statement.setString(1, film.getName());
            statement.setString(2, film.getDescription());
            statement.setDate(3, Date.valueOf(film.getReleaseDate()));
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
        validateReferences(film);

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
        enrichFilm(film);
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

        films.forEach(this::enrichFilm);
        return films;
    }

    @Override
    public void delete(Integer id) {
        jdbcTemplate.update("DELETE FROM films WHERE film_id = ?", id);
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
                LEFT JOIN film_likes fl ON fl.film_id = f.film_id
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

        films.forEach(this::enrichFilm);
        return films;
    }

    private void enrichFilm(Film film) {
        List<Genre> genres = jdbcTemplate.query("""
                SELECT g.genre_id, g.name
                FROM genres g
                JOIN film_genres fg ON fg.genre_id = g.genre_id
                WHERE fg.film_id = ?
                ORDER BY g.genre_id
                """, genreMapper, film.getId());

        film.setGenres(new LinkedHashSet<>(genres));

        Set<Integer> likes = new LinkedHashSet<>(jdbcTemplate.queryForList("""
                SELECT user_id
                FROM film_likes
                WHERE film_id = ?
                ORDER BY user_id
                """, Integer.class, film.getId()));

        film.setLikes(likes);
    }

    private void saveGenres(Film film) {
        if (film.getGenres() == null) {
            return;
        }

        for (Genre genre : film.getGenres()) {
            jdbcTemplate.update("""
                    INSERT INTO film_genres (film_id, genre_id)
                    VALUES (?, ?)
                    """, film.getId(), genre.getId());
        }
    }

    private void validateReferences(Film film) {
        if (film.getMpa() == null || film.getMpa().getId() == null) {
            throw new NotFoundException("Рейтинг MPA не указан");
        }

        if (!exists("SELECT COUNT(*) FROM mpa WHERE mpa_id = ?",
                film.getMpa().getId())) {
            throw new NotFoundException(
                    "Рейтинг MPA с id=" + film.getMpa().getId() + " не найден"
            );
        }

        if (film.getGenres() == null) {
            return;
        }

        for (Genre genre : film.getGenres()) {
            if (genre == null || genre.getId() == null ||
                    !exists("SELECT COUNT(*) FROM genres WHERE genre_id = ?",
                            genre == null ? null : genre.getId())) {
                throw new NotFoundException("Указан несуществующий жанр");
            }
        }
    }

    private boolean exists(String sql, Integer id) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }
}
