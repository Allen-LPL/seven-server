package cn.iocoder.yudao.module.system.controller.admin.task;


import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.system.api.task.ImageTaskApiService;
import cn.iocoder.yudao.module.system.api.task.dto.ImageTaskCreateResDTO;
import cn.iocoder.yudao.module.system.api.task.dto.ImageTaskQueryResDTO;
import cn.iocoder.yudao.module.system.api.task.dto.TaskStrategyConfig;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.task.ImageTaskAllocateReqVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.task.ImageTaskCreateReqVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.task.ImageTaskCreateResVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.task.ImageTaskQueryReqVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.task.ImageTaskReviewReqVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.task.ImageTaskUpdateReqVO;
import cn.iocoder.yudao.module.system.service.notify.NotifySendService;

import com.alibaba.fastjson.JSONObject;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Map;

import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "管理后台 - 任务管理")
@RestController
@RequestMapping("/task/manager")
@Slf4j
public class TaskController {

  @Resource
  private ImageTaskApiService imageTaskApiService;

  @Resource
  private NotifySendService notifySendService;

  @RequestMapping(method = RequestMethod.POST, value = "/create" , consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public CommonResult<ImageTaskCreateResVO> create( @Parameter(description = "上传文件", content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE)) @RequestPart("files") MultipartFile[] files,
      @RequestParam("taskType") Integer taskType, @RequestParam("fileType") String fileType,
      @RequestParam(value = "taskStrategyConfig",required = false) String taskStrategyConfig) {
    try {
      ImageTaskCreateReqVO reqVO = new ImageTaskCreateReqVO();
      reqVO.setTaskType(taskType);
      reqVO.setFiles(files);
      reqVO.setFileType(fileType);
      if (StringUtils.isNotBlank(taskStrategyConfig)) {
        reqVO.setTaskStrategyConfig(JSONObject.parseObject(taskStrategyConfig, TaskStrategyConfig.class));
      }
      ImageTaskCreateResDTO imageTaskResDTO =  imageTaskApiService.createTask(reqVO);

      return CommonResult.success(BeanUtils.toBean(imageTaskResDTO, ImageTaskCreateResVO.class));
    }catch (Exception e) {
      log.error("创建检测任务失败, ",e);
      return CommonResult.error(new ErrorCode(500, e.getMessage()));
    }
  }

  @GetMapping("/get")
  public CommonResult<ImageTaskQueryResDTO> get(@RequestParam("id") Long id) {
    try {
      ImageTaskQueryReqVO imageTaskQueryReqVO = new ImageTaskQueryReqVO();
      imageTaskQueryReqVO.setTaskId(id);
      return imageTaskApiService.get(imageTaskQueryReqVO);
    }catch (Exception e) {
      log.error("查询失败，",e);
      return CommonResult.error(new ErrorCode(500, e.getMessage()));
    }
  }

  @GetMapping("/query")
  public CommonResult<PageResult<ImageTaskQueryResDTO>> create(ImageTaskQueryReqVO reqVO) {
    try {
      PageResult<ImageTaskQueryResDTO> pageResult = imageTaskApiService.query(reqVO);
      return CommonResult.success(pageResult);
    }catch (Exception e) {
      log.error("查询失败，",e);
      return CommonResult.error(new ErrorCode(500, e.getMessage()));
    }
  }

  @PostMapping("/allocate")
  public CommonResult<String> allocate(@RequestBody ImageTaskAllocateReqVO allocateReqVO) {
    try {
      return imageTaskApiService.allocateTask(allocateReqVO);
    }catch (Exception e) {
      log.error("allocate error，",e);
      return CommonResult.error(new ErrorCode(500, e.getMessage()));
    }
  }

  @PostMapping("/review")
  public CommonResult<String> review(@RequestBody ImageTaskReviewReqVO reviewReqVO) {
    try {
      return imageTaskApiService.reviewTask(reviewReqVO);
    }catch (Exception e) {
      log.error("allocate error，",e);
      return CommonResult.error(new ErrorCode(500, e.getMessage()));
    }
  }

  /**
   * 标记/取消任务案例
   */
  @PutMapping("/case-flag")
  public CommonResult<String> caseFlag(@RequestBody ImageTaskUpdateReqVO updateReqVO) {
    try {
      return imageTaskApiService.updateCaseFlag(updateReqVO);
    } catch (Exception e) {
      log.error("update case flag error，", e);
      return CommonResult.error(new ErrorCode(500, e.getMessage()));
    }
  }

  @RequestMapping(method = RequestMethod.DELETE, value = "/clear-allocation/{taskId}")
  public CommonResult<String> clearAllocation(@PathVariable("taskId") Long id) {
    try {
      return imageTaskApiService.clearTaskAllocation(id);
    } catch (Exception e) {
      log.error("clear allocation error，", e);
      return CommonResult.error(new ErrorCode(500, e.getMessage()));
    }
  }

  @PutMapping("/update")
  public CommonResult<String> updateTask(@RequestBody ImageTaskUpdateReqVO updateReqVO) {
    try {
      return imageTaskApiService.updateTask(updateReqVO);
    } catch (Exception e) {
      log.error("update task error，", e);
      return CommonResult.error(new ErrorCode(500, e.getMessage()));
    }
  }

  /**
   * 逻辑删除任务
   */
  @RequestMapping(method = RequestMethod.DELETE, value = "/delete/{taskId}")
  public CommonResult<String> deleteTask(@PathVariable("taskId") Long taskId) {
    try {
      return imageTaskApiService.deleteTask(taskId);
    } catch (Exception e) {
      log.error("delete task error，", e);
      return CommonResult.error(new ErrorCode(500, e.getMessage()));
    }
  }

}
