/*
 *  Filename: RoomType.java
 *  Date Created: 3/24/2026
 *  Date Last Modified: 3/24/2026
 *  Authors: Hannah Ross, Matt Freeman
 *  File Description:
 *      XXXX
 */

/** Hannah, I know you made this as a part of the room class, but it's needed separate for searching I think **/

package Domain.Rooms;

import java.util.Objects;

public class RoomType {
    private FloorType floorType;
    private BedType bedType;

    // enums - too much variability with inputs
    public static enum FloorType { NATURAL, URBAN, VINTAGE }
    public static enum BedType { SINGLE, DOUBLE, QUEEN }

    public RoomType(FloorType floorType, BedType bedType) {
        this.floorType = floorType;
        this.bedType = bedType;
    }

    public FloorType getFloorType() {
        return floorType;
    }

    public void setFloorType(FloorType floorType) {
        this.floorType = floorType;
    }

    public BedType getBedType() {
        return bedType;
    }

    public void setBedType(BedType bedType) {
        this.bedType = bedType;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RoomType)) return false;
        RoomType rt = (RoomType) o;
        return this.floorType == rt.floorType && this.bedType == rt.bedType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(floorType, bedType);
    }
}
