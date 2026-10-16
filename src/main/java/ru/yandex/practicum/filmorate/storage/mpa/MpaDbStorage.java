package ru.yandex.practicum.filmorate.storage.mpa;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MpaDbStorage implements MpaStorage {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<Mpa> getAll() {
        return jdbcTemplate.query("""
                SELECT mpa_id, name
                FROM mpa
                ORDER BY mpa_id
                """, (rs, rowNum) ->
                new Mpa(rs.getInt("mpa_id"), rs.getString("name")));
    }

    @Override
    public Optional<Mpa> getById(Integer id) {
        return jdbcTemplate.query("""
                        SELECT mpa_id, name
                        FROM mpa
                        WHERE mpa_id = ?
                        """,
                (rs, rowNum) ->
                        new Mpa(rs.getInt("mpa_id"), rs.getString("name")),
                id
        ).stream().findFirst();
    }
}