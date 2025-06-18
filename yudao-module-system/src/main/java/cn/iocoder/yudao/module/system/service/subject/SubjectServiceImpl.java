package cn.iocoder.yudao.module.system.service.subject;

import cn.iocoder.yudao.module.system.dal.dataobject.subject.SubjectDO;
import cn.iocoder.yudao.module.system.dal.mysql.subject.SubjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 学科 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Slf4j
public class SubjectServiceImpl implements SubjectService {

    @Resource
    private SubjectMapper subjectMapper;

    @Override
    public List<SubjectDO> getFirstLevelSubjects() {
        return subjectMapper.selectListByParentId(0L)
                .stream()
                .filter(subject -> subject.getStatus() == 1) // 只返回启用状态
                .sorted((a, b) -> Integer.compare(a.getSort(), b.getSort())) // 按排序排列
                .collect(Collectors.toList());
    }

    @Override
    public List<SubjectDO> getSecondLevelSubjects(Long parentId) {
        return subjectMapper.selectListByParentId(parentId)
                .stream()
                .filter(subject -> subject.getStatus() == 1) // 只返回启用状态
                .sorted((a, b) -> Integer.compare(a.getSort(), b.getSort())) // 按排序排列
                .collect(Collectors.toList());
    }

    @Override
    public List<SubjectDO> getSubjectTree() {
        // 获取所有启用状态的学科
        List<SubjectDO> allSubjects = getAllEnabledSubjects();
        
        // 构建树形结构
        Map<Long, List<SubjectDO>> parentChildMap = allSubjects.stream()
                .filter(subject -> subject.getParentId() != 0) // 过滤出二级学科
                .collect(Collectors.groupingBy(SubjectDO::getParentId));
        
        // 获取一级学科并设置子学科
        List<SubjectDO> result = allSubjects.stream()
                .filter(subject -> subject.getParentId() == 0) // 一级学科
                .sorted((a, b) -> Integer.compare(a.getSort(), b.getSort()))
                .collect(Collectors.toList());
        
        // 为每个一级学科设置子学科（这里我们不在DO中添加children字段，只在VO中使用）
        return result;
    }

    @Override
    public SubjectDO getSubject(Long id) {
        return subjectMapper.selectById(id);
    }

    @Override
    public List<SubjectDO> getAllEnabledSubjects() {
        return subjectMapper.selectListByStatus(1)
                .stream()
                .sorted((a, b) -> Integer.compare(a.getSort(), b.getSort()))
                .collect(Collectors.toList());
    }

    @Override
    public void saveManualSubjects(List<String> subjectNames) {
        if (CollectionUtils.isEmpty(subjectNames)) {
            return;
        }
        
        // 获取现有的一级学科名称，避免重复插入
        List<SubjectDO> existingSubjects = getFirstLevelSubjects();
        List<String> existingNames = existingSubjects.stream()
                .map(SubjectDO::getName)
                .collect(Collectors.toList());
        
        // 过滤出不存在的学科名称
        List<String> newSubjectNames = subjectNames.stream()
                .filter(name -> !existingNames.contains(name))
                .distinct() // 去重
                .collect(Collectors.toList());
        
        if (CollectionUtils.isEmpty(newSubjectNames)) {
            log.info("所有手动输入的学科都已存在，无需插入");
            return;
        }
        
        // 获取当前一级学科的最大排序值
        int maxSort = existingSubjects.stream()
                .mapToInt(SubjectDO::getSort)
                .max()
                .orElse(0);
        
        // 批量插入新的一级学科
        for (int i = 0; i < newSubjectNames.size(); i++) {
            String subjectName = newSubjectNames.get(i);
            SubjectDO newSubject = new SubjectDO();
    
            newSubject.setName(subjectName);
            newSubject.setParentId(0L); // 一级学科的父ID为0
            newSubject.setLevel(1); // 一级学科
            newSubject.setSort(maxSort + i + 1); // 排序值递增
            newSubject.setStatus(1); // 启用状态
            newSubject.setCreateTime(LocalDateTime.now());
            newSubject.setUpdateTime(LocalDateTime.now());
            subjectMapper.insert(newSubject);
            log.info("成功插入手动的一级学科: {}", subjectName);
           
            SubjectDO newSubject2 = new SubjectDO();
    
            newSubject2.setName(subjectName);
            newSubject2.setParentId(newSubject.getId()); // 一级学科的父ID为0
            newSubject2.setLevel(2); // 一级学科
            newSubject2.setSort(maxSort + i + 1); // 排序值递增
            newSubject2.setStatus(1); // 启用状态
            newSubject2.setCreateTime(LocalDateTime.now());
            newSubject2.setUpdateTime(LocalDateTime.now());
            subjectMapper.insert(newSubject2);
            log.info("成功插入手动的二级学科: {}", subjectName);
        }
        
        log.info("成功保存 {} 个手动输入的一级学科和二级学科", newSubjectNames.size());
    }

} 