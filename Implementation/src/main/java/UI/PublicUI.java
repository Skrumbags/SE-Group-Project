package UI;

import Controllers.LoginController;
import Controllers.SearchController;
import Controllers.UserController;
import Domain.People.UserSession;

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
            LoginController loginController,
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

        JPanel northStack = new JPanel(new GridLayout(2, 1, 0, 6));
        JLabel welcome = new JLabel("Welcome to Hotel Reservation App", SwingConstants.CENTER);
        welcome.setFont(new Font("SansSerif", Font.BOLD, 22));
        northStack.add(welcome);
        JLabel tag = new JLabel("Search availability or sign in to book.", SwingConstants.CENTER);
        tag.setFont(new Font("SansSerif", Font.PLAIN, 15));
        northStack.add(tag);
        landing.add(northStack, BorderLayout.NORTH);
        landing.add(new RoomAvailabilityPanel(searchController), BorderLayout.SOUTH);

        AddUserUI addUserPanel = new AddUserUI(userController);
        addUserPanel.setBackAction(e -> bodyCards.show(body, "LOGIN"), "← Back to login");

        LoginUI loginPanel = new LoginUI(
                loginController,
                userSession,
                onSessionChanged,
                onAdminLogin,
                onClerkLogin,
                onGuestLogin,
                () -> bodyCards.show(body, "ADD_USER")
        );

        body.add(landing, "LANDING");
        body.add(loginPanel, "LOGIN");
        body.add(addUserPanel, "ADD_USER");
        add(body, BorderLayout.CENTER);

        homeBtn.addActionListener(e -> bodyCards.show(body, "LANDING"));
        loginNavBtn.addActionListener(e -> bodyCards.show(body, "LOGIN"));
    }

    public void showLanding() {
        bodyCards.show(body, "LANDING");
    }
}
