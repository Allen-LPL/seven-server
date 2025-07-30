package cn.iocoder.yudao.module.system.dal.mysql.user;

import cn.iocoder.yudao.module.system.controller.admin.task.vo.adminUser.AdminUserVO;
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
        String sql = "SELECT * FROM system_users WHERE id = ? AND deleted = 0";
        try {
            return jdbcTemplate.queryForObject(sql, new BeanPropertyRowMapper<>(AdminUserVO.class), userId);
        } catch (Exception e) {
            return null;
        }
    }
} 