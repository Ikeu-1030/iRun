package com.ikeu.server.controller;

import com.ikeu.common.constant.MessageConstant;
import com.ikeu.common.constant.RedisConstant;
import com.ikeu.common.result.Result;
import com.ikeu.common.utils.AliOssUtil;
import com.ikeu.common.utils.JwtUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ikeu.model.entity.SystemConfig;
import com.ikeu.model.vo.BannerVO;
import com.ikeu.server.mapper.SystemConfigMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 通用接口，提供文件上传、获取默认头像、平台公告等功能。
 * @author ikeu
 * @since 2025/05/21
 */
@Slf4j
@RestController
@Tag(name = "通用接口", description = "文件上传、默认头像、平台公告等通用接口")
public class CommonController {
    /** 可选依赖，OSS 未配置时 {@link Optional#empty()} → {@code orElse(null)} */
    private final AliOssUtil aliOssUtil;
    private final SystemConfigMapper systemConfigMapper;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate stringRedisTemplate;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public CommonController(Optional<AliOssUtil> aliOssUtil, SystemConfigMapper systemConfigMapper,
                            JwtUtil jwtUtil, StringRedisTemplate stringRedisTemplate) {
        this.aliOssUtil = aliOssUtil.orElse(null);
        this.systemConfigMapper = systemConfigMapper;
        this.jwtUtil = jwtUtil;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    private static final String DEFAULT_AVATAR_PATH = "static/imgs/default_avatar.jpg";
    private static final Path LOCAL_UPLOAD_DIR = Paths.get("uploads").toAbsolutePath().normalize();

    private static final String CONFIG_KEY_UPLOAD_DAILY = "upload.max_daily";
    private static final String CONFIG_KEY_PLATFORM_ANNOUNCEMENT = "platform.announcement";
    private static final String CONFIG_KEY_BANNER_IMAGES = "banner.images";
    private static final String CONFIG_KEY_BANNER_INTERVAL = "banner.interval_seconds";

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 单文件最大 5MB
    private static final int UPLOAD_DAILY_DEFAULT = 20; // 默认每用户每日上传限额
    private static final int BANNER_INTERVAL_DEFAULT = 3; // 轮播图默认间隔秒数

    /** 允许上传的文件类型 */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp",
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx", ".txt"
    );

    /**
     * 文件上传至阿里云OSS；OSS 未配置时回退本地文件存储。
     *
     * <p>接收 MultipartFile，提取原始文件名后缀，使用 UUID 生成本地唯一文件名，
     * 调用 {@link AliOssUtil#upload} 将文件字节流上传至 OSS 或本地存储，返回文件的公网访问URL。
     * 文件为空或上传过程中发生 IO 异常时返回错误消息。
     *
     * @param file 上传的文件
     * @return 文件访问URL
     */
    @PostMapping("/common/upload")
    @Operation(summary = "文件上传接口")
    public Result<String> upload(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        // 鉴权：支持管理端 token 头或用户端 authentication 头
        String token = request.getHeader("token");
        String auth = request.getHeader("authentication");
        Long userId = null;
        try {
            if (token != null && !token.isBlank()) {
                var claims = jwtUtil.parseAdminAccessToken(token);
                userId = jwtUtil.getAdminIdFromClaims(claims);
            } else if (auth != null && !auth.isBlank()) {
                var claims = jwtUtil.parseUserAccessToken(auth);
                userId = jwtUtil.getUserIdFromClaims(claims);
            }
        } catch (Exception e) {
            log.warn("上传鉴权失败: {}", e.getMessage());
        }
        if (userId == null) {
            return Result.error(MessageConstant.USER_NOT_LOGIN);
        }

        if (file == null || file.isEmpty()) {
            return Result.error(MessageConstant.FILE_EMPTY);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            return Result.error(MessageConstant.FILE_SIZE_EXCEED);
        }

        // 每用户每日上传次数限制（从系统配置读取，先校验再消耗配额）
        int maxDaily = UPLOAD_DAILY_DEFAULT;
        SystemConfig uploadConfig = systemConfigMapper.selectOne(
                new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getConfigKey, CONFIG_KEY_UPLOAD_DAILY));
        if (uploadConfig != null && uploadConfig.getConfigValue() != null) {
            try { maxDaily = Integer.parseInt(uploadConfig.getConfigValue()); } catch (NumberFormatException ignored) {}
        }
        String dailyKey = RedisConstant.USER_UPLOAD_DAILY_KEY + userId + ":" + java.time.LocalDate.now();
        String current = stringRedisTemplate.opsForValue().get(dailyKey);
        int currentCount = current != null ? Integer.parseInt(current) : 0;
        if (currentCount >= maxDaily) {
            return Result.error(MessageConstant.UPLOAD_LIMIT_EXCEEDED);
        }
        Long dailyCount = stringRedisTemplate.opsForValue().increment(dailyKey);
        stringRedisTemplate.expire(dailyKey, 1, TimeUnit.DAYS);

