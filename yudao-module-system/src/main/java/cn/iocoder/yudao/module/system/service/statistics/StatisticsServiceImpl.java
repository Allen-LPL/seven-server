package cn.iocoder.yudao.module.system.service.statistics;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.module.system.controller.admin.statistics.vo.StatisticsDataRespVO;
import cn.iocoder.yudao.module.system.controller.admin.statistics.vo.StatisticsRangeReqVO;
import cn.iocoder.yudao.module.system.controller.admin.statistics.vo.StatisticsTrendRespVO;
import cn.iocoder.yudao.module.system.controller.admin.statistics.vo.StatisticsUnitDistributionRespVO;
import cn.iocoder.yudao.module.system.dal.mysql.dept.DeptMapper;
import cn.iocoder.yudao.module.system.dal.mysql.statistics.StatisticsMapper;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 统计 Service 实现类
 */
@Service
@Validated
@Slf4j
public class StatisticsServiceImpl implements StatisticsService {

    @Resource
    private StatisticsMapper statisticsMapper;
    
    @Resource
    private DeptMapper deptMapper;
    
    @Resource
    private AdminUserMapper adminUserMapper;

    @Override
    public StatisticsDataRespVO getOverviewData() {
        StatisticsDataRespVO respVO = new StatisticsDataRespVO();
        
        // 设置论文数量统计
        respVO.setTotalArticles(statisticsMapper.countArticles(null));
        respVO.setNewArticles(statisticsMapper.countArticles(getLastMonthTime()));
        
        // 设置图片数量统计（大图+小图）
        Long largeImages = statisticsMapper.countLargeImages(null);
        Long smallImages = statisticsMapper.countSmallImages(null);
        respVO.setTotalImages(largeImages + smallImages);
        
        Long newLargeImages = statisticsMapper.countLargeImages(getLastMonthTime());
        Long newSmallImages = statisticsMapper.countSmallImages(getLastMonthTime());
        respVO.setNewImages(newLargeImages + newSmallImages);
        
        // 设置专家数量统计
        List<String> expertTypes = Arrays.asList("Expert_admin", "Research_admin");
        respVO.setTotalExperts(statisticsMapper.countUsersByType(expertTypes, null));
        respVO.setNewExperts(statisticsMapper.countUsersByType(expertTypes, getLastMonthTime()));
        
        // 设置用户数量统计
        respVO.setTotalUsers(statisticsMapper.countUsers(null));
        respVO.setNewUsers(statisticsMapper.countUsers(getLastMonthTime()));
        
        // 设置单位数量统计
        respVO.setTotalUnits(statisticsMapper.countDepts(null));
        respVO.setNewUnits(statisticsMapper.countDepts(getLastMonthTime()));
        
        // 设置异常检测统计
        respVO.setTotalAbnormal(statisticsMapper.countAbnormal(null));
        respVO.setNewAbnormal(statisticsMapper.countAbnormal(getLastMonthTime()));
        
        return respVO;
    }

