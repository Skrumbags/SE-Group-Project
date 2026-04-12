package UI;

import People.Guest;
import People.User;
import People.UserCatalog;
import People.UserSession;
import RoomCatalog.RoomCatalog;
import Rooms.Room;
import Utility.*;
import java.util.Date;
import java.util.List;
import java.time.LocalDate;
import Rooms.RoomType;
import Utility.DateRange;
import Utility.SearchCriteria;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class MasterUI {
    private final UserSession userSession;
    private final ReservationController reservationController;
    private final SearchController searchController;
    private final UserController userController;

    //Additions for Check Avail on Home
    private JTextField beginDate = new JTextField(10);
    private JTextField endDate = new JTextField(10);

    public MasterUI(UserSession userSession, ReservationController reservationController,
                            SearchController searchController, UserController userController) {
        this.userSession = userSession;
        this.reservationController = reservationController;
        this.searchController = searchController;
        this.userController = userController;
    }

    public void buildAndShowUI() {

        /*JFrame frame = new JFrame("Hotel Reservation App");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 450);
        frame.setLocationRelativeTo(null); // center on screen

        // CardLayout lets us swap panels in-place without opening new windows
        CardLayout cards = new CardLayout();
        JPanel root = new JPanel(cards);

        // ── Panels ────────────────────────────────────────────────────────────
        JPanel welcomePanel    = buildWelcomePanel(cards, root);
        AddUserUI addUserPanel = new AddUserUI(userController);
        AddRoomUI addRoomPanel = new AddRoomUI(searchController.getRoomService());
        ReserveRoomUI reservePanel = new ReserveRoomUI(userSession, reservationController);

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
        frame.setVisible(true);*/

        //Hannah: Adjusted for Nav Bar
        JFrame frame = new JFrame("Hotel Reservation App");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 500);
        frame.setLocationRelativeTo(null);

        CardLayout cards = new CardLayout();
        JPanel root = new JPanel(cards);

        // ── Nav Bar ───────────────────────────────────────────────────────────
        // TODO: for general page rn (no user distinction), should change for clerk + guest + admin later
        JPanel navBar = new JPanel(new GridLayout(1, 3));
        JButton homeBtn = new JButton("Home");
        JButton addRoomBtn = new JButton("Add Room");
        JButton reserveBtn = new JButton("Reservations");
        JButton loginBtn = new JButton("Login");
        navBar.add(homeBtn);
        navBar.add(addRoomBtn);
        navBar.add(reserveBtn);
        navBar.add(loginBtn); // TODO: connect login case when written

        // ── Check Available ───────────────────────────────────────────────────
        JPanel checkAvailablityPanel = new JPanel(new GridLayout(1, 1));
        checkAvailablityPanel.add(new JLabel("Room Number:"));
        checkAvailablityPanel.add(beginDate);

        // ── Panels ────────────────────────────────────────────────────────────
        JPanel welcomePanel    = buildWelcomePanel(cards, root);
        AddUserUI addUserPanel = new AddUserUI(userController);
        AddRoomUI addRoomPanel = new AddRoomUI(searchController.getRoomService());
        ReserveRoomUI reservePanel = new ReserveRoomUI(userSession, reservationController);

        root.add(welcomePanel, "WELCOME");
        root.add(addUserPanel, "ADD_USER");
        root.add(addRoomPanel, "ADD_ROOM");
        root.add(reservePanel, "RESERVE");

        // ── Nav button actions ────────────────────────────────────────────────
        homeBtn.addActionListener(e -> cards.show(root, "WELCOME"));
        addRoomBtn.addActionListener(e -> cards.show(root, "ADD_ROOM"));
        reserveBtn.addActionListener(e -> cards.show(root, "RESERVE"));

        // ── Images ────────────────────────────────────────────────

        // ── Assemble frame ────────────────────────────────────────────────────
        frame.setLayout(new BorderLayout());
        frame.add(navBar, BorderLayout.NORTH);
        frame.add(root, BorderLayout.CENTER);

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

        JButton toAddUser = new JButton("Add a User");
        JButton toReserve = new JButton("Reserve a Room");
        JButton toAddRoom = new JButton("Add a Room");

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
        /*JPanel panel = new JPanel(new BorderLayout(10, 20));
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

        return panel;*/
    }

    private void styleNavButton(JButton btn) {
        btn.setFont(new Font("SansSerif", Font.PLAIN, 16));
        btn.setPreferredSize(new Dimension(0, 45));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
}
