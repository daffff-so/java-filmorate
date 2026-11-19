package ru.yandex.practicum.filmorate.search;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:search-tests;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@Transactional
class FilmSearchTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc.update("INSERT INTO directors (director_id, name) VALUES (11001, 'Крад Режиссёр')");
        jdbc.update("INSERT INTO directors (director_id, name) VALUES (11002, 'Другой Крад')");
        for (int id = 12001; id <= 12003; id++) {
            jdbc.update("INSERT INTO users (user_id, email, login, birthday) VALUES (?, ?, ?, '1990-01-01')",
                    id, id + "@search.test", "search" + id);
        }
        insertFilm(10001, "Крадущийся тигр");
        insertFilm(10002, "Ночной крадущийся");
        insertFilm(10003, "Другой фильм");
        insertFilm(10004, "100%_! совпадение");
        jdbc.update("INSERT INTO film_directors (film_id, director_id) VALUES (10002, 11001), (10002, 11002), (10003, 11001)");
        jdbc.update("INSERT INTO film_genres (film_id, genre_id) VALUES (10002, 1), (10002, 2)");
        jdbc.update("INSERT INTO film_likes (film_id, user_id) VALUES (10001, 12001), (10001, 12002),"
                + " (10002, 12001), (10002, 12002), (10002, 12003), (10003, 12001)");
    }

    private void insertFilm(int id, String name) {
        jdbc.update("INSERT INTO films (film_id, name, description, release_date, duration, mpa_id)"
                + " VALUES (?, ?, 'Search fixture', '2000-01-01', 120, 1)", id, name);
    }

    @Test
    void searchesTitleIgnoringCaseAndOrdersByAllLikes() throws Exception {
        mvc.perform(get("/films/search").param("query", "КрАд").param("by", "title"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(10002))
                .andExpect(jsonPath("$[1].id").value(10001))
                .andExpect(jsonPath("$[0].directors.length()").value(2))
                .andExpect(jsonPath("$[0].genres.length()").value(2));
    }

    @Test
    void searchesDirectorsAndCombinedFieldsWithoutDuplicates() throws Exception {
        mvc.perform(get("/films/search").param("query", "КРАД").param("by", "director"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(10002))
                .andExpect(jsonPath("$[1].id").value(10003));
        for (String by : new String[]{"director,title", "title,director"}) {
            mvc.perform(get("/films/search").param("query", "крад").param("by", by))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(3))
                    .andExpect(jsonPath("$[0].id").value(10002))
                    .andExpect(jsonPath("$[1].id").value(10001))
                    .andExpect(jsonPath("$[2].id").value(10003));
        }
    }

    @Test
    void returnsEmptyAndTreatsWildcardsAsLiteralCharacters() throws Exception {
        mvc.perform(get("/films/search").param("query", "nothing-matches").param("by", "director,title"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/films/search").param("query", "%_!").param("by", "title"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(10004));
    }

    @Test
    void rejectsUnknownSearchField() throws Exception {
        mvc.perform(get("/films/search").param("query", "крад").param("by", "actor"))
                .andExpect(status().isBadRequest());
    }
}
