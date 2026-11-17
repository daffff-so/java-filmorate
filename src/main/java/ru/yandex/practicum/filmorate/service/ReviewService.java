package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.review.ReviewStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {
    private static final String MISSED_ID_ERROR = "Id должен быть указан";

    @Qualifier("filmDbStorage")
    private final FilmStorage filmStorage;
    @Qualifier("userDbStorage")
    private final UserStorage userStorage;
    private final ReviewStorage reviewStorage;

    public Review create(Review review) {
        checkReviewFields(review);
        checkFilmId(review.getFilmId());
        checkUserId(review.getUserId());
        return reviewStorage.create(review);
    }

    public Review update(Review review) {
        checkReviewId(review.getId());
        checkReviewFields(review);
        checkFilmId(review.getFilmId());
        checkUserId(review.getUserId());
        return reviewStorage.update(review);
    }

    public void delete(Integer reviewId) {
        checkReviewId(reviewId);
        reviewStorage.delete(reviewId);
    }

    public Review getById(Integer reviewId) {
        checkReviewId(reviewId);
        return reviewStorage.getById(reviewId)
                .orElseThrow(() -> new NotFoundException("Отзыв с id = " + reviewId + " не найден"));
    }

    public List<Review> getByFilmId(Integer filmId, Integer count) {
        if (count <= 0) {
            log.error("Значение count должно быть больше нуля");
            throw new ValidationException("Значение count должно быть больше нуля");
        }
        if (filmId != null) {
            checkFilmId(filmId);
        }
        return reviewStorage.getByFilmId(filmId, count);
    }

    public void addReaction(Integer reviewId, Integer userId, boolean isUseful) {
        checkReviewId(reviewId);
        checkUserId(userId);
        reviewStorage.addReaction(reviewId, userId, isUseful);
    }

    public void removeReaction(Integer reviewId, Integer userId) {
        checkReviewId(reviewId);
        checkUserId(userId);
        reviewStorage.removeReaction(reviewId, userId);
    }

    private void checkFilmId(Integer filmId) {
        if (filmId == null) {
            log.error(MISSED_ID_ERROR);
            throw new ValidationException(MISSED_ID_ERROR);
        }
        if (filmStorage.getById(filmId).isEmpty()) {
            log.error("Фильм с id = {} не найден", filmId);
            throw new NotFoundException("Фильм с id = " + filmId + " не найден");
        }
    }

    private void checkUserId(Integer userId) {
        if (userId == null) {
            log.error(MISSED_ID_ERROR);
            throw new ValidationException(MISSED_ID_ERROR);
        }
        if (userStorage.getById(userId).isEmpty()) {
            log.error("Пользователь с id = {} не найден", userId);
            throw new NotFoundException("Пользователь с id = " + userId + " не найден");
        }
    }

    private void checkReviewId(Integer reviewId) {
        if (reviewId == null) {
            log.error(MISSED_ID_ERROR);
            throw new ValidationException(MISSED_ID_ERROR);
        }
        if (reviewStorage.getById(reviewId).isEmpty()) {
            log.error("Отзыв с id = {} не найден", reviewId);
            throw new NotFoundException("Отзыв с id = " + reviewId + " не найден");
        }
    }

    private void checkReviewFields(Review review) {
        if (review.getContent() == null || review.getContent().isBlank()) {
            log.error("Текст отзыва не может быть пустым");
            throw new ValidationException("Отзыв не может быть пустым");
        }
        if (review.getIsPositive() == null) {
            log.error("Тип отзыва должен быть заполнен");
            throw new ValidationException("Тип отзыва должен быть заполнен");
        }
    }
}
