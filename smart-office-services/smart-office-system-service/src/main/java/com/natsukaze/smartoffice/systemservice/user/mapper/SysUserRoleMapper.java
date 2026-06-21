package com.natsukaze.smartoffice.systemservice.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.natsukaze.smartoffice.systemservice.user.entity.SysUserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Delete;

@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    int physicalDeleteByUserId(@Param("userId") Long userId);
}
