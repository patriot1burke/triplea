package games.strategy.engine.lobby.client.login;

import games.strategy.engine.framework.GameRunner2;
import games.strategy.engine.lobby.server.userDB.DBUser;
import games.strategy.engine.lobby.server.userDB.DBUserController;
import games.strategy.ui.Util;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public class CreateUpdateAccountPanel extends JPanel
{
	private static final long serialVersionUID = 2285956517232671122L;
	
	
	public static enum ReturnValue
	{
		CANCEL, OK
	}
	
	private JDialog m_dialog;
	private JTextField m_userName;
	private JTextField m_email;
	private JPasswordField m_password;
	private JPasswordField m_password2;
	private JButton m_okButton;
	private JButton m_cancelButton;
	private ReturnValue m_returnValue;
	
	public static CreateUpdateAccountPanel newUpdatePanel(final DBUser user)
	{
		final CreateUpdateAccountPanel panel = new CreateUpdateAccountPanel(false);
		panel.m_userName.setText(user.getName());
		panel.m_userName.setEditable(false);
		panel.m_email.setText(user.getEmail());
		return panel;
	}
	
	public static CreateUpdateAccountPanel newCreatePanel()
	{
		final CreateUpdateAccountPanel panel = new CreateUpdateAccountPanel(true);
		return panel;
	}
	
	private CreateUpdateAccountPanel(final boolean create)
	{
		createComponents();
		layoutComponents(create);
		setupListeners();
		setWidgetActivation();
	}
	
	private void createComponents()
	{
		m_userName = new JTextField();
		m_email = new JTextField();
		m_password = new JPasswordField();
		m_password2 = new JPasswordField();
		m_cancelButton = new JButton("Cancel");
		m_okButton = new JButton("OK");
	}
	
	private void layoutComponents(final boolean create)
	{
		final JLabel label = new JLabel(new ImageIcon(Util.getBanner(create ? "Create Account" : "Update Account")));
		setLayout(new BorderLayout());
		add(label, BorderLayout.NORTH);
		final JPanel main = new JPanel();
		add(main, BorderLayout.CENTER);
		main.setLayout(new GridBagLayout());
		main.add(new JLabel("Username:"), new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(10, 20, 0, 0), 0, 0));
		main.add(m_userName, new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(10, 5, 0, 40), 0, 0));
		main.add(new JLabel("Password:"), new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 20, 0, 0), 0, 0));
		main.add(m_password, new GridBagConstraints(1, 1, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(5, 5, 0, 40), 0, 0));
		main.add(new JLabel("Re-type Password:"), new GridBagConstraints(0, 2, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 20, 0, 0), 0, 0));
		main.add(m_password2, new GridBagConstraints(1, 2, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(5, 5, 0, 40), 0, 0));
		main.add(new JLabel("Email:"), new GridBagConstraints(0, 3, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 20, 15, 0), 0, 0));
		main.add(m_email, new GridBagConstraints(1, 3, 1, 1, 1, 1, GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(5, 5, 15, 40), 0, 0));
		final JPanel buttons = new JPanel();
		buttons.setLayout(new FlowLayout(FlowLayout.RIGHT));
		buttons.add(m_okButton);
		buttons.add(m_cancelButton);
		add(buttons, BorderLayout.SOUTH);
	}
	
	public static void main(final String[] args)
	{
		GameRunner2.setupLookAndFeel();
		final JDialog d = new JDialog();
		d.add(new CreateUpdateAccountPanel(false));
		d.setDefaultCloseOperation(JDialog.EXIT_ON_CLOSE);
		d.pack();
		d.setVisible(true);
	}
	
	private void setupListeners()
	{
		m_cancelButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				m_dialog.setVisible(false);
			}
		});
		m_okButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				okPressed();
			}
		});
	}
	
	@SuppressWarnings("deprecation")
	private void okPressed()
	{
		if (!m_password.getText().equals(m_password2.getText()))
		{
			JOptionPane.showMessageDialog(this, "The passwords do not match", "Passwords Do Not Match", JOptionPane.ERROR_MESSAGE);
			m_password.setText("");
			m_password2.setText("");
			return;
		}
		if (!games.strategy.util.Util.isMailValid(m_email.getText()))
		{
			JOptionPane.showMessageDialog(this, "You must enter a valid email", "No email", JOptionPane.ERROR_MESSAGE);
			return;
		}
		else if (DBUserController.validateUserName(m_userName.getText()) != null)
		{
			JOptionPane.showMessageDialog(this, DBUserController.validateUserName(m_userName.getText()), "Invalid Username", JOptionPane.ERROR_MESSAGE);
			return;
		}
		else if (m_password.getPassword().length == 0)
		{
			JOptionPane.showMessageDialog(this, "You must enter a password", "No Password", JOptionPane.ERROR_MESSAGE);
			return;
		}
		else if (m_password.getPassword().length < 3)
		{
			JOptionPane.showMessageDialog(this, "Passwords must be at least three characters long", "Invalid password", JOptionPane.ERROR_MESSAGE);
			return;
		}
		m_returnValue = ReturnValue.OK;
		m_dialog.setVisible(false);
	}
	
	private void setWidgetActivation()
	{
	}
	
	public ReturnValue show(final Window parent)
	{
		m_dialog = new JDialog(JOptionPane.getFrameForComponent(parent), "Login", true);
		m_dialog.getContentPane().add(this);
		m_dialog.pack();
		m_dialog.setLocationRelativeTo(parent);
		m_dialog.setVisible(true);
		m_dialog.dispose();
		m_dialog = null;
		if (m_returnValue == null)
			return ReturnValue.CANCEL;
		return m_returnValue;
	}
	
	@SuppressWarnings("deprecation")
	public String getPassword()
	{
		return m_password.getText();
	}
	
	public String getEmail()
	{
		return m_email.getText();
	}
	
	public String getUserName()
	{
		return m_userName.getText();
	}
}
