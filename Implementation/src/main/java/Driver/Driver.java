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
import People.Guest;
import People.UserCatalog;
import People.UserSession;
import RoomCatalog.RoomCatalog;
import UI.AddRoomUI;
import UI.AddUserUI;
import UI.MasterUI;
import UI.ReserveRoomUI;
import Utility.*;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;

public class Driver {

    public static void main(String[] args) {
        Path db = Path.of("data", "database.db");
        RoomCatalog roomCatalog = new RoomCatalog();
        UserCatalog userCatalog = new UserCatalog();
        RoomService roomService = new RoomService(roomCatalog, db);
        ReservationService resService = new ReservationService(db);
        UserSession userSession = new UserSession();
        userSession.login(new Guest("Matt", "testpw", "Matt Freeman", "911", "matt_freeman2@baylor.edu"));
        userCatalog.addUser(userSession.getCurrentUser());
        UserController userController = new UserController(userCatalog);
        SearchController searchController = new SearchController(roomService, resService);
        ReservationController reservationController = new ReservationController(roomService, resService, userSession);

        MasterUI ui = new MasterUI(userSession, reservationController, searchController, userController);
        SwingUtilities.invokeLater(ui::buildAndShowUI);
    }
}