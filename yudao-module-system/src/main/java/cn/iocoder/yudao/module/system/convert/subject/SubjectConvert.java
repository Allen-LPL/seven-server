package cn.iocoder.yudao.module.system.convert.subject;

import cn.iocoder.yudao.module.system.controller.admin.subject.vo.SubjectRespVO;
import cn.iocoder.yudao.module.system.dal.dataobject.subject.SubjectDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 学科 Convert
 *
 * @author 芋道源码
 */
@Mapper
public interface SubjectConvert {

    SubjectConvert INSTANCE = Mappers.getMapper(SubjectConvert.class);

    SubjectRespVO convert(SubjectDO bean);

    List<SubjectRespVO> convertList(List<SubjectDO> list);

} 