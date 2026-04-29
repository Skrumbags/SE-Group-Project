package UI.Other;

import Controllers.SearchController;
import Domain.Rooms.Room;
import Domain.Shared.DateRange;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.function.BiConsumer;

/**
 * Month-by-month calendar for one room within the search window. Days outside that window are
 * disabled. User picks check-in, then check-out (exclusive end date, matching {@link DateRange}).
 */
public class RoomCalendarAvailabilityDialog extends JDialog {

    private static final DateTimeFormatter TITLE = DateTimeFormatter.ofPattern("MMMM yyyy");

    private final SearchController searchController;
    private final Room room;
    private final LocalDate searchStartInclusive;
    private final LocalDate searchEndExclusive;
    private final BiConsumer<Room, DateRange> onConfirmed;

    private final JPanel monthsHost = new JPanel();
    private final JLabel hint = new JLabel(" ");
    private LocalDate checkIn;

    public RoomCalendarAvailabilityDialog(Window owner, SearchController searchController, Room room,
                                          LocalDate searchStartInclusive, LocalDate searchEndExclusive,
                                          BiConsumer<Room, DateRange> onConfirmed) {
        super(owner, "Availability — Room " + room.getRoomNumber(), ModalityType.APPLICATION_MODAL);
        this.searchController = searchController;
        this.room = room;
        this.searchStartInclusive = searchStartInclusive;
        this.searchEndExclusive = searchEndExclusive;
        this.onConfirmed = onConfirmed;

        setLayout(new BorderLayout(8, 8));
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(12, 12, 12, 12));

