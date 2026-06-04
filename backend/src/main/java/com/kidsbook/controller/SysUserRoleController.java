package com.kidsbook.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kidsbook.common.BusinessException;
import com.kidsbook.common.Permission;
import com.kidsbook.common.RequirePermission;
import com.kidsbook.common.Result;
import com.kidsbook.dto.UserRoleAssignDTO;
import com.kidsbook.dto.UserWithRolesVO;
import com.kidsbook.entity.Admin;
import com.kidsbook.entity.Reader;
import com.kidsbook.entity.ReaderAccount;
import com.kidsbook.entity.SysRole;
import com.kidsbook.entity.SysUserRole;
import com.kidsbook.mapper.AdminMapper;
import com.kidsbook.mapper.ReaderAccountMapper;
import com.kidsbook.mapper.ReaderMapper;
import com.kidsbook.mapper.SysRoleMapper;
import com.kidsbook.mapper.SysUserRoleMapper;
import com.kidsbook.service.RbacService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sys/user-roles")
@RequiredArgsConstructor
public class SysUserRoleController {
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMapper sysRoleMapper;
    private final AdminMapper adminMapper;
    private final ReaderAccountMapper readerAccountMapper;
    private final ReaderMapper readerMapper;
    private final RbacService rbacService;

    @GetMapping("/users/{userType}/{userId}/roles")
    @RequirePermission(Permission.USER_ROLE_ASSIGN)
    public Result<?> getUserRoles(@PathVariable String userType, @PathVariable Long userId) {
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
            new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserType, userType)
                .eq(SysUserRole::getUserId, userId));
        List<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("roleIds", roleIds);
        if (!roleIds.isEmpty()) {
            List<SysRole> roles = sysRoleMapper.selectList(
                new LambdaQueryWrapper<SysRole>().in(SysRole::getId, roleIds));
            result.put("roles", roles);
        } else {
            result.put("roles", Collections.emptyList());
        }
        return Result.success(result);
    }

    @PutMapping("/users/{userType}/{userId}/roles")
    @RequirePermission(Permission.USER_ROLE_ASSIGN)
    public Result<?> assignRoles(@PathVariable String userType,
                                 @PathVariable Long userId,
                                 @RequestBody UserRoleAssignDTO dto) {
        if (!"ADMIN".equals(userType) && !"READER".equals(userType)) {
            throw new BusinessException(400, "无效的用户类型");
        }
        sysUserRoleMapper.delete(
            new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserType, userType)
                .eq(SysUserRole::getUserId, userId));
        if (dto.getRoleIds() != null) {
            for (Long roleId : dto.getRoleIds()) {
                SysUserRole ur = new SysUserRole();
                ur.setUserType(userType);
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                sysUserRoleMapper.insert(ur);
            }
        }
        return Result.success("角色分配成功");
    }

    @GetMapping("/roles/{roleId}/users")
    @RequirePermission(Permission.USER_ROLE_ASSIGN)
    public Result<?> getUsersByRole(@PathVariable Long roleId) {
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
            new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, roleId));
        return Result.success(userRoles);
    }

    @GetMapping("/users")
    @RequirePermission(Permission.USER_ROLE_ASSIGN)
    public Result<?> listAllUsersWithRoles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String userType) {

        List<UserWithRolesVO> allUsers = new ArrayList<>();

        if (userType == null || "ADMIN".equals(userType)) {
            LambdaQueryWrapper<Admin> adminWrapper = new LambdaQueryWrapper<>();
            if (keyword != null && !keyword.isBlank()) {
                adminWrapper.like(Admin::getUsername, keyword)
                    .or().like(Admin::getNickname, keyword);
            }
            List<Admin> admins = adminMapper.selectList(adminWrapper);
            for (Admin admin : admins) {
                UserWithRolesVO vo = new UserWithRolesVO();
                vo.setUserId(admin.getId());
                vo.setUsername(admin.getUsername());
                vo.setDisplayName(admin.getNickname() != null ? admin.getNickname() : admin.getUsername());
                vo.setUserType("ADMIN");
                allUsers.add(vo);
            }
        }

        if (userType == null || "READER".equals(userType)) {
            LambdaQueryWrapper<ReaderAccount> raWrapper = new LambdaQueryWrapper<>();
            if (keyword != null && !keyword.isBlank()) {
                raWrapper.like(ReaderAccount::getUsername, keyword);
            }
            List<ReaderAccount> accounts = readerAccountMapper.selectList(raWrapper);
            for (ReaderAccount account : accounts) {
                UserWithRolesVO vo = new UserWithRolesVO();
                vo.setUserId(account.getId());
                vo.setUsername(account.getUsername());
                Reader reader = readerMapper.selectById(account.getReaderId());
                vo.setDisplayName(reader != null ? reader.getName() : account.getUsername());
                vo.setUserType("READER");
                allUsers.add(vo);
            }
        }

        // Fetch roles for all users
        for (UserWithRolesVO vo : allUsers) {
            List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>()
                    .eq(SysUserRole::getUserType, vo.getUserType())
                    .eq(SysUserRole::getUserId, vo.getUserId()));
            if (!userRoles.isEmpty()) {
                List<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
                List<SysRole> roles = sysRoleMapper.selectList(
                    new LambdaQueryWrapper<SysRole>().in(SysRole::getId, roleIds));
                vo.setRoles(roles);
            } else {
                vo.setRoles(Collections.emptyList());
            }
        }

        // Manual pagination
        int total = allUsers.size();
        int fromIndex = Math.min((page - 1) * size, total);
        int toIndex = Math.min(fromIndex + size, total);
        List<UserWithRolesVO> pageData = allUsers.subList(fromIndex, toIndex);

        Map<String, Object> result = new HashMap<>();
        result.put("records", pageData);
        result.put("total", total);
        result.put("current", page);
        result.put("size", size);
        return Result.success(result);
    }
}
