package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class UserControllerTest {
    private final UserController controller = new UserController();

    @Test
    void shouldCreateValidUser() {
        User user = new User();
        user.setEmail("user@mail.com");
        user.setLogin("userlogin");
        user.setName("User Name");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        User createdUser = controller.createUser(user);

        assertNotNull(createdUser.getId());
        assertEquals("user@mail.com", createdUser.getEmail());
    }

    @Test
    void shouldUseLoginAsNameWhenNameIsEmpty() {
        User user = new User();
        user.setEmail("user@mail.com");
        user.setLogin("userlogin");
        user.setName("");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        User createdUser = controller.createUser(user);

        assertEquals("userlogin", createdUser.getName());
    }

    @Test
    void shouldNotCreateUserWithInvalidEmail() {
        User user = new User();
        user.setEmail("mail.com");
        user.setLogin("userlogin");
        user.setName("User");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        assertThrows(ValidationException.class, () -> controller.createUser(user));
    }

    @Test
    void shouldNotCreateUserWithLoginWithSpaces() {
        User user = new User();
        user.setEmail("user@mail.com");
        user.setLogin("user login");
        user.setName("User");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        assertThrows(ValidationException.class, () -> controller.createUser(user));
    }

    @Test
    void shouldNotCreateUserWithFutureBirthday() {
        User user = new User();
        user.setEmail("user@mail.com");
        user.setLogin("userlogin");
        user.setName("User");
        user.setBirthday(LocalDate.now().plusDays(1));

        assertThrows(ValidationException.class, () -> controller.createUser(user));
    }
}