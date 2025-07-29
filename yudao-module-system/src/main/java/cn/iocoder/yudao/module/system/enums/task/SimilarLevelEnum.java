package cn.iocoder.yudao.module.system.enums.task;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SimilarLevelEnum {

  light(3,">0个", "green",0),

  middle(2,">=2个", "yellow",2),

  weight(1,">=5个", "red",5);

  private final Integer level;
  private final String desc;
  private final String color;
  private final Integer threshold;

}
