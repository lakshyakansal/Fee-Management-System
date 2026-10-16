<div align="center">

# 💰 Fee Management System — Student Fee Collection Web App

**A responsive web application for managing students, fees, payments and dues, with a full activity log**

[![Java](https://img.shields.io/badge/Java-17+-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-Fees%20%26%20Payments-4479A1?style=flat-square&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![MongoDB](https://img.shields.io/badge/MongoDB-Activity%20Log-47A248?style=flat-square&logo=mongodb&logoColor=white)](https://www.mongodb.com/)
[![Maven](https://img.shields.io/badge/Maven-Build%20Tool-C71A36?style=flat-square&logo=apachemaven&logoColor=white)](https://maven.apache.org/)

</div>

---

## 📋 Table of Contents

1. [What is this Project?](#-what-is-this-project)
2. [Key Features](#-key-features)
3. [How it Works — End-to-End Flow](#-how-it-works--end-to-end-flow)
4. [Architecture Overview](#-architecture-overview)
5. [Project Structure](#-project-structure)
6. [Database Design](#-database-design)
7. [REST API Reference](#-rest-api-reference)
8. [Installation & Running](#-installation--running)
9. [Tech Stack Summary](#-tech-stack-summary)
10. [User Roles](#-user-roles)
11. [Troubleshooting](#-troubleshooting)
12. [Limitations](#-limitations)
13. [Future Scope](#-future-scope)

---

## 🎯 What is this Project?

The **Fee Management System** is a Java web application that replaces paper registers and spreadsheets for collecting student fees. It records every student and payment in **MySQL**, works out what each student still owes, and keeps a searchable activity trail in **MongoDB**.

The system covers four areas:

- 🧑‍🎓 **Student records** — add students with a course and total fee.
- 💳 **Fee collection** — accept full or partial payments by several modes.
- 📊 **Dashboard** — live totals for students, fees, collected and pending amounts.
- 🕒 **Activity log** — every addition, deletion and payment is logged in MongoDB.

Built with **Java 17 and Spring Boot** on the backend, a **plain HTML/CSS/JavaScript** frontend served by the same app, and two databases, each used for what it does best.

---

## ✨ Key Features

| Feature | Details |
|---|---|
| 🧑‍🎓 **Student Management** | Add and delete students with name, email, course and total fee |
| 💳 **Fee Collection** | Partial or full payments by Cash, UPI, Card or Bank Transfer |
| 🛡️ **Overpayment Protection** | A payment above the outstanding due is rejected with a clear message |
| 📊 **Live Dashboard** | Total students, total fees, amount collected and amount pending |
| 🏷️ **Payment Status** | Each student is marked `PAID`, `PARTIAL` or `UNPAID` automatically |
| 🕒 **Activity Log** | Latest 20 actions shown on the page, stored in MongoDB |
| 📱 **Responsive UI** | Works on phones, tablets and desktops, with automatic dark mode |
| 🔒 **Data Safety** | Input validation, HTML escaping in the UI, and deletion of a student's payments with the student |

---

## 🔄 How it Works — End-to-End Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          FEE COLLECTION FLOW                            │
└─────────────────────────────────────────────────────────────────────────┘

1. ADD STUDENT
   ├── "Add Student" form — name, email, course, total fee
   └── Saved in MySQL; an entry is written to the MongoDB activity log
         │
         ▼
2. RECORD PAYMENT
   ├── Choose a student who still has a balance, enter amount and mode
   ├── System calculates the current due = total fee − payments so far
   ├── Amount above the due  → rejected with an error message
   └── Valid amount          → payment saved and logged
         │
         ▼
3. DASHBOARD UPDATES
   ├── Student row shows new Paid / Due values and status tag
   └── Summary cards refresh: collected and pending totals
         │
         ▼
4. FOLLOW UP
   ├── Spot unpaid students by the status tag in the table
   ├── GET /api/students/{id}/payments  — payment history of a student
   └── Activity panel shows who paid what and when
```

---

## 🏗️ Architecture Overview

The app is a single Spring Boot service that serves both the API and the web page. It reads and writes two databases.

```
┌──────────────────────────────────────────────────────────────────────┐
│                  BROWSER  (static/index.html)                        │
│        HTML + CSS + JavaScript  →  fetch() calls to /api/...         │
└───────────────────────────┬──────────────────────────────────────────┘
                            │ HTTP / JSON
                            ▼
┌──────────────────────────────────────────────────────────────────────┐
│                  FeeController.java  (REST API)                      │
│                                                                      │
│  Students            Payments               Reporting                │
│  ─────────────       ─────────────────      ─────────────            │
│  list()              pay()                  stats()                  │
│  add()               history()              logs()                   │
│  delete()                                                            │
└──────────────┬─────────────────────────────────────┬─────────────────┘
               │ Spring Data JPA                     │ Spring Data MongoDB
               ▼                                     ▼
┌──────────────────────────────┐      ┌──────────────────────────────┐
│        MySQL: fee_db         │      │     MongoDB: fee_logs        │
│     student · payment        │      │       activity_logs          │
└──────────────────────────────┘      └──────────────────────────────┘
```

---

## 📁 Project Structure

```
fee-management/
│
├── pom.xml                         # Maven build and dependencies
├── schema.sql                      # Reference SQL (tables are auto-created)
├── README.md                       # Project documentation
│
└── src/main/
    ├── java/com/fees/
    │   ├── FeeApp.java             # Application entry point
    │   ├── Student.java            # MySQL entity — student and total fee
    │   ├── Payment.java            # MySQL entity — each payment received
    │   ├── ActivityLog.java        # MongoDB document — audit entry
    │   ├── StudentRepo.java        # JPA repository for students
    │   ├── PaymentRepo.java        # JPA repository + paid-amount query
    │   ├── LogRepo.java            # MongoDB repository for logs
    │   └── FeeController.java      # REST endpoints and business rules
    │
    └── resources/
        ├── application.properties  # Database connections and port
        └── static/index.html       # Responsive frontend (single page)
```

---

## 🗄️ Database Design

### MySQL (`fee_db`)

| Table | Description | Key Fields |
|---|---|---|
| **student** | Enrolled students and their total fee | `id`, `name`, `email`, `course`, `total_fee` |
| **payment** | Every fee payment received | `id`, `student_id`, `amount`, `method`, `paid_on` |

```
student (id, name, email, course, total_fee)
   ▲
   │ student_id
payment (id, student_id, amount, method, paid_on)
```

### MongoDB (`fee_logs`)

| Collection | Description | Key Fields |
|---|---|---|
| **activity_logs** | Audit trail of every action | `_id`, `action`, `detail`, `time` |

Actions recorded: `STUDENT_ADDED`, `STUDENT_DELETED` and `PAYMENT`.

**Design decisions**

- **Dues are calculated, not stored.** Due = `total_fee − SUM(payments)`, so totals never drift out of sync.
- **Two databases, two jobs.** MySQL holds the money records that need accuracy; MongoDB holds the flexible, append-only log.
- **Money uses `DECIMAL(12,2)` and `BigDecimal`** to avoid floating-point rounding errors.
- **Status is derived:** `PAID` when due is zero, `PARTIAL` when something is paid, otherwise `UNPAID`.
- **Tables are created automatically** by Hibernate (`ddl-auto=update`). `schema.sql` is provided as a reference and also adds a foreign key with `ON DELETE CASCADE` if you create the tables by hand.

---

## 🔌 REST API Reference

| Method | Endpoint | Purpose |
|---|---|---|
| `GET` | `/api/students` | List students with paid, due and status |
| `POST` | `/api/students` | Add a student |
| `DELETE` | `/api/students/{id}` | Delete a student and their payments |
| `POST` | `/api/payments` | Record a payment (rejects overpayment) |
| `GET` | `/api/students/{id}/payments` | Payment history of one student |
| `GET` | `/api/stats` | Dashboard totals |
| `GET` | `/api/logs` | Latest 20 activity log entries |

**Example — add a student**

```bash
curl -X POST http://localhost:8080/api/students \
  -H "Content-Type: application/json" \
  -d '{"name":"Asha Verma","email":"asha@example.com","course":"PG-DAC","totalFee":95000}'
```

**Example — record a payment**

```bash
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{"studentId":1,"amount":50000,"method":"UPI"}'
```

---

## 🚀 Installation & Running

### Prerequisites

| Tool | Purpose |
|---|---|
| JDK 17 or later | Compile and run the Java code |
| Maven 3.6+ | Build and dependency management |
| MySQL 8.x | Fees and payments database |
| MongoDB 6+ | Activity log database |
| MySQL Workbench / MongoDB Compass (optional) | Viewing the data visually |
| VS Code, IntelliJ or Eclipse | Editing the project |

### Steps

```bash
# 1. Start MySQL (port 3306) and MongoDB (port 27017)

# 2. Set your MySQL username and password in
#    src/main/resources/application.properties

# 3. Build and run
mvn spring-boot:run

# 4. Open the app
#    http://localhost:8080
```

The `fee_db` database is created automatically if it does not exist, and the tables appear on first start.

### Configuration

| Property | Default |
|---|---|
| `spring.datasource.url` | `jdbc:mysql://localhost:3306/fee_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC` |
| `spring.datasource.username` | `root` |
| `spring.datasource.password` | `root` |
| `spring.data.mongodb.uri` | `mongodb://localhost:27017/fee_logs` |
| `server.port` | `8080` |

You can also override any of these without editing the file, for example:

```bash
# Linux / macOS
export SPRING_DATASOURCE_PASSWORD=yourpassword
# Windows (PowerShell)
$env:SPRING_DATASOURCE_PASSWORD="yourpassword"
```

### Typical first session

1. Open `http://localhost:8080`.
2. Use **Add Student** to enrol a student with a course and total fee.
3. Use **Record Payment** to pick the student, enter an amount and choose a mode.
4. Watch the status tag, the summary cards and the activity panel update.

---

## 🛠️ Tech Stack Summary

| Layer | Technology | Purpose |
|---|---|---|
| **Language** | Java 17 | Application logic |
| **Framework** | Spring Boot 3.3.4 (Web, Data JPA, Data MongoDB) | REST API and database access |
| **Relational DB** | MySQL 8 + Connector/J | Students and payments |
| **Document DB** | MongoDB | Activity log |
| **Frontend** | HTML, CSS, vanilla JavaScript | Responsive single-page interface |
| **Build Tool** | Maven | Dependencies and running the app |
| **Editor** | VS Code / IntelliJ / Eclipse | Development environment |

---

## 👥 User Roles

The current version has a single role with full access and no login.

### Operator (Accountant / Admin Staff)
- Adds and deletes students
- Records fee payments
- Reviews dues, status and totals on the dashboard
- Reads the activity log

---

## 🧰 Troubleshooting

| Problem | Fix |
|---|---|
| `Communications link failure` | MySQL is not running or the URL/port is wrong |
| `Access denied for user 'root'` | Fix the username and password in `application.properties` |
| `MongoTimeoutException` / activity panel empty | Start MongoDB on port 27017 or change `spring.data.mongodb.uri` |
| `Port 8080 was already in use` | Change `server.port` or stop the other program |
| `release version 17 not supported` | Install JDK 17+ and point `JAVA_HOME` to it |
| `Amount exceeds due` | The payment is larger than the remaining balance; enter a smaller amount |
| Blank page | Make sure you open `http://localhost:8080`, not the HTML file directly |

---

## ⚠️ Limitations

- No login or role-based access
- One total fee per student, with no instalment schedule or due dates
- No late-fee or fine calculation
- No printable receipts, void-payment or CSV export
- Course is a plain text field, with no separate course table
- Overpayment check is not locked, so two simultaneous payments for one student could both pass
- Payment history is available through the API only, not yet on the page
- Deleting a student permanently removes their payments (the deletion is only noted in the log)

---

## 🔮 Future Scope

- Login with admin and accountant roles (Spring Security)
- Course master table with default fees
- PDF receipts with an institute logo
- Void payment with a reason, kept in the audit log
- Instalment schedules, due dates and automatic reminders
- Late-fee calculation
- Locking or optimistic versioning to prevent simultaneous overpayment
- CSV and Excel export
- Charts and course-wise analytics on the dashboard
- SMS and email receipts for students and parents
