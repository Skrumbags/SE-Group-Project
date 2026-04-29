package UI;

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
 * Calendar for one room: pick check-in then checkout (exclusive end, matching {@link DateRange}).
 * One month at a time (Prev/Next). Stays are limited by room availability, {@link DateRange#MAX_STAY},
 * and a forward booking horizon (matches the room search date spinners), not by the last search range.
 */
public class RoomCalendarAvailabilityDialog extends JDialog {

    private static final DateTimeFormatter TITLE = DateTimeFormatter.ofPattern("MMMM yyyy");
    /** Last calendar night offered for check-in; aligned with {@code RoomAvailabilityPanel} spinners. */
    private static final int BOOKING_HORIZON_YEARS_AHEAD = 3;

    private final SearchController searchController;
    private final Room room;
    private final BiConsumer<Room, DateRange> onConfirmed;

    private final JPanel monthsHost = new JPanel();
    private final JLabel hint = new JLabel(" ");
    private final JButton prevMonthBtn = new JButton("‹ Prev month");
    private final JButton nextMonthBtn = new JButton("Next month ›");
    private final JLabel currentMonthLabel = new JLabel(" ", SwingConstants.CENTER);
    /** Month currently shown in the calendar grid. */
    private YearMonth viewMonth;
    private LocalDate checkIn;

    /**
     * @param initialCalendarMonth first month to show (e.g. from the user's last search); only affects the view
     */
    public RoomCalendarAvailabilityDialog(Window owner, SearchController searchController, Room room,
                                          LocalDate initialCalendarMonth,
                                          BiConsumer<Room, DateRange> onConfirmed) {
        super(owner, "Availability — Room " + room.getRoomNumber(), ModalityType.APPLICATION_MODAL);
        this.searchController = searchController;
        this.room = room;
        this.onConfirmed = onConfirmed;
        this.viewMonth = YearMonth.from(initialCalendarMonth);

        setLayout(new BorderLayout(8, 8));
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(12, 12, 12, 12));

        JLabel title = new JLabel(
                "Room " + room.getRoomNumber() + " · " + room.getRoomType().getFloorType()
                        + " · " + room.getRoomType().getBedType(),
                SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 16));

        JLabel rangeLine = new JLabel(
                "<html><div style='text-align:center'>Pick any available stay (checkout after check-in, up to "
                        + DateRange.MAX_STAY + " nights). Check-in may be from today through "
                        + lastBookableNightInclusive() + ".</div></html>",
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

        monthsHost.setLayout(new BorderLayout());
        JScrollPane scroll = new JScrollPane(monthsHost);
        scroll.setPreferredSize(new Dimension(520, 340));
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        currentMonthLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        prevMonthBtn.setToolTipText("Show the previous month");
        nextMonthBtn.setToolTipText("Show the next month");
        prevMonthBtn.addActionListener(e -> {
            if (viewMonth.isAfter(navigableMinMonth())) {
                viewMonth = viewMonth.minusMonths(1);
                updateHintAndRebuild();
            }
        });
        nextMonthBtn.addActionListener(e -> {
            if (viewMonth.isBefore(navigableMaxMonth())) {
                viewMonth = viewMonth.plusMonths(1);
                updateHintAndRebuild();
            }
        });

        JPanel monthNav = new JPanel(new BorderLayout(8, 0));
        monthNav.add(prevMonthBtn, BorderLayout.WEST);
        monthNav.add(currentMonthLabel, BorderLayout.CENTER);
        monthNav.add(nextMonthBtn, BorderLayout.EAST);

        JPanel middle = new JPanel(new BorderLayout(0, 8));
        middle.add(monthNav, BorderLayout.NORTH);
        middle.add(scroll, BorderLayout.CENTER);

        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.add(hintWrap, BorderLayout.NORTH);
        center.add(middle, BorderLayout.CENTER);
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
                    + "Use <b>Prev month / Next month</b> to change months. "
                    + "Unavailable or past dates are greyed out."
                    + "</div></html>");
        } else {
            hint.setText("<html><div style='text-align:center;width:480px'>"
                    + "Check-in: <b>" + checkIn + "</b>. Click <b>checkout</b>, or <b>1 night</b> below."
                    + "</div></html>");
        }
        rebuildMonths();
    }

    private static LocalDate lastBookableNightInclusive() {
        return LocalDate.now().plusYears(BOOKING_HORIZON_YEARS_AHEAD);
    }

    /** One month before the current month (context only). */
    private YearMonth navigableMinMonth() {
        return YearMonth.from(LocalDate.now()).minusMonths(1);
    }

    /** Enough months to pick checkout up to {@link DateRange#MAX_STAY} nights from the last check-in night. */
    private YearMonth navigableMaxMonth() {
        LocalDate farCheckout = lastBookableNightInclusive().plusDays(DateRange.MAX_STAY);
        return YearMonth.from(farCheckout).plusMonths(1);
    }

    private boolean canPickAsCheckIn(LocalDate date, LocalDate today) {
        if (date.isBefore(today)) {
            return false;
        }
        if (date.isAfter(lastBookableNightInclusive())) {
            return false;
        }
        return searchController.isRoomFreeForNight(room, date);
    }

    private void rebuildMonths() {
        if (viewMonth.isBefore(navigableMinMonth())) {
            viewMonth = navigableMinMonth();
        }
        if (viewMonth.isAfter(navigableMaxMonth())) {
            viewMonth = navigableMaxMonth();
        }
        monthsHost.removeAll();
        LocalDate today = LocalDate.now();
        monthsHost.add(buildMonthCard(viewMonth, today), BorderLayout.CENTER);
        monthsHost.revalidate();
        monthsHost.repaint();

        currentMonthLabel.setText(viewMonth.format(TITLE));
        prevMonthBtn.setEnabled(viewMonth.isAfter(navigableMinMonth()));
        nextMonthBtn.setEnabled(viewMonth.isBefore(navigableMaxMonth()));
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

    private JButton dayButton(LocalDate date, LocalDate today) {
        JButton b = new JButton(String.valueOf(date.getDayOfMonth()));
        b.setMargin(new Insets(4, 2, 4, 2));
        b.setFont(new Font("SansSerif", Font.PLAIN, 12));

        boolean past = date.isBefore(today);
        boolean nightTaken = !searchController.isRoomFreeForNight(room, date);
        boolean tooFarForCheckIn = date.isAfter(lastBookableNightInclusive());

        boolean enabled;
        if (checkIn == null) {
            enabled = canPickAsCheckIn(date, today);
        } else {
            if (date.isEqual(checkIn)) {
                b.setBackground(new Color(180, 210, 255));
                b.setOpaque(true);
                enabled = canPickAsCheckIn(date, today);
            } else if (date.isBefore(checkIn)) {
                enabled = canPickAsCheckIn(date, today);
            } else {
                enabled = isValidCheckoutChoice(date);
            }
        }

        if (!enabled) {
            b.setEnabled(false);
            b.setForeground(new Color(160, 160, 160));
            if (tooFarForCheckIn && (checkIn == null || !date.isAfter(checkIn))) {
                b.setBackground(new Color(232, 232, 232));
                b.setOpaque(true);
                b.setToolTipText("Check-in cannot be after " + lastBookableNightInclusive());
            } else if (nightTaken && !past) {
                b.setToolTipText("Booked that night");
            } else if (past) {
                b.setToolTipText("Past date");
            } else if (checkIn != null && date.isAfter(checkIn)) {
                b.setToolTipText("Not available for this check-in or over " + DateRange.MAX_STAY + " nights");
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

    private boolean isValidCheckoutChoice(LocalDate checkoutExclusive) {
        if (checkoutExclusive.isBefore(checkIn) || checkoutExclusive.isEqual(checkIn)) {
            return false;
        }
        long nights = ChronoUnit.DAYS.between(checkIn, checkoutExclusive);
        if (nights <= 0 || nights > DateRange.MAX_STAY) {
            return false;
        }
        return searchController.isRoomFreeForStay(room, checkIn, checkoutExclusive);
    }

    private void onDayClicked(LocalDate date) {
        LocalDate today = LocalDate.now();
        if (checkIn == null) {
            if (!canPickAsCheckIn(date, today)) {
                return;
            }
            checkIn = date;
            updateHintAndRebuild();
            return;
        }

        if (date.isBefore(checkIn) || date.isEqual(checkIn)) {
            if (!canPickAsCheckIn(date, today)) {
                return;
            }
            checkIn = date;
            updateHintAndRebuild();
            return;
        }

        if (!isValidCheckoutChoice(date)) {
            JOptionPane.showMessageDialog(this,
                    "That checkout date is not available for this room and check-in (conflict or over "
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
