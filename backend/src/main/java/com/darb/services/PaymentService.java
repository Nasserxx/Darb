package com.darb.services;

import com.darb.dtos.payment.PaymentCreateRequest;
import com.darb.dtos.payment.PaymentResponse;
import com.darb.dtos.payment.PaymentUpdateRequest;
import com.darb.entities.Circle;
import com.darb.entities.Mosque;
import com.darb.entities.Payment;
import com.darb.entities.Student;
import com.darb.entities.User;
import com.darb.exceptions.ResourceNotFoundException;
import com.darb.repositories.CircleRepository;
import com.darb.repositories.MosqueRepository;
import com.darb.repositories.PaymentRepository;
import com.darb.repositories.StudentRepository;
import com.darb.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final StudentRepository studentRepository;
    private final CircleRepository circleRepository;
    private final MosqueRepository mosqueRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<PaymentResponse> findAll(Pageable pageable) {
        return paymentRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public PaymentResponse findById(UUID id) {
        return toResponse(findEntityOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> findByMosqueId(UUID mosqueId, Pageable pageable) {
        return paymentRepository.findByMosqueId(mosqueId, pageable).map(this::toResponse);
    }

    @Transactional
    public PaymentResponse create(PaymentCreateRequest request) {
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", request.getStudentId()));
        Circle circle = circleRepository.findById(request.getCircleId())
                .orElseThrow(() -> new ResourceNotFoundException("Circle", "id", request.getCircleId()));
        Mosque mosque = mosqueRepository.findById(request.getMosqueId())
                .orElseThrow(() -> new ResourceNotFoundException("Mosque", "id", request.getMosqueId()));
        User recordedBy = userRepository.findById(request.getRecordedBy())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getRecordedBy()));

        Payment payment = Payment.builder()
                .student(student)
                .circle(circle)
                .mosque(mosque)
                .amount(request.getAmount())
                .discount(request.getDiscount())
                .amountPaid(request.getAmountPaid())
                .status(request.getStatus())
                .method(request.getMethod())
                .cycle(request.getCycle())
                .dueDate(request.getDueDate())
                .paidDate(request.getPaidDate())
                .receiptUrl(request.getReceiptUrl())
                .recordedBy(recordedBy)
                .notes(request.getNotes())
                .build();

        return toResponse(paymentRepository.save(payment));
    }

    @Transactional
    public PaymentResponse update(UUID id, PaymentUpdateRequest request) {
        Payment payment = findEntityOrThrow(id);

        if (request.getDiscount() != null) {
            payment.setDiscount(request.getDiscount());
        }
        if (request.getAmountPaid() != null) {
            payment.setAmountPaid(request.getAmountPaid());
        }
        if (request.getStatus() != null) {
            payment.setStatus(request.getStatus());
        }
        if (request.getMethod() != null) {
            payment.setMethod(request.getMethod());
        }
        if (request.getDueDate() != null) {
            payment.setDueDate(request.getDueDate());
        }
        if (request.getPaidDate() != null) {
            payment.setPaidDate(request.getPaidDate());
        }
        if (request.getReceiptUrl() != null) {
            payment.setReceiptUrl(request.getReceiptUrl());
        }
        if (request.getNotes() != null) {
            payment.setNotes(request.getNotes());
        }

        return toResponse(paymentRepository.save(payment));
    }

    @Transactional
    public void delete(UUID id) {
        paymentRepository.deleteById(id);
    }

    private Payment findEntityOrThrow(UUID id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", id));
    }

    private PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .studentId(payment.getStudent().getId())
                .circleId(payment.getCircle().getId())
                .mosqueId(payment.getMosque().getId())
                .amount(payment.getAmount())
                .discount(payment.getDiscount())
                .amountPaid(payment.getAmountPaid())
                .status(payment.getStatus())
                .method(payment.getMethod())
                .cycle(payment.getCycle())
                .dueDate(payment.getDueDate())
                .paidDate(payment.getPaidDate())
                .receiptUrl(payment.getReceiptUrl())
                .recordedBy(payment.getRecordedBy().getId())
                .notes(payment.getNotes())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
