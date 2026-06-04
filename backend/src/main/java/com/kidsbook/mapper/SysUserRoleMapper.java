package com.kidsbook.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kidsbook.entity.SysUserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

    @Select("SELECT r.code FROM sys_user_role ur JOIN sys_role r ON ur.role_id = r.id WHERE ur.user_type = #{userType} AND ur.user_id = #{userId} AND r.status = 1")
    List<String> selectRoleCodesByUser(String userType, Long userId);

    @Select("SELECT r.level FROM sys_user_role ur JOIN sys_role r ON ur.role_id = r.id WHERE ur.user_type = #{userType} AND ur.user_id = #{userId} AND r.status = 1")
    List<Integer> selectRoleLevelsByUser(String userType, Long userId);

    @Select("SELECT DISTINCT p.code FROM sys_user_role ur JOIN sys_role r ON ur.role_id = r.id JOIN sys_role_permission rp ON rp.role_id = r.id JOIN sys_permission p ON p.id = rp.permission_id WHERE ur.user_type = #{userType} AND ur.user_id = #{userId} AND r.status = 1")
    List<String> selectPermissionCodesByUser(String userType, Long userId);
}
