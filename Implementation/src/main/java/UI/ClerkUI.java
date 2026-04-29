package UI;

import Domain.People.User;
import Domain.People.UserSession;

import javax.swing.*;
import java.awt.*;

/**
 * Clerk landing: shortcuts to add rooms and full reservation CRUD.
 */
public class ClerkUI extends JPanel {

    public ClerkUI(UserSession userSession, Runnable onAddRoom, Runnable onModifyRoom, Runnable onReservations, Runnable onHome) {
        setLayout(new BorderLayout(16, 16));
        setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));

        User u = userSession.getCurrentUser();
        String greeting = (u != null) ? ("Clerk — " + u.getName()) : "Clerk";
        JLabel title = new JLabel(greeting, SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        add(title, BorderLayout.NORTH);

        JPanel actions = new JPanel(new GridLayout(3, 1, 0, 12));
        JButton addRoom = new JButton("Add room");
        addRoom.addActionListener(e -> onAddRoom.run());
        JButton modifyRoom = new JButton("Modify room information");
        modifyRoom.addActionListener(e -> onModifyRoom.run());
        JButton reservations = new JButton("Reservations (list, create, edit, delete)");
        reservations.addActionListener(e -> onReservations.run());
        actions.add(addRoom);
        actions.add(modifyRoom);
        actions.add(reservations);
        add(actions, BorderLayout.CENTER);
    }
}
