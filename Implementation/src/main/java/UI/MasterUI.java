package UI;

import People.Guest;
import People.User;
import People.UserCatalog;
import People.UserSession;
import RoomCatalog.RoomCatalog;
import Utility.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class MasterUI {
    private final UserSession userSession;
    private final ReservationController reservationController;
    private final SearchController searchController;
    private final UserController userController;
    private AddUserUI addUserPanel = null;
    private AddRoomUI addRoomPanel = null;
    private ReserveRoomUI reservePanel = null;

    public MasterUI(UserSession userSession, ReservationController reservationController,
                            SearchController searchController, UserController userController) {
        this.userSession = userSession;
        this.reservationController = reservationController;
        this.searchController = searchController;
        this.userController = userController;
    }

    public void buildAndShowUI() {

        JFrame frame = new JFrame("Hotel Reservation App");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 450);
        frame.setLocationRelativeTo(null); // center on screen

        // CardLayout lets us swap panels in-place without opening new windows
        CardLayout cards = new CardLayout();
        JPanel root = new JPanel(cards);

        // ── Panels ────────────────────────────────────────────────────────────
        JPanel welcomePanel    = buildWelcomePanel(cards, root);
        addUserPanel = new AddUserUI(userController);
        addRoomPanel = new AddRoomUI(searchController.getRoomService());
        reservePanel = new ReserveRoomUI(userSession, reservationController);

        // Back Button
        ActionListener goBack = e -> cards.show(root, "WELCOME");
        String backMessage = "← Back to Welcome";
        addUserPanel.setBackAction(goBack, backMessage);
        addRoomPanel.setBackAction(goBack, backMessage);
        reservePanel.setBackAction(goBack, backMessage);

        root.add(welcomePanel, "WELCOME");
        root.add(addUserPanel, "ADD_USER");
        root.add(addRoomPanel, "ADD_ROOM");
        root.add(reservePanel, "RESERVE");

        frame.add(root);
        cards.show(root, "WELCOME");
        frame.setVisible(true);
    }

    /** Builds the welcome screen with three navigation buttons. */
    private JPanel buildWelcomePanel(CardLayout cards, JPanel root) {
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
        toReserve.addActionListener(e -> {
            cards.show(root, "RESERVE");
            reservePanel.refreshRoomOptions();
        });
        toAddRoom.addActionListener(e -> cards.show(root, "ADD_ROOM"));

        buttonPanel.add(toAddUser);
        buttonPanel.add(toReserve);
        buttonPanel.add(toAddRoom);

        panel.add(buttonPanel, BorderLayout.CENTER);

        return panel;
    }

    private void styleNavButton(JButton btn) {
        btn.setFont(new Font("SansSerif", Font.PLAIN, 16));
        btn.setPreferredSize(new Dimension(0, 45));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
}
