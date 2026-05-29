package com.kidsbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kidsbook.entity.BookResource;
import com.kidsbook.mapper.BookResourceMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class FileUploadService {
    private final BookResourceMapper bookResourceMapper;
    private final String uploadPath;

    private static final List<String> ALLOWED_IMAGE_TYPES = List.of("image/jpeg", "image/png", "image/gif", "image/jpg");
    private static final List<String> ALLOWED_DOC_TYPES = List.of("application/pdf");
    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024;
    private static final long MAX_DOC_SIZE = 50 * 1024 * 1024;

    public FileUploadService(BookResourceMapper bookResourceMapper,
                             @Value("${kidsbook.upload.path:./uploads}") String uploadPath) {
        this.bookResourceMapper = bookResourceMapper;
        this.uploadPath = uploadPath;
    }

    public BookResource uploadFile(MultipartFile file, Long bookId, String fileType) {
        validateFile(file, fileType);

        String originalName = file.getOriginalFilename();
        String extension = getExtension(originalName);
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        String subDir = getSubDir(fileType);
        String relativePath = subDir + "/" + fileName;

        File targetDir = new File(uploadPath + "/" + subDir);
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }

        File targetFile = new File(uploadPath + "/" + relativePath);
        try {
            file.transferTo(targetFile);
        } catch (IOException e) {
            throw new RuntimeException("文件保存失败: " + e.getMessage());
        }

        BookResource resource = new BookResource();
        resource.setBookId(bookId);
        resource.setFileName(fileName);
        resource.setOriginalName(originalName);
        resource.setFilePath(relativePath);
        resource.setFileType(fileType);
        resource.setFileSize(file.getSize());
        resource.setMimeType(file.getContentType());
        resource.setCreateTime(LocalDateTime.now());
        bookResourceMapper.insert(resource);

        log.info("文件上传成功: {} -> {}", originalName, relativePath);
        return resource;
    }

    public List<BookResource> uploadBatch(MultipartFile[] files, Long bookId, String fileType) {
        List<BookResource> results = new ArrayList<>();
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                results.add(uploadFile(file, bookId, fileType));
            }
        }
        return results;
    }

    public Page<BookResource> listResources(int page, int size, String fileType, Long bookId) {
        Page<BookResource> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<BookResource> wrapper = new LambdaQueryWrapper<>();
        if (fileType != null && !fileType.isEmpty()) {
            wrapper.eq(BookResource::getFileType, fileType);
        }
        if (bookId != null) {
            wrapper.eq(BookResource::getBookId, bookId);
        }
        wrapper.orderByDesc(BookResource::getCreateTime);
        return bookResourceMapper.selectPage(pageParam, wrapper);
    }

    public List<BookResource> getByBookId(Long bookId) {
        LambdaQueryWrapper<BookResource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BookResource::getBookId, bookId)
                .orderByDesc(BookResource::getCreateTime);
        return bookResourceMapper.selectList(wrapper);
    }

    public void deleteResource(Long id) {
        BookResource resource = bookResourceMapper.selectById(id);
        if (resource == null) {
            throw new RuntimeException("资源不存在");
        }
        File file = new File(uploadPath + "/" + resource.getFilePath());
        if (file.exists()) {
            file.delete();
        }
        bookResourceMapper.deleteById(id);
        log.info("资源已删除: {}", resource.getOriginalName());
    }

    private void validateFile(MultipartFile file, String fileType) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("请选择文件");
        }
        String contentType = file.getContentType();
        if ("cover".equals(fileType) || "image".equals(fileType)) {
            if (!ALLOWED_IMAGE_TYPES.contains(contentType)) {
                throw new RuntimeException("仅支持 JPG/PNG/GIF 格式的图片");
            }
            if (file.getSize() > MAX_IMAGE_SIZE) {
                throw new RuntimeException("图片大小不能超过10MB");
            }
        } else if ("pdf".equals(fileType)) {
            if (!ALLOWED_DOC_TYPES.contains(contentType)) {
                throw new RuntimeException("仅支持 PDF 格式的文档");
            }
            if (file.getSize() > MAX_DOC_SIZE) {
                throw new RuntimeException("PDF文件大小不能超过50MB");
            }
        } else {
            if (file.getSize() > MAX_DOC_SIZE) {
                throw new RuntimeException("文件大小不能超过50MB");
            }
        }
    }

    private String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "bin";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    private String getSubDir(String fileType) {
        return switch (fileType) {
            case "cover", "image" -> "covers";
            case "pdf" -> "pdfs";
            default -> "others";
        };
    }
}
