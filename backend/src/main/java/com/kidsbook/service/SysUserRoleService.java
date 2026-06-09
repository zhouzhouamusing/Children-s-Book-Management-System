package com.kidsbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kidsbook.entity.Admin;
import com.kidsbook.entity.Reader;
import com.kidsbook.entity.SysRole;
import com.kidsbook.entity.SysUserRole;
import com.kidsbook.mapper.AdminMapper;
import com.kidsbook.mapper.ReaderMapper;
import com.kidsbook.mapper.SysRoleMapper;
import com.kidsbook.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysUserRoleService {

    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final AdminMapper adminMapper;
    private final ReaderMapper readerMapper;
    private final PermissionCacheService permissionCacheService;

    public List<SysRole> getUserRoles(String userType, Long userId) {
        List<SysUserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserType, userType)
                        .eq(SysUserRole::getUserId, userId));
        if (userRoles.isEmpty()) return Collections.emptyList();

        List<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
        return roleMapper.selectList(
                new LambdaQueryWrapper<SysRole>()
                        .in(SysRole::getId, roleIds)
                        .eq(SysRole::getStatus, 1));
    }

    @Transactional
    public void assignRoles(String userType, Long userId, List<Long> roleIds) {
        userRoleMapper.delete(
                new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserType, userType)
                        .eq(SysUserRole::getUserId, userId));

        if (roleIds != null && !roleIds.isEmpty()) {
            for (Long roleId : roleIds) {
                SysUserRole ur = new SysUserRole();
                ur.setUserType(userType);
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                userRoleMapper.insert(ur);
            }
        }
        permissionCacheService.invalidateUser(userType, userId);
    }

    public Page<Map<String, Object>> listAdminsWithRoles(int page, int size, String keyword) {
        LambdaQueryWrapper<Admin> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Admin::getUsername, keyword).or().like(Admin::getNickname, keyword);
        }
        wrapper.orderByAsc(Admin::getId);
        Page<Admin> adminPage = adminMapper.selectPage(new Page<>(page, size), wrapper);

        List<Map<String, Object>> records = new ArrayList<>();
        for (Admin admin : adminPage.getRecords()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", admin.getId());
            map.put("username", admin.getUsername());
            map.put("nickname", admin.getNickname());
            map.put("email", admin.getEmail());
            map.put("roles", getUserRoles("admin", admin.getId()));
            records.add(map);
        }

        Page<Map<String, Object>> result = new Page<>(page, size, adminPage.getTotal());
        result.setRecords(records);
        return result;
    }

    public Page<Map<String, Object>> listReadersWithRoles(int page, int size, String keyword) {
        LambdaQueryWrapper<Reader> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Reader::getName, keyword);
        }
        wrapper.orderByAsc(Reader::getId);
        Page<Reader> readerPage = readerMapper.selectPage(new Page<>(page, size), wrapper);

        List<Map<String, Object>> records = new ArrayList<>();
        for (Reader reader : readerPage.getRecords()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", reader.getId());
            map.put("name", reader.getName());
            map.put("age", reader.getAge());
            map.put("status", reader.getStatus());
            map.put("roles", getUserRoles("reader", reader.getId()));
            records.add(map);
        }

        Page<Map<String, Object>> result = new Page<>(page, size, readerPage.getTotal());
        result.setRecords(records);
        return result;
    }
}
