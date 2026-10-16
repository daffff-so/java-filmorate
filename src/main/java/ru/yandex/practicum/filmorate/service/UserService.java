package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;
import org.springframework.beans.factory.annotation.Qualifier;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Slf4j
@Service
public class UserService {
    private final UserStorage userStorage;

    public UserService(
            @Qualifier("userDbStorage") UserStorage userStorage
    ) {
        this.userStorage = userStorage;
    }

    public User createUser(User user) {
        validateUser(user);
        setDefaultName(user);

        return userStorage.create(user);
    }

    public User updateUser(User user) {
        validateUser(user);
        setDefaultName(user);

        getUserById(user.getId());

        return userStorage.update(user);
    }

    public Collection<User> getAllUsers() {
        return userStorage.getAll();
    }

    public User getUserById(Integer id) {
        return userStorage.getById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + id + " не найден"));
    }

    public User addFriend(Integer userId, Integer friendId) {
        checkDifferentUsers(userId, friendId);
        getUserById(userId);
        getUserById(friendId);

        userStorage.addFriend(userId, friendId);
        return getUserById(userId);
    }

    public User removeFriend(Integer userId, Integer friendId) {
        checkDifferentUsers(userId, friendId);
        getUserById(userId);
        getUserById(friendId);

        userStorage.removeFriend(userId, friendId);
        return getUserById(userId);
    }

    public List<User> getFriends(Integer userId) {
        getUserById(userId);
        return userStorage.getFriends(userId);
    }

    public List<User> getCommonFriends(Integer userId, Integer otherId) {
        checkDifferentUsers(userId, otherId);
        getUserById(userId);
        getUserById(otherId);

        return userStorage.getCommonFriends(userId, otherId);
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

    private void checkDifferentUsers(Integer userId, Integer otherId) {
        if (userId.equals(otherId)) {
            throw new ValidationException("Пользователь не может выполнить эту операцию с самим собой");
        }
    }
}