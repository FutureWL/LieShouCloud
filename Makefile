# ============================================================
# 猎手云 Pro · 常用命令
# ============================================================

COMPOSE = docker compose -f docker-compose.yml -f docker-compose.local.yml

.PHONY: help up down restart build logs ps health seed status

## 默认显示帮助
help:
	@echo "猎手云 Pro · 常用命令"
	@echo ""
	@grep -E '^## ' $(MAKEFILE_LIST) | sed 's/## //'

## up: 一键启动（含构建镜像）
up:
	./scripts/up.sh

## down: 停止所有服务（保留数据卷）
down:
	./scripts/down.sh

## restart: 重启后端微服务
restart:
	$(COMPOSE) restart user admin auth approval gateway

## build: 构建后端镜像
build:
	$(COMPOSE) build user admin auth approval gateway

## logs: 跟踪查看日志
logs:
	$(COMPOSE) logs -f --tail=100

## ps: 查看容器状态
ps:
	$(COMPOSE) ps

## health: 网关健康检查
health:
	@curl -s http://localhost:9001/actuator/health; echo

## seed: 创建 admin 账号 + 测登录
seed:
	./scripts/seed-admin.sh

## status: 完整状态检查
status:
	./scripts/status.sh
