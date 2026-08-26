package cn.huntercat.lieshoucloudpro.approval.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cn.huntercat.lieshoucloudpro.approval.domain.ApprovalAuditLog;
import cn.huntercat.lieshoucloudpro.approval.domain.ApprovalRequest;
import cn.huntercat.lieshoucloudpro.approval.domain.ApprovalRequestRepository;
import cn.huntercat.lieshoucloudpro.approval.feign.UserView;
import cn.huntercat.lieshoucloudpro.approval.port.UserQueryPort;
import cn.huntercat.lieshoucloudpro.approval.web.ApprovalForbiddenException;
import cn.huntercat.lieshoucloudpro.approval.web.InvalidTypeException;
import java.math.BigDecimal;
import java.util.List;

/**
 * 审批请求领域服务（ARCHITECTURE.md §4.2 下沉 —— 发起逻辑自 {@code ApprovalController.create} 迁移至此）。
 *
 * <p>本服务是服务间调用的**进程内契约**：单体（monolith）模式下 finance/inventory 的 {@code ApprovalSubmitLocalAdapter}
 * 直接注入本服务发起审批，零网络开销；微服务（msa）模式下 由 {@code ApprovalController} 的 REST 端点承担同样职责。
 *
 * <p>注意：本类仅暴露「发起」路径；审批/驳回/撤销等状态机操作暂留 Controller（后续下沉）。
 */
@Service
public class ApprovalRequestService {

  private final ApprovalRequestRepository repo;
  private final ApprovalAuditService auditService;
  private final UserQueryPort userClient;
  private final ApprovalNotifier notifier;

  public ApprovalRequestService(
      ApprovalRequestRepository repo,
      ApprovalAuditService auditService,
      UserQueryPort userClient,
      ApprovalNotifier notifier) {
    this.repo = repo;
    this.auditService = auditService;
    this.userClient = userClient;
    this.notifier = notifier;
  }

  /**
   * 发起审批（PENDING）。
   *
   * <p>approverId 可空 —— 业务挂接自动触发时由租户管理员兜底（ADR-0032 阶段 2）。审计 + 通知在 保存后执行（审计 REQUIRES_NEW
   * 独立事务，业务回滚不吞审计）。
   *
   * @param tenantId 租户 ID（已由 web 层校验 X-Tenant-Id）
   * @param requesterId 发起人 ID（X-User-Id；null → 403）
   * @param type 审批类型字符串（EXPENSE / PURCHASE / SALE / OTHER）
   * @param title 标题（非空）
   * @param amount 金额（可空）
   * @param detail 详情（可空，空白 → null）
   * @param approverId 显式审批人（可空 → 自动解析租户管理员）
   * @param sourceIp 客户端 IP（审计用，可空）
   * @param userAgent UA（审计用，可空）
   * @param requestId 请求 ID（审计用，可空）
   * @return 已保存的审批单
   */
  @Transactional
  public ApprovalRequest createRequest(
      Long tenantId,
      Long requesterId,
      String type,
      String title,
      BigDecimal amount,
      String detail,
      Long approverId,
      String sourceIp,
      String userAgent,
      String requestId) {
    if (requesterId == null) {
      throw new ApprovalForbiddenException("X-User-Id 缺失，无法识别发起人");
    }
    Long resolvedApprover = resolveApprover(tenantId, approverId);
    if (resolvedApprover == null) {
      throw new ApproverResolveException("无法解析审批人（租户无可用用户）");
    }
    ApprovalRequest a =
        new ApprovalRequest(
            tenantId, parseTypeRequired(type), title.trim(), amount, requesterId, resolvedApprover);
    a.setDetail(blankToNull(detail));
    ApprovalRequest saved = repo.save(a);
    auditService.recordSuccess(
        tenantId,
        requesterId,
        ApprovalAuditLog.Action.CREATE,
        saved.getId(),
        "发起审批 " + saved.getTitle(),
        sourceIp,
        userAgent,
        requestId);
    notifier.notifyApprover(tenantId, saved); // 异步邮件，失败降级不阻塞
    return saved;
  }

  private ApprovalRequest.Type parseTypeRequired(String value) {
    ApprovalRequest.Type t = parseType(value);
    if (t == null) throw new InvalidTypeException(value);
    return t;
  }

  private ApprovalRequest.Type parseType(String value) {
    if (value == null || value.isBlank()) return null;
    try {
      return ApprovalRequest.Type.valueOf(value.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private String blankToNull(String value) {
    return (value == null || value.isBlank()) ? null : value.trim();
  }

  /** 审批人解析：显式指定优先；否则自动选租户管理员（业务挂接 · ADR-0032） */
  private Long resolveApprover(Long tenantId, Long requested) {
    if (requested != null) return requested;
    try {
      List<UserView> users = userClient.listTenantUsers(String.valueOf(tenantId));
      if (users == null || users.isEmpty()) return null;
      return users.stream()
          .filter(u -> u.roles() != null && u.roles().contains("TENANT_ADMIN"))
          .findFirst()
          .map(UserView::id)
          .orElseGet(
              () ->
                  users.stream()
                      .filter(u -> u.roles() != null && u.roles().contains("PLATFORM_ADMIN"))
                      .findFirst()
                      .map(UserView::id)
                      .orElseGet(() -> users.stream().findFirst().map(UserView::id).orElse(null)));
    } catch (Exception e) {
      return null; // user 服务不可用 → 降级为无法解析（调用方可不阻塞业务）
    }
  }
}
