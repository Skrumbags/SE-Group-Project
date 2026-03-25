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
import RoomCatalog.RoomCatalog;
import UI.AddRoomUI;

import javax.swing.*;


public class Driver {
    public static void main(String[] args) {
        /** Idea: (maybe use command line arguments to specify which action to perform)
         *
         *  Load test rooms, users, etc. from CSV file into the program (until someone figures out how to make a database)
         *
         *  Run Use Cases, probably several hard coded (Use Case classes should just include functions with code for doing these to keep main short)
         *
         *  Display UI's
         */

        // Room UI
        RoomCatalog catalog = new RoomCatalog();

        JFrame frame = new JFrame("Hotel - Add Room");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new AddRoomUI(catalog));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

    }

}