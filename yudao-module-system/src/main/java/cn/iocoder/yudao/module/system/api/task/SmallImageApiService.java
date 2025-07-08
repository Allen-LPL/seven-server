package cn.iocoder.yudao.module.system.api.task;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.image.SmallImageQueryReqVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.image.SmallImageQueryResVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.image.SmallImageUpdateReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.SmallImageDO;
import cn.iocoder.yudao.module.system.service.task.SmallImageService;
import java.util.List;
import java.util.Objects;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SmallImageApiService {

  @Resource
  private SmallImageService smallImageService;

  public CommonResult<PageResult<SmallImageQueryResVO>> pageQuery(SmallImageQueryReqVO reqVO){
    PageResult<SmallImageDO> imageDOPageResult =  smallImageService.pageQuery(reqVO);
    PageResult<SmallImageQueryResVO> pageResult = BeanUtils.toBean(imageDOPageResult,SmallImageQueryResVO.class);
    return CommonResult.success(pageResult);
  }

  public CommonResult<Integer> deleteByIds(List<Long> ids){
    if (CollectionUtils.isAnyEmpty(ids)){
      return CommonResult.error(500, "请选择图片");
    }
    Integer sum = smallImageService.deleteByIds(ids);
    return CommonResult.success(sum);
  }

  public CommonResult<Integer> deleteById(Long id){
    if (Objects.isNull(id)){
      return CommonResult.error(500, "请选择图片");
    }
    Integer sum = smallImageService.deleteById(id);
    return CommonResult.success(sum);
  }

  public CommonResult<String> updateById(SmallImageUpdateReqVO reqVO){
    if (reqVO.getId() == null){
      return CommonResult.error(500, "id 不能为空");
    }
    try {
      SmallImageDO smallImageDO = new SmallImageDO();
      BeanUtils.copyProperties(reqVO,smallImageDO);
      smallImageService.updateById(smallImageDO);
      return CommonResult.success(smallImageDO.getId().toString());
    }catch (Exception e){
      log.error("[update-small-image-error]", e);
      return CommonResult.error(500, e.getMessage());
    }
  }

}
