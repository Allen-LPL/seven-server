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
        .eqIfPresent(ImageTaskDO::getId, reqVO.getId())
        .eqIfPresent(ImageTaskDO::getTaskType, reqVO.getTaskType())
        .eqIfPresent(ImageTaskDO::getTaskId, reqVO.getTaskId())
        .eqIfPresent(ImageTaskDO::getIsCase, Boolean.TRUE.equals(reqVO.getCaseOnly()) ? 1 : null)
        .betweenIfPresent(ImageTaskDO::getCreateTime, reqVO.getStartTime(), reqVO.getEndTime())
        .and(reqVO.getCreatorId() != null && reqVO.getReviewId() != null,
            wrapper -> wrapper.eq(ImageTaskDO::getCreatorId, reqVO.getCreatorId())
                .or()
                .eq(ImageTaskDO::getReviewerId, reqVO.getReviewId()))
        .eq(reqVO.getCreatorId() != null && reqVO.getReviewId() == null,
            ImageTaskDO::getCreatorId, reqVO.getCreatorId())
        .eq(reqVO.getReviewId() != null && reqVO.getCreatorId() == null,
            ImageTaskDO::getReviewerId, reqVO.getReviewId())
        .orderByDesc(ImageTaskDO::getCreateTime,ImageTaskDO::getUpdateTime));
  }

}
