package cn.iocoder.yudao.module.system.service.statistics;

import cn.iocoder.yudao.module.system.controller.admin.statistics.vo.StatisticsDataRespVO;
import cn.iocoder.yudao.module.system.controller.admin.statistics.vo.StatisticsRangeReqVO;
import cn.iocoder.yudao.module.system.controller.admin.statistics.vo.StatisticsTrendRespVO;
import cn.iocoder.yudao.module.system.controller.admin.statistics.vo.StatisticsUnitDistributionRespVO;

import java.util.List;

/**
 * 统计 Service 接口
 */
public interface StatisticsService {

    /**
     * 获取统计概览数据
     *
     * @return 统计概览数据
     */
    StatisticsDataRespVO getOverviewData();

    /**
     * 获取趋势分析数据
     *
     * @param reqVO 时间范围请求
     * @return 趋势分析数据
     */
    StatisticsTrendRespVO getTrendData(StatisticsRangeReqVO reqVO);

    /**
     * 获取异常单位分布数据
     *
     * @param userId 当前用户ID
     * @return 单位分布数据列表
     */
    List<StatisticsUnitDistributionRespVO> getUnitDistributionData(Long userId);

    /**
     * 获取报告审核统计数据
     *
     * @return 专家审核数据列表
     */
    List<StatisticsUnitDistributionRespVO> getReportAuditData();
}
