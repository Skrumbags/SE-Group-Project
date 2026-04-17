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

        UserSession userSession = new UserSession();
        if (userCatalog.findByUsername("Matt") == null) {
            userController.addGuest("Matt", "testpw", "Matt Freeman", "911", "matt_freeman2@baylor.edu");
        }
        // Session starts signed out; user signs in via LoginUI.
        SearchController searchController = new SearchController(roomService, resService);
        ReservationController reservationController = new ReservationController(roomService, resService, userSession);

        //ADDITIONS FOR TESTING LOGIN
        userCatalog.addUser(new Admin(1, "admin", "admin123", "Admin User"));
        userCatalog.addUser(new Clerk(2, "clerk", "clerk123", "Clerk User"));
        userCatalog.addUser(new Guest("guest", "guest123", "Guest User", "555-1234", "guest@email.com"));

        MasterUI ui = new MasterUI(userSession, reservationController, searchController, userController);
        SwingUtilities.invokeLater(ui::buildAndShowUI);

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