package cn.iocoder.yudao.module.system.dal.mysql.task;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
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
            "    creator.nickname as user_name, " +
            "    creator_dept.name as user_unit, " +
            "    admin.nickname as review_user_name, " +
            "    admin_dept.name as review_user_unit " +
            "FROM ( " +
            "    SELECT * FROM ( " +
            "        SELECT r.*, ROW_NUMBER() OVER(PARTITION BY r.task_id ORDER BY r.create_time DESC, r.id DESC) AS rn " +
            "        FROM iisd_img_report r WHERE r.deleted = 0 " +
            "    ) r1 WHERE r1.rn = 1 " +
            ") r " +
            "    JOIN ( " +
            "        SELECT DISTINCT id FROM ( " +
            "            SELECT id FROM iisd_img_task <if test='creatorId != null'>WHERE deleted = 0 AND creator_id = #{creatorId}</if> " +
            "            <if test='creatorId != null'>" +
            "            <if test='reqVO.taskType != null'> AND task_type = #{reqVO.taskType}</if> " +
            "            <if test='reqVO.reportStatus != null'> AND review_result = #{reqVO.reportStatus}</if> " +
            "            </if> " +
            "            UNION ALL " +
            "            SELECT id FROM iisd_img_task <if test='creatorId != null'>WHERE deleted = 0 AND admin_id = #{creatorId}</if> " +
            "            <if test='creatorId != null'>" +
            "            <if test='reqVO.taskType != null'> AND task_type = #{reqVO.taskType}</if> " +
            "            <if test='reqVO.reportStatus != null'> AND review_result = #{reqVO.reportStatus}</if> " +
            "            </if> " +
            "        ) tu1 " +
            "    ) tu ON r.task_id = tu.id " +
            "    JOIN iisd_img_task t ON r.task_id = t.id " +
            "    LEFT JOIN system_users creator ON t.creator_id = creator.id " +
            "    LEFT JOIN system_dept creator_dept ON creator.dept_id = creator_dept.id " +
            "    LEFT JOIN system_users admin ON t.admin_id = admin.id " +
            "    LEFT JOIN system_dept admin_dept ON admin.dept_id = admin_dept.id " +
            "<where>" +
            "    t.deleted = 0 AND r.deleted = 0 " +
            "    <if test='reqVO.taskId != null'>" +
            "        AND r.task_id = #{reqVO.taskId}" +
            "    </if>" +
            "    <if test='reqVO.createTime != null and reqVO.createTime.length == 2'>" +
            "        AND r.create_time BETWEEN #{reqVO.createTime[0]} AND #{reqVO.createTime[1]}" +
            "    </if>" +
            "</where>" +
            "ORDER BY r.id DESC " +
            "LIMIT #{reqVO.pageNo}, #{reqVO.pageSize}" +
            "</script>")
    List<ReportPageRespVO> selectReportAndTaskPage(@Param("reqVO") ReportPageReqVO reqVO, @Param("creatorId") Long creatorId);

    @Select("<script>" +
            "SELECT COUNT(DISTINCT r.id) " +
            "FROM ( " +
            "    SELECT * FROM ( " +
            "        SELECT r.*, ROW_NUMBER() OVER(PARTITION BY r.task_id ORDER BY r.create_time DESC, r.id DESC) AS rn " +
            "        FROM iisd_img_report r WHERE r.deleted = 0 " +
            "    ) r1 WHERE r1.rn = 1 " +
            ") r " +
            "    JOIN ( " +
            "        SELECT DISTINCT id FROM ( " +
            "            SELECT id FROM iisd_img_task <if test='creatorId != null'>WHERE deleted = 0 AND creator_id = #{creatorId}</if> " +
            "            <if test='creatorId != null'>" +
            "            <if test='reqVO.taskType != null'> AND task_type = #{reqVO.taskType}</if> " +
            "            <if test='reqVO.reportStatus != null'> AND review_result = #{reqVO.reportStatus}</if> " +
            "            </if> " +
            "            UNION ALL " +
            "            SELECT id FROM iisd_img_task <if test='creatorId != null'>WHERE deleted = 0 AND admin_id = #{creatorId}</if> " +
            "            <if test='creatorId != null'>" +
            "            <if test='reqVO.taskType != null'> AND task_type = #{reqVO.taskType}</if> " +
            "            <if test='reqVO.reportStatus != null'> AND review_result = #{reqVO.reportStatus}</if> " +
            "            </if> " +
            "        ) tu1 " +
            "    ) tu ON r.task_id = tu.id " +
            "    JOIN iisd_img_task t ON r.task_id = t.id " +
            "<where>" +
            "    t.deleted = 0 AND r.deleted = 0 " +
            "    <if test='reqVO.taskId != null'>" +
            "        AND r.task_id = #{reqVO.taskId}" +
            "    </if>" +
            "    <if test='reqVO.createTime != null and reqVO.createTime.length == 2'>" +
            "        AND r.create_time BETWEEN #{reqVO.createTime[0]} AND #{reqVO.createTime[1]}" +
            "    </if>" +
            "</where>" +
            "</script>")
    Long selectCounts(@Param("reqVO") ReportPageReqVO reqVO, @Param("creatorId") Long creatorId);

    /**
     * 管理人员查看报告列表（不限制创建人/分配人），支持条件筛选
     * - 可按任务类型、报告状态（任务 review_result）、创建时间筛选
     */
    @Select("<script>" +
            "SELECT " +
            "    r.id, r.task_id, r.report_name, r.report_path, r.status, r.create_time, " +
            "    t.file_type, t.task_type, t.review_result, t.total_images, t.similar_images, " +
            "    creator.nickname as user_name, " +
            "    creator_dept.name as user_unit, " +
            "    admin.nickname as review_user_name, " +
            "    admin_dept.name as review_user_unit " +
            "FROM ( " +
            "    SELECT * FROM ( " +
            "        SELECT r.*, ROW_NUMBER() OVER(PARTITION BY r.task_id ORDER BY r.create_time DESC, r.id DESC) AS rn " +
            "        FROM iisd_img_report r WHERE r.deleted = 0 " +
            "    ) r1 WHERE r1.rn = 1 " +
            ") r " +
            "    JOIN iisd_img_task t ON r.task_id = t.id " +
            "    LEFT JOIN system_users creator ON t.creator_id = creator.id " +
            "    LEFT JOIN system_dept creator_dept ON creator.dept_id = creator_dept.id " +
            "    LEFT JOIN system_users admin ON t.admin_id = admin.id " +
            "    LEFT JOIN system_dept admin_dept ON admin.dept_id = admin_dept.id " +
            "<where>" +
            "    t.deleted = 0 AND r.deleted = 0 " +
            "    <if test='reqVO.taskId != null'>" +
            "        AND r.task_id = #{reqVO.taskId}" +
            "    </if>" +
            "    <if test='reqVO.taskType != null'>" +
            "        AND t.task_type = #{reqVO.taskType}" +
            "    </if>" +
            "    <if test='reqVO.reportStatus != null'>" +
            "        AND t.review_result = #{reqVO.reportStatus}" +
            "    </if>" +
            "    <if test='reqVO.createTime != null and reqVO.createTime.length == 2'>" +
            "        AND r.create_time BETWEEN #{reqVO.createTime[0]} AND #{reqVO.createTime[1]}" +
            "    </if>" +
            "</where>" +
            "ORDER BY r.id DESC " +
            "LIMIT #{reqVO.pageNo}, #{reqVO.pageSize}" +
            "</script>")
    List<ReportPageRespVO> selectManagerReportAndTaskPage(@Param("reqVO") ReportPageReqVO reqVO);

    @Select("<script>" +
            "SELECT COUNT(DISTINCT r.id) " +
            "FROM ( " +
            "    SELECT * FROM ( " +
            "        SELECT r.*, ROW_NUMBER() OVER(PARTITION BY r.task_id ORDER BY r.create_time DESC, r.id DESC) AS rn " +
            "        FROM iisd_img_report r WHERE r.deleted = 0 " +
            "    ) r1 WHERE r1.rn = 1 " +
            ") r " +
            "    JOIN iisd_img_task t ON r.task_id = t.id " +
            "<where>" +
            "    t.deleted = 0 AND r.deleted = 0 " +
            "    <if test='reqVO.taskId != null'>" +
            "        AND r.task_id = #{reqVO.taskId}" +
            "    </if>" +
            "    <if test='reqVO.taskType != null'>" +
            "        AND t.task_type = #{reqVO.taskType}" +
            "    </if>" +
            "    <if test='reqVO.reportStatus != null'>" +
            "        AND t.review_result = #{reqVO.reportStatus}" +
            "    </if>" +
            "    <if test='reqVO.createTime != null and reqVO.createTime.length == 2'>" +
            "        AND r.create_time BETWEEN #{reqVO.createTime[0]} AND #{reqVO.createTime[1]}" +
            "    </if>" +
            "</where>" +
            "</script>")
    Long selectManagerCounts(@Param("reqVO") ReportPageReqVO reqVO);

    /**
     * 按创建人查询报告分页
     */
    @Select("<script>" +
            "SELECT " +
            "    r.id, r.task_id, r.report_name, r.report_path, r.status, r.create_time, " +
            "    t.file_type, t.task_type, t.review_result, t.total_images, t.similar_images, " +
            "    creator.nickname as user_name, " +
            "    creator_dept.name as user_unit, " +
            "    admin.nickname as review_user_name, " +
            "    admin_dept.name as review_user_unit " +
            "FROM ( " +
            "    SELECT * FROM ( " +
            "        SELECT r.*, ROW_NUMBER() OVER(PARTITION BY r.task_id ORDER BY r.create_time DESC, r.id DESC) AS rn " +
            "        FROM iisd_img_report r WHERE r.deleted = 0 " +
            "    ) r1 WHERE r1.rn = 1 " +
            ") r " +
            "    JOIN iisd_img_task t ON r.task_id = t.id " +
            "    LEFT JOIN system_users creator ON t.creator_id = creator.id " +
            "    LEFT JOIN system_dept creator_dept ON creator.dept_id = creator_dept.id " +
            "    LEFT JOIN system_users admin ON t.admin_id = admin.id " +
            "    LEFT JOIN system_dept admin_dept ON admin.dept_id = admin_dept.id " +
            "<where>" +
            "    t.deleted = 0 AND r.deleted = 0 " +
            "    AND t.creator_id = #{creatorId} " +
            "    <if test='reqVO.taskId != null'>" +
            "        AND r.task_id = #{reqVO.taskId}" +
            "    </if>" +
            "    <if test='reqVO.taskType != null'>" +
            "        AND t.task_type = #{reqVO.taskType}" +
            "    </if>" +
            "    <if test='reqVO.reportStatus != null'>" +
            "        AND t.review_result = #{reqVO.reportStatus}" +
            "    </if>" +
            "    <if test='reqVO.createTime != null and reqVO.createTime.length == 2'>" +
            "        AND r.create_time BETWEEN #{reqVO.createTime[0]} AND #{reqVO.createTime[1]}" +
            "    </if>" +
            "</where>" +
            "ORDER BY r.id DESC " +
            "LIMIT #{reqVO.pageNo}, #{reqVO.pageSize}" +
            "</script>")
    List<ReportPageRespVO> selectCreatorReportAndTaskPage(@Param("reqVO") ReportPageReqVO reqVO, @Param("creatorId") Long creatorId);

    @Select("<script>" +
            "SELECT COUNT(DISTINCT r.id) " +
            "FROM ( " +
            "    SELECT * FROM ( " +
            "        SELECT r.*, ROW_NUMBER() OVER(PARTITION BY r.task_id ORDER BY r.create_time DESC, r.id DESC) AS rn " +
            "        FROM iisd_img_report r WHERE r.deleted = 0 " +
            "    ) r1 WHERE r1.rn = 1 " +
            ") r " +
            "    JOIN iisd_img_task t ON r.task_id = t.id " +
            "<where>" +
            "    t.deleted = 0 AND r.deleted = 0 " +
            "    AND t.creator_id = #{creatorId} " +
            "    <if test='reqVO.taskId != null'>" +
            "        AND r.task_id = #{reqVO.taskId}" +
            "    </if>" +
            "    <if test='reqVO.taskType != null'>" +
            "        AND t.task_type = #{reqVO.taskType}" +
            "    </if>" +
            "    <if test='reqVO.reportStatus != null'>" +
            "        AND t.review_result = #{reqVO.reportStatus}" +
            "    </if>" +
            "    <if test='reqVO.createTime != null and reqVO.createTime.length == 2'>" +
            "        AND r.create_time BETWEEN #{reqVO.createTime[0]} AND #{reqVO.createTime[1]}" +
            "    </if>" +
            "</where>" +
            "</script>")
    Long selectCreatorCounts(@Param("reqVO") ReportPageReqVO reqVO, @Param("creatorId") Long creatorId);

    /**
     * 按审核专家（reviewer）查询报告分页
     */
    @Select("<script>" +
            "SELECT " +
            "    r.id, r.task_id, r.report_name, r.report_path, r.status, r.create_time, " +
            "    t.file_type, t.task_type, t.review_result, t.total_images, t.similar_images, " +
            "    creator.nickname as user_name, " +
            "    creator_dept.name as user_unit, " +
            "    admin.nickname as review_user_name, " +
            "    admin_dept.name as review_user_unit " +
            "FROM ( " +
            "    SELECT * FROM ( " +
            "        SELECT r.*, ROW_NUMBER() OVER(PARTITION BY r.task_id ORDER BY r.create_time DESC, r.id DESC) AS rn " +
            "        FROM iisd_img_report r WHERE r.deleted = 0 " +
            "    ) r1 WHERE r1.rn = 1 " +
            ") r " +
            "    JOIN iisd_img_task t ON r.task_id = t.id " +
            "    LEFT JOIN system_users creator ON t.creator_id = creator.id " +
            "    LEFT JOIN system_dept creator_dept ON creator.dept_id = creator_dept.id " +
            "    LEFT JOIN system_users admin ON t.admin_id = admin.id " +
            "    LEFT JOIN system_dept admin_dept ON admin.dept_id = admin_dept.id " +
            "<where>" +
            "    t.deleted = 0 AND r.deleted = 0 " +
            "    AND t.reviewer_id = #{reviewerId} " +
            "    <if test='reqVO.taskId != null'>" +
            "        AND r.task_id = #{reqVO.taskId}" +
            "    </if>" +
            "    <if test='reqVO.taskType != null'>" +
            "        AND t.task_type = #{reqVO.taskType}" +
            "    </if>" +
            "    <if test='reqVO.reportStatus != null'>" +
            "        AND t.review_result = #{reqVO.reportStatus}" +
            "    </if>" +
            "    <if test='reqVO.createTime != null and reqVO.createTime.length == 2'>" +
            "        AND r.create_time BETWEEN #{reqVO.createTime[0]} AND #{reqVO.createTime[1]}" +
            "    </if>" +
            "</where>" +
            "ORDER BY r.id DESC " +
            "LIMIT #{reqVO.pageNo}, #{reqVO.pageSize}" +
            "</script>")
    List<ReportPageRespVO> selectReviewerReportAndTaskPage(@Param("reqVO") ReportPageReqVO reqVO, @Param("reviewerId") Long reviewerId);

    @Select("<script>" +
            "SELECT COUNT(DISTINCT r.id) " +
            "FROM ( " +
            "    SELECT * FROM ( " +
            "        SELECT r.*, ROW_NUMBER() OVER(PARTITION BY r.task_id ORDER BY r.create_time DESC, r.id DESC) AS rn " +
            "        FROM iisd_img_report r WHERE r.deleted = 0 " +
            "    ) r1 WHERE r1.rn = 1 " +
            ") r " +
            "    JOIN iisd_img_task t ON r.task_id = t.id " +
            "<where>" +
            "    t.deleted = 0 AND r.deleted = 0 " +
            "    AND t.reviewer_id = #{reviewerId} " +
            "    <if test='reqVO.taskId != null'>" +
            "        AND r.task_id = #{reqVO.taskId}" +
            "    </if>" +
            "    <if test='reqVO.taskType != null'>" +
            "        AND t.task_type = #{reqVO.taskType}" +
            "    </if>" +
            "    <if test='reqVO.reportStatus != null'>" +
            "        AND t.review_result = #{reqVO.reportStatus}" +
            "    </if>" +
            "    <if test='reqVO.createTime != null and reqVO.createTime.length == 2'>" +
            "        AND r.create_time BETWEEN #{reqVO.createTime[0]} AND #{reqVO.createTime[1]}" +
            "    </if>" +
            "</where>" +
            "</script>")
    Long selectReviewerCounts(@Param("reqVO") ReportPageReqVO reqVO, @Param("reviewerId") Long reviewerId);
}
