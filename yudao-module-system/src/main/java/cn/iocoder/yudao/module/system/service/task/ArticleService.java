package cn.iocoder.yudao.module.system.service.task;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.file.FileQueryReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.ArticleDO;
import cn.iocoder.yudao.module.system.dal.mysql.task.ArticleMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.List;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class ArticleService {

  @Resource
  private ArticleMapper articleMapper;

  public Integer create(ArticleDO articleDO){
    return articleMapper.insert(articleDO);
  }

  public Boolean batchCreate(List<ArticleDO> articleDOList){
    return articleMapper.insertBatch(articleDOList);
  }

  public List<ArticleDO> queryListByTaskId(Long taskId){
    QueryWrapper<ArticleDO> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("task_id", taskId);
    queryWrapper.eq("deleted", 0 );
    return articleMapper.selectList(queryWrapper);
  }

  public PageResult<ArticleDO> queryPage(FileQueryReqVO reqVO){
    return articleMapper.selectPage(reqVO);
  }

  public Integer update(ArticleDO articleDO){
    if (articleDO.getId() == null) {
      throw new RuntimeException("更新文章信息时，文章ID不能为空");
    }
    
    // 现在ArticleDO有了正确的@TableId注解，可以直接使用updateById
    return articleMapper.updateById(articleDO);
  }

  public Boolean updateBatch(List<ArticleDO> articleDOList){
    if (CollectionUtils.isAnyEmpty(articleDOList)) {
      throw new RuntimeException("更新文章信息时，文章ID不能为空");
    }

    // 现在ArticleDO有了正确的@TableId注解，可以直接使用updateById
    return articleMapper.updateBatch(articleDOList);
  }

  public Integer batchDelete(List<Long> ids){
    return articleMapper.deleteByIds(ids);
  }

  public Integer deleteById(Long id){
    return articleMapper.deleteById(id);
  }

  public List<ArticleDO> batchQueryByIds(List<Long> ids){
    return articleMapper.selectByIds(ids);
  }

}
