package UI.Clerk;

import Controllers.ReservationController;
import Domain.Reservations.Reservation;
import Domain.Rooms.Room;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

public class ClerkMonitorRoomStatus extends JPanel {

    private final ReservationController reservationController;
    private final Runnable onBack;

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"Room #", "Type", "Rate", "Status", "Guest", "Check-in", "Check-out"}, 0
    ) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };

    private final JTable table = new JTable(tableModel);

    public ClerkMonitorRoomStatus(ReservationController reservationController, Runnable onBack) {
        this.reservationController = reservationController;
        this.onBack = onBack;

        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(new JLabel("Room Status Monitor", SwingConstants.CENTER), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JButton backBtn = new JButton("← Back");
        backBtn.addActionListener(e -> onBack.run());
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refresh());

        JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT));
        south.add(backBtn);
        south.add(refreshBtn);
        add(south, BorderLayout.SOUTH);

        refresh();
    }

    public void refresh() {
        tableModel.setRowCount(0);
        List<Room> rooms = reservationController.getRooms();
        List<Reservation> reservations = reservationController.listReservations();
        LocalDate today = LocalDate.now();

        for (Room room : rooms) {
            // find active reservation for this room today
            Reservation active = reservations.stream()
                    .filter(r -> r.getRoom().getRoomNumber() == room.getRoomNumber())
                    .filter(r -> !today.isBefore(r.getDateRange().getCheckInDate())
                            && today.isBefore(r.getDateRange().getCheckOutDate()))
                    .findFirst()
                    .orElse(null);

            String status = active != null ? (active.isActive() ? "CHECKED IN" : "RESERVED") : "AVAILABLE";
            String guest   = active != null ? active.getGuestName() : "-";
            String checkIn  = active != null ? active.getDateRange().getCheckInDate().toString() : "-";
            String checkOut = active != null ? active.getDateRange().getCheckOutDate().toString() : "-";

            tableModel.addRow(new Object[]{
                    room.getRoomNumber(),
                    room.getRoomType(),
                    String.format("$%.2f", room.getMaxDailyRate()),
                    status,
                    guest,
                    checkIn,
                    checkOut
            });
        }
    }
}