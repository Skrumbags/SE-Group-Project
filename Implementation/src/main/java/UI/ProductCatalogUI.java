package UI;

import Controllers.ShoppingController;
import Domain.Shopping.Item;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class ProductCatalogUI extends JPanel {

    private final ShoppingController shoppingController;
    private final Runnable onBackHome;
    private final Runnable onOpenCart;

    private List<Item> lastItems = List.of();

    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Item ID", "Name", "Price"},
            0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JTable table = new JTable(model);
    private final JTextField qtyField = new JTextField("1", 5);
    private final JLabel emptyStateLabel = new JLabel(" ");

    public ProductCatalogUI(ShoppingController shoppingController, Runnable onBackHome, Runnable onOpenCart) {
        this.shoppingController = shoppingController;
        this.onBackHome = onBackHome;
        this.onOpenCart = onOpenCart;

        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 12, 12, 12));

        JLabel title = new JLabel("Shopping cart");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));

        JButton backBtn = new JButton("Home");
        backBtn.addActionListener(e -> onBackHome.run());

        JButton cartBtn = new JButton("View cart");
        cartBtn.addActionListener(e -> onOpenCart.run());

        JPanel north = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        north.add(backBtn);
        north.add(title);
        north.add(cartBtn);
        add(north, BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JButton addBtn = new JButton("Add to cart");
        addBtn.addActionListener(e -> addSelectedToCart());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        emptyStateLabel.setForeground(Color.DARK_GRAY);
        south.add(new JLabel("Qty:"));
        south.add(qtyField);
        south.add(addBtn);
        south.add(emptyStateLabel);
        add(south, BorderLayout.SOUTH);
    }

    public void refresh() {
        model.setRowCount(0);
        List<Item> items = shoppingController.listProducts();
        lastItems = items;
        for (Item it : items) {
            model.addRow(new Object[]{
                    it.getId(),
                    it.getName(),
                    String.format("$%.2f", it.getUnitPrice())
            });
        }
        emptyStateLabel.setText(items.isEmpty()
                ? "No items yet. Add products to the database to display them here."
                : " ");
    }

    private void addSelectedToCart() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select an item first.", "Add to cart", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (row >= lastItems.size()) {
            JOptionPane.showMessageDialog(this, "Please reopen the shop screen.", "Add to cart", JOptionPane.ERROR_MESSAGE);
            return;
        }
        long id = lastItems.get(row).getId();
        int qty;
        try {
            qty = Integer.parseInt(qtyField.getText().trim());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Quantity must be a number.", "Add to cart", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (qty <= 0) {
            JOptionPane.showMessageDialog(this, "Quantity must be >= 1.", "Add to cart", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            shoppingController.addToCart(id, qty);
            JOptionPane.showMessageDialog(this, "Added to cart.", "Add to cart", JOptionPane.INFORMATION_MESSAGE);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Add to cart failed", JOptionPane.ERROR_MESSAGE);
        }
    }
}

