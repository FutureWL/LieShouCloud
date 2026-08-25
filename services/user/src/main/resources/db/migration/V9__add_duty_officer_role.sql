-- ============================================================
-- V9 · 值班员角色（DUTY_OFFICER）
--
-- 值班员：日常值班只看平台监控（驾驶舱/总览/拓扑/告警中心只读），
-- 前端 access 模型（apps/admin/src/access.ts）按此角色隐藏配置类菜单
-- （设备管理/产品物模型/规则配置）与管理类菜单（租户/用户/CRM/进销存/财务/审批）。
-- 幂等：ON CONFLICT DO NOTHING。
-- ============================================================

INSERT INTO roles (code, name, scope, description, is_system) VALUES
  ('DUTY_OFFICER', '值班员', 'TENANT', '值班监控：只读驾驶舱/监控总览/拓扑/告警中心，无配置权限', true)
ON CONFLICT (code) DO NOTHING;
