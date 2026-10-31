package ru.yandex.practicum.filmorate.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.Operation;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

@SpringBootTest
@AutoConfigureTestDatabase
@Transactional
class FeedServiceTest {
    @Autowired
    private FeedService feedService;

    @Autowired
    private UserService userService;

    @Autowired
    private FilmService filmService;

    @Test
    void shouldWriteAndReturnEvent() {
        User user = userService.createUser(newUser("svc@mail.ru", "svc"));
        long before = System.currentTimeMillis();

        feedService.addEvent(user.getId(), EventType.REVIEW, Operation.UPDATE, 42);

        assertThat(feedService.getFeed(user.getId())).singleElement().satisfies(event -> {
            assertThat(event.getUserId()).isEqualTo(user.getId());
            assertThat(event.getEventType()).isEqualTo(EventType.REVIEW);
            assertThat(event.getOperation()).isEqualTo(Operation.UPDATE);
            assertThat(event.getEntityId()).isEqualTo(42);
            assertThat(event.getTimestamp()).isBetween(before, System.currentTimeMillis());
        });
    }

    @Test
    void shouldThrowNotFoundForUnknownUser() {
        assertThatThrownBy(() -> feedService.getFeed(9999))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void friendEventsShouldBeInFeed() {
        User first = userService.createUser(newUser("a@mail.ru", "a"));
        User second = userService.createUser(newUser("b@mail.ru", "b"));

        userService.addFriend(first.getId(), second.getId());
        userService.removeFriend(first.getId(), second.getId());

        assertThat(feedService.getFeed(first.getId()))
                .extracting(Event::getEventType, Event::getOperation, Event::getEntityId)
                .containsExactly(
                        tuple(EventType.FRIEND, Operation.ADD, second.getId()),
                        tuple(EventType.FRIEND, Operation.REMOVE, second.getId()));
        assertThat(feedService.getFeed(second.getId())).isEmpty();
    }

    @Test
    void repeatedLikeShouldWriteTwoEvents() {
        User user = userService.createUser(newUser("l@mail.ru", "l"));
        Film film = filmService.createFilm(newFilm("Feed film"));

        filmService.addLike(film.getId(), user.getId());
        filmService.addLike(film.getId(), user.getId());
        filmService.removeLike(film.getId(), user.getId());

        assertThat(feedService.getFeed(user.getId()))
                .extracting(Event::getEventType, Event::getOperation, Event::getEntityId)
                .containsExactly(
                        tuple(EventType.LIKE, Operation.ADD, film.getId()),
                        tuple(EventType.LIKE, Operation.ADD, film.getId()),
                        tuple(EventType.LIKE, Operation.REMOVE, film.getId()));
    }

    @Test
    void shouldNotWriteEventWhenFriendNotFound() {
        User user = userService.createUser(newUser("nf@mail.ru", "nf"));

        assertThatThrownBy(() -> userService.addFriend(user.getId(), 9999))
                .isInstanceOf(NotFoundException.class);
        assertThat(feedService.getFeed(user.getId())).isEmpty();
    }

    private User newUser(String email, String login) {
        User user = new User();
        user.setEmail(email);
        user.setLogin(login);
        user.setName(login);
        user.setBirthday(LocalDate.of(2000, 1, 1));
        return user;
    }

    private Film newFilm(String name) {
        Film film = new Film();
        film.setName(name);
        film.setDescription("Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(100);
        film.setMpa(new Mpa(1, null));
        return film;
    }
}
