package UI;

import Domain.People.Admin;
import Domain.People.Clerk;
import Domain.People.Guest;
import Domain.People.User;
import Domain.People.UserSession;
import Domain.Rooms.Room;
import Controllers.*;
import java.util.Date;
import java.util.List;
import java.time.LocalDate;
import Domain.Rooms.RoomType;
import Domain.Shared.DateRange;
import Domain.Rooms.SearchCriteria;

import javax.swing.*;
import java.awt.*;

public class MasterUI {
    /** Shown under the welcome title when someone is signed in. */
    private JLabel homeUserStatusLabel;

    private final UserSession userSession;
    private final ReservationController reservationController;
    private final SearchController searchController;
    private final UserController userController;
    private AddUserUI addUserPanel = null;
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
        frame.setSize(900, 500);
        frame.setLocationRelativeTo(null);

        CardLayout cards = new CardLayout();
        JPanel root = new JPanel(cards);

        // ── Nav Bar (public shell: guests who are not signed in only see these two actions) ──
        JPanel navLeft = new JPanel();
        navLeft.setLayout(new BoxLayout(navLeft, BoxLayout.X_AXIS));
        JButton homeBtn = new JButton("Home");
        JButton reserveBtn = new JButton("Reservations");
        navLeft.add(homeBtn);

        JLabel navUserLabel = new JLabel("Not signed in");
        navUserLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        JButton sessionBtn = new JButton("Login");

        Runnable refreshSessionUi = () -> {
            User u = userSession.getCurrentUser();
            if (u != null) {
                navUserLabel.setText("Signed in as " + u.getUsername() + " (" + u.getRole() + ")");
                sessionBtn.setText("Logout");
                if (homeUserStatusLabel != null) {
                    homeUserStatusLabel.setText("Signed in as " + u.getUsername());
                }
            } else {
                navUserLabel.setText("Not signed in");
                sessionBtn.setText("Login");
                if (homeUserStatusLabel != null) {
                    homeUserStatusLabel.setText(" ");
                }
            }
            // Public shell: only Home until a guest or clerk signs in (no reservation shortcut for visitors).
            navLeft.removeAll();
            navLeft.add(homeBtn);
            if (u != null && !(u instanceof Admin)) {
                navLeft.add(Box.createHorizontalStrut(8));
                navLeft.add(reserveBtn);
            }
            navLeft.revalidate();
            navLeft.repaint();
        };

        sessionBtn.addActionListener(e -> {
            if (userSession.isLoggedIn()) {
                int ok = JOptionPane.showConfirmDialog(
                        frame,
                        "Sign out?",
                        "Logout",
                        JOptionPane.OK_CANCEL_OPTION
                );
                if (ok == JOptionPane.OK_OPTION) {
                    userSession.logout();
                    refreshSessionUi.run();
                    cards.show(root, "WELCOME");
                }
            } else {
                cards.show(root, "LOGIN");
            }
        });

