package ru.yandex.practicum.filmorate.storage.review;

import ru.yandex.practicum.filmorate.model.Review;

import java.util.List;
import java.util.Optional;

public interface ReviewStorage {
    Review create(Review review);

    Review update(Review review);

    void delete(Integer reviewId);

    Optional<Review> getById(Integer reviewId);

    List<Review> getByFilmId(Integer filmId, Integer count);

    void addReaction(Integer reviewId, Integer userId, boolean isUseful);

    void removeReaction(Integer reviewId, Integer userId);
}
