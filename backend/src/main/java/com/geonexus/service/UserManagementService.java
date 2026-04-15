package com.geonexus.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户管理与认证服务 - 完整的用户体系和登录认证
 */
@Slf4j
@Service
public class UserManagementService {
    
    private final CacheService cacheService;
    private final BCryptPasswordEncoder passwordEncoder;
    
    // 用户存储
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, UserProfile> profiles = new ConcurrentHashMap<>();
    private final Map<String, AuthToken> tokens = new ConcurrentHashMap<>();
    private final Map<String, Role> roles = new ConcurrentHashMap<>();
    private final Map<String, Permission> permissions = new ConcurrentHashMap<>();
    
    private static final String USER_PREFIX = "user:";
    private static final String TOKEN_PREFIX = "auth:token:";
    private static final String ROLE_PREFIX = "role:";
    
    public UserManagementService(CacheService cacheService) {
        this.cacheService = cacheService;
        this.passwordEncoder = new BCryptPasswordEncoder();
        initializeDefaultRoles();
    }
    
    // ==================== 初始化 ====================
    
    private void initializeDefaultRoles() {
        // 管理员
        permissions.put("admin:all", Permission.builder()
                .id("admin:all").name("全部权限").description("管理员权限").build());
        
        roles.put("admin", Role.builder()
                .id("admin")
                .name("管理员")
                .description("系统管理员")
                .permissions(Set.of("admin:all"))
                .isSystem(true)
                .build());
        
        // GIS分析师
        permissions.put("gis:read", Permission.builder()
                .id("gis:read").name("读取GIS数据").build());
        permissions.put("gis:write", Permission.builder()
                .id("gis:write").name("写入GIS数据").build());
        permissions.put("gis:analysis", Permission.builder()
                .id("gis:analysis").name("执行GIS分析").build());
        permissions.put("gis:publish", Permission.builder()
                .id("gis:publish").name("发布地图").build());
        
        roles.put("analyst", Role.builder()
                .id("analyst")
                .name("GIS分析师")
                .description("GIS数据分析师")
                .permissions(Set.of("gis:read", "gis:write", "gis:analysis"))
                .isSystem(true)
                .build());
        
        // 普通用户
        roles.put("user", Role.builder()
                .id("user")
                .name("普通用户")
                .description("普通注册用户")
                .permissions(Set.of("gis:read"))
                .isSystem(true)
                .build());
        
        // 访客
        roles.put("guest", Role.builder()
                .id("guest")
                .name("访客")
                .description("未登录访客")
                .permissions(Set.of())
                .isSystem(true)
                .build());
    }
    
    // ==================== 用户注册与登录 ====================
    
    /**
     * 用户注册
     */
    public User register(RegisterRequest request) {
        // 验证邮箱唯一性
        if (findByEmail(request.getEmail()) != null) {
            throw new UserAlreadyExistsException("邮箱已被注册: " + request.getEmail());
        }
        
        // 验证用户名唯一性
        if (findByUsername(request.getUsername()) != null) {
            throw new UserAlreadyExistsException("用户名已被使用: " + request.getUsername());
        }
        
        User user = User.builder()
                .id(UUID.randomUUID().toString())
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(hashPassword(request.getPassword()))
                .displayName(request.getDisplayName() != null ? request.getDisplayName() : request.getUsername())
                .avatar(request.getAvatar())
                .status("active")
                .roles(Set.of("user"))
                .createdAt(System.currentTimeMillis())
                .build();
        
        users.put(user.getId(), user);
        saveUser(user);
        
        // 创建用户Profile
        UserProfile profile = UserProfile.builder()
                .userId(user.getId())
                .build();
        profiles.put(user.getId(), profile);
        
        log.info("用户注册成功: id={}, username={}", user.getId(), user.getUsername());
        
        return user;
    }
    
    /**
     * 用户登录
     */
    public LoginResult login(LoginRequest request) {
        User user = findByEmail(request.getEmail());
        
        if (user == null) {
            user = findByUsername(request.getEmail());
        }
        
        if (user == null) {
            throw new AuthenticationException("用户不存在");
        }
        
        if (!verifyPassword(request.getPassword(), user.getPasswordHash())) {
            throw new AuthenticationException("密码错误");
        }
        
        if (!"active".equals(user.getStatus())) {
            throw new AuthenticationException("账户已被禁用");
        }
        
        // 生成Token
        String accessToken = generateToken();
        String refreshToken = generateToken();
        
        AuthToken authToken = AuthToken.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .createdAt(System.currentTimeMillis())
                .expiresAt(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L) // 7天
                .build();
        
        tokens.put(accessToken, authToken);
        saveToken(authToken);
        
        // 更新最后登录时间
        user.setLastLoginAt(System.currentTimeMillis());
        saveUser(user);
        
        log.info("用户登录成功: userId={}", user.getId());
        
        return LoginResult.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(7 * 24 * 60 * 60)
                .user(mapToUserDTO(user))
                .build();
    }
    
