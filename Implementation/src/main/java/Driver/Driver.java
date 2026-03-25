/*
 *  Filename: Driver.java
 *  Date Created: 3/24/2026
 *  Date Last Modified: 3/24/2026
 *  Authors: XXX, XXX
 *  File Description:
 *      XXXX
 */

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

        //Hannah's Testing

    }
}

/* Hannah's Testing
import org.example.Room;
import org.example.RoomCatalog;

public class Driver {

    public static void main(String[] args) {

        RoomCatalog catalog = new RoomCatalog();

        // --- Create some rooms ---
        Room r1 = new Room(101, Room.FloorType.NATURAL, Room.BedType.QUEEN, false, false, 150.00);
        Room r2 = new Room(102, Room.FloorType.URBAN, Room.BedType.DOUBLE, true, false, 120.00);
        Room r3 = new Room(103, Room.FloorType.VINTAGE, Room.BedType.SINGLE, false, false, 90.00);

        // --- Test addRoom ---
        System.out.println("=== addRoom ===");
        System.out.println("Add r1 (101): " + catalog.addRoom(r1));         // true
        System.out.println("Add r2 (102): " + catalog.addRoom(r2));         // true
        System.out.println("Add r3 (103): " + catalog.addRoom(r3));         // true
        System.out.println("Add r1 again (duplicate): " + catalog.addRoom(r1)); // false
        System.out.println("Add null: " + catalog.addRoom(null));            // false

        // --- Verify availability set automatically ---
        System.out.println("\n=== availability after addRoom ===");
        System.out.println("r1 available: " + r1.isAvailability()); // true
        System.out.println("r2 available: " + r2.isAvailability()); // true

        // --- Test roomInCatalog ---
        System.out.println("\n=== roomInCatalog ===");
        System.out.println("Room 101 in catalog: " + catalog.roomInCatalog(101)); // true
        System.out.println("Room 999 in catalog: " + catalog.roomInCatalog(999)); // false

        // --- Test findRoom ---
        System.out.println("\n=== findRoom ===");
        Room found = catalog.findRoom(102);
        System.out.println("Find 102: " + (found != null ? "Found - bed type: " + found.getTypeOfBeds() : "Not found")); // Found
        System.out.println("Find 999: " + catalog.findRoom(999)); // null

        // --- Test getRooms count ---
        System.out.println("\n=== getRooms ===");
        System.out.println("Total rooms in catalog: " + catalog.getRooms().size()); // 3

        // --- Test setters with validation ---
        System.out.println("\n=== setter validation ===");
        try {
            r1.setRoomNumber(-5); // should throw
        } catch (Room.InvalidRoomNumber e) {
            System.out.println("Caught InvalidRoomNumber: " + e.getMessage());
        }

        try {
            r1.setMaxDailyRate(-100); // should throw
        } catch (Room.InvalidMaxDailyRate e) {
            System.out.println("Caught InvalidMaxDailyRate: " + e.getMessage());
        }

        try {
            r1.setRoomNumber(201); // valid, should work
            System.out.println("setRoomNumber to 201: success, new number = " + r1.getRoomNumber());
        } catch (Room.InvalidRoomNumber e) {
            System.out.println("Unexpected exception: " + e.getMessage());
        }

        // --- Test enum setters ---
        System.out.println("\n=== enum setters ===");
        r2.setTypeOfFloor("VINTAGE");
        System.out.println("r2 floor type updated: " + r2.getTypeOfFloor()); // VINTAGE
        r2.setTypeOfBeds("QUEEN");
        System.out.println("r2 bed type updated: " + r2.getTypeOfBeds());   // QUEEN

        try {
            r3.setTypeOfFloor("CARPET"); // invalid enum value, should throw IllegalArgumentException
        } catch (IllegalArgumentException e) {
            System.out.println("Caught bad FloorType: " + e.getMessage());
        }
    }
} */