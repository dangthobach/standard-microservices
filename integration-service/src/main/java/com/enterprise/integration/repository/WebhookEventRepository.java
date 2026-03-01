package com.enterprise.integration.repository;

import com.enterprise.integration.entity.WebhookEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {

    Page<WebhookEvent> findByWebhookEndpointId(UUID webhookEndpointId, Pageable pageable);
}
