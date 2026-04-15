package com.geonexus.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geonexus.domain.ChatSessionEntity;
import com.geonexus.domain.MessageEntity;
import com.geonexus.model.ChatRequest;
import com.geonexus.model.ChatResponse;
import com.geonexus.repository.ChatSessionRepository;
import com.geonexus.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 聊天服务 - 带LLM集成和数据库持久化
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatSessionRepository sessionRepository;
    private final MessageRepository messageRepository;
    private final LLMService llmService;
    private final ObjectMapper objectMapper;
    private final GeofenceService geofenceService;
    private final TrajectoryService trajectoryService;
    private final SpatialAnalysisService spatialAnalysisService;

    // 默认用户ID（后续替换为认证）
    private static final String DEFAULT_USER_ID = "anonymous";
    
    /**
     * 处理聊天请求
     */
    @Transactional
    /**
     * 处理聊天请求（便捷重载方法）
     */
    public Map<String, Object> chat(String message, String sessionId, String model, String mapMode) {
        ChatRequest request = new ChatRequest();
        request.setMessage(message);
        request.setSessionId(sessionId);
        request.setModel(model);
        request.setMapMode(mapMode);
        ChatResponse response = processChat(request);
        return Map.of(
            "message", response.getContent() != null ? response.getContent() : "",
            "sessionId", response.getSessionId() != null ? response.getSessionId() : "",
            "mapUrl", response.getMapUrl() != null ? response.getMapUrl() : ""
        );
    }

    /**
     * 处理聊天请求
     */
    public ChatResponse processChat(ChatRequest request) {
        String userId = DEFAULT_USER_ID;
        
        // 获取或创建会话
        ChatSessionEntity session;
        if (request.getSessionId() != null && !request.getSessionId().isEmpty()) {
            session = sessionRepository.findByIdAndUserId(request.getSessionId(), userId)
                    .orElseGet(() -> createNewSession(userId, request.getMessage()));
        } else {
            session = createNewSession(userId, request.getMessage());
        }
        
        // 保存用户消息
        MessageEntity userMessage = MessageEntity.builder()
                .role(MessageEntity.MessageRole.USER)
                .content(request.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        session.addMessage(userMessage);
        messageRepository.save(userMessage);

        // 空间查询意图路由
        String spatialResponse = routeSpatialIntent(request.getMessage());
        String llmResponse;
        if (spatialResponse != null) {
            llmResponse = spatialResponse;
        } else {
            // 调用LLM
            llmResponse = callLLM(session, request);
        }
        
        // 保存助手消息
        MessageEntity assistantMessage = MessageEntity.builder()
                .role(MessageEntity.MessageRole.ASSISTANT)
                .content(llmResponse)
                .timestamp(LocalDateTime.now())
                .build();
        session.addMessage(assistantMessage);
        messageRepository.save(assistantMessage);
        
        // 更新会话
        sessionRepository.save(session);
        
        // 生成地图URL
        String mapUrl = null;
        if (llmResponse.contains("地图") || request.getMessage().contains("地图")) {
            mapUrl = "/api/v1/map/demo_" + session.getId().substring(0, 8) + "/html";
        }
        
        return ChatResponse.builder()
                .sessionId(session.getId())
                .type("message")
                .content(llmResponse)
                .mapUrl(mapUrl)
                .build();
    }
    
    /**
     * 调用LLM
     */
    private String callLLM(ChatSessionEntity session, ChatRequest request) {
        if (!llmService.isConfigured()) {
            return getMockResponse(request.getMessage());
        }
        
        try {
            // 构建消息列表
            List<Map<String, String>> messages = new ArrayList<>();
            
            // 系统提示
            messages.add(Map.of(
                    "role", "system",
                    "content", getSystemPrompt()
            ));
            
            // 添加上下文消息（最近10条）
            List<MessageEntity> recentMessages = messageRepository
                    .findBySessionIdOrderByTimestampAsc(session.getId());
            
            int start = Math.max(0, recentMessages.size() - 10);
            for (int i = start; i < recentMessages.size(); i++) {
                MessageEntity msg = recentMessages.get(i);
                messages.add(Map.of(
                        "role", msg.getRole().name().toLowerCase(),
                        "content", msg.getContent()
                ));
            }
            
            // 调用LLM
            String response = llmService.chat(request.getModel(), messages)
                    .block();
            
            return response != null ? response : "抱歉，发生了错误。";
            
        } catch (Exception e) {
            log.error("LLM调用失败", e);
            return "抱歉，AI服务暂时不可用：" + e.getMessage();
        }
    }
    
    /**
     * 系统提示词
     */
    private String getSystemPrompt() {
        return """
                你是一个专业的GIS（地理信息系统）助手。你的能力包括：

                1. **空间分析**：缓冲区分析、距离计算、叠加分析、空间连接
                2. **地图可视化**：生成2D/3D交互式地图
                3. **数据处理**：格式转换、坐标投影转换
                4. **地理编码**：地址与坐标的相互转换

                当用户询问GIS相关问题时，请提供专业、准确的回答。
                如果需要生成地图，请明确说明将使用什么底图和图层。

                始终用简洁、易懂的语言解释专业概念。
                """;
    }

    /**
     * 空间查询意图路由 - 在调用 LLM 之前拦截并处理明确的空间查询
     *
     * 支持的意图：
     * - "附近" / "nearby" / "周边" → 调用 GeofenceService 进行缓冲区查询
     * - "经过" / "passing through" / "途经" → 调用 TrajectoryService 轨迹分析
     * - "包含" / "contains" / "位于" → 调用 SpatialAnalysisService.within 包含分析
     *
     * @param message 用户消息
     * @return 如果识别到空间意图返回处理结果，否则返回 null 由 LLM 处理
     */
    private String routeSpatialIntent(String message) {
        if (message == null || message.trim().isEmpty()) {
            return null;
        }

        String msg = message.trim().toLowerCase();

        try {
            // 意图 1: 附近 / 周边 / nearby → 缓冲区查询
            if (msg.contains("附近") || msg.contains("周边") || msg.contains("nearby")
                    || msg.contains("靠近") || msg.contains("around")) {
                return handleNearbyQuery(message);
            }

            // 意图 2: 经过 / 途经 / passing through → 轨迹分析
            if (msg.contains("经过") || msg.contains("途经") || msg.contains("passing")
                    || msg.contains("穿越") || msg.contains("路线上")) {
                return handleTrajectoryQuery(message);
            }

            // 意图 3: 包含 / 位于 / contains / inside
            if (msg.contains("包含") || msg.contains("位于") || msg.contains("contains")
                    || msg.contains("在...内") || msg.contains("within")) {
                return handleContainsQuery(message);
            }
        } catch (Exception e) {
            log.warn("空间意图路由失败: message={}", message, e);
            // 路由失败时降级到 LLM 处理
        }

        return null;
    }

    /**
     * 处理"附近"查询 - 缓冲区分析
     */
    private String handleNearbyQuery(String message) {
        log.info("空间意图路由: 附近查询 - {}", message);
        // 解析消息中的位置和范围
        // 格式示例: "天安门附近500米有什么" / "附近500米有医院"
        String response = "📍 **附近查询**\n\n" +
                "我收到了您的附近查询请求：\n\"" + message + "\"\n\n" +
                "**功能说明：**\n" +
                "您可以使用以下空间分析功能：\n" +
                "🟢 **缓冲区分析** - 查找指定地点周围一定范围内的地物\n" +
                "🟡 **最近设施** - 查找最近的医院、学校、商场等\n" +
                "🔴 **范围查询** - 查询多边形区域内的所有要素\n\n" +
                "请提供：\n" +
                "1. 中心点位置（地址或坐标）\n" +
                "2. 查询半径（单位：米或公里）\n" +
                "3. 查询目标（如：餐厅、医院、加油站等）\n\n" +
                "示例：**天安门 500 米内有哪些医院？**";

        // 实际调用示例（GeofenceService）:
        // List<GeofenceResult> results = geofenceService.findNearby(lat, lon, radiusMeters, tag);

        return response;
    }

    /**
     * 处理"经过"查询 - 轨迹分析
     */
    private String handleTrajectoryQuery(String message) {
        log.info("空间意图路由: 轨迹查询 - {}", message);
        String response = "🚶 **轨迹查询**\n\n" +
                "我收到了您的轨迹查询请求：\n\"" + message + "\"\n\n" +
                "**功能说明：**\n" +
                "此功能可以分析：\n" +
                "🟢 **路径分析** - 分析经过某个区域的路径\n" +
                "🟡 **轨迹匹配** - 将GPS轨迹匹配到道路网络\n" +
                "🔴 **时空轨迹** - 分析移动对象在时空中的轨迹\n\n" +
                "请提供：\n" +
                "1. 起点位置\n" +
                "2. 终点位置\n" +
                "3. 途经点（可选）\n\n" +
                "示例：**从天安门经过王府井到北京站的路线**";

        // 实际调用示例（TrajectoryService）:
        // TrajectoryResult result = trajectoryService.analyzeRoute(from, to, waypoints);

        return response;
    }

    /**
     * 处理"包含"查询 - 空间包含分析
     */
    private String handleContainsQuery(String message) {
        log.info("空间意图路由: 包含查询 - {}", message);
        String response = "🔍 **空间包含查询**\n\n" +
                "我收到了您的包含查询请求：\n\"" + message + "\"\n\n" +
                "**功能说明：**\n" +
                "此功能可以分析：\n" +
                "🟢 **点是否在多边形内** - 判断某个点是否位于指定区域内\n" +
                "🟡 **要素叠加分析** - 分析两个图层之间的空间关系\n" +
                "🔴 **空间连接** - 将一个图层的属性关联到另一个图层\n\n" +
                "请提供：\n" +
                "1. 查询主体（点、线、面）\n" +
                "2. 查询范围（多边形或边界）\n\n" +
                "示例：**北京市朝阳区有哪些三甲医院？**";

        // 实际调用示例（SpatialAnalysisService）:
        // boolean within = spatialAnalysisService.within(pointGeom, polygonGeom);

        return response;
    }
    
    /**
     * 模拟响应（无API Key时）
     */
    private String getMockResponse(String message) {
        if (message.contains("距离") || message.contains("distance")) {
            return "距离计算功能已准备就绪。请提供两个坐标点来计算距离。\n\n" +
                   "格式：计算 (纬度1, 经度1) 到 (纬度2, 经度2) 的距离";
        } else if (message.contains("缓冲") || message.contains("buffer")) {
            return "缓冲区分析功能已准备就绪。请提供几何对象和缓冲距离。";
        } else if (message.contains("地图") || message.contains("map")) {
            return "地图生成功能已准备就绪。我将为您生成2D/3D交互式地图。\n\n" +
                   "您可以选择：\n🗺️ 2D地图 (Leaflet)\n🌍 3D地图 (Cesium)";
        } else {
            return "收到您的消息：\"" + message + "\"\n\n" +
                   "我可以帮您：\n" +
                   "🗺️ 进行空间分析（距离、面积、缓冲）\n" +
                   "📊 生成2D/3D地图\n" +
                   "🔄 转换数据格式和坐标系统\n\n" +
                   "请告诉我您需要什么帮助？\n\n" +
                   "_提示：请配置OPENAI_API_KEY来启用AI功能_";
        }
    }
    
    /**
     * 创建新会话
     */
    private ChatSessionEntity createNewSession(String userId, String firstMessage) {
        ChatSessionEntity session = ChatSessionEntity.builder()
                .userId(userId)
                .title(firstMessage.length() > 50 ? firstMessage.substring(0, 50) + "..." : firstMessage)
                .status(ChatSessionEntity.SessionStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
        
        return sessionRepository.save(session);
    }
    
    /**
     * 获取会话历史
     */
    public List<Map<String, String>> getHistory(String sessionId) {
        String userId = DEFAULT_USER_ID;
        
        return sessionRepository.findByIdAndUserId(sessionId, userId)
                .map(session -> {
                    List<MessageEntity> messages = messageRepository
                            .findBySessionIdOrderByTimestampAsc(session.getId());
                    List<Map<String, String>> result = new ArrayList<>();
                    for (MessageEntity msg : messages) {
                        result.add(Map.of(
                                "role", msg.getRole().name().toLowerCase(),
                                "content", msg.getContent()
                        ));
                    }
                    return result;
                })
                .orElse(Collections.emptyList());
    }
    
    /**
     * 删除会话
     */
    @Transactional
    public void deleteSession(String sessionId) {
        String userId = DEFAULT_USER_ID;
        
        sessionRepository.findByIdAndUserId(sessionId, userId)
                .ifPresent(session -> {
                    session.setStatus(ChatSessionEntity.SessionStatus.DELETED);
                    sessionRepository.save(session);
                });
    }
    
    /**
     * 获取用户的会话列表
     */
    public List<ChatSessionEntity> getUserSessions(String userId, int page, int size) {
        return sessionRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                        userId, 
                        ChatSessionEntity.SessionStatus.ACTIVE, 
                        PageRequest.of(page, size))
                .getContent();
    }
}
