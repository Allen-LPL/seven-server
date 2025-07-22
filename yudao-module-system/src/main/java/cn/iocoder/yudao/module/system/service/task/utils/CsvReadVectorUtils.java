package cn.iocoder.yudao.module.system.service.task.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.csv.CsvData;
import cn.hutool.core.text.csv.CsvReadConfig;
import cn.hutool.core.text.csv.CsvReader;
import cn.hutool.core.text.csv.CsvRow;
import cn.hutool.core.text.csv.CsvUtil;
import com.google.common.collect.Maps;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CsvReadVectorUtils {

  public static Map<String, List<Double>> readVector(String vectorPath){

    Map<String,List<Double>> vectorMap = Maps.newHashMap();

    CsvReadConfig config = CsvReadConfig.defaultConfig()
        .setFieldSeparator('\t');

    // 2. 读取 CSV 文件
    CsvReader reader = CsvUtil.getReader(config);
    CsvData data = reader.read(FileUtil.file(vectorPath));

    // 3. 遍历数据
    for (CsvRow row : data.getRows()) {
      String model = row.get(0);
      String vectorStr = row.get(1);
      List<Double> vectorArray = Arrays.stream(vectorStr.substring(1, vectorStr.length() - 1).split(","))
          .map(String::trim)
          .map(Double::parseDouble)
          .collect(Collectors.toList());
      vectorMap.put(model, vectorArray);
    }
    return  vectorMap;
  }

}
