package UI;

import Controllers.ReservationController;
import Domain.People.UserSession;
import Domain.Reservations.Reservation;
import Domain.Reservations.ReservationSummary;
import Domain.Rooms.Room;
import Domain.Shared.DateRange;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Clerk-only CRUD for reservations: list, create (preview + confirm), update selected, delete selected.
 */
public class ClerkReservationsUI extends JPanel {

    private static final String DATE_PLACEHOLDER = "YYYY-MM-DD";

    private final UserSession userSession;
    private final ReservationController reservationController;
    private final Runnable onBack;

    private final DefaultListModel<String> listModel = new DefaultListModel<>();
    private final JList<String> reservationList = new JList<>(listModel);
    private final List<Reservation> rowCache = new ArrayList<>();

    private final JTextField confirmationField = new JTextField(18);
    private final JComboBox<Integer> roomCombo = new JComboBox<>();
    private final JTextField checkInField = new JTextField(12);
    private final JTextField checkOutField = new JTextField(12);
    private final JTextField guestNameField = new JTextField(18);
    private final JTextField creditCardField = new JTextField(18);
    private final JTextField guestUsernameField = new JTextField(14);

    private ReservationSummary currentPreview;

    public ClerkReservationsUI(UserSession userSession, ReservationController reservationController,
                               Runnable onBack) {
        this.userSession = userSession;
        this.reservationController = reservationController;
        this.onBack = onBack;

        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        reservationList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        reservationList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                fillFromSelection();
            }
        });

        JPanel north = new JPanel(new BorderLayout(4, 4));
        north.add(new JLabel("All reservations (select a row to edit or delete):"), BorderLayout.NORTH);
        north.add(new JScrollPane(reservationList), BorderLayout.CENTER);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        form.add(new JLabel("Confirmation # (read-only when editing):"), gbc);
        gbc.gridx = 1;
        form.add(confirmationField, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        form.add(new JLabel("Room:"), gbc);
        gbc.gridx = 1;
        form.add(roomCombo, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        form.add(new JLabel("Check-in:"), gbc);
        gbc.gridx = 1;
        form.add(checkInField, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        form.add(new JLabel("Check-out:"), gbc);
        gbc.gridx = 1;
        form.add(checkOutField, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        form.add(new JLabel("Guest name:"), gbc);
        gbc.gridx = 1;
        form.add(guestNameField, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        form.add(new JLabel("Credit card #:"), gbc);
        gbc.gridx = 1;
        form.add(creditCardField, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        form.add(new JLabel("Guest username (optional, link to account):"), gbc);
        gbc.gridx = 1;
        form.add(guestUsernameField, gbc);

        installDatePlaceholder(checkInField);
        installDatePlaceholder(checkOutField);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton backBtn = new JButton("← Back");
        JButton refreshBtn = new JButton("Refresh list");
        JButton newBtn = new JButton("New (clear form)");
        JButton previewBtn = new JButton("Calculate cost");
        JButton createBtn = new JButton("Confirm new reservation");
        JButton saveBtn = new JButton("Save changes");
        JButton deleteBtn = new JButton("Delete selected");

        backBtn.addActionListener(e -> onBack.run());
        refreshBtn.addActionListener(e -> refreshList());
        newBtn.addActionListener(e -> clearFormForNew());
        previewBtn.addActionListener(e -> handlePreview());
        createBtn.addActionListener(e -> handleCreateConfirm());
        saveBtn.addActionListener(e -> handleSave());
        deleteBtn.addActionListener(e -> handleDelete());

        buttons.add(backBtn);
        buttons.add(refreshBtn);
        buttons.add(newBtn);
        buttons.add(previewBtn);
        buttons.add(createBtn);
        buttons.add(saveBtn);
        buttons.add(deleteBtn);

        JPanel south = new JPanel(new BorderLayout(4, 4));
        south.add(form, BorderLayout.CENTER);
        south.add(buttons, BorderLayout.SOUTH);

        add(north, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

        refreshRoomCombo();
        refreshList();
    }

    public void refreshList() {
        rowCache.clear();
        rowCache.addAll(reservationController.listReservations());
        listModel.clear();
        for (Reservation r : rowCache) {
            listModel.addElement(formatRow(r));
        }
    }

    /** Call before showing this panel so room numbers and rows match the database. */
    public void prepareShow() {
        refreshRoomCombo();
        refreshList();
    }

    private void refreshRoomCombo() {
        roomCombo.removeAllItems();
        for (Room r : reservationController.getRooms()) {
            roomCombo.addItem(r.getRoomNumber());
        }
    }

    private static String formatRow(Reservation r) {
        return r.getConfirmationNumber()
                + " | Room " + r.getRoom().getRoomNumber()
                + " | " + r.getDateRange().getCheckInDate()
                + " → " + r.getDateRange().getCheckOutDate()
                + " | " + r.getGuestName();
    }

    private void fillFromSelection() {
        int i = reservationList.getSelectedIndex();
        if (i < 0 || i >= rowCache.size()) {
            return;
        }
        Reservation r = rowCache.get(i);
        confirmationField.setText(r.getConfirmationNumber());
        confirmationField.setEditable(false);
        roomCombo.setSelectedItem(r.getRoom().getRoomNumber());
        setDateField(checkInField, r.getDateRange().getCheckInDate());
        setDateField(checkOutField, r.getDateRange().getCheckOutDate());
        guestNameField.setText(r.getGuestName());
        creditCardField.setText("");
        guestUsernameField.setText("");
        currentPreview = null;
    }

    private void clearFormForNew() {
        reservationList.clearSelection();
        confirmationField.setText("(assigned on save)");
        confirmationField.setEditable(false);
        if (roomCombo.getItemCount() > 0) {
            roomCombo.setSelectedIndex(0);
        }
        resetDatePlaceholder(checkInField);
        resetDatePlaceholder(checkOutField);
        guestNameField.setText("");
        creditCardField.setText("");
        guestUsernameField.setText("");
        currentPreview = null;
    }

    private void handlePreview() {
        try {
            userSession.requireLoggedInClerk();
            int roomNumber = (Integer) roomCombo.getSelectedItem();
            String guestName = guestNameField.getText();
            String card = creditCardField.getText();
            LocalDate in = LocalDate.parse(dateFieldText(checkInField));
            LocalDate out = LocalDate.parse(dateFieldText(checkOutField));
            DateRange range = new DateRange(in, out);
            currentPreview = reservationController.clerkBuildPreview(roomNumber, guestName, card, range);
            JOptionPane.showMessageDialog(this, currentPreview.toString(), "Reservation summary",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalStateException | IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Cannot preview", JOptionPane.ERROR_MESSAGE);
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Dates must be YYYY-MM-DD.", "Invalid date",
                    JOptionPane.ERROR_MESSAGE);
        } catch (NullPointerException ex) {
            JOptionPane.showMessageDialog(this, "Select a room and enter valid dates.", "Missing data",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    private void handleCreateConfirm() {
        try {
            userSession.requireLoggedInClerk();
            if (currentPreview == null) {
                JOptionPane.showMessageDialog(this, "Click Calculate cost first.", "Missing preview",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            Long guestId = reservationController.resolveGuestUserIdForLink(guestUsernameField.getText());
            String conf = reservationController.clerkConfirmReservation(currentPreview, true, guestId);
            currentPreview = null;
            JOptionPane.showMessageDialog(this, "Created reservation " + conf, "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            refreshList();
            clearFormForNew();
        } catch (IllegalStateException | IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Cannot create", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleSave() {
        try {
            userSession.requireLoggedInClerk();
            int idx = reservationList.getSelectedIndex();
            if (idx < 0) {
                JOptionPane.showMessageDialog(this, "Select a reservation in the list to update.",
                        "Nothing selected", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Reservation existing = rowCache.get(idx);
            String conf = existing.getConfirmationNumber();
            int roomNumber = (Integer) roomCombo.getSelectedItem();
            LocalDate in = LocalDate.parse(dateFieldText(checkInField));
            LocalDate out = LocalDate.parse(dateFieldText(checkOutField));
            DateRange range = new DateRange(in, out);
            Long guestId = reservationController.resolveGuestUserIdForLink(guestUsernameField.getText());
            reservationController.clerkUpdateReservation(
                    conf,
                    roomNumber,
                    range,
                    guestNameField.getText(),
                    creditCardField.getText(),
                    guestId
            );
            JOptionPane.showMessageDialog(this, "Reservation updated.", "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            refreshList();
        } catch (IllegalStateException | IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Cannot update", JOptionPane.ERROR_MESSAGE);
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Dates must be YYYY-MM-DD.", "Invalid date",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleDelete() {
        try {
            userSession.requireLoggedInClerk();
            int idx = reservationList.getSelectedIndex();
            if (idx < 0) {
                JOptionPane.showMessageDialog(this, "Select a reservation to delete.", "Nothing selected",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            String conf = rowCache.get(idx).getConfirmationNumber();
            int ok = JOptionPane.showConfirmDialog(this,
                    "Delete reservation " + conf + "?",
                    "Confirm delete",
                    JOptionPane.OK_CANCEL_OPTION);
            if (ok != JOptionPane.OK_OPTION) {
                return;
            }
            reservationController.clerkDeleteReservation(conf);
            JOptionPane.showMessageDialog(this, "Reservation deleted.", "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            refreshList();
            clearFormForNew();
        } catch (IllegalStateException | IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Cannot delete", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String dateFieldText(JTextField field) {
        String t = field.getText().trim();
        if (t.isEmpty() || DATE_PLACEHOLDER.equals(t)) {
            return "";
        }
        return t;
    }

    private static void installDatePlaceholder(JTextField field) {
        Color normalFg = UIManager.getColor("TextField.foreground");
        Color inactiveFg = UIManager.getColor("TextField.inactiveForeground");
        final Color hintColor = inactiveFg != null ? inactiveFg : Color.GRAY;
        final Color normalColor = normalFg != null ? normalFg : Color.BLACK;
        field.setText(DATE_PLACEHOLDER);
        field.setForeground(hintColor);
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (DATE_PLACEHOLDER.equals(field.getText())) {
                    field.setText("");
                    field.setForeground(normalColor);
                }
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setText(DATE_PLACEHOLDER);
                    field.setForeground(hintColor);
                }
            }
        });
    }

    private static void setDateField(JTextField field, LocalDate date) {
        field.setText(date.toString());
        field.setForeground(UIManager.getColor("TextField.foreground"));
    }

    private static void resetDatePlaceholder(JTextField field) {
        field.setText(DATE_PLACEHOLDER);
        Color inactiveFg = UIManager.getColor("TextField.inactiveForeground");
        field.setForeground(inactiveFg != null ? inactiveFg : Color.GRAY);
    }
}
