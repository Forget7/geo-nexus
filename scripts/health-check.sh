#!/bin/bash
# GeoAgent 健康检查脚本

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# API地址
API_URL=${API_URL:-http://localhost:8000}
GEO_URL=${GEO_URL:-http://localhost:8080}

echo "======================================"
echo "GeoAgent 健康检查"
echo "======================================"
echo ""

# 检查API服务
check_api() {
    echo -n "检查 API 服务 (${API_URL})... "
    
    if curl -sf "${API_URL}/health" > /dev/null 2>&1; then
        echo -e "${GREEN}OK${NC}"
        return 0
    else
        echo -e "${RED}失败${NC}"
        return 1
    fi
}

# 检查GeoServer
check_geoserver() {
    echo -n "检查 GeoServer (${GEO_URL})... "
    
    if curl -sf "${GEO_URL}/geoserver/rest/about/version.json" > /dev/null 2>&1; then
        echo -e "${GREEN}OK${NC}"
        return 0
    else
        echo -e "${YELLOW}跳过 (未配置或不可用)${NC}"
        return 0
    fi
}

# 检查PostgreSQL
check_postgres() {
    echo -n "检查 PostgreSQL... "
    
    if PGPASSWORD=${DB_PASSWORD:-geoagent123} psql -h ${DB_HOST:-localhost} -U geoagent -d geoagent -c "SELECT 1" > /dev/null 2>&1; then
        echo -e "${GREEN}OK${NC}"
        return 0
    else
        echo -e "${RED}失败${NC}"
        return 1
    fi
}

# 检查Redis
check_redis() {
    echo -n "检查 Redis... "
    
    if redis-cli -h ${REDIS_HOST:-localhost} ping > /dev/null 2>&1; then
        echo -e "${GREEN}OK${NC}"
        return 0
    else
        echo -e "${RED}失败${NC}"
        return 1
    fi
}

# 检查前端
check_frontend() {
    echo -n "检查 Frontend... "
    
    if curl -sf "http://localhost:3000" > /dev/null 2>&1; then
        echo -e "${GREEN}OK${NC}"
        return 0
    else
        echo -e "${YELLOW}跳过 (未启动或不可用)${NC}"
        return 0
    fi
}

# 获取API版本
get_api_version() {
    echo -n "API 版本: "
    VERSION=$(curl -sf "${API_URL}/api/v1" 2>/dev/null | grep -o '"version":"[^"]*"' | cut -d'"' -f4 || echo "未知")
    echo -e "${GREEN}${VERSION}${NC}"
}

# 获取系统状态
get_system_status() {
    echo ""
    echo "======================================"
    echo "详细状态"
    echo "======================================"
    
    # API健康检查详情
    echo ""
    echo "API 服务:"
    curl -sf "${API_URL}/health" 2>/dev/null | jq '.' || echo "无法获取详情"
    
    # 数据库连接数
    echo ""
    echo "数据库连接:"
    PGPASSWORD=${DB_PASSWORD:-geoagent123} psql -h ${DB_HOST:-localhost} -U geoagent -d geoagent -c "SELECT count(*) as connections FROM pg_stat_activity WHERE datname='geoagent';" 2>/dev/null || echo "无法获取"
    
    # Redis信息
    echo ""
    echo "Redis 状态:"
    redis-cli -h ${REDIS_HOST:-localhost} info clients 2>/dev/null | grep connected_clients || echo "无法获取"
}

# 主程序
main() {
    local failed=0
    
    check_api || failed=$((failed + 1))
    check_redis || failed=$((failed + 1))
    check_postgres || failed=$((failed + 1))
    check_geoserver || true
    check_frontend || true
    
    get_api_version
    
    if [ "$1" == "--verbose" ] || [ "$1" == "-v" ]; then
        get_system_status
    fi
    
    echo ""
    echo "======================================"
    
    if [ $failed -eq 0 ]; then
        echo -e "${GREEN}所有检查通过!${NC}"
        exit 0
    else
        echo -e "${RED}${failed} 项检查失败${NC}"
        exit 1
    fi
}

# 帮助信息
if [ "$1" == "--help" ] || [ "$1" == "-h" ]; then
    echo "用法: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  -v, --verbose    显示详细信息"
    echo "  -h, --help      显示帮助信息"
    echo ""
    echo "环境变量:"
    echo "  API_URL         API服务地址 (默认: http://localhost:8000)"
    echo "  GEO_URL         GeoServer地址 (默认: http://localhost:8080)"
    echo "  DB_HOST         PostgreSQL主机"
    echo "  DB_PASSWORD     PostgreSQL密码"
    echo "  REDIS_HOST      Redis主机"
    exit 0
fi

main "$@"
