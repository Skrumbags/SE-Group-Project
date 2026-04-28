package UI;

import Controllers.SearchController;
import Domain.Rooms.Room;
import Domain.Rooms.RoomType;
import Domain.Rooms.SearchCriteria;
import Domain.Shared.DateRange;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.function.BiConsumer;

public class RoomAvailabilityPanel extends JPanel {

    private static final Color BLUE       = new Color(24, 95, 165);
    private static final Color BLUE_LIGHT = new Color(230, 241, 251);
    private static final Color CARD_BG    = Color.WHITE;
    private static final Color BORDER_C   = new Color(210, 210, 210);
    private static final Color TEXT_MAIN  = new Color(30, 30, 30);
    private static final Color TEXT_MUTED = new Color(110, 110, 110);
    private static final Color GREEN      = new Color(34, 139, 34);
    private static final Color GREEN_LIGHT= new Color(220, 245, 220);

    private final JPanel resultsGrid = new JPanel();
    private final SearchController searchController;
    private final BiConsumer<Room, DateRange> onRoomStayChosen;

    private LocalDate lastSearchStartInclusive;
    private LocalDate lastSearchEndExclusive;

    public RoomAvailabilityPanel(SearchController searchController,
                                   BiConsumer<Room, DateRange> onRoomStayChosen) {
        this.searchController = searchController;
        this.onRoomStayChosen = onRoomStayChosen;
        setLayout(new BorderLayout(0, 0));
        setBackground(new Color(245, 247, 250));
        resultsGrid.setOpaque(true);

        // ── search bar ──────────────────────────────────────────────
        JPanel bar = new JPanel(new BorderLayout(0, 0));
        bar.setBackground(CARD_BG);
        bar.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 1, 0, BORDER_C),
                new EmptyBorder(10, 12, 10, 12)
        ));

        SpinnerDateModel beginModel = new SpinnerDateModel(
                new Date(), null, null, java.util.Calendar.DAY_OF_MONTH
        );
        SpinnerDateModel endModel = new SpinnerDateModel(
                new Date(System.currentTimeMillis() + 86400000L), null, null, java.util.Calendar.DAY_OF_MONTH
        );

        //SpinnerDateModel beginModel = new SpinnerDateModel();
        JSpinner beginDate = new JSpinner(beginModel);
        beginDate.setEditor(new JSpinner.DateEditor(beginDate, "MM/dd/yyyy"));
        beginDate.setPreferredSize(new Dimension(110, 30));

        //SpinnerDateModel endModel = new SpinnerDateModel();
        JSpinner endDate = new JSpinner(endModel);
        endDate.setEditor(new JSpinner.DateEditor(endDate, "MM/dd/yyyy"));
        endDate.setPreferredSize(new Dimension(110, 30));

        JComboBox<RoomType.FloorType> floorBox = new JComboBox<>(RoomType.FloorType.values());
        JComboBox<RoomType.BedType>   bedBox   = new JComboBox<>(RoomType.BedType.values());
        JTextField guestsField = new JTextField(3);
        guestsField.setPreferredSize(new Dimension(50, 30));

        JButton searchButton = new JButton("Search rooms");
        searchButton.setBackground(BLUE);
        searchButton.setForeground(Color.WHITE);
        searchButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        searchButton.setBorderPainted(false);
        searchButton.setFocusPainted(false);
        searchButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        searchButton.setPreferredSize(new Dimension(130, 30));

        // row 1 — dates and guests
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        row1.setBackground(CARD_BG);
        row1.add(makeBarLabel("From:"));  row1.add(beginDate);
        row1.add(makeBarLabel("To:"));    row1.add(endDate);
        row1.add(makeBarLabel("Guests:")); row1.add(guestsField);

        // row 2 — filters and button
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        row2.setBackground(CARD_BG);
        row2.add(makeBarLabel("Floor:")); row2.add(floorBox);
        row2.add(makeBarLabel("Bed:"));   row2.add(bedBox);
        row2.add(searchButton);

        bar.add(row1, BorderLayout.NORTH);
        bar.add(row2, BorderLayout.SOUTH);

        // ── results grid ─────────────────────────────────────────────
        resultsGrid.setLayout(new WrapLayout(FlowLayout.LEFT, 12, 12));
        resultsGrid.setBackground(new Color(245, 247, 250));

        JScrollPane scroll = new JScrollPane(resultsGrid);
        scroll.setPreferredSize(null);   // let it size naturally
        scroll.setMinimumSize(new Dimension(0, 0));

        // placeholder
        showPlaceholder("Search for available rooms above.");

        add(bar, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);

        // ── search action ─────────────────────────────────────────────
        searchButton.addActionListener(e -> {
            try {
                Date s = (Date) beginDate.getValue();
                Date en = (Date) endDate.getValue();
                LocalDate start = s.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                LocalDate end   = en.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                int numGuests   = Integer.parseInt(guestsField.getText().trim());

                long span = ChronoUnit.DAYS.between(start, end);
                if (span <= 0) {
                    JOptionPane.showMessageDialog(this, "\"To\" must be after \"From\".",
                            "Invalid date range", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (span > SearchCriteria.MAX_SEARCH_SPAN_DAYS) {
                    JOptionPane.showMessageDialog(this,
                            "From and To can be at most " + SearchCriteria.MAX_SEARCH_SPAN_DAYS
                                    + " days apart (1 year).",
                            "Invalid date range", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                RoomType roomType  = new RoomType(
                        (RoomType.FloorType) floorBox.getSelectedItem(),
                        (RoomType.BedType)   bedBox.getSelectedItem()
                );
                SearchCriteria criteria = new SearchCriteria(start, end, roomType, numGuests);
                List<Room> results = searchController.searchRooms(criteria);

                lastSearchStartInclusive = start;
                lastSearchEndExclusive = end;

                resultsGrid.removeAll();
                if (results.isEmpty()) {
                    showPlaceholder("No available rooms found.");
                } else {
                    for (Room r : results) resultsGrid.add(buildRoomCard(r));
                }
                resultsGrid.revalidate();
                resultsGrid.repaint();

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter a valid number of guests.",
                        "Invalid Input", JOptionPane.ERROR_MESSAGE);
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(),
                        "Invalid Dates", JOptionPane.ERROR_MESSAGE);
            } catch (RuntimeException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(),
                        "Invalid Dates", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private JPanel buildRoomCard(Room r) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_C, 1, true),
                new EmptyBorder(14, 16, 14, 16)
        ));
        card.setPreferredSize(new Dimension(195, 175));
        card.setMaximumSize(new Dimension(195, 175));

        // room number badge
        JLabel badge = new JLabel("Room " + r.getRoomNumber());
        badge.setFont(new Font("Segoe UI", Font.BOLD, 15));
        badge.setForeground(BLUE);
        badge.setAlignmentX(LEFT_ALIGNMENT);

        // floor + bed type
        JLabel typeLabel = new JLabel(
                r.getRoomType().getFloorType() + " · " + r.getRoomType().getBedType()
        );
        typeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        typeLabel.setForeground(TEXT_MUTED);
        typeLabel.setAlignmentX(LEFT_ALIGNMENT);

        // rate
        JLabel rateLabel = new JLabel("$" + r.getMaxDailyRate() + " / night");
        rateLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        rateLabel.setForeground(TEXT_MAIN);
        rateLabel.setAlignmentX(LEFT_ALIGNMENT);

        // smoking pill
        JLabel smokingPill = new JLabel(r.isSmoking() ? "Smoking" : "Non-smoking");
        smokingPill.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        smokingPill.setForeground(new Color(0, 100, 0));
        smokingPill.setBackground(GREEN_LIGHT);
        smokingPill.setOpaque(true);
        smokingPill.setBorder(new EmptyBorder(2, 8, 2, 8));
        smokingPill.setAlignmentX(LEFT_ALIGNMENT);

        card.add(badge);
        card.add(Box.createVerticalStrut(4));
        card.add(typeLabel);
        card.add(Box.createVerticalStrut(8));
        card.add(rateLabel);
        card.add(Box.createVerticalStrut(8));
        card.add(smokingPill);
        card.add(Box.createVerticalStrut(6));
        JLabel clickHint = new JLabel("Click for calendar");
        clickHint.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        clickHint.setForeground(BLUE);
        clickHint.setAlignmentX(LEFT_ALIGNMENT);
        card.add(clickHint);

        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (lastSearchStartInclusive == null || lastSearchEndExclusive == null) {
                    JOptionPane.showMessageDialog(RoomAvailabilityPanel.this,
                            "Run a room search first to set your date range.",
                            "Search required",
                            JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                Window w = SwingUtilities.getWindowAncestor(RoomAvailabilityPanel.this);
                new RoomCalendarAvailabilityDialog(
                        w,
                        searchController,
                        r,
                        lastSearchStartInclusive,
                        lastSearchEndExclusive,
                        onRoomStayChosen
                ).setVisible(true);
            }
        });

        return card;
    }

    private void showPlaceholder(String msg) {
        resultsGrid.removeAll();
        JLabel lbl = new JLabel(msg);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lbl.setForeground(TEXT_MUTED);
        lbl.setBorder(new EmptyBorder(24, 24, 24, 24));
        resultsGrid.add(lbl);
        resultsGrid.revalidate();
        resultsGrid.repaint();
    }

    private JLabel makeBarLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        l.setForeground(TEXT_MUTED);
        return l;
    }
}