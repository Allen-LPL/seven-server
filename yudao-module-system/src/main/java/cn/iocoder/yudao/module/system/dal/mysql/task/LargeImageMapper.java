package cn.iocoder.yudao.module.system.dal.mysql.task;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.image.LargeImageQueryReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.LargeImageDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LargeImageMapper extends BaseMapperX<LargeImageDO> {

  default PageResult<LargeImageDO> selectPage(LargeImageQueryReqVO reqVO) {

    return selectPage(reqVO, new LambdaQueryWrapperX<LargeImageDO>()
        .betweenIfPresent(LargeImageDO::getCreateTime, reqVO.getStartTime(), reqVO.getEndTime())
        .orderByDesc(LargeImageDO::getCreateTime,LargeImageDO::getUpdateTime));
  }

}
