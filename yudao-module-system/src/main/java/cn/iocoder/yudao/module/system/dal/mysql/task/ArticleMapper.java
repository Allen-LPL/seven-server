package cn.iocoder.yudao.module.system.dal.mysql.task;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.file.FileQueryReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.ArticleDO;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ArticleMapper extends BaseMapperX<ArticleDO> {

  default PageResult<ArticleDO> selectPage(FileQueryReqVO reqVO) {

    return selectPage(reqVO, new LambdaQueryWrapperX<ArticleDO>()
        .eqIfPresent(ArticleDO::getId, reqVO.getId())
        .eqIfPresent(ArticleDO::getCreator, reqVO.getCreatorId())
        .likeIfPresent(ArticleDO::getArticleKeywords,reqVO.getArticleKeywords())
        .likeIfPresent(ArticleDO::getArticleJournal, reqVO.getArticleJournal())
        .betweenIfPresent(ArticleDO::getCreateTime, reqVO.getStartTime(), reqVO.getEndTime())
        .eqIfPresent(ArticleDO::getIsSource, reqVO.getIsSource())
        .eqIfPresent(ArticleDO::getIsImage,reqVO.getIsImage())
        .likeIfPresent(ArticleDO::getFileName,reqVO.getFileName())
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

  default Long getSourceMaxId(){
    QueryWrapper<ArticleDO> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("deleted",0);
    queryWrapper.eq("is_source",1);
    queryWrapper.orderByDesc("id");
    queryWrapper.last("limit 1");
    return selectOne(queryWrapper).getId();
  }

  default Long getSourceMinId(){
    QueryWrapper<ArticleDO> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("deleted",0);
    queryWrapper.eq("is_source",1);
    queryWrapper.orderByAsc("id");
    queryWrapper.last("limit 1");
    return selectOne(queryWrapper).getId();
  }


    /**
     * 获取所有单位分布数据
     *
     * @return 单位名称-数量映射（institution: 单位名称, count: 数量）
     */
    @Select("<script>"
            + "SELECT jt.institution as institution "
            + "FROM iisd_article a "
            + "LEFT JOIN JSON_TABLE(COALESCE(a.author_institution, JSON_ARRAY()), '$[*]' COLUMNS (institution VARCHAR(512) PATH '$')) jt ON TRUE "
            + "WHERE a.deleted = 0 AND jt.institution IS NOT NULL AND jt.institution != '' "
            + "GROUP BY jt.institution "
            + "</script>")
    @InterceptorIgnore(tenantLine = "true")
    List<String> getAllUnitDistributionList();
}
