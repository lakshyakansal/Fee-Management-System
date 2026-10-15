import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;
import java.util.Scanner;

public class FeeApp {

    static final String URL  = env("DB_URL",
            "jdbc:mysql://localhost:3306/fee_db?useSSL=false&allowPublicKeyRetrieval=true");
    static final String USER = env("DB_USER", "root");
    static final String PASS = env("DB_PASS", "");

    static Connection con;
    static final Scanner in = new Scanner(System.in);

    static String env(String k, String d) {
        String v = System.getenv(k);
        return v == null ? d : v;
    }

    // ---------- input helpers ----------
    static String ask(String p) {
        System.out.print(p + ": ");
        return in.nextLine().trim();
    }

    static int askInt(String p) {
        while (true) {
            try { return Integer.parseInt(ask(p)); }
            catch (NumberFormatException e) { System.out.println("Enter a whole number."); }
        }
    }

    static BigDecimal askMoney(String p) {
        while (true) {
            try {
                BigDecimal v = new BigDecimal(ask(p));
                if (v.signum() >= 0) return v.setScale(2, RoundingMode.HALF_UP);
            } catch (NumberFormatException ignored) { }
            System.out.println("Enter a valid amount.");
        }
    }

    static String inr(BigDecimal v) { return String.format("Rs %,.2f", v); }

