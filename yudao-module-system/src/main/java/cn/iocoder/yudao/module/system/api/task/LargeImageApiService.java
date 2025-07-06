package cn.iocoder.yudao.module.system.api.task;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.image.LargeImageQueryReqVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.image.LargeImageQueryResVO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.LargeImageDO;
import cn.iocoder.yudao.module.system.service.task.LargeImageService;
import cn.iocoder.yudao.module.system.service.task.SmallImageService;
import java.util.List;
import java.util.Objects;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LargeImageApiService {

  @Resource
  private LargeImageService largeImageService;

  @Resource
  private SmallImageService smallImageService;

  public CommonResult<PageResult<LargeImageQueryResVO>> pageQuery(LargeImageQueryReqVO reqVO){
    PageResult<LargeImageDO> imageDOPageResult =  largeImageService.pageResult(reqVO);
    PageResult<LargeImageQueryResVO> pageResult = BeanUtils.toBean(imageDOPageResult,LargeImageQueryResVO.class);
    for(LargeImageQueryResVO imageDO : pageResult.getList()){
      Long sum = smallImageService.querySumByLargeImageId(imageDO.getId());
      imageDO.setSmallImageSum(sum);
    }
    return CommonResult.success(pageResult);
  }

  public CommonResult<Integer> deleteByIds(List<Long> ids){
    if (CollectionUtils.isAnyEmpty(ids)){
      return CommonResult.error(500, "请选择图片");
    }
    Integer sum = largeImageService.deleteByIds(ids);
    return CommonResult.success(sum);
  }

  public CommonResult<Integer> deleteById(Long id){
    if (Objects.isNull(id)){
      return CommonResult.error(500, "请选择图片");
    }
    Integer sum = largeImageService.deleteById(id);
    return CommonResult.success(sum);
  }

}
