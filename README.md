<div align="center">

# 💰 Fee Management System — Student Fee Collection & Receipts

**A console-based application for managing courses, student fees, payments, receipts and dues**

[![Java](https://img.shields.io/badge/Java-17+-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![MySQL](https://img.shields.io/badge/MySQL-Database-4479A1?style=flat-square&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![JDBC](https://img.shields.io/badge/JDBC-Connector%2FJ%208.4-005C84?style=flat-square)](https://dev.mysql.com/downloads/connector/j/)
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
7. [Menu Reference](#-menu-reference)
8. [Installation & Running](#-installation--running)
9. [Tech Stack Summary](#-tech-stack-summary)
10. [User Roles](#-user-roles)
11. [Troubleshooting](#-troubleshooting)
12. [Limitations](#-limitations)
13. [Future Scope](#-future-scope)

---

## 🎯 What is this Project?

The **Fee Management System** is a Java application that replaces paper registers and spreadsheets for collecting student fees. It records every course, student and payment in a MySQL database, calculates what each student still owes, and prints a receipt for every payment.

The system covers four areas:

- 📚 **Course setup** — define courses and their fee structure.
- 🧑‍🎓 **Student enrolment** — enrol students with a course, fee and optional discount or scholarship.
- 💳 **Fee collection** — accept full or partial payments and issue receipts.
- 📊 **Reporting** — track dues, review payment history and export data to CSV.

Built with **Java 17** and **JDBC** on the backend and **MySQL 8** for storage. It runs as a menu-driven console program.

---

## ✨ Key Features

| Feature | Details |
|---|---|
| 📚 **Course Management** | Add courses, or update the fee of an existing course (`Add course`, `List courses`) |
| 🧑‍🎓 **Student Enrolment** | Student ID is auto-generated, with course, phone, fee and discount captured at enrolment |
| 💳 **Fee Collection** | Partial or full payments by Cash, UPI, Card, Bank transfer or Cheque |
| 🛡️ **Overpayment Protection** | A payment above the outstanding due is rejected, using a transaction with `SELECT ... FOR UPDATE` so two users cannot overpay at once |
| 🧾 **Receipts** | A printable receipt for every payment showing amount paid, total paid and balance due |
| 📊 **Reports** | Student list with status (Paid, Partial, Unpaid), dues-only report with total outstanding, and full payment history |
| ↩️ **Void Payment** | Remove a payment, and the amount returns to the student's due automatically |
| 📤 **CSV Export** | Write all payments to `payments.csv` for Excel or accounts |
| 🔒 **Data Integrity** | Primary and foreign keys, `NOT NULL` and `CHECK` constraints, `DECIMAL` money values and prepared statements |

---

## 🔄 How it Works — End-to-End Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          FEE COLLECTION FLOW                            │
└─────────────────────────────────────────────────────────────────────────┘

1. SET UP COURSES
   └── Option 1 — add a course and its fee (or update an existing one)
         │
         ▼
2. ENROL STUDENT
   ├── Option 3 — name, phone, course, discount
   └── Fee is copied from the course; Student ID (S0001, S0002 ...) is issued
         │
         ▼
3. COLLECT FEE
   ├── Option 5 — student ID, amount, mode, reference
   ├── System locks the student row and calculates the current due
   ├── Amount above the due  → rejected, transaction rolled back
   └── Valid amount          → payment saved and committed
         │
         ▼
4. RECEIPT
   └── Receipt (R00001, R00002 ...) printed with total paid and balance due
         │
         ▼
5. FOLLOW UP
   ├── Option 9  — dues report, with total outstanding
   ├── Option 6  — payment history
   ├── Option 8  — void a wrong entry
   └── Option 10 — export payments to CSV
```

---

## 🏗️ Architecture Overview

The system is a single Java program that talks to MySQL through JDBC. Business rules such as dues and overpayment checks run in SQL and Java, with no separate server layer.

```
┌──────────────────────────────────────────────────────────────────────┐
│                      CONSOLE (Menu-driven UI)                        │
│            Scanner input  →  numbered menu  →  printed output        │
└───────────────────────────┬──────────────────────────────────────────┘
                            │ method calls
                            ▼
┌──────────────────────────────────────────────────────────────────────┐
│                         FeeApp.java (Java 17)                        │
│                                                                      │
│  Courses            Students            Payments                     │
│  ─────────────      ─────────────       ─────────────────            │
│  addCourse()        addStudent()        collectFee()                 │
│  listCourses()      studentReport()     paymentHistory()             │
│                     deleteStudent()     printReceipt()               │
│                                         voidPayment()                │
│                                         exportCsv()                  │
└───────────────────────────┬──────────────────────────────────────────┘
                            │ JDBC (PreparedStatement, transactions)
                            ▼
┌──────────────────────────────────────────────────────────────────────┐
│                           MySQL DATABASE                             │
│                 fee_db:  courses · students · payments               │
└──────────────────────────────────────────────────────────────────────┘
```

---

## 📁 Project Structure

```
fee-system/
│
├── schema.sql                  # Database, tables and seed courses
├── pom.xml                     # Maven build and dependency configuration
├── README.md                   # Project documentation
│
└── src/
    └── main/
        └── java/
            └── FeeApp.java     # Complete application (menu, JDBC, receipts, CSV)
```

Running the CSV export creates `payments.csv` in the folder you launch the app from.

---

## 🗄️ Database Design

| Table | Description | Key Fields |
|---|---|---|
| **courses** | Course names and standard fees | `id`, `name` (unique), `fee` |
| **students** | Enrolled students with their own fee and discount | `id`, `name`, `phone`, `course_id`, `fee`, `discount`, `created_at` |
| **payments** | Every fee payment received | `id`, `student_id`, `amount`, `paid_on`, `mode`, `reference` |

**Relationships**

- **Course → Student**: one-to-many (a course has many students)
- **Student → Payment**: one-to-many (a student can pay in several instalments)

```
courses (id, name, fee)
   ▲
   │ course_id
students (id, name, phone, course_id, fee, discount, created_at)
   ▲
   │ student_id  (ON DELETE CASCADE)
payments (id, student_id, amount, paid_on, mode, reference)
```

**Design decisions**

- **Dues are calculated, not stored.** Due = `fee − discount − SUM(payments)`. Voiding a payment restores the due automatically and the totals never drift out of sync.
- **Each student keeps their own fee.** The fee is copied from the course at enrolment, so a later change to the course fee does not alter existing students' dues.
- **Money uses `DECIMAL(10,2)` and `BigDecimal`** to avoid floating-point rounding errors.
- **Integrity rules:** primary keys on all tables, foreign keys for `course_id` and `student_id`, a `UNIQUE` course name, and `CHECK` constraints that keep fees and amounts above zero and discounts non-negative.
- **Receipt and student numbers** come from auto-increment IDs and are shown as `R00001` and `S0001`.

---

## 📋 Menu Reference

| Option | Action |
|---|---|
| `1` | Add a course, or update its fee if the name already exists |
| `2` | List courses |
| `3` | Add a student |
| `4` | List all students with payable, paid, due and status |
| `5` | Collect a fee and print the receipt |
| `6` | Show payment history |
| `7` | Print a receipt by payment ID |
| `8` | Void a payment |
| `9` | Dues report (students with a pending balance) |
| `10` | Export payments to `payments.csv` |
| `11` | Delete a student and their payments |
| `0` | Exit |

### Sample receipt

```
----------------------------------------
              FEE RECEIPT
----------------------------------------
Receipt no.   : R00001
Date          : 2026-10-14
Student       : Asha Verma (S0001)
Course        : PG-DAC
Mode          : UPI / 4821937
Amount paid   : Rs 50,000.00
Total paid    : Rs 50,000.00
Balance due   : Rs 45,000.00
----------------------------------------
```

---

## 🚀 Installation & Running

### Prerequisites

| Tool | Purpose |
|---|---|
| JDK 17 or later | Compile and run the Java code |
| Maven 3.6+ | Build and dependency management |
| MySQL 8.x | Database server |
| MySQL Workbench / phpMyAdmin (optional) | Viewing the data visually |
| VS Code, IntelliJ or Eclipse | Editing the project |

### Steps

```bash
# 1. Create the database, tables and sample courses
mysql -u root -p < schema.sql

# 2. Set your database credentials (defaults: root / empty password)
#    Linux / macOS
export DB_USER=root
export DB_PASS=yourpassword

#    Windows (Command Prompt)
set DB_USER=root
set DB_PASS=yourpassword

#    Windows (PowerShell)
$env:DB_USER="root"
$env:DB_PASS="yourpassword"

# 3. Build and run
mvn compile exec:java
```

### Configuration

| Variable | Default |
|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/fee_db?useSSL=false&allowPublicKeyRetrieval=true` |
| `DB_USER` | `root` |
| `DB_PASS` | *(empty)* |

### Typical first session

1. Run the app and choose `2` to see the seeded courses (PG-DAC and PG-DBDA).
2. Choose `3` to enrol a student and note the Student ID shown.
3. Choose `5` to collect a fee using that ID. The receipt prints immediately.
4. Choose `9` to check pending dues.

---

## 🛠️ Tech Stack Summary

| Layer | Technology | Purpose |
|---|---|---|
| **Language** | Java 17 | Application logic and console interface |
| **Database Access** | JDBC (MySQL Connector/J 8.4.0) | Prepared statements and transactions |
| **Database** | MySQL 8 | Storing courses, students and payments |
| **Build Tool** | Maven | Dependencies and running the app |
| **Database Tooling** | MySQL Workbench / phpMyAdmin | Visual database management |
| **Editor** | VS Code / IntelliJ / Eclipse | Development environment |

---

## 👥 User Roles

The current version has a single role with full access and no login.

### Operator (Accountant / Placement or Admin Staff)
- Creates courses and updates their fees
- Enrols and deletes students
- Collects fees and prints receipts
- Voids incorrect payments
- Reviews dues and payment history, and exports data to CSV

---

## 🧰 Troubleshooting

| Problem | Fix |
|---|---|
| `Cannot connect to MySQL` | Check that MySQL is running and that `DB_URL`, `DB_USER` and `DB_PASS` are correct |
| `Unknown database 'fee_db'` | Run `schema.sql` first |
| `Access denied for user` | Set the right `DB_USER` and `DB_PASS`, or create a MySQL user with access to `fee_db` |
| `Public Key Retrieval is not allowed` | Keep `allowPublicKeyRetrieval=true` in `DB_URL` |
| `release version 17 not supported` | Install JDK 17 or later and make sure `JAVA_HOME` points to it |
| Rupee symbol shows incorrectly | The app prints `Rs` instead of ₹ to avoid console encoding problems |

---

## ⚠️ Limitations

- Console only, with no graphical or web interface
- Single-user, with no login or role-based access
- One total fee per student, with no instalment schedule or due dates
- No late-fee or fine calculation
- Voiding permanently deletes the payment row, so there is no audit trail
- Receipts print to the console only, with no PDF output
- No SMS or email notifications

---

## 🔮 Future Scope

- Web version using Spring Boot REST APIs
- JavaFX or Swing desktop interface
- Login with admin and accountant roles
- Instalment schedules with due dates and automatic reminders
- Late-fee calculation
- PDF receipts with an institute logo
- Audit log for voided and deleted records
- Dashboard with collection charts and course-wise analytics
- SMS and email receipts for students and parents

<div align="center">

Developed by Lakshya Kansal

</div>
