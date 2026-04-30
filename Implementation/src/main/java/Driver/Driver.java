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
import Domain.Services.ReservationService;
import Domain.Services.RoomService;
import Domain.Services.ShoppingService;
import Domain.Services.UserService;
import TechnicalServices.Persistence.SqliteReservationPersistence;
import TechnicalServices.Persistence.SqliteStorePersistence;
import UI.Other.MasterUI;
import Controllers.*;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;


public class Driver {

    public static void main(String[] args) throws UnsupportedLookAndFeelException, ClassNotFoundException, InstantiationException, IllegalAccessException {

        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");

            UIManager.put("nimbusBase", new Color(24, 95, 165));
            UIManager.put("nimbusLightBackground", Color.WHITE);
            UIManager.put("Panel.background", new Color(230, 241, 251));

        } catch (Exception e) {}

        Path db = Path.of("data", "database.db");
        RoomService roomService = new RoomService(db);
        ReservationService resService = new ReservationService(db);
        UserService userService = new UserService(db);
        UserController userController = new UserController(userService);
        SqliteStorePersistence storeDb = new SqliteStorePersistence(db);
        SqliteReservationPersistence reservationDb = new SqliteReservationPersistence(db);
        reservationDb.initialize();
        ShoppingService shoppingService = new ShoppingService(storeDb, reservationDb, 0.0825);

        // Session starts anonymous; users sign in only through the UI.
        UserSession userSession = new UserSession();
        userSession.logout();

        if (userController.findByUsername("Matt") == null) {
            userController.addGuest("Matt", "testpw", "Matt Freeman", "911", "matt_freeman2@baylor.edu");
        }
        SearchController searchController = new SearchController(roomService, resService);
        ReservationController reservationController =
                new ReservationController(roomService, resService, userSession, userController);
        ShoppingController shoppingController =
                new ShoppingController(shoppingService, resService, userSession);

        userService.addAdmin(2, "admin", "admin123", "Admin");
        userService.addClerk(3, "clerk", "clerk123", "Clerk");

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

}