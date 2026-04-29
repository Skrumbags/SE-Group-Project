package UI.Other;

import Controllers.SearchController;
import Controllers.UserController;
import Domain.People.UserSession;
import UI.Guest.CreateAccountUI;

import javax.swing.*;
import java.awt.*;

/**
 * Full experience for users who are not signed in: home / room search, login, and guest sign-up.
 */
public class PublicUI extends JPanel {

    private final CardLayout bodyCards = new CardLayout();
    private final JPanel body = new JPanel(bodyCards);

    public PublicUI(
            SearchController searchController,
            UserController userController,
            UserSession userSession,
            Runnable onSessionChanged,
            Runnable onAdminLogin,
            Runnable onClerkLogin,
            Runnable onGuestLogin) {

        setLayout(new BorderLayout());

        JPanel nav = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        JButton homeBtn = new JButton("Home");
        JButton loginNavBtn = new JButton("Login");
        nav.add(homeBtn);
        nav.add(loginNavBtn);
        add(nav, BorderLayout.NORTH);

        JPanel landing = new JPanel(new BorderLayout(10, 20));
        landing.setBorder(BorderFactory.createEmptyBorder(40, 60, 40, 60));

        CreateAccountUI createAccountPanel = new CreateAccountUI(userController);
        createAccountPanel.setBackAction(e -> bodyCards.show(body, "LOGIN"), "← Back to login");

        LoginUI loginPanel = new LoginUI(
                userController,
                userSession,
                onSessionChanged,
                onAdminLogin,
                onClerkLogin,
                onGuestLogin,
                () -> {
                    createAccountPanel.refresh();
                    bodyCards.show(body, "ADD_USER");
                }
        );

        JPanel northStack = new JPanel(new GridLayout(2, 1, 0, 6));
        JLabel welcome = new JLabel("Welcome to Hotel Reservation App", SwingConstants.CENTER);
        welcome.setFont(new Font("SansSerif", Font.BOLD, 22));
        northStack.add(welcome);
        JLabel tag = new JLabel("Search availability or sign in to book.", SwingConstants.CENTER);
        tag.setFont(new Font("SansSerif", Font.PLAIN, 15));
        northStack.add(tag);
        landing.add(northStack, BorderLayout.NORTH);
        landing.add(new RoomAvailabilityPanel(searchController, (room, range) -> {
            userSession.setPendingReservation(new UserSession.PendingReservation(
                    room.getRoomNumber(), range.getCheckInDate(), range.getCheckOutDate()));
            JOptionPane.showMessageDialog(this,
                    "Sign in or create a guest account to complete your reservation.\n\nRoom "
                            + room.getRoomNumber() + ": "
                            + range.getCheckInDate() + " → " + range.getCheckOutDate() + " (checkout morning).",
                    "Sign in to reserve",
                    JOptionPane.INFORMATION_MESSAGE);
            loginPanel.refresh();
            bodyCards.show(body, "LOGIN");
        }), BorderLayout.SOUTH);



        body.add(landing, "LANDING");
        body.add(loginPanel, "LOGIN");
        body.add(createAccountPanel, "ADD_USER");
        add(body, BorderLayout.CENTER);

        homeBtn.addActionListener(e -> bodyCards.show(body, "LANDING"));
        loginNavBtn.addActionListener(e -> {
            loginPanel.refresh();
            bodyCards.show(body, "LOGIN");
        });
    }

    public void showLanding() {
        bodyCards.show(body, "LANDING");
    }
}
