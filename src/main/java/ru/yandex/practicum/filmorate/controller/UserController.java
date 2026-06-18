package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {
    private final UserStorage userStorage;
    private final UserService userService;

    @PostMapping
    public User createUser(@RequestBody User user) {
        validateUser(user);
        setDefaultName(user);

        User createdUser = userStorage.create(user);
        log.info("Добавлен пользователь: {}", createdUser);

        return createdUser;
    }

    @PutMapping
    public User updateUser(@RequestBody User user) {
        validateUser(user);
        setDefaultName(user);

        User updatedUser = userStorage.update(user);
        log.info("Обновлён пользователь: {}", updatedUser);

        return updatedUser;
    }

    @GetMapping
    public Collection<User> getAllUsers() {
        return userStorage.getAll();
    }

    @GetMapping("/{id}")
    public User getUserById(@PathVariable Integer id) {
        return userStorage.getById(id);
    }

    @PutMapping("/{id}/friends/{friendId}")
    public User addFriend(@PathVariable Integer id, @PathVariable Integer friendId) {
        log.info("Добавление в друзья: userId={}, friendId={}", id, friendId);
        return userService.addFriend(id, friendId);
    }

    @DeleteMapping("/{id}/friends/{friendId}")
    public User removeFriend(@PathVariable Integer id, @PathVariable Integer friendId) {
        log.info("Удаление из друзей: userId={}, friendId={}", id, friendId);
        return userService.removeFriend(id, friendId);
    }

    @GetMapping("/{id}/friends")
    public List<User> getFriends(@PathVariable Integer id) {
        return userService.getFriends(id);
    }

    @GetMapping("/{id}/friends/common/{otherId}")
    public List<User> getCommonFriends(@PathVariable Integer id, @PathVariable Integer otherId) {
        return userService.getCommonFriends(id, otherId);
    }

    private void validateUser(User user) {
        if (user == null) {
            log.warn("Ошибка валидации пользователя: тело запроса пустое");
            throw new ValidationException("Пользователь не может быть null");
        }

        if (user.getEmail() == null || user.getEmail().isBlank() || !user.getEmail().contains("@")) {
            log.warn("Ошибка валидации пользователя: некорректный email");
            throw new ValidationException("Email не может быть пустым и должен содержать символ @");
        }

        if (user.getLogin() == null || user.getLogin().isBlank() || user.getLogin().contains(" ")) {
            log.warn("Ошибка валидации пользователя: некорректный логин");
            throw new ValidationException("Логин не может быть пустым и содержать пробелы");
        }

        if (user.getBirthday() == null || user.getBirthday().isAfter(LocalDate.now())) {
            log.warn("Ошибка валидации пользователя: дата рождения в будущем");
            throw new ValidationException("Дата рождения не может быть в будущем");
        }
    }

    private void setDefaultName(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
    }
}