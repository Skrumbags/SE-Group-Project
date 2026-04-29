/*
 *  Filename: CombinedBill.java
 *  Date Created: 3/24/2026
 *  Date Last Modified: 3/24/2026
 *  Authors: XXX, XXX
 *  File Description:
 *      XXXX
 */

package Domain.Shared;

import Domain.Reservations.Reservation;
import Domain.Shopping.Purchase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CombinedBill {
    private final long guestUserId;
    private final List<Reservation> reservations;
    private final List<Purchase> purchases;
    /** Sum of room charges before tax (from reservation rows). */
    private final double staySubtotal;
    /** Tax applied to {@link #staySubtotal} only. */
    private final double roomTax;
    /** {@link #staySubtotal} + {@link #roomTax}. */
    private final double stayTotal;
    /** Sum of purchase subtotals (store purchase tax is excluded from the combined bill). */
    private final double shoppingSubtotal;
    private final double combinedTotal;

    public CombinedBill(long guestUserId,
                        List<Reservation> reservations,
                        List<Purchase> purchases,
                        double staySubtotal,
                        double roomTax,
                        double stayTotal,
                        double shoppingSubtotal,
                        double combinedTotal) {
        this.guestUserId = guestUserId;
        this.reservations = new ArrayList<>(reservations == null ? List.of() : reservations);
        this.purchases = new ArrayList<>(purchases == null ? List.of() : purchases);
        this.staySubtotal = staySubtotal;
        this.roomTax = roomTax;
        this.stayTotal = stayTotal;
        this.shoppingSubtotal = shoppingSubtotal;
        this.combinedTotal = combinedTotal;
    }

    public long getGuestUserId() {
        return guestUserId;
    }

    public List<Reservation> getReservations() {
        return Collections.unmodifiableList(reservations);
    }

    public List<Purchase> getPurchases() {
        return Collections.unmodifiableList(purchases);
    }

    public double getStaySubtotal() {
        return staySubtotal;
    }

    public double getRoomTax() {
        return roomTax;
    }

    /** Room charges including room tax. */
    public double getStayTotal() {
        return stayTotal;
    }

    public double getShoppingSubtotal() {
        return shoppingSubtotal;
    }

    public double getCombinedTotal() {
        return combinedTotal;
    }
}
