package cn.iocoder.yudao.module.system.util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileWriterUtils {

  public static void copyFile(String srcPath, String destPath) {

    Path target = Paths.get(destPath);
    Path parentDir = target.getParent();
    if (!Files.exists(parentDir)) {
      System.out.println("正在创建目录: " + parentDir);
      try {
        Files.createDirectories(parentDir); // 创建所有不存在的父目录
      } catch (IOException e) {
        System.err.println("目录创建失败: " + e.getMessage());
      }
    }

    try (InputStream in = Files.newInputStream(Paths.get(srcPath));
        OutputStream out = Files.newOutputStream(target)) {

      byte[] buffer = new byte[1024];
      int length;
      while ((length = in.read(buffer)) > 0) {
        out.write(buffer, 0, length);
      }
      log.info("文件复制成功！");
    } catch (IOException e) {
      log.error("copyFile error : ",e);
    }
  }

}
