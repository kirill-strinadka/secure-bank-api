package com.kstrinadka.securebankapi.integration;

import com.kstrinadka.securebankapi.config.CacheNames;
import com.kstrinadka.securebankapi.repository.EmailDataRepository;
import com.kstrinadka.securebankapi.repository.PhoneDataRepository;
import com.kstrinadka.securebankapi.repository.UserRepository;
import com.kstrinadka.securebankapi.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CacheIntegrationTest extends AbstractIntegrationTest {

    private static final Long USER_ID = 1L;
    private static final String CACHE_EMAIL = "cache.email@mail.com";
    private static final String CACHE_PHONE = "79200001001";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailDataRepository emailDataRepository;

    @Autowired
    private PhoneDataRepository phoneDataRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        cleanupInsertedData();
        clearAllCaches();
    }

    @AfterEach
    void tearDown() {
        cleanupInsertedData();
        clearAllCaches();
    }

    @Test
    void repeatedUserSearchCachesResponse() throws Exception {
        mockMvc.perform(authenticatedGet("/api/v1/users"))
                .andExpect(status().isOk());

        assertThat(cacheSize(CacheNames.USER_SEARCH)).isEqualTo(1);

        mockMvc.perform(authenticatedGet("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());

        assertThat(cacheSize(CacheNames.USER_SEARCH)).isEqualTo(1);
    }

    @Test
    void getCurrentUserEmailsCachesResponseByUserId() throws Exception {
        mockMvc.perform(authenticatedGet("/api/v1/users/me/emails"))
                .andExpect(status().isOk());

        assertThat(cacheValue(CacheNames.CURRENT_USER_EMAILS, USER_ID)).isNotNull();
    }

    @Test
    void emailWriteEvictsCurrentUserEmailsUserSearchAndEmailExistsCaches() throws Exception {
        mockMvc.perform(authenticatedGet("/api/v1/users"))
                .andExpect(status().isOk());
        mockMvc.perform(authenticatedGet("/api/v1/users/me/emails"))
                .andExpect(status().isOk());
        emailDataRepository.existsByEmail(CACHE_EMAIL);

        assertThat(cacheSize(CacheNames.USER_SEARCH)).isEqualTo(1);
        assertThat(cacheValue(CacheNames.CURRENT_USER_EMAILS, USER_ID)).isNotNull();
        assertThat(cacheValue(CacheNames.EMAIL_EXISTS, CACHE_EMAIL)).isNotNull();

        mockMvc.perform(authenticatedPost("/api/v1/users/me/emails", "{\"email\":\"" + CACHE_EMAIL + "\"}"))
                .andExpect(status().isCreated());

        assertThat(cacheSize(CacheNames.USER_SEARCH)).isZero();
        assertThat(cacheValue(CacheNames.CURRENT_USER_EMAILS, USER_ID)).isNull();
        assertThat(cacheSize(CacheNames.EMAIL_EXISTS)).isZero();

        mockMvc.perform(authenticatedGet("/api/v1/users/me/emails"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].email", hasItem(CACHE_EMAIL)));
    }

    @Test
    void phoneWriteEvictsCurrentUserPhonesUserSearchAndPhoneExistsCaches() throws Exception {
        mockMvc.perform(authenticatedGet("/api/v1/users"))
                .andExpect(status().isOk());
        mockMvc.perform(authenticatedGet("/api/v1/users/me/phones"))
                .andExpect(status().isOk());
        phoneDataRepository.existsByPhone(CACHE_PHONE);

        assertThat(cacheSize(CacheNames.USER_SEARCH)).isEqualTo(1);
        assertThat(cacheValue(CacheNames.CURRENT_USER_PHONES, USER_ID)).isNotNull();
        assertThat(cacheValue(CacheNames.PHONE_EXISTS, CACHE_PHONE)).isNotNull();

        mockMvc.perform(authenticatedPost("/api/v1/users/me/phones", "{\"phone\":\"" + CACHE_PHONE + "\"}"))
                .andExpect(status().isCreated());

        assertThat(cacheSize(CacheNames.USER_SEARCH)).isZero();
        assertThat(cacheValue(CacheNames.CURRENT_USER_PHONES, USER_ID)).isNull();
        assertThat(cacheSize(CacheNames.PHONE_EXISTS)).isZero();

        mockMvc.perform(authenticatedGet("/api/v1/users/me/phones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].phone", hasItem(CACHE_PHONE)));
    }

    @Test
    void daoExistsLookupsAreCached() {
        assertThat(userRepository.existsById(1L)).isTrue();
        assertThat(emailDataRepository.existsByEmail("ivan@mail.com")).isTrue();
        assertThat(phoneDataRepository.existsByPhone("79207865431")).isTrue();

        assertThat(cacheValue(CacheNames.USER_EXISTS, 1L)).isNotNull();
        assertThat(cacheValue(CacheNames.EMAIL_EXISTS, "ivan@mail.com")).isNotNull();
        assertThat(cacheValue(CacheNames.PHONE_EXISTS, "79207865431")).isNotNull();
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder authenticatedGet(String url) {
        return get(url).header("Authorization", "Bearer " + jwtService.generateToken(USER_ID));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder authenticatedPost(
            String url,
            String json
    ) {
        return post(url)
                .header("Authorization", "Bearer " + jwtService.generateToken(USER_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json);
    }

    private Object cacheValue(String cacheName, Object key) {
        Cache.ValueWrapper wrapper = cache(cacheName).get(key);
        return wrapper == null ? null : wrapper.get();
    }

    private long cacheSize(String cacheName) {
        return ((CaffeineCache) cache(cacheName)).getNativeCache().estimatedSize();
    }

    private Cache cache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        assertThat(cache).isNotNull();
        return cache;
    }

    private void clearAllCaches() {
        cacheManager.getCacheNames()
                .stream()
                .map(cacheManager::getCache)
                .forEach(cache -> {
                    if (cache != null) {
                        cache.clear();
                    }
                });
    }

    private void cleanupInsertedData() {
        jdbcTemplate.update("DELETE FROM email_data WHERE email LIKE 'cache.%@mail.com'");
        jdbcTemplate.update("DELETE FROM phone_data WHERE phone LIKE '79200001%'");
    }
}
