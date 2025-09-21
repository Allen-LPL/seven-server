package cn.iocoder.yudao.module.system.dal.mysql.statistics;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 统计 Mapper 接口
 */
@Mapper
public interface StatisticsMapper {

    /**
     * 统计论文数量
     *
     * @param startTime 开始时间
     * @return 论文数量
     */
    @Select("<script>"
            + "SELECT COUNT(DISTINCT file_name) FROM iisd_article "
            + "WHERE deleted = 0 "
            + "<if test='startTime != null'> AND create_time >= #{startTime} </if>"
            + "</script>")
    Long countArticles(@Param("startTime") LocalDateTime startTime);

    /**
     * 统计大图数量
     *
     * @param startTime 开始时间
     * @return 大图数量
     */
    @Select("<script>"
            + "SELECT COUNT(*) FROM iisd_large_image "
            + "WHERE deleted = 0 "
            + "<if test='startTime != null'> AND create_time >= #{startTime} </if>"
            + "</script>")
    Long countLargeImages(@Param("startTime") LocalDateTime startTime);

    /**
     * 统计小图数量
     *
     * @param startTime 开始时间
     * @return 小图数量
     */
    @Select("<script>"
            + "SELECT COUNT(*) FROM iisd_small_image "
            + "WHERE deleted = 0 "
            + "<if test='startTime != null'> AND create_time >= #{startTime} </if>"
            + "</script>")
    Long countSmallImages(@Param("startTime") LocalDateTime startTime);

    /**
     * 根据用户类型统计用户数量
     *
     * @param userTypes 用户类型列表
     * @param startTime 开始时间
     * @return 用户数量
     */
    @Select("<script>"
            + "SELECT COUNT(*) FROM system_users "
            + "WHERE deleted = 0 "
            + "<if test='userTypes != null and userTypes.size() > 0'> AND user_type IN "
            + "<foreach collection='userTypes' item='type' open='(' separator=',' close=')'> #{type} </foreach> </if>"
            + "<if test='startTime != null'> AND create_time >= #{startTime} </if>"
            + "</script>")
    Long countUsersByType(@Param("userTypes") List<String> userTypes, @Param("startTime") LocalDateTime startTime);

    /**
     * 统计用户数量
     *
     * @param startTime 开始时间
     * @return 用户数量
     */
    @Select("<script>"
            + "SELECT COUNT(*) FROM system_users "
            + "WHERE deleted = 0 "
            + "<if test='startTime != null'> AND create_time >= #{startTime} </if>"
            + "</script>")
    Long countUsers(@Param("startTime") LocalDateTime startTime);

    /**
     * 统计部门数量
     *
     * @param startTime 开始时间
     * @return 部门数量
     */
    @Select("<script>"
            + "SELECT COUNT(*) FROM system_dept "
            + "WHERE deleted = 0 "
            + "<if test='startTime != null'> AND create_time >= #{startTime} </if>"
            + "</script>")
    Long countDepts(@Param("startTime") LocalDateTime startTime);

    /**
     * 统计异常检测数量
     *
     * @param startTime 开始时间
     * @return 异常检测数量
     */
    @Select("<script>"
            + "SELECT COUNT(DISTINCT a.file_name) FROM iisd_img_similarity s "
            + "LEFT JOIN iisd_article a ON s.source_article_id = a.id "
            + "WHERE s.deleted = 0 "
            + "<if test='startTime != null'> AND s.create_time >= #{startTime} </if>"
            + "</script>")
    Long countAbnormal(@Param("startTime") LocalDateTime startTime);

