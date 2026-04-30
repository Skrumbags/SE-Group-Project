package UI.Guest;

import Controllers.ReservationController;
import Domain.People.UserSession;
import Domain.Reservations.Reservation;
import Domain.Rooms.Room;
import Domain.Shared.DateRange;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class GuestReservationsUI extends JPanel {

    private final UserSession userSession;
    private final ReservationController reservationController;

    private final DefaultListModel<String> listModel = new DefaultListModel<>();
    private final JList<String> reservationList = new JList<>(listModel);
    private final List<Reservation> rowCache = new ArrayList<>();

    private final JTextArea detailsArea = new JTextArea(10, 30);

    public GuestReservationsUI(UserSession userSession, ReservationController reservationController, Runnable onBack) {
        this.userSession = userSession;
        this.reservationController = reservationController;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        detailsArea.setEditable(false);
        detailsArea.setFont(new Font("Monospaced", Font.PLAIN, 14));

        reservationList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        reservationList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) fillFromSelection();
        });

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(new JLabel("My Reservations:"), BorderLayout.NORTH);
        leftPanel.add(new JScrollPane(reservationList), BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));
        rightPanel.add(new JLabel("Reservation Details:"), BorderLayout.NORTH);
        rightPanel.add(new JScrollPane(detailsArea), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton updateDetailsBtn = new JButton("Update Details");
        JButton modifyBtn = new JButton("Modify Itinerary");
        JButton cancelBtn = new JButton("Cancel Selected");

        buttonPanel.add(updateDetailsBtn);
        buttonPanel.add(modifyBtn);
        buttonPanel.add(cancelBtn);
        rightPanel.add(buttonPanel, BorderLayout.SOUTH);

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton backBtn = new JButton("← Back to Home");
        backBtn.addActionListener(e -> onBack.run());
        topPanel.add(backBtn);

        updateDetailsBtn.addActionListener(e -> handleUpdateDetails());
        modifyBtn.addActionListener(e -> handleModify());
        cancelBtn.addActionListener(e -> handleCancel());

        add(topPanel, BorderLayout.NORTH);
        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.CENTER);
    }

    public void refreshList() {
        reservationList.clearSelection();
        rowCache.clear();
        rowCache.addAll(reservationController.getMyReservations());
        listModel.clear();
        for (Reservation r : rowCache) {
            if (r.isCheckedOutOrExpired()) {
                continue;
            }
            listModel.addElement(r.getConfirmationNumber() + " - " + r.getDateRange().getCheckInDate() + " to " + r.getDateRange().getCheckOutDate());
        }
        detailsArea.setText("");
    }

    /**
     * Refreshes the list and selects the reservation with this confirmation (e.g. from guest home preview).
     */
    public void selectReservationByConfirmation(String confirmationNumber) {
        if (confirmationNumber == null || confirmationNumber.isBlank()) {
            return;
        }
        refreshList();
        for (int i = 0; i < rowCache.size(); i++) {
            if (confirmationNumber.equals(rowCache.get(i).getConfirmationNumber())) {
                reservationList.setSelectedIndex(i);
                reservationList.ensureIndexIsVisible(i);
                return;
            }
        }
    }

    private void fillFromSelection() {
        int idx = reservationList.getSelectedIndex();
        if (idx < 0) return;
        Reservation r = rowCache.get(idx);

        String details = "Confirmation #: " + r.getConfirmationNumber() + "\n" +
                "Room: " + r.getRoom().getRoomNumber() + "\n" +
                "Check-In: " + r.getDateRange().getCheckInDate() + "\n" +
                "Check-Out: " + r.getDateRange().getCheckOutDate() + "\n" +
                "Total Cost: $" + String.format("%.2f", r.getTotalCost());
        detailsArea.setText(details);
    }

    private void handleUpdateDetails() {
        int idx = reservationList.getSelectedIndex();
        if (idx < 0) {
            JOptionPane.showMessageDialog(this, "Please select a reservation first.", "None Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Reservation r = rowCache.get(idx);

        JTextField nameField = new JTextField(r.getGuestName(), 15);
        JTextField ccField = new JTextField(r.getCardNumber(), 15);

        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        panel.add(new JLabel("Guest Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Credit Card #:"));
        panel.add(ccField);

        int confirm = JOptionPane.showConfirmDialog(this, panel, "Update Personal Details", JOptionPane.OK_CANCEL_OPTION);
        if (confirm == JOptionPane.OK_OPTION) {
            try {
                reservationController.modifyGuestPersonalDetails(
                        r.getConfirmationNumber(),
                        nameField.getText().trim(),
                        ccField.getText().trim()
                );
                JOptionPane.showMessageDialog(this, "Details updated successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                refreshList();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Update Failed", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleCancel() {
        int idx = reservationList.getSelectedIndex();
        if (idx < 0) {
            JOptionPane.showMessageDialog(this, "Please select a reservation first.", "None Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Reservation r = rowCache.get(idx);

        // UI blocking constraint for active/past reservations
        if (!LocalDate.now().isBefore(r.getDateRange().getCheckInDate())) {
            JOptionPane.showMessageDialog(this, "Reservations cannot be cancelled on or after the check-in date.", "Cancellation Prohibited", JOptionPane.ERROR_MESSAGE);
            return;
        }

        double fee = r.peekPenaltyFee(LocalDate.now());

        // Front-load the fee warning
        String warningMsg = fee > 0 ?
                String.format("WARNING: Cancelling this reservation is past the free cancellation window. A penalty fee of $%.2f will be charged to your card.\n\nAre you sure you want to cancel?", fee) :
                "Are you sure you want to cancel this reservation?";

        int confirm = JOptionPane.showConfirmDialog(this, warningMsg, "Confirm Cancellation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                String result = reservationController.cancelReservation(r.getConfirmationNumber());
                JOptionPane.showMessageDialog(this, result, "Cancelled", JOptionPane.INFORMATION_MESSAGE);
                refreshList();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Failed", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleModify() {
        int idx = reservationList.getSelectedIndex();
        if (idx < 0) {
            JOptionPane.showMessageDialog(this, "Please select a reservation first.", "None Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Reservation r = rowCache.get(idx);

        // UI blocking constraint for active/past reservations
        if (!LocalDate.now().isBefore(r.getDateRange().getCheckInDate())) {
            JOptionPane.showMessageDialog(this, "Reservations cannot be modified on or after the check-in date.", "Modification Prohibited", JOptionPane.ERROR_MESSAGE);
            return;
        }

        double fee = r.peekPenaltyFee(LocalDate.now());
        if (fee > 0) {
            int proceed = JOptionPane.showConfirmDialog(this,
                    String.format("Modifying this itinerary is past the free change window. A modification fee of $%.2f will be added to your new total.\n\nDo you wish to proceed?", fee),
                    "Modification Fee Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

            if (proceed != JOptionPane.YES_OPTION) {
                return; // User cancelled out of the warning
            }
        }

        // STEP 1: Select Dates and Preferences
        String currentIn = r.getDateRange().getCheckInDate().toString();
        String currentOut = r.getDateRange().getCheckOutDate().toString();

        JTextField inField = new JTextField(currentIn, 10);
        JTextField outField = new JTextField(currentOut, 10);

        JComboBox<Domain.Rooms.RoomType.FloorType> floorCombo = new JComboBox<>(Domain.Rooms.RoomType.FloorType.values());
        JComboBox<Domain.Rooms.RoomType.BedType> bedCombo = new JComboBox<>(Domain.Rooms.RoomType.BedType.values());
        JComboBox<String> smokingCombo = new JComboBox<>(new String[]{"Any", "Smoking", "Non-smoking"});

        Room fullRoom = reservationController.getRooms().stream()
                .filter(room -> room.getRoomNumber() == r.getRoom().getRoomNumber())
                .findFirst()
                .orElse(null);

        // Set the dropdown defaults
        if (fullRoom != null) {
            if (fullRoom.getRoomType() != null) {
                floorCombo.setSelectedItem(fullRoom.getRoomType().getFloorType());
                bedCombo.setSelectedItem(fullRoom.getRoomType().getBedType());
            }
            smokingCombo.setSelectedItem(fullRoom.isSmoking() ? "Smoking" : "Non-smoking");
        }

        JPanel datePanel = new JPanel(new GridLayout(5, 2, 5, 5));
        datePanel.add(new JLabel("New Check-In (YYYY-MM-DD):")); datePanel.add(inField);
        datePanel.add(new JLabel("New Check-Out (YYYY-MM-DD):")); datePanel.add(outField);
        datePanel.add(new JLabel("Theme / Floor:")); datePanel.add(floorCombo);
        datePanel.add(new JLabel("Bed Type:")); datePanel.add(bedCombo);
        datePanel.add(new JLabel("Smoking:")); datePanel.add(smokingCombo);

        int dateConfirm = JOptionPane.showConfirmDialog(this, datePanel, "Step 1: Choose Dates & Room Type", JOptionPane.OK_CANCEL_OPTION);
        if (dateConfirm != JOptionPane.OK_OPTION) return;

        try {
            LocalDate in = LocalDate.parse(inField.getText().trim());
            LocalDate out = LocalDate.parse(outField.getText().trim());

            // UI blocking constraint for past dates
            if (in.isBefore(LocalDate.now())) {
                throw new IllegalArgumentException("Check-in date cannot be in the past.");
            }

            DateRange newDates = new DateRange(in, out);

            Domain.Rooms.RoomType.FloorType prefFloor = (Domain.Rooms.RoomType.FloorType) floorCombo.getSelectedItem();
            Domain.Rooms.RoomType.BedType prefBed = (Domain.Rooms.RoomType.BedType) bedCombo.getSelectedItem();

            Boolean isSmoking = null; // Default to "Any"
            String smokingSelection = (String) smokingCombo.getSelectedItem();
            if ("Smoking".equals(smokingSelection)) {
                isSmoking = true;
            } else if ("Non-smoking".equals(smokingSelection)) {
                isSmoking = false;
            }

            // STEP 2: Fetch Available Rooms and filter by selected type
            List<Room> allAvailable = reservationController.getAvailableRoomsForModification(newDates, r.getConfirmationNumber());
            List<Room> filteredRooms = new ArrayList<>();
            for (Room room : allAvailable) {
                boolean smokingMatch = (isSmoking == null || room.isSmoking() == isSmoking);

                if (room.getRoomType().getFloorType() == prefFloor && room.getRoomType().getBedType() == prefBed && smokingMatch) {
                    filteredRooms.add(room);
                }
            }

            if (filteredRooms.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No rooms available matching those dates and preferences.", "Unavailable", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // STEP 3: Select Room
            JComboBox<String> roomCombo = new JComboBox<>();
            for (Room room : filteredRooms) {
                String smokeStr = room.isSmoking() ? "Smoking" : "Non-smoking";
                roomCombo.addItem("Room " + room.getRoomNumber() + " [" + smokeStr + "] ($" + String.format("%.2f", room.getMaxDailyRate()) + "/night)");
            }

            JPanel roomPanel = new JPanel(new GridLayout(2, 1, 5, 5));
            roomPanel.add(new JLabel("Step 2: Choose a Room"));
            roomPanel.add(roomCombo);

            int finalConfirm = JOptionPane.showConfirmDialog(this, roomPanel, "Confirm Modification", JOptionPane.OK_CANCEL_OPTION);
            if (finalConfirm == JOptionPane.OK_OPTION) {
                int selectedIdx = roomCombo.getSelectedIndex();
                Room selectedRoom = filteredRooms.get(selectedIdx);

                String result = reservationController.modifyGuestItinerary(r.getConfirmationNumber(), selectedRoom.getRoomNumber(), newDates);
                JOptionPane.showMessageDialog(this, result, "Modification Complete", JOptionPane.INFORMATION_MESSAGE);
                refreshList();
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid input: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}