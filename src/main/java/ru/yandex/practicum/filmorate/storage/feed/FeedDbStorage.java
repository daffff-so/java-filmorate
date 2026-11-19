package ru.yandex.practicum.filmorate.storage.feed;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Operation;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class FeedDbStorage implements FeedStorage {
    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Event> eventMapper = (rs, rowNum) -> Event.builder()
            .eventId(rs.getInt("event_id"))
            .userId(rs.getInt("user_id"))
            .eventType(EventType.valueOf(rs.getString("event_type")))
            .operation(Operation.valueOf(rs.getString("operation")))
            .entityId(rs.getInt("entity_id"))
            .timestamp(rs.getLong("event_timestamp"))
            .build();

    @Override
    public Event add(Event event) {
        String sql = """
                INSERT INTO events (user_id, event_type, operation, entity_id, event_timestamp)
                VALUES (?, ?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    sql, Statement.RETURN_GENERATED_KEYS
            );
            statement.setInt(1, event.getUserId());
            statement.setString(2, event.getEventType().name());
            statement.setString(3, event.getOperation().name());
            statement.setInt(4, event.getEntityId());
            statement.setLong(5, event.getTimestamp());
            return statement;
        }, keyHolder);

        event.setEventId(keyHolder.getKey().intValue());
        return event;
    }

    @Override
    public List<Event> getByUserId(Integer userId) {
        return jdbcTemplate.query("""
                SELECT event_id, user_id, event_type, operation, entity_id, event_timestamp
                FROM events
                WHERE user_id = ?
                ORDER BY event_id
                """, eventMapper, userId);
    }
}
