# Email Notification Service

A lightweight **Email Notification microservice** built with **Spring Boot** and **Apache Kafka**. Listens to domain-specific Kafka topics published by other services and dispatches transactional emails accordingly.

---

## Highlights

- **Event-driven architecture** — decoupled from the User Auth Service via Kafka; email dispatch requires zero changes to upstream services
- **Topic-per-action design** — each user action has its own Kafka topic, so any future service can subscribe to exactly the events it cares about without consuming irrelevant noise
- **Per-action sender identity** — signup and security emails can be dispatched from different addresses by swapping environment variables, no code changes required
- **Credentials fully externalized** — SMTP credentials and sender addresses loaded from environment variables at runtime

---

## Architecture

```
User Auth Service
  │  (constructs full email payload per action — recipient, subject, body)
  │
  ├── publishes → user-signup
  ├── publishes → user-login
  ├── publishes → user-update-profile
  └── publishes → user-reset-password
                        │
                  Kafka Broker
                        │
              Email Notification Service
              (consumes payload, dispatches as-is)
                        │
                   SMTP / Mail Server
                        │
                     User inbox
```



## Design Decisions

- **Why topic-per-action over a single topic?**<br><br>
A single `user-notifications` topic would work today, but any downstream service (e.g. an analytics service etc) that only cares about profile updates would be forced to consume all events and filter them out. With one topic per action, services subscribe to exactly what they need — no filtering, no wasted consumption, and no coupling between unrelated event types.<br><br>

- **Why does the User Auth Service own the email payload?**<br><br>
 Each action (signup, login, reset, profile update) has a different email body. Since the User Auth Service has full context about what happened and to whom, it constructs the complete payload — recipient, subject, and body — before publishing. The Email Service stays intentionally thin, just consuming and dispatching. This means adding a new notification type requires zero changes to the Email Service.

---



## Kafka Topics

| Topic | Trigger | Email sent |
|---|---|---|
| `user-signup` | New user registration | Welcome email |
| `user-login` | Successful login | Login alert |
| `user-update-profile` | Profile details updated | Update confirmation |
| `user-reset-password` | Password reset completed | Security alert |

---

## Environment Variables

| Variable | Description |
|---|---|
| `SIGNUP_EMAIL` | Sender address for signup emails |
| `SIGNUP_PASSWORD` | SMTP password for signup sender |
| `SECURITY_EMAIL` | Sender address for login, reset, and update emails |
| `SECURITY_PASSWORD` | SMTP password for security sender |



---

## Tech Stack

| Layer | Technology |
|-------|------------|
| Framework | Spring Boot |
| Messaging | Apache Kafka |
| Email | Spring Mail (JavaMailSender) |
| Build Tool | Maven |

---

## Getting Started

```bash
# Clone the repository
git clone https://github.com/your-username/email-notification-service.git

# Set required environment variables
export SIGNUP_EMAIL=signup@yourdomain.com
export SIGNUP_PASSWORD=your_smtp_password
export SECURITY_EMAIL=security@yourdomain.com
export SECURITY_PASSWORD=your_smtp_password

# Run the service
./mvnw spring-boot:run
```

> Ensure a Kafka broker is running and the topics exist before starting the service. Topics can be auto-created or pre-provisioned depending on your Kafka config.

---

## Project Structure

```
src/
├── service/
│   └── EmailService.java          # Single service, one @KafkaListener per topic
│       ├── sendEmailOnSignup()             # listens to user-signup
│       ├── sendEmailOnLogin()              # listens to user-login
│       ├── sendEmailOnUpdateProfile()      # listens to user-update-profile
│       └── sendEmailOnResetPassword()      # listens to user-reset-password
├── config/
│   └── KafkaConsumerConfig.java
└── util/
    └── TLSEmail.java     # Send Email in Java SMTP with TLS Authentication
```

---

## Roadmap

- [ ] Content-rich emails (OTPs, reset links, account details)
- [ ] Email delivery status tracking
- [ ] Retry logic in case of delivery failure

---

## Related Services

- [User Auth Service](https://github.com/your-username/user-auth-service) — publishes events to the Kafka topics this service consumes

---