    /**
     * 刷新Token
     */
    public LoginResult refreshToken(String refreshToken) {
        AuthToken authToken = null;
        for (AuthToken at : tokens.values()) {
            if (at.getRefreshToken().equals(refreshToken)) {
                authToken = at;
                break;
            }
        }
        
        if (authToken == null) {
            throw new AuthenticationException("无效的刷新令牌");
        }
        
        // Refresh token有效期检查（30天）
        long refreshExpiry = 30L * 24 * 60 * 60 * 1000L;
        if (System.currentTimeMillis() - authToken.getCreatedAt() > refreshExpiry) {
            tokens.remove(authToken.getToken());
            throw new AuthenticationException("刷新令牌已过期，请重新登录");
        }
        
        User user = users.get(authToken.getUserId());
        if (user == null) {
            throw new AuthenticationException("用户不存在");
        }
        
        // 生成新Token
        String newAccessToken = generateToken();
        String newRefreshToken = generateToken();
        
        // 删除旧Token
        tokens.remove(authToken.getToken());
        
        // 创建新Token
        authToken = AuthToken.builder()
                .token(newAccessToken)
                .refreshToken(newRefreshToken)
                .userId(user.getId())
                .createdAt(System.currentTimeMillis())
                .expiresAt(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L)
                .build();
        
        tokens.put(newAccessToken, authToken);
        saveToken(authToken);
        
        return LoginResult.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(7 * 24 * 60 * 60)
                .user(mapToUserDTO(user))
                .build();
    }
    
    /**
     * 登出
     */
    public void logout(String accessToken) {
        tokens.remove(accessToken);
        log.info("用户登出: token={}", accessToken.substring(0, 10) + "...");
    }
    
    /**
     * 验证Token
     */
    public User validateToken(String accessToken) {
        AuthToken authToken = tokens.get(accessToken);
        
        if (authToken == null) {
            throw new AuthenticationException("Token不存在");
        }
        
        if (System.currentTimeMillis() > authToken.getExpiresAt()) {
            tokens.remove(accessToken);
            throw new AuthenticationException("Token已过期");
        }
        
        User user = users.get(authToken.getUserId());
        if (user == null) {
            throw new AuthenticationException("用户不存在");
        }
        
        return user;
    }
    
    // ==================== 用户管理 ====================
    
    /**
     * 获取用户
     */
    public User getUser(String userId) {
        User user = users.get(userId);
        if (user == null) {
            throw new UserNotFoundException("用户不存在: " + userId);
        }
        return user;
    }
    
    /**
     * 获取用户公开信息
     */
    public UserDTO getUserPublicInfo(String userId) {
        return mapToUserDTO(getUser(userId));
    }
    
