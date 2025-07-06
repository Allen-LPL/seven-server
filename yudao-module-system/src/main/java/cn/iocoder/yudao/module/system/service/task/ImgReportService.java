package cn.iocoder.yudao.module.system.service.task;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.report.ImgReportQueryReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.ImgReportDO;
import cn.iocoder.yudao.module.system.dal.mysql.task.ImgReportMapper;
import java.util.List;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class ImgReportService {

  @Resource
  private ImgReportMapper imgReportMapper;

  public PageResult<ImgReportDO> pageResult(ImgReportQueryReqVO reqVO){
    return imgReportMapper.selectPage(reqVO);
  }

  public Integer deleteById(Long id){
    return imgReportMapper.deleteById(id);
  }

  public Integer deleteByIds(List<Long> ids){
    return imgReportMapper.deleteByIds(ids);
  }

  public Integer insert(ImgReportDO image) {
    return imgReportMapper.insert(image);
  }

}
