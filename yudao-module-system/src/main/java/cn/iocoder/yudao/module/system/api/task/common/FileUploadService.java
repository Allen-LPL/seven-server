package cn.iocoder.yudao.module.system.api.task.common;

import cn.iocoder.yudao.module.system.api.task.dto.FileContent;
import cn.iocoder.yudao.module.system.api.task.dto.ImageTaskCreateResDTO;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class FileUploadService {

  public ImageTaskCreateResDTO uploadFiles(MultipartFile[] files, String taskFilePath){

    ImageTaskCreateResDTO imageTaskResDTO = new ImageTaskCreateResDTO();
    try {
      // 确保上传目录存在
      File uploadDir = new File(taskFilePath);
      if (!uploadDir.exists()) {
        uploadDir.mkdirs();
      }

      List<String> failedFile = new ArrayList<>();
      List<FileContent> successFile = new ArrayList<>();

      // 上传文件
      for (MultipartFile file : files) {
        String originalFilename = file.getOriginalFilename();
        FileContent fileContent = new FileContent();

        try {

          if (!isValidFileSize(file)) {
            failedFile.add(originalFilename + " (文件过大)");
            imageTaskResDTO.setSuccess(Boolean.FALSE);
            continue;
          }

          // 保存文件
          String fullLocalFilePath = taskFilePath + "/" + originalFilename;
          Path filePath = Paths.get(fullLocalFilePath);
          Files.write(filePath, file.getBytes());
          fileContent.setFileName(originalFilename);
          fileContent.setFilePath(fullLocalFilePath);
          fileContent.setFileSize(file.getSize());
          successFile.add(fileContent);
        } catch (IOException e) {
          failedFile.add(originalFilename + " (上传失败: " + e.getMessage() + ")");
          imageTaskResDTO.setSuccess(Boolean.FALSE);
        }
      }
      imageTaskResDTO.setFailedFile(failedFile);
      imageTaskResDTO.setSuccessFile(successFile);
    }catch (Exception e){
      log.error("uploadFiles error", e);
      imageTaskResDTO.setSuccess(Boolean.FALSE);
      imageTaskResDTO.setFailedMsg(e.getMessage());
    }
    return imageTaskResDTO;
  }

  private boolean isValidFileType(MultipartFile file) {
    String contentType = file.getContentType();
    if (contentType == null) {
      return false;
    }

    // 允许的MIME类型
    return contentType.startsWith("image/") ||
        contentType.equals("application/pdf") ||
        contentType.equals("application/octet-stream"); // 某些PDF可能返回此类型
  }

  private boolean isValidFileSize(MultipartFile file) {
    return file.getSize() <= 10 * 1024 * 1024; // 10MB限制
  }


}
