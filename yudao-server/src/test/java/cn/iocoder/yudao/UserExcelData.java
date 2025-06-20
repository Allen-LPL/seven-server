package cn.iocoder.yudao;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class UserExcelData {

  @ExcelProperty(index = 0)
  private String email;

  @ExcelProperty(index = 1)
  private String nickName;

  @ExcelProperty(index = 2)
  private String type;

  @ExcelProperty(index = 3)
  private String password;

  @ExcelProperty(index = 4)
  private String major;

  @ExcelProperty(index = 5)
  private String expertise;
}
