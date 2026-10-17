<div align="center">

# 💰 Fee Management System — Student Fee Collection & Receipts

**A responsive web application for managing courses, student fees, payments, receipts and dues, with a full activity log**

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

The **Fee Management System** is a Java web application that replaces paper registers and spreadsheets for collecting student fees. It records every course, student and payment in **MySQL**, works out what each student still owes, prints a receipt for every payment, and keeps an activity trail in **MongoDB**.

The system covers five areas:

- 📚 **Course setup** — define courses and their fees.
- 🧑‍🎓 **Student enrolment** — enrol students with a course, phone and optional discount or scholarship.
- 💳 **Fee collection** — accept full or partial payments and issue receipts.
- 📊 **Dashboard** — live totals, a searchable student table and recent payments.
- 🕒 **Activity log** — every course, student and payment action is logged in MongoDB.

Built with **Java 17 and Spring Boot** on the backend, a **plain HTML/CSS/JavaScript** frontend served by the same app, and two databases, each used for what it does best.

---

## ✨ Key Features

| Feature | Details |
|---|---|
| 📚 **Course Management** | Add a course, or enter an existing course name to update its fee |
| 🧑‍🎓 **Student Enrolment** | Student ID auto-generated (`S0001`, `S0002` ...), with course, phone, fee and discount |
| 💸 **Discount / Scholarship** | Payable amount = fee − discount, used everywhere dues are calculated |
| 💳 **Fee Collection** | Partial or full payments by Cash, UPI, Card, Bank Transfer or Cheque, with an optional reference number |
| 🛡️ **Overpayment Protection** | A payment above the outstanding due is rejected. The student row is locked (`SELECT ... FOR UPDATE`) inside a transaction, so two users cannot overpay at once |
| 🧾 **Receipts** | Numbered receipt (`R00001`, `R00002` ...) with amount paid, total paid and balance due, printable from the browser |
| ↩️ **Void Payment** | Remove a wrong payment and the amount returns to the student's due automatically |
| 📤 **CSV Export** | Download all payments as `payments.csv` for Excel or accounts |
| 🔍 **Search** | Filter the student table by name or course |
| 📊 **Live Dashboard** | Total students, total payable, amount collected and amount pending |
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

1. SET UP COURSES
   └── "Add / Update Course" form — name and fee (existing name = fee update)
         │
         ▼
2. ENROL STUDENT
   ├── "Add Student" form — name, phone, course, discount
   ├── Fee is copied from the course; Student ID (S0001, S0002 ...) is issued
   └── Saved in MySQL; an entry is written to the MongoDB activity log
         │
         ▼
3. COLLECT FEE
   ├── "Record Payment" — student, amount, mode, reference
   ├── System locks the student row and calculates the current due
   ├── Amount above the due  → rejected, nothing saved
   └── Valid amount          → payment saved, committed and logged
         │
         ▼
4. RECEIPT
   └── Receipt (R00001, R00002 ...) shown with total paid and balance due
         │
         ▼
5. FOLLOW UP
   ├── Status tags and search in the student table show who still owes
   ├── Void a wrong entry from Recent Payments
   ├── Export all payments to CSV
   └── Activity panel shows who did what and when
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
│  Courses        Students        Payments           Reporting         │
│  ───────────    ───────────     ─────────────      ───────────       │
│  courses()      list()          pay()  (locked)    stats()           │
│  addCourse()    add()           recent()           logs()            │
│                 delete()        voidPayment()                        │
│                                 receipt()                            │
│                                 export()                             │
└──────────────┬─────────────────────────────────────┬─────────────────┘
               │ Spring Data JPA                     │ Spring Data MongoDB
               ▼                                     ▼
