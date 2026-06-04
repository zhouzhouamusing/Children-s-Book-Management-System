package com.kidsbook.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kidsbook.common.PageResult;
import com.kidsbook.common.Permission;
import com.kidsbook.common.RequirePermission;
import com.kidsbook.common.Result;
import com.kidsbook.entity.BookResource;
import com.kidsbook.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {
    private final FileUploadService fileUploadService;

    @PostMapping("/upload")
    @RequirePermission(Permission.FILE_CREATE)
    public Result<?> upload(@RequestParam("file") MultipartFile file,
                            @RequestParam(value = "bookId", required = false) Long bookId,
                            @RequestParam(value = "fileType", defaultValue = "other") String fileType) {
        BookResource resource = fileUploadService.uploadFile(file, bookId, fileType);
        return Result.success(resource);
    }

    @PostMapping("/upload-batch")
    @RequirePermission(Permission.FILE_CREATE)
    public Result<?> uploadBatch(@RequestParam("files") MultipartFile[] files,
                                 @RequestParam(value = "bookId", required = false) Long bookId,
                                 @RequestParam(value = "fileType", defaultValue = "other") String fileType) {
        List<BookResource> resources = fileUploadService.uploadBatch(files, bookId, fileType);
        return Result.success(resources);
    }

    @GetMapping("/list")
    @RequirePermission(Permission.FILE_READ)
    public Result<PageResult<BookResource>> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size,
                          @RequestParam(required = false) String fileType,
                          @RequestParam(required = false) Long bookId) {
        Page<BookResource> result = fileUploadService.listResources(page, size, fileType, bookId);
        return Result.success(PageResult.of(result));
    }

    @GetMapping("/book/{bookId}")
    @RequirePermission(Permission.FILE_READ)
    public Result<?> getByBookId(@PathVariable Long bookId) {
        return Result.success(fileUploadService.getByBookId(bookId));
    }

    @PutMapping("/{id}/link")
    @RequirePermission(Permission.FILE_CREATE)
    public Result<?> linkToBook(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        Long bookId = body.get("bookId");
        if (bookId == null) {
            return Result.error(400, "请选择要关联的图书");
        }
        fileUploadService.linkResourceToBook(id, bookId);
        return Result.success(null);
    }

    @DeleteMapping("/{id}")
    @RequirePermission(Permission.FILE_DELETE)
    public Result<?> delete(@PathVariable Long id) {
        fileUploadService.deleteResource(id);
        return Result.success(null);
    }
}
