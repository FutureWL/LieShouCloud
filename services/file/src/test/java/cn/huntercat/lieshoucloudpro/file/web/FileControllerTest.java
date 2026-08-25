package cn.huntercat.lieshoucloudpro.file.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cn.huntercat.lieshoucloudpro.file.PostgresTestSupport;
import cn.huntercat.lieshoucloudpro.file.domain.FileEntity;
import cn.huntercat.lieshoucloudpro.file.domain.FileRepository;
import java.util.Optional;

/**
 * FileController 集成测试（{@code @SpringBootTest} + MockMvc + Mock repo）.
 *
 * <p>重点覆盖 ADR-0025 安全关键行为：
 *
 * <ul>
 *   <li>强制租户上下文：缺失 / 非法 X-Tenant-Id → 401
 *   <li>跨租户 / 已软删：get / content / delete → 404
 *   <li>上传：元数据正确 + created_by 取 X-User-Id
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("FileController（全上下文 + MockMvc · 强制租户过滤）")
class FileControllerTest extends PostgresTestSupport {

  private static final String TID = "X-Tenant-Id";
  private static final String UID = "X-User-Id";

  @Autowired private MockMvc mockMvc;

  @MockitoBean private FileRepository repo;

  private FileEntity aFile(Long id, Long tenantId, String stored) {
    FileEntity f = new FileEntity(tenantId, "委托合同.pdf", stored, "application/pdf", 1024);
    f.setId(id);
    return f;
  }

  @Test
  @DisplayName("无 X-Tenant-Id → 401（upload / list / get / content / delete）")
  void tenantRequired_allEndpoints() throws Exception {
    MockMultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", "hello".getBytes());
    mockMvc.perform(multipart("/api/files").file(file)).andExpect(status().isUnauthorized());
    mockMvc.perform(get("/api/files")).andExpect(status().isUnauthorized());
    mockMvc.perform(get("/api/files/1")).andExpect(status().isUnauthorized());
    mockMvc.perform(get("/api/files/1/content")).andExpect(status().isUnauthorized());
    mockMvc.perform(delete("/api/files/1")).andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("X-Tenant-Id 非数字 → 401")
  void invalidTenant_returns401() throws Exception {
    mockMvc.perform(get("/api/files").header(TID, "abc")).andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("upload：元数据正确 + created_by 取 X-User-Id")
  void upload_returnsMetadata() throws Exception {
    when(repo.save(any(FileEntity.class)))
        .thenAnswer(inv -> {
          FileEntity f = inv.getArgument(0);
          f.setId(9L);
          return f;
        });
    MockMultipartFile file =
        new MockMultipartFile("file", "委托合同.pdf", "application/pdf", "pdf-bytes".getBytes());
    mockMvc.perform(multipart("/api/files").file(file).header(TID, "1").header(UID, "42"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(9))
        .andExpect(jsonPath("$.tenantId").value(1))
        .andExpect(jsonPath("$.originalName").value("委托合同.pdf"))
        .andExpect(jsonPath("$.contentType").value("application/pdf"))
        .andExpect(jsonPath("$.createdBy").value(42))
        .andExpect(jsonPath("$.size").value(9)); // "pdf-bytes" 9 字节
  }

  @Test
  @DisplayName("get / content：跨租户或已软删 → 404")
  void crossTenant_returns404() throws Exception {
    when(repo.findByIdAndTenantIdAndDeletedFalse(1L, 1L)).thenReturn(Optional.empty());
    mockMvc.perform(get("/api/files/1").header(TID, "1")).andExpect(status().isNotFound());
    mockMvc.perform(get("/api/files/1/content").header(TID, "1")).andExpect(status().isNotFound());
    mockMvc.perform(delete("/api/files/1").header(TID, "1")).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("content：租户内命中 → 200 + Content-Disposition inline")
  void content_streamsFile() throws Exception {
    when(repo.findByIdAndTenantIdAndDeletedFalse(1L, 1L))
        .thenReturn(Optional.of(aFile(1L, 1L, "abc123")));
    // 磁盘真实写入测试文件（storeDir = target/test-files，见 PostgresTestSupport）
    java.nio.file.Path dir = java.nio.file.Paths.get("target/test-files/1");
    java.nio.file.Files.createDirectories(dir);
    java.nio.file.Files.write(dir.resolve("abc123"), "pdf-bytes".getBytes());
    mockMvc.perform(get("/api/files/1/content").header(TID, "1"))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Disposition", "inline; filename*=UTF-8''%E5%A7%94%E6%89%98%E5%90%88%E5%90%8C.pdf"));
  }

  @Test
  @DisplayName("delete：租户内命中 → 204 且软删")
  void delete_softDeletes() throws Exception {
    FileEntity f = aFile(1L, 1L, "abc123");
    when(repo.findByIdAndTenantIdAndDeletedFalse(1L, 1L)).thenReturn(Optional.of(f));
    when(repo.save(any(FileEntity.class))).thenAnswer(inv -> inv.getArgument(0));
    mockMvc.perform(delete("/api/files/1").header(TID, "1")).andExpect(status().isNoContent());
    verify(repo).save(any(FileEntity.class));
  }
}
