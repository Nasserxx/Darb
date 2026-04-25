package com.darb.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.darb.entities.Notification;
import com.darb.entities.enums.NotificationStatus;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID>, JpaSpecificationExecutor<Notification> {
    List<Notification> findByRecipientId(UUID recipientUserId);
    List<Notification> findByMosqueId(UUID mosqueId);
    List<Notification> findByStatus(NotificationStatus status);
    Page<Notification> findByRecipientId(UUID recipientUserId, Pageable pageable);
}
