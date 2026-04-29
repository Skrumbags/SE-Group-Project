package UI.Clerk;

import Domain.Rooms.Room;
import Domain.Rooms.RoomType;
import Domain.Rooms.RoomType.BedType;
import Domain.Rooms.RoomType.FloorType;
import Domain.Services.RoomService;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Clerk UI for modifying existing room information.
 */
public class ModifyRoomUI extends JPanel {

    private final RoomService roomService;

    private final JTextField searchField = new JTextField(14);
    private final DefaultListModel<String> resultsModel = new DefaultListModel<>();
    private final JList<String> resultsList = new JList<>(resultsModel);

    private final List<Integer> allRoomNumbers = new ArrayList<>();

    private final JComboBox<FloorType> floorTypeBox = new JComboBox<>(FloorType.values());
    private final JComboBox<BedType> bedTypeBox = new JComboBox<>(BedType.values());
    private final JCheckBox smokingCheck = new JCheckBox();
    private final JTextField dailyRateField = new JTextField(10);

    private final JButton backButton = new JButton();

    public ModifyRoomUI(RoomService roomService) {
        this.roomService = roomService;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        refreshRoomOptions();

        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));

        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchRow.add(new JLabel("Search room #:"));
        searchRow.add(searchField);
        north.add(searchRow);
        north.add(Box.createVerticalStrut(8));

        resultsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultsList.setVisibleRowCount(8);
        resultsList.setSelectionBackground(new Color(24, 95, 165));
        resultsList.setSelectionForeground(Color.WHITE);
        resultsList.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        JScrollPane scroll = new JScrollPane(resultsList);
        scroll.setPreferredSize(new Dimension(320, 160));
        north.add(scroll);

        add(north, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(4, 2, 8, 6));
        form.add(new JLabel("Floor Type:"));   form.add(floorTypeBox);
        form.add(new JLabel("Bed Type:"));     form.add(bedTypeBox);
        form.add(new JLabel("Smoking:"));      form.add(smokingCheck);
        form.add(new JLabel("Daily Rate:"));   form.add(dailyRateField);
        add(form, BorderLayout.CENTER);

        JButton saveButton = new JButton("Save room changes");
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        south.add(backButton);
        backButton.setVisible(false);
        south.add(saveButton);
        add(south, BorderLayout.SOUTH);

        resultsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelectedRoom();
            }
        });
        saveButton.addActionListener(e -> handleSave());

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            private void changed() { applyRoomFilter(); }
            @Override public void insertUpdate(DocumentEvent e) { changed(); }
            @Override public void removeUpdate(DocumentEvent e) { changed(); }
            @Override public void changedUpdate(DocumentEvent e) { changed(); }
        });

        applyRoomFilter();
    }

    public void refreshRoomOptions() {
        allRoomNumbers.clear();
        for (Room r : roomService.getRooms()) {
            allRoomNumbers.add(r.getRoomNumber());
        }
        allRoomNumbers.sort(Integer::compareTo);
    }

    private void applyRoomFilter() {
        String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        resultsModel.clear();
        for (Integer roomNum : allRoomNumbers) {
            String text = String.valueOf(roomNum);
            if (q.isBlank() || text.toLowerCase().contains(q)) {
                resultsModel.addElement(text);
            }
        }
        if (!resultsModel.isEmpty() && resultsList.getSelectedIndex() < 0) {
            resultsList.setSelectedIndex(0);
        }
    }

    private void loadSelectedRoom() {
        String selected = resultsList.getSelectedValue();
        Integer num = null;
        try {
            if (selected != null) {
                num = Integer.parseInt(selected);
            }
        } catch (NumberFormatException ignored) {}
        if (num == null) {
            return;
        }
        Room r = roomService.findRoom(num);
        if (r == null) {
            return;
        }
        RoomType type = r.getRoomType();
        if (type != null) {
            floorTypeBox.setSelectedItem(type.getFloorType());
            bedTypeBox.setSelectedItem(type.getBedType());
        }
        smokingCheck.setSelected(r.isSmoking());
        dailyRateField.setText(String.valueOf(r.getMaxDailyRate()));
    }

    private void handleSave() {
        try {
            String selected = resultsList.getSelectedValue();
            Integer num = selected == null ? null : Integer.parseInt(selected);
            if (num == null) {
                JOptionPane.showMessageDialog(this, "Select a room to modify.");
                return;
            }
            double dailyRate = Double.parseDouble(dailyRateField.getText().trim());
            if (dailyRate < 0) {
                JOptionPane.showMessageDialog(this, "Daily rate must be >= 0.");
                return;
            }

            RoomType roomType = new RoomType(
                    (FloorType) floorTypeBox.getSelectedItem(),
                    (BedType) bedTypeBox.getSelectedItem()
            );
            Room existing = roomService.findRoom(num);
            boolean availability = existing != null && existing.isAvailability();
            Room updated = new Room(
                    num,
                    smokingCheck.isSelected(),
                    availability,
                    dailyRate,
                    roomType
            );

            boolean ok = roomService.updateRoom(updated);
            if (!ok) {
                JOptionPane.showMessageDialog(this, "Room " + num + " not found.");
                return;
            }
            JOptionPane.showMessageDialog(this, "Room " + num + " updated!");
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid daily rate.");
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Invalid room data", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void setBackAction(ActionListener goBack, String backMessage) {
        backButton.addActionListener(goBack);
        backButton.setLabel(backMessage);
        backButton.setVisible(true);
    }
}