    @Override
    public StatisticsTrendRespVO getTrendData(StatisticsRangeReqVO reqVO) {
        StatisticsTrendRespVO respVO = new StatisticsTrendRespVO();
        
        // 设置默认时间范围，如果未指定则为最近30天
        LocalDateTime startTime = reqVO.getStartTime();
        LocalDateTime endTime = reqVO.getEndTime();
        if (startTime == null) {
            startTime = LocalDateTime.now().minusDays(30);
        }
        if (endTime == null) {
            endTime = LocalDateTime.now();
        }
        
        // 获取异常趋势数据
        StatisticsTrendRespVO.TrendData abnormalTrends = new StatisticsTrendRespVO.TrendData();
        List<Map<String, Object>> abnormalTrendList = statisticsMapper.getAbnormalTrendList(startTime, endTime);
        
        // 提取日期和数量
        List<String> dates = new ArrayList<>();
        List<Long> values = new ArrayList<>();
        
        for (Map<String, Object> item : abnormalTrendList) {
            String date = String.valueOf(item.get("date"));
            Long count = Long.valueOf(String.valueOf(item.get("count")));
            dates.add(date);
            values.add(count);
        }
        
        abnormalTrends.setDates(dates);
        abnormalTrends.setValues(values);
        respVO.setAbnormalTrends(abnormalTrends);
        
        // 计算统计数据
        if (!values.isEmpty()) {
            // 单日最高异常数
            Long maxAnomalyCount = values.stream().max(Long::compare).orElse(0L);
            respVO.setMaxAnomalyCount(maxAnomalyCount);
            
            // 单日最低异常数
            Long minAnomalyCount = values.stream().min(Long::compare).orElse(0L);
            respVO.setMinAnomalyCount(minAnomalyCount);
            
            // 总异常数
            Long totalAnomalyCount = values.stream().mapToLong(Long::longValue).sum();
            respVO.setTotalAnomalyCount(totalAnomalyCount);
            
            // 分析天数
            respVO.setDaysCount(values.size());
            
            // 日均异常数
            Long avgAnomalyCount = values.isEmpty() ? 0L : Math.round((double) totalAnomalyCount / values.size());
            respVO.setAvgAnomalyCount(avgAnomalyCount);
        } else {
            // 如果没有数据，设置默认值
            respVO.setMaxAnomalyCount(0L);
            respVO.setMinAnomalyCount(0L);
            respVO.setAvgAnomalyCount(0L);
            respVO.setTotalAnomalyCount(0L);
            respVO.setDaysCount(0);
        }
        
        // 获取异常领域分布数据
        StatisticsTrendRespVO.FieldDistributionData fieldDistribution = new StatisticsTrendRespVO.FieldDistributionData();
        List<Map<String, Object>> fieldDistributionList = statisticsMapper.getFieldDistributionList(startTime, endTime);
        
        // 提取领域和数量
        List<String> fields = new ArrayList<>();
        List<Long> fieldValues = new ArrayList<>();
        
        for (Map<String, Object> item : fieldDistributionList) {
            String field = String.valueOf(item.get("field"));
            Long count = Long.valueOf(String.valueOf(item.get("count")));
            fields.add(field);
            fieldValues.add(count);
        }
        
        fieldDistribution.setFields(fields);
        fieldDistribution.setValues(fieldValues);
        respVO.setFieldDistribution(fieldDistribution);
        
        return respVO;
    }

    @Override
    public List<StatisticsUnitDistributionRespVO> getUnitDistributionData(Long userId) {
        // 检查用户权限，只有系统管理员才能查看
        boolean isAdmin = statisticsMapper.isAdminUser(userId);
        if (!isAdmin) {
            return Collections.emptyList();
        }
        
        // 获取异常单位分布数据
        List<Map<String, Object>> unitDistributionList = statisticsMapper.getUnitDistributionList();
        if (CollUtil.isEmpty(unitDistributionList)) {
            return Collections.emptyList();
        }
        
        // 计算总数用于百分比计算
        long total = unitDistributionList.stream()
                .mapToLong(map -> Long.parseLong(String.valueOf(map.get("count"))))
                .sum();
        
        // 构建返回结果
        return unitDistributionList.stream()
                .map(map -> {
                    StatisticsUnitDistributionRespVO vo = new StatisticsUnitDistributionRespVO();
                    Long deptId = Long.valueOf(String.valueOf(map.get("deptId")));
                    Long count = Long.valueOf(String.valueOf(map.get("count")));
                    String deptName = statisticsMapper.getDeptNameById(deptId);
                    vo.setName(deptName != null ? deptName : "未知单位");
                    vo.setValue(count);
                    vo.setPercentage(total > 0 ? (count * 100.0 / total) : 0.0);
                    return vo;
                })
                .sorted(Comparator.comparing(StatisticsUnitDistributionRespVO::getValue).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<StatisticsUnitDistributionRespVO> getReportAuditData() {
        // 获取专家审核数据
        List<Map<String, Object>> reviewerList = statisticsMapper.getReviewerDistributionList();
        if (CollUtil.isEmpty(reviewerList)) {
            return Collections.emptyList();
        }
        
        // 计算总数用于百分比计算
        long total = reviewerList.stream()
                .mapToLong(map -> Long.parseLong(String.valueOf(map.get("count"))))
                .sum();
        
        // 构建返回结果
        return reviewerList.stream()
                .map(map -> {
                    StatisticsUnitDistributionRespVO vo = new StatisticsUnitDistributionRespVO();
                    Long reviewerId = Long.valueOf(String.valueOf(map.get("reviewerId")));
                    Long count = Long.valueOf(String.valueOf(map.get("count")));
                    String reviewerName = statisticsMapper.getUsernameById(reviewerId);
                    vo.setName(reviewerName != null ? reviewerName : "未知专家");
                    vo.setValue(count);
                    vo.setPercentage(total > 0 ? (count * 100.0 / total) : 0.0);
                    return vo;
                })
                .sorted(Comparator.comparing(StatisticsUnitDistributionRespVO::getValue).reversed())
                .collect(Collectors.toList());
    }
    
    /**
     * 获取过去一个月的时间
     * 
     * @return 过去一个月的时间
     */
    private LocalDateTime getLastMonthTime() {
        return LocalDateTime.now().minusMonths(1);
    }
}
