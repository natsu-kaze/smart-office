package com.natsukaze.smartoffice.approvalservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.natsukaze.smartoffice.approvalservice.entity.ApprovalProcess;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ApprovalProcessMapper extends BaseMapper<ApprovalProcess> {

    @Delete("DELETE FROM approval_process WHERE form_id = #{formId}")
    int physicalDeleteByFormId(Long formId);
}
