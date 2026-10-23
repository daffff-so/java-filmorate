package ru.yandex.practicum.filmorate.storage.genre;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Genre;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class GenreDbStorage implements GenreStorage {
    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Genre> genreMapper = (rs, rowNum) ->
            new Genre(
                    rs.getInt("genre_id"),
                    rs.getString("name")
            );

    @Override
    public List<Genre> getAll() {
        return jdbcTemplate.query("""
                SELECT genre_id, name
                FROM genres
                ORDER BY genre_id
                """, genreMapper);
    }

    @Override
    public Optional<Genre> getById(Integer id) {
        return jdbcTemplate.query("""
                        SELECT genre_id, name
                        FROM genres
                        WHERE genre_id = ?
                        """,
                genreMapper,
                id
        ).stream().findFirst();
    }

    @Override
    public List<Genre> getByIds(Collection<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        String placeholders = String.join(
                ", ",
                Collections.nCopies(ids.size(), "?")
        );

        String sql = """
                SELECT genre_id, name
                FROM genres
                WHERE genre_id IN (%s)
                ORDER BY genre_id
                """.formatted(placeholders);

        return jdbcTemplate.query(
                sql,
                genreMapper,
                ids.toArray()
        );
    }
}