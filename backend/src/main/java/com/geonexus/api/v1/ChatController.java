package com.geonexus.api.v1;

import com.geonexus.common.ApiResponse;
import com.geonexus.model.ChatRequest;
import com.geonexus.model.ChatResponse;
import com.geonexus.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 聊天控制器 - 实时聊天消息 REST API
 */
@RestController
@RequestMapping("/api/v1/chat")
@Tag(name = "聊天", description = "实时聊天消息处理")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    @Operation(summary = "发送消息")
    public ResponseEntity<ApiResponse<ChatResponse>> sendMessage(@RequestBody ChatRequest request) {
        ChatResponse response = chatService.processChat(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/simple")
    @Operation(summary = "发送消息（简化参数）")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendSimpleMessage(
            @RequestParam String message,
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String mapMode) {
        Map<String, Object> response = chatService.chat(message, sessionId, model, mapMode);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/history/{sessionId}")
    @Operation(summary = "获取会话历史")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getHistory(@PathVariable String sessionId) {
        List<Map<String, String>> history = chatService.getHistory(sessionId);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    @DeleteMapping("/session/{sessionId}")
    @Operation(summary = "删除会话")
    public ResponseEntity<ApiResponse<Void>> deleteSession(@PathVariable String sessionId) {
        chatService.deleteSession(sessionId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/sessions")
    @Operation(summary = "获取用户会话列表")
    public ResponseEntity<ApiResponse<List<com.geonexus.domain.ChatSessionEntity>>> getUserSessions(
            @RequestParam(defaultValue = "anonymous") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<com.geonexus.domain.ChatSessionEntity> sessions = chatService.getUserSessions(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }
}
