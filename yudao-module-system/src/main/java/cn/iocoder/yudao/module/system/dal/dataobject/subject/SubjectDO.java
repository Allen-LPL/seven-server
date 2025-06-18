package cn.iocoder.yudao.module.system.dal.dataobject.subject;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 学科 DO
 *
 * @author 芋道源码
 */
@TableName("system_subject")
@Data
@EqualsAndHashCode(callSuper = true)
public class SubjectDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 学科名称
     */
    private String name;

    /**
     * 父级ID，0表示一级学科
     */
    private Long parentId;

    /**
     * 层级：1-一级学科，2-二级学科
     */
    private Integer level;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 状态：1-启用，0-禁用
     */
    private Integer status;

} 