package cn.iocoder.yudao.module.system.api.task;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.FileQueryReqVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.FileQueryResVO;
import cn.iocoder.yudao.module.system.service.task.ArticleService;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ArticleApiService {

  @Resource
  private ArticleService articleService;

  public CommonResult<PageResult<FileQueryResVO>> pageQuery(FileQueryReqVO fileQueryReqVO) {
    PageResult<FileQueryResVO> result = new PageResult<>();

    return CommonResult.success(result);
  }
}
