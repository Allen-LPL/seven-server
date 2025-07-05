package cn.iocoder.yudao.module.system.api.task.dto;


import cn.iocoder.yudao.module.system.dal.dataobject.task.SmallImageDO;
import java.util.List;
import lombok.Data;

@Data
public class SmallImageMilvusDTO extends SmallImageDO {

  private List<String> keywords;

  private List<String> author;

  private List<String> institution;

  private Long articleDate;

  private String specialty;

  private List<Float> resnet50Vectors;
}
