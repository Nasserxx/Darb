package com.darb.services;

import com.darb.dtos.user.UserCreateRequest;
import com.darb.dtos.user.UserPickerResponse;
import com.darb.dtos.user.UserResponse;
import com.darb.dtos.user.UserUpdateRequest;
import com.darb.entities.MosqueAdmin;
import com.darb.entities.MosqueMemberJoinRequest;
import com.darb.entities.ParentStudent;
import com.darb.entities.Student;
import com.darb.entities.Teacher;
import com.darb.entities.User;
import com.darb.entities.enums.EnrollmentStatus;
import com.darb.entities.enums.JoinRequestStatus;
import com.darb.entities.enums.UserRole;
import com.darb.exceptions.BadRequestException;
import com.darb.exceptions.DuplicateResourceException;
import com.darb.exceptions.ForbiddenException;
import com.darb.exceptions.ResourceNotFoundException;
import com.darb.repositories.UserRepository;
import com.darb.security.MosqueAccessService;
import com.darb.util.NameFilterSpecs;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
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
        return userRepository.findAll(pageable).map(user -> toResponse(user, false));
    }

    @Transactional(readOnly = true)
    public UserResponse findById(UUID callerId, UUID id) {
        mosqueAccessService.assertCanAccessUser(callerId, id);
        return toResponse(findEntityOrThrow(id), true);
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
                .city(request.getCity())
                .addressCountry(request.getAddressCountry())
                .addressPostalCode(request.getAddressPostalCode())
                .addressStreet(request.getAddressStreet())
                .addressHouseNumber(request.getAddressHouseNumber())
                .addressState(request.getAddressState())
                .isActive(true)
                .build();

        return toResponse(userRepository.save(user), true);
    }

    @Transactional
    public UserResponse update(UUID callerId, UUID targetId, UserUpdateRequest request) {
        mosqueAccessService.assertCanAccessUser(callerId, targetId);
        User user = findEntityOrThrow(targetId);
        applyUpdate(user, request);
        return toResponse(userRepository.save(user), true);
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

        return userRepository.findAll(spec, pageable).map(user -> toResponse(user, false));
    }

    private static final int PICKER_MAX_PAGE_SIZE = 20;

    @Transactional(readOnly = true)
    public Page<UserPickerResponse> pickUsers(
            UUID callerId,
            String q,
            String country,
            String state,
            String city,
            LocalDate dateOfBirth,
            UUID mosqueId,
            UserRole role,
            Pageable pageable) {
        if ((mosqueId == null) != (role == null)) {
            throw new BadRequestException("mosqueId and role must be provided together");
        }
        String trimmedQ = NameFilterSpecs.normalizeQ(q);
        String trimmedCountry = StringUtils.hasText(country) ? country.trim() : null;
        String trimmedState = StringUtils.hasText(state) ? state.trim() : null;
        String trimmedCity = StringUtils.hasText(city) ? city.trim() : null;

        boolean hasQ = trimmedQ != null && trimmedQ.length() >= 2;
        boolean hasCountry = trimmedCountry != null;
        boolean hasDob = dateOfBirth != null;
        if (!hasQ && !hasCountry && !hasDob) {
            throw new BadRequestException("Provide q (min 2 chars), country, or dateOfBirth");
        }
        if ((trimmedState != null || trimmedCity != null) && !hasCountry) {
            throw new BadRequestException("country is required when filtering by state or city");
        }

        Pageable capped = pageable.getPageSize() > PICKER_MAX_PAGE_SIZE
                ? PageRequest.of(pageable.getPageNumber(), PICKER_MAX_PAGE_SIZE, pageable.getSort())
                : pageable;

        Specification<User> spec = (root, query, cb) -> cb.conjunction();
        if (hasQ) {
            String pattern = NameFilterSpecs.likePattern(trimmedQ);
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("fullName")), pattern),
                    cb.like(cb.lower(root.get("email")), pattern)
            ));
        }
        if (hasCountry) {
            String countryUpper = trimmedCountry.toUpperCase(Locale.ROOT);
            spec = spec.and((root, query, cb) ->
                    cb.equal(cb.upper(root.get("addressCountry")), countryUpper));
        }
        if (trimmedState != null) {
            String stateLower = trimmedState.toLowerCase(Locale.ROOT);
            spec = spec.and((root, query, cb) ->
                    cb.equal(cb.lower(cb.trim(root.get("addressState"))), stateLower));
        }
        if (trimmedCity != null) {
            String cityLower = trimmedCity.toLowerCase(Locale.ROOT);
            spec = spec.and((root, query, cb) ->
                    cb.equal(cb.lower(cb.trim(root.get("city"))), cityLower));
        }
        if (hasDob) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("dateOfBirth"), dateOfBirth));
        }
        if (mosqueId != null) {
            UserRole callerRole = findEntityOrThrow(callerId).getRole();
            if (callerRole != UserRole.SUPER_ADMIN && callerRole != UserRole.MOSQUE_ADMIN) {
                throw new ForbiddenException("Picker occupancy filter is an admin tool");
            }
            mosqueAccessService.requireMosqueAdminWith(callerId, mosqueId);
            if (role != UserRole.STUDENT && role != UserRole.TEACHER && role != UserRole.PARENT) {
                throw new BadRequestException("role must be STUDENT, TEACHER, or PARENT");
            }
            spec = spec.and((root, query, cb) -> cb.equal(root.get("role"), role));
            spec = spec.and(excludeOccupiedPickerUsers(mosqueId, role));
        }

        return userRepository.findAll(spec, capped).map(this::toPickerResponse);
    }

    private Specification<User> excludeOccupiedPickerUsers(UUID mosqueId, UserRole role) {
        return (root, query, cb) -> {
            Subquery<UUID> pending = query.subquery(UUID.class);
            Root<MosqueMemberJoinRequest> pendingRoot = pending.from(MosqueMemberJoinRequest.class);
            pending.select(pendingRoot.get("user").get("id"))
                    .where(
                            cb.equal(pendingRoot.get("mosque").get("id"), mosqueId),
                            cb.equal(pendingRoot.get("requestedRole"), role),
                            cb.equal(pendingRoot.get("status"), JoinRequestStatus.PENDING));

            Subquery<UUID> seated = query.subquery(UUID.class);
            if (role == UserRole.STUDENT) {
                Root<Student> studentRoot = seated.from(Student.class);
                seated.select(studentRoot.get("user").get("id"))
                        .where(
                                cb.equal(studentRoot.get("mosque").get("id"), mosqueId),
                                cb.equal(studentRoot.get("status"), EnrollmentStatus.ACTIVE));
            } else if (role == UserRole.TEACHER) {
                Root<Teacher> teacherRoot = seated.from(Teacher.class);
                seated.select(teacherRoot.get("user").get("id"))
                        .where(
                                cb.equal(teacherRoot.get("mosque").get("id"), mosqueId),
                                cb.isTrue(teacherRoot.get("isActive")));
            } else {
                Root<ParentStudent> parentRoot = seated.from(ParentStudent.class);
                seated.select(parentRoot.get("parent").get("id"))
                        .where(cb.equal(parentRoot.get("student").get("mosque").get("id"), mosqueId));
            }
            return cb.and(cb.not(root.get("id").in(pending)), cb.not(root.get("id").in(seated)));
        };
    }

    @Transactional(readOnly = true)
    public List<String> listPickerStates(String country) {
        String trimmed = StringUtils.hasText(country) ? country.trim() : null;
        if (trimmed == null) {
            throw new BadRequestException("country is required");
        }
        return userRepository.findDistinctStatesByCountry(trimmed.toUpperCase(Locale.ROOT));
    }

    @Transactional(readOnly = true)
    public List<String> listPickerCities(String country, String state) {
        String trimmedCountry = StringUtils.hasText(country) ? country.trim() : null;
        if (trimmedCountry == null) {
            throw new BadRequestException("country is required");
        }
        String countryUpper = trimmedCountry.toUpperCase(Locale.ROOT);
        String trimmedState = StringUtils.hasText(state) ? state.trim() : null;
        if (trimmedState == null) {
            return userRepository.findDistinctCitiesByCountry(countryUpper);
        }
        return userRepository.findDistinctCitiesByCountryAndState(countryUpper, trimmedState);
    }

    @Transactional(readOnly = true)
    public UserResponse findByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        return toResponse(user, true);
    }

    @Transactional
    public UserResponse updateByEmail(String email, UserUpdateRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        applyUpdate(user, request);
        return toResponse(userRepository.save(user), true);
    }

    private void applyUpdate(User user, UserUpdateRequest request) {
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
        if (request.getCity() != null) {
            user.setCity(request.getCity());
        }
        if (request.getAddressCountry() != null) {
            user.setAddressCountry(request.getAddressCountry());
        }
        if (request.getAddressPostalCode() != null) {
            user.setAddressPostalCode(request.getAddressPostalCode());
        }
        if (request.getAddressStreet() != null) {
            user.setAddressStreet(request.getAddressStreet());
        }
        if (request.getAddressHouseNumber() != null) {
            user.setAddressHouseNumber(request.getAddressHouseNumber());
        }
        if (request.getAddressState() != null) {
            user.setAddressState(request.getAddressState());
        }
    }

    private User findEntityOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    private UserResponse toResponse(User user, boolean includeAddress) {
        UserResponse.UserResponseBuilder builder = UserResponse.builder()
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
                .createdAt(user.getCreatedAt());

        if (includeAddress) {
            builder.city(user.getCity())
                    .addressCountry(user.getAddressCountry())
                    .addressPostalCode(user.getAddressPostalCode())
                    .addressStreet(user.getAddressStreet())
                    .addressHouseNumber(user.getAddressHouseNumber())
                    .addressState(user.getAddressState());
        }

        return builder.build();
    }

    private UserPickerResponse toPickerResponse(User user) {
        return UserPickerResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .dateOfBirth(user.getDateOfBirth())
                .city(user.getCity())
                .addressCountry(user.getAddressCountry())
                .addressState(user.getAddressState())
                .build();
    }
}
