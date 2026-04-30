package UI.Other;

import Controllers.UserController;
import Domain.People.Guest;
import Domain.People.User;
import Domain.People.UserSession;
import Domain.Services.UserService;

import javax.swing.*;
import java.awt.*;

public class ProfileUI extends JPanel {

    private final UserSession userSession;
    private final UserController userController;

    private final JTextField usernameField = new JTextField(15);
    private final JTextField nameField = new JTextField(15);
    private final JTextField phoneField = new JTextField(15);
    private final JTextField emailField = new JTextField(15);
    private final JPasswordField oldPasswordField = new JPasswordField(15);
    private final JPasswordField newPasswordField = new JPasswordField(15);
    private final JPasswordField confirmPasswordField = new JPasswordField(15);

    private final JLabel phoneLabel = new JLabel("Phone:");
    private final JLabel emailLabel = new JLabel("Email:");

    public ProfileUI(UserSession userSession, UserController userController, Runnable onBack) {
        this.userSession = userSession;
        this.userController = userController;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

        JLabel title = new JLabel("Edit Profile", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        int y = 0;
        addFormRow(form, gbc, new JLabel("Username:"), usernameField, y++);
        addFormRow(form, gbc, new JLabel("Full Name:"), nameField, y++);
        addFormRow(form, gbc, phoneLabel, phoneField, y++);
        addFormRow(form, gbc, emailLabel, emailField, y++);

        gbc.gridx = 0; gbc.gridy = y++; gbc.gridwidth = 2;
        form.add(new JLabel("--- Change Password (leave blank to keep current) ---"), gbc);
        gbc.gridwidth = 1;

        addFormRow(form, gbc, new JLabel("Old Password:"), oldPasswordField, y++);
        addFormRow(form, gbc, new JLabel("New Password:"), newPasswordField, y++);
        addFormRow(form, gbc, new JLabel("Confirm New Password:"), confirmPasswordField, y++);

        add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        JButton saveBtn = new JButton("Save Changes");
        JButton backBtn = new JButton("← Back to Home");
        buttons.add(backBtn);
        buttons.add(saveBtn);
        add(buttons, BorderLayout.SOUTH);

        saveBtn.addActionListener(e -> handleSave(onBack));
        backBtn.addActionListener(e -> onBack.run());
    }

    private void addFormRow(JPanel panel, GridBagConstraints gbc, JLabel label, JComponent field, int y) {
        gbc.gridx = 0; gbc.gridy = y;
        panel.add(label, gbc);
        gbc.gridx = 1;
        panel.add(field, gbc);
    }

    public void refresh() {
        User u = userSession.getCurrentUser();
        if (u == null) return;

        usernameField.setText(u.getUsername());
        nameField.setText(u.getName());

        oldPasswordField.setText("");
        newPasswordField.setText("");
        confirmPasswordField.setText("");

        if (u instanceof Guest) {
            Guest g = (Guest) u;
            phoneField.setText(g.getPhone());
            emailField.setText(g.getEmail());
            phoneLabel.setVisible(true);
            phoneField.setVisible(true);
            emailLabel.setVisible(true);
            emailField.setVisible(true);
        } else {
            phoneField.setText("");
            emailField.setText("");
            phoneLabel.setVisible(false);
            phoneField.setVisible(false);
            emailLabel.setVisible(false);
            emailField.setVisible(false);
        }
    }

    private void handleSave(Runnable onBack) {
        User u = userSession.getCurrentUser();
        if (u == null) return;

        String username = usernameField.getText().trim();
        String name = nameField.getText().trim();
        String phone = phoneField.getText().trim();
        String email = emailField.getText().trim();
        String oldPw = new String(oldPasswordField.getPassword());
        String newPw = new String(newPasswordField.getPassword());
        String confirmPw = new String(confirmPasswordField.getPassword());

        if (!newPw.isEmpty() || !confirmPw.isEmpty() || !oldPw.isEmpty()) {
            if (!newPw.equals(confirmPw)) {
                JOptionPane.showMessageDialog(this, "New passwords do not match.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (oldPw.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Must provide old password to change password.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        UserService.Result result = userController.updateUserProfile(
                u.getDatabaseId(), username, name, phone, email, oldPw, newPw
        );

        switch (result) {
            case SUCCESS -> {
                JOptionPane.showMessageDialog(this, "Profile updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                userSession.login(userController.findById(u.getDatabaseId())); // Update the session's active object reference
                onBack.run();
            }
            case DUPLICATE_USERNAME -> JOptionPane.showMessageDialog(this, "Username is already taken.", "Duplicate Username", JOptionPane.WARNING_MESSAGE);
            case DUPLICATE_EMAIL -> JOptionPane.showMessageDialog(this, "Email is already associated with another account.", "Duplicate Email", JOptionPane.WARNING_MESSAGE);
            case INVALID_INPUT -> JOptionPane.showMessageDialog(this, "Invalid input. Check lengths and formats.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
            case INCORRECT_PASSWORD -> JOptionPane.showMessageDialog(this, "Old password is incorrect.", "Incorrect Password", JOptionPane.ERROR_MESSAGE);
        }
    }
}