        log.info("文件上传：{}", file.getOriginalFilename());

        String originalFileName = file.getOriginalFilename();
        String suffix = null;
        if (originalFileName != null) {
            suffix = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        if (suffix == null || !ALLOWED_EXTENSIONS.contains(suffix.toLowerCase())) {
            return Result.error(MessageConstant.FILE_TYPE_NOT_SUPPORTED + "，" + String.join(", ", ALLOWED_EXTENSIONS));
        }
        String fileName = UUID.randomUUID() + suffix;

        try {
            if (aliOssUtil != null && aliOssUtil.getEndpoint() != null && !aliOssUtil.getEndpoint().isBlank()) {
                // OSS 已配置 → 上传到阿里云
                String filePath = aliOssUtil.upload(file.getBytes(), fileName);
                return Result.successData(filePath);
            } else {
                // OSS 未配置 → 本地存储（测试环境）
                Files.createDirectories(LOCAL_UPLOAD_DIR);
                Path targetPath = LOCAL_UPLOAD_DIR.resolve(fileName);
                file.transferTo(targetPath.toFile());
                String fileUrl = "/common/local-upload/" + fileName;
                log.info("本地文件上传到: {}", targetPath);
                return Result.successData(fileUrl);
            }
        } catch (IOException e) {
            log.error("文件上传失败：{}", e.getMessage());
        }

        return Result.error(MessageConstant.UPLOAD_FAILED);
    }

    /**
     * 提供本地存储的上传文件访问（测试环境无 OSS 时使用）。
     *
     * @param fileName 文件名（含扩展名）
     * @return 文件资源
     */
    @GetMapping("/common/local-upload/{fileName}")
    @Operation(summary = "获取本地上传文件")
    public ResponseEntity<Resource> getLocalUpload(@PathVariable String fileName) {
        Path filePath = LOCAL_UPLOAD_DIR.resolve(fileName).normalize();
        // 路径穿越防护
        if (!filePath.startsWith(LOCAL_UPLOAD_DIR)) {
            return ResponseEntity.badRequest().build();
        }
        if (!Files.exists(filePath)) {
            return ResponseEntity.notFound().build();
        }
        try {
            Resource resource = new UrlResource(filePath.toUri());
            String contentType = Files.probeContentType(filePath);
            return ResponseEntity.ok()
                    .contentType(contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM)
                    .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                    .body(resource);
        } catch (IOException e) {
            log.error("本地文件读取失败: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取平台公告文本，供用户端首页滚动展示。
     * @return 公告文本，未配置时返回空字符串
     */
    @GetMapping("/common/announcement")
    @Operation(summary = "获取平台公告")
    public Result<String> getAnnouncement() {
        SystemConfig config = systemConfigMapper.selectOne(
                new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getConfigKey, CONFIG_KEY_PLATFORM_ANNOUNCEMENT));
        if (config == null) return Result.successData("");
        return Result.successData(config.getConfigValue());
    }



    /**
     * 获取首页轮播图，返回图片URL列表和切换间隔。
     * @return 轮播图数据：images（URL列表）和 interval（秒）
     */
    @GetMapping("/common/banners")
    @Operation(summary = "获取首页轮播图")
    public Result<BannerVO> getBanners() {
        var imagesCfg = systemConfigMapper.selectOne(
                new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getConfigKey, CONFIG_KEY_BANNER_IMAGES));
        var intervalCfg = systemConfigMapper.selectOne(
                new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getConfigKey, CONFIG_KEY_BANNER_INTERVAL));

        ArrayList<String> images = new ArrayList<>();
        if (imagesCfg != null && imagesCfg.getConfigValue() != null && !imagesCfg.getConfigValue().isBlank()) {
            for (String s : imagesCfg.getConfigValue().split(",")) {
                String trimmed = s.trim();
                if (!trimmed.isEmpty()) images.add(trimmed);
            }
        }

        int interval = BANNER_INTERVAL_DEFAULT;
        if (intervalCfg != null && intervalCfg.getConfigValue() != null) {
            try { interval = Integer.parseInt(intervalCfg.getConfigValue()); } catch (NumberFormatException ignored) {}
        }
        interval = Math.min(10, Math.max(1, interval));

        return Result.successData(BannerVO.builder().images(images).interval(interval).build());
    }

    /**
     * 获取默认头像图片资源。
     *
     * <p>从 classpath 下的 {@code static/imgs/default_avatar.jpg} 加载默认头像文件，
     * 以 JPEG 媒体类型返回 ResponseEntity。文件不存在时返回 404。
     *
     * @return 默认头像图片响应实体
     */
    @GetMapping("/imgs/default_avatar.jpg")
    @Operation(summary = "获取默认头像")
    public ResponseEntity<Resource> defaultAvatar() throws IOException {
        ClassPathResource resource = new ClassPathResource(DEFAULT_AVATAR_PATH);
        if (!resource.exists()) {
            log.error("默认头像文件不存在:" + DEFAULT_AVATAR_PATH);
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                .body(resource);
    }
}
