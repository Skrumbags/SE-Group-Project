package UI;

import Controllers.UserController;
import Domain.People.User;

import javax.swing.*;
import java.awt.*;

public class AdminUI extends JPanel {
    // After logging in as admin

    public AdminUI(UserController usercontroller, User user, Runnable onAddUserClerk) {

        // Sections off information in grids
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        gbc.insets = new Insets(10, 10, 10, 10);

        // Top-Left: Name and Welcome
        String greeting = (user != null) ? ("Hello, " + user.getName() + "!") : "Hello!";
        JLabel nameLabel = new JLabel(greeting);
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 24));

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE; // reset for each component
        gbc.weightx = 1;

        panel.add(nameLabel, gbc);

        // Top-Right: Image
        ImageIcon icon = new ImageIcon("employee.png");
        Image img = icon.getImage().getScaledInstance(150, 150, Image.SCALE_SMOOTH);
        JLabel imageLabel = new JLabel(new ImageIcon(img));

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE; // reset for each component
        gbc.weightx = 1;

        panel.add(imageLabel, gbc);

        // Bottom-Right: Buttons
        JButton manageUsersButton = new JButton("Manage Users");

        manageUsersButton.addActionListener(e -> {
            onAddUserClerk.run();
        });

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.SOUTHEAST;
        gbc.fill = GridBagConstraints.NONE; // reset for each component
        gbc.weightx = 1;

        panel.add(manageUsersButton);

        // Add everything together:
        add(panel, BorderLayout.CENTER);
    }
}
