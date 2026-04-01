package UI;
import People.UserCatalog;
import UseCases.AddUser;
import Utility.UserController;

import javax.swing.*;
import java.awt.*;

public class AddUserUI extends JPanel {

    private final UserController userController;

    private final JTextField usernameField  = new JTextField(15);
    private final JPasswordField passwordField = new JPasswordField(15);
    private final JTextField nameField      = new JTextField(15);
    private final JTextField phoneField     = new JTextField(15);
    private final JTextField emailField     = new JTextField(15);

    public AddUserUI(UserController userController) {
        this.userController = userController;
        setLayout(new GridLayout(6, 2, 5, 5));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(new JLabel("Username:"));  add(usernameField);
        add(new JLabel("Password:"));  add(passwordField);
        add(new JLabel("Full Name:")); add(nameField);
        add(new JLabel("Phone:"));     add(phoneField);
        add(new JLabel("Email:"));     add(emailField);

        JButton registerButton = new JButton("Register User");
        add(new JLabel()); // spacer
        add(registerButton);

        registerButton.addActionListener(e -> handleAddUser());
    }

    private void handleAddUser() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        String name     = nameField.getText().trim();
        String phone    = phoneField.getText().trim();
        String email    = emailField.getText().trim();

        UserController.Result result = userController.addGuest(username, password, name, phone, email);

        switch (result) {
            case SUCCESS ->
                    JOptionPane.showMessageDialog(this,
                            "Guest \"" + username + "\" registered successfully!");
            case DUPLICATE_USERNAME ->
                    JOptionPane.showMessageDialog(this,
                            "Username \"" + username + "\" is already taken. Please choose another.",
                            "Duplicate Username", JOptionPane.WARNING_MESSAGE);
            case INVALID_INPUT ->
                    JOptionPane.showMessageDialog(this,
                            "Username, password, and full name are required.",
                            "Invalid Input", JOptionPane.ERROR_MESSAGE);
        }
    }
}
