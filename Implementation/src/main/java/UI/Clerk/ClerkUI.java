package UI.Clerk;

import Controllers.ReservationController;
import Domain.People.User;
import Domain.People.UserSession;
import Domain.Reservations.Reservation;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Clerk landing: guests currently checked in (searchable), plus shortcuts to rooms and reservation CRUD.
 */
public class ClerkUI extends JPanel {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("MMM d");
    private static final Dimension ACTION_BTN_SIZE = new Dimension(200, 48);

    private final ReservationController reservationController;
    private final Consumer<String> onOpenCheckedInReservation;

    private final List<Reservation> allCheckedIn = new ArrayList<>();
    private final JTextField checkedInSearchField = new JTextField(24);
    private final DefaultListModel<Reservation> checkedInModel = new DefaultListModel<>();
    private final JList<Reservation> checkedInList = new JList<>(checkedInModel);

    public ClerkUI(UserSession userSession,
                   Runnable onAddRoom,
                   Runnable onModifyRoom,
                   Runnable onCheckInOut,
                   Runnable onReservations,
                   Runnable onEditProfile,
                   ReservationController reservationController,
                   Consumer<String> onOpenCheckedInReservation) {
        this.reservationController = reservationController;
        this.onOpenCheckedInReservation = onOpenCheckedInReservation;

        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                refreshCheckedInOverview();
            }
        });

        setLayout(new BorderLayout(16, 16));
        setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));

        User u = userSession.getCurrentUser();
        String greeting = (u != null) ? ("Clerk — " + u.getName()) : "Clerk";
        JLabel title = new JLabel(greeting, SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        add(title, BorderLayout.NORTH);

        JPanel overview = new JPanel(new BorderLayout(8, 8));
        overview.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                "Guests checked in today (" + LocalDate.now() + ")",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("SansSerif", Font.BOLD, 13)));

        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        searchRow.add(new JLabel("Search (guest name or confirmation #):"));
        searchRow.add(checkedInSearchField);
        overview.add(searchRow, BorderLayout.NORTH);

        checkedInList.setVisibleRowCount(8);
        checkedInList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        checkedInList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Reservation r) {
                    String guest = r.getGuestName() != null ? r.getGuestName() : "(no name)";
                    String in = r.getDateRange().getCheckInDate().format(DAY);
                    String out = r.getDateRange().getCheckOutDate().format(DAY);
                    setText(guest + "  ·  Room " + r.getRoom().getRoomNumber()
                            + "  ·  " + r.getConfirmationNumber()
                            + "  ·  " + in + " → " + out);
                }
                return this;
            }
        });
        checkedInList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int i = checkedInList.locationToIndex(e.getPoint());
                if (i < 0 || i >= checkedInModel.getSize()) {
                    return;
                }
                Rectangle cell = checkedInList.getCellBounds(i, i);
                if (cell == null || !cell.contains(e.getPoint())) {
                    return;
                }
                checkedInList.setSelectedIndex(i);
                openSelectedReservation();
            }
        });

        JLabel hint = new JLabel("Click a guest row to open that reservation for editing.");
        hint.setFont(new Font("SansSerif", Font.ITALIC, 11));
        hint.setForeground(new Color(90, 90, 90));
        overview.add(new JScrollPane(checkedInList), BorderLayout.CENTER);
        overview.add(hint, BorderLayout.SOUTH);

        add(overview, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        Font actionFont = new Font("SansSerif", Font.PLAIN, 14);
        JButton addRoom = styleActionButton(new JButton("Add room"), actionFont);
        addRoom.addActionListener(e -> onAddRoom.run());
        JButton modifyRoom = styleActionButton(new JButton("Modify room information"), actionFont);
        modifyRoom.addActionListener(e -> onModifyRoom.run());
        JButton checkInOut = styleActionButton(new JButton("Check in / Check out guest"), actionFont);
        checkInOut.addActionListener(e -> onCheckInOut.run());
        JButton reservations = styleActionButton(new JButton("Reservations"), actionFont);
        reservations.setToolTipText("List, create, edit, and delete reservations");
        reservations.addActionListener(e -> onReservations.run());
        JButton editProfile = styleActionButton(new JButton("Edit profile"), actionFont);
        editProfile.addActionListener(e -> onEditProfile.run());
        actions.add(addRoom);
        actions.add(modifyRoom);
        actions.add(checkInOut);
        actions.add(reservations);
        actions.add(editProfile);
        add(actions, BorderLayout.SOUTH);

        checkedInSearchField.getDocument().addDocumentListener(new DocumentListener() {
            private void changed() {
                applyCheckedInFilter();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                changed();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                changed();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                changed();
            }
        });
    }

    private static JButton styleActionButton(JButton b, Font font) {
        b.setFont(font);
        b.setPreferredSize(ACTION_BTN_SIZE);
        b.setMinimumSize(new Dimension(160, 44));
        return b;
    }

    private void openSelectedReservation() {
        Reservation r = checkedInList.getSelectedValue();
        if (r == null || r.getConfirmationNumber() == null) {
            return;
        }
        onOpenCheckedInReservation.accept(r.getConfirmationNumber());
    }

    public void refreshCheckedInOverview() {
        allCheckedIn.clear();
        try {
            allCheckedIn.addAll(reservationController.listCheckedInGuestsToday());
        } catch (IllegalStateException ignored) {
            // Not logged in as clerk
        }
        applyCheckedInFilter();
    }

    private void applyCheckedInFilter() {
        String q = checkedInSearchField.getText() == null ? "" : checkedInSearchField.getText().trim().toLowerCase();
        checkedInModel.clear();
        for (Reservation r : allCheckedIn) {
            String conf = r.getConfirmationNumber() == null ? "" : r.getConfirmationNumber();
            String guest = r.getGuestName() == null ? "" : r.getGuestName();
            String hay = (conf + " " + guest).toLowerCase();
            if (q.isBlank() || hay.contains(q)) {
                checkedInModel.addElement(r);
            }
        }
    }
}
