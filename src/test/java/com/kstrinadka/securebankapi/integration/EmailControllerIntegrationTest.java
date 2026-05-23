package com.kstrinadka.securebankapi.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kstrinadka.securebankapi.dto.request.EmailCreateRequest;
import com.kstrinadka.securebankapi.dto.request.EmailUpdateRequest;
import com.kstrinadka.securebankapi.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class EmailControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Test
    void getEmailsReturnsCurrentUserEmails() throws Exception {
        mockMvc.perform(authenticatedGet("/api/v1/users/me/emails", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].email").value("ivan@mail.com"));
    }

    @Test
    void postAddsEmailToCurrentUser() throws Exception {
        EmailCreateRequest request = new EmailCreateRequest("ivan.extra@mail.com");

        mockMvc.perform(authenticatedPost("/api/v1/users/me/emails", 1L, request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value("ivan.extra@mail.com"));

        mockMvc.perform(authenticatedGet("/api/v1/users/me/emails", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].email", hasItem("ivan.extra@mail.com")));
    }

    @Test
    void postWithUsedEmailReturnsConflict() throws Exception {
        EmailCreateRequest request = new EmailCreateRequest("petr@mail.com");

        mockMvc.perform(authenticatedPost("/api/v1/users/me/emails", 1L, request))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value("Email is already used"));
    }

    @Test
    void postWithInvalidEmailReturnsBadRequest() throws Exception {
        EmailCreateRequest request = new EmailCreateRequest("not-an-email");

        mockMvc.perform(authenticatedPost("/api/v1/users/me/emails", 1L, request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void putUpdatesCurrentUserEmail() throws Exception {
        EmailUpdateRequest request = new EmailUpdateRequest("ivan.updated@mail.com");

        mockMvc.perform(authenticatedPut("/api/v1/users/me/emails/1", 1L, request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("ivan.updated@mail.com"));
    }

    @Test
    void putAnotherUsersEmailReturnsNotFound() throws Exception {
        EmailUpdateRequest request = new EmailUpdateRequest("ivan.updated@mail.com");

        mockMvc.perform(authenticatedPut("/api/v1/users/me/emails/2", 1L, request))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_FOUND"));
    }

    @Test
    void deleteRemovesEmailWhenUserHasMoreThanOneEmail() throws Exception {
        MvcResult addResult = mockMvc.perform(authenticatedPost(
                        "/api/v1/users/me/emails",
                        1L,
                        new EmailCreateRequest("ivan.delete@mail.com")
                ))
                .andExpect(status().isCreated())
                .andReturn();

        Long emailId = readId(addResult);

        mockMvc.perform(authenticatedDelete("/api/v1/users/me/emails/" + emailId, 1L))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteLastEmailReturnsConflict() throws Exception {
        mockMvc.perform(authenticatedDelete("/api/v1/users/me/emails/1", 1L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LAST_EMAIL_CANNOT_BE_DELETED"))
                .andExpect(jsonPath("$.message").value("Last email cannot be deleted"));
    }

    @Test
    void requestWithoutJwtReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/emails"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    private Long readId(MvcResult result) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asLong();
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder authenticatedGet(
            String url,
            Long userId
    ) {
        return get(url).header("Authorization", "Bearer " + jwtService.generateToken(userId));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder authenticatedPost(
            String url,
            Long userId,
            Object request
    ) throws Exception {
        return post(url)
                .header("Authorization", "Bearer " + jwtService.generateToken(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder authenticatedPut(
            String url,
            Long userId,
            Object request
    ) throws Exception {
        return put(url)
                .header("Authorization", "Bearer " + jwtService.generateToken(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder authenticatedDelete(
            String url,
            Long userId
    ) {
        return delete(url).header("Authorization", "Bearer " + jwtService.generateToken(userId));
    }
}
