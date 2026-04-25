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
    private final double stayTotal;
    private final double shoppingTotal;
    private final double combinedTotal;

    public CombinedBill(long guestUserId,
                        List<Reservation> reservations,
                        List<Purchase> purchases,
                        double stayTotal,
                        double shoppingTotal,
                        double combinedTotal) {
        this.guestUserId = guestUserId;
        this.reservations = new ArrayList<>(reservations == null ? List.of() : reservations);
        this.purchases = new ArrayList<>(purchases == null ? List.of() : purchases);
        this.stayTotal = stayTotal;
        this.shoppingTotal = shoppingTotal;
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

    public double getStayTotal() {
        return stayTotal;
    }

    public double getShoppingTotal() {
        return shoppingTotal;
    }

    public double getCombinedTotal() {
        return combinedTotal;
    }
}
