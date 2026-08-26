-- ============================================================
-- V11 · 租户菜单覆盖表（tenant_menu_configs · ADR-0024 Phase 2 阶段 4）
--
-- 平台基础层：菜单数据驱动（阶段 4 A+B 混合）。
--   默认菜单清单 = 平台代码内置（MenuService.DEFAULT_MENUS）
--   租户覆盖     = 本表（enabled 开关 / rename 改名 / sort 重排）
--   权限裁决     = role_permissions（阶段 1 已落地）
--   返回         = GET /api/users/me/menus（合并 + 过滤 + 排序后的菜单树）
-- 客户差异（隐藏某菜单/改名/排序）从此"配数据"而非"发代码"（ADR-0035 配置层承诺）。
-- ============================================================

CREATE TABLE tenant_menu_configs (
    tenant_id BIGINT NOT NULL,
    menu_key  VARCHAR(64)  NOT NULL,
    enabled   BOOLEAN      NOT NULL DEFAULT true,
    sort      INT          NOT NULL DEFAULT 0,
    rename    VARCHAR(64),
    updated_by BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (tenant_id, menu_key)
);

CREATE INDEX idx_tenant_menu_configs_tenant ON tenant_menu_configs (tenant_id);

COMMENT ON TABLE tenant_menu_configs IS '租户菜单覆盖（菜单数据驱动 · 客户差异进配置层）';
COMMENT ON COLUMN tenant_menu_configs.menu_key IS '菜单 key（对应平台默认菜单清单 MenuService.DEFAULT_MENUS）';
COMMENT ON COLUMN tenant_menu_configs.enabled IS '是否启用（false = 该租户隐藏此菜单）';
COMMENT ON COLUMN tenant_menu_configs.sort IS '排序（越小越靠前；0 = 跟随默认）';
COMMENT ON COLUMN tenant_menu_configs.rename IS '改名（null = 跟随默认名称）';
