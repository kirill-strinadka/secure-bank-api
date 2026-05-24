package com.kstrinadka.securebankapi.unit;

import com.kstrinadka.securebankapi.dto.request.PhoneCreateRequest;
import com.kstrinadka.securebankapi.dto.request.PhoneUpdateRequest;
import com.kstrinadka.securebankapi.dto.response.PhoneResponse;
import com.kstrinadka.securebankapi.entity.PhoneDataEntity;
import com.kstrinadka.securebankapi.entity.UserEntity;
import com.kstrinadka.securebankapi.exception.ConflictException;
import com.kstrinadka.securebankapi.exception.NotFoundException;
import com.kstrinadka.securebankapi.mapper.PhoneMapper;
import com.kstrinadka.securebankapi.repository.PhoneDataRepository;
import com.kstrinadka.securebankapi.repository.UserRepository;
import com.kstrinadka.securebankapi.security.CurrentUserProvider;
import com.kstrinadka.securebankapi.service.CacheInvalidationService;
import com.kstrinadka.securebankapi.service.impl.PhoneServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PhoneServiceTest extends AbstractUnitTest {

    private static final Long CURRENT_USER_ID = 1L;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PhoneDataRepository phoneDataRepository;

    @Mock
    private CacheInvalidationService cacheInvalidationService;

    private PhoneServiceImpl phoneService;

    @BeforeEach
    void setUp() {
        phoneService = new PhoneServiceImpl(
                currentUserProvider,
                userRepository,
                phoneDataRepository,
                new PhoneMapper(),
                cacheInvalidationService
        );
        when(currentUserProvider.getCurrentUserId()).thenReturn(CURRENT_USER_ID);
    }

    @Test
    void addPhoneAddsPhoneSuccessfully() {
        UserEntity user = user(CURRENT_USER_ID);
        when(userRepository.findById(CURRENT_USER_ID)).thenReturn(Optional.of(user));
        when(phoneDataRepository.existsByPhone("79207865434")).thenReturn(false);
        when(phoneDataRepository.save(any(PhoneDataEntity.class))).thenAnswer(invocation -> {
            PhoneDataEntity phoneData = invocation.getArgument(0);
            phoneData.setId(10L);
            return phoneData;
        });

        PhoneResponse response = phoneService.addPhone(new PhoneCreateRequest("79207865434"));

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getPhone()).isEqualTo("79207865434");
        verify(phoneDataRepository).save(any(PhoneDataEntity.class));
        verify(cacheInvalidationService).evictPhoneCaches(CURRENT_USER_ID);
    }

    @Test
    void addPhoneFailsWhenPhoneIsAlreadyUsed() {
        when(userRepository.findById(CURRENT_USER_ID)).thenReturn(Optional.of(user(CURRENT_USER_ID)));
        when(phoneDataRepository.existsByPhone("79207865432")).thenReturn(true);

        assertThatThrownBy(() -> phoneService.addPhone(new PhoneCreateRequest("79207865432")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Phone is already used");

        verify(phoneDataRepository, never()).save(any(PhoneDataEntity.class));
    }

    @Test
    void updatePhoneUpdatesPhoneSuccessfully() {
        PhoneDataEntity existingPhone = phoneData(10L, CURRENT_USER_ID, "79207865431");
        when(phoneDataRepository.findByIdAndUserId(10L, CURRENT_USER_ID)).thenReturn(Optional.of(existingPhone));
        when(phoneDataRepository.findByPhone("79207865434")).thenReturn(Optional.empty());
        when(phoneDataRepository.save(existingPhone)).thenReturn(existingPhone);

        PhoneResponse response = phoneService.updatePhone(10L, new PhoneUpdateRequest("79207865434"));

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getPhone()).isEqualTo("79207865434");
        verify(cacheInvalidationService).evictPhoneCaches(CURRENT_USER_ID);
    }

    @Test
    void updatePhoneAllowsSamePhone() {
        PhoneDataEntity existingPhone = phoneData(10L, CURRENT_USER_ID, "79207865431");
        when(phoneDataRepository.findByIdAndUserId(10L, CURRENT_USER_ID)).thenReturn(Optional.of(existingPhone));
        when(phoneDataRepository.findByPhone("79207865431")).thenReturn(Optional.of(existingPhone));
        when(phoneDataRepository.save(existingPhone)).thenReturn(existingPhone);

        PhoneResponse response = phoneService.updatePhone(10L, new PhoneUpdateRequest("79207865431"));

        assertThat(response.getPhone()).isEqualTo("79207865431");
        verify(cacheInvalidationService).evictPhoneCaches(CURRENT_USER_ID);
    }

    @Test
    void updatePhoneFailsWhenPhoneBelongsToAnotherUser() {
        when(phoneDataRepository.findByIdAndUserId(20L, CURRENT_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> phoneService.updatePhone(20L, new PhoneUpdateRequest("79207865434")))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Phone not found");

        verify(phoneDataRepository, never()).save(any(PhoneDataEntity.class));
    }

    @Test
    void updatePhoneFailsWhenNewPhoneIsUsedByAnotherPhoneData() {
        PhoneDataEntity existingPhone = phoneData(10L, CURRENT_USER_ID, "79207865431");
        PhoneDataEntity usedPhone = phoneData(20L, 2L, "79207865432");
        when(phoneDataRepository.findByIdAndUserId(10L, CURRENT_USER_ID)).thenReturn(Optional.of(existingPhone));
        when(phoneDataRepository.findByPhone("79207865432")).thenReturn(Optional.of(usedPhone));

        assertThatThrownBy(() -> phoneService.updatePhone(10L, new PhoneUpdateRequest("79207865432")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Phone is already used");

        verify(phoneDataRepository, never()).save(any(PhoneDataEntity.class));
    }

    @Test
    void deletePhoneDeletesPhoneSuccessfully() {
        PhoneDataEntity phoneData = phoneData(10L, CURRENT_USER_ID, "79207865431");
        when(phoneDataRepository.findByIdAndUserId(10L, CURRENT_USER_ID)).thenReturn(Optional.of(phoneData));
        when(phoneDataRepository.countByUserId(CURRENT_USER_ID)).thenReturn(2L);

        phoneService.deletePhone(10L);

        verify(phoneDataRepository).delete(phoneData);
        verify(cacheInvalidationService).evictPhoneCaches(CURRENT_USER_ID);
    }

    @Test
    void deletePhoneFailsWhenItIsLastPhone() {
        PhoneDataEntity phoneData = phoneData(10L, CURRENT_USER_ID, "79207865431");
        when(phoneDataRepository.findByIdAndUserId(10L, CURRENT_USER_ID)).thenReturn(Optional.of(phoneData));
        when(phoneDataRepository.countByUserId(CURRENT_USER_ID)).thenReturn(1L);

        assertThatThrownBy(() -> phoneService.deletePhone(10L))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Last phone cannot be deleted");

        verify(phoneDataRepository, never()).delete(any(PhoneDataEntity.class));
    }

    private UserEntity user(Long id) {
        UserEntity user = new UserEntity();
        user.setId(id);
        return user;
    }

    private PhoneDataEntity phoneData(Long id, Long userId, String phone) {
        PhoneDataEntity phoneData = new PhoneDataEntity();
        phoneData.setId(id);
        phoneData.setUser(user(userId));
        phoneData.setPhone(phone);
        return phoneData;
    }
}
