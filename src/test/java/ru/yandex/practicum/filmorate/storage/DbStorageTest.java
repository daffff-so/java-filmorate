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

        List<Film> popular = filmStorage.getPopular(10, null, null);
        assertThat(popular.getFirst().getId()).isEqualTo(second.getId());

        filmStorage.removeLike(second.getId(), user.getId());

        assertThat(filmStorage.getById(second.getId()))
                .isPresent()
                .hasValueSatisfying(film ->
                        assertThat(film.getLikes()).isEmpty());
    }

    @Test
    void shouldFilterPopularByGenre() {
        User first = createUser("first@mail.ru", "first");
        User second = createUser("second@mail.ru", "second");
        User third = createUser("third@mail.ru", "third");
        Film comedyA = createFilm("Comedy A", LocalDate.of(2000, 1, 1), 1);
        Film drama = createFilm("Drama", LocalDate.of(2000, 1, 1), 2);
        Film comedyB = createFilm("Comedy B", LocalDate.of(2005, 1, 1), 1);
        Film comedyC = createFilm("Comedy C", LocalDate.of(2005, 1, 1), 1);

        like(drama, first, second, third);
        like(comedyB, first, second);

        assertThat(filmStorage.getPopular(10, 1, null))
                .extracting(Film::getId)
                .containsExactly(
                        comedyB.getId(),
                        comedyA.getId(),
                        comedyC.getId()
                );
    }

    @Test
    void shouldFilterPopularByYear() {
        User first = createUser("first@mail.ru", "first");
        User second = createUser("second@mail.ru", "second");
        User third = createUser("third@mail.ru", "third");
        Film a = createFilm("A", LocalDate.of(2000, 6, 1), 1);
        Film b = createFilm("B", LocalDate.of(2001, 6, 1), 1);
        Film c = createFilm("C", LocalDate.of(2000, 6, 1), 1);

        like(a, first);
        like(b, first, second, third);
        like(c, first, second);

        assertThat(filmStorage.getPopular(10, null, 2000))
                .extracting(Film::getId)
                .containsExactly(c.getId(), a.getId());
    }

    @Test
    void shouldFilterPopularByGenreAndYear() {
        User first = createUser("first@mail.ru", "first");
        User second = createUser("second@mail.ru", "second");
        User third = createUser("third@mail.ru", "third");
        Film match = createFilm("Match", LocalDate.of(2000, 6, 1), 1);
        Film wrongYear = createFilm("Wrong year", LocalDate.of(2001, 6, 1), 1);
        Film wrongGenre = createFilm("Wrong genre", LocalDate.of(2000, 6, 1), 2);
        Film match2 = createFilm("Match 2", LocalDate.of(2000, 6, 1), 1, 2);

        like(match, first);
        like(wrongYear, first, second, third);
        like(wrongGenre, first, second, third);
        like(match2, first, second);

        assertThat(filmStorage.getPopular(10, 1, 2000))
                .extracting(Film::getId)
                .containsExactly(match2.getId(), match.getId());
    }

    @Test
    void shouldApplyCountAfterFilters() {
        User first = createUser("first@mail.ru", "first");
        User second = createUser("second@mail.ru", "second");
        User third = createUser("third@mail.ru", "third");
        Film top = createFilm("Top", LocalDate.of(2000, 6, 1), 2);
        Film comedy = createFilm("Comedy", LocalDate.of(2000, 6, 1), 1);
        createFilm("Comedy 2", LocalDate.of(2000, 6, 1), 1);

        like(top, first, second, third);
        like(comedy, first);

        assertThat(filmStorage.getPopular(1, 1, null))
                .extracting(Film::getId)
                .containsExactly(comedy.getId());

        assertThat(filmStorage.getPopular(1, null, 2000))
                .extracting(Film::getId)
                .containsExactly(top.getId());
    }

    @Test
    void shouldNotDuplicateOrInflateMultiGenreFilm() {
        User first = createUser("first@mail.ru", "first");
        User second = createUser("second@mail.ru", "second");
        User third = createUser("third@mail.ru", "third");
        Film multi = createFilm("Multi", LocalDate.of(2000, 6, 1), 1, 2, 3);
        Film single = createFilm("Single", LocalDate.of(2000, 6, 1), 1);

        like(multi, first, second);
        like(single, first, second, third);

        List<Film> popular = filmStorage.getPopular(10, 1, null);

        assertThat(popular)
                .extracting(Film::getId)
                .containsExactly(single.getId(), multi.getId());
        assertThat(popular.get(1).getGenres())
                .extracting(Genre::getId)
                .containsExactly(1, 2, 3);
        assertThat(popular.get(1).getLikes()).hasSize(2);

        assertThat(filmStorage.getPopular(10, null, null))
                .extracting(Film::getId)
                .containsExactly(single.getId(), multi.getId());
    }

    @Test
    void shouldFilterByYearBoundaries() {
        createFilm("Before", LocalDate.of(1999, 12, 31), 1);
        Film jan1 = createFilm("Jan 1", LocalDate.of(2000, 1, 1), 1);
        Film dec31 = createFilm("Dec 31", LocalDate.of(2000, 12, 31), 1);
        createFilm("After", LocalDate.of(2001, 1, 1), 1);

        assertThat(filmStorage.getPopular(10, null, 2000))
                .extracting(Film::getId)
                .containsExactly(jan1.getId(), dec31.getId());
    }

    @Test
    void shouldReturnEmptyForUnknownGenreOrYearWithoutFilms() {
        createFilm("Comedy", LocalDate.of(2000, 6, 1), 1);

        assertThat(filmStorage.getPopular(10, 999, null)).isEmpty();
        assertThat(filmStorage.getPopular(10, null, 1990)).isEmpty();
        assertThat(filmStorage.getPopular(10, 3, null)).isEmpty();
        assertThat(filmStorage.getPopular(10, 1, 1990)).isEmpty();
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

        assertThat(genreStorage.getByIds(List.of(1, 2)))
                .extracting(Genre::getId)
                .containsExactly(1, 2);

        assertThat(genreStorage.getByIds(List.of()))
                .isEmpty();
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

    private Film createFilm(
            String name,
            LocalDate releaseDate,
            Integer... genreIds
    ) {
        Film film = new Film();
        film.setName(name);
        film.setDescription("Description");
        film.setReleaseDate(releaseDate);
        film.setDuration(100);
        film.setMpa(new Mpa(2, null));

        LinkedHashSet<Genre> genres = new LinkedHashSet<>();
        for (Integer genreId : genreIds) {
            genres.add(new Genre(genreId, null));
        }
        film.setGenres(genres);

        return filmStorage.create(film);
    }

    private void like(Film film, User... users) {
        for (User user : users) {
            filmStorage.addLike(film.getId(), user.getId());
        }
    }
}
