package com.base.project.spring.boot.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.base.project.spring.boot.domain.User;
import com.base.project.spring.boot.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class CurrentUserLoaderTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CurrentUserLoader loader;

    @Test
    void byId_whenUserExists_returnsUser() {
        UUID id = UUID.randomUUID();
        User user = User.builder().id(id).email("rafael@example.com").build();
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        assertThat(loader.byId(id)).isEqualTo(user);
    }

    @Test
    void byId_whenUserMissing_throwsIllegalState() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loader.byId(id)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void byEmail_whenUserExists_returnsUser() {
        User user = User.builder().id(UUID.randomUUID()).email("rafael@example.com").build();
        when(userRepository.findByEmail("rafael@example.com")).thenReturn(Optional.of(user));

        assertThat(loader.byEmail("rafael@example.com")).isEqualTo(user);
    }

    @Test
    void byEmail_whenUserMissing_throwsIllegalState() {
        when(userRepository.findByEmail("rafael@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loader.byEmail("rafael@example.com")).isInstanceOf(IllegalStateException.class);
    }

}
