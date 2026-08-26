package cn.huntercat.lieshoucloudpro.file.web;

/** 文件存储读写异常（磁盘 IO 失败）→ 500 FILE_STORAGE。 */
public class FileStorageException extends RuntimeException {

  public FileStorageException(String message) {
    super(message);
  }

  public FileStorageException(String message, Throwable cause) {
    super(message, cause);
  }
}
