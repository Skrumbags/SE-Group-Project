package UI.Shopping;

import Controllers.ShoppingController;
import Domain.Shopping.Cart;
import Domain.Shopping.CartItem;
import Domain.Shopping.Item;
import Domain.Shopping.Purchase;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class CartUI extends JPanel {

    private final ShoppingController shoppingController;
    private final Runnable onBackHome;
    private final Runnable onBackToShop;

    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"ProductId", "Name", "Unit price", "Quantity", "Line total"},
            0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JTable table = new JTable(model);
    private final JTextField qtyField = new JTextField("1", 5);

    public CartUI(ShoppingController shoppingController, Runnable onBackHome, Runnable onBackToShop) {
        this.shoppingController = shoppingController;
        this.onBackHome = onBackHome;
        this.onBackToShop = onBackToShop;

        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 12, 12, 12));

        JLabel title = new JLabel("Shopping Cart");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));

        JButton homeBtn = new JButton("Home");
        homeBtn.addActionListener(e -> onBackHome.run());

        JButton backBtn = new JButton("← Back to shop");
        backBtn.addActionListener(e -> onBackToShop.run());

        JPanel north = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        north.add(homeBtn);
        north.add(backBtn);
        north.add(title);
        add(north, BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JButton updateQtyBtn = new JButton("Set qty");
        updateQtyBtn.addActionListener(e -> setSelectedQty());

        JButton removeBtn = new JButton("Remove");
        removeBtn.addActionListener(e -> removeSelected());

        JButton checkoutBtn = new JButton("Checkout / Purchase");
        checkoutBtn.addActionListener(e -> checkout());

        JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        south.add(new JLabel("Qty:"));
        south.add(qtyField);
        south.add(updateQtyBtn);
        south.add(removeBtn);
        south.add(checkoutBtn);
        add(south, BorderLayout.SOUTH);
    }

    public void refresh() {
        model.setRowCount(0);
        Cart cart = shoppingController.getCart();
        for (CartItem ci : cart.getItems()) {
            Item it = shoppingController.findProduct(ci.getItemId());
            double line = it.getUnitPrice() * ci.getQuantity();
            model.addRow(new Object[]{
                    ci.getItemId(),
                    it.getName(),
                    String.format("$%.2f", it.getUnitPrice()),
                    ci.getQuantity(),
                    String.format("$%.2f", line)
            });
        }

        // Hide internal id column from users, but keep it for updates/removes.
        if (table.getColumnModel().getColumnCount() > 0) {
            table.getColumnModel().getColumn(0).setMinWidth(0);
            table.getColumnModel().getColumn(0).setMaxWidth(0);
            table.getColumnModel().getColumn(0).setPreferredWidth(0);
        }
    }

    private void setSelectedQty() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a cart row first.", "Cart", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        long productId = Long.parseLong(String.valueOf(model.getValueAt(row, 0)));
        int qty;
        try {
            qty = Integer.parseInt(qtyField.getText().trim());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Quantity must be a number.", "Cart", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            shoppingController.setCartItemQuantity(productId, qty);
            refresh();
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Cart update failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void removeSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a cart row first.", "Cart", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        long productId = Long.parseLong(String.valueOf(model.getValueAt(row, 0)));
        try {
            shoppingController.removeCartItem(productId);
            refresh();
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Cart update failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void checkout() {
        int ok = JOptionPane.showConfirmDialog(this, "Purchase items in your cart now?", "Checkout",
                JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return;
        try {
            Purchase p = shoppingController.purchaseItems();
            JOptionPane.showMessageDialog(this,
                    "Purchase complete. Total: $" + String.format("%.2f", p.getTotal()),
                    "Checkout", JOptionPane.INFORMATION_MESSAGE);
            refresh();
            onBackHome.run();
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Checkout failed", JOptionPane.ERROR_MESSAGE);
        }
    }
}

