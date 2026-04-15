package com.geonexus.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geonexus.domain.StoryMapEntity;
import com.geonexus.domain.StoryMapEntity.StoryChapter;
import com.geonexus.repository.StoryMapRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 叙事地图服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StoryMapService {

    private final StoryMapRepository repository;
    private final CacheService cacheService;
    private final ObjectMapper objectMapper;

    private static final String CACHE_PREFIX = "story:";

    public StoryMapEntity create(StoryMapEntity story) {
        story.setId(UUID.randomUUID().toString());
        story.setStatus("DRAFT");
        if (story.getChaptersJson() == null) {
            story.setChaptersJson("[]");
        }
        return repository.save(story);
    }

    @Transactional
    public StoryMapEntity update(String id, StoryMapEntity updates) {
        StoryMapEntity existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("StoryMap not found: " + id));
        if (updates.getTitle() != null) existing.setTitle(updates.getTitle());
        if (updates.getDescription() != null) existing.setDescription(updates.getDescription());
        if (updates.getChaptersJson() != null) existing.setChaptersJson(updates.getChaptersJson());
        if (updates.getContent() != null) existing.setContent(updates.getContent());
        if (updates.getCoverImageUrl() != null) existing.setCoverImageUrl(updates.getCoverImageUrl());
        if (updates.getAuthorName() != null) existing.setAuthorName(updates.getAuthorName());
        StoryMapEntity saved = repository.save(existing);
        cacheService.delete(CACHE_PREFIX + id);
        return saved;
    }

    @Transactional
    public void delete(String id) {
        repository.findById(id).ifPresent(story -> {
            story.markAsDeleted("system");
            repository.save(story);
        });
        cacheService.delete(CACHE_PREFIX + id);
    }

    public StoryMapEntity getById(String id) {
        StoryMapEntity story = (StoryMapEntity) cacheService.get(CACHE_PREFIX + id);
        if (story == null) {
            story = repository.findById(id).orElse(null);
            if (story != null) {
                cacheService.set(CACHE_PREFIX + id, story, 3600);
            }
        }
        return story;
    }

    public List<StoryMapEntity> getByAuthor(String authorId) {
        return repository.findByAuthorIdAndDeletedAtIsNullOrderByUpdatedAtDesc(authorId);
    }

    public List<StoryMapEntity> getPublished() {
        return repository.findByStatusOrderByPublishedAtDesc("PUBLISHED");
    }

    @Transactional
    public StoryMapEntity publish(String id) {
        StoryMapEntity story = getById(id);
        if (story == null) throw new RuntimeException("StoryMap not found: " + id);
        story.setStatus("PUBLISHED");
        story.setPublishedAt(java.time.LocalDateTime.now());
        story.setShareToken(UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        return repository.save(story);
    }

    public StoryMapEntity getByShareToken(String token) {
        return repository.findByShareTokenAndDeletedAtIsNull(token).orElse(null);
    }

    public String generateShareUrl(String shareToken) {
        return "/stories/" + shareToken;
    }

    // ============ JSON 序列化辅助 ============

    public String chaptersToJson(List<StoryChapter> chapters) {
        try {
            return objectMapper.writeValueAsString(chapters);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize chapters", e);
            return "[]";
        }
    }

    public List<StoryChapter> chaptersFromJson(String json) {
        if (json == null || json.isEmpty()) return new java.util.ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<StoryChapter>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize chapters", e);
            return new java.util.ArrayList<>();
        }
    }
}
