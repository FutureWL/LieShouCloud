package cn.huntercat.lieshoucloudpro.gateway.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PermissionRouteFilter 路径→权限码映射单测（ADR-0024 Phase 2 阶段 3）.
 *
 * <p>覆盖：域级映射、iot 配置类细粒度、最长前缀匹配、未映射路径放行。
 */
class PermissionRouteFilterTest {

  @Test
  @DisplayName("业务域路径 → 对应权限码（legal/crm/inventory/finance/approval）")
  void businessDomains() {
    assertThat(PermissionRouteFilter.requiredPermission("/api/legal/cases")).isEqualTo("legal:use");
    assertThat(PermissionRouteFilter.requiredPermission("/api/legal/cases/1/gates")).isEqualTo("legal:use");
    assertThat(PermissionRouteFilter.requiredPermission("/api/crm/customers")).isEqualTo("crm:use");
    assertThat(PermissionRouteFilter.requiredPermission("/api/inventory/products")).isEqualTo("inventory:use");
    assertThat(PermissionRouteFilter.requiredPermission("/api/finance/ledger")).isEqualTo("finance:use");
    assertThat(PermissionRouteFilter.requiredPermission("/api/approval/requests")).isEqualTo("approval:use");
  }

  @Test
  @DisplayName("平台管理路径 → tenant:manage（tenants/roles/audit）")
  void platformAdmin() {
    assertThat(PermissionRouteFilter.requiredPermission("/api/tenants")).isEqualTo("tenant:manage");
    assertThat(PermissionRouteFilter.requiredPermission("/api/tenants/1/invites")).isEqualTo("tenant:manage");
    assertThat(PermissionRouteFilter.requiredPermission("/api/roles")).isEqualTo("tenant:manage");
    assertThat(PermissionRouteFilter.requiredPermission("/api/audit/logs")).isEqualTo("tenant:manage");
  }

  @Test
  @DisplayName("iot 细粒度：配置类子路径 → iot:config，监控 → iot:monitor（最长前缀优先）")
  void iotGranular() {
    assertThat(PermissionRouteFilter.requiredPermission("/api/iot/devices")).isEqualTo("iot:config");
    assertThat(PermissionRouteFilter.requiredPermission("/api/iot/products")).isEqualTo("iot:config");
    assertThat(PermissionRouteFilter.requiredPermission("/api/iot/rules/1")).isEqualTo("iot:config");
    assertThat(PermissionRouteFilter.requiredPermission("/api/iot/cockpit")).isEqualTo("iot:monitor");
    assertThat(PermissionRouteFilter.requiredPermission("/api/iot/alerts")).isEqualTo("iot:monitor");
  }

  @Test
  @DisplayName("未映射路径 → null（放行，不误拦）")
  void unmapped_null() {
    assertThat(PermissionRouteFilter.requiredPermission("/api/users/1")).isNull();
    assertThat(PermissionRouteFilter.requiredPermission("/api/auth/me")).isNull();
    assertThat(PermissionRouteFilter.requiredPermission("/actuator/health")).isNull();
    assertThat(PermissionRouteFilter.requiredPermission("/unknown/foo")).isNull();
  }
}
