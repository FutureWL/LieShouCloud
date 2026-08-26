package cn.huntercat.lieshoucloudpro.file.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** 文件元数据 Repository（租户内强制过滤 · ADR-0025）。 */
public interface FileRepository extends JpaRepository<FileEntity, Long> {

  /** 租户内未软删文件列表（按 id 倒序）。 */
  @Query(
      """
      select f from FileEntity f
      where f.tenantId = :tenantId and f.deleted = false
      order by f.id desc
      """)
  List<FileEntity> findTenantFiles(@Param("tenantId") Long tenantId);

  /** 租户内未软删文件详情（下载/元数据用）。 */
  Optional<FileEntity> findByIdAndTenantIdAndDeletedFalse(Long id, Long tenantId);

  long countByTenantIdAndDeletedFalse(Long tenantId);
}
