package com.example.ex.config;

import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class SpringConfig {
    
    private final JobLauncher jobLauncher;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    public SpringConfig(JobLauncher jobLauncher, JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        this.jobLauncher = jobLauncher;
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
    }
}
