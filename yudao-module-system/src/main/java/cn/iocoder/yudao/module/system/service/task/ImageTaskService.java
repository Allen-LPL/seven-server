package cn.iocoder.yudao.module.system.service.task;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.ImageTaskQueryReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.ImageTaskDO;
import cn.iocoder.yudao.module.system.dal.mysql.task.ArticleMapper;
import cn.iocoder.yudao.module.system.dal.mysql.task.ImageTaskMapper;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class ImageTaskService {

  @Resource
  private ImageTaskMapper imageTaskMapper;

  public Integer create(ImageTaskDO imageTaskDO) {
    return imageTaskMapper.insert(imageTaskDO);
  }

  public PageResult<ImageTaskDO> pageQuery(ImageTaskQueryReqVO reqVO){
    return imageTaskMapper.selectPage(reqVO);
  }

}
