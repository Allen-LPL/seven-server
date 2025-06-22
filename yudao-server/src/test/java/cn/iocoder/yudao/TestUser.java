//package cn.iocoder.yudao;
//
//import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
//import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
//import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
//import cn.iocoder.yudao.module.system.dal.dataobject.permission.UserRoleDO;
//import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
//import cn.iocoder.yudao.module.system.dal.mysql.permission.UserRoleMapper;
//import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
//import cn.iocoder.yudao.module.system.service.dept.DeptService;
//import cn.iocoder.yudao.module.system.service.permission.RoleService;
//import cn.iocoder.yudao.module.system.service.user.AdminUserService;
//import cn.iocoder.yudao.server.YudaoServerApplication;
//import com.alibaba.excel.EasyExcel;
//import com.alibaba.excel.context.AnalysisContext;
//import com.alibaba.excel.event.AnalysisEventListener;
//import com.alibaba.fastjson.JSONObject;
//import java.io.File;
//import java.util.Arrays;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//import javax.annotation.Resource;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.compress.utils.Lists;
//import org.junit.jupiter.api.Test;
//import org.junit.platform.commons.util.StringUtils;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.security.crypto.password.PasswordEncoder;
//
//
//@Slf4j
//@SpringBootTest(classes = YudaoServerApplication.class)
//public class TestUser {
//
//  @Resource
//  private UserRoleMapper userRoleMapper;
//
//  @Resource
//  private AdminUserMapper adminUserMapper;
//
//  @Resource
//  private PasswordEncoder passwordEncoder;
//
//  @Test
//  public void createUser() {
//    File fileName = new File("/Users/fangliu/Documents/WQ/副本user.xlsx");
//
//    // 1. 打印表头信息（调试用）
//    EasyExcel.read(fileName, new AnalysisEventListener<Map<Integer, String>>() {
//      @Override
//      public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
//        System.out.println("实际表头：" + headMap); // 打印Excel的真实表头
//      }
//      @Override public void invoke(Map<Integer, String> data, AnalysisContext context) {}
//      @Override public void doAfterAllAnalysed(AnalysisContext context) {}
//    }).sheet().doRead();
//
//    // 2. 尝试读取为Map（验证数据是否能被原生读取）
//    List<Map<Integer, String>> rawData = EasyExcel.read(fileName)
//        .sheet()
//        .doReadSync();
//    System.out.println("原始数据：" + rawData);
//
//    // 3. 正式读取（带异常处理）
//    List<UserExcelData> list = Lists.newArrayList();
//    try {
//      for (Map<Integer, String> map : rawData) {
//        UserExcelData data = new UserExcelData();
//        data.setEmail(map.get(0));
//        data.setNickName(map.get(1));
//        data.setType(map.get(2));
//        data.setPassword(map.get(3));
//        data.setMajor(map.get(4));
//        data.setExpertise(map.get(5));
//        list.add(data);
//      }
//      System.out.println("解析结果：" + list);
//    } catch (Exception e) {
//      log.error("error : ",e);
//    }
//
//    if (CollectionUtils.isAnyEmpty(list)){
//      return;
//    }
//
//
//    for (UserExcelData data : list) {
//      AdminUserDO adminUserDO = new AdminUserDO();
//      adminUserDO.setEmail(data.getEmail());
//      adminUserDO.setNickname(data.getNickName());
//      adminUserDO.setPassword(passwordEncoder.encode(data.getPassword()));
//      adminUserDO.setUsername(data.getNickName());
//      if (StringUtils.isNotBlank(data.getMajor())) {
//        List<String> major = Arrays.stream(data.getMajor().split("/")).collect(Collectors.toList());
//        adminUserDO.setMajor(major);
//      }
//      if (StringUtils.isNotBlank(data.getExpertise())) {
//        List<String> expertise = Arrays.stream(data.getExpertise().split("/")).collect(Collectors.toList());
//        adminUserDO.setExpertise(expertise);
//      }
//      adminUserDO.setCreator(String.valueOf(1L));
//      adminUserDO.setStatus(1);
//      if (data.getNickName().equals("ADMIN")){
//        adminUserDO.setPassword(passwordEncoder.encode("admin123456"));
//      }
//      adminUserDO.setTenantId(1L);
//      adminUserMapper.insert(adminUserDO);
//
//      UserRoleDO roleDO = new UserRoleDO();
//      roleDO.setUserId(adminUserDO.getId());
//      if ("普通用户".equals(data.getType())) {
//        roleDO.setRoleId(101L);
//      }else if ("专家用户".equals(data.getType().trim())) {
//        roleDO.setRoleId(3L);
//      }else if ("科研管理员".equals(data.getType().trim())) {
//        roleDO.setRoleId(2L);
//      }else if ("系统管理员".equals(data.getType().trim())) {
//        roleDO.setRoleId(1L);
//      }
//      roleDO.setCreator("1L");
//      roleDO.setDeleted(Boolean.FALSE);
//      roleDO.setTenantId(1L);
//      userRoleMapper.insert(roleDO);
//    }
//  }
//
//}
