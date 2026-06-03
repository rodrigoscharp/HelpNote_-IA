package com.example.HelpNote;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@SpringBootApplication
@EnableAsync
public class HelpnoteIaApplication {

	public static void main(String[] args) {
		SpringApplication.run(HelpnoteIaApplication.class, args);
	}

	@Bean(name = "aiTaskExecutor")
	public Executor aiTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2);
		executor.setMaxPoolSize(5);
		executor.setQueueCapacity(25);
		executor.setThreadNamePrefix("ai-async-");
		executor.initialize();
		return executor;
	}
}