        JPanel navRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 5));
        navRight.add(navUserLabel);
        navRight.add(sessionBtn);

        JPanel navBar = new JPanel(new BorderLayout());
        navBar.add(navLeft, BorderLayout.WEST);
        navBar.add(navRight, BorderLayout.EAST);

        // ── Panels ────────────────────────────────────────────────────────────
        JPanel welcomePanel    = buildWelcomePanel(cards, root);
        AddUserUI addUserPanel = new AddUserUI(userController);
        addUserPanel.setBackAction(e -> cards.show(root, "LOGIN"), "← Back to login");
        AddCA_UI addClerkAdmin = new AddCA_UI(userController);
        addClerkAdmin.setBackAction(e -> cards.show(root, "ADMIN"), "← Back to admin");
        ReserveRoomUI reservePanel = new ReserveRoomUI(userSession, reservationController);

        // Add-room is clerk-only: modal dialog, not a root card (public home never loads that UI).
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
                () -> cards.show(root, "CLERK")
        );
        JPanel adminPanel = new AdminUI(
                userSession,
                () -> cards.show(root, "ADD_USER_CLERK"),
                () -> cards.show(root, "WELCOME")
        );
        JPanel clerkPanel = new ClerkUI(
                userSession,
                openClerkAddRoomDialog,
                () -> {
                    clerkReservationsPanel.prepareShow();
                    cards.show(root, "CLERK_RES");
                },
                () -> cards.show(root, "WELCOME")
        );

        root.add(welcomePanel, "WELCOME");
        root.add(addUserPanel, "ADD_USER");
        root.add(reservePanel, "RESERVE");
        root.add(addClerkAdmin, "ADD_USER_CLERK");
        root.add(adminPanel, "ADMIN");
        root.add(clerkPanel, "CLERK");
        root.add(clerkReservationsPanel, "CLERK_RES");

        // ── Nav button actions ────────────────────────────────────────────────
        homeBtn.addActionListener(e -> cards.show(root, "WELCOME"));
        reserveBtn.addActionListener(e -> {
            User u = userSession.getCurrentUser();
            if (u == null) {
                JOptionPane.showMessageDialog(frame,
                        "Please log in to manage or create reservations.",
                        "Login required",
                        JOptionPane.INFORMATION_MESSAGE);
                cards.show(root, "LOGIN");
                return;
            }
            if (u instanceof Guest) {
                reservePanel.refreshRoomOptions();
                cards.show(root, "RESERVE");
            } else if (u instanceof Clerk) {
                clerkReservationsPanel.prepareShow();
                cards.show(root, "CLERK_RES");
            } else {
                JOptionPane.showMessageDialog(frame,
                        "Administrator accounts do not change reservations from this app.",
                        "Not available",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });

        LoginController loginController = new LoginController(
                userController.getUserCatalog(),
                userController.getSqliteUserPersistence());
        LoginUI loginPanel = new LoginUI(
                loginController,
                userSession,
                refreshSessionUi,
                () -> cards.show(root, "ADMIN"),
                () -> cards.show(root, "CLERK"),
                () -> cards.show(root, "WELCOME"),
                () -> cards.show(root, "ADD_USER")
        );
        root.add(loginPanel, "LOGIN");

        // ── Assemble frame ────────────────────────────────────────────────────
        frame.setLayout(new BorderLayout());
        frame.add(navBar, BorderLayout.NORTH);
        frame.add(root, BorderLayout.CENTER);

        cards.show(root, "WELCOME");
        refreshSessionUi.run();
        frame.setVisible(true);
    }

    /** Public home: welcome copy plus room search (no staff tools). */
    private JPanel buildWelcomePanel(CardLayout cards, JPanel root) {
        JPanel panel = new JPanel(new BorderLayout(10, 20));
        panel.setBorder(BorderFactory.createEmptyBorder(40, 60, 40, 60));

        // ── Welcome label + signed-in line (updated when session changes) ────
        JPanel northStack = new JPanel(new GridLayout(2, 1, 0, 6));
        JLabel welcome = new JLabel("Welcome to Hotel Reservation App", SwingConstants.CENTER);
        welcome.setFont(new Font("SansSerif", Font.BOLD, 22));
        northStack.add(welcome);
        homeUserStatusLabel = new JLabel(" ", SwingConstants.CENTER);
        homeUserStatusLabel.setFont(new Font("SansSerif", Font.PLAIN, 15));
        northStack.add(homeUserStatusLabel);
        panel.add(northStack, BorderLayout.NORTH);

        //Allows checking availability before login:
        // ── Availability checker ─────────────────────────────────────────────
        JPanel availPanel = new JPanel(new BorderLayout(5, 5));
        availPanel.setBorder(BorderFactory.createTitledBorder("Search Available Rooms"));

        // ── Search fields ────────────────────────────────────────────────────
        JPanel fieldsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));

        SpinnerDateModel beginModel = new SpinnerDateModel();
        JSpinner beginDate = new JSpinner(beginModel);
        beginDate.setEditor(new JSpinner.DateEditor(beginDate, "MM/dd/yyyy"));
        beginDate.setPreferredSize(new Dimension(110, 25));

        SpinnerDateModel endModel = new SpinnerDateModel();
        JSpinner endDate = new JSpinner(endModel);
        endDate.setEditor(new JSpinner.DateEditor(endDate, "MM/dd/yyyy"));
        endDate.setPreferredSize(new Dimension(110, 25));

        JComboBox<RoomType.FloorType> floorBox =
                new JComboBox<>(RoomType.FloorType.values());
        JComboBox<RoomType.BedType> bedBox =
                new JComboBox<>(RoomType.BedType.values());
        JTextField guestsField = new JTextField(3);
        JButton searchButton = new JButton("Search");

        fieldsPanel.add(new JLabel("From:"));    fieldsPanel.add(beginDate);
        fieldsPanel.add(new JLabel("To:"));      fieldsPanel.add(endDate);
        fieldsPanel.add(new JLabel("Floor:"));   fieldsPanel.add(floorBox);
        fieldsPanel.add(new JLabel("Bed:"));     fieldsPanel.add(bedBox);
        fieldsPanel.add(new JLabel("Guests:")); fieldsPanel.add(guestsField);
        fieldsPanel.add(searchButton);

        // ── Results list ─────────────────────────────────────────────────────
        JTextArea resultsArea = new JTextArea(3, 40);
        resultsArea.setEditable(false);
        resultsArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(resultsArea);

        availPanel.add(fieldsPanel, BorderLayout.NORTH);
        availPanel.add(scrollPane, BorderLayout.CENTER);

        // ── Search button action ─────────────────────────────────────────────
        searchButton.addActionListener(e -> {
            try {
                // convert spinner Date to LocalDate
                Date startDate = (Date) beginDate.getValue();
                Date endDateVal = (Date) endDate.getValue();
                LocalDate start = startDate.toInstant()
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                LocalDate end = endDateVal.toInstant()
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDate();

                int numGuests = Integer.parseInt(guestsField.getText().trim());

                RoomType roomType = new RoomType(
                        (RoomType.FloorType) floorBox.getSelectedItem(),
                        (RoomType.BedType) bedBox.getSelectedItem()
                );

                DateRange dateRange = new DateRange(start, end);
                SearchCriteria criteria = new SearchCriteria(dateRange, roomType, numGuests);
                List<Room> results = searchController.searchRooms(criteria);

                if (results.isEmpty()) {
                    resultsArea.setText("No available rooms found.");
                } else {
                    StringBuilder sb = new StringBuilder();
                    for (Room r : results) {
                        sb.append("Room ").append(r.getRoomNumber())
                                .append(" | ").append(r.getRoomType().getFloorType())
                                .append(" | ").append(r.getRoomType().getBedType())
                                .append(" | $").append(r.getMaxDailyRate())
                                .append(r.isSmoking() ? " | Smoking" : " | Non-smoking")
                                .append("\n");
                    }
                    resultsArea.setText(sb.toString());
                }

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(panel,
                        "Please enter a valid number of guests.",
                        "Invalid Input", JOptionPane.ERROR_MESSAGE);
            } catch (RuntimeException ex) {
                JOptionPane.showMessageDialog(panel,
                        ex.getMessage(),
                        "Invalid Dates", JOptionPane.ERROR_MESSAGE);
            }
        });

        panel.add(availPanel, BorderLayout.SOUTH);

        return panel;
    }
}
