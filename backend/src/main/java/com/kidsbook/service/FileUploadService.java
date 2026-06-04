package com.kidsbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kidsbook.common.BusinessException;
import com.kidsbook.entity.BookResource;
import com.kidsbook.mapper.BookResourceMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
public class FileUploadService {
    private final BookResourceMapper bookResourceMapper;
    private final String uploadPath;

    private static final List<String> ALLOWED_IMAGE_TYPES = List.of("image/jpeg", "image/png", "image/gif", "image/jpg");
    private static final List<String> ALLOWED_DOC_TYPES = List.of("application/pdf");
    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024;
    private static final long MAX_DOC_SIZE = 50 * 1024 * 1024;
    private static final List<String> ALLOWED_EXTENSIONS = List.of(
        "jpg", "jpeg", "png", "gif", "pdf", "bin"
    );
    private static final Map<String, List<String>> MIME_EXTENSION_MAP = Map.of(
        "image/jpeg", List.of("jpg", "jpeg"),
        "image/jpg", List.of("jpg", "jpeg"),
        "image/png", List.of("png"),
        "image/gif", List.of("gif"),
        "application/pdf", List.of("pdf")
    );
    private static final Pattern WINDOWS_RESERVED_NAMES = Pattern.compile(
        "^(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(\\.|$)", Pattern.CASE_INSENSITIVE);
    private static final int MAX_FILENAME_LENGTH = 200;

    public FileUploadService(BookResourceMapper bookResourceMapper,
                             @Value("${kidsbook.upload.path:./uploads}") String uploadPath) {
        this.bookResourceMapper = bookResourceMapper;
        this.uploadPath = uploadPath;
    }

