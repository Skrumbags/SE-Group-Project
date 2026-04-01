package UI;

import Rooms.Room;
import Rooms.RoomType;
import Rooms.RoomType.FloorType;
import Rooms.RoomType.BedType;
import RoomCatalog.RoomCatalog;
import Utility.RoomService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class AddRoomUI extends JPanel {

    private RoomService roomService;

    private JTextField roomNumberField = new JTextField(10);
    private JTextField dailyRateField = new JTextField(10);
    private JComboBox<FloorType> floorTypeBox = new JComboBox<>(FloorType.values());
    private JComboBox<BedType> bedTypeBox = new JComboBox<>(BedType.values());
    private JCheckBox smokingCheck = new JCheckBox();
    private JButton backButton = new JButton();

    public AddRoomUI(RoomService roomService) {
        this.roomService = roomService;
        setLayout(new GridLayout(7, 2, 5, 5));

        add(new JLabel("Room Number:"));  add(roomNumberField);
        add(new JLabel("Floor Type:"));   add(floorTypeBox);
        add(new JLabel("Bed Type:"));     add(bedTypeBox);
        add(new JLabel("Smoking:"));      add(smokingCheck);
        add(new JLabel("Daily Rate:"));   add(dailyRateField);

        JButton addButton = new JButton("Add Room");
        add(backButton);
        backButton.setVisible(false);
        add(addButton);

        addButton.addActionListener(e -> handleAddRoom());
    }

    private void handleAddRoom() {
        try {
            int roomNumber = Integer.parseInt(roomNumberField.getText().trim());
            double dailyRate = Double.parseDouble(dailyRateField.getText().trim());
            RoomType roomType = new RoomType(
                    (FloorType) floorTypeBox.getSelectedItem(),
                    (BedType) bedTypeBox.getSelectedItem()
            );

            Room newRoom = new Room(roomNumber, smokingCheck.isSelected(), false, dailyRate, roomType);
            boolean success = roomService.addRoom(newRoom);

            if (success) {
                JOptionPane.showMessageDialog(this, "Room " + roomNumber + " added!");
            } else {
                JOptionPane.showMessageDialog(this, "Room " + roomNumber + " already exists!");
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter valid numbers.");
        }
    }

    public void setBackAction(ActionListener goBack, String backMessage) {
        backButton.addActionListener(goBack);
        backButton.setLabel(backMessage);
        backButton.setVisible(true);
    }
}