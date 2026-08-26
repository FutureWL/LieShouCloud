-- ============================================================
-- V9__add_group_company_fields.sql · 子公司档案字段（集团管理行业版 · group）
--
-- 集团版需求（docs/group-edition-design.md §3.1 子公司档案）：
--   集团总部统一管控子公司——工商档案 + 集团归属。
--   所有列可空（通用版 / 其他行业版不填无感）；新增列不改动既有数据。
-- ============================================================

ALTER TABLE tenants ADD COLUMN credit_code        VARCHAR(64);
ALTER TABLE tenants ADD COLUMN legal_person       VARCHAR(64);
ALTER TABLE tenants ADD COLUMN registered_capital NUMERIC(14, 2);
ALTER TABLE tenants ADD COLUMN established_at     DATE;
ALTER TABLE tenants ADD COLUMN industry           VARCHAR(64);
ALTER TABLE tenants ADD COLUMN parent_tenant_id   BIGINT REFERENCES tenants(id);

COMMENT ON COLUMN tenants.credit_code        IS '统一社会信用代码（工商档案 · 集团版）';
COMMENT ON COLUMN tenants.legal_person       IS '法定代表人（工商档案 · 集团版）';
COMMENT ON COLUMN tenants.registered_capital IS '注册资本（万元 · 集团版）';
COMMENT ON COLUMN tenants.established_at     IS '成立日期（集团版）';
COMMENT ON COLUMN tenants.industry           IS '所属行业（集团版）';
COMMENT ON COLUMN tenants.parent_tenant_id   IS '集团归属（上级租户；集团总部=null，子公司=总部租户 id）';
