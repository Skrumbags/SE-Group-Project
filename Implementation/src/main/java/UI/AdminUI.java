package UI;

import Domain.People.User;
import Domain.People.UserSession;

import javax.swing.*;
import java.awt.*;

/**
 * Minimal admin landing: create clerk accounts only (no reservation tools).
 */
public class AdminUI extends JPanel {

    public AdminUI(UserSession userSession, Runnable onCreateClerk, Runnable onHome) {
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
        JButton home = new JButton("Back to home");
        home.addActionListener(e -> onHome.run());
        actions.add(createClerk);
        actions.add(home);
        add(actions, BorderLayout.SOUTH);
    }
}
