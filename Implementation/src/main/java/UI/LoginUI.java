package UI;

import Domain.People.Admin;
import Domain.People.Clerk;
import Domain.People.Guest;
import Domain.People.User;
import Domain.People.UserSession;
import Controllers.LoginController;

import javax.swing.*;
import java.awt.*;

public class LoginUI extends JPanel {

    private JTextField usernameField = new JTextField(15);
    private JPasswordField passwordField = new JPasswordField(15);

    public LoginUI(LoginController loginController, UserSession userSession, Runnable onSessionChanged,
                   Runnable onAdminLogin, Runnable onClerkLogin, Runnable onGuestLogin,
                   Runnable onCreateUser, Runnable onCreateClerk) {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Hotel Login", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        add(title, gbc);

        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 1;
        add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        add(usernameField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        add(passwordField, gbc);

        JButton loginButton = new JButton("Login");
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        add(loginButton, gbc);

        loginButton.addActionListener(e -> {
            if (userSession.isLoggedIn()) {
                JOptionPane.showMessageDialog(this,
                        "You are already signed in. Sign out before signing in as a different user.",
                        "Already signed in", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());

            User user = loginController.login(username, password);

            if (user == null) {
                JOptionPane.showMessageDialog(this,
                        "Invalid username or password.",
                        "Login Failed", JOptionPane.ERROR_MESSAGE);
                return;
            }

            userSession.login(user);
            onSessionChanged.run();

            passwordField.setText("");

            // route to correct screen based on role
            if (user instanceof Admin) {
                onAdminLogin.run();
            } else if (user instanceof Clerk) {
                onClerkLogin.run();
            } else if (user instanceof Guest) {
                onGuestLogin.run();
            }
        });

        //Create a new user moved here >
        JButton createUser = new JButton("Create New User");
        createUser.addActionListener(e -> onCreateUser.run());
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        add(createUser, gbc);

    }

    private void styleNavButton(JButton btn) {
        btn.setFont(new Font("SansSerif", Font.PLAIN, 16));
        btn.setPreferredSize(new Dimension(0, 45));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
}