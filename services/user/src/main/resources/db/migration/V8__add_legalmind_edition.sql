-- ============================================================
-- V8 · 租户版别扩展：LEGALMIND（LegalMind Unity · 智法云枢 · ADR-0036）
--
-- LegalMind Unity 为凌科安时联合定制版（layer 之上的专属版别，律师成长操作系统）：
--   - LEGALMIND：LegalMind Unity 版（凌科安时律所定制 · 联合品牌）
-- PostgreSQL 的 CHECK 约束不支持追加值 → 先 DROP 再按全量重建（含既有 4 版别）。
-- ============================================================

ALTER TABLE tenants DROP CONSTRAINT ck_tenants_edition;
ALTER TABLE tenants ADD CONSTRAINT ck_tenants_edition
    CHECK (edition IN ('GENERIC', 'LAYER', 'ZHIYE', 'JMZZ', 'LEGALMIND'));

COMMENT ON COLUMN tenants.edition IS
    '版别：GENERIC（通用）/ LAYER（法律）/ ZHIYE（教育）/ JMZZ（精密制造）/ LEGALMIND（LegalMind Unity · 凌科安时定制）；前端按此渲染门户/登录品牌（ADR-0035/0036）';

-- 已知版别租户回填（幂等：UPDATE 只影响匹配 code 的行；无匹配则跳过）
UPDATE tenants SET edition = 'LEGALMIND' WHERE code = 'legalmind';
