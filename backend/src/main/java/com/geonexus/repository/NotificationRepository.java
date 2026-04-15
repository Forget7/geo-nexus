package com.geonexus.repository;

import com.geonexus.domain.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, String> {

    Page<NotificationEntity> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    Page<NotificationEntity> findByUserIdAndReadOrderByCreatedAtDesc(String userId, boolean read, Pageable pageable);

    Page<NotificationEntity> findByUserIdAndTypeOrderByCreatedAtDesc(String userId, String type, Pageable pageable);

    long countByUserIdAndRead(String userId, boolean read);

    void deleteByUserId(String userId);
}
