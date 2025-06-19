package cn.iocoder.yudao.module.system.service.task;

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

}
