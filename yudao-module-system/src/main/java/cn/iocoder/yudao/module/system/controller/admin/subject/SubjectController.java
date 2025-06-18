package cn.iocoder.yudao.module.system.controller.admin.subject;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.system.controller.admin.subject.vo.SubjectRespVO;
import cn.iocoder.yudao.module.system.convert.subject.SubjectConvert;
import cn.iocoder.yudao.module.system.dal.dataobject.subject.SubjectDO;
import cn.iocoder.yudao.module.system.service.subject.SubjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 学科管理")
@RestController
@RequestMapping("/system/subject")
@Validated
public class SubjectController {

    @Resource
    private SubjectService subjectService;

    @GetMapping("/first-level")
    @Operation(summary = "获取一级学科列表")
    @PermitAll
    public CommonResult<List<SubjectRespVO>> getFirstLevelSubjects() {
        List<SubjectDO> subjects = subjectService.getFirstLevelSubjects();
        return success(SubjectConvert.INSTANCE.convertList(subjects));
    }

    @GetMapping("/second-level")
    @Operation(summary = "根据父级ID获取二级学科列表")
    @Parameter(name = "parentId", description = "父级学科编号", required = true, example = "1")
    @PermitAll
    public CommonResult<List<SubjectRespVO>> getSecondLevelSubjects(@RequestParam("parentId") Long parentId) {
        List<SubjectDO> subjects = subjectService.getSecondLevelSubjects(parentId);
        return success(SubjectConvert.INSTANCE.convertList(subjects));
    }

    @GetMapping("/tree")
    @Operation(summary = "获取学科树形结构")
    @PermitAll
    public CommonResult<List<SubjectRespVO>> getSubjectTree() {
        // 获取所有启用状态的学科
        List<SubjectDO> allSubjects = subjectService.getAllEnabledSubjects();
        
        // 构建树形结构
        Map<Long, List<SubjectDO>> parentChildMap = allSubjects.stream()
                .filter(subject -> subject.getParentId() != 0) // 过滤出二级学科
                .collect(Collectors.groupingBy(SubjectDO::getParentId));
        
        // 获取一级学科并设置子学科
        List<SubjectRespVO> result = allSubjects.stream()
                .filter(subject -> subject.getParentId() == 0) // 一级学科
                .sorted((a, b) -> Integer.compare(a.getSort(), b.getSort()))
                .map(subject -> {
                    SubjectRespVO vo = SubjectConvert.INSTANCE.convert(subject);
                    // 设置子学科
                    List<SubjectDO> children = parentChildMap.get(subject.getId());
                    if (children != null) {
                        List<SubjectRespVO> childrenVOs = children.stream()
                                .sorted((a, b) -> Integer.compare(a.getSort(), b.getSort()))
                                .map(SubjectConvert.INSTANCE::convert)
                                .collect(Collectors.toList());
                        vo.setChildren(childrenVOs);
                    }
                    return vo;
                })
                .collect(Collectors.toList());
        
        return success(result);
    }

    @PostMapping("/manual")
    @Operation(summary = "保存手动输入的一级学科")
    @PermitAll
    public CommonResult<Boolean> saveManualSubjects(@Valid @RequestBody ManualSubjectsReqVO reqVO) {
        subjectService.saveManualSubjects(reqVO.getSubjects());
        return success(true);
    }

    // 请求VO类
    @Validated
    public static class ManualSubjectsReqVO {
        @javax.validation.constraints.NotEmpty(message = "学科列表不能为空")
        private List<String> subjects;

        public List<String> getSubjects() {
            return subjects;
        }

        public void setSubjects(List<String> subjects) {
            this.subjects = subjects;
        }
    }

} 