import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.*;
import java.util.List;

public class DBConnection extends JFrame {

    // ---------- Database settings ----------
    static final String SERVER_URL = "jdbc:mysql://localhost:3306/";
    static final String DB_NAME = "student_db";
    static final String USER = "root";
    static final String PASSWORD = "root";   // change this

    static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(SERVER_URL + DB_NAME, USER, PASSWORD);
    }

    // Creates the database and table automatically if they don't exist
    static void setupDatabase() throws SQLException {
        try (Connection con = DriverManager.getConnection(SERVER_URL, USER, PASSWORD);
             Statement st = con.createStatement()) {
            st.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DB_NAME);
            st.executeUpdate("USE " + DB_NAME);
            st.executeUpdate("CREATE TABLE IF NOT EXISTS students ("
                + "id INT AUTO_INCREMENT PRIMARY KEY,"
                + "roll_no VARCHAR(20) UNIQUE NOT NULL,"
                + "name VARCHAR(100) NOT NULL,"
                + "maths INT, physics INT, chemistry INT, english INT, computer INT,"
                + "total INT,"
                + "percentage DECIMAL(5,2),"
                + "grade VARCHAR(3),"
                + "status VARCHAR(10))");
        }
    }

    // ---------- Result calculation ----------
    static final int PASS_MARK = 40;

    static int total(int... marks) {
        int t = 0;
        for (int m : marks) t += m;
        return t;
    }

    static double percentage(int total, int subjects) {
        return total / (double) subjects;
    }

    static String grade(double p) {
        if (p >= 90) return "A+";
        if (p >= 80) return "A";
        if (p >= 70) return "B+";
        if (p >= 60) return "B";
        if (p >= 50) return "C";
        if (p >= 40) return "D";
        return "F";
    }

    static String status(int... marks) {
        for (int m : marks) if (m < PASS_MARK) return "Fail";
        return "Pass";
    }

    // ---------- Database operations ----------
    static boolean addStudent(String roll, String name, int[] m) throws SQLException {
        int total = total(m);
        double pct = percentage(total, m.length);
        String sql = "INSERT INTO students (roll_no,name,maths,physics,chemistry,english,computer,total,percentage,grade,status) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, roll);
            ps.setString(2, name);
            for (int i = 0; i < 5; i++) ps.setInt(3 + i, m[i]);
            ps.setInt(8, total);
            ps.setDouble(9, pct);
            ps.setString(10, grade(pct));
            ps.setString(11, status(m));
            return ps.executeUpdate() > 0;
        }
    }

    static boolean updateStudent(String roll, String name, int[] m) throws SQLException {
        int total = total(m);
        double pct = percentage(total, m.length);
        String sql = "UPDATE students SET name=?,maths=?,physics=?,chemistry=?,english=?,computer=?,total=?,percentage=?,grade=?,status=? WHERE roll_no=?";
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name);
            for (int i = 0; i < 5; i++) ps.setInt(2 + i, m[i]);
            ps.setInt(7, total);
            ps.setDouble(8, pct);
            ps.setString(9, grade(pct));
            ps.setString(10, status(m));
            ps.setString(11, roll);
            return ps.executeUpdate() > 0;
        }
    }

    static boolean deleteStudent(String roll) throws SQLException {
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM students WHERE roll_no=?")) {
            ps.setString(1, roll);
            return ps.executeUpdate() > 0;
        }
    }

    static List<Object[]> getAllStudents() throws SQLException {
        List<Object[]> rows = new ArrayList<>();
        try (Connection con = getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM students ORDER BY roll_no")) {
            while (rs.next()) {
                rows.add(new Object[]{
                    rs.getString("roll_no"), rs.getString("name"),
                    rs.getInt("maths"), rs.getInt("physics"), rs.getInt("chemistry"),
                    rs.getInt("english"), rs.getInt("computer"),
                    rs.getInt("total"), rs.getDouble("percentage"),
                    rs.getString("grade"), rs.getString("status")
                });
            }
        }
        return rows;
    }

    // ---------- Swing UI ----------
    private final JTextField txtRoll = new JTextField(10), txtName = new JTextField(15);
    private final JTextField[] txtMarks = new JTextField[5];
    private final String[] subjects = {"Maths", "Physics", "Chemistry", "English", "Computer"};
    private final DefaultTableModel model = new DefaultTableModel(
        new String[]{"Roll No","Name","Maths","Physics","Chemistry","English","Computer","Total","%","Grade","Status"}, 0);
    private final JTable table = new JTable(model);

    public DBConnection() {
        setTitle("Student Marks Analysis System");
        setSize(950, 640);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        for (int i = 0; i < 5; i++) txtMarks[i] = new JTextField(6);

        // Grid layout: 4 rows x 4 columns, so every subject is visible
        JPanel form = new JPanel(new GridLayout(4, 4, 10, 10));
        form.setBorder(BorderFactory.createTitledBorder("Student Details"));

        form.add(new JLabel("Roll No:"));    form.add(txtRoll);
        form.add(new JLabel("Name:"));       form.add(txtName);

        form.add(new JLabel("Maths:"));      form.add(txtMarks[0]);
        form.add(new JLabel("Physics:"));    form.add(txtMarks[1]);

        form.add(new JLabel("Chemistry:"));  form.add(txtMarks[2]);
        form.add(new JLabel("English:"));    form.add(txtMarks[3]);

        form.add(new JLabel("Computer:"));   form.add(txtMarks[4]);
        form.add(new JLabel(""));            form.add(new JLabel(""));

        JButton btnAdd = new JButton("Add"), btnUpdate = new JButton("Update"),
                btnDelete = new JButton("Delete"), btnClear = new JButton("Clear");
        JPanel buttons = new JPanel();
        buttons.add(btnAdd); buttons.add(btnUpdate); buttons.add(btnDelete); buttons.add(btnClear);

        JPanel top = new JPanel(new BorderLayout());
        top.add(form, BorderLayout.CENTER);
        top.add(buttons, BorderLayout.SOUTH);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        btnAdd.addActionListener(e -> save(true));
        btnUpdate.addActionListener(e -> save(false));
        btnDelete.addActionListener(e -> delete());
        btnClear.addActionListener(e -> clearFields());
        table.getSelectionModel().addListSelectionListener(e -> fillFromTable());

        loadTable();
    }

    private int[] readMarks() {
        int[] m = new int[5];
        for (int i = 0; i < 5; i++) {
            m[i] = Integer.parseInt(txtMarks[i].getText().trim());
            if (m[i] < 0 || m[i] > 100) throw new NumberFormatException("Marks must be 0-100");
        }
        return m;
    }

    private void save(boolean isNew) {
        try {
            String roll = txtRoll.getText().trim(), name = txtName.getText().trim();
            if (roll.isEmpty() || name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Roll number and name are required.");
                return;
            }
            int[] marks = readMarks();
            boolean ok = isNew ? addStudent(roll, name, marks) : updateStudent(roll, name, marks);
            JOptionPane.showMessageDialog(this, ok ? "Saved successfully." : "No record changed.");
            loadTable(); clearFields();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter valid marks (0-100) in all subjects.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    private void delete() {
        try {
            if (deleteStudent(txtRoll.getText().trim())) {
                loadTable(); clearFields();
            } else JOptionPane.showMessageDialog(this, "Roll number not found.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    private void loadTable() {
        try {
            model.setRowCount(0);
            for (Object[] row : getAllStudents()) model.addRow(row);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Load error: " + ex.getMessage());
        }
    }

    private void fillFromTable() {
        int r = table.getSelectedRow();
        if (r < 0) return;
        txtRoll.setText(model.getValueAt(r, 0).toString());
        txtName.setText(model.getValueAt(r, 1).toString());
        for (int i = 0; i < 5; i++) txtMarks[i].setText(model.getValueAt(r, 2 + i).toString());
    }

    private void clearFields() {
        txtRoll.setText(""); txtName.setText("");
        for (JTextField t : txtMarks) t.setText("");
        table.clearSelection();
    }

    public static void main(String[] args) {
        try {
            setupDatabase();   // creates database + table automatically
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null,
                "Database setup failed: " + ex.getMessage()
                + "\nCheck that MySQL is running and the password is correct.");
            return;
        }
        SwingUtilities.invokeLater(() -> new DBConnection().setVisible(true));
    }
}