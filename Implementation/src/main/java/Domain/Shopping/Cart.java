/*
 *  Filename: Cart.java
 *  Date Created: 3/24/2026
 *  Date Last Modified: 3/24/2026
 *  Authors: XXX, XXX
 *  File Description:
 *      XXXX
 */

package Domain.Shopping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Cart {
    private final long id;
    private final long guestUserId;
    private final List<CartItem> items;

    public Cart(long id, long guestUserId, List<CartItem> items) {
        if (guestUserId <= 0) throw new IllegalArgumentException("Guest user id is required.");
        this.id = id;
        this.guestUserId = guestUserId;
        this.items = new ArrayList<>(items == null ? List.of() : items);
    }

    public long getId() {
        return id;
    }

    public long getGuestUserId() {
        return guestUserId;
    }

    public List<CartItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
