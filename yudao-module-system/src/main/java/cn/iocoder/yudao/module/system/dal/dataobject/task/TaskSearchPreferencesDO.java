package cn.iocoder.yudao.module.system.dal.dataobject.task;

import lombok.*;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 任务搜索参数 DO
 *
 * @author 芋道源码
 */
@TableName("system_task_search_preferences")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskSearchPreferencesDO extends BaseDO {

    /**
     * 自增ID
     */
    @TableId
    private Long id;
    /**
     * 任务ID
     */
    private Long taskId;
    /**
     * 模型名称
     */
    private String modelName;
    /**
     * 图像类型列表
     */
    private String imageTypes;
    /**
     * 特征点数量
     */
    private Integer featurePoints;
    /**
     * 相似度阈值
     */
    private Double similarScoreThreshold;

}
