package cn.iocoder.yudao.module.system.service.task;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.LargeImageQueryReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.LargeImageDO;
import cn.iocoder.yudao.module.system.dal.mysql.task.ArticleMapper;
import cn.iocoder.yudao.module.system.dal.mysql.task.LargeImageMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.List;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class LargeImageService {

  @Resource
  private LargeImageMapper largeImageMapper;

  public PageResult<LargeImageDO> pageResult(LargeImageQueryReqVO reqVO){
    return largeImageMapper.selectPage(reqVO);
  }

  public Integer deleteById(Long id){
    return largeImageMapper.deleteById(id);
  }

  public Integer deleteByIds(List<Long> ids){
    return largeImageMapper.deleteByIds(ids);
  }

  public Long querySumByArticleId(Long articleId) {
    QueryWrapper<LargeImageDO> wrapper = new QueryWrapper<>();
    wrapper.eq("article_id", articleId);
    wrapper.eq("status", 1);
    return largeImageMapper.selectCount(wrapper);
  }

}
