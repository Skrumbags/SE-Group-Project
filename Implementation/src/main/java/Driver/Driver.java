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
import Rooms.Room;
import Rooms.RoomType;
import Rooms.RoomType.FloorType;
import Rooms.RoomType.BedType;


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
        System.out.print("--------ROOM TESTING--------");
        System.out.println();

        // --- Create RoomTypes ---
        RoomType rt1 = new RoomType(FloorType.NATURAL, BedType.QUEEN);
        RoomType rt2 = new RoomType(FloorType.URBAN, BedType.DOUBLE);
        RoomType rt3 = new RoomType(FloorType.VINTAGE, BedType.SINGLE);

        // --- Create Rooms ---
        Room r1 = new Room(101, false, false, 150.00, rt1);
        Room r2 = new Room(102, true,  false, 120.00, rt2);
        Room r3 = new Room(103, false, false,  90.00, rt3);

        // --- Basic getters ---
        System.out.println("=== basic getters ===");
        System.out.println("r1 number: " + r1.getRoomNumber());                        // 101
        System.out.println("r1 smoking: " + r1.isSmoking());                          // false
        System.out.println("r1 available: " + r1.isAvailability());                   // false
        System.out.println("r1 rate: " + r1.getMaxDailyRate());                       // 150.0
        System.out.println("r1 floor: " + r1.getRoomType().getFloorType());           // NATURAL
        System.out.println("r1 bed: " + r1.getRoomType().getBedType());               // QUEEN

        // --- Test setters ---
        System.out.println("\n=== setters ===");
        r1.setAvailability(true);
        System.out.println("r1 availability set to true: " + r1.isAvailability());    // true
        r1.setSmoking(true);
        System.out.println("r1 smoking set to true: " + r1.isSmoking());              // true

        // --- RoomType setters ---
        System.out.println("\n=== RoomType setters ===");
        rt2.setFloorType(FloorType.VINTAGE);
        System.out.println("rt2 floor updated: " + rt2.getFloorType());               // VINTAGE
        rt2.setBedType(BedType.QUEEN);
        System.out.println("rt2 bed updated: " + rt2.getBedType());                   // QUEEN

        // --- setRoomType ---
        System.out.println("\n=== setRoomType ===");
        r3.setRoomType(new RoomType(FloorType.URBAN, BedType.DOUBLE));
        System.out.println("r3 floor updated: " + r3.getRoomType().getFloorType());   // URBAN
        System.out.println("r3 bed updated: " + r3.getRoomType().getBedType());       // DOUBLE

        // --- Setter validation ---
        System.out.println("\n=== setter validation ===");
        try {
            r1.setRoomNumber(-5);
        } catch (Room.InvalidRoomNumber e) {
            System.out.println("Caught InvalidRoomNumber: " + e.getMessage());
        }

        try {
            r1.setMaxDailyRate(-100);
        } catch (Room.InvalidMaxDailyRate e) {
            System.out.println("Caught InvalidMaxDailyRate: " + e.getMessage());
        }

        try {
            r1.setRoomNumber(201);
            System.out.println("setRoomNumber to 201: success, new number = " + r1.getRoomNumber()); // 201
        } catch (Room.InvalidRoomNumber e) {
            System.out.println("Unexpected exception: " + e.getMessage());
        }

        // --- equals and hashCode ---
        System.out.println("\n=== equals ===");
        Room r1copy = new Room(201, false, true, 200.00, rt3); // same number as updated r1
        System.out.println("r1 equals r1copy (same room number): " + r1.equals(r1copy)); // true
        System.out.println("r1 equals r2 (different number): " + r1.equals(r2));         // false

        // --- RoomType equals ---
        System.out.println("\n=== RoomType equals ===");
        RoomType rtMatch = new RoomType(FloorType.VINTAGE, BedType.QUEEN);
        RoomType rtNoMatch = new RoomType(FloorType.NATURAL, BedType.SINGLE);
        System.out.println("rt1 equals rtMatch (NATURAL/QUEEN vs VINTAGE/QUEEN): " + rt1.equals(rtMatch)); // false
        System.out.println("rt2 equals rtMatch (VINTAGE/QUEEN vs VINTAGE/QUEEN): " + rt2.equals(rtMatch)); // true
        System.out.println("rt1 equals rtNoMatch: " + rt1.equals(rtNoMatch));                              // false

    }
}

/* Hannah's Testing
import org.example.Room;
import org.example.RoomCatalog;

public class Driver {

    public static void main(String[] args) {


    }
} */