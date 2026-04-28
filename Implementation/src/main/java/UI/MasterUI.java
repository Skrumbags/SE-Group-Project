package UI;

import Domain.People.Clerk;
import Domain.People.User;
import Domain.People.UserSession;
import Controllers.*;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 * Composes role-specific shells: anonymous users see {@link PublicUI}; guests, clerks, and admins
 * each get their own shell after login.
 */
public class MasterUI {

    private final UserSession userSession;
    private final ReservationController reservationController;
    private final SearchController searchController;
    private final UserController userController;
    private final ShoppingController shoppingController;

    public MasterUI(UserSession userSession, ReservationController reservationController,
                    //SearchController searchController, UserController userController) {
//=======
    SearchController searchController, UserController userController,
                ShoppingController shoppingController) {
        this.userSession = userSession;
        this.reservationController = reservationController;
        this.searchController = searchController;
        this.userController = userController;
        this.shoppingController = shoppingController;
    }

    public void buildAndShowUI() {

        JFrame frame = new JFrame("Hotel Reservation App");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 500);
        frame.setLocationRelativeTo(null);

        CardLayout shellLayout = new CardLayout();
        JPanel shellRoot = new JPanel(shellLayout);

        JLabel navUserLabel = new JLabel(" ");
        navUserLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        JButton sessionBtn = new JButton("Logout");
        JPanel sessionStrip = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 5));
        sessionStrip.add(navUserLabel);
        sessionStrip.add(sessionBtn);
        sessionStrip.setVisible(false);

        CardLayout guestLayout = new CardLayout();
        JPanel guestShell = new JPanel(guestLayout);

        CardLayout clerkLayout = new CardLayout();
        JPanel clerkShell = new JPanel(clerkLayout);

        CardLayout adminLayout = new CardLayout();
        JPanel adminShell = new JPanel(adminLayout);

        ReserveRoomUI reservePanel = new ReserveRoomUI(userSession, reservationController);
        reservePanel.setBackAction(e -> guestLayout.show(guestShell, "HOME"), "← Back to guest home");

        CombinedBillUI billPanel = new CombinedBillUI(
                shoppingController,
                () -> guestLayout.show(guestShell, "HOME")
        );

        CartUI cartPanel = new CartUI(
                shoppingController,
                () -> guestLayout.show(guestShell, "HOME"),
                () -> guestLayout.show(guestShell, "SHOP")
        );

        ProductCatalogUI shopPanel = new ProductCatalogUI(
                shoppingController,
                () -> guestLayout.show(guestShell, "HOME"),
                () -> {
                    cartPanel.refresh();
                    guestLayout.show(guestShell, "CART");
                }
        );

        Runnable openClerkAddRoomDialog = () -> {
            if (!(userSession.getCurrentUser() instanceof Clerk)) {
                JOptionPane.showMessageDialog(frame,
                        "Only clerks can add rooms. Sign in as a clerk first.",
                        "Add room",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            JDialog addRoomDialog = new JDialog(frame, "Add room", true);
            AddRoomUI addRoomForm = new AddRoomUI(searchController.getRoomService());
            addRoomForm.setBackAction(e -> addRoomDialog.dispose(), "Close");
            addRoomDialog.setContentPane(addRoomForm);
            addRoomDialog.pack();
            addRoomDialog.setLocationRelativeTo(frame);
            addRoomDialog.setVisible(true);
        };

        ClerkReservationsUI clerkReservationsPanel = new ClerkReservationsUI(
                userSession,
                reservationController,
                () -> clerkLayout.show(clerkShell, "CLERK")
        );

        JPanel adminPanel = new AdminUI(
                userSession,
                () -> adminLayout.show(adminShell, "ADD_USER_CLERK"),
                () -> adminLayout.show(adminShell, "ADMIN")
        );
        AddCA_UI addClerkAdmin = new AddCA_UI(userController);
        addClerkAdmin.setBackAction(e -> adminLayout.show(adminShell, "ADMIN"), "← Back to admin");

        JPanel clerkPanel = new ClerkUI(
                userSession,
                openClerkAddRoomDialog,
                () -> {
                    clerkReservationsPanel.prepareShow();
                    clerkLayout.show(clerkShell, "CLERK_RES");
                },
                () -> clerkLayout.show(clerkShell, "CLERK")
        );

        JPanel guestHomeWrap = new JPanel(new BorderLayout());

        Runnable rebuildGuestHome = () -> {
            guestHomeWrap.removeAll();
            User u = userSession.getCurrentUser();
            if (u != null) {
                GuestUI guestHome = new GuestUI(
                        u,
                        () -> {
                            reservePanel.refreshRoomOptions();
                            guestLayout.show(guestShell, "RESERVE");
                        },
                        () -> guestLayout.show(guestShell, "SEARCH"),
                        () -> {
                            shopPanel.refresh();
                            guestLayout.show(guestShell, "SHOP");
                        },
                        () -> {
                            cartPanel.refresh();
                            guestLayout.show(guestShell, "CART");
                        },
                        () -> {
                            billPanel.refresh();
                            guestLayout.show(guestShell, "BILL");
                        }
                );
                guestHomeWrap.add(guestHome, BorderLayout.CENTER);
            }
            guestHomeWrap.revalidate();
            guestHomeWrap.repaint();
        };

        JPanel guestSearch = new JPanel(new BorderLayout(8, 8));
        guestSearch.setBorder(BorderFactory.createEmptyBorder(8, 12, 12, 12));
        JPanel guestSearchNorth = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        JButton guestSearchBack = new JButton("← Back to guest home");
        guestSearchBack.addActionListener(e -> guestLayout.show(guestShell, "HOME"));
        guestSearchNorth.add(guestSearchBack);
        guestSearch.add(guestSearchNorth, BorderLayout.NORTH);
        guestSearch.add(new RoomAvailabilityPanel(searchController), BorderLayout.CENTER);

        guestShell.add(guestHomeWrap, "HOME");
        guestShell.add(guestSearch, "SEARCH");
        guestShell.add(reservePanel, "RESERVE");
        guestShell.add(shopPanel, "SHOP");
        guestShell.add(cartPanel, "CART");
        guestShell.add(billPanel, "BILL");

        adminShell.add(adminPanel, "ADMIN");
        adminShell.add(addClerkAdmin, "ADD_USER_CLERK");

        clerkShell.add(clerkPanel, "CLERK");
        clerkShell.add(clerkReservationsPanel, "CLERK_RES");

        Runnable refreshSessionUi = () -> {
            User u = userSession.getCurrentUser();
            if (u != null) {
                navUserLabel.setText("Signed in as " + u.getUsername() + " (" + u.getRole() + ")");
                sessionStrip.setVisible(true);
            } else {
                navUserLabel.setText(" ");
                sessionStrip.setVisible(false);
            }
            sessionStrip.revalidate();
            sessionStrip.repaint();
        };

        LoginController loginController = new LoginController(
                userController.getUserCatalog(),
                userController.getSqliteUserPersistence());

        PublicUI publicUI = new PublicUI(
                searchController,
                loginController,
                userController,
                userSession,
                refreshSessionUi,
                () -> {
                    refreshSessionUi.run();
                    adminLayout.show(adminShell, "ADMIN");
                    shellLayout.show(shellRoot, "ADMIN");
                },
                () -> {
                    refreshSessionUi.run();
                    clerkLayout.show(clerkShell, "CLERK");
                    shellLayout.show(shellRoot, "CLERK");
                },
                () -> {
                    refreshSessionUi.run();
                    rebuildGuestHome.run();
                    guestLayout.show(guestShell, "HOME");
                    shellLayout.show(shellRoot, "GUEST");
                }
        );

        shellRoot.add(publicUI, "PUBLIC");
        shellRoot.add(guestShell, "GUEST");
        shellRoot.add(clerkShell, "CLERK");
        shellRoot.add(adminShell, "ADMIN");

        sessionBtn.addActionListener(e -> {
            if (!userSession.isLoggedIn()) {
                return;
            }
            int ok = JOptionPane.showConfirmDialog(
                    frame,
                    "Sign out?",
                    "Logout",
                    JOptionPane.OK_CANCEL_OPTION
            );
            if (ok == JOptionPane.OK_OPTION) {
                userSession.logout();
                refreshSessionUi.run();
                shellLayout.show(shellRoot, "PUBLIC");
                publicUI.showLanding();
            }
        });

        frame.setLayout(new BorderLayout());
        frame.add(sessionStrip, BorderLayout.NORTH);
        frame.add(shellRoot, BorderLayout.CENTER);

        shellLayout.show(shellRoot, "PUBLIC");
        publicUI.showLanding();
        refreshSessionUi.run();
        frame.setVisible(true);
    }
}
