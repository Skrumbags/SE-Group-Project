package UI.Clerk;

import Controllers.ShoppingController;
import Domain.Shopping.Item;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;

/**
 * Clerk tool: create product types (SKUs), set unit price, and set on-hand stock quantity.
 */
public class ClerkManageProductsUI extends JPanel {

    private final ShoppingController shoppingController;
    private final Runnable onClose;

    private final DefaultListModel<Item> model = new DefaultListModel<>();
    private final JList<Item> list = new JList<>(model);

    private final JTextField editPrice = new JTextField(10);
    private final JSpinner editStock = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));

    private final JTextField newSku = new JTextField(12);
    private final JTextField newName = new JTextField(18);
    private final JTextField newDescription = new JTextField(24);
    private final JTextField newPrice = new JTextField(8);
    private final JSpinner newStock = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));

    public ClerkManageProductsUI(ShoppingController shoppingController, Runnable onClose) {
        this.shoppingController = shoppingController;
        this.onClose = onClose;

        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel title = new JLabel("Manage store products", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        add(title, BorderLayout.NORTH);

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setVisibleRowCount(12);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> jList, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(jList, value, index, isSelected, cellHasFocus);
                if (value instanceof Item it) {
                    String act = it.isActive() ? "" : " [inactive]";
                    setText(it.getSku() + " — " + it.getName() + " — $" + String.format("%.2f", it.getUnitPrice())
                            + " — stock " + it.getStockQty() + act);
                }
                return this;
            }
        });
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelectionIntoEditors();
            }
        });

        JPanel editPanel = new JPanel();
        editPanel.setLayout(new BoxLayout(editPanel, BoxLayout.Y_AXIS));
        editPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                "Selected product",
                TitledBorder.LEFT,
                TitledBorder.TOP));

        JPanel rowPrice = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        rowPrice.add(new JLabel("Unit price ($):"));
        rowPrice.add(editPrice);
        editPanel.add(rowPrice);

        JPanel rowStock = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        rowStock.add(new JLabel("Stock quantity:"));
        rowStock.add(editStock);
        editPanel.add(rowStock);

        JPanel rowApply = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton apply = new JButton("Save price & stock");
        apply.addActionListener(e -> applyPriceAndStock());
        rowApply.add(apply);
        editPanel.add(rowApply);

        JPanel addPanel = new JPanel(new GridBagLayout());
        addPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                "Add new product",
                TitledBorder.LEFT,
                TitledBorder.TOP));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        c.gridy = 0;
        addPanel.add(new JLabel("SKU:"), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        addPanel.add(newSku, c);
        c.gridx = 0;
        c.gridy = 1;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        addPanel.add(new JLabel("Name:"), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        addPanel.add(newName, c);
        c.gridx = 0;
        c.gridy = 2;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        addPanel.add(new JLabel("Description (optional):"), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        addPanel.add(newDescription, c);
        c.gridx = 0;
        c.gridy = 3;
        c.fill = GridBagConstraints.NONE;
        addPanel.add(new JLabel("Unit price ($):"), c);
        c.gridx = 1;
        JPanel pRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pRow.add(newPrice);
        pRow.add(Box.createHorizontalStrut(16));
        pRow.add(new JLabel("Initial stock:"));
        pRow.add(newStock);
        addPanel.add(pRow, c);
        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 2;
        JButton addBtn = new JButton("Add product");
        addBtn.addActionListener(e -> addNewProduct());
        JPanel addBtnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        addBtnRow.add(addBtn);
        addPanel.add(addBtnRow, c);

        JPanel south = new JPanel(new BorderLayout(8, 8));
        south.add(editPanel, BorderLayout.NORTH);
        south.add(addPanel, BorderLayout.CENTER);

        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.add(new JScrollPane(list), BorderLayout.CENTER);
        center.add(south, BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton refresh = new JButton("Refresh list");
        refresh.addActionListener(e -> refreshList());
        JButton close = new JButton("Close");
        close.addActionListener(e -> onClose.run());
        bottom.add(refresh);
        bottom.add(close);
        add(bottom, BorderLayout.SOUTH);

        refreshList();
    }

    private void loadSelectionIntoEditors() {
        Item it = list.getSelectedValue();
        if (it == null) {
            editPrice.setText("");
            editStock.setValue(0);
            return;
        }
        editPrice.setText(String.format("%.2f", it.getUnitPrice()));
        editStock.setValue(it.getStockQty());
    }

    private void applyPriceAndStock() {
        Item it = list.getSelectedValue();
        if (it == null) {
            JOptionPane.showMessageDialog(this, "Select a product first.", "Edit product", JOptionPane.WARNING_MESSAGE);
            return;
        }
        double price;
        try {
            price = Double.parseDouble(editPrice.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter a valid unit price.", "Edit product", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int stock = (Integer) editStock.getValue();
        try {
            shoppingController.clerkUpdateProductUnitPrice(it.getId(), price);
            shoppingController.clerkUpdateProductStockQty(it.getId(), stock);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Edit product", JOptionPane.WARNING_MESSAGE);
            return;
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Edit product", JOptionPane.ERROR_MESSAGE);
            return;
        }
        refreshList();
        selectById(it.getId());
        JOptionPane.showMessageDialog(this, "Product updated.", "Edit product", JOptionPane.INFORMATION_MESSAGE);
    }

    private void addNewProduct() {
        double price;
        try {
            price = Double.parseDouble(newPrice.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter a valid unit price.", "Add product", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int stock = (Integer) newStock.getValue();
        try {
            long id = shoppingController.clerkCreateProduct(
                    newSku.getText(),
                    newName.getText(),
                    newDescription.getText(),
                    price,
                    stock);
            newSku.setText("");
            newName.setText("");
            newDescription.setText("");
            newPrice.setText("");
            newStock.setValue(0);
            refreshList();
            selectById(id);
            JOptionPane.showMessageDialog(this, "Product created.", "Add product", JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Add product", JOptionPane.WARNING_MESSAGE);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Add product", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void selectById(long id) {
        for (int i = 0; i < model.getSize(); i++) {
            if (model.getElementAt(i).getId() == id) {
                list.setSelectedIndex(i);
                list.ensureIndexIsVisible(i);
                return;
            }
        }
    }

    public void refreshList() {
        try {
            List<Item> items = shoppingController.listAllProductsForClerk();
            model.clear();
            for (Item it : items) {
                model.addElement(it);
            }
            loadSelectionIntoEditors();
        } catch (IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Store products", JOptionPane.WARNING_MESSAGE);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Store products", JOptionPane.ERROR_MESSAGE);
        }
    }
}
