package cn.iocoder.yudao.module.system.dal.mysql.task;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.image.SmallImageQueryReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.SmallImageDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SmallImageMapper extends BaseMapperX<SmallImageDO> {

  default PageResult<SmallImageDO> selectPage(SmallImageQueryReqVO reqVO) {

    return selectPage(reqVO, new LambdaQueryWrapperX<SmallImageDO>()
        .betweenIfPresent(SmallImageDO::getCreateTime, reqVO.getStartTime(), reqVO.getEndTime())
        .orderByDesc(SmallImageDO::getCreateTime,SmallImageDO::getUpdateTime));
  }

}
