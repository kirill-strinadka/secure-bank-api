package com.kstrinadka.securebankapi.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kstrinadka.securebankapi.dto.request.PhoneCreateRequest;
import com.kstrinadka.securebankapi.dto.request.PhoneUpdateRequest;
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
class PhoneControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Test
    void getPhonesReturnsCurrentUserPhones() throws Exception {
        mockMvc.perform(authenticatedGet("/api/v1/users/me/phones", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].phone").value("79207865431"));
    }

    @Test
    void postAddsPhoneToCurrentUser() throws Exception {
        PhoneCreateRequest request = new PhoneCreateRequest("79207865434");

        mockMvc.perform(authenticatedPost("/api/v1/users/me/phones", 1L, request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.phone").value("79207865434"));

        mockMvc.perform(authenticatedGet("/api/v1/users/me/phones", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].phone", hasItem("79207865434")));
    }

    @Test
    void postWithUsedPhoneReturnsConflict() throws Exception {
        PhoneCreateRequest request = new PhoneCreateRequest("79207865432");

        mockMvc.perform(authenticatedPost("/api/v1/users/me/phones", 1L, request))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PHONE_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value("Phone is already used"));
    }

    @Test
    void postWithInvalidPhoneReturnsBadRequest() throws Exception {
        PhoneCreateRequest request = new PhoneCreateRequest("89207865431");

        mockMvc.perform(authenticatedPost("/api/v1/users/me/phones", 1L, request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void putUpdatesCurrentUserPhone() throws Exception {
        PhoneUpdateRequest request = new PhoneUpdateRequest("79207865434");

        mockMvc.perform(authenticatedPut("/api/v1/users/me/phones/1", 1L, request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.phone").value("79207865434"));
    }

    @Test
    void putAnotherUsersPhoneReturnsNotFound() throws Exception {
        PhoneUpdateRequest request = new PhoneUpdateRequest("79207865434");

        mockMvc.perform(authenticatedPut("/api/v1/users/me/phones/2", 1L, request))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PHONE_NOT_FOUND"));
    }

    @Test
    void deleteRemovesPhoneWhenUserHasMoreThanOnePhone() throws Exception {
        MvcResult addResult = mockMvc.perform(authenticatedPost(
                        "/api/v1/users/me/phones",
                        1L,
                        new PhoneCreateRequest("79207865434")
                ))
                .andExpect(status().isCreated())
                .andReturn();

        Long phoneId = readId(addResult);

        mockMvc.perform(authenticatedDelete("/api/v1/users/me/phones/" + phoneId, 1L))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteLastPhoneReturnsConflict() throws Exception {
        mockMvc.perform(authenticatedDelete("/api/v1/users/me/phones/1", 1L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LAST_PHONE_CANNOT_BE_DELETED"))
                .andExpect(jsonPath("$.message").value("Last phone cannot be deleted"));
    }

    @Test
    void requestWithoutJwtReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/phones"))
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
