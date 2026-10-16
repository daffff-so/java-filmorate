package ru.yandex.practicum.filmorate.storage.user;

import ru.yandex.practicum.filmorate.model.User;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserStorage {
    User create(User user);

    User update(User user);

    Optional<User> getById(Integer id);

    Collection<User> getAll();

    void delete(Integer id);

    default void addFriend(Integer userId, Integer friendId) {
        getById(userId).ifPresent(user -> user.getFriends().add(friendId));
    }

    default void removeFriend(Integer userId, Integer friendId) {
        getById(userId).ifPresent(user -> user.getFriends().remove(friendId));
    }

    default List<User> getFriends(Integer userId) {
        return getById(userId)
                .stream()
                .flatMap(user -> user.getFriends().stream())
                .map(this::getById)
                .flatMap(Optional::stream)
                .toList();
    }

    default List<User> getCommonFriends(Integer userId, Integer otherId) {
        Optional<User> first = getById(userId);
        Optional<User> second = getById(otherId);

        if (first.isEmpty() || second.isEmpty()) {
            return List.of();
        }

        return first.get().getFriends().stream()
                .filter(second.get().getFriends()::contains)
                .map(this::getById)
                .flatMap(Optional::stream)
                .toList();
    }
}