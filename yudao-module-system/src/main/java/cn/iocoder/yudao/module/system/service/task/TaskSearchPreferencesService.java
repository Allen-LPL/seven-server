package cn.iocoder.yudao.module.system.service.task;

import cn.iocoder.yudao.module.system.controller.admin.task.vo.similar.ImgSimilarityQueryReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.TaskSearchPreferencesDO;
import cn.iocoder.yudao.module.system.dal.mapper.task.TaskSearchPreferencesMapper;
import com.alibaba.fastjson.JSON;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.Objects;

@Service
public class TaskSearchPreferencesService {

    @Resource
    private TaskSearchPreferencesMapper taskSearchPreferencesMapper;

    public void saveSearchPreferences(ImgSimilarityQueryReqVO reqVO) {
        TaskSearchPreferencesDO preferences = taskSearchPreferencesMapper.selectByTaskId(reqVO.getTaskId());
        if (Objects.isNull(preferences)) {
            preferences = new TaskSearchPreferencesDO();
            preferences.setTaskId(reqVO.getTaskId());
        }

        preferences.setModelName(String.join(",", reqVO.getModelNameList()));
        if (reqVO.getImageTypeList() != null) {
            preferences.setImageTypes(JSON.toJSONString(reqVO.getImageTypeList()));
        } else {
            preferences.setImageTypes(null);
        }
        preferences.setFeaturePoints(JSON.toJSONString(reqVO.getFeaturePoints()));
        preferences.setSimilarScoreThreshold(reqVO.getSimilarScoreThreshold());
        if (Objects.isNull(preferences.getId())) {
            taskSearchPreferencesMapper.insert(preferences);
        } else {
            taskSearchPreferencesMapper.updateById(preferences);
        }
    }

    public TaskSearchPreferencesDO getSearchPreferences(Long taskId) {
        return taskSearchPreferencesMapper.selectByTaskId(taskId);
    }
}
