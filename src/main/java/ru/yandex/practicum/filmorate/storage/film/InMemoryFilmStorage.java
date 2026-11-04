package ru.yandex.practicum.filmorate.storage.film;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Optional;

@Component
public class InMemoryFilmStorage implements FilmStorage {
    private final Map<Integer, Film> films = new HashMap<>();
    private int nextId = 1;

    @Override
    public Film create(Film film) {
        film.setId(nextId++);
        films.put(film.getId(), film);
        return film;
    }

    @Override
    public Film update(Film film) {
        films.put(film.getId(), film);
        return film;
    }

    @Override
    public Optional<Film> getById(Integer id) {
        return Optional.ofNullable(films.get(id));
    }

    @Override
    public Collection<Film> getAll() {
        return films.values();
    }

    // Возвращает фильмы режиссёра, отсортированные по году или лайкам
    @Override
    public List<Film> getByDirector(Integer directorId, String sortBy) {
        java.util.Comparator<Film> comparator = "year".equals(sortBy)
                ? java.util.Comparator.comparing(Film::getReleaseDate)
                : java.util.Comparator.comparingInt((Film film) -> film.getLikes().size()).reversed();
        return films.values().stream()
                .filter(film -> film.getDirectors() != null && film.getDirectors().stream()
                        .anyMatch(d -> directorId.equals(d.getId())))
                .sorted(comparator.thenComparing(Film::getId)).toList();
    }

    @Override
    public void delete(Integer id) {
        films.remove(id);
    }
}
