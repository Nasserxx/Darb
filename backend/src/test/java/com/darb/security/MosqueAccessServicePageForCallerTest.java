package com.darb.security;

import com.darb.entities.Mosque;
import com.darb.entities.MosqueAdmin;
import com.darb.entities.User;
import com.darb.entities.enums.UserRole;
import com.darb.repositories.MosqueAdminRepository;
import com.darb.repositories.ParentStudentRepository;
import com.darb.repositories.StudentRepository;
import com.darb.repositories.TeacherRepository;
import com.darb.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Focused unit coverage for {@link MosqueAccessService#pageForCaller} mosqueId filter overload.
 */
@ExtendWith(MockitoExtension.class)
class MosqueAccessServicePageForCallerTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private MosqueAdminRepository mosqueAdminRepository;
    @Mock
    private TeacherRepository teacherRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private ParentStudentRepository parentStudentRepository;

    private MosqueAccessService service;

    private final Pageable pageable = PageRequest.of(0, 20);
    private final UUID callerId = UUID.randomUUID();
    private final UUID mosqueA = UUID.randomUUID();
    private final UUID mosqueB = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new MosqueAccessService(
                userRepository,
                mosqueAdminRepository,
                teacherRepository,
                studentRepository,
                parentStudentRepository);
    }

    @Test
    void superAdmin_withMosqueIdFilter_usesFindByMosqueId() {
        stubUser(UserRole.SUPER_ADMIN);

        AtomicReference<UUID> appliedMosque = new AtomicReference<>();
        Page<String> byMosque = new PageImpl<>(List.of("a"), pageable, 1);

        Page<String> result = service.pageForCaller(
                callerId,
                pageable,
                id -> {
                    appliedMosque.set(id);
                    return byMosque;
                },
                p -> new PageImpl<>(List.of("a", "b"), pageable, 2),
                mosqueA);

        assertThat(result.getContent()).containsExactly("a");
        assertThat(appliedMosque.get()).isEqualTo(mosqueA);
    }

    @Test
    void superAdmin_withoutMosqueIdFilter_usesFindAll() {
        stubUser(UserRole.SUPER_ADMIN);

        AtomicReference<Boolean> findAllCalled = new AtomicReference<>(false);
        Page<String> all = new PageImpl<>(List.of("a", "b"), pageable, 2);

        Page<String> result = service.pageForCaller(
                callerId,
                pageable,
                id -> Page.empty(pageable),
                p -> {
                    findAllCalled.set(true);
                    return all;
                },
                null);

        assertThat(result.getContent()).containsExactly("a", "b");
        assertThat(findAllCalled.get()).isTrue();
    }

    @Test
    void mosqueAdmin_ignoresForeignMosqueIdFilter_staysOnCallerMosque() {
        stubUser(UserRole.MOSQUE_ADMIN);
        Mosque mosque = Mosque.builder().id(mosqueA).build();
        MosqueAdmin admin = MosqueAdmin.builder().mosque(mosque).build();
        when(mosqueAdminRepository.findByUserIdAndIsActiveTrue(callerId)).thenReturn(List.of(admin));

        AtomicReference<UUID> appliedMosque = new AtomicReference<>();
        Page<String> byMosque = new PageImpl<>(List.of("own"), pageable, 1);

        Function<UUID, Page<String>> findByMosqueId = id -> {
            appliedMosque.set(id);
            return byMosque;
        };

        Page<String> result = service.pageForCaller(
                callerId,
                pageable,
                findByMosqueId,
                p -> new PageImpl<>(List.of("should-not-appear"), pageable, 1),
                mosqueB);

        assertThat(result.getContent()).containsExactly("own");
        assertThat(appliedMosque.get()).isEqualTo(mosqueA);
    }

    private void stubUser(UserRole role) {
        User user = User.builder()
                .id(callerId)
                .fullName("Caller")
                .email("caller-" + callerId + "@test.darb")
                .passwordHash("hash")
                .role(role)
                .isActive(true)
                .build();
        when(userRepository.findById(callerId)).thenReturn(Optional.of(user));
    }
}
