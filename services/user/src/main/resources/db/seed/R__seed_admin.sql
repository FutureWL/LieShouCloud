-- ============================================================
-- R__seed_admin.sql · 开发环境种子数据（repeatable migration）
--
-- 种子数据策略（ADR-0027 · 环境分离 location）：
--   - 本文件位于 classpath:db/seed，**仅**在 dev / docker profile 启用
--     （application-dev.yml / application-docker.yml 追加该 location）
--   - prod 走 base application.yml（classpath:db/migration），不含本文件
--   - 幂等：ON CONFLICT DO NOTHING —— 重复执行不报错、不重复插入
--   - Flyway repeatable migration：内容变更（checksum）时自动重跑
--
-- 内容：猎手猫租户（code=huntercat）的平台管理员账号
--   用户名 admin / 默认密码 admin123（⚠️ 仅开发环境；prod 用邀请/注册创建真实账号）
--   角色：PLATFORM_ADMIN（跨租户平台运营，前端 access 模型全权限）
--   参考：services/scripts/seed-admin.sh（HTTP 创建路径，保留作手工兜底）
-- ============================================================

-- 管理员账号（password_hash = BCrypt("admin123", strength 10)，与
-- user-service / auth-service 的 BCryptPasswordEncoder 默认强度一致，已用
-- spring-security-crypto 6.5.6 实测 matches=true）
INSERT INTO users (tenant_id, username, display_name, email, password_hash, status)
SELECT t.id, 'admin', '平台管理员', 'admin@huntercat.cn',
       '$2b$10$/zX3Q157zlkzZ3dcg41yO.oGKDLMdPL6ZGHQS8Bt9iJVlC1u/u4ee', 'ACTIVE'
FROM tenants t
WHERE t.code = 'huntercat'
ON CONFLICT (tenant_id, username) DO NOTHING;

-- 角色关联（PLATFORM_ADMIN；user_roles PK 幂等）
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN tenants t ON t.id = u.tenant_id AND t.code = 'huntercat'
JOIN roles r ON r.code = 'PLATFORM_ADMIN'
WHERE u.username = 'admin'
ON CONFLICT (user_id, role_id) DO NOTHING;

-- ============================================================
-- 值班员账号（dwjk 物联网云平台 · 2026-08-25）
--   用户名 duty / 默认密码 admin123（⚠️ 仅开发环境）
--   角色：DUTY_OFFICER（值班监控只读；前端隐藏配置/管理菜单）
-- ============================================================
INSERT INTO users (tenant_id, username, display_name, email, password_hash, status)
SELECT t.id, 'duty', '值班员', 'duty@dwjk.local',
       '$2b$10$/zX3Q157zlkzZ3dcg41yO.oGKDLMdPL6ZGHQS8Bt9iJVlC1u/u4ee', 'ACTIVE'
FROM tenants t
WHERE t.code = 'dwjk'
ON CONFLICT (tenant_id, username) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN tenants t ON t.id = u.tenant_id AND t.code = 'dwjk'
JOIN roles r ON r.code = 'DUTY_OFFICER'
WHERE u.username = 'duty'
ON CONFLICT (user_id, role_id) DO NOTHING;
