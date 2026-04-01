/*
 *  Filename: Driver.java
 *  Date Created: 3/24/2026
 *  Date Last Modified: 3/24/2026
 *  Authors: XXX, XXX
 *  File Description:
 *      XXXX
 */
package Driver;

// Imports for Room + RoomType
import People.Guest;
import People.UserCatalog;
import People.UserSession;
import RoomCatalog.RoomCatalog;
import UI.AddRoomUI;
import UI.AddUserUI;
import UI.ReserveRoomUI;
import Utility.ReservationController;
import Utility.ReservationService;
import Utility.RoomService;

import javax.swing.*;
import java.awt.*;

public class Driver {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Driver::buildAndShowUI);
    }

    private static void buildAndShowUI() {
        // Shared application state — one instance each, passed by reference
        RoomCatalog roomCatalog = new RoomCatalog();
        UserCatalog userCatalog = new UserCatalog();
        ReservationService resService = new ReservationService();
        RoomService roomService = new RoomService(roomCatalog);
        UserSession userSession = new UserSession();
        userSession.login(new Guest("Matt", "testpw", "Matt Freeman", "911", "matt_freeman2@baylor.edu"));
        ReservationController ResC = new ReservationController(roomService, resService, userSession);

        JFrame frame = new JFrame("Hotel Reservation App");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 450);
        frame.setLocationRelativeTo(null); // center on screen

        // CardLayout lets us swap panels in-place without opening new windows
        CardLayout cards = new CardLayout();
        JPanel root = new JPanel(cards);

        // ── Panels ────────────────────────────────────────────────────────────
        JPanel welcomePanel    = buildWelcomePanel(cards, root);
        AddUserUI addUserPanel = new AddUserUI(userCatalog);
        AddRoomUI addRoomPanel = new AddRoomUI(roomCatalog);
        ReserveRoomUI reservePanel = new ReserveRoomUI(userSession, ResC);

        root.add(welcomePanel, "WELCOME");
        root.add(addUserPanel, "ADD_USER");
        root.add(addRoomPanel, "ADD_ROOM");
        root.add(reservePanel, "RESERVE");

        frame.add(root);
        cards.show(root, "WELCOME");
        frame.setVisible(true);
    }

    /** Builds the welcome screen with three navigation buttons. */
    private static JPanel buildWelcomePanel(CardLayout cards, JPanel root) {
        JPanel panel = new JPanel(new BorderLayout(10, 20));
        panel.setBorder(BorderFactory.createEmptyBorder(40, 60, 40, 60));

        // ── Welcome label ────────────────────────────────────────────────────
        JLabel welcome = new JLabel("Welcome to Hotel Reservation App", SwingConstants.CENTER);
        welcome.setFont(new Font("SansSerif", Font.BOLD, 22));
        panel.add(welcome, BorderLayout.NORTH);

        // ── Navigation buttons ───────────────────────────────────────────────
        JPanel buttonPanel = new JPanel(new GridLayout(3, 1, 0, 15));

        JButton toAddUser  = new JButton("Add a User");
        JButton toReserve  = new JButton("Reserve a Room");
        JButton toAddRoom  = new JButton("Add a Room");

        styleNavButton(toAddUser);
        styleNavButton(toReserve);
        styleNavButton(toAddRoom);

        toAddUser.addActionListener(e -> cards.show(root, "ADD_USER"));
        toReserve.addActionListener(e -> cards.show(root, "RESERVE"));
        toAddRoom.addActionListener(e -> cards.show(root, "ADD_ROOM"));

        buttonPanel.add(toAddUser);
        buttonPanel.add(toReserve);
        buttonPanel.add(toAddRoom);

        panel.add(buttonPanel, BorderLayout.CENTER);

        // ── Back button (invisible on welcome, shown on sub-panels via shared bar) ──
        // Each sub-panel can add its own back button, or you can add one globally:
        JButton backButton = new JButton("← Back to Welcome");
        backButton.addActionListener(e -> cards.show(root, "WELCOME"));
        panel.add(backButton, BorderLayout.SOUTH);
        backButton.setVisible(false); // not needed on the welcome screen itself

        return panel;
    }

    private static void styleNavButton(JButton btn) {
        btn.setFont(new Font("SansSerif", Font.PLAIN, 16));
        btn.setPreferredSize(new Dimension(0, 45));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
}