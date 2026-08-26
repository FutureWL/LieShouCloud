package cn.huntercat.lieshoucloudpro.user.service.dto;

/** 用户域请求 DTO（自 UserController 内联 record 下沉，供 Controller / UserService / Local 适配器共用）. */
public final class UserDtos {

  private UserDtos() {}

  /** 创建用户（tenantCode 可选默认 huntercat；inviteCode 可选则租户/角色来自邀请码） */
  public record CreateUserRequest(
      @jakarta.validation.constraints.NotBlank String username,
      @jakarta.validation.constraints.NotBlank String displayName,
      @jakarta.validation.constraints.NotBlank String password,
      String email,
      String phone,
      String tenantCode,
      String inviteCode) {}

  /** 更新用户（字段均可选，传入才更新；password 传入才改） */
  public record UpdateUserRequest(
      String displayName,
      String email,
      String phone,
      String status,
      String[] roles,
      String password) {}
}
