package com.kstrinadka.securebankapi.integration;

import com.kstrinadka.securebankapi.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserSearchIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Test
    void searchWithoutFiltersReturnsUsersPage() throws Exception {
        mockMvc.perform(authenticatedGet("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(3)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void searchByExactEmailReturnsOneUser() throws Exception {
        mockMvc.perform(authenticatedGet("/api/v1/users").param("email", "ivan@mail.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].id").value(1))
                .andExpect(jsonPath("$.items[0].emails[0]").value("ivan@mail.com"));
    }

    @Test
    void searchByExactPhoneReturnsOneUser() throws Exception {
        mockMvc.perform(authenticatedGet("/api/v1/users").param("phone", "79207865432"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].id").value(2))
                .andExpect(jsonPath("$.items[0].phones[0]").value("79207865432"));
    }

    @Test
    void searchByNamePrefixReturnsUsers() throws Exception {
        mockMvc.perform(authenticatedGet("/api/v1/users").param("name", "Iv"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].name", startsWith("Ivan")));
    }

    @Test
    void searchByDateOfBirthReturnsUsersBornAfterDate() throws Exception {
        mockMvc.perform(authenticatedGet("/api/v1/users").param("dateOfBirth", "1992-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[*].id", everyItem(greaterThan(0))));
    }

    @Test
    void paginationWorks() throws Exception {
        mockMvc.perform(authenticatedGet("/api/v1/users").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void invalidSizeReturnsBadRequest() throws Exception {
        mockMvc.perform(authenticatedGet("/api/v1/users").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void searchWithoutJwtReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void searchWithJwtReturnsOk() throws Exception {
        mockMvc.perform(authenticatedGet("/api/v1/users"))
                .andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder authenticatedGet(String url) {
        return get(url).header("Authorization", "Bearer " + jwtService.generateToken(1L));
    }
}
