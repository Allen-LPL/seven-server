package cn.iocoder.yudao.module.system.dal.mysql.task;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.file.FileQueryReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.ArticleDO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ArticleMapper extends BaseMapperX<ArticleDO> {

  default PageResult<ArticleDO> selectPage(FileQueryReqVO reqVO) {

    return selectPage(reqVO, new LambdaQueryWrapperX<ArticleDO>()
        .eqIfPresent(ArticleDO::getId, reqVO.getId())
        .eqIfPresent(ArticleDO::getCreator, reqVO.getCreatorId())
        .likeIfPresent(ArticleDO::getArticleKeywords,reqVO.getArticleKeywords())
        .likeIfPresent(ArticleDO::getArticleJournal, reqVO.getArticleJournal())
        .betweenIfPresent(ArticleDO::getCreateTime, reqVO.getStartTime(), reqVO.getEndTime())
        .orderByDesc(ArticleDO::getCreateTime,ArticleDO::getUpdateTime));
  }

  default Long getMaxId(){
    QueryWrapper<ArticleDO> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("deleted",0);
    queryWrapper.orderByDesc("id");
    queryWrapper.last("limit 1");
    return selectOne(queryWrapper).getId();
  }

  default Long getMinId(){
    QueryWrapper<ArticleDO> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("deleted",0);
    queryWrapper.orderByAsc("id");
    queryWrapper.last("limit 1");
    return selectOne(queryWrapper).getId();
  }

}
