package UI;

import Controllers.SearchController;
import Domain.Rooms.Room;
import Domain.Rooms.RoomType;
import Domain.Rooms.SearchCriteria;
import Domain.Shared.DateRange;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

/**
 * Shared "search available rooms" form (used on the public landing and in the guest shell).
 */
public class RoomAvailabilityPanel extends JPanel {

    public RoomAvailabilityPanel(SearchController searchController) {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createTitledBorder("Search Available Rooms"));

        JPanel fieldsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));

        SpinnerDateModel beginModel = new SpinnerDateModel();
        JSpinner beginDate = new JSpinner(beginModel);
        beginDate.setEditor(new JSpinner.DateEditor(beginDate, "MM/dd/yyyy"));
        beginDate.setPreferredSize(new Dimension(110, 25));

        SpinnerDateModel endModel = new SpinnerDateModel();
        JSpinner endDate = new JSpinner(endModel);
        endDate.setEditor(new JSpinner.DateEditor(endDate, "MM/dd/yyyy"));
        endDate.setPreferredSize(new Dimension(110, 25));

        JComboBox<RoomType.FloorType> floorBox =
                new JComboBox<>(RoomType.FloorType.values());
        JComboBox<RoomType.BedType> bedBox =
                new JComboBox<>(RoomType.BedType.values());
        JTextField guestsField = new JTextField(3);
        JButton searchButton = new JButton("Search");

        fieldsPanel.add(new JLabel("From:"));
        fieldsPanel.add(beginDate);
        fieldsPanel.add(new JLabel("To:"));
        fieldsPanel.add(endDate);
        fieldsPanel.add(new JLabel("Floor:"));
        fieldsPanel.add(floorBox);
        fieldsPanel.add(new JLabel("Bed:"));
        fieldsPanel.add(bedBox);
        fieldsPanel.add(new JLabel("Guests:"));
        fieldsPanel.add(guestsField);
        fieldsPanel.add(searchButton);

        JTextArea resultsArea = new JTextArea(3, 40);
        resultsArea.setEditable(false);
        resultsArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(resultsArea);

        add(fieldsPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        searchButton.addActionListener(e -> {
            try {
                Date startDate = (Date) beginDate.getValue();
                Date endDateVal = (Date) endDate.getValue();
                LocalDate start = startDate.toInstant()
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                LocalDate end = endDateVal.toInstant()
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDate();

                int numGuests = Integer.parseInt(guestsField.getText().trim());

                RoomType roomType = new RoomType(
                        (RoomType.FloorType) floorBox.getSelectedItem(),
                        (RoomType.BedType) bedBox.getSelectedItem()
                );

                DateRange dateRange = new DateRange(start, end);
                SearchCriteria criteria = new SearchCriteria(dateRange, roomType, numGuests);
                List<Room> results = searchController.searchRooms(criteria);

                if (results.isEmpty()) {
                    resultsArea.setText("No available rooms found.");
                } else {
                    StringBuilder sb = new StringBuilder();
                    for (Room r : results) {
                        sb.append("Room ").append(r.getRoomNumber())
                                .append(" | ").append(r.getRoomType().getFloorType())
                                .append(" | ").append(r.getRoomType().getBedType())
                                .append(" | $").append(r.getMaxDailyRate())
                                .append(r.isSmoking() ? " | Smoking" : " | Non-smoking")
                                .append("\n");
                    }
                    resultsArea.setText(sb.toString());
                }

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this,
                        "Please enter a valid number of guests.",
                        "Invalid Input", JOptionPane.ERROR_MESSAGE);
            } catch (RuntimeException ex) {
                JOptionPane.showMessageDialog(this,
                        ex.getMessage(),
                        "Invalid Dates", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
