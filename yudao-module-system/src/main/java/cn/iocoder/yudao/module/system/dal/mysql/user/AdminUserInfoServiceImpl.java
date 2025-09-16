package cn.iocoder.yudao.module.system.dal.mysql.user;

import cn.iocoder.yudao.module.system.controller.admin.task.vo.adminUser.AdminUserVO;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AdminUserInfoServiceImpl {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 使用原生JDBC查询用户信息，完全绕过MyBatis和租户过滤
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    public AdminUserVO getUserById(Long userId) {
        String sql = "SELECT id, username, nickname, dept_id FROM system_users WHERE id = ? AND deleted = 0";
        try {
            return jdbcTemplate.queryForObject(sql, new BeanPropertyRowMapper<>(AdminUserVO.class), userId);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 批量查询用户信息，绕过 MyBatis 与多租户过滤
     * @param userIds 用户ID集合
     * @return 用户ID到用户信息的映射
     */
    public Map<Long, AdminUserVO> getUsersByIds(Collection<Long> userIds) {
        if (Objects.isNull(userIds) || userIds.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        String idList = userIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        String sql = "SELECT id, username, nickname, dept_id FROM system_users WHERE id IN (" + idList + ") AND deleted = 0";
        List<AdminUserVO> list = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(AdminUserVO.class));
        return list.stream().collect(Collectors.toMap(AdminUserVO::getId, v -> v, (a, b) -> a));
    }
}
