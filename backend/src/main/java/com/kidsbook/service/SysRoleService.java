package com.kidsbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kidsbook.entity.SysRole;
import com.kidsbook.entity.SysRolePermission;
import com.kidsbook.entity.SysUserRole;
import com.kidsbook.mapper.SysRoleMapper;
import com.kidsbook.mapper.SysRolePermissionMapper;
import com.kidsbook.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysRoleService {

    private final SysRoleMapper roleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PermissionCacheService permissionCacheService;

    public Page<SysRole> listRoles(int page, int size, String keyword) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(SysRole::getName, keyword).or().like(SysRole::getCode, keyword);
        }
        wrapper.orderByAsc(SysRole::getId);
        return roleMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public List<SysRole> getAllRoles() {
        return roleMapper.selectList(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getStatus, 1).orderByAsc(SysRole::getId));
    }

    public SysRole getRoleById(Long id) {
        return roleMapper.selectById(id);
    }

    public void addRole(SysRole role) {
        roleMapper.insert(role);
    }

    public void updateRole(SysRole role) {
        roleMapper.updateById(role);
    }

    @Transactional
    public void deleteRole(Long id) {
        long userCount = userRoleMapper.selectCount(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, id));
        if (userCount > 0) {
            throw new RuntimeException("该角色已分配给用户，无法删除");
        }
        rolePermissionMapper.delete(
                new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, id));
        roleMapper.deleteById(id);
    }

    @Transactional
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        rolePermissionMapper.delete(
                new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, roleId));

        if (permissionIds != null && !permissionIds.isEmpty()) {
            for (Long permId : permissionIds) {
                SysRolePermission rp = new SysRolePermission();
                rp.setRoleId(roleId);
                rp.setPermissionId(permId);
                rolePermissionMapper.insert(rp);
            }
        }
        permissionCacheService.invalidateAll();
    }

    public List<Long> getRolePermissionIds(Long roleId) {
        List<SysRolePermission> rps = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, roleId));
        return rps.stream().map(SysRolePermission::getPermissionId).collect(Collectors.toList());
    }
}
