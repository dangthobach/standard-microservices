package com.enterprise.business.config;

import com.enterprise.business.repository.base.SoftDeleteRepositoryImpl;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.enterprise.business.repository.RepositoryPackageMarker;

import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Cấu hình nền tảng Auditing và Repositories cho toàn bộ Hệ thống
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackageClasses = {
        RepositoryPackageMarker.class }, repositoryBaseClass = SoftDeleteRepositoryImpl.class)
public class JpaConfig {
}
