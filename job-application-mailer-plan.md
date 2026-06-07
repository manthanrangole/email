# 📧 Job Application Email Sender — Spring Boot

A Spring Boot REST API that reads a JSON file containing HR email addresses and bulk-sends job application emails with your CV attached.

---

## 📁 Project Structure

```
job-application-mailer/
├── src/
│   └── main/
│       ├── java/com/manthan/mailer/
│       │   ├── JobApplicationMailerApplication.java
│       │   ├── controller/
│       │   │   └── EmailController.java
│       │   ├── service/
│       │   │   ├── EmailService.java
│       │   │   └── EmailJsonLoaderService.java
│       │   ├── model/
│       │   │   ├── EmailRequest.java
│       │   │   └── HrContact.java
│       │   ├── config/
│       │   │   └── MailConfig.java
│       │   └── dto/
│       │       └── BulkEmailResponse.java
│       └── resources/
│           ├── application.yml
│           ├── cv/
│           │   └── Manthan_Rangole_CV.pdf        ← your CV goes here
│           └── data/
│               └── hr_emails.json                ← uploaded via API or pre-placed
├── pom.xml
└── README.md
```

---

## 🔧 Phase 1 — Dependencies (`pom.xml`)

```xml
<!-- Core -->
spring-boot-starter-web
spring-boot-starter-mail
spring-boot-starter-validation

<!-- File Handling -->
commons-io

<!-- JSON Parsing -->
jackson-databind  <!-- auto-included with spring-boot-starter-web -->

<!-- Lombok -->
lombok

<!-- Optional: Swagger UI for testing -->
springdoc-openapi-starter-webmvc-ui
```

---

## ⚙️ Phase 2 — Configuration (`application.yml`)

```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${MAIL_USERNAME}        # your Gmail
    password: ${MAIL_PASSWORD}        # Gmail App Password
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true

app:
  cv-path: classpath:cv/Manthan_Rangole_CV.pdf
  email:
    subject: "Application - Java Backend Developer"
    sender-name: "Manthan Rangole"
```

> ⚠️ **Important:** Use Gmail **App Password** (not your real password).
> Enable 2FA → Generate App Password in **Google Account → Security → App Passwords**.

---

## 📦 Phase 3 — Data Model

### `hr_emails.json` — uploaded by you
```json
[
  {
    "name": "HR Team",
    "email": "hr@company1.com",
    "company": "TechCorp Pvt Ltd"
  },
  {
    "name": "Talent Acquisition",
    "email": "careers@company2.com",
    "company": "Infosys"
  }
]
```

### `HrContact.java`
```java
@Data
public class HrContact {
    private String name;
    private String email;
    private String company;
}
```

---

## 🌐 Phase 4 — API Design

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/email/upload-and-send` | Upload JSON file → parse → send all emails |
| `POST` | `/api/email/send-from-file` | Use pre-placed JSON in resources → send all |
| `GET`  | `/api/email/status`          | Get last bulk send summary |

### Request — `upload-and-send`
```
Content-Type: multipart/form-data
Field: file → hr_emails.json
```

### Response
```json
{
  "totalEmails": 10,
  "successCount": 9,
  "failedCount": 1,
  "failedEmails": ["hr@broken.com"],
  "timeTaken": "4.2s"
}
```

---

## 🔁 Phase 5 — Core Logic Flow

```
POST /upload-and-send
        │
        ▼
[EmailController] receives MultipartFile (JSON)
        │
        ▼
[EmailJsonLoaderService] parses JSON → List<HrContact>
        │
        ▼
[EmailService] loops through each HrContact
        │
        ├── Build MimeMessage
        │     ├── To:         hr.email
        │     ├── Subject:    "Application - Java Backend Developer"
        │     ├── Body:       HTML formatted email body (personalized per company)
        │     └── Attachment: Manthan_Rangole_CV.pdf (from classpath)
        │
        ├── Send via JavaMailSender
        │
        └── Track success / failure per email
        │
        ▼
Return BulkEmailResponse (success/fail counts + details)
```

---

## 💡 Phase 6 — Key Implementation Details

### Email Body (personalized per company)
```
Hi [HR Name],

I am writing to express my interest in the Java Backend Developer
position at [Company Name].

I have over 3.8 years of experience building scalable backend
applications using Java, Spring Boot, Microservices, Kafka, Redis,
and REST APIs.

...

Please find my resume attached for your review.

Thanks & Regards,
Manthan Rangole
7337852017 | manthanvr12@gmail.com
LinkedIn: https://www.linkedin.com/in/manthan-rangole/
```

### Important Handling

| Concern | Solution |
|---------|----------|
| Rate limiting | `Thread.sleep(1000)` between emails to avoid Gmail spam flags |
| Async sending | `@Async` so the API returns immediately while emails send in background |
| Error isolation | One failed email must NOT stop the rest (try-catch per email) |
| Validation | Validate each email format before attempting send |
| Logging | Log success/failure per recipient with timestamp |

---

## 🔒 Phase 7 — Security Considerations

- Store credentials in **environment variables**, never hardcode in source
- Use `.env` file locally and add it to `.gitignore`
- For production, use **AWS Secrets Manager** or **Spring Vault**

```bash
# .env (local only — never commit this)
MAIL_USERNAME=manthanvr12@gmail.com
MAIL_PASSWORD=your-app-password
```

```
# .gitignore
.env
*.env
```

---

## 🗂️ Phase 8 — Implementation Order (Step-by-Step)

```
Step 1  → Set up project with Spring Initializr (spring-boot-starter-web, mail, validation)
Step 2  → Configure application.yml + Gmail App Password
Step 3  → Create HrContact model + JSON parser service
Step 4  → Build EmailService with MimeMessage + CV attachment
Step 5  → Create EmailController with file upload endpoint
Step 6  → Test with 1–2 emails first
Step 7  → Add @Async + rate limiting (Thread.sleep) for bulk send
Step 8  → Add Swagger UI for easy API testing
Step 9  → Add BulkEmailResponse tracking (success/fail per email)
Step 10 → Final testing with full HR list
```

---

## 🚀 Quick Start After Build

```bash
# 1. Set environment variables
export MAIL_USERNAME=manthanvr12@gmail.com
export MAIL_PASSWORD=your-app-password

# 2. Build and run
mvn spring-boot:run

# 3. Open Swagger UI
open http://localhost:8080/swagger-ui.html

# 4. Or test via curl
curl -X POST http://localhost:8080/api/email/upload-and-send \
  -F "file=@hr_emails.json"
```

---

## 📊 Sample BulkEmailResponse

```json
{
  "totalEmails": 25,
  "successCount": 24,
  "failedCount": 1,
  "failedEmails": [
    "invalid@baddomain.xyz"
  ],
  "startedAt": "2025-06-05T10:30:00",
  "completedAt": "2025-06-05T10:30:52",
  "timeTaken": "52.3s"
}
```

---

## 📌 Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Java 17 |
| Framework | Spring Boot 3.x |
| Email | Spring Boot Starter Mail (JavaMailSender) |
| JSON Parsing | Jackson ObjectMapper |
| File Upload | Spring MultipartFile |
| Async | `@Async` + `ThreadPoolTaskExecutor` |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Build Tool | Maven |

---

## ✉️ Author

**Manthan Rangole**
📞 7337852017
📧 manthanvr12@gmail.com
🔗 [linkedin.com/in/manthan-rangole](https://www.linkedin.com/in/manthan-rangole/)
