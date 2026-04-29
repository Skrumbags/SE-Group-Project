/*
 *  Filename: ReservationRoomTesting.java
 *  Date Created: 3/25/2026
 *  Date Last Modified: 3/25/2026
 *  Authors: XXX, XXX
 *  File Description:
 *      Integration test for Reserve Room against a dedicated SQLite file (not the dev database).
 */
package Testing.UseCaseTests;

import Domain.Services.UserService;
import TechnicalServices.Persistence.SqliteReservationPersistence;
import Domain.People.Guest;
import Domain.People.UserSession;
import Domain.Reservations.Reservation;
import Domain.Reservations.ReservationSummary;
import Domain.Rooms.Room;
import Domain.Rooms.RoomType;
import Testing.UseCases.ReserveRoom;
import UI.Guest.ReserveRoomUI;
import Domain.Shared.DateRange;
import Controllers.ReservationController;
import Controllers.UserController;
import Domain.Services.ReservationService;
import Domain.Services.RoomService;

import javax.swing.JFrame;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;

public class ReservationRoomTesting {

    /** Isolated DB so the test does not depend on leftover rows in {@code database.db}. */
    private static final Path TEST_DB = Path.of("data", "reservation_test.db");

    public static void main(String[] args) throws Exception {
        testReserveRoomHappyPath();
        launchReserveRoomUI();
    }

    private static void testReserveRoomHappyPath() throws IOException, SQLException {
        Files.createDirectories(TEST_DB.getParent());
        Files.deleteIfExists(TEST_DB);

        UserSession userSession = new UserSession();
        userSession.login(new Guest("guest1", "pass", "John Doe", "555-0100", "john@example.com"));

        RoomService roomService = new RoomService(TEST_DB);
        Room room = new Room(
                101,
                false,
                true,
                120.00,
                new RoomType(RoomType.FloorType.NATURAL, RoomType.BedType.SINGLE)
        );
        assertTrue(roomService.addRoom(room), "Test room should be inserted into DB-backed catalog.");

        ReservationService reservationService = new ReservationService(TEST_DB);
        ReserveRoom reserveRoom = new ReserveRoom(userSession, roomService, reservationService);
        DateRange range = new DateRange(3, 25, 2026, 3, 28, 2026);

        ReservationSummary preview = reserveRoom.buildPreview(
                101,
                "John Doe",
                "4111111111111111",
                range
        );

        assertTrue(preview.getRoom().equals(room), "Preview should use the selected room.");
        assertTrue(preview.getNumberOfNights() == 3, "Preview should calculate number of nights.");
        assertTrue(preview.getTotalCost() == 360.00, "Preview total cost should be 3 * 120.00.");
        assertTrue("4111111111111111".equals(preview.getCardNumber()), "Preview should store full card number.");

        String confirmationNumber = reserveRoom.confirmAndSave(preview, true);
        assertTrue(confirmationNumber.startsWith("CONF-"), "Confirmation number should be generated.");

        SqliteReservationPersistence verifyDb = new SqliteReservationPersistence(TEST_DB);
        Reservation fromDb = verifyDb.findByConfirmationNumber(confirmationNumber)
                .orElseThrow(() -> new AssertionError("Reservation row should exist in SQLite."));

        assertTrue(fromDb.getRoom().equals(room), "DB row should match room number.");
        assertTrue(fromDb.getDateRange().overlaps(range), "DB row should store overlapping date range.");
        assertTrue("John Doe".equals(fromDb.getGuestName()), "DB row should store guest name.");
        assertTrue(Math.abs(fromDb.getTotalCost() - 360.00) < 0.001, "DB row should store total cost.");

        System.out.println("ReserveRoom happy-path test passed (verified in " + TEST_DB + ").");
    }

    private static void launchReserveRoomUI() {
        UserSession userSession = new UserSession();
        userSession.login(new Guest("guest1", "pass", "John Doe", "555-0100", "john@example.com"));

        Path db = Path.of("data", "database.db");
        RoomService roomService = new RoomService(db);
        if (roomService.getRooms().isEmpty()) {
            roomService.addRoom(new Room(
                    101,
                    false,
                    true,
                    120.00,
                    new RoomType(RoomType.FloorType.NATURAL, RoomType.BedType.SINGLE)
            ));
            roomService.addRoom(new Room(
                    102,
                    false,
                    true,
                    150.00,
                    new RoomType(RoomType.FloorType.URBAN, RoomType.BedType.DOUBLE)
            ));
        }

        ReservationService reservationService = new ReservationService(db);
        UserController userController = new UserController(new UserService());
        ReservationController reservationController =
                new ReservationController(roomService, reservationService, userSession, userController);

        ReserveRoomUI ui = new ReserveRoomUI(userSession, reservationController);

        JFrame frame = new JFrame("Hotel - Reserve Room");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(ui);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
