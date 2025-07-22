package cn.iocoder.yudao.module.system.service.task.utils;


import cn.iocoder.yudao.module.system.enums.task.VectorQueryTypeEnum;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class VectorCalculateUtils {


  public static double computeSimilarity(List<Double> vec1, List<Double> vec2, VectorQueryTypeEnum queryTypeEnum) {
    if (vec1.size() != vec2.size()) {
      throw new IllegalArgumentException("Vectors must have the same length");
    }

    switch (queryTypeEnum) {
      case IP:
        return ipSimilarity(vec1, vec2);
      case COSINE:
        return cosineSimilarity(vec1, vec2);
      case L2:
        return l2Distance(vec1, vec2);
      default:
        throw new IllegalArgumentException("Unknown similarity method");
    }
  }

  /**
   * 内积相似度 (IP)
   */
  public static double ipSimilarity(List<Double> v1, List<Double> v2) {
    return IntStream.range(0, v1.size())
        .mapToDouble(i -> v1.get(i) * v2.get(i))
        .sum();
  }

  /**
   * 余弦相似度 (COSINE)
   */
  private static double cosineSimilarity(List<Double> vec1, List<Double> vec2) {
    List<Double> normV1 = normalize(vec1);
    List<Double> normV2 = normalize(vec2);
    return ipSimilarity(normV1, normV2);
  }

  /**
   * 欧式距离 (L2) 相似度计算
   * 注意: 这里返回的是距离值，越小表示越相似
   * @param v1 向量1
   * @param v2 向量2
   * @return 欧式距离
   */
  public static double l2Distance(List<Double> v1, List<Double> v2) {
    double sum = IntStream.range(0, v1.size())
        .mapToDouble(i -> Math.pow(v1.get(i) - v2.get(i), 2))
        .sum();
    return Math.sqrt(sum);
  }

  /**
   * 向量归一化 (L2归一化)
   */
  public static List<Double> normalize(List<Double> vector) {
    double norm = Math.sqrt(vector.stream()
        .mapToDouble(d -> d * d)
        .sum());

    if (norm == 0.0) {
      return vector; // 避免除以零，零向量归一化后仍为零向量
    }

    return vector.stream()
        .map(d -> d / norm)
        .collect(Collectors.toList());
  }

}