    @PostConstruct
    public void init() {
        File dir = new File(uploadPath);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created) {
                throw new IllegalStateException("无法创建上传目录: " + dir.getAbsolutePath() + "，请检查路径和权限");
            }
        }
        if (!dir.canWrite()) {
            throw new IllegalStateException("上传目录不可写: " + dir.getAbsolutePath());
        }
        log.info("文件上传路径: {}", dir.getAbsolutePath());
    }

    public BookResource uploadFile(MultipartFile file, Long bookId, String fileType) {
        validateFile(file, fileType);

        String originalName = file.getOriginalFilename();
        String extension = getExtension(originalName);
        String contentType = file.getContentType();
        if (contentType != null && MIME_EXTENSION_MAP.containsKey(contentType)) {
            List<String> validExts = MIME_EXTENSION_MAP.get(contentType);
            if (!validExts.contains(extension)) {
                throw new BusinessException(400, "文件扩展名与内容类型不匹配");
            }
        }
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        String subDir = getSubDir(fileType);
        String relativePath = subDir + "/" + fileName;

        File targetDir = new File(uploadPath + "/" + subDir);
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }

        String sanitizedOriginalName = sanitizeFileName(originalName);

        File targetFile = new File(uploadPath + "/" + relativePath);
        try {
            if (!targetFile.getCanonicalPath().startsWith(new File(uploadPath).getCanonicalPath())) {
                throw new BusinessException(400, "非法的文件路径");
            }
        } catch (IOException e) {
            throw new BusinessException(400, "非法的文件路径");
        }
        try {
            file.transferTo(targetFile);
        } catch (IOException e) {
            throw new BusinessException(400, "文件保存失败: " + e.getMessage());
        }

        BookResource resource = new BookResource();
        resource.setBookId(bookId);
        resource.setFileName(fileName);
        resource.setOriginalName(sanitizedOriginalName);
        resource.setFilePath(relativePath);
        resource.setFileType(fileType);
        resource.setFileSize(file.getSize());
        resource.setMimeType(file.getContentType());
        resource.setCreateTime(LocalDateTime.now());
        bookResourceMapper.insert(resource);

        log.info("文件上传成功: {} -> {}", sanitizedOriginalName, relativePath);
        return resource;
    }

    @Transactional
    public List<BookResource> uploadBatch(MultipartFile[] files, Long bookId, String fileType) {
        if (files == null || files.length == 0) {
            throw new BusinessException(400, "请选择文件");
        }
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
            throw new BusinessException(404, "资源不存在");
        }
        File file = new File(uploadPath + "/" + resource.getFilePath());
        try {
            if (file.getCanonicalPath().startsWith(new File(uploadPath).getCanonicalPath()) && file.exists()) {
                file.delete();
            }
        } catch (IOException e) {
            log.warn("路径验证失败，跳过文件删除: {}", e.getMessage());
        }
        bookResourceMapper.deleteById(id);
        log.info("资源已删除: {}", resource.getOriginalName());
    }

    @Transactional
    public void deleteByBookId(Long bookId) {
        List<BookResource> resources = getByBookId(bookId);
        for (BookResource resource : resources) {
            File file = new File(uploadPath + "/" + resource.getFilePath());
            try {
                if (file.getCanonicalPath().startsWith(new File(uploadPath).getCanonicalPath()) && file.exists()) {
                    file.delete();
                }
            } catch (IOException e) {
                log.warn("路径验证失败，跳过文件删除: {}", e.getMessage());
            }
            bookResourceMapper.deleteById(resource.getId());
        }
        if (!resources.isEmpty()) {
            log.info("已清理图书ID={}的{}个资源文件", bookId, resources.size());
        }
    }

    public void linkResourceToBook(Long resourceId, Long bookId) {
        BookResource resource = bookResourceMapper.selectById(resourceId);
        if (resource == null) {
            throw new BusinessException(404, "资源不存在");
        }
        resource.setBookId(bookId);
        bookResourceMapper.updateById(resource);
        log.info("资源{}已关联到图书ID={}", resource.getOriginalName(), bookId);
    }

    private void validateFile(MultipartFile file, String fileType) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "请选择文件");
        }
        String contentType = file.getContentType();
        if ("cover".equals(fileType) || "image".equals(fileType)) {
            if (!ALLOWED_IMAGE_TYPES.contains(contentType)) {
                throw new BusinessException(400, "仅支持 JPG/PNG/GIF 格式的图片");
            }
            if (file.getSize() > MAX_IMAGE_SIZE) {
                throw new BusinessException(400, "图片大小不能超过10MB");
            }
        } else if ("pdf".equals(fileType)) {
            if (!ALLOWED_DOC_TYPES.contains(contentType)) {
                throw new BusinessException(400, "仅支持 PDF 格式的文档");
            }
            if (file.getSize() > MAX_DOC_SIZE) {
                throw new BusinessException(400, "PDF文件大小不能超过50MB");
            }
        } else {
            if (file.getSize() > MAX_DOC_SIZE) {
                throw new BusinessException(400, "文件大小不能超过50MB");
            }
        }
    }

    private String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "bin";
        }
        String ext = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BusinessException(400, "不支持的文件扩展名: " + ext);
        }
        return ext;
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "unnamed";
        }
        String sanitized = fileName.replaceAll("\\x00", "");
        sanitized = sanitized.replaceAll("[/\\\\]", "_");
        sanitized = sanitized.replaceAll("\\.\\.", "_");
        sanitized = sanitized.replaceAll("[\\x01-\\x1f\\x7f]", "");
        if (WINDOWS_RESERVED_NAMES.matcher(sanitized).find()) {
            sanitized = "_" + sanitized;
        }
        if (sanitized.length() > MAX_FILENAME_LENGTH) {
            String ext = "";
            int dotIdx = sanitized.lastIndexOf('.');
            if (dotIdx > 0) {
                ext = sanitized.substring(dotIdx);
                sanitized = sanitized.substring(0, MAX_FILENAME_LENGTH - ext.length()) + ext;
            } else {
                sanitized = sanitized.substring(0, MAX_FILENAME_LENGTH);
            }
        }
        if (sanitized.isBlank()) {
            return "unnamed";
        }
        return sanitized;
    }

    private String getSubDir(String fileType) {
        return switch (fileType) {
            case "cover", "image" -> "covers";
            case "pdf" -> "pdfs";
            default -> "others";
        };
    }
}
