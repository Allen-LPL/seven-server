package cn.iocoder.yudao.module.system.api.task.dto;

import java.util.List;
import lombok.Data;

@Data
public class ProcessImageResponse {

  private Long articleId;

  private List<LargeImage> largeImageList;


  @Data
  public static class LargeImage{
    private String page_number;
    private String origin_name;
    private String caption;
    private String path;
    private List<SmallImage> smallImageList;

    @Data
    public static class SmallImage{
      private String path;
    }
  }

}
