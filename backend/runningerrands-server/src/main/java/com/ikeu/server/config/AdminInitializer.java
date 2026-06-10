package com.ikeu.server.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ikeu.model.entity.Admin;
import com.ikeu.server.mapper.AdminMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 管理员初始化器，应用启动时检查并创建默认超级管理员账号。
 * @author ikeu
 * @since 2026/05/31
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private final AdminMapper adminMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${runningerrands.admin.init.username:admin}")
    private String defaultUsername;

    @Value("${runningerrands.admin.init.password:admin}")
    private String defaultPassword;

    @Value("${runningerrands.admin.init.name:系统管理员}")
    private String defaultName;

    @Override
    public void run(ApplicationArguments args) {
        boolean exists = adminMapper.exists(new LambdaQueryWrapper<Admin>()
                .eq(Admin::getUsername, defaultUsername));
        if (exists) {
            log.info("管理员 {} 已存在，跳过初始化", defaultUsername);
            return;
        }

        Admin admin = new Admin();
        admin.setUsername(defaultUsername);
        admin.setName(defaultName);
        admin.setPassword(passwordEncoder.encode(defaultPassword));
        admin.setPhone("");
        admin.setSex("无");
        admin.setIdNumber("");
        admin.setRole(1);
        admin.setStatus(1);
        admin.setLastLoginTime(LocalDateTime.now());
        admin.setCreatedAt(LocalDateTime.now());

        adminMapper.insert(admin);
        log.info("已创建默认超级管理员: {}", defaultUsername);
    }
}
