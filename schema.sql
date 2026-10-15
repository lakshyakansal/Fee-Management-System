CREATE DATABASE IF NOT EXISTS fee_db;
USE fee_db;

CREATE TABLE IF NOT EXISTS courses (
  id   INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(80) NOT NULL UNIQUE,
  fee  DECIMAL(10,2) NOT NULL CHECK (fee > 0)
);

CREATE TABLE IF NOT EXISTS students (
  id         INT AUTO_INCREMENT PRIMARY KEY,
  name       VARCHAR(100) NOT NULL,
  phone      VARCHAR(20),
  course_id  INT NOT NULL,
  fee        DECIMAL(10,2) NOT NULL CHECK (fee > 0),
  discount   DECIMAL(10,2) NOT NULL DEFAULT 0 CHECK (discount >= 0),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (course_id) REFERENCES courses(id)
);

CREATE TABLE IF NOT EXISTS payments (
  id         INT AUTO_INCREMENT PRIMARY KEY,
  student_id INT NOT NULL,
  amount     DECIMAL(10,2) NOT NULL CHECK (amount > 0),
  paid_on    DATE NOT NULL,
  mode       VARCHAR(20) NOT NULL,
  reference  VARCHAR(100),
  FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
);

INSERT IGNORE INTO courses (name, fee) VALUES ('PG-DAC', 95000), ('PG-DBDA', 90000);
