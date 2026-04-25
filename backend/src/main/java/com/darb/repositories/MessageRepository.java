package com.darb.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.darb.entities.Message;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID>, JpaSpecificationExecutor<Message> {
    List<Message> findBySenderId(UUID senderId);
    List<Message> findByReceiverId(UUID receiverId);
    List<Message> findByCircleId(UUID circleId);
    Page<Message> findBySenderIdOrReceiverId(UUID senderId, UUID receiverId, Pageable pageable);
    Page<Message> findByCircleId(UUID circleId, Pageable pageable);
}
