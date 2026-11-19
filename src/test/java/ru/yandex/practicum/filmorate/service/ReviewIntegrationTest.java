package ru.yandex.practicum.filmorate.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:review-tests;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@Transactional
class ReviewIntegrationTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        for (int id = 21001; id <= 21002; id++) {
            jdbc.update("INSERT INTO users (user_id, email, login, birthday) VALUES (?, ?, ?, '1990-01-01')",
                    id, id + "@review.test", "review" + id);
            jdbc.update("INSERT INTO films (film_id, name, description, release_date, duration, mpa_id)"
                    + " VALUES (?, ?, 'Review fixture', '2000-01-01', 120, 1)", id, "Review film " + id);
        }
    }

    @Test
    void updatePreservesAuthorFilmAndReactions() throws Exception {
        int id = createReview();
        mvc.perform(put("/reviews/{id}/like/{userId}", id, 21002)).andExpect(status().isOk());

        mvc.perform(put("/reviews").contentType(MediaType.APPLICATION_JSON).content("""
                        {"reviewId":%d,"content":"Updated","isPositive":false,"userId":21002,"filmId":21002}
                        """.formatted(id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(21001))
                .andExpect(jsonPath("$.filmId").value(21001))
                .andExpect(jsonPath("$.content").value("Updated"))
                .andExpect(jsonPath("$.isPositive").value(false))
                .andExpect(jsonPath("$.useful").value(1));

        mvc.perform(get("/reviews/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(21001))
                .andExpect(jsonPath("$.filmId").value(21001));
        mvc.perform(get("/reviews").param("filmId", "21001"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].reviewId").value(id));
        mvc.perform(get("/reviews").param("filmId", "21002"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/users/21002/feed"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void reviewLifecycleWritesEventsForOriginalAuthor() throws Exception {
        int id = createReview();
        mvc.perform(put("/reviews").contentType(MediaType.APPLICATION_JSON).content("""
                        {"reviewId":%d,"content":"Updated","isPositive":false}
                        """.formatted(id)))
                .andExpect(status().isOk());
        mvc.perform(delete("/reviews/{id}", id)).andExpect(status().isOk());
        mvc.perform(get("/reviews/{id}", id)).andExpect(status().isNotFound());

        mvc.perform(get("/users/21001/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[*].eventType").value(org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.is("REVIEW"))))
                .andExpect(jsonPath("$[*].entityId").value(org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.is(id))))
                .andExpect(jsonPath("$[*].userId").value(org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.is(21001))))
                .andExpect(jsonPath("$[0].operation").value("ADD"))
                .andExpect(jsonPath("$[1].operation").value("UPDATE"))
                .andExpect(jsonPath("$[2].operation").value("REMOVE"))
                .andExpect(jsonPath("$[0].timestamp").isNumber());
    }

    @Test
    void failedOperationsDoNotWriteEvents() throws Exception {
        int id = createReview();
        mvc.perform(put("/reviews").contentType(MediaType.APPLICATION_JSON).content("""
                        {"reviewId":%d,"content":"","isPositive":true}
                        """.formatted(id)))
                .andExpect(status().isBadRequest());
        mvc.perform(delete("/reviews/999999")).andExpect(status().isNotFound());
        mvc.perform(post("/reviews").contentType(MediaType.APPLICATION_JSON).content("""
                        {"content":"Invalid film","isPositive":true,"userId":21001,"filmId":999999}
                        """))
                .andExpect(status().isNotFound());
        mvc.perform(get("/users/21001/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].operation").value("ADD"));
    }

    private int createReview() throws Exception {
        String response = mvc.perform(post("/reviews").contentType(MediaType.APPLICATION_JSON).content("""
                        {"content":"Original","isPositive":true,"userId":21001,"filmId":21001}
                        """))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode review = mapper.readTree(response);
        return review.get("reviewId").asInt();
    }
}
