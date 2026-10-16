package ru.yandex.practicum.filmorate.storage;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.genre.GenreDbStorage;
import ru.yandex.practicum.filmorate.storage.mpa.MpaDbStorage;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase
@Import({
        UserDbStorage.class,
        FilmDbStorage.class,
        GenreDbStorage.class,
        MpaDbStorage.class
})
class DbStorageTest {
    private final UserDbStorage userStorage;
    private final FilmDbStorage filmStorage;
    private final GenreDbStorage genreStorage;
    private final MpaDbStorage mpaStorage;

    @Autowired
    DbStorageTest(
            UserDbStorage userStorage,
            FilmDbStorage filmStorage,
            GenreDbStorage genreStorage,
            MpaDbStorage mpaStorage
    ) {
        this.userStorage = userStorage;
        this.filmStorage = filmStorage;
        this.genreStorage = genreStorage;
        this.mpaStorage = mpaStorage;
    }

    @Test
    void shouldCreateFindAndUpdateUser() {
        User user = createUser("one@mail.ru", "one");

        assertThat(user.getId()).isPositive();
        assertThat(userStorage.getById(user.getId()))
                .isPresent()
                .hasValueSatisfying(found ->
                        assertThat(found.getEmail()).isEqualTo("one@mail.ru"));

        user.setName("New name");
        User updated = userStorage.update(user);

        assertThat(updated.getName()).isEqualTo("New name");
        assertThat(userStorage.getAll()).hasSize(1);

        userStorage.delete(user.getId());
        assertThat(userStorage.getById(user.getId())).isEmpty();
    }

    @Test
    void friendshipShouldBeOneDirectional() {
        User first = createUser("first@mail.ru", "first");
        User second = createUser("second@mail.ru", "second");

        userStorage.addFriend(first.getId(), second.getId());

        assertThat(userStorage.getFriends(first.getId()))
                .extracting(User::getId)
                .containsExactly(second.getId());

        assertThat(userStorage.getFriends(second.getId())).isEmpty();

        userStorage.removeFriend(first.getId(), second.getId());
        assertThat(userStorage.getFriends(first.getId())).isEmpty();
    }

    @Test
    void shouldFindCommonFriends() {
        User first = createUser("first@mail.ru", "first");
        User second = createUser("second@mail.ru", "second");
        User common = createUser("common@mail.ru", "common");

        userStorage.addFriend(first.getId(), common.getId());
        userStorage.addFriend(second.getId(), common.getId());

        assertThat(userStorage.getCommonFriends(
                first.getId(), second.getId()
        )).extracting(User::getId).containsExactly(common.getId());
    }

    @Test
    void shouldCreateFilmWithGenresAndMpa() {
        Film film = createFilm("Film one");

        assertThat(film.getId()).isPositive();
        assertThat(film.getMpa().getName()).isEqualTo("PG");
        assertThat(film.getGenres())
                .extracting(Genre::getName)
                .containsExactly("Комедия", "Драма");

        assertThat(filmStorage.getById(film.getId())).isPresent();
        assertThat(filmStorage.getAll()).hasSize(1);

        film.setName("Updated film");
        Film updated = filmStorage.update(film);
        assertThat(updated.getName()).isEqualTo("Updated film");

        filmStorage.delete(film.getId());
        assertThat(filmStorage.getById(film.getId())).isEmpty();
    }

    @Test
    void shouldSaveLikesAndReturnPopularFilms() {
        User user = createUser("viewer@mail.ru", "viewer");
        Film first = createFilm("First");
        Film second = createFilm("Second");

        filmStorage.addLike(second.getId(), user.getId());

        List<Film> popular = filmStorage.getPopular(10);
        assertThat(popular.getFirst().getId()).isEqualTo(second.getId());

        filmStorage.removeLike(second.getId(), user.getId());

        assertThat(filmStorage.getById(second.getId()))
                .isPresent()
                .hasValueSatisfying(film ->
                        assertThat(film.getLikes()).isEmpty());
    }

    @Test
    void shouldReturnAllGenresAndMpaRatings() {
        assertThat(genreStorage.getAll()).hasSize(6);
        assertThat(genreStorage.getById(1))
                .isPresent()
                .hasValueSatisfying(genre ->
                        assertThat(genre.getName()).isEqualTo("Комедия"));

        assertThat(mpaStorage.getAll()).hasSize(5);
        assertThat(mpaStorage.getById(3))
                .isPresent()
                .hasValueSatisfying(mpa ->
                        assertThat(mpa.getName()).isEqualTo("PG-13"));

        assertThat(genreStorage.getById(100)).isEmpty();
        assertThat(mpaStorage.getById(100)).isEmpty();
    }

    private User createUser(String email, String login) {
        User user = new User();
        user.setEmail(email);
        user.setLogin(login);
        user.setName(login);
        user.setBirthday(LocalDate.of(1990, 1, 1));
        return userStorage.create(user);
    }

    private Film createFilm(String name) {
        Film film = new Film();
        film.setName(name);
        film.setDescription("Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(100);
        film.setMpa(new Mpa(2, null));
        film.setGenres(new LinkedHashSet<>(List.of(
                new Genre(1, null),
                new Genre(2, null)
        )));
        return filmStorage.create(film);
    }
}
