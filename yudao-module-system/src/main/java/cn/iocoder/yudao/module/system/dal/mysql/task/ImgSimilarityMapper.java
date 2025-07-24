package cn.iocoder.yudao.module.system.dal.mysql.task;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.similar.ImgSimilarityQueryReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.ImgSimilarityDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ImgSimilarityMapper extends BaseMapperX<ImgSimilarityDO> {

  default PageResult<ImgSimilarityDO> selectPage(ImgSimilarityQueryReqVO reqVO) {

    return selectPage(reqVO, new LambdaQueryWrapperX<ImgSimilarityDO>()
        .eqIfPresent(ImgSimilarityDO::getId, reqVO.getId())
        .eqIfPresent(ImgSimilarityDO::getTaskId,reqVO.getTaskId())
        .eqIfPresent(ImgSimilarityDO::getCreator, reqVO.getCreator())
        .ge(ImgSimilarityDO::getFeaturePointCnt, reqVO.getFeaturePoints())
        .orderByDesc(ImgSimilarityDO::getFeaturePointCnt,ImgSimilarityDO::getUpdateTime));
  }

}
