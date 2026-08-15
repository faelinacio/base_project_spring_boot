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
class GetCurrentUserUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GetCurrentUserUseCase useCase;

    @Test
    void execute_whenUserExists_returnsUser() {
        UUID id = UUID.randomUUID();
        User user = User.builder().id(id).email("rafael@example.com").build();
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        assertThat(useCase.execute(id)).isEqualTo(user);
    }

    @Test
    void execute_whenUserMissing_throwsIllegalStateException() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(id)).isInstanceOf(IllegalStateException.class);
    }

}
