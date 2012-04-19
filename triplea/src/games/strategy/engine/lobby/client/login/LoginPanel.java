package games.strategy.engine.lobby.client.login;

import games.strategy.engine.framework.GameRunner2;
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
import java.awt.event.KeyEvent;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

public class LoginPanel extends JPanel
{
	private static final long serialVersionUID = -1115199161238394717L;
	private final static Logger s_logger = Logger.getLogger(LoginPanel.class.getName());
	
	
	public static enum ReturnValue
	{
		CANCEL, LOGON, CREATE_ACCOUNT
	}
	
	public static final String LAST_LOGIN_NAME_PREF = "LAST_LOGIN_NAME_PREF";
	public static final String ANONYMOUS_LOGIN_PREF = "ANONYMOUS_LOGIN_PREF";
	private JDialog m_dialog;
	private JPasswordField m_password;
	private JTextField m_userName;
	private JCheckBox m_anonymous;
	private JButton m_createAccount;
	private ReturnValue m_returnValue;
	private JButton m_logon;
	private JButton m_cancel;
	
	public LoginPanel()
	{
		createComponents();
		layoutComponents();
		setupListeners();
		readDefaults();
		setWidgetActivation();
	}
	
	private void readDefaults()
	{
		final Preferences prefs = Preferences.userNodeForPackage(this.getClass());
		final String name = prefs.get(LAST_LOGIN_NAME_PREF, System.getProperty("user.name"));
		final boolean anonymous = prefs.getBoolean(ANONYMOUS_LOGIN_PREF, true);
		m_anonymous.setSelected(anonymous);
		m_userName.setText(name);
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				if (!m_anonymous.isSelected())
				{
					m_password.requestFocusInWindow();
				}
				else
				{
					m_userName.requestFocusInWindow();
				}
			}
		});
	}
	
	private void createComponents()
	{
		m_password = new JPasswordField();
		m_userName = new JTextField();
		m_anonymous = new JCheckBox("Login Anonymously?");
		m_createAccount = new JButton("Create Account...");
		m_logon = new JButton("Login");
		m_cancel = new JButton("Cancel");
	}
	
	private void layoutComponents()
	{
		final JLabel label = new JLabel(new ImageIcon(Util.getBanner("Login")));
		setLayout(new BorderLayout());
		add(label, BorderLayout.NORTH);
		final JPanel main = new JPanel();
		add(main, BorderLayout.CENTER);
		main.setLayout(new GridBagLayout());
		main.add(new JLabel("Username:"), new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(10, 20, 0, 0), 0, 0));
		main.add(m_userName, new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(10, 5, 0, 40), 0, 0));
		main.add(new JLabel("Password:"), new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 20, 0, 0), 0, 0));
		main.add(m_password, new GridBagConstraints(1, 1, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(5, 5, 0, 40), 0, 0));
		main.add(m_anonymous, new GridBagConstraints(0, 2, 2, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 20, 0, 0), 0, 0));
		main.add(m_createAccount, new GridBagConstraints(0, 3, 2, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 20, 0, 0), 0, 0));
		final JPanel buttons = new JPanel();
		buttons.setLayout(new FlowLayout(FlowLayout.RIGHT));
		buttons.add(m_logon);
		buttons.add(m_cancel);
		add(buttons, BorderLayout.SOUTH);
	}
	
	private void setupListeners()
	{
		m_logon.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				logonPressed();
			}
		});
		m_createAccount.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				m_returnValue = ReturnValue.CREATE_ACCOUNT;
				m_dialog.setVisible(false);
			}
		});
		m_cancel.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				m_dialog.setVisible(false);
			}
		});
		m_anonymous.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				setWidgetActivation();
			}
		});
		// close when hitting the escape key
		final Action enterAction = new AbstractAction()
		{
			private static final long serialVersionUID = 4248565786079247230L;
			
			public void actionPerformed(final ActionEvent arg0)
			{
				logonPressed();
			}
		};
		final String key = "logon.through.enter.key";
		getActionMap().put(key, enterAction);
		getActionMap().put(key, enterAction);
		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), key);
	}
	
	private void logonPressed()
	{
		final String userName = m_userName.getText();
		final boolean anonymous = m_anonymous.isSelected();
		if (DBUserController.validateUserName(userName) != null)
		{
			JOptionPane.showMessageDialog(this, DBUserController.validateUserName(userName), "Invalid Username", JOptionPane.ERROR_MESSAGE);
			return;
		}
		else if (m_password.getPassword().length == 0 && !anonymous)
		{
			JOptionPane.showMessageDialog(LoginPanel.this, "You must enter a password", "No Password", JOptionPane.ERROR_MESSAGE);
			return;
		}
		else if (m_password.getPassword().length < 3 && !anonymous)
		{
			JOptionPane.showMessageDialog(LoginPanel.this, "Passwords must be at least three characters long", "Invalid password", JOptionPane.ERROR_MESSAGE);
			return;
		}
		m_returnValue = ReturnValue.LOGON;
		m_dialog.setVisible(false);
	}
	
	public static void storePrefs(final String userName, final boolean anonymous)
	{
		final Preferences prefs = Preferences.userNodeForPackage(LoginPanel.class);
		prefs.put(LAST_LOGIN_NAME_PREF, userName);
		prefs.putBoolean(ANONYMOUS_LOGIN_PREF, anonymous);
		try
		{
			prefs.flush();
		} catch (final BackingStoreException e)
		{
			// not a big deal
			s_logger.warning(e.getMessage());
		}
	}
	
	private void setWidgetActivation()
	{
		if (!m_anonymous.isSelected())
		{
			m_password.setEnabled(true);
			m_password.setBackground(m_userName.getBackground());
		}
		else
		{
			m_password.setEnabled(false);
			m_password.setBackground(this.getBackground());
		}
	}
	
	public static void main(final String[] args)
	{
		GameRunner2.setupLookAndFeel();
		final JDialog d = new JDialog();
		d.add(new LoginPanel());
		d.setDefaultCloseOperation(JDialog.EXIT_ON_CLOSE);
		d.pack();
		d.setVisible(true);
	}
	
	public boolean isAnonymous()
	{
		return m_anonymous.isSelected();
	}
	
	public String getUserName()
	{
		return m_userName.getText();
	}
	
	@SuppressWarnings("deprecation")
	public String getPassword()
	{
		return m_password.getText();
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
}
