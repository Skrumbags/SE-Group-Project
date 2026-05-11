package UI.Clerk;

import Controllers.ReservationController;
import Domain.Reservations.Reservation;
import Domain.Rooms.Room;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

public class ClerkMonitorRoomStatus extends JPanel {

    private final ReservationController reservationController;
    private final Runnable onBack;

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{
                    "Room #", "Floor Type", "Bed Type", "Smoking", "Rate",
                    "Status", "Available Until", "Guest", "Check-in", "Check-out"
            }, 0
    ) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };

    private final JTable table = new JTable(tableModel);

    public ClerkMonitorRoomStatus(ReservationController reservationController, Runnable onBack) {
        this.reservationController = reservationController;
        this.onBack = onBack;

        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        TableColumnModel cm = table.getColumnModel();
        cm.getColumn(0).setPreferredWidth(60);  // Room #
        cm.getColumn(1).setPreferredWidth(100); // Floor Type
        cm.getColumn(2).setPreferredWidth(80);  // Bed Type
        cm.getColumn(3).setPreferredWidth(60);  // Smoking
        cm.getColumn(4).setPreferredWidth(70);  // Rate
        cm.getColumn(5).setPreferredWidth(100); // Status
        cm.getColumn(6).setPreferredWidth(100); // Available Until
        cm.getColumn(7).setPreferredWidth(130); // Guest Name
        cm.getColumn(8).setPreferredWidth(90);  // Check-in
        cm.getColumn(9).setPreferredWidth(90);  // Check-out

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
            // If there's an outgoing guest and an incoming guest today, max() prioritizes the checked-in guest.
            Reservation active = reservations.stream()
                    .filter(r -> r.getRoom().getRoomNumber() == room.getRoomNumber())
                    .filter(r -> r.isActive() || (!today.isBefore(r.getDateRange().getCheckInDate())
                            && today.isBefore(r.getDateRange().getCheckOutDate())))
                    .max((r1, r2) -> Boolean.compare(r1.isActive(), r2.isActive()))
                    .orElse(null);

            String status;
            if (active != null) {
                status = active.isActive() ? "OCCUPIED" : "RESERVED";
            } else {
                status = room.isAvailability() ? "AVAILABLE" : "UNAVAILABLE";
            }

            String availableUntil = "-";
            if ("AVAILABLE".equals(status)) {
                LocalDate nextReservation = reservations.stream()
                        .filter(r -> r.getRoom().getRoomNumber() == room.getRoomNumber())
                        .map(r -> r.getDateRange().getCheckInDate())
                        .filter(checkIn -> checkIn.isAfter(today))
                        .min(LocalDate::compareTo)
                        .orElse(null);

                if (nextReservation != null) {
                    availableUntil = nextReservation.toString();
                }
            }

            String guest   = active != null ? active.getGuestName() : "-";
            String checkIn  = active != null ? active.getDateRange().getCheckInDate().toString() : "-";
            String checkOut = active != null ? active.getDateRange().getCheckOutDate().toString() : "-";

            String floorStr = room.getRoomType() != null && room.getRoomType().getFloorType() != null
                    ? room.getRoomType().getFloorType().name() : "-";
            String bedStr = room.getRoomType() != null && room.getRoomType().getBedType() != null
                    ? room.getRoomType().getBedType().name() : "-";
            String smokingStr = room.isSmoking() ? "Y" : "N";

            tableModel.addRow(new Object[]{
                    room.getRoomNumber(),
                    floorStr,
                    bedStr,
                    smokingStr,
                    String.format("$%.2f", room.getMaxDailyRate()),
                    status,
                    availableUntil,
                    guest,
                    checkIn,
                    checkOut
            });
        }
    }
}