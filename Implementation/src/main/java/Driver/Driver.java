/*
 *  Filename: Driver.java
 *  Date Created: 3/24/2026
 *  Date Last Modified: 3/24/2026
 *  Authors: XXX, XXX
 *  File Description:
 *      XXXX
 */
package Driver;

// Imports for Room + RoomType
import Domain.People.*;
import Domain.Rooms.RoomCatalog;
import Domain.Services.ReservationService;
import Domain.Services.RoomService;
import Domain.Services.ShoppingService;
import TechnicalServices.Persistence.SqliteReservationPersistence;
import TechnicalServices.Persistence.SqliteStorePersistence;
import UI.MasterUI;
import Controllers.*;

import javax.swing.*;
import java.nio.file.Path;

import Controllers.*;
import Domain.People.*;
import TechnicalServices.Persistence.SchemaInstaller;

import javax.swing.*;
import java.sql.Connection;
import java.sql.DriverManager;

public class Driver {

    public static void main(String[] args) {
        Path db = Path.of("data", "database.db");
        RoomCatalog roomCatalog = new RoomCatalog();
        UserCatalog userCatalog = new UserCatalog();
        RoomService roomService = new RoomService(roomCatalog, db);
        ReservationService resService = new ReservationService(db);
        UserController userController = new UserController(userCatalog, db);
        SqliteStorePersistence storeDb = new SqliteStorePersistence(db);
        SqliteReservationPersistence reservationDb = new SqliteReservationPersistence(db);
        reservationDb.initialize();
        ShoppingService shoppingService = new ShoppingService(storeDb, reservationDb, 0.0825);

        // Session starts anonymous; users sign in only through the UI.
        UserSession userSession = new UserSession();
        userSession.logout();

        if (userCatalog.findByUsername("Matt") == null) {
            userController.addGuest("Matt", "testpw", "Matt Freeman", "911", "matt_freeman2@baylor.edu");
        }
        SearchController searchController = new SearchController(roomService, resService);
        ReservationController reservationController =
                new ReservationController(roomService, resService, userSession, userController);
        ShoppingController shoppingController =
                new ShoppingController(shoppingService, resService, userSession);

        seedStoreProductsIfEmpty(storeDb);

        MasterUI ui = new MasterUI(userSession, reservationController, searchController, userController, shoppingController);
        SwingUtilities.invokeLater(() -> {
            userSession.logout();
            ui.buildAndShowUI();
        });

        /*RoomCatalog rooms = new RoomCatalog();
        RoomService service = new RoomService(rooms);
        AddRoomUI ui = new AddRoomUI(service);

        JFrame frame = new JFrame("Test Add Room");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(ui);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);*/

    }

    private static void seedStoreProductsIfEmpty(SqliteStorePersistence storeDb) {
        try {
            storeDb.initialize();
            if (storeDb.countProducts() > 0) {
                return;
            }
            storeDb.createProduct("TSHIRT-001", "Hotel T-Shirt", "Soft cotton tee with logo", 19.99, 25, true);
            storeDb.createProduct("MUG-001", "Coffee Mug", "Ceramic mug", 9.99, 40, true);
            storeDb.createProduct("SOAP-001", "Artisan Soap", "Local handmade soap bar", 6.50, 60, true);
            storeDb.createProduct("HAT-001", "Baseball Cap", "Adjustable cap with logo", 14.00, 30, true);
        } catch (Exception e) {
            throw new RuntimeException("Failed to seed store products", e);
        }
    }
}