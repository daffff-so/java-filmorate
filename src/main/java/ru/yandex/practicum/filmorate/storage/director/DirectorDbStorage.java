package ru.yandex.practicum.filmorate.storage.director;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Director;

import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
// Класс для работы с режиссёрами в базе данных
public class DirectorDbStorage implements DirectorStorage {

    // Объект для выполнения SQL-запросов
    private final JdbcTemplate jdbcTemplate;

    // Преобразователь строки таблицы в объект Directo
    private final RowMapper<Director> mapper = (rs, rowNum) ->
            new Director(rs.getInt("director_id"), rs.getString("name"));

    @Override
    // Получение списка всех режиссёров
    public List<Director> getAll() {
        return jdbcTemplate.query("SELECT director_id, name FROM directors ORDER BY director_id", mapper);
    }

    @Override
    // Получение режиссёра по идентификатору
    public Optional<Director> getById(Integer id) {
        return jdbcTemplate.query("SELECT director_id, name FROM directors WHERE director_id = ?", mapper, id)
                .stream().findFirst();
    }

    @Override
    // Создание нового режиссёра
    public Director create(Director director) {
        KeyHolder holder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement("INSERT INTO directors (name) VALUES (?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, director.getName());
            return statement;
        }, holder);
        director.setId(holder.getKey().intValue());
        return getById(director.getId()).orElseThrow();
    }

    @Override
    // Обновление информации о режиссёре
    public Director update(Director director) {
        jdbcTemplate.update("UPDATE directors SET name = ? WHERE director_id = ?",
                director.getName(), director.getId());
        return getById(director.getId()).orElseThrow();
    }

    @Override
    // Удаление режиссёра по идентификатору
    public void delete(Integer id) {
        jdbcTemplate.update("DELETE FROM directors WHERE director_id = ?", id);
    }
}
