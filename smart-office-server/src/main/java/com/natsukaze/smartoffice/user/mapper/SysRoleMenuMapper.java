package com.natsukaze.smartoffice.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.natsukaze.smartoffice.user.entity.SysRoleMenu;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysRoleMenuMapper extends BaseMapper<SysRoleMenu> {

    @Delete("DELETE FROM sys_role_menu WHERE role_id = #{roleId}")
    int physicalDeleteByRoleId(Long roleId);
}
