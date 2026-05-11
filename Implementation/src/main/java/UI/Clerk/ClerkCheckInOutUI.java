package UI.Clerk;

import Controllers.ReservationController;
import Controllers.ShoppingController;
import Domain.People.UserSession;
import Domain.Reservations.Reservation;
import UI.Shopping.CombinedBillUI;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Clerk workflow: search reservations by guest name or confirmation number, then check in/out.
 */
public class ClerkCheckInOutUI extends JPanel {

    private final UserSession userSession;
    private final ReservationController reservationController;
    private final ShoppingController shoppingController;

    private final JTextField searchField = new JTextField(18);
    private final DefaultListModel<String> resultsModel = new DefaultListModel<>();
    private final JList<String> resultsList = new JList<>(resultsModel);

    private final List<Reservation> cache = new ArrayList<>();

    private final JLabel selectedLabel = new JLabel("Selected: —");
    private final JLabel activeLabel = new JLabel("Active: —");

    private final JButton backButton = new JButton();

    public ClerkCheckInOutUI(UserSession userSession,
                             ReservationController reservationController,
                             ShoppingController shoppingController) {
        this.userSession = userSession;
        this.reservationController = reservationController;
        this.shoppingController = shoppingController;

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
        checkInBtn.addActionListener(e -> handleCheckIn());
        checkOutBtn.addActionListener(e -> handleCheckOut());

        resultsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateSelectedLabels();
            }
        });

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            private void changed() {
                applyFilter();
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
        for (Reservation r : cache) {
            if (r.isCheckedOutOrExpired()) {
                continue;
            }

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
        if (selected == null) {
            return null;
        }
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

    private void handleCheckIn() {
        try {
            userSession.requireLoggedInClerk();
            Reservation r = getSelectedReservationOrNull();
            if (r == null) {
                JOptionPane.showMessageDialog(this, "Select a reservation first.", "No selection",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (r.isActive()) {
                JOptionPane.showMessageDialog(this,
                        "This reservation is already checked in.",
                        "Already active",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            if (r.getGuestUserId() == null) {
                JOptionPane.showMessageDialog(this,
                        "This reservation has no linked guest account. Link a guest username on the reservation before check-in.",
                        "Cannot check in",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            String conf = r.getConfirmationNumber();
            LocalDate today = LocalDate.now();
            LocalDate checkIn = r.getDateRange().getCheckInDate();

            String message;
            if (today.isBefore(checkIn)) {
                long daysUntil = ChronoUnit.DAYS.between(today, checkIn);
                String dayWord = daysUntil == 1 ? "day" : "days";
                message = "This reservation doesn't start for " + daysUntil + " " + dayWord + ".\n"
                        + "Are you sure you want to check the guest in?";
            } else {
                message = "Check in reservation " + conf + "?";
            }

            int ok = JOptionPane.showConfirmDialog(this, message, "Confirm check-in",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ok != JOptionPane.OK_OPTION) {
                return;
            }

            reservationController.clerkCheckIn(conf);
            refreshCache();
            applyFilter();
            JOptionPane.showMessageDialog(this, "Checked in " + conf + ".", "Success",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Check-in failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleCheckOut() {
        try {
            userSession.requireLoggedInClerk();
            Reservation r = getSelectedReservationOrNull();
            if (r == null) {
                JOptionPane.showMessageDialog(this, "Select a reservation first.", "No selection",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!r.isActive()) {
                JOptionPane.showMessageDialog(this,
                        "This reservation is not active and cannot be checked out.",
                        "Cannot check out",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            String conf = r.getConfirmationNumber();
            LocalDate today = LocalDate.now();
            LocalDate checkOutMorning = r.getDateRange().getCheckOutDate();
            long daysLeft = ChronoUnit.DAYS.between(today, checkOutMorning);

            String message;
            if (daysLeft > 0) {
                String dayWord = daysLeft == 1 ? "day" : "days";
                message = "This reservation still has " + daysLeft + " " + dayWord + " until checkout (morning of "
                        + checkOutMorning + ").\n"
                        + "Are you sure you want to check them out?";
            } else {
                message = "Check out reservation " + conf + "?";
            }

            int ok = JOptionPane.showConfirmDialog(this, message, "Confirm check-out",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ok != JOptionPane.OK_OPTION) {
                return;
            }

            Long guestId = r.getGuestUserId();
            reservationController.clerkCheckOut(conf);
            refreshCache();
            applyFilter();

            showCombinedBillAfterCheckout(conf, guestId);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Check-out failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showCombinedBillAfterCheckout(String confirmationNumber, Long guestUserId) {
        Window owner = SwingUtilities.getWindowAncestor(this);
        if (guestUserId == null) {
            JOptionPane.showMessageDialog(owner,
                    "Checked out " + confirmationNumber + ".\n"
                            + "No linked guest account on this reservation — a combined bill is not available.",
                    "Check-out complete",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JDialog dlg = new JDialog(owner, "Guest combined bill", Dialog.ModalityType.APPLICATION_MODAL);

        // Pass confirmationNumber here instead of guestUserId
        CombinedBillUI bill = new CombinedBillUI(shoppingController, dlg::dispose, null, confirmationNumber);

        bill.refresh();
        dlg.setContentPane(bill);
        dlg.pack();
        dlg.setSize(Math.max(dlg.getWidth(), 640), Math.max(dlg.getHeight(), 400));
        dlg.setLocationRelativeTo(owner);
        dlg.setVisible(true);
    }

    private static String formatRow(Reservation r) {
        String conf = r.getConfirmationNumber();
        String guest = r.getGuestName();
        String dates = r.getDateRange().getCheckInDate() + " → " + r.getDateRange().getCheckOutDate();
        return conf + " | " + guest + " | " + dates + (r.isActive() ? " | ACTIVE" : "");
    }
}
