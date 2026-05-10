package support;

import Controllers.ReservationController;
import Controllers.ShoppingController;
import Controllers.UserController;
import Domain.People.UserSession;
import Domain.Services.ReservationService;
import Domain.Services.RoomService;
import Domain.Services.ShoppingService;
import Domain.Services.UserService;
import TechnicalServices.Persistence.SqliteReservationPersistence;
import TechnicalServices.Persistence.SqliteStorePersistence;

import java.nio.file.Path;

/**
 * Wires the same services as the Swing {@code Driver} against a single SQLite file (typically under
 * {@link org.junit.jupiter.api.io.TempDir}) so tests never touch {@code data/database.db}.
 */
public final class HotelTestHarness {

    public record Wiring(
            Path databasePath,
            UserService userService,
            RoomService roomService,
            ReservationService reservationService,
            ShoppingService shoppingService,
            UserController userController,
            ReservationController reservationController,
            ShoppingController shoppingController,
            UserSession session
    ) {}

    private HotelTestHarness() {}

    public static Wiring build(Path databaseFile) {
        UserService userService = new UserService(databaseFile);
        RoomService roomService = new RoomService(databaseFile);
        ReservationService reservationService = new ReservationService(databaseFile);

        SqliteStorePersistence storeDb = new SqliteStorePersistence(databaseFile);
        SqliteReservationPersistence reservationDb = new SqliteReservationPersistence(databaseFile);
        reservationDb.initialize();

        ShoppingService shoppingService = new ShoppingService(storeDb, reservationDb, 0.0825);
        UserSession session = new UserSession();
        UserController userController = new UserController(userService);
        ReservationController reservationController =
                new ReservationController(roomService, reservationService, session, userController);
        ShoppingController shoppingController =
                new ShoppingController(shoppingService, reservationService, session);

        return new Wiring(
                databaseFile,
                userService,
                roomService,
                reservationService,
                shoppingService,
                userController,
                reservationController,
                shoppingController,
                session);
    }
}
