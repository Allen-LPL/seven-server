package cn.iocoder.yudao.module.system.service.task;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.ImageTaskQueryReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.ImageTaskDO;
import cn.iocoder.yudao.module.system.dal.mysql.task.ArticleMapper;
import cn.iocoder.yudao.module.system.dal.mysql.task.ImageTaskMapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import java.util.Objects;
import javax.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
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

  public Integer update(ImageTaskDO imageTaskDO) {
    UpdateWrapper<ImageTaskDO> updateWrapper = new UpdateWrapper<>();
    if (StringUtils.isNotBlank(imageTaskDO.getFirstImage())){
      updateWrapper.set("first_image", imageTaskDO.getFirstImage());
    }
    if (Objects.nonNull(imageTaskDO.getAdminId())){
      updateWrapper.set("admin_id", imageTaskDO.getAdminId());
    }
    if (Objects.nonNull(imageTaskDO.getReviewerId())){
      updateWrapper.set("reviewer_id", imageTaskDO.getReviewerId());
    }
    if (Objects.nonNull(imageTaskDO.getAdminTime())){
      updateWrapper.set("admin_time", imageTaskDO.getAdminTime());
    }
    updateWrapper.eq("id", imageTaskDO.getId());
    return imageTaskMapper.update(updateWrapper);
  }

  public ImageTaskDO getById(Long id) {
    return imageTaskMapper.selectById(id);
  }

}
