package cn.iocoder.yudao.module.system.service.task;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
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
import cn.iocoder.yudao.module.system.dal.dataobject.task.ImgSimilarityDO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.TaskSearchPreferencesDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.task.ImgReportMapper;
// import com.google.common.collect.Lists;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
// import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.util.Units;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

// iText7 imports
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.extgstate.PdfExtGState;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.io.font.constants.StandardFonts;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import java.net.URL;
import java.net.URLConnection;

// duplicate import removed

// Query with preferences
import cn.iocoder.yudao.module.system.controller.admin.task.vo.similar.ImgSimilarityQueryReqVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.similar.ImgSimilarQueryResVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.similar.ImgSimilarCompareResVO;
// import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.system.api.task.ImgSimilarApiService;
// import cn.iocoder.yudao.module.system.service.task.TaskSearchPreferencesService;
import com.alibaba.fastjson.JSON;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STPageOrientation;

@Slf4j
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
  @Resource
  private ImgSimilarityService imgSimilarityService;
  @Resource
  private SmallImageService smallImageService;
  @Resource
  private cn.iocoder.yudao.module.system.service.user.AdminUserService adminUserService;
  @Resource
  private TaskSearchPreferencesService taskSearchPreferencesService;
  @Resource
  private ImgSimilarApiService imgSimilarApiService;
  @Resource
  private cn.iocoder.yudao.module.system.api.task.ImageTaskApiService imageTaskApiService;

  // private final AtomicLong idCounter = new AtomicLong(System.currentTimeMillis());

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
   * 生成 Word 报告（.doc/.docx）并上传文件
   *
   * 语义与 generateReport 一致，仅区分报告类型与保存路径前缀，便于管理与后续统计。
   */
  public CommonResult<Long> generateWordReport(ReportGenerateReqVO reqVO, MultipartFile file) {
    // 0. 生成与前端 PDF 相同规则的报告编号（yyyyMMdd + 当日生成数量+1，5位补零）
    String todayStr = new SimpleDateFormat("yyyyMMdd").format(new Date());
    LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
    LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59).withNano(0);

    log.info(reqVO.toString());
    long todayCount = imgReportMapper.selectCount(new LambdaQueryWrapperX<ImgReportDO>().between(ImgReportDO::getCreateTime, startOfDay, endOfDay));
    String reportNumber = todayStr + String.format("%05d", todayCount + 1);

    // 1. 组织 Word 文档内容（模仿 PDF 报告布局与数据）
    try (XWPFDocument doc = new XWPFDocument()) {
      // ============ 页面布局：A4 竖版 + 6cm 顶部空白（红头区）============
      configureA4Page(doc);

      // 第一页顶部留红头空（段前距方式，仅作用于第一页）
      XWPFParagraph spacer = doc.createParagraph();
      spacer.setSpacingBefore(6 * 567); // 约 6cm（1cm≈567twips）

      // 报告编号（第一页左上角）- 先编号后标题（与前端 PDF 一致）
      XWPFParagraph numP = doc.createParagraph();
      numP.setAlignment(ParagraphAlignment.LEFT);
      XWPFRun numRun = numP.createRun();
      numRun.setText("报告编号：【" + reportNumber + "】");
      numRun.setFontSize(12);
      numRun.addBreak();

      // 标题
      XWPFParagraph title = doc.createParagraph();
      title.setAlignment(ParagraphAlignment.CENTER);
      XWPFRun titleRun = title.createRun();
      titleRun.addBreak();
      titleRun.setText("论文图像相似检测报告");
      titleRun.setBold(true);
      titleRun.setFontSize(18);
      titleRun.addBreak();



      Long taskId = reqVO.getTaskId();
      ImageTaskDO task = imageTaskService.getById(taskId);
      String fileType = task != null ? task.getFileType() : "";

      // 论文元信息（仅 PDF 有）
      List<ArticleDO> articleDOList = articleService.queryListByTaskId(taskId);
      log.info("articleDOList: {}", articleDOList);
      if ("pdf".equalsIgnoreCase(fileType) && articleDOList != null && !articleDOList.isEmpty()) {
        ArticleDO first = articleDOList.stream().findFirst().orElse(null);
        log.info("first: {}", first);

        // 创建论文作者段落（缩进两个中文字符，冒号对齐）
        XWPFParagraph authorPara = doc.createParagraph();
        authorPara.setAlignment(ParagraphAlignment.LEFT);
        authorPara.setIndentationFirstLine(480); // 缩进两个中文字符（24pt = 480 twips）
        authorPara.setSpacingBefore(0);
        authorPara.setSpacingAfter(0);
        XWPFRun authorRun = authorPara.createRun();
        authorRun.setFontSize(12);
        authorRun.setText("  论文作者：" + (first.getAuthorName() == null || first.getAuthorName().isEmpty() ? "" : first.getAuthorName().get(0)));

        // 创建作者单位段落（缩进两个中文字符，冒号对齐）
        XWPFParagraph unitPara = doc.createParagraph();
        unitPara.setAlignment(ParagraphAlignment.LEFT);
        unitPara.setIndentationFirstLine(480); // 缩进两个中文字符（24pt = 480 twips）
        unitPara.setSpacingBefore(0);
        unitPara.setSpacingAfter(0);
        XWPFRun unitRun = unitPara.createRun();
        unitRun.setFontSize(12);
        unitRun.setText("  作者单位：" + (first.getAuthorInstitution() == null || first.getAuthorInstitution().isEmpty() ? "" : first.getAuthorInstitution().get(0)));

        // 创建论文题目段落（缩进两个中文字符，冒号对齐）
        XWPFParagraph titlePara = doc.createParagraph();
        // 按需：与用户要求一致，论文题目左对齐且去掉首行缩进，清零段前/段后距以消除间隙
        titlePara.setAlignment(ParagraphAlignment.LEFT);
        titlePara.setIndentationFirstLine(480); // 缩进两个中文字符（24pt = 480 twips）
        titlePara.setSpacingBefore(0);
        titlePara.setSpacingAfter(0);
        XWPFRun titleMetaRun = titlePara.createRun();
        titleMetaRun.setFontSize(12);
        // 不在 run 末尾 addBreak，保持与下一段正常间距；去掉开头两个空格确保左对齐
        titleMetaRun.setText("  论文题目：" + (first.getArticleTitle() == null || first.getArticleTitle().isEmpty() ? "" : first.getArticleTitle()));
      }

      // 依据已保存的检索条件，调用统一查询服务获取过滤后的图片对
      TaskSearchPreferencesDO searchPreferences = taskSearchPreferencesService.getSearchPreferences(taskId);
      log.info("searchPreferences: {}", searchPreferences);
      List<ImgSimilarityDO> similarsAll;
      if (searchPreferences != null) {
        ImgSimilarityQueryReqVO req = new ImgSimilarityQueryReqVO();
        req.setTaskId(taskId);
        // 模型
        if (searchPreferences.getModelName() != null) {
          req.setModelNameList(Collections.singletonList(searchPreferences.getModelName()));
        }
        // 图像类型
        if (searchPreferences.getImageTypes() != null) {
          req.setImageTypeList(JSON.parseArray(searchPreferences.getImageTypes(), String.class));
        }
        // 特征点
        req.setFeaturePoints(JSON.parseArray(searchPreferences.getFeaturePoints(), Integer.class));
        // 相似度阈值（后端用0-1）
        if (searchPreferences.getSimilarScoreThreshold() != null) {
          req.setSimilarScoreThreshold(searchPreferences.getSimilarScoreThreshold());
        }
        // 使用已有分页接口拿全量（pageSize足够大）
        req.setPageNo(1);
        req.setPageSize(5000);
        req.setIsSimilar(true);
        PageResult<ImgSimilarQueryResVO> page = imgSimilarApiService.query(req);
        similarsAll = page.getList() == null ? Collections.emptyList() : BeanUtils.toBean(page.getList(), ImgSimilarityDO.class);
      } else {
        similarsAll = imgSimilarityService.queryByTaskId(taskId);
      }
      log.info("similarsAll: {}", similarsAll);

      int similarCnt = similarsAll == null ? 0 : similarsAll.size();
      int uploadCnt = 0;
      String unit = "";
      if ("pdf".equalsIgnoreCase(fileType)) {
        uploadCnt = articleDOList == null ? 0 : articleDOList.size();
        unit = "篇PDF";
      } else {
              uploadCnt = task != null && task.getTotalImages() != null ? task.getTotalImages() : 0;
      unit = "对图片";
    }

    // 获取用户昵称
    String nickName = "?"; // 默认昵称
    if (task != null && task.getCreatorId() != null) {
      AdminUserDO creator = adminUserService.getUserByUserId(task.getCreatorId());
      if (creator != null && creator.getNickname() != null) {
        nickName = creator.getNickname();
      }
    }

    // 创建检测结果段落（缩进两个中文字符，冒号对齐）
    XWPFParagraph resultPara = doc.createParagraph();
    XWPFRun resultRun = resultPara.createRun();
    resultRun.setFontSize(12);
    resultPara.setAlignment(ParagraphAlignment.LEFT);
    resultPara.setIndentationFirstLine(480); // 缩进两个中文字符（24pt = 480 twips）
    resultRun.setText("  检测结果：尊敬的" + nickName + "，您上传" + uploadCnt + unit + "，经检测对比后，其中有" + similarCnt + ("pdf".equalsIgnoreCase(fileType) ? "对图片" : "张图片") + "可能存在相似异常，具体情况如下：");

      // ============ 图片对表格（参照 PDF：第一页 1 组、之后每页 3 组）============
      List<ImgSimilarityDO> similars = similarsAll;
      int totalPairs = similars == null ? 0 : similars.size();
      int rendered = 0;
      int groupIndex = 0; // 全局序号，从 1 开始

      String staticPath = "http://101.37.104.90:48080/admin-api/infra/file/29/get/";
      Integer circle = 1;
      while (rendered < totalPairs) {
        int perPage = (rendered == 0) ? 1 : 3;
        int end = Math.min(rendered + perPage, totalPairs);
        for (int i = rendered; i < end; i++) {
          groupIndex++;
          ImgSimilarityDO item = similars.get(i);

          // 组标题 【n】
          XWPFParagraph gTitle = doc.createParagraph();
          XWPFRun gRun = gTitle.createRun();
          if (circle == 1) {
            gRun.addBreak();
            gRun.addBreak();
          }
          gRun.setText("【" + groupIndex + "】");
          gRun.setBold(true);

          // 四列：上传图片、相似图片、框图、线图（若缺失则显示占位文字）
          org.apache.poi.xwpf.usermodel.XWPFTable table = doc.createTable(2, 4);
          // 以 A4 内容宽度为基准，按 1/6、1/6、1/3、1/3 设置列宽，确保表格严格落在 A4 可写区域内
          int contentWidthTwips = getWordContentWidthTwips(doc);
          int[] colWidths = new int[]{ contentWidthTwips/6, contentWidthTwips/6, contentWidthTwips/3, contentWidthTwips/3 };
          setTableWidthAndColumnWidths(table, contentWidthTwips, colWidths);

          hideTableBorder(table);

          // 第一行：图片
          String sourcePath = null;
          String targetPath = null;
          if (item.getSourceSmallImageId() != null) {
            cn.iocoder.yudao.module.system.dal.dataobject.task.SmallImageDO s = smallImageService.queryById(item.getSourceSmallImageId());
            if (s != null && s.getImagePath() != null) {
              sourcePath = staticPath + StrUtil.replace(s.getImagePath(), "./", "");
            }
          }
          if (item.getTargetSmallImageId() != null) {
            cn.iocoder.yudao.module.system.dal.dataobject.task.SmallImageDO t = smallImageService.queryById(item.getTargetSmallImageId());
            if (t != null && t.getImagePath() != null) {
              targetPath = staticPath + StrUtil.replace(t.getImagePath(), "./", "");
            }
          }

                  // 优先使用数据库字段，如果缺失则动态调用 compare 获取
        String blockImage = getBlockDotImagePath(item.getBlockImage(), staticPath);
        String dotImage = getBlockDotImagePath(item.getDotImage(), staticPath);
        if ((blockImage == null || dotImage == null) && item.getId() != null) {
          try {
            CommonResult<ImgSimilarCompareResVO> cmp = imgSimilarApiService.compare(item.getId());
            if (cmp != null && Boolean.TRUE.equals(cmp.isSuccess()) && cmp.getData() != null) {
              // compare 返回的路径是本地前缀，保持与 Word 处理一致转换
              String cmpBlock = cmp.getData().getBlockImage();
              String cmpDot = cmp.getData().getDotImage();
              blockImage = blockImage == null ? getBlockDotImagePath(cmpBlock, staticPath) : blockImage;
              dotImage = dotImage == null ? getBlockDotImagePath(cmpDot, staticPath) : dotImage;
            }
          } catch (Exception e) {
            log.warn("compare 接口调用失败: {}", e.getMessage());
          }
        }

          log.info("sourcePath: {}", sourcePath);
          log.info("targetPath: {}", targetPath);
          log.info("blockImage: {}", blockImage);
          log.info("dotImage: {}", dotImage);

          addImageCell(table, 0, 0, sourcePath, "上传图片缺失");
          addImageCell(table, 0, 1, targetPath, "相似图片缺失");
          addImageCell(table, 0, 2, blockImage, "框图缺失");
          addImageCell(table, 0, 3, dotImage, "线图缺失");

          // 第二行：标题
          setCellText(table, 1, 0, "【上传图片】");
          setCellText(table, 1, 1, "【相似图片】");
          setCellText(table, 1, 2, "【框图】");
          setCellText(table, 1, 3, "【线图】");

          // 相似程度 + 评语
          XWPFParagraph desc = doc.createParagraph();
          XWPFRun descRun = desc.createRun();
          String levelText = "";
          Integer featurePoints = item.getFeaturePointCnt();
          if (featurePoints == null) {
            levelText = "低相似风险";
          } else if (featurePoints >= 1 && featurePoints <= 5) {
            levelText = "低相似风险";
          } else if (featurePoints > 5 && featurePoints <= 25) {
            levelText = "中相似风险";
          } else if (featurePoints > 25) {
            levelText = "高相似风险";
          } else {
            levelText = "低相似风险";
          }

          descRun.addBreak();
          descRun.setText("相似程度为【" + levelText + "】【" + (item.getReviewComment() == null ? "暂无评语" : item.getReviewComment()) + "】。");
        }

        rendered = end;
        if (rendered < totalPairs) {
          // 新起一页
          XWPFParagraph pb = doc.createParagraph();
          pb.setPageBreak(true);
        }
      }

      // ============ 仅在最后一页底部展示检测机构与时间（这里在文末追加一段右对齐文本近似）============
      XWPFParagraph footer = doc.createParagraph();
      footer.setAlignment(ParagraphAlignment.RIGHT);
      XWPFRun footerRun = footer.createRun();
      footerRun.addBreak();
      footerRun.setText("检测机构：中国医学科学院医学信息研究所/图书馆");
      footerRun.addBreak();
      java.time.LocalDate today = java.time.LocalDate.now();
      footerRun.setText(String.format("%d年%d月%d日", today.getYear(), today.getMonthValue(), today.getDayOfMonth()));

      // 2. 输出到内存并封装为 MultipartFile
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      doc.write(bos);
      byte[] bytes = bos.toByteArray();
      ByteArrayInputStream bis = new ByteArrayInputStream(bytes);

      String fileName = reportNumber + ".docx";
      MultipartFile generated = new MockMultipartFile(
          "file",
          fileName,
          MediaType.APPLICATION_OCTET_STREAM_VALUE,
          bis
      );

      // 3. 上传报告文件（路径前缀区分为 report-word/YYYYMMDD）
      String dateStr = new SimpleDateFormat("yyyyMMdd").format(new Date());
      String filePath = "report/" + dateStr;
      MultipartFile[] files = new MultipartFile[] {generated};
      ImageTaskCreateResDTO imageTaskResDTO = fileUploadService.uploadFiles(files, filePath);
      if (!Boolean.TRUE.equals(imageTaskResDTO.getSuccess()) || CollectionUtils.isEmpty(imageTaskResDTO.getSuccessFile())) {
        return CommonResult.error(500, "报告文件上传失败");
      }

      String reportPath = imageTaskResDTO.getSuccessFile().get(0).getFilePath();

    // 2. 旧报告清理（同一个 taskId 仅保留最新）
    ImgReportDO oldReport = imgReportMapper.selectOne(new LambdaQueryWrapper<ImgReportDO>().eq(ImgReportDO::getTaskId, reqVO.getTaskId()), false);
    if (oldReport != null && oldReport.getReportPath() != null) {
      File oldFile = new File(oldReport.getReportPath());
      if (Objects.nonNull(oldFile) && oldFile.exists()) {
        oldFile.delete();
      }
      ImageTaskDO imageTaskDO = new ImageTaskDO();
      imageTaskDO.setId(reqVO.getTaskId());
      imageTaskDO.setDeleted(true);
      imageTaskService.update(imageTaskDO);
    }

    // 3. 保存数据库
    ImgReportDO report = new ImgReportDO();
    report.setTaskId(reqVO.getTaskId());
    // 名称采用与 PDF 一致的编号策略
    report.setReportName(reportNumber);
    report.setReportPath(reportPath);
    report.setReviewerId(task.getReviewerId());
    report.setReviewTime(LocalDateTime.now());
    report.setModelList(searchPreferences.getModelName());
    report.setImageTypeList(searchPreferences.getImageTypes());
    // featurePoints already stored as JSON string in searchPreferences, keep as is
    if (searchPreferences.getFeaturePoints() != null) {
      report.setFeaturePoints(searchPreferences.getFeaturePoints());
    } else {
      report.setFeaturePoints("[]");
    }
    report.setSimilarThreshold(searchPreferences.getSimilarScoreThreshold());
    report.setStatus(1); // 已完成
    report.setReportType(3); // 报告类型: Word 报告（与 PDF 区分，2=任务报告(PDF)，3=任务报告(Word)）
    report.setCreateTime(LocalDateTime.now());
    report.setUpdateTime(LocalDateTime.now());
    report.setCreatorId(1L);
    imgReportMapper.insert(report);

    return CommonResult.success(report.getId());
    } catch (Exception e) {
      log.error("生成 Word 报告失败: {}", e.getMessage());
      return CommonResult.error(500, "生成 Word 报告失败: " + e.getMessage());
    }
  }

  /**
   * 生成PDF报告（使用iText7直接生成，包含用户名水印）
   *
   * @param reqVO 报告生成请求参数
   * @param file 前端传入的占位文件
   * @return 报告ID
   */
  public CommonResult<Long> generatePdfReport(ReportGenerateReqVO reqVO, MultipartFile file) {
    try {
      // 生成报告编号
      String todayStr = new SimpleDateFormat("yyyyMMdd").format(new Date());
      LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
      LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59).withNano(0);

      long todayCount = imgReportMapper.selectCount(new LambdaQueryWrapperX<ImgReportDO>().between(ImgReportDO::getCreateTime, startOfDay, endOfDay));
      String reportNumber = todayStr + String.format("%05d", todayCount + 1);

      // 获取任务信息和用户昵称（用于水印）
      Long taskId = reqVO.getTaskId();
      ImageTaskDO task = imageTaskService.getById(taskId);
      String fileType = task != null ? task.getFileType() : "";

      String nickName = "未知用户";
      if (task != null && task.getCreatorId() != null) {
        cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO creator = adminUserService.getUser(task.getCreatorId());
        if (creator != null && creator.getNickname() != null) {
          nickName = creator.getNickname();
        }
      }

      // 创建PDF文档
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      PdfWriter writer = new PdfWriter(baos);
      PdfDocument pdfDoc = new PdfDocument(writer);
      Document document = new Document(pdfDoc, PageSize.A4);

      // 设置中文字体（多重回退方案）
      PdfFont chineseFont = createSafeFont();
      log.info("PDF字体创建成功，开始生成内容");

      // 6cm顶部留白（约170pt）
      document.setTopMargin(170);

      // 报告编号（左上角）
      Paragraph reportNumberPara = new Paragraph("报告编号：【" + reportNumber + "】")
          .setFont(chineseFont)
          .setFontSize(12)
          .setFontColor(ColorConstants.BLACK)
          .setTextAlignment(TextAlignment.LEFT);
      document.add(reportNumberPara);

      // 标题（居中，黑体）
      Paragraph title = new Paragraph("论文图像相似检测报告")
          .setFont(chineseFont)
          .setFontSize(18)
          .setBold()
          .setFontColor(ColorConstants.BLACK)
          .setTextAlignment(TextAlignment.CENTER)
          .setMarginBottom(20);
      document.add(title);

      // 根据文件类型添加元信息
      if ("pdf".equalsIgnoreCase(fileType)) {
        List<ArticleDO> articleDOList = articleService.queryListByTaskId(taskId);
        if (articleDOList != null && !articleDOList.isEmpty()) {
          ArticleDO first = articleDOList.get(0);

          // 论文作者（缩进两个中文字符）
          Paragraph authorPara = new Paragraph("  论文作者：" +
              (first.getAuthorName() == null || first.getAuthorName().isEmpty() ? "" : first.getAuthorName().get(0)))
              .setFont(chineseFont)
              .setFontSize(12)
              .setFontColor(ColorConstants.BLACK)
              .setFirstLineIndent(24); // 首行缩进24pt（约2个中文字符）
          document.add(authorPara);

          // 作者单位
          Paragraph unitPara = new Paragraph("  作者单位：" +
              (first.getAuthorInstitution() == null || first.getAuthorInstitution().isEmpty() ? "" : first.getAuthorInstitution().get(0)))
              .setFont(chineseFont)
              .setFontSize(12)
              .setFontColor(ColorConstants.BLACK)
              .setFirstLineIndent(24);
          document.add(unitPara);

          // 论文题目
          Paragraph titlePara = new Paragraph("  论文题目：" +
              (first.getArticleTitle() == null ? "" : first.getArticleTitle()))
              .setFont(chineseFont)
              .setFontSize(12)
              .setFontColor(ColorConstants.BLACK)
              .setFirstLineIndent(24);
          document.add(titlePara);
        }
      }

      // 依据已保存的检索条件，调用统一查询服务获取过滤后的图片对
      TaskSearchPreferencesDO searchPreferences = taskSearchPreferencesService.getSearchPreferences(taskId);
      List<ImgSimilarityDO> similarsAll;
      if (searchPreferences != null) {
        ImgSimilarityQueryReqVO req = new ImgSimilarityQueryReqVO();
        req.setTaskId(taskId);
        if (searchPreferences.getModelName() != null) {
          req.setModelNameList(Collections.singletonList(searchPreferences.getModelName()));
        }
        if (searchPreferences.getImageTypes() != null) {
          req.setImageTypeList(JSON.parseArray(searchPreferences.getImageTypes(), String.class));
        }
        req.setFeaturePoints(JSON.parseArray(searchPreferences.getFeaturePoints(), Integer.class));
        if (searchPreferences.getSimilarScoreThreshold() != null) {
          req.setSimilarScoreThreshold(searchPreferences.getSimilarScoreThreshold());
        }
        req.setPageNo(1);
        req.setPageSize(5000);
        req.setIsSimilar(true);
        log.info("req: {}", req);
        PageResult<ImgSimilarQueryResVO> page = imgSimilarApiService.query(req);
        log.info("page.list: {}", page.getList());
        similarsAll = page.getList() == null ? Collections.emptyList() : BeanUtils.toBean(page.getList(), ImgSimilarityDO.class);
      } else {
        similarsAll = imgSimilarityService.queryByTaskId(taskId);
      }
      int similarCnt = similarsAll == null ? 0 : similarsAll.size();
      int uploadCnt = 0;
      String unit = "";

      if ("pdf".equalsIgnoreCase(fileType)) {
        List<ArticleDO> articleDOList = articleService.queryListByTaskId(taskId);
        uploadCnt = articleDOList == null ? 0 : articleDOList.size();
        unit = "篇PDF";
      } else {
        uploadCnt = task != null && task.getTotalImages() != null ? task.getTotalImages() : 0;
        unit = "对图片";
      }

      Paragraph resultPara = new Paragraph("  检测结果：尊敬的" + nickName + "，您上传" + uploadCnt + unit +
          "，经检测对比后，其中有" + similarCnt + ("pdf".equalsIgnoreCase(fileType) ? "对图片" : "对图片") +
          "可能存在相似异常，具体情况如下：")
          .setFont(chineseFont)
          .setFontSize(12)
          .setFontColor(ColorConstants.BLACK)
          .setFirstLineIndent(24);
      document.add(resultPara);

      // ============ 添加图片对数据（参考Word报告逻辑）============
      if (similarsAll != null && !similarsAll.isEmpty()) {
        addSimilarImagePairsToPdf(document, chineseFont, similarsAll);
      }

      // 检测机构与时间（右对齐）
      Paragraph footer = new Paragraph()
          .add(new Text("检测机构：中国医学科学院医学信息研究所/图书馆\n"))
          .add(new Text(getCurrentTimeString()))
          .setFont(chineseFont)
          .setFontSize(12)
          .setFontColor(ColorConstants.BLACK)
          .setTextAlignment(TextAlignment.RIGHT)
          .setMarginTop(50);
      document.add(footer);

      // 在关闭文档前添加水印到所有页面
      try {
        addWatermarkToAllPages(pdfDoc, nickName, chineseFont);
        log.info("水印添加成功");
      } catch (Exception e) {
        log.warn("水印添加失败，但继续生成PDF: {}", e.getMessage());
      }

      // 关闭文档以完成内容写入
      document.close();
      log.info("PDF文档生成完成，大小: {} bytes", baos.size());

      // 转换为MultipartFile并上传
      byte[] pdfBytes = baos.toByteArray();
      String fileName = reportNumber + ".pdf";
      MultipartFile generated = new MockMultipartFile(
          "file",
          fileName,
          "application/pdf",
          new ByteArrayInputStream(pdfBytes)
      );

      // 上传文件
      String dateStr = new SimpleDateFormat("yyyyMMdd").format(new Date());
      String filePath = "report/" + dateStr;
      MultipartFile[] files = new MultipartFile[] {generated};
      ImageTaskCreateResDTO imageTaskResDTO = fileUploadService.uploadFiles(files, filePath);

      if (!Boolean.TRUE.equals(imageTaskResDTO.getSuccess()) || CollectionUtils.isEmpty(imageTaskResDTO.getSuccessFile())) {
        return CommonResult.error(500, "PDF报告文件上传失败");
      }

      String reportPath = imageTaskResDTO.getSuccessFile().get(0).getFilePath();

      // 保存到数据库
      ImgReportDO report = new ImgReportDO();
      report.setTaskId(reqVO.getTaskId());
      report.setReportName(reportNumber);
      report.setReportPath(reportPath);
      report.setStatus(1);
      report.setReportType(4); // PDF报告类型
      report.setCreateTime(LocalDateTime.now());
      report.setUpdateTime(LocalDateTime.now());
      report.setCreatorId(1L);
      imgReportMapper.insert(report);

      return CommonResult.success(report.getId());

    } catch (Exception e) {
      log.error("生成PDF报告失败，尝试简化版本", e);
      return CommonResult.error(500, "生成PDF报告失败: " + e.getMessage());
    }
  }

  /**
   * 为PDF添加相似图片对数据（参考Word报告逻辑）
   */
  private void addSimilarImagePairsToPdf(Document document, PdfFont font, List<ImgSimilarityDO> similarsAll) {
    try {
      int totalPairs = similarsAll.size();
      int groupIndex = 0; // 全局序号，从 1 开始
      String staticPath = "http://101.37.104.90:48080/admin-api/infra/file/29/get/";

      log.info("开始添加{}个相似图片对到PDF", totalPairs);

      for (ImgSimilarityDO item : similarsAll) {
        groupIndex++;

        // 添加组标题 【n】
        Paragraph groupTitle = new Paragraph("【" + groupIndex + "】")
            .setFont(font)
            .setFontSize(12)
            .setFontColor(ColorConstants.BLACK)
            .setBold()
            .setMarginTop(10);
        document.add(groupTitle);

        // 创建表格：2行4列（上传图片、相似图片、框图、线图）
        com.itextpdf.layout.element.Table table = new com.itextpdf.layout.element.Table(4);
        table.setWidth(com.itextpdf.layout.properties.UnitValue.createPercentValue(100));

        // 获取图片路径
        String sourcePath = getImagePath(item.getSourceSmallImageId(), staticPath);
        String targetPath = getImagePath(item.getTargetSmallImageId(), staticPath);
        // 优先使用数据库字段，如果缺失则动态调用 compare 获取
        String blockImage = getBlockDotImagePath(item.getBlockImage(), staticPath);
        String dotImage = getBlockDotImagePath(item.getDotImage(), staticPath);
        if ((blockImage == null || dotImage == null) && item.getId() != null) {
          try {
            CommonResult<ImgSimilarCompareResVO> cmp = imgSimilarApiService.compare(item.getId());
            if (cmp != null && Boolean.TRUE.equals(cmp.isSuccess()) && cmp.getData() != null) {
              // compare 返回的路径是本地前缀，保持与 Word 处理一致转换
              String cmpBlock = cmp.getData().getBlockImage();
              String cmpDot = cmp.getData().getDotImage();
              blockImage = blockImage == null ? getBlockDotImagePath(cmpBlock, staticPath) : blockImage;
              dotImage = dotImage == null ? getBlockDotImagePath(cmpDot, staticPath) : dotImage;
            }
          } catch (Exception e) {
            log.warn("compare 接口调用失败: {}", e.getMessage());
          }
        }

        // 第一行：图片标题
        table.addCell(createTableCell("【上传图片】", font));
        table.addCell(createTableCell("【相似图片】", font));
        table.addCell(createTableCell("【框图】", font));
        table.addCell(createTableCell("【线图】", font));

        // 第二行：图片或占位文字（后续可替换为实际图片插入逻辑）
        table.addCell(createImageOrTextCell(sourcePath, "上传图片缺失", font));
        table.addCell(createImageOrTextCell(targetPath, "相似图片缺失", font));
        table.addCell(createImageOrTextCell(blockImage, "框图缺失", font));
        table.addCell(createImageOrTextCell(dotImage, "线图缺失", font));

        document.add(table);

        // 添加相似程度和评语
        String levelText = getSimilarityLevelText(item.getSimilarityScore());
        String reviewComment = item.getReviewComment() == null ? "暂无评语" : item.getReviewComment();

        Paragraph desc = new Paragraph("相似程度为【" + levelText + "】【" + reviewComment + "】。")
            .setFont(font)
            .setFontSize(12)
            .setFontColor(ColorConstants.BLACK)
            .setMarginBottom(15);
        document.add(desc);

        log.debug("已添加第{}个相似图片对", groupIndex);
      }

      log.info("相似图片对添加完成，共{}个", totalPairs);

    } catch (Exception e) {
      log.error("添加相似图片对失败：{}", e.getMessage(), e);
    }
  }

  /**
   * 获取图片路径（参考Word报告逻辑）
   */
  private String getImagePath(Long imageId, String staticPath) {
    if (imageId == null) return null;
    try {
      cn.iocoder.yudao.module.system.dal.dataobject.task.SmallImageDO image = smallImageService.queryById(imageId);
      if (image != null && image.getImagePath() != null) {
        return staticPath + StrUtil.replace(image.getImagePath(), "./", "");
      }
    } catch (Exception e) {
      log.warn("获取图片路径失败，imageId: {}", imageId, e);
    }
    return null;
  }

  /**
   * 获取框图/线图路径
   */
  private String getBlockDotImagePath(String imagePath, String staticPath) {
    if (imagePath == null || imagePath.trim().isEmpty()) return null;
    return staticPath + StrUtil.replace(imagePath, "./", "");
  }

  /**
   * 创建表格单元格
   */
  private com.itextpdf.layout.element.Cell createTableCell(String text, PdfFont font) {
    return new com.itextpdf.layout.element.Cell()
        .add(new Paragraph(text)
            .setFont(font)
            .setFontSize(10)
            .setFontColor(ColorConstants.BLACK)
            .setTextAlignment(TextAlignment.CENTER));
  }

  /**
   * 创建带图片或占位文字的单元格（当前先文本占位，后续可替换为图片）
   */
  private com.itextpdf.layout.element.Cell createImageOrTextCell(String imagePath, String placeholder, PdfFont font) {
    String text = (imagePath != null) ? "图片显示" : placeholder;
    return createTableCell(text, font);
  }

  /**
   * 获取相似度级别文本（基于相似度分数）
   */
  private String getSimilarityLevelText(Double similarityScore) {
    if (similarityScore == null) return "中";
    // 根据相似度分数划分级别（参考前端逻辑）
    if (similarityScore >= 0.9) return "高";
    else if (similarityScore >= 0.7) return "中";
    else return "低";
  }

  /**
   * 为PDF的所有页面添加用户名水印
   *
   * @param pdfDoc PDF文档对象
   * @param watermarkText 水印文本（用户名）
   * @param font 字体
   */
  private void addWatermarkToAllPages(PdfDocument pdfDoc, String watermarkText, PdfFont font) {
    try {
      int pageCount = pdfDoc.getNumberOfPages();
      log.info("开始为{}页PDF添加水印：{}", pageCount, watermarkText);

      for (int i = 1; i <= pageCount; i++) {
        PdfPage page = pdfDoc.getPage(i);
        Rectangle pageSize = page.getPageSize();

        // 创建新的Canvas用于添加水印
        PdfCanvas canvas = new PdfCanvas(page.newContentStreamBefore(), page.getResources(), page.getDocument());

        // 设置透明度
        PdfExtGState gState = new PdfExtGState();
        gState.setFillOpacity(0.15f); // 15%透明度
        canvas.setExtGState(gState);

        // 设置水印字体和颜色
        canvas.setFillColor(ColorConstants.LIGHT_GRAY);
        canvas.setFontAndSize(font, 48);

        // 计算水印位置（页面中心）
        float x = pageSize.getWidth() / 2;
        float y = pageSize.getHeight() / 2;

        // 保存图形状态
        canvas.saveState();

        // 移动到水印位置并旋转45度
        canvas.concatMatrix((float) Math.cos(Math.toRadians(45)), (float) Math.sin(Math.toRadians(45)),
                           (float) -Math.sin(Math.toRadians(45)), (float) Math.cos(Math.toRadians(45)), x, y);

        // 添加水印文本
        canvas.beginText();
        canvas.moveText(-50, 0); // 调整文本位置使其居中
        canvas.showText(watermarkText);
        canvas.endText();

        // 恢复图形状态
        canvas.restoreState();

        log.debug("已为第{}页添加水印", i);
      }

      log.info("PDF水印添加完成，共{}页", pageCount);

    } catch (Exception e) {
      log.error("添加PDF水印失败：{}", e.getMessage(), e);
      // 不抛出异常，确保PDF生成流程能继续
    }
  }

  /**
   * 创建安全的字体（多重回退方案）
   *
   * @return PDF字体对象
   */
  private PdfFont createSafeFont() {
    // 尝试多种字体方案，确保PDF不会因字体问题损坏
    String[] fontOptions = {
        "STSong-Light,UniGB-UCS2-H",  // 中文字体方案1
        "SimSun,UniGB-UCS2-H",        // 中文字体方案2
        "Arial Unicode MS",           // Unicode字体
        StandardFonts.HELVETICA       // 最后的保险方案
    };

    for (String fontOption : fontOptions) {
      try {
        if (fontOption.contains(",")) {
          String[] parts = fontOption.split(",");
          return PdfFontFactory.createFont(parts[0], parts[1]);
        } else {
          return PdfFontFactory.createFont(fontOption);
        }
      } catch (Exception e) {
        log.debug("字体加载失败，尝试下一个方案: {}", fontOption, e);
      }
    }

    // 如果所有方案都失败，使用最基础的字体
    try {
      return PdfFontFactory.createFont(StandardFonts.HELVETICA);
    } catch (Exception e) {
      log.error("所有字体方案都失败，这应该不会发生", e);
      throw new RuntimeException("无法创建任何字体", e);
    }
  }

  /**
   * 获取当前时间字符串
   *
   * @return 格式化的时间字符串
   */
  private String getCurrentTimeString() {
    java.time.LocalDate today = java.time.LocalDate.now();
    return String.format("%d年%d月%d日", today.getYear(), today.getMonthValue(), today.getDayOfMonth());
  }

  /**
   * 将 Word 文档设置为 A4 页面，四边常规页边距，并在顶部额外保留约 6cm 的“红头”空白区
   */
  private void configureA4Page(XWPFDocument document) {
    CTSectPr sectPr = document.getDocument().getBody().isSetSectPr()
        ? document.getDocument().getBody().getSectPr()
        : document.getDocument().getBody().addNewSectPr();

    // 页面大小：A4（宽 11906 twips，高 16838 twips）
    CTPageSz pageSz = sectPr.isSetPgSz() ? sectPr.getPgSz() : sectPr.addNewPgSz();
    pageSz.setW(java.math.BigInteger.valueOf(11906));
    pageSz.setH(java.math.BigInteger.valueOf(16838));
    pageSz.setOrient(STPageOrientation.PORTRAIT);

    // 页边距：上 6cm（红头区），左右/下 2.54cm（1 英寸 = 1440 twips）。1cm ≈ 567 twips
    int oneCm = 567;
    int topMargin = 6 * oneCm;   // 顶部红头区 6cm
    int regular = 1440;          // 左/右/下 约 2.54cm

    CTPageMar pageMar = sectPr.isSetPgMar() ? sectPr.getPgMar() : sectPr.addNewPgMar();
    // 全局页边距采用常规 2.54cm；第一页额外的 6cm 红头通过段前距 spacer 控制
    pageMar.setTop(java.math.BigInteger.valueOf(regular));
    pageMar.setLeft(java.math.BigInteger.valueOf(regular));
    pageMar.setRight(java.math.BigInteger.valueOf(regular));
    pageMar.setBottom(java.math.BigInteger.valueOf(regular));
  }

  // 将图片放入表格单元格；若路径无效则写占位文本
  private void addImageCell(org.apache.poi.xwpf.usermodel.XWPFTable table, int row, int col, String imagePath, String placeholder) {
    try {
      if (imagePath != null && !imagePath.trim().isEmpty() && !imagePath.contains("null")) {
        log.info("尝试插入图片：{}", imagePath);

        // 支持 HTTP(S) 与 本地文件两种来源
        XWPFParagraph p = table.getRow(row).getCell(col).getParagraphs().get(0);
        p.setAlignment(ParagraphAlignment.CENTER); // 居中对齐
        XWPFRun r = p.createRun();
        java.io.InputStream is;
        String fileName;

        if (imagePath.startsWith("http")) {
          URL url = new URL(imagePath);
          URLConnection conn = url.openConnection();
          conn.setConnectTimeout(10000);
          conn.setReadTimeout(30000);
          // 设置User-Agent避免某些服务器拒绝请求
          conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
          is = conn.getInputStream();
          fileName = new java.io.File(url.getPath()).getName();
          if (fileName.isEmpty()) fileName = "image";
        } else {
          java.io.File f = new java.io.File(imagePath);
          if (!f.exists()) {
            log.warn("本地文件不存在：{}", imagePath);
            throw new RuntimeException("image not exists");
          }
          is = new java.io.FileInputStream(f);
          fileName = f.getName();
        }

        // 确定图片类型
        int pictureType;
        String lowerPath = imagePath.toLowerCase();
        if (lowerPath.endsWith(".png")) {
          pictureType = XWPFDocument.PICTURE_TYPE_PNG;
        } else if (lowerPath.endsWith(".jpg") || lowerPath.endsWith(".jpeg")) {
          pictureType = XWPFDocument.PICTURE_TYPE_JPEG;
        } else if (lowerPath.endsWith(".gif")) {
          pictureType = XWPFDocument.PICTURE_TYPE_GIF;
        } else {
          pictureType = XWPFDocument.PICTURE_TYPE_JPEG; // 默认JPEG
        }

        // 根据列的宽度比例计算图片尺寸，保持固定高度，按比例调整宽度
        int imageHeight = 80; // 固定高度80像素
        int imageWidth;
        switch (col) {
          case 0: // 上传图片 - 1/6宽度
          case 1: // 相似图片 - 1/6宽度
            imageWidth = 60; // 较小宽度
            break;
          case 2: // 框图 - 1/3宽度
          case 3: // 线图 - 1/3宽度
            imageWidth = 120; // 较大宽度
            break;
          default:
            imageWidth = 80;
        }

        try (java.io.InputStream autoClose = is) {
          // 按表格列宽比例设置图片大小，让图片填充表格单元格
          r.addPicture(autoClose, pictureType, fileName, Units.toEMU(imageWidth), Units.toEMU(imageHeight));
          log.info("图片插入成功：{}, 尺寸: {}x{}", imagePath, imageWidth, imageHeight);
        }
        return;
      } else {
        log.warn("图片路径无效：{}", imagePath);
      }
    } catch (Exception e) {
      log.error("图片插入失败：{}, 错误：{}", imagePath, e.getMessage());
    }
    setCellText(table, row, col, placeholder);
  }

  private void setCellText(org.apache.poi.xwpf.usermodel.XWPFTable table, int row, int col, String text) {
    XWPFParagraph p = table.getRow(row).getCell(col).getParagraphs().get(0);
    XWPFRun r = p.createRun();
    r.setText(text);
  }

  // 隐藏表格边框
  private void hideTableBorder(org.apache.poi.xwpf.usermodel.XWPFTable table) {
    try {
      // 获取或创建表格属性
      org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr tblPr = table.getCTTbl().getTblPr();
      if (tblPr == null) {
        tblPr = table.getCTTbl().addNewTblPr();
      }

      // 获取或创建表格边框
      org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblBorders borders = tblPr.getTblBorders();
      if (borders == null) {
        borders = tblPr.addNewTblBorders();
      }

      // 设置所有边框为NONE
      setBorderToNone(borders.addNewTop());
      setBorderToNone(borders.addNewBottom());
      setBorderToNone(borders.addNewLeft());
      setBorderToNone(borders.addNewRight());
      setBorderToNone(borders.addNewInsideH());
      setBorderToNone(borders.addNewInsideV());

      // 同时设置每个单元格的边框为NONE
      for (org.apache.poi.xwpf.usermodel.XWPFTableRow row : table.getRows()) {
        for (org.apache.poi.xwpf.usermodel.XWPFTableCell cell : row.getTableCells()) {
          org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr tcPr = cell.getCTTc().getTcPr();
          if (tcPr == null) {
            tcPr = cell.getCTTc().addNewTcPr();
          }
          org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcBorders cellBorders = tcPr.getTcBorders();
          if (cellBorders == null) {
            cellBorders = tcPr.addNewTcBorders();
          }
          setBorderToNone(cellBorders.addNewTop());
          setBorderToNone(cellBorders.addNewBottom());
          setBorderToNone(cellBorders.addNewLeft());
          setBorderToNone(cellBorders.addNewRight());
        }
      }

      log.info("表格边框已隐藏");
    } catch (Exception e) {
      log.error("隐藏表格边框失败：{}", e.getMessage());
    }
  }

  // 设置边框为NONE的辅助方法
  private void setBorderToNone(org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder border) {
    border.setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.NONE);
    border.setSz(java.math.BigInteger.ZERO);
    border.setSpace(java.math.BigInteger.ZERO);
    border.setColor("FFFFFF"); // 白色
  }

  // 设置表格列宽比例（方法已停用）
  // private void setTableColumnWidths(org.apache.poi.xwpf.usermodel.XWPFTable table, int[] widths) { }

  /**
   * 设置表格宽度为内容区宽度，并精确设置各列宽
   */
  private void setTableWidthAndColumnWidths(org.apache.poi.xwpf.usermodel.XWPFTable table, int tableWidthTwips, int[] colWidths) {
    try {
      org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr tblPr = table.getCTTbl().getTblPr();
      if (tblPr == null) tblPr = table.getCTTbl().addNewTblPr();
      org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth tblWidth = tblPr.isSetTblW() ? tblPr.getTblW() : tblPr.addNewTblW();
      tblWidth.setType(org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth.DXA);
      tblWidth.setW(java.math.BigInteger.valueOf(tableWidthTwips));

      for (org.apache.poi.xwpf.usermodel.XWPFTableRow row : table.getRows()) {
        for (int i = 0; i < row.getTableCells().size() && i < colWidths.length; i++) {
          org.apache.poi.xwpf.usermodel.XWPFTableCell cell = row.getCell(i);
          org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr tcPr = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
          org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth cw = tcPr.isSetTcW() ? tcPr.getTcW() : tcPr.addNewTcW();
          cw.setType(org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth.DXA);
          cw.setW(java.math.BigInteger.valueOf(colWidths[i]));
        }
      }
    } catch (Exception e) {
      log.error("设置表格宽度失败：{}", e.getMessage());
    }
  }

  /**
   * 计算 Word 文档当前节的可用内容宽度（A4 宽度 - 左右页边距），单位 twips
   */
  private int getWordContentWidthTwips(XWPFDocument document) {
    CTSectPr sectPr = document.getDocument().getBody().isSetSectPr()
        ? document.getDocument().getBody().getSectPr()
        : document.getDocument().getBody().addNewSectPr();
    CTPageSz pgSz = sectPr.isSetPgSz() ? sectPr.getPgSz() : sectPr.addNewPgSz();
    CTPageMar mar = sectPr.isSetPgMar() ? sectPr.getPgMar() : sectPr.addNewPgMar();
    int w = Integer.parseInt(pgSz.getW().toString());
    int left = Integer.parseInt(mar.getLeft().toString());
    int right = Integer.parseInt(mar.getRight().toString());
    return Math.max(0, w - left - right);
  }

  /**
   * 获取报告分页列表
   *
   * @param pageReqVO 分页查询参数
   * @return 报告分页结果
   */
  public PageResult<ReportPageRespVO> getReportPage(ReportPageReqVO pageReqVO) {
    pageReqVO.setPageNo(pageReqVO.getPageNo() - 1);
    Long userId = cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils.getLoginUserId();

    // 基于角色决定可见范围
    String roleCode = imageTaskApiService.getCurrentUserPrimaryRoleCode();

    List<ReportPageRespVO> reportPageRespVOList;
    Long total;

    if ("super_admin".equals(roleCode)) {
      // 系统管理员：查看所有报告
      reportPageRespVOList = imgReportMapper.selectManagerReportAndTaskPage(pageReqVO);
      total = imgReportMapper.selectManagerCounts(pageReqVO);
    } else if ("Research_admin".equals(roleCode)) {
      // 科研管理员：自己上传 + 自己分配给专家的任务
      reportPageRespVOList = imgReportMapper.selectReportAndTaskPage(pageReqVO, userId);
      total = imgReportMapper.selectCounts(pageReqVO, userId);
    } else if ("Expert_admin".equals(roleCode)) {
      // 专家用户：仅自己被分配审核（reviewer）的任务
      reportPageRespVOList = imgReportMapper.selectReviewerReportAndTaskPage(pageReqVO, userId);
      total = imgReportMapper.selectReviewerCounts(pageReqVO, userId);
    } else {
      // 普通用户：仅自己上传
      reportPageRespVOList = imgReportMapper.selectCreatorReportAndTaskPage(pageReqVO, userId);
      total = imgReportMapper.selectCreatorCounts(pageReqVO, userId);
    }
    log.info("total: {}, reportPageRespVOList: {}", total, reportPageRespVOList);
    if (total == null || total == 0) {
      return new PageResult<>(reportPageRespVOList, total);
    }
   if (CollectionUtil.isEmpty(reportPageRespVOList)) {
    return new PageResult<>(reportPageRespVOList, total);
   }

    List<Long> taskIds = reportPageRespVOList.stream().map(ReportPageRespVO::getTaskId).collect(Collectors.toList());
    List<ArticleDO> articleDOList = articleService.queryListByTaskIds(taskIds);
    Map<Long, List<ArticleDO>> articleDOMap = articleDOList.stream().collect(Collectors.groupingBy(ArticleDO::getTaskId));

    reportPageRespVOList.forEach(reportPageRespVO -> {
      if (reportPageRespVO.getFileType().equals("pdf")) {
        reportPageRespVO.setArticleTitleMap(articleDOMap.get(reportPageRespVO.getTaskId()).stream().collect(Collectors.toMap(ArticleDO::getId, ArticleDO::getArticleTitle)));
        reportPageRespVO.setArticleJournalMap(articleDOMap.get(reportPageRespVO.getTaskId()).stream().collect(Collectors.toMap(ArticleDO::getId, ArticleDO::getArticleJournal)));
        reportPageRespVO.setAuthorNameMap(articleDOMap.get(reportPageRespVO.getTaskId()).stream().collect(Collectors.toMap(ArticleDO::getId, ArticleDO::getAuthorName)));
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
