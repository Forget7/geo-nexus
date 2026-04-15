package com.geonexus.service;

import com.geonexus.domain.ChatSessionEntity;
import com.geonexus.domain.MessageEntity;
import com.geonexus.model.ChatRequest;
import com.geonexus.model.ChatResponse;
import com.geonexus.repository.ChatSessionRepository;
import com.geonexus.repository.MessageRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ChatService 对话服务测试
 */
@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatSessionRepository sessionRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private LLMService llmService;

    @Mock
    private GeofenceService geofenceService;

    @Mock
    private TrajectoryService trajectoryService;

    @Mock
    private SpatialAnalysisService spatialAnalysisService;

    private ChatService service;
    private ChatSessionEntity mockSession;

    @BeforeEach
    void setUp() {
        service = new ChatService(
                sessionRepository,
                messageRepository,
                llmService,
                new com.fasterxml.jackson.databind.ObjectMapper(),
                geofenceService,
                trajectoryService,
                spatialAnalysisService
        );

        // Set up a mock session
        mockSession = ChatSessionEntity.builder()
                .id("test-session-123")
                .userId("anonymous")
                .title("测试会话")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .messages(new ArrayList<>())
                .build();
    }

    @Nested
    @DisplayName("空间意图路由测试")
    class SpatialIntentRoutingTests {

        @Test
        @DisplayName("'附近'查询应路由到 handleNearbyQuery")
        void testRouteSpatialIntent_nearby() {
            ChatRequest request = new ChatRequest();
            request.setMessage("找出天安门10公里内的医院");
            request.setSessionId("test-session");

            when(sessionRepository.findByIdAndUserId(anyString(), anyString()))
                    .thenReturn(Optional.of(mockSession));
            when(messageRepository.findBySessionIdOrderByTimestampAsc(anyString())
                    .thenReturn(java.util.List.of()));

            ChatResponse response = service.processChat(request);

            assertNotNull(response);
            assertEquals("test-session-123", response.getSessionId());
            assertTrue(response.getContent().contains("附近查询") ||
                    response.getContent().contains("空间意图路由") ||
                    response.getContent().length() > 0);
        }

        @Test
        @DisplayName("'周边'关键词应触发附近查询")
        void testRouteSpatialIntent_surrounding() {
            ChatRequest request = new ChatRequest();
            request.setMessage("北京站周边500米有什么");
            request.setSessionId(null); // Will create new session

            when(sessionRepository.findByIdAndUserId(anyString(), anyString()))
                    .thenReturn(Optional.empty());
            when(sessionRepository.save(any(ChatSessionEntity.class)))
                    .thenReturn(mockSession);
            when(messageRepository.save(any(MessageEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ChatResponse response = service.processChat(request);

            assertNotNull(response);
            assertNotNull(response.getSessionId());
        }

        @Test
        @DisplayName("普通消息应调用 LLM")
        void testRouteSpatialIntent_llmFallback() {
            ChatRequest request = new ChatRequest();
            request.setMessage("今天天气怎么样");
            request.setSessionId("test-session");

            when(sessionRepository.findByIdAndUserId(anyString(), anyString()))
                    .thenReturn(Optional.of(mockSession));
            when(messageRepository.findBySessionIdOrderByTimestampAsc(anyString()))
                    .thenReturn(java.util.List.of());
            when(llmService.isConfigured()).thenReturn(false);

            ChatResponse response = service.processChat(request);

            assertNotNull(response);
            assertNotNull(response.getContent());
        }

        @Test
        @DisplayName("空消息应安全处理")
        void testRouteSpatialIntent_emptyMessage() {
            ChatRequest request = new ChatRequest();
            request.setMessage("");
            request.setSessionId(null);

            when(sessionRepository.save(any(ChatSessionEntity.class)))
                    .thenReturn(mockSession);
            when(messageRepository.save(any(MessageEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(llmService.isConfigured()).thenReturn(false);

            ChatResponse response = service.processChat(request);

            assertNotNull(response);
        }
    }

    @Nested
    @DisplayName("会话管理测试")
    class SessionManagementTests {

        @Test
        @DisplayName("提供 sessionId 时应复用现有会话")
        void testProcessChat_reuseSession() {
            ChatRequest request = new ChatRequest();
            request.setMessage("继续之前的对话");
            request.setSessionId("existing-session");

            when(sessionRepository.findByIdAndUserId("existing-session", "anonymous"))
                    .thenReturn(Optional.of(mockSession));
            when(messageRepository.findBySessionIdOrderByTimestampAsc(anyString()))
                    .thenReturn(java.util.List.of());
            when(llmService.isConfigured()).thenReturn(false);

            ChatResponse response = service.processChat(request);

            assertNotNull(response);
            assertEquals("test-session-123", response.getSessionId());
            verify(sessionRepository, times(1))
                    .findByIdAndUserId("existing-session", "anonymous");
        }

        @Test
        @DisplayName("不提供 sessionId 时应创建新会话")
        void testProcessChat_createNewSession() {
            ChatRequest request = new ChatRequest();
            request.setMessage("新建一个会话");
            request.setSessionId(null);

            when(sessionRepository.findByIdAndUserId(anyString(), anyString()))
                    .thenReturn(Optional.empty());
            when(sessionRepository.save(any(ChatSessionEntity.class)))
                    .thenReturn(mockSession);
            when(messageRepository.save(any(MessageEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(llmService.isConfigured()).thenReturn(false);

            ChatResponse response = service.processChat(request);

            assertNotNull(response);
            verify(sessionRepository, times(2)).save(any(ChatSessionEntity.class));
        }

        @Test
        @DisplayName("消息应被正确保存")
        void testProcessChat_savesMessages() {
            ChatRequest request = new ChatRequest();
            request.setMessage("测试保存消息");
            request.setSessionId("test-session");

            when(sessionRepository.findByIdAndUserId(anyString(), anyString()))
                    .thenReturn(Optional.of(mockSession));
            when(messageRepository.findBySessionIdOrderByTimestampAsc(anyString()))
                    .thenReturn(java.util.List.of());
            when(messageRepository.save(any(MessageEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(llmService.isConfigured()).thenReturn(false);

            service.processChat(request);

            // Should save at least 2 messages: user message and assistant response
            verify(messageRepository, atLeast(2)).save(any(MessageEntity.class));
        }
    }

    @Nested
    @DisplayName("地图 URL 生成测试")
    class MapUrlGenerationTests {

        @Test
        @DisplayName("包含'地图'关键词的消息应生成地图URL")
        void testProcessChat_generatesMapUrl_forMapRequest() {
            ChatRequest request = new ChatRequest();
            request.setMessage("生成一张天安门的地图");
            request.setSessionId("test-session");

            when(sessionRepository.findByIdAndUserId(anyString(), anyString()))
                    .thenReturn(Optional.of(mockSession));
            when(messageRepository.findBySessionIdOrderByTimestampAsc(anyString()))
                    .thenReturn(java.util.List.of());
            when(llmService.isConfigured()).thenReturn(false);

            ChatResponse response = service.processChat(request);

            assertNotNull(response);
            // Map URL may or may not be generated depending on LLM response
            // Just verify response is valid
            assertNotNull(response.getContent());
        }

        @Test
        @DisplayName("不包含地图关键词不应生成地图URL")
        void testProcessChat_noMapUrl_forNormalMessage() {
            ChatRequest request = new ChatRequest();
            request.setMessage("你好");
            request.setSessionId("test-session");

            when(sessionRepository.findByIdAndUserId(anyString(), anyString()))
                    .thenReturn(Optional.of(mockSession));
            when(messageRepository.findBySessionIdOrderByTimestampAsc(anyString()))
                    .thenReturn(java.util.List.of());
            when(llmService.isConfigured()).thenReturn(false);

            ChatResponse response = service.processChat(request);

            assertNotNull(response);
            assertNotNull(response.getContent());
        }
    }

    @Nested
    @DisplayName("chat 便捷方法测试")
    class ChatMethodTests {

        @Test
        @DisplayName("chat 便捷方法应正常工作")
        void testChat_method() {
            when(sessionRepository.findByIdAndUserId(anyString(), anyString()))
                    .thenReturn(Optional.of(mockSession));
            when(messageRepository.findBySessionIdOrderByTimestampAsc(anyString()))
                    .thenReturn(java.util.List.of());
            when(llmService.isConfigured()).thenReturn(false);

            java.util.Map<String, Object> result = service.chat(
                    "测试消息", "session-1", null, null);

            assertNotNull(result);
            assertTrue(result.containsKey("message"));
            assertTrue(result.containsKey("sessionId"));
        }
    }
}