        JLabel title = new JLabel(
                "Room " + room.getRoomNumber() + " · " + room.getRoomType().getFloorType()
                        + " · " + room.getRoomType().getBedType(),
                SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 16));

        JLabel rangeLine = new JLabel(
                "Your search: " + searchStartInclusive + " → " + searchEndExclusive + " (checkout morning)",
                SwingConstants.CENTER);
        rangeLine.setFont(new Font("SansSerif", Font.PLAIN, 12));
        rangeLine.setForeground(new Color(80, 80, 80));

        JPanel north = new JPanel(new GridLayout(2, 1, 4, 4));
        north.add(title);
        north.add(rangeLine);
        add(north, BorderLayout.NORTH);

        hint.setFont(new Font("SansSerif", Font.PLAIN, 13));
        hint.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel hintWrap = new JPanel(new BorderLayout());
        hintWrap.add(hint, BorderLayout.CENTER);

        monthsHost.setLayout(new BoxLayout(monthsHost, BoxLayout.Y_AXIS));
        JScrollPane scroll = new JScrollPane(monthsHost);
        scroll.setPreferredSize(new Dimension(520, 420));

        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.add(hintWrap, BorderLayout.NORTH);
        center.add(scroll, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        JButton oneNight = new JButton("1 night (checkout next day)");
        oneNight.addActionListener(e -> confirmOneNight());
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> dispose());
        south.add(oneNight);
        south.add(cancel);
        add(south, BorderLayout.SOUTH);

        updateHintAndRebuild();
        pack();
        setLocationRelativeTo(owner);
    }

    private void updateHintAndRebuild() {
        if (checkIn == null) {
            hint.setText("<html><div style='text-align:center;width:480px'>"
                    + "Step 1: Click your <b>check-in</b> date (first night). "
                    + "Step 2: Click <b>checkout</b> (morning you leave). "
                    + "Days outside your search range are greyed out."
                    + "</div></html>");
        } else {
            hint.setText("<html><div style='text-align:center;width:480px'>"
                    + "Check-in: <b>" + checkIn + "</b>. Click <b>checkout</b>, or <b>1 night</b> below."
                    + "</div></html>");
        }
        rebuildMonths();
    }

    private void rebuildMonths() {
        monthsHost.removeAll();
        LocalDate today = LocalDate.now();
        YearMonth cur = YearMonth.from(searchStartInclusive);
        YearMonth last = YearMonth.from(searchEndExclusive.minusDays(1));
        if (last.isBefore(cur)) {
            last = cur;
        }
        while (!cur.isAfter(last)) {
            monthsHost.add(buildMonthCard(cur, today));
            monthsHost.add(Box.createVerticalStrut(16));
            cur = cur.plusMonths(1);
        }
        monthsHost.revalidate();
        monthsHost.repaint();
    }

    private JPanel buildMonthCard(YearMonth ym, LocalDate today) {
        JPanel card = new JPanel(new BorderLayout(4, 4));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                new EmptyBorder(8, 10, 10, 10)
        ));
        JLabel head = new JLabel(ym.format(TITLE), SwingConstants.CENTER);
        head.setFont(new Font("SansSerif", Font.BOLD, 14));
        card.add(head, BorderLayout.NORTH);

        String[] dow = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        JPanel grid = new JPanel(new GridLayout(0, 7, 2, 2));
        for (String d : dow) {
            JLabel lab = new JLabel(d, SwingConstants.CENTER);
            lab.setFont(new Font("SansSerif", Font.PLAIN, 11));
            lab.setForeground(new Color(90, 90, 90));
            grid.add(lab);
        }

        LocalDate first = ym.atDay(1);
        int lead = first.getDayOfWeek().getValue() % 7;
        int daysInMonth = ym.lengthOfMonth();
        int cells = lead + daysInMonth;
        int rows = (cells + 6) / 7;
        int totalSlots = rows * 7;

        for (int i = 0; i < totalSlots; i++) {
            int dayNum = i - lead + 1;
            if (i < lead || dayNum > daysInMonth) {
                grid.add(new JLabel());
                continue;
            }
            LocalDate date = ym.atDay(dayNum);
            grid.add(dayButton(date, today));
        }

        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    /** Calendar band matching search spinners: inclusive through exclusive end date. */
    private boolean inSearchBand(LocalDate date) {
        return !date.isBefore(searchStartInclusive) && !date.isAfter(searchEndExclusive);
    }

    private JButton dayButton(LocalDate date, LocalDate today) {
        JButton b = new JButton(String.valueOf(date.getDayOfMonth()));
        b.setMargin(new Insets(4, 2, 4, 2));
        b.setFont(new Font("SansSerif", Font.PLAIN, 12));

        boolean outsideSearch = !inSearchBand(date);
        boolean past = date.isBefore(today);
        boolean nightTaken = !searchController.isRoomFreeForNight(room, date);

        boolean enabled;
        if (checkIn == null) {
            enabled = !outsideSearch && !past && !nightTaken && date.isBefore(searchEndExclusive);
        } else {
            if (date.isEqual(checkIn)) {
                b.setBackground(new Color(180, 210, 255));
                b.setOpaque(true);
                enabled = !outsideSearch && !past && !nightTaken;
            } else if (date.isBefore(checkIn) || date.isEqual(checkIn)) {
                enabled = !outsideSearch && !past && !nightTaken && date.isBefore(searchEndExclusive);
            } else {
                enabled = !outsideSearch && isValidCheckoutChoice(date);
            }
        }

        if (!enabled) {
            b.setEnabled(false);
            b.setForeground(new Color(160, 160, 160));
            if (outsideSearch) {
                b.setBackground(new Color(232, 232, 232));
                b.setOpaque(true);
                b.setToolTipText("Outside your search date range");
            } else if (nightTaken && !past) {
                b.setToolTipText("Booked that night");
            } else if (past) {
                b.setToolTipText("Past date");
            } else if (checkIn != null && date.isAfter(checkIn)) {
                b.setToolTipText("Not available for this check-in / exceeds max stay");
            } else if (checkIn == null && !date.isBefore(searchEndExclusive)) {
                b.setToolTipText("Check-in must be before your search \"To\" date");
            }
        } else {
            if (checkIn == null) {
                b.setBackground(new Color(220, 245, 220));
            } else if (date.isAfter(checkIn)) {
                b.setBackground(new Color(200, 235, 200));
            } else {
                b.setBackground(new Color(220, 245, 220));
            }
            b.setOpaque(true);
        }

        b.addActionListener(e -> onDayClicked(date));
        return b;
    }

    private boolean stayWithinSearchWindow(LocalDate in, LocalDate outExclusive) {
        return !in.isBefore(searchStartInclusive)
                && !outExclusive.isAfter(searchEndExclusive)
                && in.isBefore(outExclusive);
    }

    private boolean isValidCheckoutChoice(LocalDate checkoutExclusive) {
        if (checkoutExclusive.isBefore(checkIn) || checkoutExclusive.isEqual(checkIn)) {
            return false;
        }
        long nights = ChronoUnit.DAYS.between(checkIn, checkoutExclusive);
        if (nights <= 0 || nights > DateRange.MAX_STAY) {
            return false;
        }
        if (!stayWithinSearchWindow(checkIn, checkoutExclusive)) {
            return false;
        }
        return searchController.isRoomFreeForStay(room, checkIn, checkoutExclusive);
    }

    private void onDayClicked(LocalDate date) {
        if (!inSearchBand(date)) {
            return;
        }
        if (checkIn == null) {
            if (date.isBefore(LocalDate.now())) {
                return;
            }
            if (!inSearchBand(date) || !date.isBefore(searchEndExclusive)) {
                return;
            }
            if (!searchController.isRoomFreeForNight(room, date)) {
                return;
            }
            checkIn = date;
            updateHintAndRebuild();
            return;
        }

        if (date.isBefore(checkIn) || date.isEqual(checkIn)) {
            if (date.isBefore(LocalDate.now()) || !inSearchBand(date) || !date.isBefore(searchEndExclusive)) {
                return;
            }
            if (!searchController.isRoomFreeForNight(room, date)) {
                return;
            }
            checkIn = date;
            updateHintAndRebuild();
            return;
        }

        if (!isValidCheckoutChoice(date)) {
            JOptionPane.showMessageDialog(this,
                    "That checkout date is not available for this room and check-in (conflict, outside search range, or over "
                            + DateRange.MAX_STAY + " nights).",
                    "Unavailable",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            DateRange range = new DateRange(checkIn, date);
            onConfirmed.accept(room, range);
            dispose();
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Invalid range", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void confirmOneNight() {
        if (checkIn == null) {
            JOptionPane.showMessageDialog(this, "Select a check-in date first.", "Check-in required",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        LocalDate checkout = checkIn.plusDays(1);
        if (!stayWithinSearchWindow(checkIn, checkout)) {
            JOptionPane.showMessageDialog(this,
                    "One night from this check-in falls outside your search dates.",
                    "Outside search range",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!searchController.isRoomFreeForStay(room, checkIn, checkout)) {
            JOptionPane.showMessageDialog(this, "That night is not available.", "Unavailable",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            DateRange range = new DateRange(checkIn, checkout);
            onConfirmed.accept(room, range);
            dispose();
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Invalid range", JOptionPane.ERROR_MESSAGE);
        }
    }
}
