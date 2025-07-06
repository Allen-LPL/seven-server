package cn.iocoder.yudao.module.system.dal.mysql.task;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.task.ImageTaskQueryReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.ImageTaskDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ImageTaskMapper extends BaseMapperX<ImageTaskDO> {

  default PageResult<ImageTaskDO> selectPage(ImageTaskQueryReqVO reqVO) {

    return selectPage(reqVO, new LambdaQueryWrapperX<ImageTaskDO>()
        .eqIfPresent(ImageTaskDO::getTaskType, reqVO.getTaskType())
        .eqIfPresent(ImageTaskDO::getTaskId, reqVO.getTaskId())
        .eqIfPresent(ImageTaskDO::getCreatorId,reqVO.getCreatorId())
        .eqIfPresent(ImageTaskDO::getReviewerId,reqVO.getReviewId())
        .betweenIfPresent(ImageTaskDO::getCreateTime, reqVO.getStartTime(), reqVO.getEndTime())
        .orderByDesc(ImageTaskDO::getCreateTime,ImageTaskDO::getUpdateTime));
  }

}
