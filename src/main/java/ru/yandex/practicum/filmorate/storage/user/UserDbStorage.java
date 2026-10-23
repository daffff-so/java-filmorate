package ru.yandex.practicum.filmorate.storage.user;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository("userDbStorage")
@RequiredArgsConstructor
public class UserDbStorage implements UserStorage {
    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<User> userMapper = (rs, rowNum) -> {
        User user = new User();
        user.setId(rs.getInt("user_id"));
        user.setEmail(rs.getString("email"));
        user.setLogin(rs.getString("login"));
        user.setName(rs.getString("name"));
        user.setBirthday(rs.getDate("birthday").toLocalDate());
        return user;
    };

    @Override
    public User create(User user) {
        String sql = """
                INSERT INTO users (email, login, name, birthday)
                VALUES (?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    sql, Statement.RETURN_GENERATED_KEYS
            );
            statement.setString(1, user.getEmail());
            statement.setString(2, user.getLogin());
            statement.setString(3, user.getName());
            statement.setDate(4, Date.valueOf(user.getBirthday()));
            return statement;
        }, keyHolder);

        user.setId(keyHolder.getKey().intValue());
        return user;
    }

    @Override
    public User update(User user) {
        jdbcTemplate.update("""
                        UPDATE users
                        SET email = ?, login = ?, name = ?, birthday = ?
                        WHERE user_id = ?
                        """,
                user.getEmail(),
                user.getLogin(),
                user.getName(),
                Date.valueOf(user.getBirthday()),
                user.getId()
        );

        return getById(user.getId()).orElseThrow();
    }

    @Override
    public Optional<User> getById(Integer id) {
        return jdbcTemplate.query("""
                        SELECT user_id, email, login, name, birthday
                        FROM users
                        WHERE user_id = ?
                        """,
                userMapper,
                id
        ).stream().findFirst();
    }

    @Override
    public Collection<User> getAll() {
        return jdbcTemplate.query("""
                SELECT user_id, email, login, name, birthday
                FROM users
                ORDER BY user_id
                """, userMapper);
    }

    @Override
    public void delete(Integer id) {
        jdbcTemplate.update("DELETE FROM users WHERE user_id = ?", id);
    }

    @Override
    @Transactional
    public void addFriend(Integer userId, Integer friendId) {
        jdbcTemplate.update("""
                MERGE INTO friendships (user_id, friend_id, confirmed)
                KEY (user_id, friend_id)
                VALUES (?, ?, FALSE)
                """, userId, friendId);

        Integer reverseRequestCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM friendships
                WHERE user_id = ? AND friend_id = ?
                """, Integer.class, friendId, userId);

        if (reverseRequestCount != null && reverseRequestCount > 0) {
            jdbcTemplate.update("""
                    UPDATE friendships
                    SET confirmed = TRUE
                    WHERE (user_id = ? AND friend_id = ?)
                       OR (user_id = ? AND friend_id = ?)
                    """, userId, friendId, friendId, userId);
        }
    }

    @Override
    @Transactional
    public void removeFriend(Integer userId, Integer friendId) {
        jdbcTemplate.update("""
                DELETE FROM friendships
                WHERE user_id = ? AND friend_id = ?
                """, userId, friendId);

        jdbcTemplate.update("""
                UPDATE friendships
                SET confirmed = FALSE
                WHERE user_id = ? AND friend_id = ?
                """, friendId, userId);
    }

    @Override
    public List<User> getFriends(Integer userId) {
        return jdbcTemplate.query("""
                SELECT u.user_id, u.email, u.login, u.name, u.birthday
                FROM users u
                JOIN friendships f ON f.friend_id = u.user_id
                WHERE f.user_id = ?
                ORDER BY u.user_id
                """, userMapper, userId);
    }

    @Override
    public List<User> getCommonFriends(Integer userId, Integer otherId) {
        return jdbcTemplate.query("""
                SELECT u.user_id, u.email, u.login, u.name, u.birthday
                FROM users u
                JOIN friendships first_friend
                    ON first_friend.friend_id = u.user_id
                JOIN friendships second_friend
                    ON second_friend.friend_id = u.user_id
                WHERE first_friend.user_id = ?
                  AND second_friend.user_id = ?
                ORDER BY u.user_id
                """, userMapper, userId, otherId);
    }
}
