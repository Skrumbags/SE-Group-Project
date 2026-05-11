package UI.Guest;

import Controllers.ReservationController;
import Domain.People.User;
import Domain.Reservations.Reservation;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Guest landing: greeting, reservations preview, and shortcuts to search, book, shop, and profile.
 */
public class GuestUI extends JPanel {

    private final ReservationController reservationController;
    private final Runnable onManageReservations;
    private final Consumer<String> onOpenReservationInManage;

    private final DefaultListModel<String> previewModel = new DefaultListModel<>();
    private final JList<String> previewList = new JList<>(previewModel);
    /** Same indices as {@link #previewModel} for real reservations only (empty for placeholder rows). */
    private final List<Reservation> previewRowCache = new ArrayList<>();

    public GuestUI(User user,
                   ReservationController reservationController,
                   Runnable onSearchRooms,
                   Runnable onShopBrowse,
                   Runnable onShopCart,
                   Runnable onCombinedBill,
                   Runnable onEditProfile,
                   Runnable onManageReservations,
                   Consumer<String> onOpenReservationInManage) {
        this.reservationController = reservationController;
        this.onManageReservations = onManageReservations;
        this.onOpenReservationInManage = onOpenReservationInManage;

        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                refreshReservationsPreview();
            }
        });

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        gbc.insets = new Insets(10, 10, 10, 10);

        String greeting = (user != null) ? ("Hello, " + user.getName() + "!") : "Hello!";
        JLabel nameLabel = new JLabel(greeting);
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 24));

        int row = 0;
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 1;
        gbc.weighty = 0;

        panel.add(nameLabel, gbc);

        ImageIcon icon = new ImageIcon("employee.png");
        if (icon.getImage() != null) {
            Image img = icon.getImage().getScaledInstance(150, 150, Image.SCALE_SMOOTH);
            JLabel imageLabel = new JLabel(new ImageIcon(img));
            gbc.gridy = row++;
            gbc.anchor = GridBagConstraints.CENTER;
            panel.add(imageLabel, gbc);
        }

        JPanel reservationsPanel = buildReservationsPreview();
        gbc.gridy = row++;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 0.25;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.ipady = 0;
        reservationsPanel.setPreferredSize(new Dimension(520, 160));
        reservationsPanel.setMaximumSize(new Dimension(800, 200));
        panel.add(reservationsPanel, gbc);
        gbc.weighty = 0;

        JPanel buttonPanel = new JPanel(new GridLayout(2, 1, 0, 12));
        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 10));
        JPanel bottomRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 10));

        Font buttonFont = new Font("Segoe UI", Font.PLAIN, 17);
        Dimension buttonSize = new Dimension(260, 56);

        JButton manageBtn = new JButton("Manage Reservations");
        manageBtn.setToolTipText("View or modify existing reservations");
        manageBtn.addActionListener(e -> onManageReservations.run());
        JButton searchBtn = new JButton("Search & Book Rooms");
        searchBtn.addActionListener(e -> onSearchRooms.run());
        JButton shopBtn = new JButton("Shopping");
        shopBtn.addActionListener(e -> onShopBrowse.run());
        JButton cartBtn = new JButton("View Cart");
        cartBtn.addActionListener(e -> onShopCart.run());
        JButton billBtn = new JButton("Combined Bill");
        billBtn.addActionListener(e -> onCombinedBill.run());
        JButton editProfile = new JButton("Edit Profile");
        editProfile.addActionListener(e -> onEditProfile.run());

        for (JButton b : new JButton[]{manageBtn, searchBtn, shopBtn, cartBtn, billBtn, editProfile}) {
            b.setFont(buttonFont);
            b.setPreferredSize(buttonSize);
            b.setMinimumSize(new Dimension(200, 52));
        }

        topRow.add(searchBtn);
        topRow.add(manageBtn);
        topRow.add(editProfile);
        bottomRow.add(shopBtn);
        bottomRow.add(cartBtn);
        bottomRow.add(billBtn);

        buttonPanel.add(topRow);
        buttonPanel.add(bottomRow);

        gbc.gridy = row;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 0;
        panel.add(buttonPanel, gbc);

        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);

        refreshReservationsPreview();
    }

    /** Reloads the reservation lines from the controller (e.g. when this panel becomes visible). */
    private void refreshReservationsPreview() {
        previewModel.clear();
        previewRowCache.clear();
        try {
            List<Reservation> mine = reservationController.getMyReservations();
            if (mine.isEmpty()) {
                previewModel.addElement("No reservations yet — use Search or Book a room to get started.");
            } else {
                for (Reservation r : mine) {
                    previewRowCache.add(r);
                    previewModel.addElement(formatPreviewLine(r));
                }
            }
        } catch (IllegalStateException ex) {
            previewModel.addElement("Sign in to see your reservations.");
        }
    }

    private JPanel buildReservationsPreview() {
        JPanel wrap = new JPanel(new BorderLayout(8, 8));
        wrap.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(200, 200, 200)),
                        BorderFactory.createEmptyBorder(8, 10, 10, 10)),
                "Your reservations",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 14)));

        previewList.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        previewList.setVisibleRowCount(5);
        previewList.setFixedCellHeight(22);
        previewList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        previewList.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        previewList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int i = previewList.locationToIndex(e.getPoint());
                if (i < 0 || i >= previewRowCache.size()) {
                    return;
                }
                Rectangle cell = previewList.getCellBounds(i, i);
                if (cell == null || !cell.contains(e.getPoint())) {
                    return;
                }
                Reservation r = previewRowCache.get(i);
                if (r.getConfirmationNumber() != null) {
                    onOpenReservationInManage.accept(r.getConfirmationNumber());
                }
            }
        });

        JScrollPane scroll = new JScrollPane(previewList);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        wrap.add(scroll, BorderLayout.CENTER);

        return wrap;
    }

    private static String formatPreviewLine(Reservation r) {
        String conf = r.getConfirmationNumber();
        String dates = r.getDateRange().getCheckInDate() + " → " + r.getDateRange().getCheckOutDate();
        String room = "Room " + r.getRoom().getRoomNumber();
        String tail = r.isActive() ? " · checked in" : "";
        return conf + " · " + room + " · " + dates + tail;
    }
}
