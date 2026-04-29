package UI.User;

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
        JButton modifyBtn = new JButton("Modify Selected");
        JButton cancelBtn = new JButton("Cancel Selected");
        buttonPanel.add(modifyBtn);
        buttonPanel.add(cancelBtn);
        rightPanel.add(buttonPanel, BorderLayout.SOUTH);

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton backBtn = new JButton("← Back to Home");
        backBtn.addActionListener(e -> onBack.run());
        topPanel.add(backBtn);

        modifyBtn.addActionListener(e -> handleModify());
        cancelBtn.addActionListener(e -> handleCancel());

        add(topPanel, BorderLayout.NORTH);
        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.CENTER);
    }

    public void refreshList() {
        rowCache.clear();
        rowCache.addAll(reservationController.getMyReservations());
        listModel.clear();
        for (Reservation r : rowCache) {
            listModel.addElement(r.getConfirmationNumber() + " - " + r.getDateRange().getCheckInDate() + " to " + r.getDateRange().getCheckOutDate());
        }
        detailsArea.setText("");
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

    private void handleCancel() {
        int idx = reservationList.getSelectedIndex();
        if (idx < 0) return;
        Reservation r = rowCache.get(idx);

        double fee = r.peekPenaltyFee(LocalDate.now());
        String warningMsg = fee > 0 ?
                String.format("WARNING: Cancelling this reservation will incur a penalty fee of $%.2f. Are you sure?", fee) :
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
        if (idx < 0) return;
        Reservation r = rowCache.get(idx);

        // STEP 1: Select Dates
        JTextField inField = new JTextField("YYYY-MM-DD", 10);
        JTextField outField = new JTextField("YYYY-MM-DD", 10);
        JPanel datePanel = new JPanel(new GridLayout(2, 2, 5, 5));
        datePanel.add(new JLabel("New Check-In:")); datePanel.add(inField);
        datePanel.add(new JLabel("New Check-Out:")); datePanel.add(outField);

        int dateConfirm = JOptionPane.showConfirmDialog(this, datePanel, "Step 1: Choose New Dates", JOptionPane.OK_CANCEL_OPTION);
        if (dateConfirm != JOptionPane.OK_OPTION) return;

        try {
            LocalDate in = LocalDate.parse(inField.getText().trim());
            LocalDate out = LocalDate.parse(outField.getText().trim());
            DateRange newDates = new DateRange(in, out);

            // STEP 2: Fetch Available Rooms
            List<Room> availableRooms = reservationController.getAvailableRoomsForModification(newDates, r.getConfirmationNumber());
            if (availableRooms.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No rooms available for those dates.", "Unavailable", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // STEP 3: Select Room & Show Warnings
            JComboBox<Integer> roomCombo = new JComboBox<>();
            for (Room room : availableRooms) roomCombo.addItem(room.getRoomNumber());

            double fee = r.peekPenaltyFee(LocalDate.now());

            JPanel roomPanel = new JPanel(new GridLayout(4, 1, 5, 5));
            roomPanel.add(new JLabel("Step 2: Choose a Room"));
            roomPanel.add(roomCombo);
            if (fee > 0) {
                JLabel feeLabel = new JLabel(String.format("<html><font color='red'>WARNING: A modification fee of $%.2f will be added to your new total.</font></html>", fee));
                roomPanel.add(feeLabel);
            }

            int finalConfirm = JOptionPane.showConfirmDialog(this, roomPanel, "Confirm Modification", JOptionPane.OK_CANCEL_OPTION);
            if (finalConfirm == JOptionPane.OK_OPTION) {
                int selectedRoom = (Integer) roomCombo.getSelectedItem();
                String result = reservationController.modifyGuestItinerary(r.getConfirmationNumber(), selectedRoom, newDates);
                JOptionPane.showMessageDialog(this, result, "Modification Complete", JOptionPane.INFORMATION_MESSAGE);
                refreshList();
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid input: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

