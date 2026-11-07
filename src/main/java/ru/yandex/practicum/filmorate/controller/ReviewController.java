package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.service.ReviewService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    @PostMapping
    public Review create(@RequestBody Review review) {
        return reviewService.create(review);
    }

    @PutMapping
    public Review update(@RequestBody Review review) {
        return reviewService.update(review);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        reviewService.delete(id);
    }

    @GetMapping("/{id}")
    public Review getReview(@PathVariable Integer id) {
        return reviewService.getById(id);
    }

    @GetMapping
    public List<Review> getByFilmId(@RequestParam(required = false) Integer filmId,
                                    @RequestParam(defaultValue = "10") Integer count) {
        return reviewService.getByFilmId(filmId, count);
    }

    @PutMapping("/{id}/like/{userId}")
    public void like(@PathVariable Integer id, @PathVariable Integer userId) {
        reviewService.addReaction(id, userId, true);
    }

    @PutMapping("/{id}/dislike/{userId}")
    public void dislike(@PathVariable Integer id, @PathVariable Integer userId) {
        reviewService.addReaction(id, userId, false);
    }

    @DeleteMapping("/{id}/like/{userId}")
    public void removeLike(@PathVariable Integer id, @PathVariable Integer userId) {
        reviewService.removeReaction(id, userId);
    }

    @DeleteMapping("/{id}/dislike/{userId}")
    public void removeDislike(@PathVariable Integer id, @PathVariable Integer userId) {
        reviewService.removeReaction(id, userId);
    }

}
