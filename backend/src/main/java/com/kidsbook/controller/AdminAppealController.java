package com.kidsbook.controller;

import com.kidsbook.common.PageResult;
import com.kidsbook.common.Permission;
import com.kidsbook.common.RequirePermission;
import com.kidsbook.common.Result;
import com.kidsbook.entity.Appeal;
import com.kidsbook.service.AppealService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/appeals")
@RequiredArgsConstructor
public class AdminAppealController {
    private final AppealService appealService;

    @GetMapping
    @RequirePermission(Permission.APPEAL_READ)
    public Result<Object> listAppeals(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        var result = appealService.getAllAppeals(page, size, status, keyword);
        return Result.success(PageResult.of(result));
    }

    @GetMapping("/{id}")
    @RequirePermission(Permission.APPEAL_READ)
    public Result<Appeal> getAppeal(@PathVariable Long id) {
        return Result.success(appealService.getAppealById(id));
    }

    @PutMapping("/{id}/review")
    @RequirePermission(Permission.APPEAL_REVIEW)
    public Result<Void> reviewAppeal(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String action = body != null ? body.getOrDefault("action", "") : "";
        String feedback = body != null ? body.getOrDefault("feedback", "") : "";
        appealService.reviewAppeal(id, action, feedback);
        return Result.success(null);
    }
}
