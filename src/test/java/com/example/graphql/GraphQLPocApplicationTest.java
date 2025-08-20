package com.example.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

import com.example.graphql.service.PostService;
import com.example.graphql.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.Environment;

@ExtendWith(MockitoExtension.class)
class GraphQLPocApplicationTest {

    @Mock
    private UserService userService;

    @Mock
    private PostService postService;

    @Mock
    private Environment environment;

    @InjectMocks
    private GraphQLPocApplication application;

    @Test
    void main_shouldRunSpringApplication() {
        try (MockedStatic<SpringApplication> springApplicationMockedStatic = mockStatic(SpringApplication.class)) {
            GraphQLPocApplication.main(new String[] {});
            springApplicationMockedStatic.verify(
                    () -> SpringApplication.run(GraphQLPocApplication.class, new String[] {}), times(1));
        }
    }

    @Test
    void run_whenDevProfileActive_shouldLogSeeding() {
        when(environment.getActiveProfiles()).thenReturn(new String[] {"dev"});
        assertThatCode(() -> application.run()).doesNotThrowAnyException();
        verifyNoInteractions(userService, postService);
    }

    @Test
    void run_whenNonDevProfileActive_shouldLogSkipSeeding() {
        when(environment.getActiveProfiles()).thenReturn(new String[] {"prod"});
        assertThatCode(() -> application.run()).doesNotThrowAnyException();
        verifyNoInteractions(userService, postService);
    }

    @Test
    void run_whenNoActiveProfiles_shouldCompleteWithoutErrors() {
        when(environment.getActiveProfiles()).thenReturn(new String[] {});
        assertThatCode(() -> application.run()).doesNotThrowAnyException();
        verify(environment, atLeastOnce()).getActiveProfiles();
        verifyNoInteractions(userService, postService);
    }

    @Test
    void constructor_shouldInjectDependencies() {
        assertThatCode(() -> new GraphQLPocApplication(userService, postService, environment))
                .doesNotThrowAnyException();
    }

    @Test
    void class_shouldHaveRequiredAnnotations() {
        assertThat(GraphQLPocApplication.class.isAnnotationPresent(
                        org.springframework.boot.autoconfigure.SpringBootApplication.class))
                .isTrue();
        assertThat(GraphQLPocApplication.class.isAnnotationPresent(
                        org.springframework.cache.annotation.EnableCaching.class))
                .isTrue();
        assertThat(GraphQLPocApplication.class.isAnnotationPresent(
                        org.springframework.transaction.annotation.EnableTransactionManagement.class))
                .isTrue();
        assertThat(GraphQLPocApplication.class.isAnnotationPresent(
                        org.springframework.boot.autoconfigure.domain.EntityScan.class))
                .isTrue();
        assertThat(GraphQLPocApplication.class.isAnnotationPresent(
                        org.springframework.data.jpa.repository.config.EnableJpaRepositories.class))
                .isTrue();
    }
}