    // ---------- main ----------
    public static void main(String[] args) {
        try {
            con = DriverManager.getConnection(URL, USER, PASS);
        } catch (SQLException e) {
            System.out.println("Cannot connect to MySQL: " + e.getMessage());
            System.out.println("Check DB_URL, DB_USER, DB_PASS and that schema.sql was run.");
            return;
        }

        while (true) {
            System.out.println("""

                ===== FEE MANAGEMENT SYSTEM =====
                1. Add course            6. Payment history
                2. List courses          7. Print receipt
                3. Add student           8. Void payment
                4. List students         9. Dues report
                5. Collect fee          10. Export payments to CSV
                11. Delete student       0. Exit""");
            int choice = askInt("Choose");
            try {
                switch (choice) {
                    case 1 -> addCourse();
                    case 2 -> listCourses();
                    case 3 -> addStudent();
                    case 4 -> studentReport(false);
                    case 5 -> collectFee();
                    case 6 -> paymentHistory();
                    case 7 -> printReceipt(askInt("Payment ID"));
                    case 8 -> voidPayment();
                    case 9 -> studentReport(true);
                    case 10 -> exportCsv();
                    case 11 -> deleteStudent();
                    case 0 -> { con.close(); System.out.println("Goodbye."); return; }
                    default -> System.out.println("Invalid choice.");
                }
            } catch (SQLException | IOException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    // ---------- courses ----------
    static void addCourse() throws SQLException {
        String name = ask("Course name");
        BigDecimal fee = askMoney("Fee");
        if (name.isEmpty() || fee.signum() == 0) { System.out.println("Name and fee are required."); return; }
        String sql = "INSERT INTO courses(name, fee) VALUES(?,?) ON DUPLICATE KEY UPDATE fee=VALUES(fee)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setBigDecimal(2, fee);
            ps.executeUpdate();
        }
        System.out.println("Course saved.");
    }

    static void listCourses() throws SQLException {
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, name, fee FROM courses ORDER BY id")) {
            System.out.printf("%-5s %-20s %s%n", "ID", "Course", "Fee");
            while (rs.next())
                System.out.printf("%-5d %-20s %s%n", rs.getInt(1), rs.getString(2), inr(rs.getBigDecimal(3)));
        }
    }

    // ---------- students ----------
    static void addStudent() throws SQLException {
        String name = ask("Student name");
        if (name.isEmpty()) { System.out.println("Name is required."); return; }
        String phone = ask("Phone");
        listCourses();
        int courseId = askInt("Course ID");

        BigDecimal fee;
        try (PreparedStatement ps = con.prepareStatement("SELECT fee FROM courses WHERE id=?")) {
            ps.setInt(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) { System.out.println("Course not found."); return; }
                fee = rs.getBigDecimal(1);
            }
        }
        BigDecimal discount = askMoney("Discount / scholarship (0 for none)");
        if (discount.compareTo(fee) > 0) { System.out.println("Discount cannot exceed the fee."); return; }

        String sql = "INSERT INTO students(name, phone, course_id, fee, discount) VALUES(?,?,?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, phone);
            ps.setInt(3, courseId);
            ps.setBigDecimal(4, fee);
            ps.setBigDecimal(5, discount);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                if (k.next()) System.out.println("Student added with ID " + k.getInt(1));
            }
        }
    }

    static void studentReport(boolean onlyDues) throws SQLException {
        String sql = """
            SELECT s.id, s.name, c.name AS course, s.fee - s.discount AS payable,
                   COALESCE(SUM(p.amount), 0) AS paid
            FROM students s
            JOIN courses c ON c.id = s.course_id
            LEFT JOIN payments p ON p.student_id = s.id
            GROUP BY s.id, s.name, c.name, s.fee, s.discount
            """ + (onlyDues ? "HAVING paid < payable " : "") + "ORDER BY s.id";
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            System.out.printf("%-5s %-22s %-10s %-16s %-16s %-16s %s%n",
                    "ID", "Name", "Course", "Payable", "Paid", "Due", "Status");
            boolean any = false;
            BigDecimal totalDue = BigDecimal.ZERO;
            while (rs.next()) {
                any = true;
                BigDecimal payable = rs.getBigDecimal("payable");
                BigDecimal paid = rs.getBigDecimal("paid");
                BigDecimal due = payable.subtract(paid).max(BigDecimal.ZERO);
                totalDue = totalDue.add(due);
                String status = due.signum() == 0 ? "Paid" : paid.signum() > 0 ? "Partial" : "Unpaid";
                System.out.printf("%-5d %-22s %-10s %-16s %-16s %-16s %s%n",
                        rs.getInt("id"), rs.getString("name"), rs.getString("course"),
                        inr(payable), inr(paid), inr(due), status);
            }
            if (!any) System.out.println(onlyDues ? "No pending dues." : "No students yet.");
            else if (onlyDues) System.out.println("Total outstanding: " + inr(totalDue));
        }
    }

    static void deleteStudent() throws SQLException {
        int id = askInt("Student ID");
        if (!ask("Delete this student and all their payments? (yes/no)").equalsIgnoreCase("yes")) return;
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM students WHERE id=?")) {
            ps.setInt(1, id);
            System.out.println(ps.executeUpdate() > 0 ? "Student deleted." : "Student not found.");
        }
    }

    // ---------- payments ----------
    static void collectFee() throws SQLException {
        int sid = askInt("Student ID");
        BigDecimal amount = askMoney("Amount");
        if (amount.signum() == 0) { System.out.println("Amount must be more than zero."); return; }
        String mode = ask("Mode (Cash/UPI/Card/Bank/Cheque)");
        if (mode.isEmpty()) mode = "Cash";
        String ref = ask("Reference (optional)");

        con.setAutoCommit(false);
        try {
            // Lock the student row so two clerks cannot overpay at the same time
            String dueSql = """
                SELECT s.fee - s.discount - COALESCE(
                       (SELECT SUM(amount) FROM payments WHERE student_id = s.id), 0) AS due
                FROM students s WHERE s.id = ? FOR UPDATE""";
            BigDecimal due;
            try (PreparedStatement ps = con.prepareStatement(dueSql)) {
                ps.setInt(1, sid);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) { System.out.println("Student not found."); con.rollback(); return; }
                    due = rs.getBigDecimal(1);
                }
            }
            if (amount.compareTo(due) > 0) {
                System.out.println("Amount is more than the due " + inr(due) + ".");
                con.rollback();
                return;
            }

            int paymentId;
            String ins = "INSERT INTO payments(student_id, amount, paid_on, mode, reference) VALUES(?,?,?,?,?)";
            try (PreparedStatement ps = con.prepareStatement(ins, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, sid);
                ps.setBigDecimal(2, amount);
                ps.setDate(3, Date.valueOf(LocalDate.now()));
                ps.setString(4, mode);
                ps.setString(5, ref);
                ps.executeUpdate();
                try (ResultSet k = ps.getGeneratedKeys()) { k.next(); paymentId = k.getInt(1); }
            }
            con.commit();
            printReceipt(paymentId);
        } catch (SQLException e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(true);
        }
    }

    static void paymentHistory() throws SQLException {
        String sql = """
            SELECT p.id, p.paid_on, s.name, p.amount, p.mode
            FROM payments p JOIN students s ON s.id = p.student_id
            ORDER BY p.id DESC""";
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            System.out.printf("%-9s %-12s %-22s %-16s %s%n", "Receipt", "Date", "Student", "Amount", "Mode");
            boolean any = false;
            while (rs.next()) {
                any = true;
                System.out.printf("R%05d    %-12s %-22s %-16s %s%n", rs.getInt("id"), rs.getDate("paid_on"),
                        rs.getString("name"), inr(rs.getBigDecimal("amount")), rs.getString("mode"));
            }
            if (!any) System.out.println("No payments recorded yet.");
        }
    }

    static void printReceipt(int paymentId) throws SQLException {
        String sql = """
            SELECT p.id, p.paid_on, p.mode, p.reference, p.amount,
                   s.id AS sid, s.name, c.name AS course, s.fee - s.discount AS payable,
                   (SELECT COALESCE(SUM(amount), 0) FROM payments WHERE student_id = s.id) AS total_paid
            FROM payments p
            JOIN students s ON s.id = p.student_id
            JOIN courses c ON c.id = s.course_id
            WHERE p.id = ?""";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, paymentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) { System.out.println("Payment not found."); return; }
                BigDecimal totalPaid = rs.getBigDecimal("total_paid");
                BigDecimal balance = rs.getBigDecimal("payable").subtract(totalPaid).max(BigDecimal.ZERO);
                String ref = rs.getString("reference");
                System.out.println("\n----------------------------------------");
                System.out.println("              FEE RECEIPT");
                System.out.println("----------------------------------------");
                System.out.printf("Receipt no.   : R%05d%n", rs.getInt("id"));
                System.out.printf("Date          : %s%n", rs.getDate("paid_on"));
                System.out.printf("Student       : %s (S%04d)%n", rs.getString("name"), rs.getInt("sid"));
                System.out.printf("Course        : %s%n", rs.getString("course"));
                System.out.printf("Mode          : %s%s%n", rs.getString("mode"),
                        ref == null || ref.isBlank() ? "" : " / " + ref);
                System.out.printf("Amount paid   : %s%n", inr(rs.getBigDecimal("amount")));
                System.out.printf("Total paid    : %s%n", inr(totalPaid));
                System.out.printf("Balance due   : %s%n", inr(balance));
                System.out.println("----------------------------------------");
            }
        }
    }

    static void voidPayment() throws SQLException {
        int id = askInt("Payment ID to void");
        if (!ask("The amount will return to the student's due. Continue? (yes/no)").equalsIgnoreCase("yes")) return;
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM payments WHERE id=?")) {
            ps.setInt(1, id);
            System.out.println(ps.executeUpdate() > 0 ? "Payment voided." : "Payment not found.");
        }
    }

    static void exportCsv() throws SQLException, IOException {
        String sql = """
            SELECT p.id, p.paid_on, s.id AS sid, s.name, c.name AS course, p.amount, p.mode, p.reference
            FROM payments p
            JOIN students s ON s.id = p.student_id
            JOIN courses c ON c.id = s.course_id
            ORDER BY p.id""";
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql);
             PrintWriter out = new PrintWriter(new FileWriter("payments.csv"))) {
            out.println("Receipt,Date,Student ID,Name,Course,Amount,Mode,Reference");
            int n = 0;
            while (rs.next()) {
                out.printf("R%05d,%s,S%04d,%s,%s,%s,%s,%s%n", rs.getInt("id"), rs.getDate("paid_on"),
                        rs.getInt("sid"), q(rs.getString("name")), q(rs.getString("course")),
                        rs.getBigDecimal("amount"), q(rs.getString("mode")), q(rs.getString("reference")));
                n++;
            }
            System.out.println(n + " payments written to payments.csv");
        }
    }

    static String q(String v) {
        return "\"" + (v == null ? "" : v.replace("\"", "\"\"")) + "\"";
    }
}