┌──────────────────────────────┐      ┌──────────────────────────────┐
│        MySQL: fee_db         │      │     MongoDB: fee_logs        │
│  course · student · payment  │      │       activity_logs          │
└──────────────────────────────┘      └──────────────────────────────┘
```

---

## 📁 Project Structure

```
fee-management/
│
├── pom.xml                         # Maven build and dependencies
├── schema.sql                      # Reference SQL with constraints and seed courses
├── README.md                       # Project documentation
│
└── src/main/
    ├── java/com/fees/
    │   ├── FeeApp.java             # Application entry point
    │   ├── Course.java             # MySQL entity — course and standard fee
    │   ├── Student.java            # MySQL entity — student, fee and discount
    │   ├── Payment.java            # MySQL entity — each payment received
    │   ├── ActivityLog.java        # MongoDB document — audit entry
    │   ├── CourseRepo.java         # JPA repository for courses
    │   ├── StudentRepo.java        # JPA repository + row-locking query
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
| **course** | Course names and standard fees | `id`, `name` (unique), `fee` |
| **student** | Enrolled students with their own fee and discount | `id`, `name`, `phone`, `email`, `course`, `course_id`, `total_fee`, `discount` |
| **payment** | Every fee payment received | `id`, `student_id`, `amount`, `method`, `reference`, `paid_on` |

**Relationships**

- **Course → Student**: one-to-many (a course has many students)
- **Student → Payment**: one-to-many (a student can pay in several instalments)

```
course (id, name, fee)
   ▲
   │ course_id
student (id, name, phone, email, course, course_id, total_fee, discount)
   ▲
   │ student_id
payment (id, student_id, amount, method, reference, paid_on)
```

### MongoDB (`fee_logs`)

| Collection | Description | Key Fields |
|---|---|---|
| **activity_logs** | Audit trail of every action | `_id`, `action`, `detail`, `time` |

Actions recorded: `COURSE_SAVED`, `STUDENT_ADDED`, `STUDENT_DELETED`, `PAYMENT` and `PAYMENT_VOIDED`.

**Design decisions**

- **Dues are calculated, not stored.** Payable = `total_fee − discount`, Due = `payable − SUM(payments)`. Voiding a payment restores the due automatically and totals never drift out of sync.
- **Each student keeps their own fee.** The fee is copied from the course at enrolment, so a later change to the course fee does not alter existing students' dues.
- **Two databases, two jobs.** MySQL holds the money records that need accuracy; MongoDB holds the flexible, append-only log.
- **Money uses `DECIMAL(12,2)` and `BigDecimal`** to avoid floating-point rounding errors.
- **Overpayment is prevented with a row lock.** The payment runs in a transaction that locks the student row until commit.
- **Status is derived:** `PAID` when due is zero, `PARTIAL` when something is paid, otherwise `UNPAID`.
- **Student and receipt numbers** come from auto-increment IDs and are shown as `S0001` and `R00001`.
- **Tables are created automatically** by Hibernate (`ddl-auto=update`). `schema.sql` is optional: use it if you want the `CHECK` constraints, foreign keys with `ON DELETE CASCADE`, and the two seed courses (PG-DAC and PG-DBDA).

---

## 🔌 REST API Reference

| Method | Endpoint | Purpose |
|---|---|---|
| `GET` | `/api/courses` | List courses |
| `POST` | `/api/courses` | Add a course, or update its fee if the name exists |
| `GET` | `/api/students` | List students with payable, paid, due and status |
| `POST` | `/api/students` | Add a student (fee copied from the chosen course) |
| `DELETE` | `/api/students/{id}` | Delete a student and their payments |
| `GET` | `/api/students/{id}/payments` | Payment history of one student |
| `POST` | `/api/payments` | Record a payment (row-locked, rejects overpayment) |
| `GET` | `/api/payments` | 20 most recent payments |
| `DELETE` | `/api/payments/{id}` | Void a payment |
| `GET` | `/api/payments/{id}/receipt` | Receipt details for one payment |
| `GET` | `/api/payments/export` | Download all payments as CSV |
| `GET` | `/api/stats` | Dashboard totals |
| `GET` | `/api/logs` | Latest 20 activity log entries |

**Example — add a course**

```bash
curl -X POST http://localhost:8080/api/courses \
  -H "Content-Type: application/json" \
  -d '{"name":"PG-DAC","fee":95000}'
```

**Example — add a student with a discount**

```bash
curl -X POST http://localhost:8080/api/students \
  -H "Content-Type: application/json" \
  -d '{"name":"Asha Verma","phone":"9876543210","courseId":1,"discount":5000}'
```

**Example — record a payment**

```bash
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{"studentId":1,"amount":50000,"method":"UPI","reference":"4821937"}'
```

