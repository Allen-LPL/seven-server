package cn.iocoder.yudao.module.system.dal.mysql.task;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.report.ImgReportQueryReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.ImgReportDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ImgReportMapper extends BaseMapperX<ImgReportDO> {

  default PageResult<ImgReportDO> selectPage(ImgReportQueryReqVO reqVO) {

    return selectPage(reqVO, new LambdaQueryWrapperX<ImgReportDO>()
        .eqIfPresent(ImgReportDO::getId, reqVO.getId())
        .eqIfPresent(ImgReportDO::getCreator, reqVO.getCreator())
        .eqIfPresent(ImgReportDO::getUpdater, reqVO.getUpdater())
        .betweenIfPresent(ImgReportDO::getCreateTime, reqVO.getStartTime(), reqVO.getEndTime())
        .orderByDesc(ImgReportDO::getCreateTime,ImgReportDO::getUpdateTime));
  }

}
