package cn.iocoder.yudao.module.system.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class TaskConfig {

  @Value("${image.replace.prefix}")
  private String replacePrefix;

  @Value("${image.process.url}")
  private String processImageUrl;

  @Value("${image.compare.url}")
  private String compareImageUrl;

  @Value("${image.classify.url}")
  private String classifyImageUrl;

  @Value("${image.feature.url}")
  private String featureImageUrl;

}