    /**
     * 更新用户信息
     */
    public User updateUser(String userId, UpdateUserRequest request) {
        User user = getUser(userId);
        
        if (request.getDisplayName() != null) {
            user.setDisplayName(request.getDisplayName());
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        
        user.setUpdatedAt(System.currentTimeMillis());
        saveUser(user);
        
        return user;
    }
    
    /**
     * 修改密码
     */
    public void changePassword(String userId, String oldPassword, String newPassword) {
        User user = getUser(userId);
        
        if (!verifyPassword(oldPassword, user.getPasswordHash())) {
            throw new AuthenticationException("原密码错误");
        }
        
        user.setPasswordHash(hashPassword(newPassword));
        user.setUpdatedAt(System.currentTimeMillis());
        saveUser(user);
        
        log.info("用户修改密码: userId={}", userId);
    }
    
    /**
     * 重置密码（管理员）
     */
    public void resetPassword(String userId, String newPassword) {
        User user = getUser(userId);
        user.setPasswordHash(hashPassword(newPassword));
        user.setUpdatedAt(System.currentTimeMillis());
        saveUser(user);
        
        log.info("管理员重置用户密码: userId={}", userId);
    }
    
    /**
     * 删除用户
     */
    public void deleteUser(String userId) {
        User user = users.get(userId);
        if (user != null) {
            user.setStatus("deleted");
            user.setDeletedAt(System.currentTimeMillis());
            saveUser(user);
            log.info("删除用户: userId={}", userId);
        }
    }
    
    /**
     * 禁用/启用用户
     */
    public void setUserStatus(String userId, String status) {
        User user = getUser(userId);
        user.setStatus(status);
        user.setUpdatedAt(System.currentTimeMillis());
        saveUser(user);
        
        log.info("设置用户状态: userId={}, status={}", userId, status);
    }
    
    // ==================== 角色权限 ====================
    
    /**
     * 分配角色
     */
    public void assignRole(String userId, String roleId) {
        User user = getUser(userId);
        Role role = getRole(roleId);
        
        user.getRoles().add(roleId);
        user.setUpdatedAt(System.currentTimeMillis());
        saveUser(user);
        
        log.info("分配角色: userId={}, roleId={}", userId, roleId);
    }
    
    /**
     * 移除角色
     */
    public void removeRole(String userId, String roleId) {
        User user = getUser(userId);
        
        if ("admin".equals(roleId) && user.getRoles().contains("admin")) {
            throw new IllegalArgumentException("不能移除管理员角色");
        }
        
        user.getRoles().remove(roleId);
        user.setUpdatedAt(System.currentTimeMillis());
        saveUser(user);
        
        log.info("移除角色: userId={}, roleId={}", userId, roleId);
    }
    
    /**
     * 检查权限
     */
    public boolean hasPermission(String userId, String permission) {
        User user = getUser(userId);
        
        for (String roleId : user.getRoles()) {
            Role role = roles.get(roleId);
            if (role != null && role.getPermissions().contains(permission)) {
                return true;
            }
        }
        
        return false;
    }
    
    // ==================== 私有辅助方法 ====================
    
    private User findByEmail(String email) {
        for (User user : users.values()) {
            if (user.getEmail().equalsIgnoreCase(email)) {
                return user;
            }
        }
        return null;
    }
    
    private User findByUsername(String username) {
        for (User user : users.values()) {
            if (user.getUsername().equals(username)) {
                return user;
            }
        }
        return null;
    }
    
    private void saveUser(User user) {
        String key = USER_PREFIX + user.getId();
        cacheService.set(key, user);
    }
    
    private void saveToken(AuthToken token) {
        String key = TOKEN_PREFIX + token.getToken();
        cacheService.set(key, token);
    }
    
    private Role getRole(String roleId) {
        Role role = roles.get(roleId);
        if (role == null) {
            throw new RoleNotFoundException("角色不存在: " + roleId);
        }
        return role;
    }
    
    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "") + 
               UUID.randomUUID().toString().replace("-", "");
    }
    
    private String hashPassword(String password) {
        // 使用BCrypt进行安全密码哈希（包含盐值）
        return passwordEncoder.encode(password);
    }
    
    private boolean verifyPassword(String password, String hash) {
        // 使用BCrypt验证密码
        return passwordEncoder.matches(password, hash);
    }
    
    private UserDTO mapToUserDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .avatar(user.getAvatar())
                .status(user.getStatus())
                .roles(user.getRoles())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
    
    // ==================== 内部类 ====================
    
    @lombok.Data
    @lombok.Builder
    public static class User {
        private String id;
        private String username;
        private String email;
        private String passwordHash;
        private String displayName;
        private String avatar;
        private String phone;
        private String bio;
        private String status; // active, disabled, deleted
        private Set<String> roles;
        private Long createdAt;
        private Long updatedAt;
        private Long lastLoginAt;
        private Long deletedAt;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class UserDTO {
        private String id;
        private String username;
        private String email;
        private String displayName;
        private String avatar;
        private String status;
        private Set<String> roles;
        private Long createdAt;
        private Long lastLoginAt;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class UserProfile {
        private String userId;
        private String organization;
        private String department;
        private String title;
        private Map<String, Object> preferences;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class Role {
        private String id;
        private String name;
        private String description;
        private Set<String> permissions;
        private boolean isSystem;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class Permission {
        private String id;
        private String name;
        private String description;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class AuthToken {
        private String token;
        private String refreshToken;
        private String userId;
        private Long createdAt;
        private Long expiresAt;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class RegisterRequest {
        private String username;
        private String email;
        private String password;
        private String displayName;
        private String avatar;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class LoginRequest {
        private String email;
        private String password;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class LoginResult {
        private String accessToken;
        private String refreshToken;
        private int expiresIn;
        private UserDTO user;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class UpdateUserRequest {
        private String displayName;
        private String avatar;
        private String phone;
        private String bio;
    }
    
    // 异常
    public static class UserAlreadyExistsException extends RuntimeException {
        public UserAlreadyExistsException(String message) { super(message); }
    }
    
    public static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(String message) { super(message); }
    }
    
    public static class AuthenticationException extends RuntimeException {
        public AuthenticationException(String message) { super(message); }
    }
    
    public static class RoleNotFoundException extends RuntimeException {
        public RoleNotFoundException(String message) { super(message); }
    }
}
