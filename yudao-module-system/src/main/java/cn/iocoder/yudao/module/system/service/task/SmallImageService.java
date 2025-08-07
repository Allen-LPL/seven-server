package cn.iocoder.yudao.module.system.service.task;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.image.SmallImageQueryReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.LargeImageDO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.SmallImageDO;
import cn.iocoder.yudao.module.system.dal.mysql.task.SmallImageMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import java.util.List;
import java.util.Set;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class SmallImageService {

  @Resource
  private SmallImageMapper smallImageMapper;

  public PageResult<SmallImageDO> pageQuery(SmallImageQueryReqVO reqVO){
    return smallImageMapper.selectPage(reqVO);
  }

  public Integer deleteById(Long id){
    return smallImageMapper.deleteById(id);
  }

  public Integer deleteByIds(List<Long> ids){
    return smallImageMapper.deleteByIds(ids);
  }

  public Integer deleteByArticleId(Long articleId){
    return smallImageMapper.delete("article_id",String.valueOf(articleId));
  }

  public List<SmallImageDO> queryByIds(Set<Long> ids){
    return smallImageMapper.selectByIds(ids);
  }

  public SmallImageDO queryById(Long id){
    return smallImageMapper.selectById(id);
  }

  public Long querySumByLargeImageIds(List<Long> largeImageIds) {
    QueryWrapper<SmallImageDO> wrapper = new QueryWrapper<>();
    wrapper.in("large_image_id", largeImageIds);
    wrapper.eq("status", 1);
    return smallImageMapper.selectCount(wrapper);
  }

  public Long querySumByArticleId(Long articleId) {
    QueryWrapper<SmallImageDO> wrapper = new QueryWrapper<>();
    wrapper.eq("article_id", articleId);
    wrapper.eq("status", 1);
    return smallImageMapper.selectCount(wrapper);
  }

  public Long querySumByLargeImageId(Long largeImageId) {
    QueryWrapper<SmallImageDO> wrapper = new QueryWrapper<>();
    wrapper.eq("large_image_id", largeImageId);
    wrapper.eq("status", 1);
    return smallImageMapper.selectCount(wrapper);
  }

  public Boolean batchSave(List<SmallImageDO> smallImages) {
    return smallImageMapper.insertBatch(smallImages);
  }

  public Integer updateById(SmallImageDO smallImage) {
    return smallImageMapper.updateById(smallImage);
  }

  public List<SmallImageDO> queryByArticleId(Long articleId){
    return smallImageMapper.selectList(new QueryWrapper<SmallImageDO>()
        .eq("article_id", articleId)
        .eq("status", 1));
  }

  public Boolean updateBatch(List<SmallImageDO> smallImageDOList) {
    return smallImageMapper.updateBatch(smallImageDOList);
  }

}
