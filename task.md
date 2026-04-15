# Task 003: LLM 服务容错增强

## 状态
- **状态**: ✅ 已完成
- **创建时间**: 2026-04-13
- **完成时间**: 2026-04-13 20:15

## 任务内容
为 LLMService.java 添加:
1. Resilience4j CircuitBreaker ✅
2. @Retry 重试机制 (maxAttempts=3) ✅
3. Fallback 降级方法 (GPT-4 → GPT-3.5) ✅
4. @Counted Token 统计 ✅
5. 统一配置前缀 geonexus.llm.* ✅

## 实现细节

### 1. 新增依赖 (pom.xml)
- io.github.resilience4j:resilience4j-spring-boot3 (2.2.0)
- io.github.resilience4j:resilience4j-micrometer (2.2.0)
- io.github.resilience4j:resilience4j-reactor (2.2.0)
- io.micrometer:micrometer-registry-prometheus

### 2. LLMService.java 改动
- 配置前缀从 `geonexus.model.*` 改为 `geonexus.llm.*`
- chat() 方法添加 @CircuitBreaker(name = "llm", fallbackMethod = "chatFallback")
- chat() 方法添加 @Retry(name = "llm", maxAttempts = 3)
- chat() 方法添加 @Counted(value = "llm.chat")
- chatStream() 方法同样添加熔断、重试
- 新增 chatFallback() 降级方法 (GPT-4 → GPT-3.5)
- 新增 countPromptTokens/completionTokens/totalTokens 统计方法
- 新增 countTokenUsage() 解析各平台Token响应

### 3. application.yml 改动
- 新增 geonexus.llm 配置节点 (统一LLM配置)
- 新增 resilience4j.circuitbreaker.instances.llm 配置
- 新增 resilience4j.retry.instances.llm 配置

### 4. 测试类
- 新增 LLMServiceTest.java 测试用例

## 文件变更
- backend/pom.xml - 添加resilience4j依赖
- backend/src/main/java/com/geonexus/service/LLMService.java - 容错增强
- backend/src/main/resources/application.yml - Resilience4j配置
- backend/src/test/java/com/geonexus/service/LLMServiceTest.java - 测试类
