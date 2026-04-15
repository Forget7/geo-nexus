package com.geonexus.api.v1;

import com.geonexus.service.PushService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 推送订阅控制器 - Web Push 推送订阅管理
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/push")
@Tag(name = "Push", description = "推送订阅 - Web Push 推送订阅管理")
@CrossOrigin(origins = "http://localhost:*")
@RequiredArgsConstructor
public class PushController {

    private final PushService pushService;

    @PostMapping("/subscribe")
    @Operation(summary = "订阅推送", description = "保存用户的 Web Push 订阅信息")
    public ResponseEntity<Map<String, Object>> subscribe(
            @RequestParam String userId,
            @RequestBody String subscriptionJson) {
        pushService.subscribe(userId, subscriptionJson);
        return ResponseEntity.ok(Map.of("success", true, "message", "订阅成功"));
    }

    @DeleteMapping("/unsubscribe")
    @Operation(summary = "取消订阅", description = "移除用户的 Web Push 订阅信息")
    public ResponseEntity<Map<String, Object>> unsubscribe(@RequestParam String userId) {
        pushService.unsubscribe(userId);
        return ResponseEntity.ok(Map.of("success", true, "message", "取消订阅成功"));
    }
}
