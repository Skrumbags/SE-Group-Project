package UI.Guest;

import Domain.People.User;

import javax.swing.*;
import java.awt.*;

/**
 * Guest landing after sign-in: shortcuts to reservations and room availability search.
 */
public class GuestUI extends JPanel {

    public GuestUI(User user,
                   Runnable onReservations,
                   Runnable onSearchRooms,
                   Runnable onShopBrowse,
                   Runnable onShopCart,
                   Runnable onCombinedBill,
                   Runnable updateGuestPanel,
                   Runnable onManageReservations) {

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        gbc.insets = new Insets(10, 10, 10, 10);

        String greeting = (user != null) ? ("Hello, " + user.getName() + "!") : "Hello!";
        JLabel nameLabel = new JLabel(greeting);
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 24));

        int row = 0;
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 1;
        gbc.weighty = 0;

        panel.add(nameLabel, gbc);

        ImageIcon icon = new ImageIcon("employee.png");
        if (icon.getImage() != null) {
            Image img = icon.getImage().getScaledInstance(150, 150, Image.SCALE_SMOOTH);
            JLabel imageLabel = new JLabel(new ImageIcon(img));

            gbc.gridy = row++;
            gbc.anchor = GridBagConstraints.CENTER;

            panel.add(imageLabel, gbc);
        }

        JPanel buttonPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        JPanel bottomRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        JButton reservationsBtn = new JButton("Reservations");
        reservationsBtn.addActionListener(e -> onReservations.run());
        JButton searchBtn = new JButton("Search available rooms");
        searchBtn.addActionListener(e -> onSearchRooms.run());
        JButton shopBtn = new JButton("Shopping cart");
        shopBtn.addActionListener(e -> onShopBrowse.run());
        JButton cartBtn = new JButton("View cart");
        cartBtn.addActionListener(e -> onShopCart.run());
        JButton billBtn = new JButton("View combined bill");
        billBtn.addActionListener(e -> onCombinedBill.run());
        JButton editProfile = new JButton("Edit profile");
        editProfile.addActionListener(e -> { updateGuestPanel.run();});
        JButton manageBtn = new JButton("View My Reservations");
        manageBtn.addActionListener(e -> onManageReservations.run());

        topRow.add(searchBtn);
        topRow.add(reservationsBtn);
        topRow.add(manageBtn);
        topRow.add(editProfile);
        bottomRow.add(shopBtn);
        bottomRow.add(cartBtn);
        bottomRow.add(billBtn);

        buttonPanel.add(topRow);
        buttonPanel.add(bottomRow);

        gbc.gridy = row;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(buttonPanel, gbc);

        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);
    }
}

