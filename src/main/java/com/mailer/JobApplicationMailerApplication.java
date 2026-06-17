package com.mailer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@SpringBootApplication
@EnableAsync
public class JobApplicationMailerApplication {

	public static void main(String[] args) {
		loadEnv();
		SpringApplication.run(JobApplicationMailerApplication.class, args);
	}

	private static void loadEnv() {
		try {
			java.nio.file.Path envPath = Paths.get(".env");
			if (!Files.exists(envPath) && Files.exists(Paths.get("../.env"))) {
				envPath = Paths.get("../.env");
			}
			if (Files.exists(envPath)) {
				List<String> lines = Files.readAllLines(envPath);
				for (String line : lines) {
					line = line.trim();
					if (line.isEmpty() || line.startsWith("#")) {
						continue;
					}
					int eqIdx = line.indexOf('=');
					if (eqIdx > 0) {
						String key = line.substring(0, eqIdx).trim();
						String value = line.substring(eqIdx + 1).trim();
						if ((value.startsWith("\"") && value.endsWith("\"")) ||
								(value.startsWith("'") && value.endsWith("'"))) {
							value = value.substring(1, value.length() - 1);
						}
						System.setProperty(key, value);
					}
				}
			}
		} catch (IOException e) {
			System.err.println("Failed to load .env file: " + e.getMessage());
		}
	}

}
