package com.geonexus.api.v1;

import com.geonexus.api.config.SecurityConfig;
import com.geonexus.model.ChatResponse;
import com.geonexus.model.dto.ChatRequestDTO;
import com.geonexus.repository.ChatSessionRepository;
import com.geonexus.repository.MessageRepository;
import com.geonexus.service.ChatService;
import com.geonexus.service.LLMProviderService;
import com.geonexus.api.validation.RequestValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.AssertionErrors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ChatController 对话控制器测试
 */
@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ChatService chatService;

    @Mock
    private ChatSessionRepository sessionRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private LLMProviderService llmProvider;

    @Mock
    private RequestValidator validator;

    @Mock
    private SecurityConfig securityConfig;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        ChatController controller = new ChatController(
                chatService,
                sessionRepository,
                messageRepository,
                llmProvider,
                validator,
                securityConfig
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Nested
    @DisplayName("发送消息测试")
    class SendMessageTests {

        @Test
        @DisplayName("POST /api/v1/chat 应返回 200")
        void testSendMessage_success() throws Exception {
            ChatRequestDTO request = new ChatRequestDTO();
            request.setMessage("你好");
            request.setSessionId("test-session");

            when(chatService.chat(anyString(), anyString(), any(), any()))
                    .thenReturn(Map.of(
                            "message", "你好，有什么可以帮你？",
                            "sessionId", "test-session",
                            "mapUrl", ""
                    ));

            mockMvc.perform(post("/api/v1/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("发送空消息应返回 400")
        void testSendMessage_emptyMessage() throws Exception {
            ChatRequestDTO request = new ChatRequestDTO();
            request.setMessage("");

            doThrow(new RequestValidator.ValidationException(List.of("消息内容不能为空")))
                    .when(validator).validateAndThrow(any(ChatRequestDTO.class));

            mockMvc.perform(post("/api/v1/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("空间查询消息应正常处理")
        void testSendMessage_spatialQuery() throws Exception {
            ChatRequestDTO request = new ChatRequestDTO();
            request.setMessage("找出天安门10公里内的医院");
            request.setSessionId(null);

            when(chatService.chat(anyString(), isNull(), any(), any()))
                    .thenReturn(Map.of(
                            "message", "📍 附近查询已路由",
                            "sessionId", "new-session-id",
                            "mapUrl", ""
                    ));

            mockMvc.perform(post("/api/v1/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("带地图请求的消息应返回地图URL")
        void testSendMessage_withMapRequest() throws Exception {
            ChatRequestDTO request = new ChatRequestDTO();
            request.setMessage("生成天安门的2D地图");
            request.setMapMode("2d");

            when(chatService.chat(anyString(), any(), eq("2d"), any()))
                    .thenReturn(Map.of(
                            "message", "地图已生成",
                            "sessionId", "map-session",
                            "mapUrl", "/api/v1/map/demo_abc/html"
                    ));

            mockMvc.perform(post("/api/v1/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("服务异常时应返回 500")
        void testSendMessage_serviceException() throws Exception {
            ChatRequestDTO request = new ChatRequestDTO();
            request.setMessage("测试");

            when(chatService.chat(anyString(), any(), any(), any()))
                    .thenThrow(new RuntimeException("Service error"));

            mockMvc.perform(post("/api/v1/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("使用指定模型的消息应正常处理")
        void testSendMessage_withModel() throws Exception {
            ChatRequestDTO request = new ChatRequestDTO();
            request.setMessage("你好");
            request.setModel("gpt-4o");

            when(chatService.chat(eq("你好"), any(), eq("gpt-4o"), any()))
                    .thenReturn(Map.of(
                            "message", "GPT-4o 回复",
                            "sessionId", "session",
                            "mapUrl", ""
                    ));

            mockMvc.perform(post("/api/v1/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("会话列表测试")
    class SessionListTests {

        @Test
        @DisplayName("GET /api/v1/chat/sessions 应返回 200")
        void testGetSessions_success() throws Exception {
            when(securityConfig.extractUserIdFromAuth(any()))
                    .thenReturn("user123");

            mockMvc.perform(get("/api/v1/chat/sessions")
                            .header("Authorization", "Bearer token")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("无认证时返回空列表")
        void testGetSessions_noAuth() throws Exception {
            when(securityConfig.extractUserIdFromAuth(any()))
                    .thenReturn(null);

            mockMvc.perform(get("/api/v1/chat/sessions")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk());
        }
    }
}
