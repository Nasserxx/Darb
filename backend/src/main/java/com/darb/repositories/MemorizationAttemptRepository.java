package com.darb.repositories;

import com.darb.entities.MemorizationAttempt;
import com.darb.entities.enums.PageHalf;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemorizationAttemptRepository extends JpaRepository<MemorizationAttempt, UUID> {

    Optional<MemorizationAttempt> findByStudentIdAndCircleIdAndPageAndHalf(
            UUID studentId, UUID circleId, short page, PageHalf half);

    List<MemorizationAttempt> findByStudentIdAndCircleIdOrderBySessionDateDesc(
            UUID studentId, UUID circleId);

    List<MemorizationAttempt> findByStudentIdAndCircleIdAndPageOrderBySessionDateDesc(
            UUID studentId, UUID circleId, short page);

    List<MemorizationAttempt> findByStudentIdAndCircleId(UUID studentId, UUID circleId);
}
