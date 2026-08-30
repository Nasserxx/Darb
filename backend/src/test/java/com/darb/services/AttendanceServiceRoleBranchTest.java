package com.darb.services;

import com.darb.dtos.attendance.AttendanceResponse;
import com.darb.entities.Attendance;
import com.darb.entities.Circle;
import com.darb.entities.Enrollment;
import com.darb.entities.Mosque;
import com.darb.entities.ParentStudent;
import com.darb.entities.Student;
import com.darb.entities.Teacher;
import com.darb.entities.User;
import com.darb.entities.enums.AttendanceStatus;
import com.darb.entities.enums.EnrollmentStatus;
import com.darb.entities.enums.UserRole;
import com.darb.repositories.AttendanceRepository;
import com.darb.repositories.CircleRepository;
import com.darb.repositories.EnrollmentRepository;
import com.darb.repositories.ParentStudentRepository;
import com.darb.repositories.StudentRepository;
import com.darb.repositories.UserRepository;
import com.darb.security.MosqueAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalTime;
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
class AttendanceServiceRoleBranchTest {

    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private CircleRepository circleRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private ParentStudentRepository parentStudentRepository;
    @Mock
    private MosqueAccessService mosqueAccessService;
    @Mock
    private OverrideAuditService overrideAuditService;

    private AttendanceService service;

    private final Pageable pageable = PageRequest.of(0, 20);
    private final UUID circleId = UUID.randomUUID();
    private final UUID mosqueId = UUID.randomUUID();
    private final UUID callerId = UUID.randomUUID();
    private final UUID selfStudentId = UUID.randomUUID();
    private final UUID peerStudentId = UUID.randomUUID();

    private Mosque mosque;
    private Circle circle;
    private Student selfStudent;
    private Student peerStudent;
    private User recorder;

    @BeforeEach
    void setUp() {
        service = new AttendanceService(
                attendanceRepository,
                enrollmentRepository,
                circleRepository,
                userRepository,
                studentRepository,
                parentStudentRepository,
                mosqueAccessService,
                overrideAuditService);

        mosque = Mosque.builder().id(mosqueId).name("Al-Noor").build();
        Teacher teacher = Teacher.builder().id(UUID.randomUUID()).build();
        circle = Circle.builder().id(circleId).mosque(mosque).teacher(teacher).name("C1").build();
        recorder = User.builder().id(UUID.randomUUID()).role(UserRole.TEACHER).fullName("T").email("t@t").passwordHash("x").build();

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

        Attendance own = attendance(selfStudent);
        Attendance peer = attendance(peerStudent);
        when(attendanceRepository.findByCircleId(circleId)).thenReturn(List.of(own, peer));

        Page<AttendanceResponse> page = service.findByCircleId(callerId, circleId, pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().getStudentName()).isEqualTo("Self");
        verify(mosqueAccessService).assertCanAccessMosque(callerId, mosqueId);
        verify(attendanceRepository, never()).findByCircleId(eq(circleId), any(Pageable.class));
    }

    @Test
    void findByCircleId_teacher_returnsFullCirclePage() {
        stubRole(UserRole.TEACHER);
        Attendance own = attendance(selfStudent);
        Attendance peer = attendance(peerStudent);
        when(attendanceRepository.findByCircleId(circleId, pageable))
                .thenReturn(new PageImpl<>(List.of(own, peer), pageable, 2));

        Page<AttendanceResponse> page = service.findByCircleId(callerId, circleId, pageable);

        assertThat(page.getContent()).hasSize(2);
        verify(attendanceRepository).findByCircleId(circleId, pageable);
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

        Attendance own = attendance(selfStudent);
        Attendance peer = attendance(peerStudent);
        when(attendanceRepository.findByCircleId(circleId)).thenReturn(List.of(own, peer));

        Page<AttendanceResponse> page = service.findByCircleId(callerId, circleId, pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().getStudentName()).isEqualTo("Self");
        verify(mosqueAccessService, never()).assertCanAccessMosque(any(), any());
    }

    private void stubRole(UserRole role) {
        when(userRepository.findById(callerId)).thenReturn(Optional.of(
                User.builder().id(callerId).role(role).fullName("U").email("u@t").passwordHash("x").build()));
    }

    private Attendance attendance(Student student) {
        Enrollment enrollment = Enrollment.builder()
                .id(UUID.randomUUID())
                .student(student)
                .circle(circle)
                .status(EnrollmentStatus.ACTIVE)
                .build();
        return Attendance.builder()
                .id(UUID.randomUUID())
                .enrollment(enrollment)
                .circle(circle)
                .sessionDate(LocalDate.of(2026, 8, 1))
                .status(AttendanceStatus.PRESENT)
                .scheduledStart(LocalTime.of(16, 0))
                .recordedBy(recorder)
                .build();
    }
}
