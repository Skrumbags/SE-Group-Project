package UI;

import Controllers.UserController;
import Domain.People.Guest;
import Domain.Services.UserService;

import javax.swing.*;
import java.awt.*;

/**
 * Guest self-service password reset: verify account email, then set and confirm a new password.
 */
public class GuestPasswordResetDialog extends JDialog {

    private final UserController userController;

    private final CardLayout cards = new CardLayout();
    private final JPanel cardPanel = new JPanel(cards);

    private final JTextField emailField = new JTextField(22);
    private final JPasswordField newPasswordField = new JPasswordField(22);
    private final JPasswordField confirmPasswordField = new JPasswordField(22);

    private String verifiedEmail;

    public GuestPasswordResetDialog(Window owner, UserController userController) {
        super(owner, "Reset guest password", ModalityType.APPLICATION_MODAL);
        this.userController = userController;

        JPanel emailStep = buildEmailStep();
        JPanel passwordStep = buildPasswordStep();

        cardPanel.add(emailStep, "EMAIL");
        cardPanel.add(passwordStep, "PASSWORD");

        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> dispose());

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(cancel);

        setLayout(new BorderLayout(8, 8));
        add(cardPanel, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);
        setResizable(false);
    }

    private JPanel buildEmailStep() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        p.add(new JLabel("Enter the email address on your guest account."), gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1;
        gbc.gridx = 0;
        p.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        p.add(emailField, gbc);

        JButton next = new JButton("Continue");
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        p.add(next, gbc);

        next.addActionListener(e -> onEmailContinue());
        return p;
    }

    private JPanel buildPasswordStep() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        p.add(new JLabel("Choose a new password."), gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1;
        gbc.gridx = 0;
        p.add(new JLabel("New password:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        p.add(newPasswordField, gbc);

        gbc.gridy = 2;
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        p.add(new JLabel("Confirm password:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        p.add(confirmPasswordField, gbc);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        JButton back = new JButton("Back");
        JButton save = new JButton("Save new password");
        row.add(back);
        row.add(save);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        p.add(row, gbc);

        back.addActionListener(e -> {
            newPasswordField.setText("");
            confirmPasswordField.setText("");
            cards.show(cardPanel, "EMAIL");
        });
        save.addActionListener(e -> onSavePassword());
        return p;
    }

    private void onEmailContinue() {
        String email = emailField.getText().trim();
        Guest guest = userController.findGuestByEmail(email);
        if (guest == null) {
            JOptionPane.showMessageDialog(this,
                    "No valid email.",
                    "Reset password",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        verifiedEmail = email;
        newPasswordField.setText("");
        confirmPasswordField.setText("");
        cards.show(cardPanel, "PASSWORD");
    }

    private void onSavePassword() {
        String p1 = new String(newPasswordField.getPassword());
        String p2 = new String(confirmPasswordField.getPassword());
        UserService.GuestPasswordResetResult r =
                userController.resetGuestPassword(verifiedEmail, p1, p2);
        switch (r) {
            case NO_VALID_EMAIL -> JOptionPane.showMessageDialog(this,
                    "No valid email.",
                    "Reset password",
                    JOptionPane.ERROR_MESSAGE);
            case PASSWORD_BLANK -> JOptionPane.showMessageDialog(this,
                    "Password cannot be blank.",
                    "Reset password",
                    JOptionPane.ERROR_MESSAGE);
            case PASSWORD_MISMATCH -> JOptionPane.showMessageDialog(this,
                    "Passwords do not match.",
                    "Reset password",
                    JOptionPane.ERROR_MESSAGE);
            case SAVE_FAILED -> JOptionPane.showMessageDialog(this,
                    "Could not save the new password. Try again or contact the hotel.",
                    "Reset password",
                    JOptionPane.ERROR_MESSAGE);
            case SUCCESS -> {
                JOptionPane.showMessageDialog(this,
                        "Your password has been updated. You can sign in with your username and new password.",
                        "Reset password",
                        JOptionPane.INFORMATION_MESSAGE);
                dispose();
            }
        }
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            verifiedEmail = null;
            emailField.setText("");
            newPasswordField.setText("");
            confirmPasswordField.setText("");
            cards.show(cardPanel, "EMAIL");
        }
        super.setVisible(visible);
    }
}
