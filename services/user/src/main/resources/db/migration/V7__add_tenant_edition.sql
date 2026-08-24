-- ============================================================
-- V7 · 租户版别：tenants.edition（ADR-0035 客户项目模型 · 行业版）
--
-- 版别（Edition）= 行业版 / 客户版标识，驱动前端门户/登录的品牌与功能开关。
--   - GENERIC：通用版（猎手云 Pro 默认，如 huntercat）
--   - LAYER  ：法律行业版（律所/事务所）
--   - ZHIYE  ：教育行业版（教育机构）
--   - JMZZ   ：精密制造版（制造企业）
-- 版别是「部署配置层」的锚点：同一套前端代码按 VITE_EDITION / 登录返回的
-- edition 渲染对应门户/登录页（ADR-0035：客户差异进配置层，禁止 fork 代码）。
-- ============================================================

ALTER TABLE tenants ADD COLUMN edition VARCHAR(32) NOT NULL DEFAULT 'GENERIC';
ALTER TABLE tenants ADD CONSTRAINT ck_tenants_edition
    CHECK (edition IN ('GENERIC', 'LAYER', 'ZHIYE', 'JMZZ'));

COMMENT ON COLUMN tenants.edition IS
    '版别：GENERIC（通用）/ LAYER（法律）/ ZHIYE（教育）/ JMZZ（精密制造）；前端按此渲染门户/登录品牌（ADR-0035）';

-- 已知行业版租户回填（幂等：UPDATE 只影响匹配 code 的行；无匹配则跳过）
UPDATE tenants SET edition = 'LAYER' WHERE code = 'layer';
UPDATE tenants SET edition = 'ZHIYE' WHERE code = 'zhiye';
UPDATE tenants SET edition = 'JMZZ'  WHERE code = 'jmzz';