### Sample receipt

```
----------------------------------------
           FEE RECEIPT
----------------------------------------
Receipt no.   : R00001
Date          : 2026-10-17
Student       : Asha Verma (S0001)
Course        : PG-DAC
Mode          : UPI / 4821937
Amount paid   : Rs 50,000.00
Total paid    : Rs 50,000.00
Balance due   : Rs 40,000.00
----------------------------------------
```

---

## 🚀 Installation & Running

### Prerequisites

| Tool | Purpose |
|---|---|
| JDK 17 or later | Compile and run the Java code |
| Maven 3.6+ | Build and dependency management |
| MySQL 8.x | Courses, students and payments database |
| MongoDB 6+ | Activity log database |
| MySQL Workbench / MongoDB Compass (optional) | Viewing the data visually |
| VS Code, IntelliJ or Eclipse | Editing the project |

### Steps

```bash
# 1. Start MySQL (port 3306) and MongoDB (port 27017)

# 2. (Optional) create tables with constraints and seed courses
mysql -u root -p < schema.sql

# 3. Set your MySQL username and password in
#    src/main/resources/application.properties

# 4. Build and run
mvn spring-boot:run

# 5. Open the app
#    http://localhost:8080
```

If you skip step 2, the `fee_db` database and tables are created automatically on first start, but there will be no courses until you add them from the page.

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

### Upgrading from the earlier version

If you already ran the first version, its student rows have no discount value. Either drop the `fee_db` database and let it recreate, or start the app once and then run:

```sql
UPDATE fee_db.student SET discount = 0 WHERE discount IS NULL;
```

### Typical first session

1. Open `http://localhost:8080`.
2. Use **Add / Update Course** to create a course (skip this if you ran `schema.sql`).
3. Use **Add Student**, choose the course and note the student ID shown in the table.
4. Use **Record Payment** to collect a fee. Open the receipt from **Recent Payments**.
5. Check the status tags, summary cards and activity panel.

---

## 🛠️ Tech Stack Summary

| Layer | Technology | Purpose |
|---|---|---|
| **Language** | Java 17 | Application logic |
| **Framework** | Spring Boot 3.3.4 (Web, Data JPA, Data MongoDB) | REST API, transactions and database access |
| **Relational DB** | MySQL 8 + Connector/J | Courses, students and payments |
| **Document DB** | MongoDB | Activity log |
| **Frontend** | HTML, CSS, vanilla JavaScript | Responsive single-page interface |
| **Build Tool** | Maven | Dependencies and running the app |
| **Editor** | VS Code / IntelliJ / Eclipse | Development environment |

---

## 👥 User Roles

The current version has a single role with full access and no login.

### Operator (Accountant / Admin Staff)
- Creates courses and updates their fees
- Enrols and deletes students
- Collects fees and prints receipts
- Voids incorrect payments
- Reviews dues, status and totals, and exports payments to CSV
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
| `Select a course or enter a fee above 0` | Choose a course, or type a fee when no course is selected |
| `Discount must be between 0 and the fee` | The discount is negative or higher than the fee |
| Student list errors after upgrading | Old rows have a null discount; run the `UPDATE` shown in the upgrade section |
| Blank page | Make sure you open `http://localhost:8080`, not the HTML file directly |

---

## ⚠️ Limitations

- No login or role-based access
- One total fee per student, with no instalment schedule or due dates
- No late-fee or fine calculation
- Receipts are shown on screen and printed from the browser, with no PDF file
- No edit-student option; a wrong entry must be deleted and re-added
- Voiding or deleting permanently removes the payment row; only a short note stays in the activity log, and no reason is recorded
- The page shows only the 20 most recent payments, while the CSV contains all of them
- The student table loads every student at once, with no paging
- No SMS or email notifications

---

## 🔮 Future Scope

- Login with admin and accountant roles (Spring Security)
- Edit student details
- PDF receipts with an institute logo
- Void payment with a reason, kept in the audit log
- Instalment schedules, due dates and automatic reminders
- Late-fee calculation
- Excel export and date-range filters
- Charts and course-wise analytics on the dashboard
- Paging for large student lists
- SMS and email receipts for students and parents

<div align="center">

Developed by Lakshya Kansal

</div>
