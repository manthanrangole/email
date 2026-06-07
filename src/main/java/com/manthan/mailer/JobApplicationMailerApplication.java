package com.manthan.mailer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class JobApplicationMailerApplication {

	public static void main(String[] args) {
		SpringApplication.run(JobApplicationMailerApplication.class, args);
	}

}
