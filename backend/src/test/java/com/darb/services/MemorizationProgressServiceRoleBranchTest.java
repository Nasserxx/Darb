package com.darb.services;

import com.darb.dtos.memorization.MemorizationProgressCreateRequest;
import com.darb.dtos.memorization.MemorizationProgressResponse;
import com.darb.entities.Circle;
import com.darb.entities.Enrollment;
import com.darb.entities.MemorizationProgress;
import com.darb.entities.Mosque;
import com.darb.entities.ParentStudent;
import com.darb.entities.Student;
import com.darb.entities.Teacher;
import com.darb.entities.User;
import com.darb.entities.enums.EnrollmentStatus;
import com.darb.entities.enums.RecitationGrade;
import com.darb.entities.enums.UserRole;
import com.darb.repositories.CircleRepository;
import com.darb.repositories.EnrollmentRepository;
import com.darb.repositories.MemorizationProgressRepository;
import com.darb.repositories.ParentStudentRepository;
import com.darb.repositories.StudentRepository;
import com.darb.repositories.TeacherRepository;
import com.darb.repositories.UserRepository;
import com.darb.security.MosqueAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemorizationProgressServiceRoleBranchTest {

    @Mock
    private MemorizationProgressRepository memorizationProgressRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private CircleRepository circleRepository;
    @Mock
    private TeacherRepository teacherRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ParentStudentRepository parentStudentRepository;
    @Mock
    private MosqueAccessService mosqueAccessService;

    private MemorizationProgressService service;

    private final Pageable pageable = PageRequest.of(0, 20);
    private final UUID circleId = UUID.randomUUID();
    private final UUID mosqueId = UUID.randomUUID();
    private final UUID callerId = UUID.randomUUID();
    private final UUID selfStudentId = UUID.randomUUID();
    private final UUID peerStudentId = UUID.randomUUID();
    private final UUID teacherEntityId = UUID.randomUUID();

    private Mosque mosque;
    private Circle circle;
    private Student selfStudent;
    private Student peerStudent;
    private Teacher teacher;

    @BeforeEach
    void setUp() {
        service = new MemorizationProgressService(
                memorizationProgressRepository,
                studentRepository,
                circleRepository,
                teacherRepository,
                enrollmentRepository,
                userRepository,
                parentStudentRepository,
                mosqueAccessService);

        mosque = Mosque.builder().id(mosqueId).name("Al-Noor").build();
        teacher = Teacher.builder().id(teacherEntityId).build();
        circle = Circle.builder().id(circleId).mosque(mosque).teacher(teacher).name("C1").build();

        User selfUser = User.builder().id(callerId).role(UserRole.STUDENT).fullName("Self").email("s@t").passwordHash("x").build();
        selfStudent = Student.builder().id(selfStudentId).user(selfUser).mosque(mosque).status(EnrollmentStatus.ACTIVE).build();

        User peerUser = User.builder().id(UUID.randomUUID()).role(UserRole.STUDENT).fullName("Peer").email("p@t").passwordHash("x").build();
        peerStudent = Student.builder().id(peerStudentId).user(peerUser).mosque(mosque).status(EnrollmentStatus.ACTIVE).build();

        when(circleRepository.findById(circleId)).thenReturn(Optional.of(circle));
    }

    @Test
    void findByCircleId_student_returnsOnlyOwnRows() {
        stubRole(UserRole.STUDENT);
        when(studentRepository.findByUserIdAndStatus(callerId, EnrollmentStatus.ACTIVE))
                .thenReturn(List.of(selfStudent));

        MemorizationProgress own = progress(selfStudent, RecitationGrade.GOOD);
        when(memorizationProgressRepository.findByStudentIdAndCircleId(selfStudentId, circleId))
                .thenReturn(List.of(own));

        Page<MemorizationProgressResponse> page = service.findByCircleId(callerId, circleId, pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().getStudentId()).isEqualTo(selfStudentId);
        verify(mosqueAccessService).assertCanAccessMosque(callerId, mosqueId);
        verify(memorizationProgressRepository, never()).findByCircleId(eq(circleId), any(Pageable.class));
    }

    @Test
    void findByCircleId_teacher_returnsFullCirclePage() {
        stubRole(UserRole.TEACHER);
        MemorizationProgress own = progress(selfStudent, RecitationGrade.GOOD);
        MemorizationProgress peer = progress(peerStudent, RecitationGrade.VERY_GOOD);
        when(memorizationProgressRepository.findByCircleId(circleId, pageable))
                .thenReturn(new PageImpl<>(List.of(own, peer), pageable, 2));

        Page<MemorizationProgressResponse> page = service.findByCircleId(callerId, circleId, pageable);

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().stream().map(MemorizationProgressResponse::getStudentId))
                .containsExactlyInAnyOrder(selfStudentId, peerStudentId);
        verify(memorizationProgressRepository).findByCircleId(circleId, pageable);
    }

    @Test
    void findByCircleId_parent_filtersToLinkedChildren() {
        stubRole(UserRole.PARENT);
        ParentStudent link = ParentStudent.builder()
                .id(UUID.randomUUID())
                .parent(User.builder().id(callerId).role(UserRole.PARENT).fullName("P").email("par@t").passwordHash("x").build())
                .student(selfStudent)
                .build();
        when(parentStudentRepository.findByParentId(callerId)).thenReturn(List.of(link));

        MemorizationProgress own = progress(selfStudent, RecitationGrade.GOOD);
        MemorizationProgress peer = progress(peerStudent, RecitationGrade.VERY_GOOD);
        when(memorizationProgressRepository.findByCircleId(circleId)).thenReturn(List.of(own, peer));

        Page<MemorizationProgressResponse> page = service.findByCircleId(callerId, circleId, pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().getStudentId()).isEqualTo(selfStudentId);
        verify(mosqueAccessService, never()).assertCanAccessMosque(any(), any());
    }

    @Test
    void createMyProgress_resolvesCircleTeacher_andDefaultsGrade() {
        when(studentRepository.findByUserId(callerId)).thenReturn(List.of(selfStudent));
        Enrollment enrollment = Enrollment.builder()
                .id(UUID.randomUUID())
                .student(selfStudent)
                .circle(circle)
                .status(EnrollmentStatus.ACTIVE)
                .build();
        when(enrollmentRepository.findByStudentIdAndCircleId(selfStudentId, circleId))
                .thenReturn(Optional.of(enrollment));
        when(studentRepository.findById(selfStudentId)).thenReturn(Optional.of(selfStudent));
        when(circleRepository.findById(circleId)).thenReturn(Optional.of(circle));
        when(teacherRepository.findById(teacherEntityId)).thenReturn(Optional.of(teacher));

        MemorizationProgress saved = progress(selfStudent, RecitationGrade.ACCEPTABLE);
        when(memorizationProgressRepository.save(any())).thenReturn(saved);

        MemorizationProgressCreateRequest request = new MemorizationProgressCreateRequest();
        request.setCircleId(circleId);
        request.setStudentId(UUID.randomUUID()); // forged — must be overwritten
        request.setTeacherId(UUID.randomUUID()); // bogus — must be overwritten
        request.setSurahNumber((short) 1);
        request.setAyahFrom((short) 1);
        request.setAyahTo((short) 7);
        request.setSessionDate(LocalDate.of(2026, 8, 26));
        request.setGrade(null);
        request.setTeacherNotes("should be stripped");
        request.setTajweedScore(99);

        MemorizationProgressResponse response = service.createMyProgress(callerId, request);

        assertThat(response.getStudentId()).isEqualTo(selfStudentId);
        assertThat(request.getStudentId()).isEqualTo(selfStudentId);
        assertThat(request.getTeacherId()).isEqualTo(teacherEntityId);
        assertThat(request.getGrade()).isEqualTo(RecitationGrade.ACCEPTABLE);
        assertThat(request.getTeacherNotes()).isNull();
        assertThat(request.getTajweedScore()).isNull();

        ArgumentCaptor<MemorizationProgress> captor = ArgumentCaptor.forClass(MemorizationProgress.class);
        verify(memorizationProgressRepository).save(captor.capture());
        assertThat(captor.getValue().getTeacher().getId()).isEqualTo(teacherEntityId);
        assertThat(captor.getValue().getStudent().getId()).isEqualTo(selfStudentId);
    }

    private void stubRole(UserRole role) {
        when(userRepository.findById(callerId)).thenReturn(Optional.of(
                User.builder().id(callerId).role(role).fullName("U").email("u@t").passwordHash("x").build()));
    }

    private MemorizationProgress progress(Student student, RecitationGrade grade) {
        return MemorizationProgress.builder()
                .id(UUID.randomUUID())
                .student(student)
                .circle(circle)
                .teacher(teacher)
                .surahNumber((short) 1)
                .ayahFrom((short) 1)
                .ayahTo((short) 7)
                .grade(grade)
                .sessionDate(LocalDate.of(2026, 8, 1))
                .build();
    }
}
