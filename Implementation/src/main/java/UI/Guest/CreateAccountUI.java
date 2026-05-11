package UI.Guest;

import Controllers.UserController;
import Domain.Services.UserService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.Objects;

public class CreateAccountUI extends JPanel {

    private final UserController userController;
    /** After a successful registration (after the success dialog is dismissed). */
    private Runnable registrationSuccessAction = () -> {};

    private final JTextField usernameField  = new JTextField(15);
    private final JPasswordField passwordField = new JPasswordField(15);
    private final JTextField nameField      = new JTextField(15);
    private final JTextField phoneField     = new JTextField(15);
    private final JTextField emailField     = new JTextField(15);
    private JButton backButton = new JButton();

    public CreateAccountUI(UserController userController) {
        this.userController = userController;
        setLayout(new GridLayout(6, 2, 5, 5));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(new JLabel("Username:"));  add(usernameField);
        add(new JLabel("Password:"));  add(passwordField);
        add(new JLabel("Full Name:")); add(nameField);
        add(new JLabel("Phone:"));     add(phoneField);
        add(new JLabel("Email:"));     add(emailField);

        JButton registerButton = new JButton("Register User");
        add(backButton);
        backButton.setVisible(false);
        add(registerButton);

        registerButton.addActionListener(e -> handleAddUser());
    }

    /**
     * Called after a successful registration and the success message is closed.
     * Typical use: clear this form and show the login card.
     */
    public void setRegistrationSuccessAction(Runnable action) {
        this.registrationSuccessAction = Objects.requireNonNullElse(action, () -> {});
    }

    private void handleAddUser() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        String name     = nameField.getText().trim();
        String phone    = phoneField.getText().trim();
        String email    = emailField.getText().trim();

        if (username.isBlank() || password.isBlank() || name.isBlank()) {
            JOptionPane.showMessageDialog(this,
                    "Username, password, and full name are required.",
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
        if (!phone.isBlank() && !phone.matches("[0-9+()\\-\\s]{7,}")) {
            JOptionPane.showMessageDialog(this,
                    "Phone number looks invalid.",
                    "Invalid Input", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!email.isBlank() && !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            JOptionPane.showMessageDialog(this,
                    "Email address looks invalid.",
                    "Invalid Input", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!email.isBlank() && userController.emailExists(email)) {
            JOptionPane.showMessageDialog(this,
                    "Email \"" + email + "\" is already associated with an existing account.",
                    "Duplicate Email", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (userController.exists(username)) {
            JOptionPane.showMessageDialog(this,
                    "Username \"" + username + "\" is already taken. Please choose another.",
                    "Duplicate Username", JOptionPane.WARNING_MESSAGE);
            return;
        }

        UserService.Result result = userController.addGuest(username, password, name, phone, email);

        switch (result) {
            case SUCCESS -> {
                JOptionPane.showMessageDialog(this,
                        "Guest \"" + username + "\" registered successfully.\nYou can sign in now.",
                        "Account created",
                        JOptionPane.INFORMATION_MESSAGE);
                refresh();
                registrationSuccessAction.run();
            }
            case DUPLICATE_USERNAME ->
                    JOptionPane.showMessageDialog(this,
                            "Username \"" + username + "\" is already taken. Please choose another.",
                            "Duplicate Username", JOptionPane.WARNING_MESSAGE);
            case DUPLICATE_EMAIL ->
                    JOptionPane.showMessageDialog(this,
                            "Email \"" + email + "\" is already associated with an existing account.",
                            "Duplicate Email", JOptionPane.WARNING_MESSAGE);
            case INVALID_INPUT ->
                    JOptionPane.showMessageDialog(this,
                            "Username, password, and full name are required.",
                            "Invalid Input", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void setBackAction(ActionListener goBack, String backMessage) {
        backButton.addActionListener(goBack);
        backButton.setLabel(backMessage);
        backButton.setVisible(true);
    }

    public void refresh() {
        usernameField.setText("");
        passwordField.setText("");
        nameField.setText("");
        phoneField.setText("");
        emailField.setText("");
    }
}

