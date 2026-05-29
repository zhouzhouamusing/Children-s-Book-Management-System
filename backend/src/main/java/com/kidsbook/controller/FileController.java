package com.kidsbook.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kidsbook.common.Result;
import com.kidsbook.entity.BookResource;
import com.kidsbook.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {
    private final FileUploadService fileUploadService;

    @PostMapping("/upload")
    public Result<?> upload(@RequestParam("file") MultipartFile file,
                            @RequestParam(value = "bookId", required = false) Long bookId,
                            @RequestParam(value = "fileType", defaultValue = "other") String fileType) {
        BookResource resource = fileUploadService.uploadFile(file, bookId, fileType);
        return Result.success(resource);
    }

    @PostMapping("/upload-batch")
    public Result<?> uploadBatch(@RequestParam("files") MultipartFile[] files,
                                 @RequestParam(value = "bookId", required = false) Long bookId,
                                 @RequestParam(value = "fileType", defaultValue = "other") String fileType) {
        List<BookResource> resources = fileUploadService.uploadBatch(files, bookId, fileType);
        return Result.success(resources);
    }

    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size,
                          @RequestParam(required = false) String fileType,
                          @RequestParam(required = false) Long bookId) {
        Page<BookResource> result = fileUploadService.listResources(page, size, fileType, bookId);
        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        return Result.success(data);
    }

    @GetMapping("/book/{bookId}")
    public Result<?> getByBookId(@PathVariable Long bookId) {
        return Result.success(fileUploadService.getByBookId(bookId));
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        fileUploadService.deleteResource(id);
        return Result.success(null);
    }
}
