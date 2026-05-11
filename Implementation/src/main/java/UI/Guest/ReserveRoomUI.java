package UI.Guest;

import Domain.People.UserSession;
import Domain.Reservations.ReservationSummary;
import Domain.Shared.DateRange;
import Controllers.ReservationController;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class ReserveRoomUI extends JPanel {

    private final ReservationController ResC;
    private final UserSession userSession;

    private final JLabel roomNumberLabel = new JLabel("");
    private final JLabel checkInDateLabel = new JLabel("");
    private final JLabel checkOutDateLabel = new JLabel("");

    private final JTextField guestNameField = new JTextField(15);
    private final JTextField creditCardField = new JTextField(16);
    private final JButton backButton = new JButton();

    private ReservationSummary currentPreview;

    public ReserveRoomUI(UserSession userSession, ReservationController ResC) {
        this.userSession = userSession;
        this.ResC = ResC;

        setLayout(new GridLayout(8, 2, 5, 5));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 1. Room Number
        add(new JLabel("Room Number:"));
        add(roomNumberLabel);

        // 2. Check-in Date
        add(new JLabel("Check-in Date:"));
        add(checkInDateLabel);

        // 3. Check-out Date
        add(new JLabel("Check-out Date:"));
        add(checkOutDateLabel);

        // 4. Guest Name (Mutable)
        add(new JLabel("Guest Name:"));
        add(guestNameField);

        // 5. Credit Card (Mutable)
        add(new JLabel("Credit Card #:"));
        add(creditCardField);

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

    public void applyPreselection(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        currentPreview = null;
        roomNumberLabel.setText(String.valueOf(roomNumber));
        checkInDateLabel.setText(checkIn.toString());
        checkOutDateLabel.setText(checkOut.toString());

        guestNameField.setText("");
        creditCardField.setText("");
    }

    private void handlePreview() {
        try {
            if (roomNumberLabel.getText().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please select a room from the search page first.", "Missing Room", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int roomNumber = Integer.parseInt(roomNumberLabel.getText().trim());
            String guestName = guestNameField.getText();
            String creditCard = creditCardField.getText();

            String checkInRaw = checkInDateLabel.getText().trim();
            String checkOutRaw = checkOutDateLabel.getText().trim();

            if (checkInRaw.isEmpty() || checkOutRaw.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please select dates from the search page first.", "Missing Dates", JOptionPane.WARNING_MESSAGE);
                return;
            }

            LocalDate checkIn = LocalDate.parse(checkInRaw);
            LocalDate checkOut = LocalDate.parse(checkOutRaw);
            DateRange dateRange = new DateRange(checkIn, checkOut);

            currentPreview = ResC.reserveRoom(roomNumber, guestName, creditCard, dateRange);

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

            String confirmationNumber = ResC.confirmAndSaveReservation(currentPreview, true);
            currentPreview = null;
            JOptionPane.showMessageDialog(this, "Reservation confirmed! Confirmation: " + confirmationNumber, "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalStateException | IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Cannot Confirm", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void setBackAction(ActionListener goBack, String backMessage) {
        backButton.addActionListener(goBack);
        backButton.setText(backMessage);
        backButton.setVisible(true);
    }

    public void refresh() {
        currentPreview = null;
        roomNumberLabel.setText("");
        checkInDateLabel.setText("");
        checkOutDateLabel.setText("");

        guestNameField.setText("");
        creditCardField.setText("");
    }
}

