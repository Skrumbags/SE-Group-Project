package TechnicalServices.Persistence;

import Domain.Shopping.Cart;
import Domain.Shopping.CartItem;
import Domain.Shopping.Item;
import Domain.Shopping.Purchase;
import Domain.Shopping.PurchaseItem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SqliteStorePersistence {

    private final Path dbPath;

    public SqliteStorePersistence(Path dbPath) {
        this.dbPath = dbPath;
    }

    private String jdbcUrl() {
        return "jdbc:sqlite:" + dbPath.toAbsolutePath();
    }

    public void initialize() {
        try {
            if (dbPath.getParent() != null) {
                Files.createDirectories(dbPath.getParent());
            }
            try (Connection conn = DriverManager.getConnection(jdbcUrl())) {
                SchemaInstaller.apply(conn);
            }
        } catch (IOException | SQLException e) {
            throw new RuntimeException("Failed to initialize SQLite at " + dbPath, e);
        }
    }

    // ── Products ──────────────────────────────────────────────────────────────

    public long createProduct(String sku, String name, String description, double unitPrice, int stockQty, boolean active)
            throws SQLException {
        String sql = """
                INSERT INTO Products (sku, name, description, unit_price, stock_qty, active)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DriverManager.getConnection(jdbcUrl());
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, sku);
            ps.setString(2, name);
            if (description != null && !description.isBlank()) {
                ps.setString(3, description);
            } else {
                ps.setNull(3, Types.VARCHAR);
            }
            ps.setDouble(4, unitPrice);
            ps.setInt(5, stockQty);
            ps.setInt(6, active ? 1 : 0);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
        }
        throw new SQLException("INSERT Products did not return generated id");
    }

    public long countProducts() throws SQLException {
        try (Connection conn = DriverManager.getConnection(jdbcUrl());
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM Products")) {
            rs.next();
            return rs.getLong(1);
        }
    }

    public List<Item> listActiveProducts() throws SQLException {
        String sql = """
                SELECT id, sku, name, description, unit_price, stock_qty, active
                FROM Products
                WHERE active = 1
                ORDER BY name
                """;
        List<Item> list = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(jdbcUrl());
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapItemRow(rs));
            }
        }
        return list;
    }

    /** All products (including inactive), for clerk inventory management. */
    public List<Item> listAllProducts() throws SQLException {
        String sql = """
                SELECT id, sku, name, description, unit_price, stock_qty, active
                FROM Products
                ORDER BY name
                """;
        List<Item> list = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(jdbcUrl());
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapItemRow(rs));
            }
        }
        return list;
    }

    public int updateProductUnitPrice(long productId, double unitPrice) throws SQLException {
        if (unitPrice < 0) throw new IllegalArgumentException("Unit price cannot be negative.");
        String sql = "UPDATE Products SET unit_price = ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(jdbcUrl());
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, unitPrice);
            ps.setLong(2, productId);
            return ps.executeUpdate();
        }
    }

    public int updateProductStockQty(long productId, int stockQty) throws SQLException {
        if (stockQty < 0) throw new IllegalArgumentException("Stock cannot be negative.");
        String sql = "UPDATE Products SET stock_qty = ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(jdbcUrl());
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, stockQty);
            ps.setLong(2, productId);
            return ps.executeUpdate();
        }
    }

    public Optional<Item> findProductById(long id) throws SQLException {
        String sql = """
                SELECT id, sku, name, description, unit_price, stock_qty, active
                FROM Products
                WHERE id = ?
                """;
        try (Connection conn = DriverManager.getConnection(jdbcUrl());
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapItemRow(rs));
            }
        }
        return Optional.empty();
    }

    private static Item mapItemRow(ResultSet rs) throws SQLException {
        return new Item(
                rs.getLong("id"),
                rs.getString("sku"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getDouble("unit_price"),
                rs.getInt("stock_qty"),
                rs.getInt("active") != 0
        );
    }

    // ── Cart ──────────────────────────────────────────────────────────────────

    private long getOrCreateCartId(Connection conn, long guestUserId) throws SQLException {
        String find = "SELECT id FROM Carts WHERE guest_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(find)) {
            ps.setLong(1, guestUserId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        }

        String insert = "INSERT INTO Carts (guest_id, created_date) VALUES (?, date('now'))";
        try (PreparedStatement ps = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, guestUserId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
        }
        throw new SQLException("INSERT Carts did not return generated id");
    }

    public Cart getCart(long guestUserId) throws SQLException {
        try (Connection conn = DriverManager.getConnection(jdbcUrl())) {
            long cartId = getOrCreateCartId(conn, guestUserId);
            List<CartItem> items = listCartItems(conn, cartId);
            return new Cart(cartId, guestUserId, items);
        }
    }

    private static List<CartItem> listCartItems(Connection conn, long cartId) throws SQLException {
        String sql = """
                SELECT product_id, quantity
                FROM CartItems
                WHERE cart_id = ?
                ORDER BY product_id
                """;
        List<CartItem> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, cartId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new CartItem(rs.getLong("product_id"), rs.getInt("quantity")));
                }
            }
        }
        return list;
    }

    public void setCartItemQuantity(long guestUserId, long productId, int quantity) throws SQLException {
        if (quantity <= 0) {
            removeCartItem(guestUserId, productId);
            return;
        }
        try (Connection conn = DriverManager.getConnection(jdbcUrl())) {
            long cartId = getOrCreateCartId(conn, guestUserId);
            String sql = """
                    INSERT INTO CartItems (cart_id, product_id, quantity)
                    VALUES (?, ?, ?)
                    ON CONFLICT(cart_id, product_id) DO UPDATE SET quantity = excluded.quantity
                    """;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, cartId);
                ps.setLong(2, productId);
                ps.setInt(3, quantity);
                ps.executeUpdate();
            }
        }
    }

    /**
     * Adds {@code deltaQuantity} to an existing cart line; creates the row if missing.
     * If {@code deltaQuantity <= 0}, this is a no-op.
     */
    public void addCartItemQuantity(long guestUserId, long productId, int deltaQuantity) throws SQLException {
        if (deltaQuantity <= 0) {
            return;
        }
        try (Connection conn = DriverManager.getConnection(jdbcUrl())) {
            long cartId = getOrCreateCartId(conn, guestUserId);
            String sql = """
                    INSERT INTO CartItems (cart_id, product_id, quantity)
                    VALUES (?, ?, ?)
                    ON CONFLICT(cart_id, product_id) DO UPDATE SET quantity = quantity + excluded.quantity
                    """;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, cartId);
                ps.setLong(2, productId);
                ps.setInt(3, deltaQuantity);
                ps.executeUpdate();
            }
        }
    }

    public void removeCartItem(long guestUserId, long productId) throws SQLException {
        try (Connection conn = DriverManager.getConnection(jdbcUrl())) {
            long cartId = getOrCreateCartId(conn, guestUserId);
            String sql = "DELETE FROM CartItems WHERE cart_id = ? AND product_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, cartId);
                ps.setLong(2, productId);
                ps.executeUpdate();
            }
        }
    }

    private static void clearCartItems(Connection conn, long cartId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM CartItems WHERE cart_id = ?")) {
            ps.setLong(1, cartId);
            ps.executeUpdate();
        }
    }

    // ── Purchases ─────────────────────────────────────────────────────────────

    public Purchase purchaseItems(long guestUserId,
                                  String reservationConfirmationOrNull,
                                  double taxRate) throws SQLException {
        if (taxRate < 0) throw new IllegalArgumentException("Tax rate cannot be negative.");

        try (Connection conn = DriverManager.getConnection(jdbcUrl())) {
            conn.setAutoCommit(false);
            try {
                long cartId = getOrCreateCartId(conn, guestUserId);
                List<CartItem> cartItems = listCartItems(conn, cartId);
                if (cartItems.isEmpty()) {
                    throw new IllegalArgumentException("Cart is empty.");
                }

                // Validate stock and compute totals
                double subtotal = 0.0;
                List<PurchaseItem> purchaseItems = new ArrayList<>();
                for (CartItem ci : cartItems) {
                    Item item = lockAndLoadProduct(conn, ci.getItemId());
                    if (!item.isActive()) {
                        throw new IllegalStateException("Item is not available: " + item.getName());
                    }
                    if (item.getStockQty() < ci.getQuantity()) {
                        throw new IllegalStateException("Insufficient stock for " + item.getName()
                                + " (requested " + ci.getQuantity() + ", have " + item.getStockQty() + ")");
                    }
                    subtotal += item.getUnitPrice() * ci.getQuantity();
                    purchaseItems.add(new PurchaseItem(item.getId(), ci.getQuantity(), item.getUnitPrice()));
                }

                double tax = roundMoney(subtotal * taxRate);
                double total = roundMoney(subtotal + tax);
                subtotal = roundMoney(subtotal);

                long purchaseId = insertPurchase(conn, guestUserId, reservationConfirmationOrNull, subtotal, tax, total);
                insertPurchaseItems(conn, purchaseId, purchaseItems);
                decrementStock(conn, purchaseItems);
                clearCartItems(conn, cartId);

                conn.commit();
                return new Purchase(purchaseId, guestUserId, reservationConfirmationOrNull, Instant.now(),
                        subtotal, tax, total, purchaseItems);
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private static double roundMoney(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static Item lockAndLoadProduct(Connection conn, long productId) throws SQLException {
        String sql = """
                SELECT id, sku, name, description, unit_price, stock_qty, active
                FROM Products
                WHERE id = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("Unknown product id: " + productId);
                }
                return mapItemRow(rs);
            }
        }
    }

    private static long insertPurchase(Connection conn, long guestUserId, String reservationConfirmationOrNull,
                                       double subtotal, double tax, double total) throws SQLException {
        String sql = """
                INSERT INTO Purchases (guest_id, reservation_confirmation, purchased_at, subtotal, tax, total)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, guestUserId);
            if (reservationConfirmationOrNull != null && !reservationConfirmationOrNull.isBlank()) {
                ps.setString(2, reservationConfirmationOrNull.trim());
            } else {
                ps.setNull(2, Types.VARCHAR);
            }
            ps.setString(3, Instant.now().toString());
            ps.setDouble(4, subtotal);
            ps.setDouble(5, tax);
            ps.setDouble(6, total);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
        }
        throw new SQLException("INSERT Purchases did not return generated id");
    }

    private static void insertPurchaseItems(Connection conn, long purchaseId, List<PurchaseItem> items) throws SQLException {
        String sql = """
                INSERT INTO PurchaseItems (purchase_id, product_id, quantity, unit_price_at_purchase)
                VALUES (?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (PurchaseItem it : items) {
                ps.setLong(1, purchaseId);
                ps.setLong(2, it.getItemId());
                ps.setInt(3, it.getQuantity());
                ps.setDouble(4, it.getUnitPriceAtPurchase());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static void decrementStock(Connection conn, List<PurchaseItem> items) throws SQLException {
        String sql = "UPDATE Products SET stock_qty = stock_qty - ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (PurchaseItem it : items) {
                ps.setInt(1, it.getQuantity());
                ps.setLong(2, it.getItemId());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public List<Purchase> listPurchasesForGuest(long guestUserId) throws SQLException {
        String sql = """
                SELECT id, guest_id, reservation_confirmation, purchased_at, subtotal, tax, total
                FROM Purchases
                WHERE guest_id = ?
                ORDER BY id DESC
                """;
        List<Purchase> list = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(jdbcUrl());
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, guestUserId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long purchaseId = rs.getLong("id");
                    List<PurchaseItem> items = listPurchaseItems(conn, purchaseId);
                    list.add(new Purchase(
                            purchaseId,
                            rs.getLong("guest_id"),
                            rs.getString("reservation_confirmation"),
                            Instant.parse(rs.getString("purchased_at")),
                            rs.getDouble("subtotal"),
                            rs.getDouble("tax"),
                            rs.getDouble("total"),
                            items
                    ));
                }
            }
        }
        return list;
    }

    private static List<PurchaseItem> listPurchaseItems(Connection conn, long purchaseId) throws SQLException {
        String sql = """
                SELECT product_id, quantity, unit_price_at_purchase
                FROM PurchaseItems
                WHERE purchase_id = ?
                ORDER BY product_id
                """;
        List<PurchaseItem> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, purchaseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new PurchaseItem(
                            rs.getLong("product_id"),
                            rs.getInt("quantity"),
                            rs.getDouble("unit_price_at_purchase")
                    ));
                }
            }
        }
        return list;
    }
}

