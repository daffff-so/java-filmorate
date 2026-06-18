package ru.yandex.practicum.filmorate.storage.film;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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
        if (film.getId() == null || !films.containsKey(film.getId())) {
            throw new NotFoundException("Фильм с id=" + film.getId() + " не найден");
        }

        films.put(film.getId(), film);
        return film;
    }

    @Override
    public Film getById(Integer id) {
        Film film = films.get(id);

        if (film == null) {
            throw new NotFoundException("Фильм с id=" + id + " не найден");
        }

        return film;
    }

    @Override
    public Collection<Film> getAll() {
        return films.values();
    }

    @Override
    public void delete(Integer id) {
        if (!films.containsKey(id)) {
            throw new NotFoundException("Фильм с id=" + id + " не найден");
        }

        films.remove(id);
    }
}