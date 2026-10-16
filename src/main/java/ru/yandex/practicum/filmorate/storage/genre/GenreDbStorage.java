package ru.yandex.practicum.filmorate.storage.genre;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Genre;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class GenreDbStorage implements GenreStorage {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<Genre> getAll() {
        return jdbcTemplate.query("""
                SELECT genre_id, name
                FROM genres
                ORDER BY genre_id
                """, (rs, rowNum) ->
                new Genre(rs.getInt("genre_id"), rs.getString("name")));
    }

    @Override
    public Optional<Genre> getById(Integer id) {
        return jdbcTemplate.query("""
                        SELECT genre_id, name
                        FROM genres
                        WHERE genre_id = ?
                        """,
                (rs, rowNum) ->
                        new Genre(rs.getInt("genre_id"), rs.getString("name")),
                id
        ).stream().findFirst();
    }
}