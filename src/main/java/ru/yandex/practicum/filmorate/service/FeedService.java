package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Operation;
import ru.yandex.practicum.filmorate.storage.feed.FeedStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.List;

@Slf4j
@Service
public class FeedService {
    private final FeedStorage feedStorage;
    private final UserStorage userStorage;

    public FeedService(
            FeedStorage feedStorage,
            @Qualifier("userDbStorage") UserStorage userStorage
    ) {
        this.feedStorage = feedStorage;
        this.userStorage = userStorage;
    }

    public void addEvent(Integer userId, EventType eventType, Operation operation, Integer entityId) {
        Event event = feedStorage.add(Event.builder()
                .userId(userId)
                .eventType(eventType)
                .operation(operation)
                .entityId(entityId)
                .timestamp(System.currentTimeMillis())
                .build());
        log.debug("Добавлено событие в ленту: {}", event);
    }

    public List<Event> getFeed(Integer userId) {
        userStorage.getById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));

        return feedStorage.getByUserId(userId);
    }
}
