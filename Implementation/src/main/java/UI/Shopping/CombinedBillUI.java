package UI.Shopping;

import Controllers.ShoppingController;
import Domain.Reservations.Reservation;
import Domain.Shared.CombinedBill;
import Domain.Shopping.Purchase;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class CombinedBillUI extends JPanel {

    private final ShoppingController shoppingController;
    private final Runnable onBack;
    /** When non-null, bill is for this guest (clerk viewing); otherwise the logged-in guest. */
    private final Long fixedGuestUserId;

    private final JLabel totalsLabel = new JLabel(" ");

    private final DefaultTableModel reservationsModel = new DefaultTableModel(
            new Object[]{"Confirmation", "Dates", "Room", "Cost"},
            0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable reservationsTable = new JTable(reservationsModel);

    private final DefaultTableModel purchasesModel = new DefaultTableModel(
            new Object[]{"PurchaseId", "PurchasedAt", "Subtotal"},
            0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable purchasesTable = new JTable(purchasesModel);

    public CombinedBillUI(ShoppingController shoppingController, Runnable onBack) {
        this(shoppingController, onBack, null);
    }

    /**
     * @param fixedGuestUserId when set, combined bill is loaded for this guest id (clerk context);
     *                         when {@code null}, uses the logged-in guest.
     */
    public CombinedBillUI(ShoppingController shoppingController, Runnable onBack, Long fixedGuestUserId) {
        this.shoppingController = shoppingController;
        this.onBack = onBack;
        this.fixedGuestUserId = fixedGuestUserId;

        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 12, 12, 12));

        JLabel title = new JLabel(fixedGuestUserId == null ? "Combined Bill" : "Guest combined bill (clerk)");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));

        JButton backBtn = new JButton(fixedGuestUserId == null ? "Home" : "Close");
        backBtn.addActionListener(e -> onBack.run());

        JPanel north = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        north.add(backBtn);
        north.add(title);
        add(north, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(2, 1, 8, 8));

        JPanel resPanel = new JPanel(new BorderLayout(6, 6));
        resPanel.add(new JLabel("Reservations (room charges before tax)"), BorderLayout.NORTH);
        resPanel.add(new JScrollPane(reservationsTable), BorderLayout.CENTER);

        JPanel purPanel = new JPanel(new BorderLayout(6, 6));
        purPanel.add(new JLabel("Purchases (subtotal; store tax not included in combined total)"), BorderLayout.NORTH);
        purPanel.add(new JScrollPane(purchasesTable), BorderLayout.CENTER);

        center.add(resPanel);
        center.add(purPanel);
        add(center, BorderLayout.CENTER);

        totalsLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        add(totalsLabel, BorderLayout.SOUTH);
    }

    public void refresh() {
        CombinedBill bill = fixedGuestUserId == null
                ? shoppingController.combinedBill()
                : shoppingController.combinedBillForGuest(fixedGuestUserId);

        reservationsModel.setRowCount(0);
        for (Reservation r : bill.getReservations()) {
            reservationsModel.addRow(new Object[]{
                    r.getConfirmationNumber(),
                    r.getDateRange().getCheckInDate() + " → " + r.getDateRange().getCheckOutDate(),
                    r.getRoom().getRoomNumber(),
                    String.format("$%.2f", r.getTotalCost())
            });
        }

        purchasesModel.setRowCount(0);
        for (Purchase p : bill.getPurchases()) {
            purchasesModel.addRow(new Object[]{
                    p.getId(),
                    p.getPurchasedAt().toString(),
                    String.format("$%.2f", p.getSubtotal())
            });
        }

        totalsLabel.setText(
                "Room subtotal: " + String.format("$%.2f", bill.getStaySubtotal())
                        + " | Room tax: " + String.format("$%.2f", bill.getRoomTax())
                        + " | Stay total: " + String.format("$%.2f", bill.getStayTotal())
                        + " | Shopping subtotal: " + String.format("$%.2f", bill.getShoppingSubtotal())
                        + " | Grand total: " + String.format("$%.2f", bill.getCombinedTotal()));
    }
}
