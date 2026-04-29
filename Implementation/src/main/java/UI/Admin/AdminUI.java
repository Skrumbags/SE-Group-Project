package UI.Admin;

import Controllers.UserController;
import Domain.People.User;
import Domain.People.UserSession;
import Domain.Services.UserService;
import TechnicalServices.Security.PasswordHasher;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal admin landing: create clerk accounts only (no reservation tools).
 */
public class AdminUI extends JPanel {

    public AdminUI(UserSession userSession, UserController userController, Runnable onCreateClerk, Runnable onHome) {
        setLayout(new BorderLayout(16, 16));
        setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));

        User u = userSession.getCurrentUser();
        String name = u != null ? u.getName() : "Admin";
        JLabel title = new JLabel("Administrator — " + name, SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        add(title, BorderLayout.NORTH);

        JLabel hint = new JLabel("<html><div style='text-align:center;width:420px'>"
                + "Create front-desk clerk accounts. Clerks can manage rooms and all reservations."
                + "</div></html>", SwingConstants.CENTER);
        hint.setFont(new Font("SansSerif", Font.PLAIN, 14));
        add(hint, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        JButton createClerk = new JButton("Create clerk account");
        createClerk.addActionListener(e -> onCreateClerk.run());
        JButton resetPassword = new JButton("Reset user password");
        resetPassword.addActionListener(e -> {
            try {
                userSession.requireLoggedInAdmin();
                showResetPasswordDialog(userController.getUserService());
            } catch (RuntimeException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Cannot reset password",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
        actions.add(createClerk);
        actions.add(resetPassword);
        add(actions, BorderLayout.SOUTH);
    }

    private void showResetPasswordDialog(UserService userService) {
        JTextField searchField = new JTextField(18);
        DefaultListModel<String> resultsModel = new DefaultListModel<>();
        JList<String> results = new JList<>(resultsModel);
        results.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        results.setVisibleRowCount(8);
        results.setSelectionBackground(new Color(24, 95, 165));
        results.setSelectionForeground(Color.WHITE);
        results.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        List<String> allUsernames = new ArrayList<>();
        for (User u : userService.getUsers()) {
            if (u != null && u.getUsername() != null) {
                allUsernames.add(u.getUsername());
            }
        }
        allUsernames.sort(String.CASE_INSENSITIVE_ORDER);

        Runnable applyFilter = () -> {
            String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
            resultsModel.clear();
            for (String uname : allUsernames) {
                if (q.isBlank() || uname.toLowerCase().contains(q)) {
                    resultsModel.addElement(uname);
                }
            }
            if (!resultsModel.isEmpty()) {
                results.setSelectedIndex(0);
            }
        };
        applyFilter.run();

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            private void changed() { applyFilter.run(); }
            @Override public void insertUpdate(DocumentEvent e) { changed(); }
            @Override public void removeUpdate(DocumentEvent e) { changed(); }
            @Override public void changedUpdate(DocumentEvent e) { changed(); }
        });

        JPasswordField pw1 = new JPasswordField(16);
        JPasswordField pw2 = new JPasswordField(16);

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchRow.add(new JLabel("Search username:"));
        searchRow.add(searchField);
        form.add(searchRow);
        form.add(Box.createVerticalStrut(8));

        JScrollPane scroll = new JScrollPane(results);
        scroll.setPreferredSize(new Dimension(420, 160));
        form.add(scroll);
        form.add(Box.createVerticalStrut(10));

        JPanel pwGrid = new JPanel(new GridLayout(0, 2, 8, 6));
        pwGrid.add(new JLabel("New password:"));
        pwGrid.add(pw1);
        pwGrid.add(new JLabel("Confirm password:"));
        pwGrid.add(pw2);
        form.add(pwGrid);

        int ok = JOptionPane.showConfirmDialog(
                this,
                form,
                "Reset user password",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
        if (ok != JOptionPane.OK_OPTION) {
            return;
        }

        String username = results.getSelectedValue();
        if (username == null || username.isBlank()) {
            JOptionPane.showMessageDialog(this, "Select a username from the list.", "Invalid user",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        User target = userService.findByUsername(username);
        if (target == null || target.getDatabaseId() == null) {
            JOptionPane.showMessageDialog(this, "User not found: " + username, "Invalid user",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        String p1 = new String(pw1.getPassword());
        String p2 = new String(pw2.getPassword());
        if (p1.isBlank()) {
            JOptionPane.showMessageDialog(this, "Password cannot be blank.", "Invalid password",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!p1.equals(p2)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match.", "Invalid password",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Reset password for \"" + username + "\"?",
                "Confirm password reset",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (confirm != JOptionPane.OK_OPTION) {
            return;
        }

        String encoded = PasswordHasher.hashPassword(p1);
        User updated = userService.updatePassword(target.getDatabaseId(), encoded);
        if (updated == null) {
            JOptionPane.showMessageDialog(this, "Password reset failed.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        JOptionPane.showMessageDialog(this, "Password reset for " + updated.getUsername() + ".", "Success",
                JOptionPane.INFORMATION_MESSAGE);
    }
}

