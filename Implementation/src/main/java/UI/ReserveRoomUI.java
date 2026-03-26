package UI;

import People.UserSession;
import Reservations.ReservationSummary;
import RoomCatalog.RoomCatalog;
import Rooms.Room;
import UseCases.ReserveRoom;
import Utility.DateRange;
import Utility.ReservationService;

import javax.swing.*;
import java.awt.*;
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

    private final ReserveRoom reserveRoomUseCase;
    private final RoomCatalog roomCatalog;

    private final JComboBox<Integer> roomNumberCombo;
    private final JTextField guestNameField = new JTextField(15);
    private final JTextField guestAddressField = new JTextField(20);
    private final JTextField creditCardField = new JTextField(16);
    private final JTextField checkInDateField = new JTextField("YYYY-MM-DD", 10);
    private final JTextField checkOutDateField = new JTextField("YYYY-MM-DD", 10);

    private ReservationSummary currentPreview;

    public ReserveRoomUI(UserSession userSession, RoomCatalog roomCatalog, ReservationService reservationService) {
        this.roomCatalog = roomCatalog;
        this.reserveRoomUseCase = new ReserveRoom(userSession, roomCatalog, reservationService);

        setLayout(new GridLayout(9, 2, 5, 5));

        // Step 1: select desired room
        roomNumberCombo = new JComboBox<>();
        refreshRoomOptions();

        add(new JLabel("Room Number:"));
        add(roomNumberCombo);

        // Step 2: request guest info + dates
        add(new JLabel("Guest Name:"));
        add(guestNameField);

        add(new JLabel("Address:"));
        add(guestAddressField);

        add(new JLabel("Credit Card #:"));
        add(creditCardField);

        add(new JLabel("Check-in (YYYY-MM-DD):"));
        add(checkInDateField);

        add(new JLabel("Check-out (YYYY-MM-DD):"));
        add(checkOutDateField);

        // Step 5-6: show summary then confirm
        JButton previewButton = new JButton("Calculate Cost");
        JButton confirmButton = new JButton("Confirm Reservation");
        add(previewButton);
        add(confirmButton);

        previewButton.addActionListener(e -> handlePreview());
        confirmButton.addActionListener(e -> handleConfirm());
    }

    private void refreshRoomOptions() {
        roomNumberCombo.removeAllItems();
        for (Room r : roomCatalog.getRooms()) {
            roomNumberCombo.addItem(r.getRoomNumber());
        }
    }

    private void handlePreview() {
        try {
            int roomNumber = (Integer) roomNumberCombo.getSelectedItem();
            String guestName = guestNameField.getText();
            String guestAddress = guestAddressField.getText();
            String creditCard = creditCardField.getText();

            LocalDate checkIn = parseDate(checkInDateField.getText().trim());
            LocalDate checkOut = parseDate(checkOutDateField.getText().trim());
            DateRange dateRange = new DateRange(checkIn, checkOut);

            // Steps 4-5: validate and compute preview summary
            currentPreview = reserveRoomUseCase.buildPreview(
                    roomNumber,
                    guestName,
                    guestAddress,
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
            String confirmationNumber = reserveRoomUseCase.confirmAndSave(currentPreview, true);
            currentPreview = null;
            JOptionPane.showMessageDialog(this, "Reservation confirmed! Confirmation: " + confirmationNumber, "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalStateException | IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Cannot Confirm", JOptionPane.ERROR_MESSAGE);
        }
    }

    private LocalDate parseDate(String text) {
        return LocalDate.parse(text);
    }
}

