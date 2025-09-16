package cn.iocoder.yudao.module.system.service.task;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.task.ImageTaskQueryReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.ImageTaskDO;
import cn.iocoder.yudao.module.system.dal.mysql.task.ImageTaskMapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;

import java.time.LocalDateTime;
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
    if (StringUtils.isNotBlank(imageTaskDO.getTaskNo())){
      updateWrapper.set("task_no", imageTaskDO.getTaskNo());
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
    if (Objects.nonNull(imageTaskDO.getTaskStatus())){
      updateWrapper.set("task_status", imageTaskDO.getTaskStatus());
    }
    if (Objects.nonNull(imageTaskDO.getTotalImages())){
      updateWrapper.set("total_images", imageTaskDO.getTotalImages());
    }
    if (Objects.nonNull(imageTaskDO.getReviewResult())){
      updateWrapper.set("review_result", imageTaskDO.getReviewResult());
      updateWrapper.set("review_time", LocalDateTime.now());
    }
    if (Objects.nonNull(imageTaskDO.getIsCase())){
      updateWrapper.set("is_case", imageTaskDO.getIsCase());
    }
    updateWrapper.set("update_time", LocalDateTime.now());
    updateWrapper.eq("id", imageTaskDO.getId());
    return imageTaskMapper.update(updateWrapper);
  }

  public ImageTaskDO getById(Long id) {
    return imageTaskMapper.selectById(id);
  }

  // 删除专家用户的置空逻辑
  public Integer clearExpertUser(Long id) {
    ImageTaskDO imageTaskDO = imageTaskMapper.selectById(id);
    UpdateWrapper<ImageTaskDO> updateWrapper = new UpdateWrapper<>();
    if (imageTaskDO != null) {
      updateWrapper.set("reviewer_id", null);
      updateWrapper.set("admin_id", null);
      updateWrapper.set("admin_time", null);
      updateWrapper.set("update_time", LocalDateTime.now());
      updateWrapper.eq("id", id);
      return imageTaskMapper.update(updateWrapper);
    } else {
      return 0; // 如果任务不存在，直接返回0
    }
  }

  /**
   * 逻辑删除任务
   *
   * @param id 任务ID
   * @return 删除结果
   */
  public Integer deleteTask(Long id) {
    UpdateWrapper<ImageTaskDO> updateWrapper = new UpdateWrapper<>();
    updateWrapper.set("deleted", 1); // 逻辑删除标记
    updateWrapper.set("update_time", LocalDateTime.now());
    updateWrapper.eq("id", id);
    return imageTaskMapper.update(updateWrapper);
  }
}
