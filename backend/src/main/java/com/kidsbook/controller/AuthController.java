package com.kidsbook.controller;

import com.kidsbook.common.Result;
import com.kidsbook.entity.Reader;
import com.kidsbook.mapper.ReaderMapper;
import com.kidsbook.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final JwtUtil jwtUtil;
    private final ReaderMapper readerMapper;

    @GetMapping("/validate")
    public Result<Map<String, Object>> validateToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getCredentials() == null) {
            return Result.error(401, "未登录");
        }
        String token = auth.getCredentials().toString();
        String username = jwtUtil.getUsernameFromToken(token);
        String role = jwtUtil.getRoleFromToken(token);

        List<String> roles = jwtUtil.getRolesFromToken(token);
        List<String> permissions = jwtUtil.getPermissionsFromToken(token);

        Map<String, Object> data = new HashMap<>();
        data.put("username", username);
        data.put("role", role);
        data.put("roles", roles);
        data.put("permissions", permissions);
        data.put("valid", true);

        if ("READER".equals(role)) {
            Long readerId = jwtUtil.getReaderIdFromToken(token);
            if (readerId != null) {
                Reader reader = readerMapper.selectById(readerId);
                data.put("suspended", reader != null && "suspended".equals(reader.getStatus()));
            } else {
                data.put("suspended", false);
            }
        } else {
            data.put("suspended", false);
        }

        return Result.success(data);
    }
}
