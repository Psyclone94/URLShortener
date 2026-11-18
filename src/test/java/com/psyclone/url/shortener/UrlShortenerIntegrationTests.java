/* package com.psyclone.url.shortener;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class UrlShortenerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    @Autowired
    JdbcClient jdbc;

    @Test
    void createReturnsGeneratedShortCode() throws Exception {
        mvc.perform(post("/api/v1/urls")
                        .contentType("application/json")
                        .content("""
                                {"longUrl": "https://example.com/a"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode").isString())
                .andExpect(jsonPath("$.longUrl").value("https://example.com/a"));
    }

    @Test
    void redirectReturns302ToLongUrl() throws Exception {
        String code = createAndGetCode("https://example.com/redirect-me", null);

        mvc.perform(get("/" + code))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/redirect-me"));
    }

    @Test
    void customAliasIsUsedAsShortCode() throws Exception {
        String code = createAndGetCode("https://example.com/alias", "my-link");

        assertThat(code).isEqualTo("my-link");
        mvc.perform(get("/my-link"))
                .andExpect(status().isFound());
    }

    @Test
    void duplicateAliasReturns409() throws Exception {
        createAndGetCode("https://example.com/first", "taken-alias");

        mvc.perform(post("/api/v1/urls")
                        .contentType("application/json")
                        .content("""
                                {"longUrl": "https://example.com/second", "alias": "taken-alias"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void unknownCodeReturns404() throws Exception {
        mvc.perform(get("/doesNotExist"))
                .andExpect(status().isNotFound());
    }

    @Test
    void expiredLinkReturns410() throws Exception {
        jdbc.sql("""
                INSERT INTO url_mapping (short_code, long_url, expires_at)
                VALUES ('expired1', 'https://example.com/old', now() - interval '1 day')
                """).update();

        mvc.perform(get("/expired1"))
                .andExpect(status().isGone());
    }

    @Test
    void rejectsJavascriptScheme() throws Exception {
        mvc.perform(post("/api/v1/urls")
                        .contentType("application/json")
                        .content("""
                                {"longUrl": "javascript:alert(1)"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsShortAlias() throws Exception {
        mvc.perform(post("/api/v1/urls")
                        .contentType("application/json")
                        .content("""
                                {"longUrl": "https://example.com/x", "alias": "ab"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsReservedAlias() throws Exception {
        mvc.perform(post("/api/v1/urls")
                        .contentType("application/json")
                        .content("""
                                {"longUrl": "https://example.com/x", "alias": "api"}
                                """))
                .andExpect(status().isBadRequest());
    }

    private String createAndGetCode(String longUrl, String alias) throws Exception {
        String aliasJson = alias == null ? "" : ", \"alias\": \"" + alias + "\"";
        var result = mvc.perform(post("/api/v1/urls")
                        .contentType("application/json")
                        .content("{\"longUrl\": \"" + longUrl + "\"" + aliasJson + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        return mapper.readTree(result.getResponse().getContentAsString())
                .get("shortCode").asText();
    }
}
 */