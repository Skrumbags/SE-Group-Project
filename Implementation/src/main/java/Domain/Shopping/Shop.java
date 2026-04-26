/*
 *  Filename: Shop.java
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

public class Shop {
    private final List<Item> catalog = new ArrayList<>();

    public void setCatalog(List<Item> items) {
        catalog.clear();
        if (items != null) {
            catalog.addAll(items);
        }
    }

    public List<Item> getCatalog() {
        return Collections.unmodifiableList(catalog);
    }
}
