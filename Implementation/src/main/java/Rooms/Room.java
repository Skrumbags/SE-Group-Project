package org.example;

public class Room {
    private int roomNumber;
    private FloorType floorType;
    private BedType bedType;
    private boolean smoking; //T = smoking, F = nonsmoking
    private boolean availability = false; // set to true when added
    private double maxDailyRate = 0; // decide value later?
    private int roomCapacity; // maybe?

    // enums - too much variability with inputs
    enum FloorType { NATURAL, URBAN, VINTAGE }
    enum BedType { SINGLE, DOUBLE, QUEEN }


    public Room(int roomNumber, FloorType floorType, BedType bedType,
                boolean smoking, boolean availability, double maxDailyRate){
        this.roomNumber = roomNumber;
        this.floorType = floorType;
        this.bedType = bedType;
        this.smoking = smoking;
        this.availability = availability;
        this.maxDailyRate = maxDailyRate;
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

    public String getTypeOfFloor() {
        return floorType.toString();
    }

    public void setTypeOfFloor(String typeOfFloor) {
        this.floorType = FloorType.valueOf(typeOfFloor);
    }

    public String getTypeOfBeds() {
        return bedType.toString();
    }

    public void setTypeOfBeds(String typeOfBeds) {
        this.bedType = BedType.valueOf(typeOfBeds);
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
