package cn.iocoder.yudao.module.system.dal.mysql.subject;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.system.dal.dataobject.subject.SubjectDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 学科 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface SubjectMapper extends BaseMapperX<SubjectDO> {

    /**
     * 根据父级ID查询学科列表
     *
     * @param parentId 父级ID
     * @return 学科列表
     */
    default List<SubjectDO> selectListByParentId(Long parentId) {
        return selectList(SubjectDO::getParentId, parentId);
    }

    /**
     * 根据层级查询学科列表
     *
     * @param level 层级
     * @return 学科列表
     */
    default List<SubjectDO> selectListByLevel(Integer level) {
        return selectList(SubjectDO::getLevel, level);
    }

    /**
     * 查询所有启用状态的学科
     *
     * @return 学科列表
     */
    default List<SubjectDO> selectListByStatus(Integer status) {
        return selectList(SubjectDO::getStatus, status);
    }

} 