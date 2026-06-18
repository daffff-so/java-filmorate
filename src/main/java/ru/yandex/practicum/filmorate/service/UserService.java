package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserStorage userStorage;

    public User addFriend(Integer userId, Integer friendId) {
        User user = userStorage.getById(userId);
        User friend = userStorage.getById(friendId);

        user.getFriends().add(friendId);
        friend.getFriends().add(userId);

        return user;
    }

    public User removeFriend(Integer userId, Integer friendId) {
        User user = userStorage.getById(userId);
        User friend = userStorage.getById(friendId);

        user.getFriends().remove(friendId);
        friend.getFriends().remove(userId);

        return user;
    }

    public List<User> getFriends(Integer userId) {
        User user = userStorage.getById(userId);

        return user.getFriends().stream()
                .map(userStorage::getById)
                .toList();
    }

    public List<User> getCommonFriends(Integer userId, Integer otherId) {
        User user = userStorage.getById(userId);
        User otherUser = userStorage.getById(otherId);

        return user.getFriends().stream()
                .filter(otherUser.getFriends()::contains)
                .map(userStorage::getById)
                .toList();
    }
}