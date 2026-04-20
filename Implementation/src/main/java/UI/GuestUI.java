package UI;

import Domain.People.User;

import javax.swing.*;
import java.awt.*;

/**
 * Guest landing after sign-in: shortcuts to reservations and room availability search.
 */
public class GuestUI extends JPanel {

    public GuestUI(User user, Runnable onReservations, Runnable onSearchRooms) {

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        gbc.insets = new Insets(10, 10, 10, 10);

        String greeting = (user != null) ? ("Hello, " + user.getName() + "!") : "Hello!";
        JLabel nameLabel = new JLabel(greeting);
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 24));

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 1;

        panel.add(nameLabel, gbc);

        ImageIcon icon = new ImageIcon("employee.png");
        if (icon.getImage() != null) {
            Image img = icon.getImage().getScaledInstance(150, 150, Image.SCALE_SMOOTH);
            JLabel imageLabel = new JLabel(new ImageIcon(img));

            gbc.gridx = 1;
            gbc.gridy = 0;
            gbc.anchor = GridBagConstraints.EAST;
            gbc.fill = GridBagConstraints.NONE;
            gbc.weightx = 1;

            panel.add(imageLabel, gbc);
        }

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton reservationsBtn = new JButton("Reservations");
        reservationsBtn.addActionListener(e -> onReservations.run());
        JButton searchBtn = new JButton("Search available rooms");
        searchBtn.addActionListener(e -> onSearchRooms.run());
        buttonPanel.add(searchBtn);
        buttonPanel.add(reservationsBtn);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.SOUTHEAST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 1;

        panel.add(buttonPanel, gbc);

        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);
    }
}
