package UI;

import Controllers.UserController;

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

        JButton registerButton = new JButton("Register User");
        add(backButton);
        backButton.setVisible(false);
        add(registerButton);

        registerButton.addActionListener(e -> handleAddUser());
    }

    private void handleAddUser() {
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

        UserController.Result result = userController.addClerk(id, username, password, name);

        switch (result) {
            case SUCCESS ->
                    JOptionPane.showMessageDialog(this,
                            "Clerk \"" + username + "\" registered successfully!");
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

    public void setBackAction(ActionListener goBack, String backMessage) {
        backButton.addActionListener(goBack);
        backButton.setLabel("← Back to Welcome");
        backButton.setVisible(true);
    }
}
