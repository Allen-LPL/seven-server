package cn.iocoder.yudao.module.system.dal.mapper.task;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.system.dal.dataobject.task.TaskSearchPreferencesDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskSearchPreferencesMapper extends BaseMapperX<TaskSearchPreferencesDO> {

    default TaskSearchPreferencesDO selectByTaskId(Long taskId) {
        return selectOne(TaskSearchPreferencesDO::getTaskId, taskId);
    }

}
