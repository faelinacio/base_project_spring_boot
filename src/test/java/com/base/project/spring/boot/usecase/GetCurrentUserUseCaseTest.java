package com.base.project.spring.boot.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.base.project.spring.boot.domain.User;

@ExtendWith(MockitoExtension.class)
class GetCurrentUserUseCaseTest {

    @Mock
    private CurrentUserLoader currentUserLoader;

    @InjectMocks
    private GetCurrentUserUseCase useCase;

    @Test
    void execute_delegatesToCurrentUserLoader() {
        UUID id = UUID.randomUUID();
        User user = User.builder().id(id).email("rafael@example.com").build();
        when(currentUserLoader.byId(id)).thenReturn(user);

        assertThat(useCase.execute(id)).isEqualTo(user);
    }

}
