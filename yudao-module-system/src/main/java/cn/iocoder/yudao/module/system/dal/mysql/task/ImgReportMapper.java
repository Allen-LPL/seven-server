package cn.iocoder.yudao.module.system.dal.mysql.task;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.report.ReportPageReqVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.report.ReportPageRespVO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.ImgReportDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ImgReportMapper extends BaseMapperX<ImgReportDO> {

    /**
     * 分页查询报告列表（仅从报告表查询）
     * @param reqVO 查询条件
     * @return 分页结果
     */
//    default PageResult<ImgReportDO> selectPage(ReportPageReqVO reqVO) {
//        return selectPage(reqVO, new LambdaQueryWrapperX<ImgReportDO>()
//                .eqIfPresent(ImgReportDO::getTaskId, reqVO.getTaskId())
//                .eqIfPresent(ImgReportDO::getStatus, reqVO.getReportStatus())
//                .betweenIfPresent(ImgReportDO::getCreateTime, reqVO.getCreateTime())
//                .orderByDesc(ImgReportDO::getId));
//    }

    /**
     * 联合查询报告和任务信息（包含用户信息）
     * 关联 iisd_img_report、iisd_img_task、system_users 三张表
     * 获取报告基本信息、任务详情、用户姓名和单位信息
     * 
     * @param reqVO 查询条件，支持按任务ID、任务类型、报告状态、创建时间筛选
     * @return 分页的报告详情列表
     */
    @Select("<script>" +
            "SELECT " +
            "    r.id, r.task_id, r.report_name, r.report_path, r.status, r.create_time, " +
            "    t.file_type, t.task_type, t.review_result, t.total_images, t.similar_images, " +
            // "    t.article_title_map, t.article_journal_map, t.author_name_map, t.first_image, " +
            "    creator.nickname as user_name, " +
            "    creator_dept.name as user_unit, " +
            "    reviewer.nickname as review_user_name, " +
            "    reviewer_dept.name as review_user_unit " +
            "FROM iisd_img_report r " +
            "    INNER JOIN iisd_img_task t ON r.task_id = t.id " +
            "    LEFT JOIN system_users creator ON t.creator_id = creator.id " +
            "    LEFT JOIN system_dept creator_dept ON creator.dept_id = creator_dept.id " +
            "    LEFT JOIN system_users reviewer ON t.reviewer_id = reviewer.id " +
            "    LEFT JOIN system_dept reviewer_dept ON reviewer.dept_id = reviewer_dept.id " +
            "<where>" +
            "    t.deleted = 0 AND r.deleted = 0 " +
            "    <if test='reqVO.taskId != null'>" +
            "        AND r.task_id = #{reqVO.taskId}" +
            "    </if>" +
            "    <if test='reqVO.reportStatus != null'>" +
            "        AND t.review_result = #{reqVO.reportStatus}" +
            "    </if>" +
            "    <if test='reqVO.taskType != null'>" +
            "        AND t.task_type = #{reqVO.taskType}" +
            "    </if>" +
            "    <if test='reqVO.createTime != null and reqVO.createTime.length == 2'>" +
            "        AND r.create_time BETWEEN #{reqVO.createTime[0]} AND #{reqVO.createTime[1]}" +
            "    </if>" +
            "</where>" +
            "ORDER BY r.id DESC" +
            "</script>")
    List<ReportPageRespVO> selectReportAndTaskPage(@Param("reqVO") ReportPageReqVO reqVO);

    @Select("<script>" +
            "SELECT COUNT(*) " +
           "FROM iisd_img_report r " +
            "    INNER JOIN iisd_img_task t ON r.task_id = t.id " +
            "    LEFT JOIN system_users creator ON t.creator_id = creator.id " +
            "    LEFT JOIN system_dept creator_dept ON creator.dept_id = creator_dept.id " +
            "    LEFT JOIN system_users reviewer ON t.reviewer_id = reviewer.id " +
            "    LEFT JOIN system_dept reviewer_dept ON reviewer.dept_id = reviewer_dept.id " +
            "<where>" +
            "    t.deleted = 0 AND r.deleted = 0 " +
            "    <if test='reqVO.taskId != null'>" +
            "        AND r.task_id = #{reqVO.taskId}" +
            "    </if>" +
            "    <if test='reqVO.reportStatus != null'>" +
            "        AND t.review_result = #{reqVO.reportStatus}" +
            "    </if>" +
            "    <if test='reqVO.taskType != null'>" +
            "        AND t.task_type = #{reqVO.taskType}" +
            "    </if>" +
            "    <if test='reqVO.createTime != null and reqVO.createTime.length == 2'>" +
            "        AND r.create_time BETWEEN #{reqVO.createTime[0]} AND #{reqVO.createTime[1]}" +
            "    </if>" +
            "</where>" +
            "</script>")
    Long selectCounts(@Param("reqVO") ReportPageReqVO reqVO);
}
