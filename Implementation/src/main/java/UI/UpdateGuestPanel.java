package UI;

import Domain.People.*;
import Domain.Services.ReservationService;
import Domain.Services.RoomService;
import Domain.Services.ShoppingService;
import Domain.Services.UserService;
import TechnicalServices.Persistence.SqliteReservationPersistence;
import TechnicalServices.Persistence.SqliteStorePersistence;
import UI.MasterUI;
import Controllers.*;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;

import Controllers.*;
import Domain.People.*;
import TechnicalServices.Persistence.SchemaInstaller;

import javax.swing.*;
import java.sql.Connection;
import java.sql.DriverManager;

public class UpdateGuestPanel extends JPanel {

    private final UserSession userSession;
    private final UserService userService;

    private final JTextField nameField = new JTextField();
    private final JTextField phoneField = new JTextField();
    private final JTextField emailField = new JTextField();

    public UpdateGuestPanel(
            UserSession userSession,
            UserService userService,
            Runnable onBackToGuestHome
    ) {
        this.userSession = userSession;
        this.userService = userService;

        setLayout(new BorderLayout());

        JLabel title = new JLabel("Update Profile", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 20));
        add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(6, 1, 10, 10));
        form.add(new JLabel("Name:"));
        form.add(nameField);
        form.add(new JLabel("Phone:"));
        form.add(phoneField);
        form.add(new JLabel("Email:"));
        form.add(emailField);

        add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout());
        JButton saveBtn = new JButton("Save");
        JButton cancelBtn = new JButton("Cancel");

        buttons.add(saveBtn);
        buttons.add(cancelBtn);
        add(buttons, BorderLayout.SOUTH);

        // SAVE BUTTON
        saveBtn.addActionListener(e -> {
            Guest g = (Guest) userSession.getCurrentUser();
            if (g == null) return;

            UserService.Result result = userService.updateGuestProfile(
                    g.getDatabaseId(),
                    nameField.getText().trim(),
                    phoneField.getText().trim(),
                    emailField.getText().trim()
            );

            if (result == UserService.Result.SUCCESS) {
                JOptionPane.showMessageDialog(this, "Profile updated successfully");
                onBackToGuestHome.run();
            } else {
                JOptionPane.showMessageDialog(this, "Invalid input");
            }
        });

        // CANCEL BUTTON
        cancelBtn.addActionListener(e -> onBackToGuestHome.run());
    }

    public void refresh() {
        Guest g = (Guest) userSession.getCurrentUser();
        if (g != null) {
            nameField.setText(g.getName());
            phoneField.setText(g.getPhone());
            emailField.setText(g.getEmail());
        }
    }
}
