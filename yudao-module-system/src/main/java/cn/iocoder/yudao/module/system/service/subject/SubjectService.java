package cn.iocoder.yudao.module.system.service.subject;

import cn.iocoder.yudao.module.system.dal.dataobject.subject.SubjectDO;

import java.util.List;

/**
 * 学科 Service 接口
 *
 * @author 芋道源码
 */
public interface SubjectService {

    /**
     * 获取一级学科列表
     *
     * @return 一级学科列表
     */
    List<SubjectDO> getFirstLevelSubjects();

    /**
     * 根据父级ID获取二级学科列表
     *
     * @param parentId 父级ID
     * @return 二级学科列表
     */
    List<SubjectDO> getSecondLevelSubjects(Long parentId);

    /**
     * 获取学科树形结构
     *
     * @return 学科树形列表
     */
    List<SubjectDO> getSubjectTree();

    /**
     * 根据ID获取学科信息
     *
     * @param id 学科ID
     * @return 学科信息
     */
    SubjectDO getSubject(Long id);

    /**
     * 获取所有启用状态的学科
     *
     * @return 学科列表
     */
    List<SubjectDO> getAllEnabledSubjects();

    /**
     * 保存手动输入的一级学科
     *
     * @param subjectNames 学科名称列表
     */
    void saveManualSubjects(List<String> subjectNames);

} 