    /**
     * 获取异常趋势数据
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 日期-数量映射
     */
    @Select("<script>"
            + "SELECT DATE_FORMAT(s.create_time, '%Y-%m-%d') as date, COUNT(DISTINCT a.file_name) as count "
            + "FROM iisd_img_similarity s "
            + "LEFT JOIN iisd_article a ON s.source_article_id = a.id "
            + "WHERE s.deleted = 0 "
            + "<if test='startTime != null'> AND s.create_time >= #{startTime} </if>"
            + "<if test='endTime != null'> AND s.create_time &lt;= #{endTime} </if>"
            + "GROUP BY DATE_FORMAT(s.create_time, '%Y-%m-%d') "
            + "ORDER BY date ASC"
            + "</script>")
    List<Map<String, Object>> getAbnormalTrendList(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 获取异常领域分布数据（按文章关键词 article_keywords 统计）
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 关键词-数量映射（field: 关键词, count: 数量）
     */
    @Select("<script>"
            + "SELECT IFNULL(jt.keyword, '未知') as field, COUNT(DISTINCT a.file_name) as count "
            + "FROM iisd_img_similarity s "
            + "LEFT JOIN iisd_article a ON s.source_article_id = a.id "
            + "LEFT JOIN JSON_TABLE(COALESCE(a.article_keywords, JSON_ARRAY()), '$[*]' COLUMNS (keyword VARCHAR(255) PATH '$')) jt ON TRUE "
            + "WHERE s.deleted = 0 "
            + "AND (jt.keyword IS NOT NULL AND jt.keyword != '' "
            + "AND LOWER(jt.keyword) != 'and' AND LOWER(jt.keyword) != 'or') "
            + "<if test='startTime != null'> AND s.create_time >= #{startTime} </if>"
            + "<if test='endTime != null'> AND s.create_time &lt;= #{endTime} </if>"
            + "GROUP BY field "
            + "ORDER BY count DESC "
            + "LIMIT 10"
            + "</script>")
    @InterceptorIgnore(tenantLine = "true")
    List<Map<String, Object>> getFieldDistributionList(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 获取异常单位分布数据（按文章作者单位 author_institution 统计）
     *
     * @return 单位名称-数量映射（institution: 单位名称, count: 数量）
     */
    @Select("<script>"
            + "SELECT jt.institution as institution, COUNT(DISTINCT a.file_name) as count "
            + "FROM iisd_img_similarity s "
            + "LEFT JOIN iisd_article a ON s.source_article_id = a.id "
            + "LEFT JOIN JSON_TABLE(COALESCE(a.author_institution, JSON_ARRAY()), '$[*]' COLUMNS (institution VARCHAR(512) PATH '$')) jt ON TRUE "
            + "WHERE s.deleted = 0 AND jt.institution IS NOT NULL AND jt.institution != '' "
            + "GROUP BY jt.institution "
            + "ORDER BY count DESC "
            + "LIMIT 10"
            + "</script>")
    @InterceptorIgnore(tenantLine = "true")
    List<Map<String, Object>> getUnitDistributionList();

    /**
     * 获取专家审核分布数据
     *
     * @return 专家ID-数量列表
     */
    @Select("SELECT reviewer_id as reviewerId, COUNT(*) as count "
            + "FROM iisd_img_task "
            + "WHERE deleted = 0 AND reviewer_id IS NOT NULL "
            + "GROUP BY reviewer_id "
            + "ORDER BY count DESC")
    List<Map<String, Object>> getReviewerDistributionList();

    /**
     * 根据部门ID获取部门名称
     *
     * @param deptId 部门ID
     * @return 部门名称
     */
    @Select("SELECT name FROM system_dept WHERE id = #{deptId} AND deleted = 0")
    String getDeptNameById(@Param("deptId") Long deptId);

    /**
     * 根据用户ID获取用户名
     *
     * @param userId 用户ID
     * @return 用户名
     */
    @Select("SELECT username FROM system_users WHERE id = #{userId} AND deleted = 0")
    String getUsernameById(@Param("userId") Long userId);

    /**
     * 判断用户是否为系统管理员
     *
     * @param userId 用户ID
     * @return 是否为系统管理员
     */
    @Select("SELECT COUNT(*) > 0 FROM system_users u "
            + "JOIN system_user_role ur ON u.id = ur.user_id "
            + "JOIN system_role r ON ur.role_id = r.id "
            + "WHERE u.id = #{userId} AND r.code = 'super_admin' AND u.deleted = 0 AND r.deleted = 0")
    boolean isAdminUser(@Param("userId") Long userId);
}
