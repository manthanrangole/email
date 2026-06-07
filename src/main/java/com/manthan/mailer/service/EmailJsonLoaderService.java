package com.manthan.mailer.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manthan.mailer.model.HrContact;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Parses the HR contacts JSON file (either uploaded or pre-placed in resources).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmailJsonLoaderService {

    private final ObjectMapper objectMapper;

    /**
     * Parse HR contacts from an uploaded MultipartFile.
     */
    public List<HrContact> loadFromMultipartFile(MultipartFile file) throws IOException {
        log.info("Parsing HR contacts from uploaded file: {}", file.getOriginalFilename());
        try (InputStream inputStream = file.getInputStream()) {
            List<HrContact> contacts = objectMapper.readValue(inputStream, new TypeReference<>() {});
            log.info("Parsed {} HR contacts from file", contacts.size());
            return contacts;
        }
    }

    /**
     * Parse HR contacts from a file path on disk.
     */
    public List<HrContact> loadFromFile(File file) throws IOException {
        log.info("Parsing HR contacts from file: {}", file.getAbsolutePath());
        List<HrContact> contacts = objectMapper.readValue(file, new TypeReference<>() {});
        log.info("Parsed {} HR contacts from file", contacts.size());
        return contacts;
    }

    /**
     * Validate individual email format.
     */
    public boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) return false;
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

}
