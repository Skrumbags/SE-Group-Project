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
    private final Long targetGuestUserId;
    private final String targetConfirmationNumber;

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
        this(shoppingController, onBack, null, null);
    }

    /**
     * @param targetConfirmationNumber when set, combined bill is loaded for this reservation (clerk context);
     *                         when {@code null}, uses the logged-in guest.
     */
    public CombinedBillUI(ShoppingController shoppingController, Runnable onBack, Long targetGuestUserId, String targetConfirmationNumber) {
        this.shoppingController = shoppingController;
        this.onBack = onBack;
        this.targetGuestUserId = targetGuestUserId;
        this.targetConfirmationNumber = targetConfirmationNumber;

        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 12, 12, 12));

        String titleStr = "Combined Bill";
        if (targetConfirmationNumber != null) {
            titleStr = "Reservation Bill: " + targetConfirmationNumber;
        } else if (targetGuestUserId != null) {
            titleStr = "Guest combined bill (clerk)";
        }

        JLabel title = new JLabel(titleStr);
        title.setFont(new Font("SansSerif", Font.BOLD, 18));

        JButton backBtn = new JButton((targetGuestUserId == null && targetConfirmationNumber == null) ? "Home" : "Close");
        backBtn.addActionListener(e -> onBack.run());

        JPanel north = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        north.add(backBtn);
        north.add(title);
        add(north, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        JPanel resPanel = new JPanel(new BorderLayout(6, 6));
        resPanel.add(new JLabel("Reservations (room charges before tax)"), BorderLayout.NORTH);
        JScrollPane resScroll = new JScrollPane(reservationsTable);
        resScroll.setPreferredSize(new Dimension(600, 160));
        resPanel.add(resScroll, BorderLayout.CENTER);

        JPanel purPanel = new JPanel(new BorderLayout(6, 6));
        purPanel.add(new JLabel("Purchases (subtotal; store tax not included in combined total)"), BorderLayout.NORTH);
        JScrollPane purScroll = new JScrollPane(purchasesTable);
        purScroll.setPreferredSize(new Dimension(600, 160));
        purPanel.add(purScroll, BorderLayout.CENTER);

        center.add(resPanel);
        center.add(Box.createVerticalStrut(8));
        center.add(purPanel);
        add(center, BorderLayout.CENTER);

        totalsLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        add(totalsLabel, BorderLayout.SOUTH);
    }

    public void refresh() {
        CombinedBill bill;

        // Route to the correct Domain logic based on what parameters were passed
        if (targetConfirmationNumber != null) {
            bill = shoppingController.combinedBillForReservation(targetConfirmationNumber);
        } else if (targetGuestUserId != null) {
            bill = shoppingController.combinedBillForGuest(targetGuestUserId);
        } else {
            bill = shoppingController.combinedBill();
        }

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
