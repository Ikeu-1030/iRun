package com.ikeu.server.config;

import com.ikeu.common.properties.AliOssProperties;
import com.ikeu.common.utils.AliOssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云 OSS 配置，创建 AliOssUtil 工具 Bean。
 * 仅当 runningerrands.alioss.access-key-id 有值时生效，无 OSS 环境自动回退本地存储。
 * @author ikeu
 * @since 2025/05/21
 */
@Slf4j
@Configuration
public class OssConfiguration {

    /**
     * 创建阿里云 OSS 工具类 Bean。
     *
     * <p>使用 {@link AliOssProperties} 中的 endpoint、accessKeyId、accessKeySecret、bucketName
     * 构造 {@link AliOssUtil} 实例，通过 @ConditionalOnMissingBean 确保仅在没有自定义 Bean 时才创建。
     * @ConditionalOnProperty 确保仅配置了 runningerrands.alioss.access-key-id 时才加载，
     * 未配置 OSS 的环境（如测试虚拟机）自动跳过，CommonController 回退本地文件存储。
     *
     * @param aliOssProperties OSS 配置属性（从 application.yml 读取）
     * @return AliOssUtil 实例
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "runningerrands.alioss.access-key-id")
    public AliOssUtil aliOssUtil(AliOssProperties aliOssProperties) {
        log.info("开始创建AliOssUtil对象：{}", aliOssProperties);
        return new AliOssUtil(aliOssProperties.getEndpoint(), aliOssProperties.getAccessKeyId(),
                aliOssProperties.getAccessKeySecret(), aliOssProperties.getBucketName());
    }
}
