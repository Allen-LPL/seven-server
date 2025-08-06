package cn.iocoder.yudao.module.system.service.task;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.system.api.task.common.FileUploadService;
import cn.iocoder.yudao.module.system.api.task.dto.ImageTaskCreateResDTO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.report.ReportGenerateReqVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.report.ReportPageReqVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.report.ReportPageRespVO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.ArticleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.ImageTaskDO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.ImgReportDO;
import cn.iocoder.yudao.module.system.dal.mysql.task.ImgReportMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.io.File;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ReportService {

  @Resource
  private ImgReportMapper imgReportMapper;

  @Resource
  private ImageTaskService imageTaskService;

  @Resource
  private FileUploadService fileUploadService;

  @Resource
  private ArticleService articleService;

  private final AtomicLong idCounter = new AtomicLong(System.currentTimeMillis());

  /**
   * 生成报告并上传文件
   *
   * @param reqVO 报告生成请求参数
   * @param file  PDF文件
   * @return 报告ID
   */
  public CommonResult<Long> generateReport(ReportGenerateReqVO reqVO, MultipartFile file) {
    if (file == null || file.isEmpty()) {
      return CommonResult.error(500, "上传的报告文件不能为空");
    }

    // 1. 上传报告文件
    String dateStr = new SimpleDateFormat("yyyyMMdd").format(new Date());
    String filePath = "report/" + dateStr;

    // 使用 fileUploadService 上传文件
    MultipartFile[] files = new MultipartFile[] {file};
    ImageTaskCreateResDTO imageTaskResDTO = fileUploadService.uploadFiles(files, filePath);
    if (!Boolean.TRUE.equals(imageTaskResDTO.getSuccess()) || CollectionUtils.isEmpty(imageTaskResDTO.getSuccessFile())) {
      return CommonResult.error(500, "报告文件上传失败");
    }

    String reportPath = imageTaskResDTO.getSuccessFile()
        .get(0).getFilePath();
    // 3. 保存报告信息到数据库
    // 查询有没有旧的报告数据，如果有判断路径下是否存在，如果存在则删除
    ImgReportDO oldReport = imgReportMapper.selectOne(new LambdaQueryWrapper<ImgReportDO>().eq(ImgReportDO::getTaskId, reqVO.getTaskId()), false);
    if (oldReport != null && oldReport.getReportPath() != null) {
      File oldFile = new File(oldReport.getReportPath());
      if (Objects.nonNull(oldFile) && oldFile.exists()) {
        oldFile.delete();
      }
      // 修改状态为已删除
      ImageTaskDO imageTaskDO = new ImageTaskDO();
      imageTaskDO.setId(reqVO.getTaskId());
      imageTaskDO.setDeleted(true);
      imageTaskService.update(imageTaskDO);
    }
    ImgReportDO report = new ImgReportDO();
    report.setTaskId(reqVO.getTaskId());
    // 使用前端传递的报告名称，如果为空，则使用 taskId 作为文件名
    Long reportName = reqVO.getName() != null ? reqVO.getName() : reqVO.getTaskId();
    report.setReportName(String.valueOf(reportName));
    report.setReportPath(reportPath);
    report.setStatus(1); // 报告状态: 已完成
    report.setReportType(2); // 报告类型: 任务报告
    report.setCreateTime(LocalDateTime.now());
    report.setUpdateTime(LocalDateTime.now());
    report.setCreatorId(1L);
    imgReportMapper.insert(report);

    return CommonResult.success(report.getId());
  }

  /**
   * 获取报告分页列表
   *
   * @param pageReqVO 分页查询参数
   * @return 报告分页结果
   */
  public PageResult<ReportPageRespVO> getReportPage(ReportPageReqVO pageReqVO) {
    List<ReportPageRespVO> reportPageRespVOList = imgReportMapper.selectReportAndTaskPage(pageReqVO);
    Long total = imgReportMapper.selectCount(pageReqVO);

    reportPageRespVOList.forEach(reportPageRespVO -> {
      List<ArticleDO> articleDOList = articleService.queryListByTaskId(reportPageRespVO.getTaskId());
      if (reportPageRespVO.getFileType().equals("pdf")) {
        reportPageRespVO.setArticleTitleMap(articleDOList.stream().collect(Collectors.toMap(ArticleDO::getId, ArticleDO::getArticleTitle)));
        reportPageRespVO.setArticleJournalMap(articleDOList.stream().collect(Collectors.toMap(ArticleDO::getId, ArticleDO::getArticleJournal)));
        reportPageRespVO.setAuthorNameMap(articleDOList.stream().collect(Collectors.toMap(ArticleDO::getId, ArticleDO::getAuthorName)));
      } else {
        reportPageRespVO.setFirstImage(articleDOList.stream()
            .map(ArticleDO::getFilePath)
            .collect(Collectors.toList()));
      }
    });

    return new PageResult<>(reportPageRespVOList, total);
  }

  public ImgReportDO getReport(Long id) {
    return imgReportMapper.selectById(id);
  }

  public void deleteReport(List<Long> ids) {
    for (Long id : ids) {
      ImgReportDO report = imgReportMapper.selectById(id);
      if (report != null) {
        // 删除报告文件
        File file = new File(report.getReportPath());
        if (file.exists()) {
          file.delete();
        }
        // 删除数据库记录
        imgReportMapper.deleteById(id);
      }
    }
  }

}
