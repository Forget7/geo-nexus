FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# 复制pom.xml
COPY pom.xml .

# 下载依赖（利用缓存）
RUN mvn dependency:go-offline -B

# 复制源代码
COPY src ./src

# 构建
RUN mvn package -DskipTests -B

# ========== 运行镜像 ==========
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 安装GIS依赖
RUN apk add --no-cache \
    geos \
    proj \
    gdal \
    geos-dev \
    proj-dev \
    gdal-dev

# 复制JAR
COPY --from=builder /app/target/*.jar app.jar

# 创建数据目录
RUN mkdir -p /app/data/uploads /app/data/output

# 暴露端口
EXPOSE 8000

# 启动
ENTRYPOINT ["java", "-jar", "app.jar"]
