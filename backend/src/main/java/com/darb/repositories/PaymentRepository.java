package com.darb.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.darb.entities.Payment;
import com.darb.entities.enums.PaymentStatus;

import java.util.List;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID>, JpaSpecificationExecutor<Payment> {
    List<Payment> findByStudentId(UUID studentId);
    List<Payment> findByCircleId(UUID circleId);
    List<Payment> findByMosqueId(UUID mosqueId);
    List<Payment> findByStatus(PaymentStatus status);
    Page<Payment> findByMosqueId(UUID mosqueId, Pageable pageable);
}
