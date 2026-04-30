package UI.Clerk;

import Domain.Rooms.Room;
import Domain.Rooms.RoomType;
import Domain.Rooms.RoomType.FloorType;
import Domain.Rooms.RoomType.BedType;
import Domain.Services.RoomService;

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
    private JCheckBox availabilityCheck = new JCheckBox();

    public AddRoomUI(RoomService roomService) {
        this.roomService = roomService;
        setLayout(new GridLayout(7, 2, 5, 5));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(new JLabel("Room Number:"));  add(roomNumberField);
        add(new JLabel("Floor Type:"));   add(floorTypeBox);
        add(new JLabel("Bed Type:"));     add(bedTypeBox);
        add(new JLabel("Smoking:"));      add(smokingCheck);
        add(new JLabel("Daily Rate:"));   add(dailyRateField);
        add(new JLabel("Available:"));  add(availabilityCheck);

        JButton addButton = new JButton("Save room to catalog");
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

            if (roomNumber < 0 || dailyRate < 0) {
                JOptionPane.showMessageDialog(this, "Room number and Daily rate must be >= 0.");
            }
            else {
                Room newRoom = new Room(roomNumber, smokingCheck.isSelected(), availabilityCheck.isSelected(), dailyRate, roomType);
                boolean success = roomService.addRoom(newRoom);

                if (success) {
                    JOptionPane.showMessageDialog(this, "Room " + roomNumber + " added!");
                } else {
                    JOptionPane.showMessageDialog(this, "Room " + roomNumber + " already exists!");
                }
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

