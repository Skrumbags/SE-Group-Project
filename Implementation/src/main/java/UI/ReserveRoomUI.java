package UI;

import People.UserSession;
import Reservations.ReservationSummary;
import Rooms.Room;
import Utility.DateRange;
import Utility.ReservationController;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Reserve-room UI flow (steps 1-8):
 * 1) Guest selects a room
 * 2) UI requests guest info + stay dates
 * 4-5) Use case validates and builds a cost summary
 * 6-8) Guest confirms; reservation is saved; confirmation number shown
 */
public class ReserveRoomUI extends JPanel {

    private static final String DATE_PLACEHOLDER = "YYYY-MM-DD";

    private final ReservationController ResC;
    private final UserSession userSession;

    private final JComboBox<Integer> roomNumberCombo;
    private final JTextField guestNameField = new JTextField(15);
    private final JTextField creditCardField = new JTextField(16);
    private final JTextField checkInDateField = new JTextField(10);
    private final JTextField checkOutDateField = new JTextField(10);
    private final JButton backButton = new JButton();

    private ReservationSummary currentPreview;

    public ReserveRoomUI(UserSession userSession, ReservationController ResC) {
        this.userSession = userSession;
        this.ResC = ResC;

        setLayout(new GridLayout(8, 2, 5, 5));

        // Step 1: select desired room
        roomNumberCombo = new JComboBox<>();
        refreshRoomOptions();

        add(new JLabel("Room Number:"));
        add(roomNumberCombo);

        // Step 2: request guest info + dates
        add(new JLabel("Guest Name:"));
        add(guestNameField);

        add(new JLabel("Credit Card #:"));
        add(creditCardField);

        add(new JLabel("Check-in (YYYY-MM-DD):"));
        add(checkInDateField);

        add(new JLabel("Check-out (YYYY-MM-DD):"));
        add(checkOutDateField);

        installDatePlaceholder(checkInDateField);
        installDatePlaceholder(checkOutDateField);

        // Step 5-6: show summary then confirm
        JButton previewButton = new JButton("Calculate Cost");
        JButton confirmButton = new JButton("Confirm Reservation");
        add(previewButton);
        add(confirmButton);

        previewButton.addActionListener(e -> handlePreview());
        confirmButton.addActionListener(e -> handleConfirm());

        add(backButton);
        backButton.setVisible(false);
    }

    public void refreshRoomOptions() {
        roomNumberCombo.removeAllItems();
        for (Room r : ResC.getRooms()) {
            roomNumberCombo.addItem(r.getRoomNumber());
        }
    }

    private void handlePreview() {
        try {
            int roomNumber = (Integer) roomNumberCombo.getSelectedItem();
            String guestName = guestNameField.getText();
            String creditCard = creditCardField.getText();

            String checkInRaw = dateFieldText(checkInDateField);
            String checkOutRaw = dateFieldText(checkOutDateField);
            if (checkInRaw.isEmpty() || checkOutRaw.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter check-in and check-out dates.", "Missing Dates", JOptionPane.WARNING_MESSAGE);
                return;
            }
            LocalDate checkIn = parseDate(checkInRaw);
            LocalDate checkOut = parseDate(checkOutRaw);
            DateRange dateRange = new DateRange(checkIn, checkOut);

            // Steps 4-5: validate and compute preview summary
            currentPreview = ResC.reserveRoom(
                    roomNumber,
                    guestName,
                    creditCard,
                    dateRange
            );

            JOptionPane.showMessageDialog(this, currentPreview.toString(), "Reservation Summary", JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalStateException | IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Cannot Reserve", JOptionPane.ERROR_MESSAGE);
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Dates must be in YYYY-MM-DD format.", "Invalid Date", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleConfirm() {
        try {
            if (currentPreview == null) {
                JOptionPane.showMessageDialog(this, "Click 'Calculate Cost' first.", "Missing Summary", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Steps 6-8: save reservation and show confirmation
            String confirmationNumber = ResC.confirmAndSaveReservation(currentPreview, true);
            currentPreview = null;
            JOptionPane.showMessageDialog(this, "Reservation confirmed! Confirmation: " + confirmationNumber, "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalStateException | IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Cannot Confirm", JOptionPane.ERROR_MESSAGE);
        }
    }

    private LocalDate parseDate(String text) {
        return LocalDate.parse(text);
    }

    /** Returns trimmed text, or empty if the field still shows the placeholder. */
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
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (DATE_PLACEHOLDER.equals(field.getText())) {
                    field.setText("");
                    field.setForeground(normalColor);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setText(DATE_PLACEHOLDER);
                    field.setForeground(hintColor);
                }
            }
        });
    }

    public void setBackAction(ActionListener goBack, String backMessage) {
        backButton.addActionListener(goBack);
        backButton.setLabel(backMessage);
        backButton.setVisible(true);
    }
}

