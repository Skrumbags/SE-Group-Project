/*
 *  Filename: Room.java
 *  Date Created: 3/24/2026
 *  Date Last Modified: 3/24/2026
 *  Authors: Hannah Ross, XXX
 *  File Description:
 *      XXXX
 */

package Rooms;

public class Room {
    private int roomNumber;
    private boolean smoking; //T = smoking, F = nonsmoking
    private boolean availability = false; // set to true when added
    private double maxDailyRate = 0; // decide value later?
    private int roomCapacity; // maybe?
    private RoomType roomType;

    public Room(int roomNumber, boolean smoking, boolean availability, double maxDailyRate, RoomType roomType){
        this.roomNumber = roomNumber;
        this.smoking = smoking;
        this.availability = availability;
        this.maxDailyRate = maxDailyRate;
        this.roomType = roomType;
    }

    public int getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(int roomNumber) throws InvalidRoomNumber {
        if (roomNumber < 1) {
            throw new InvalidRoomNumber("Room Number must be greater than 0.");
        }
        this.roomNumber = roomNumber;
    }

    public boolean isSmoking() {
        return smoking;
    }

    public void setSmoking(boolean smoking) {
        this.smoking = smoking;
    }

    public boolean isAvailability() {
        return availability;
    }

    public void setAvailability(boolean availability) {
        this.availability = availability;
    }

    public double getMaxDailyRate() {
        return maxDailyRate;
    }

    public void setRoomType(RoomType roomType) { this.roomType = roomType; }

    public RoomType getRoomType() { return roomType; }

    public void setMaxDailyRate(double maxDailyRate) throws InvalidMaxDailyRate {
        if  (maxDailyRate < 1) {
            throw new InvalidMaxDailyRate("Daily rate must be 0 or higher.");
        }
        this.maxDailyRate = maxDailyRate;
        // FIXME: more implementation regarding sales, promotions, late fees, etc?
    }

    // CUSTOM EXCEPTIONS
    public class InvalidRoomNumber extends Exception {
        public InvalidRoomNumber(String message) {
            super(message);
        }
    }

    public class InvalidMaxDailyRate extends Exception {
        public InvalidMaxDailyRate(String message) {
            super(message);
        }
    }

}
