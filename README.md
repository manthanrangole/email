# 📧 Job Application Mailer

A robust Spring Boot REST API designed to automate the process of bulk-sending personalized job application emails with your CV/Resume attached. It reads recipient details from a JSON list of HR contacts, rates limits requests to avoid spam flags, and runs asynchronously in the background.

---

## 🚀 Quick Start

### 1. Set Up Environment Variables

To protect your personal data and account security, credentials should **never** be hardcoded. Create a `.env` file in the root directory (this is ignored by Git in `.gitignore`):

```bash
# .env file
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-16-character-gmail-app-password
```

> ⚠️ **Important:** Google accounts require a **Gmail App Password** instead of your regular password:
> 1. Go to your **Google Account** -> **Security**.
> 2. Enable **2-Step Verification** if not already active.
> 3. Search or navigate to **App Passwords**.
> 4. Generate a new app password for "Mail" (copy the 16-character code).

### 2. Configure Your CV/Resume
1. Place your resume PDF in the directory: `src/main/resources/cv/`
2. Name it `ResumeManthan.pdf` (or edit the filename mapping in `src/main/resources/application.yml` under `app.cv-path`).

### 3. Run the Application
You can run the application using Maven. If port `8080` is occupied on your system, you can start the application on port `8081` (or any other port) by passing it as an argument:

```bash
# Set variables and run on default port (8080)
export MAIL_USERNAME=your-email@gmail.com
export MAIL_PASSWORD=your-app-password
mvn spring-boot:run

# Or run directly on port 8081 with command arguments
mvn spring-boot:run -Dspring-boot.run.arguments="--MAIL_USERNAME=your-email@gmail.com --MAIL_PASSWORD=your-app-password --server.port=8081"
```

### 4. Interactive API Documentation (Swagger UI)
Once started, you can explore, test, and trigger the APIs directly through the Swagger UI:
* **Port 8080 (Default):** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
* **Port 8081:** [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html)

---

## 📋 API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/email/health` | Check service status & CV attachment readiness |
| `POST` | `/api/email/upload-and-send` | Upload `hr_emails.json` -> Parse -> Send emails (Synchronous) |
| `POST` | `/api/email/upload-and-send-async` | Upload `hr_emails.json` -> Queue send in background (Asynchronous) |
| `POST` | `/api/email/send-from-file` | Use pre-placed `hr_emails.json` in project resources |
| `GET` | `/api/email/status` | Fetch the status/report of the last bulk send operation |

---

## 📄 HR Contacts JSON Format
To send bulk emails, format your contacts in JSON like this:
```json
[
  {
    "name": "HR Manager",
    "email": "hr@company.com",
    "company": "TechCorp Solutions"
  },
  {
    "name": "Talent Acquisition",
    "email": "careers@example.com",
    "company": "DesignStudio"
  }
}
```
Place the file at `src/main/resources/data/hr_emails.json` or upload it directly via the Swagger UI.

---

## 🧪 Testing via cURL (Example with Port 8081)

```bash
# 1. Health check
curl http://localhost:8081/api/email/health

# 2. Upload and send bulk (Synchronous)
curl -X POST http://localhost:8081/api/email/upload-and-send \
  -F "file=@hr_emails.json"

# 3. Upload and send bulk (Asynchronous - returns immediately)
curl -X POST http://localhost:8081/api/email/upload-and-send-async \
  -F "file=@hr_emails.json"

# 4. Check background sending status
curl http://localhost:8081/api/email/status
```

---

## 🔒 Security & Best Practices
- **No Hardcoded Credentials:** The `.gitignore` file is configured to exclude all `.env` files, PDF documents, and target compilation output.
- **Rate Limiting:** A delay (default 1000ms) is implemented between sending each email to comply with Google SMTP spam guidelines.
- **Asynchronous Execution:** Async bulk-mailing handles large recipient files without blocking the HTTP request thread.

---

## 🛠️ Tech Stack
* **Language:** Java 17
* **Framework:** Spring Boot 3.3
* **Email Sender:** Spring Boot Starter Mail (JavaMailSender)
* **API Documentation:** SpringDoc OpenAPI / Swagger UI
* **Build Tool:** Maven

---

## ✉️ Author
**Manthan Rangole**  
📞 7337852017  
📧 manthanvr12@gmail.com  
🔗 [LinkedIn](https://www.linkedin.com/in/manthan-rangole/)
