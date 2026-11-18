package ru.yandex.practicum.filmorate.storage.review;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.exception.InternalServerException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Review;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ReviewDbStorage implements ReviewStorage {
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Review> reviewMapper = (rs, rowNum) -> {
        Review review = new Review();
        review.setId(rs.getInt("review_id"));
        review.setContent(rs.getString("content"));
        review.setIsPositive(rs.getBoolean("is_positive"));
        review.setUserId(rs.getInt("user_id"));
        review.setFilmId(rs.getInt("film_id"));
        review.setUseful(rs.getInt("useful"));
        return review;
    };

    @Override
    @Transactional
    public Review create(Review review) {
        String query = """
                INSERT INTO reviews (user_id, film_id, content, is_positive)
                VALUES (?, ?, ?, ?)
                """;
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con ->  {
            PreparedStatement ps = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, review.getUserId());
            ps.setInt(2, review.getFilmId());
            ps.setString(3, review.getContent());
            ps.setBoolean(4, review.getIsPositive());
            return ps;
        }, keyHolder);
        Integer id = keyHolder.getKeyAs(Integer.class);
        if (id == null) {
            log.error("Не удалось получить id отзыва");
            throw new InternalServerException("Не удалось получить id отзыва");
        }
        review.setId(id);
        log.info("Отзыв с Id={} успешно создан", review.getId());
        return getById(review.getId()).orElseThrow();
    }

    @Override
    @Transactional
    public Review update(Review review) {
        String query = """
                UPDATE reviews
                SET film_id=?, user_id=?, content=?, is_positive=?
                WHERE review_id=?
                """;
        int updated = jdbcTemplate.update(query,
                review.getFilmId(),
                review.getUserId(),
                review.getContent(),
                review.getIsPositive(),
                review.getId());
        if (updated == 0) {
            log.error("Отзыв с Id={} не найден", review.getId());
            throw new NotFoundException("Отзыв с id = " + review.getId() + " не найден");
        }
        log.info("Отзыв с Id={} успешно обновлен", review.getId());
        return getById(review.getId()).orElseThrow();
    }

    @Override
    @Transactional
    public void delete(Integer reviewId) {
        log.info("Удаление отзыва с id={}", reviewId);
        jdbcTemplate.update("DELETE FROM reviews WHERE review_id = ?", reviewId);
    }

    @Override
    public Optional<Review> getById(Integer reviewId) {
        log.info("Получение отзыва с id={}", reviewId);
        return jdbcTemplate.query("""
                        SELECT r.*,
                        SUM(CASE
                            WHEN rr.is_useful THEN 1
                            WHEN NOT rr.is_useful THEN -1
                            ELSE 0 END) AS useful
                        FROM reviews r
                        LEFT JOIN review_reactions rr ON rr.review_id=r.review_id
                        WHERE r.review_id=?
                        GROUP BY r.review_id, r.content, r.is_positive, r.user_id, r.film_id
                        """, reviewMapper, reviewId)
                .stream()
                .findFirst();
    }

    @Override
    public List<Review> getByFilmId(Integer filmId, Integer count) {
        log.info("Получение отзывов фильма с id={}", filmId);
        String baseQuery = """
                        SELECT r.*,
                        SUM(CASE
                            WHEN rr.is_useful THEN 1
                            WHEN NOT rr.is_useful THEN -1
                            ELSE 0 END) AS useful
                        FROM reviews r
                        LEFT JOIN review_reactions rr ON rr.review_id=r.review_id
                        """;
        String groupByPart = """
                        GROUP BY r.review_id, r.content, r.is_positive, r.user_id, r.film_id
                        ORDER BY useful DESC, r.review_id ASC
                        LIMIT ?
                        """;
        if (filmId == null) {
            // если фильм не указан то все
            return jdbcTemplate.query(baseQuery + groupByPart, reviewMapper, count);
        }
        return jdbcTemplate.query(baseQuery + "WHERE r.film_id=?" + groupByPart, reviewMapper, filmId, count);
    }

    @Override
    @Transactional
    public void addReaction(Integer reviewId, Integer userId, boolean isUseful) {
        log.info("Добавление реакции на отзыв с id={}", reviewId);
        jdbcTemplate.update("""
                MERGE INTO review_reactions (review_id, user_id, is_useful)
                VALUES (?, ?, ?)
                """, reviewId, userId, isUseful);
    }

    @Override
    @Transactional
    public void removeReaction(Integer reviewId, Integer userId) {
        log.info("Удаление реакции на отзыв с id={}", reviewId);
        jdbcTemplate.update("""
                DELETE FROM review_reactions
                WHERE review_id=? AND user_id=?
                """, reviewId, userId);
    }
}
