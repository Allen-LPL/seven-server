package cn.iocoder.yudao.module.system.service.task;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.similar.ImgSimilarityQueryReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.ImgSimilarityDO;
import cn.iocoder.yudao.module.system.dal.mysql.task.ImgSimilarityMapper;
import java.util.List;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class ImgSimilarityService {

  @Resource
  private ImgSimilarityMapper imgSimilarityMapper;

  public PageResult<ImgSimilarityDO> pageResult(ImgSimilarityQueryReqVO reqVO){
    return imgSimilarityMapper.selectPage(reqVO);
  }

  public ImgSimilarityDO getById(Long id) {
    return imgSimilarityMapper.selectById(id);
  }

  public List<ImgSimilarityDO> queryByTaskId(Long taskId) {
    return imgSimilarityMapper.selectList("task_id", taskId);
  }

  public Integer deleteById(Long id){
    return imgSimilarityMapper.deleteById(id);
  }

  public Integer deleteByIds(List<Long> ids){
    return imgSimilarityMapper.deleteByIds(ids);
  }

  public Integer insert(ImgSimilarityDO image) {
    return imgSimilarityMapper.insert(image);
  }

  public Boolean batchInsert(List<ImgSimilarityDO> list) {
    return imgSimilarityMapper.insertBatch(list);
  }

  public Integer updateById(ImgSimilarityDO image) {
    return imgSimilarityMapper.updateById(image);
  }

  public Boolean updateBatch (List<ImgSimilarityDO> image) {
    return imgSimilarityMapper.updateBatch(image);
  }

}
