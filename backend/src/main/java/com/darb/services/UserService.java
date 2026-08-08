package com.darb.services;

import com.darb.dtos.user.UserCreateRequest;
import com.darb.dtos.user.UserResponse;
import com.darb.dtos.user.UserUpdateRequest;
import com.darb.entities.MosqueAdmin;
import com.darb.entities.Student;
import com.darb.entities.Teacher;
import com.darb.entities.User;
import com.darb.entities.enums.UserRole;
import com.darb.exceptions.DuplicateResourceException;
import com.darb.exceptions.ResourceNotFoundException;
import com.darb.repositories.UserRepository;
import com.darb.security.MosqueAccessService;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.jpa.domain.Specification;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MosqueAccessService mosqueAccessService;

    @Transactional(readOnly = true)
    public Page<UserResponse> findAll(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse findById(UUID callerId, UUID id) {
        mosqueAccessService.assertCanAccessUser(callerId, id);
        return toResponse(findEntityOrThrow(id));
    }

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }
        if (request.getPhone() != null && !request.getPhone().isBlank()
                && userRepository.existsByPhone(request.getPhone())) {
            throw new DuplicateResourceException("User", "phone", request.getPhone());
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .gender(request.getGender())
                .dateOfBirth(request.getDateOfBirth())
                .isActive(true)
                .build();

        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(UUID id, UserUpdateRequest request) {
        User user = findEntityOrThrow(id);

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        if (request.getDateOfBirth() != null) {
            user.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void delete(UUID id) {
        User user = findEntityOrThrow(id);
        user.setIsActive(false);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> searchUsers(UUID callerId, String query, Pageable pageable) {
        UserRole role = findEntityOrThrow(callerId).getRole();

        String like = "%" + query.toLowerCase() + "%";
        Specification<User> spec = (root, _, cb) -> cb.or(
                cb.like(cb.lower(root.get("fullName")), like),
                cb.like(cb.lower(root.get("email")), like)
        );

        if (role == UserRole.MOSQUE_ADMIN || role == UserRole.TEACHER) {
            UUID mosqueId = mosqueAccessService.resolveCallerMosqueId(callerId, role);
            if (mosqueId == null) {
                return Page.empty(pageable);
            }
            spec = spec.and((root, q, cb) -> {
                q.distinct(true);
                Subquery<UUID> studentSubquery = q.subquery(UUID.class);
                Root<Student> studentRoot = studentSubquery.from(Student.class);
                studentSubquery.select(studentRoot.get("user").get("id"))
                        .where(cb.equal(studentRoot.get("mosque").get("id"), mosqueId));

                Subquery<UUID> teacherSubquery = q.subquery(UUID.class);
                Root<Teacher> teacherRoot = teacherSubquery.from(Teacher.class);
                teacherSubquery.select(teacherRoot.get("user").get("id"))
                        .where(cb.equal(teacherRoot.get("mosque").get("id"), mosqueId));

                Subquery<UUID> adminSubquery = q.subquery(UUID.class);
                Root<MosqueAdmin> adminRoot = adminSubquery.from(MosqueAdmin.class);
                adminSubquery.select(adminRoot.get("user").get("id"))
                        .where(cb.equal(adminRoot.get("mosque").get("id"), mosqueId));

                return cb.or(
                        root.get("id").in(studentSubquery),
                        root.get("id").in(teacherSubquery),
                        root.get("id").in(adminSubquery)
                );
            });
        }

        return userRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse findByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        return toResponse(user);
    }

    @Transactional
    public UserResponse updateByEmail(String email, UserUpdateRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        if (request.getDateOfBirth() != null) {
            user.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        return toResponse(userRepository.save(user));
    }

    private User findEntityOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .gender(user.getGender())
                .dateOfBirth(user.getDateOfBirth())
                .avatarUrl(user.getAvatarUrl())
                .isActive(user.getIsActive())
                .lastLogin(user.getLastLogin())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
