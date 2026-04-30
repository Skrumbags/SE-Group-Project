package UI.Admin;

import Controllers.UserController;
import Domain.Reservations.BookingValidation;
import Domain.Services.UserService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

// TODO: lowkey duplicate of Add User, but couldn't find way to separate need for id and no need
public class AddCA_UI extends JPanel {

    private final UserController userController;

    private final JTextField employeeID =  new JTextField();
    private final JTextField usernameField  = new JTextField(15);
    private final JPasswordField passwordField = new JPasswordField(15);
    private final JTextField nameField      = new JTextField(15);
    private JButton backButton = new JButton();

    public AddCA_UI(UserController userController) {
        this.userController = userController;
        setLayout(new GridLayout(6, 2, 5, 5));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(new JLabel("Employee ID:")); add(employeeID);
        add(new JLabel("Username:"));  add(usernameField);
        add(new JLabel("Password:"));  add(passwordField);
        add(new JLabel("Full Name:")); add(nameField);

        JButton registerButton = new JButton("Register Clerk");
        add(backButton);
        backButton.setVisible(false);
        add(registerButton);

        registerButton.addActionListener(e -> handleAddClerk());
    }

    private void handleAddClerk() {
        int id;
        try {
            id = Integer.parseInt(employeeID.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Employee ID must be a whole number.",
                    "Invalid Input", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        String name     = nameField.getText().trim();

        if (username.isBlank() || password.isBlank()) {
            JOptionPane.showMessageDialog(this,
                    "Username and password are required.",
                    "Invalid Input", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String nameError = BookingValidation.validateGuestName(name);
        if (nameError != null) {
            JOptionPane.showMessageDialog(this,
                    nameError,
                    "Invalid Input", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (username.contains(" ")) {
            JOptionPane.showMessageDialog(this,
                    "Username cannot contain spaces.",
                    "Invalid Input", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (password.length() < 4) {
            JOptionPane.showMessageDialog(this,
                    "Password must be at least 4 characters.",
                    "Invalid Input", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (userController.exists(username)) {
            JOptionPane.showMessageDialog(this,
                    "Username \"" + username + "\" is already taken. Please choose another.",
                    "Duplicate Username", JOptionPane.WARNING_MESSAGE);
            return;
        }

        UserService.Result result = userController.addClerk(id, username, password, name);

        switch (result) {
            case SUCCESS ->
                    JOptionPane.showMessageDialog(this,
                            "Clerk \"" + username + "\" registered successfully!");
            case DUPLICATE_USERNAME ->
                    JOptionPane.showMessageDialog(this,
                            "Username \"" + username + "\" is already taken. Please choose another.",
                            "Duplicate Username", JOptionPane.WARNING_MESSAGE);
            case DUPLICATE_EMPLOYEE_ID ->
                    JOptionPane.showMessageDialog(this,
                            "Employee ID \"" + id + "\" is already assigned to another account.",
                            "Duplicate Employee ID", JOptionPane.WARNING_MESSAGE);
            case INVALID_INPUT ->
                    JOptionPane.showMessageDialog(this,
                            "Invalid input provided. Please check the fields and try again.",
                            "Invalid Input", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void setBackAction(ActionListener goBack, String backMessage) {
        backButton.addActionListener(goBack);
        backButton.setText("← Back to Welcome");
        backButton.setVisible(true);
    }

    public void refresh() {
        employeeID.setText("");
        usernameField.setText("");
        passwordField.setText("");
        nameField.setText("");
    }
}