package cn.huntercat.lieshoucloudpro.approval.feign;

import java.util.List;

/** user-service 用户视图（Feign DTO，只取审批需要的字段）. */
public record UserView(
    Long id,
    String username,
    String displayName,
    String email,
    String status,
    List<String> roles) {}
