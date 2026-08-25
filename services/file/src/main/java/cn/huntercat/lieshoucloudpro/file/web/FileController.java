package cn.huntercat.lieshoucloudpro.file.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import cn.huntercat.lieshoucloudpro.file.domain.FileEntity;
import cn.huntercat.lieshoucloudpro.file.domain.FileRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

/**
 * 文件服务 REST 端点（上传/下载/预览/回收站 · 强制 X-Tenant-Id · ADR-0025）.
 *
 * <p>完整路径含上下文：{@code /api/files/**}（由 gateway 转发）。存储：{@code file.store.dir}/{tenantId}/{uuid}。
 */
@RestController
@RequestMapping("/api/files")
@Tag(name = "File", description = "文件服务（上传/下载/预览 · 租户内强制过滤 · ADR-0025）")
public class FileController {

  private static final String HDR_TENANT_ID = "X-Tenant-Id";
  private static final String HDR_USER_ID = "X-User-Id";

  private final FileRepository repo;
  private final Path storeDir;

  public FileController(FileRepository repo, @Value("${file.store.dir:/data/files}") String storeDir) {
    this.repo = repo;
    this.storeDir = Paths.get(storeDir);
  }

  @Operation(
      summary = "Upload file (multipart)",
      description =
          "multipart/form-data 字段名 file；必须带 X-Tenant-Id。返回文件元数据（含 url = /api/files/{id}/content）。")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Uploaded file metadata"),
    @ApiResponse(responseCode = "401", description = "TENANT_CONTEXT_REQUIRED"),
    @ApiResponse(responseCode = "413", description = "FILE_TOO_LARGE")
  })
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<FileEntity> upload(
      @RequestParam("file") MultipartFile file,
      @RequestHeader(value = HDR_TENANT_ID, required = false) String tenantHeader,
      @RequestHeader(value = HDR_USER_ID, required = false) String userIdHeader) {
    Long tid = requireTenant(tenantHeader);
    if (file.isEmpty()) {
      throw new FileStorageException("上传文件为空");
    }
    String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
    String stored = UUID.randomUUID().toString().replace("-", "");
    Path tenantDir = storeDir.resolve(String.valueOf(tid));
    try {
      Files.createDirectories(tenantDir);
      file.transferTo(tenantDir.resolve(stored).toAbsolutePath());
    } catch (Exception e) {
      throw new FileStorageException("文件写入失败: " + e.getMessage(), e);
    }
    FileEntity f =
        new FileEntity(tid, original, stored, file.getContentType(), file.getSize());
    f.setCreatedBy(parseUserId(userIdHeader));
    f.setUpdatedBy(parseUserId(userIdHeader));
    return ResponseEntity.ok(repo.save(f));
  }

  @Operation(
      summary = "Download / preview file content",
      description = "流式返回文件字节；跨租户或已软删 → 404。")
  @GetMapping("/{id}/content")
  public ResponseEntity<Resource> content(@PathVariable Long id,
      @RequestHeader(value = HDR_TENANT_ID, required = false) String tenantHeader) {
    Long tid = requireTenant(tenantHeader);
    FileEntity f = findTenantFile(id, tid);
    Path path = storeDir.resolve(String.valueOf(f.getTenantId())).resolve(f.getStoredName());
    Resource resource;
    try {
      resource = new UrlResource(path.toUri());
      if (!resource.exists() || !resource.isReadable()) {
        return ResponseEntity.notFound().build();
      }
    } catch (Exception e) {
      throw new FileStorageException("文件读取失败: " + e.getMessage(), e);
    }
    String contentType = f.getContentType() != null ? f.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;
    String encoded = URLEncoder.encode(f.getOriginalName(), StandardCharsets.UTF_8).replace("+", "%20");
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(contentType))
        .header(HttpHeaders.CONTENT_DISPOSITION,
            "inline; filename*=UTF-8''" + encoded)
        .body(resource);
  }

  @Operation(summary = "Get file metadata (tenant-scoped)")
  @GetMapping("/{id}")
  public ResponseEntity<FileEntity> metadata(@PathVariable Long id,
      @RequestHeader(value = HDR_TENANT_ID, required = false) String tenantHeader) {
    Long tid = requireTenant(tenantHeader);
    return ResponseEntity.ok(findTenantFile(id, tid));
  }

  @Operation(summary = "List tenant files")
  @GetMapping
  public ResponseEntity<?> list(
      @RequestHeader(value = HDR_TENANT_ID, required = false) String tenantHeader) {
    Long tid = requireTenant(tenantHeader);
    return ResponseEntity.ok(repo.findTenantFiles(tid));
  }

  @Operation(summary = "Delete file (soft, recycle bin semantics)")
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id,
      @RequestHeader(value = HDR_TENANT_ID, required = false) String tenantHeader) {
    Long tid = requireTenant(tenantHeader);
    FileEntity f = findTenantFile(id, tid);
    f.setDeleted(true);
    repo.save(f);
    return ResponseEntity.noContent().build();
  }

  private FileEntity findTenantFile(Long id, Long tid) {
    return repo.findByIdAndTenantIdAndDeletedFalse(id, tid)
        .orElseThrow(() -> new FileNotFoundException(id));
  }

  private Long requireTenant(String header) {
    if (header == null || header.isBlank()) {
      throw new TenantContextRequiredException("缺少租户上下文（X-Tenant-Id header）");
    }
    try {
      return Long.parseLong(header.trim());
    } catch (NumberFormatException e) {
      throw new TenantContextRequiredException("租户上下文非法（X-Tenant-Id 必须为数字）");
    }
  }

  private Long parseUserId(String header) {
    if (header == null || header.isBlank()) return null;
    try {
      return Long.parseLong(header.trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
