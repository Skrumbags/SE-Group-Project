package UI;

import Controllers.ReservationController;
import Domain.People.UserSession;
import Domain.Reservations.Reservation;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Clerk workflow: search reservations by guest name or confirmation number, then check in/out.
 */
public class ClerkCheckInOutUI extends JPanel {

    private final UserSession userSession;
    private final ReservationController reservationController;

    private final JTextField searchField = new JTextField(18);
    private final DefaultListModel<String> resultsModel = new DefaultListModel<>();
    private final JList<String> resultsList = new JList<>(resultsModel);

    private final List<Reservation> cache = new ArrayList<>();

    private final JLabel selectedLabel = new JLabel("Selected: —");
    private final JLabel activeLabel = new JLabel("Active: —");

    private final JButton backButton = new JButton();

    public ClerkCheckInOutUI(UserSession userSession, ReservationController reservationController) {
        this.userSession = userSession;
        this.reservationController = reservationController;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchRow.add(new JLabel("Search (guest name or confirmation #):"));
        searchRow.add(searchField);
        north.add(searchRow);
        north.add(Box.createVerticalStrut(8));

        resultsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultsList.setVisibleRowCount(10);
        resultsList.setSelectionBackground(new Color(24, 95, 165));
        resultsList.setSelectionForeground(Color.WHITE);
        resultsList.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        JScrollPane scroll = new JScrollPane(resultsList);
        scroll.setPreferredSize(new Dimension(560, 220));
        north.add(scroll);

        add(north, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(0, 1, 4, 4));
        center.add(selectedLabel);
        center.add(activeLabel);
        add(center, BorderLayout.CENTER);

        JButton refreshBtn = new JButton("Refresh");
        JButton checkInBtn = new JButton("Check in");
        JButton checkOutBtn = new JButton("Check out");

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        south.add(backButton);
        backButton.setVisible(false);
        south.add(refreshBtn);
        south.add(checkInBtn);
        south.add(checkOutBtn);
        add(south, BorderLayout.SOUTH);

        refreshBtn.addActionListener(e -> {
            refreshCache();
            applyFilter();
        });
        checkInBtn.addActionListener(e -> handleToggle(true));
        checkOutBtn.addActionListener(e -> handleToggle(false));

        resultsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateSelectedLabels();
            }
        });

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            private void changed() { applyFilter(); }
            @Override public void insertUpdate(DocumentEvent e) { changed(); }
            @Override public void removeUpdate(DocumentEvent e) { changed(); }
            @Override public void changedUpdate(DocumentEvent e) { changed(); }
        });

        refreshCache();
        applyFilter();
    }

    public void setBackAction(ActionListener goBack, String backMessage) {
        backButton.addActionListener(goBack);
        backButton.setLabel(backMessage);
        backButton.setVisible(true);
    }

    private void refreshCache() {
        cache.clear();
        cache.addAll(reservationController.listReservations());
    }

    private void applyFilter() {
        String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        resultsModel.clear();
        for (int i = 0; i < cache.size(); i++) {
            Reservation r = cache.get(i);
            String conf = r.getConfirmationNumber() == null ? "" : r.getConfirmationNumber();
            String guest = r.getGuestName() == null ? "" : r.getGuestName();
            String hay = (conf + " " + guest).toLowerCase();
            if (q.isBlank() || hay.contains(q)) {
                resultsModel.addElement(formatRow(r));
            }
        }
        if (!resultsModel.isEmpty()) {
            resultsList.setSelectedIndex(0);
        } else {
            selectedLabel.setText("Selected: —");
            activeLabel.setText("Active: —");
        }
    }

    private Reservation getSelectedReservationOrNull() {
        String selected = resultsList.getSelectedValue();
        if (selected == null) return null;
        // Match by confirmation number prefix in the formatted row.
        for (Reservation r : cache) {
            if (selected.startsWith(r.getConfirmationNumber())) {
                return r;
            }
        }
        return null;
    }

    private void updateSelectedLabels() {
        Reservation r = getSelectedReservationOrNull();
        if (r == null) {
            selectedLabel.setText("Selected: —");
            activeLabel.setText("Active: —");
            return;
        }
        selectedLabel.setText("Selected: " + r.getConfirmationNumber() + " | " + r.getGuestName());
        activeLabel.setText("Active: " + (r.isActive() ? "Yes (checked in)" : "No"));
    }

    private void handleToggle(boolean active) {
        try {
            userSession.requireLoggedInClerk();
            Reservation r = getSelectedReservationOrNull();
            if (r == null) {
                JOptionPane.showMessageDialog(this, "Select a reservation first.", "No selection",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            String conf = r.getConfirmationNumber();
            int ok = JOptionPane.showConfirmDialog(
                    this,
                    (active ? "Check in" : "Check out") + " reservation " + conf + "?",
                    "Confirm",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (ok != JOptionPane.OK_OPTION) {
                return;
            }

            if (active) {
                reservationController.clerkCheckIn(conf);
            } else {
                reservationController.clerkCheckOut(conf);
            }
            refreshCache();
            applyFilter();
            JOptionPane.showMessageDialog(this,
                    (active ? "Checked in " : "Checked out ") + conf + ".",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    active ? "Check-in failed" : "Check-out failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String formatRow(Reservation r) {
        String conf = r.getConfirmationNumber();
        String guest = r.getGuestName();
        String dates = r.getDateRange().getCheckInDate() + " → " + r.getDateRange().getCheckOutDate();
        return conf + " | " + guest + " | " + dates + (r.isActive() ? " | ACTIVE" : "");
    }
}

