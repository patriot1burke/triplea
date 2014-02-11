package games.strategy.engine.lobby.client.ui;

import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.lobby.client.login.CreateUpdateAccountPanel;
import games.strategy.engine.lobby.server.IModeratorController;
import games.strategy.engine.lobby.server.IUserManager;
import games.strategy.engine.lobby.server.ModeratorController;
import games.strategy.engine.lobby.server.userDB.DBUser;
import games.strategy.engine.lobby.server.userDB.DBUserController;
import games.strategy.net.DesktopUtilityBrowserLauncher;
import games.strategy.net.INode;
import games.strategy.net.Node;
import games.strategy.sound.SoundOptions;
import games.strategy.sound.SoundPath;
import games.strategy.util.MD5Crypt;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class LobbyMenu extends JMenuBar
{
	private static final long serialVersionUID = 4980621864542042057L;
	private final LobbyFrame m_frame;
	
	public LobbyMenu(final LobbyFrame frame)
	{
		m_frame = frame;
		// file only has one value
		// and on mac it is in the apple menu
		if (!GameRunner.isMac())
			createFileMenu(this);
		else
			MacLobbyWrapper.registerMacShutdownHandler(m_frame);
		createAccountMenu(this);
		if (m_frame.getLobbyClient().isAdmin())
			createAdminMenu(this);
		createSettingsMenu(this);
		createHelpMenu(this);
	}
	
	private void createAccountMenu(final LobbyMenu menuBar)
	{
		final JMenu account = new JMenu("Account");
		menuBar.add(account);
		addUpdateAccountMenu(account);
	}
	
	private void createAdminMenu(final LobbyMenu menuBar)
	{
		final JMenu powerUser = new JMenu("Admin");
		menuBar.add(powerUser);
		createDiagnosticsMenu(powerUser);
		createToolboxMenu(powerUser);
	}
	
	private void createDiagnosticsMenu(final JMenu menuBar)
	{
		final JMenu diagnostics = new JMenu("Diagnostics");
		menuBar.add(diagnostics);
		addDisplayPlayersInformationMenu(diagnostics);
	}
	
	private void createToolboxMenu(final JMenu menuBar)
	{
		final JMenu toolbox = new JMenu("Toolbox");
		menuBar.add(toolbox);
		addBanUsernameMenu(toolbox);
		addBanIPAddressMenu(toolbox);
		addBanMacAddressMenu(toolbox);
		addUnbanUsernameMenu(toolbox);
		addUnbanIPAddressMenu(toolbox);
		addUnbanMacAddressMenu(toolbox);
	}
	
	private void addDisplayPlayersInformationMenu(final JMenu parentMenu)
	{
		final JMenuItem revive = new JMenuItem("Display Players Information");
		revive.setEnabled(true);
		revive.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				final Runnable runner = new Runnable()
				{
					public void run()
					{
						final IModeratorController controller = (IModeratorController) m_frame.getLobbyClient().getMessengers().getRemoteMessenger()
									.getRemote(ModeratorController.getModeratorControllerName());
						final StringBuilder builder = new StringBuilder();
						builder.append("Online Players:\r\n\r\n");
						for (final INode player : m_frame.GetChatMessagePanel().getChat().GetOnlinePlayers())
						{
							builder.append(controller.getInformationOn(player)).append("\r\n\r\n");
						}
						builder.append("Players That Have Left (Last 10):\r\n\r\n");
						for (final INode player : m_frame.GetChatMessagePanel().getChat().GetPlayersThatLeft_Last10())
						{
							builder.append(controller.getInformationOn(player)).append("\r\n\r\n");
						}
						final Runnable componentCreation = new Runnable()
						{
							public void run()
							{
								final JDialog dialog = new JDialog(m_frame, "Players Information");
								final JTextArea label = new JTextArea(builder.toString());
								label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
								label.setEditable(false);
								label.setAutoscrolls(true);
								label.setLineWrap(false);
								label.setFocusable(true);
								label.setWrapStyleWord(true);
								label.setLocation(0, 0);
								dialog.setBackground(label.getBackground());
								dialog.setLayout(new BorderLayout());
								final JScrollPane pane = new JScrollPane();
								pane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
								pane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
								pane.setViewportView(label);
								dialog.add(pane, BorderLayout.CENTER);
								final JButton button = new JButton(new AbstractAction()
								{
									private static final long serialVersionUID = 823636925655070882L;
									
									public void actionPerformed(final ActionEvent e)
									{
										dialog.dispose();
									}
								});
								button.setText("Close");
								button.setMinimumSize(new Dimension(100, 30));
								dialog.add(button, BorderLayout.SOUTH);
								dialog.setMinimumSize(new Dimension(500, 300));
								dialog.setSize(new Dimension(800, 600));
								dialog.setResizable(true);
								dialog.setLocationRelativeTo(m_frame);
								dialog.setDefaultCloseOperation(2);
								dialog.setVisible(true);
							}
						};
						SwingUtilities.invokeLater(componentCreation);
					}
				};
				final Thread thread = new Thread(runner);
				thread.start();
			}
		});
		parentMenu.add(revive);
	}
	
	private void addBanUsernameMenu(final JMenu parentMenu)
	{
		final JMenuItem item = new JMenuItem("Ban Username");
		item.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				final String name = JOptionPane.showInputDialog(null,
							"Enter the username that you want to ban from the lobby.\r\n\r\nNote that this ban is effective on any username, registered or anonymous, online or offline.", "");
				if (name == null || name.length() < 1)
					return;
				if (DBUserController.validateUserName(name) != null)
				{
					if (JOptionPane.showConfirmDialog(m_frame, "The username you entered is invalid. Do you want to ban it anyhow?", "Invalid Username", JOptionPane.YES_NO_CANCEL_OPTION) != JOptionPane.YES_OPTION)
						return;
				}
				final long ticks = requestTimespanSupplication();
				final long expire = System.currentTimeMillis() + ticks;
				final IModeratorController controller = (IModeratorController) m_frame.getLobbyClient().getMessengers().getRemoteMessenger()
							.getRemote(ModeratorController.getModeratorControllerName());
				try
				{
					controller.banUsername(new Node(name, InetAddress.getByName("0.0.0.0"), 0), new Date(expire));
				} catch (final UnknownHostException ex)
				{
				}
			}
		});
		item.setEnabled(true);
		parentMenu.add(item);
	}
	
	private void addBanIPAddressMenu(final JMenu parentMenu)
	{
		final JMenuItem item = new JMenuItem("Ban IP Address");
		item.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				final String ip = JOptionPane.showInputDialog(null, "Enter the IP Address that you want to ban from the lobby.\r\n\r\nIP Addresses should be entered in this format: 192.168.1.0", "");
				if (ip == null || ip.length() < 1)
					return;
				final long ticks = requestTimespanSupplication();
				final long expire = System.currentTimeMillis() + ticks;
				final IModeratorController controller = (IModeratorController) m_frame.getLobbyClient().getMessengers().getRemoteMessenger()
							.getRemote(ModeratorController.getModeratorControllerName());
				try
				{
					controller.banIp(new Node("None (Admin menu originated ban)", InetAddress.getByName(ip), 0), new Date(expire));
				} catch (final UnknownHostException ex)
				{
				}
			}
		});
		item.setEnabled(true);
		parentMenu.add(item);
	}
	
	private void addBanMacAddressMenu(final JMenu parentMenu)
	{
		final JMenuItem item = new JMenuItem("Ban Hashed Mac Address");
		item.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				final String mac = JOptionPane.showInputDialog(null,
							"Enter the hashed Mac Address that you want to ban from the lobby.\r\n\r\nHashed Mac Addresses should be entered in this format: $1$MH$345ntXD4G3AKpAeHZdaGe3", "");
				if (mac == null || mac.length() < 1)
					return;
				final String prefix = MD5Crypt.MAGIC + "MH$";
				final String error;
				if (mac.length() != 28)
					error = "Must be 28 characters long";
				else if (!mac.startsWith(prefix))
					error = "Must start with: " + prefix;
				else if (!mac.matches("[0-9a-zA-Z$./]+"))
					error = "Must use only these characters: 0-9a-zA-Z$./";
				else
					error = null;
				if (error != null)
				{
					if (JOptionPane.showConfirmDialog(m_frame, "The hashed Mac Address you entered is invalid (" + error + "). Do you want to ban it anyhow?",
								"Invalid Hashed Mac", JOptionPane.YES_NO_CANCEL_OPTION) != JOptionPane.YES_OPTION)
						return;
				}
				final long ticks = requestTimespanSupplication();
				final long expire = System.currentTimeMillis() + ticks;
				final IModeratorController controller = (IModeratorController) m_frame.getLobbyClient().getMessengers().getRemoteMessenger()
							.getRemote(ModeratorController.getModeratorControllerName());
				try
				{
					controller.banMac(new Node("None (Admin menu originated ban)", InetAddress.getByName("0.0.0.0"), 0), mac, new Date(expire));
				} catch (final UnknownHostException ex)
				{
				}
			}
		});
		item.setEnabled(true);
		parentMenu.add(item);
	}
	
	private void addUnbanUsernameMenu(final JMenu parentMenu)
	{
		final JMenuItem item = new JMenuItem("Unban Username");
		item.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				final String name = JOptionPane.showInputDialog(null, "Enter the username that you want to unban from the lobby.", "");
				if (name == null || name.length() < 1)
					return;
				if (DBUserController.validateUserName(name) != null)
				{
					if (JOptionPane.showConfirmDialog(m_frame, "The username you entered is invalid. Do you want to ban it anyhow?", "Invalid Username", JOptionPane.YES_NO_CANCEL_OPTION) != JOptionPane.YES_OPTION)
						return;
				}
				final IModeratorController controller = (IModeratorController) m_frame.getLobbyClient().getMessengers().getRemoteMessenger()
							.getRemote(ModeratorController.getModeratorControllerName());
				try
				{
					controller.banUsername(new Node(name, InetAddress.getByName("0.0.0.0"), 0), new Date(0));
				} catch (final UnknownHostException ex)
				{
				}
			}
		});
		item.setEnabled(true);
		parentMenu.add(item);
	}
	
	private void addUnbanIPAddressMenu(final JMenu parentMenu)
	{
		final JMenuItem item = new JMenuItem("Unban IP Address");
		item.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				final String ip = JOptionPane
							.showInputDialog(null, "Enter the IP Address that you want to unban from the lobby.\r\n\r\nIP Addresses should be entered in this format: 192.168.1.0", "");
				if (ip == null || ip.length() < 1)
					return;
				final IModeratorController controller = (IModeratorController) m_frame.getLobbyClient().getMessengers().getRemoteMessenger()
							.getRemote(ModeratorController.getModeratorControllerName());
				try
				{
					controller.banIp(new Node("None (Admin menu originated unban)", InetAddress.getByName(ip), 0), new Date(0));
				} catch (final UnknownHostException ex)
				{
				}
			}
		});
		item.setEnabled(true);
		parentMenu.add(item);
	}
	
	private void addUnbanMacAddressMenu(final JMenu parentMenu)
	{
		final JMenuItem item = new JMenuItem("Unban Hashed Mac Address");
		item.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				final String mac = JOptionPane.showInputDialog(null,
							"Enter the hashed Mac Address that you want to unban from the lobby.\r\n\r\nHashed Mac Addresses should be entered in this format: $1$MH$345ntXD4G3AKpAeHZdaGe3", "");
				if (mac == null || mac.length() < 1)
					return;
				final String prefix = MD5Crypt.MAGIC + "MH$";
				final String error;
				if (mac.length() != 28)
					error = "Must be 28 characters long";
				else if (!mac.startsWith(prefix))
					error = "Must start with: " + prefix;
				else if (!mac.matches("[0-9a-zA-Z$./]+"))
					error = "Must use only these characters: 0-9a-zA-Z$./";
				else
					error = null;
				if (error != null)
				{
					if (JOptionPane.showConfirmDialog(m_frame, "The hashed Mac Address you entered is invalid (" + error + "). Do you want to ban it anyhow?",
								"Invalid Hashed Mac", JOptionPane.YES_NO_CANCEL_OPTION) != JOptionPane.YES_OPTION)
						return;
				}
				final IModeratorController controller = (IModeratorController) m_frame.getLobbyClient().getMessengers().getRemoteMessenger()
							.getRemote(ModeratorController.getModeratorControllerName());
				try
				{
					controller.banMac(new Node("None (Admin menu originated unban)", InetAddress.getByName("0.0.0.0"), 0), mac, new Date(0));
				} catch (final UnknownHostException ex)
				{
				}
			}
		});
		item.setEnabled(true);
		parentMenu.add(item);
	}
	
	private long requestTimespanSupplication()
	{
		final List<String> timeUnits = new ArrayList<String>();
		timeUnits.add("Minute");
		timeUnits.add("Hour");
		timeUnits.add("Day");
		timeUnits.add("Week");
		timeUnits.add("Month");
		timeUnits.add("Year");
		timeUnits.add("Forever");
		final int result = JOptionPane.showOptionDialog(m_frame, "Select the unit of measurement: ", "Select Timespan Unit", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
					timeUnits.toArray(), timeUnits.toArray()[3]);
		if (result < 0)
			return -1;
		final String selectedTimeUnit = (String) timeUnits.toArray()[result];
		if (selectedTimeUnit.equals("Forever"))
			return Long.MAX_VALUE;
		final String stringr = JOptionPane.showInputDialog(m_frame, "Now please enter the length of time: (In " + selectedTimeUnit + "s) ", 1);
		if (stringr == null)
			return -1;
		final long result2 = Long.parseLong(stringr);
		if (result2 < 0)
			return -1;
		long ticks = 0;
		if (selectedTimeUnit.equals("Minute"))
			ticks = result2 * 1000 * 60;
		else if (selectedTimeUnit.equals("Hour"))
			ticks = result2 * 1000 * 60 * 60;
		else if (selectedTimeUnit.equals("Day"))
			ticks = result2 * 1000 * 60 * 60 * 24;
		else if (selectedTimeUnit.equals("Week"))
			ticks = result2 * 1000 * 60 * 60 * 24 * 7;
		else if (selectedTimeUnit.equals("Month"))
			ticks = result2 * 1000 * 60 * 60 * 24 * 30;
		else if (selectedTimeUnit.equals("Year"))
			ticks = result2 * 1000 * 60 * 60 * 24 * 365;
		return ticks;
	}
	
	private void createSettingsMenu(final LobbyMenu menuBar)
	{
		final JMenu settings = new JMenu("Settings");
		menuBar.add(settings);
		SoundOptions.addGlobalSoundSwitchMenu(settings);
		SoundOptions.addToMenu(settings, SoundPath.SoundType.TRIPLEA);
		addChatTimeMenu(settings);
	}
	
	private void createHelpMenu(final LobbyMenu menuBar)
	{
		final JMenu help = new JMenu("Help");
		menuBar.add(help);
		addHelpMenu(help);
	}
	
	/**
	 * @param parentMenu
	 */
	private void addHelpMenu(final JMenu parentMenu)
	{
		final JMenuItem hostingLink = new JMenuItem("How to Host...");
		final JMenuItem mapLink = new JMenuItem("Install Maps...");
		final JMenuItem bugReport = new JMenuItem("Bug Report...");
		final JMenuItem lobbyRules = new JMenuItem("Lobby Rules...");
		final JMenuItem warClub = new JMenuItem("War Club & Ladder...");
		final JMenuItem devForum = new JMenuItem("Developer Forum...");
		final JMenuItem donateLink = new JMenuItem("Donate...");
		final JMenuItem guidesLink = new JMenuItem("Guides...");
		hostingLink.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				try
				{
					DesktopUtilityBrowserLauncher.openURL("http://tripleadev.1671093.n2.nabble.com/Download-Maps-Links-Hosting-Games-General-Information-tp4074312p4085700.html");
				} catch (final Exception e1)
				{
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});
		mapLink.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				try
				{
					DesktopUtilityBrowserLauncher.openURL("http://tripleadev.1671093.n2.nabble.com/Download-Maps-Links-Hosting-Games-General-Information-tp4074312.html");
				} catch (final Exception e1)
				{
					e1.printStackTrace();
				}
			}
		});
		bugReport.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				try
				{
					DesktopUtilityBrowserLauncher.openURL("https://sourceforge.net/p/triplea/_list/tickets");
				} catch (final Exception e1)
				{
					e1.printStackTrace();
				}
			}
		});
		lobbyRules.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				try
				{
					DesktopUtilityBrowserLauncher.openURL("http://www.tripleawarclub.org/modules/newbb/viewtopic.php?topic_id=100&forum=1");
				} catch (final Exception e1)
				{
					e1.printStackTrace();
				}
			}
		});
		warClub.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				try
				{
					DesktopUtilityBrowserLauncher.openURL("http://www.tripleawarclub.org/");
				} catch (final Exception e1)
				{
					e1.printStackTrace();
				}
			}
		});
		devForum.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				try
				{
					DesktopUtilityBrowserLauncher.openURL("http://triplea.sourceforge.net/mywiki/Forum");
				} catch (final Exception e1)
				{
					e1.printStackTrace();
				}
			}
		});
		donateLink.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				try
				{
					DesktopUtilityBrowserLauncher.openURL("https://sourceforge.net/donate/index.php?group_id=44492");
				} catch (final Exception e1)
				{
					e1.printStackTrace();
				}
			}
		});
		guidesLink.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				try
				{
					DesktopUtilityBrowserLauncher.openURL("http://triplea.sourceforge.net/mywiki/Guides");
				} catch (final Exception e1)
				{
					e1.printStackTrace();
				}
			}
		});
		parentMenu.add(hostingLink);
		parentMenu.add(mapLink);
		parentMenu.add(bugReport);
		parentMenu.add(lobbyRules);
		parentMenu.add(warClub);
		parentMenu.add(devForum);
		parentMenu.add(donateLink);
		parentMenu.add(guidesLink);
	}
	
	private void addChatTimeMenu(final JMenu parentMenu)
	{
		final JCheckBoxMenuItem chatTimeBox = new JCheckBoxMenuItem("Show Chat Times");
		chatTimeBox.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				m_frame.setShowChatTime(chatTimeBox.isSelected());
			}
		});
		chatTimeBox.setSelected(true);
		parentMenu.add(chatTimeBox);
	}
	
	private void addUpdateAccountMenu(final JMenu account)
	{
		final JMenuItem update = new JMenuItem("Update Account...");
		// only if we are not anonymous login
		update.setEnabled(!m_frame.getLobbyClient().isAnonymousLogin());
		update.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				updateAccountDetails();
			}
		});
		account.add(update);
	}
	
	private void updateAccountDetails()
	{
		final IUserManager manager = (IUserManager) m_frame.getLobbyClient().getRemoteMessenger().getRemote(IUserManager.USER_MANAGER);
		final DBUser user = manager.getUserInfo(m_frame.getLobbyClient().getMessenger().getLocalNode().getName());
		if (user == null)
		{
			JOptionPane.showMessageDialog(this, "No user info found", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		final CreateUpdateAccountPanel panel = CreateUpdateAccountPanel.newUpdatePanel(user);
		final CreateUpdateAccountPanel.ReturnValue rVal = panel.show(m_frame);
		if (rVal == CreateUpdateAccountPanel.ReturnValue.CANCEL)
			return;
		final String error = manager.updateUser(panel.getUserName(), panel.getEmail(), MD5Crypt.crypt(panel.getPassword()));
		if (error != null)
		{
			JOptionPane.showMessageDialog(this, error, "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private void createFileMenu(final JMenuBar menuBar)
	{
		final JMenu fileMenu = new JMenu("File");
		menuBar.add(fileMenu);
		addExitMenu(fileMenu);
	}
	
	private void addExitMenu(final JMenu parentMenu)
	{
		final boolean isMac = GameRunner.isMac();
		// Mac OS X automatically creates a Quit menu item under the TripleA menu,
		// so all we need to do is register that menu item with triplea's shutdown mechanism
		if (!isMac)
		{ // On non-Mac operating systems, we need to manually create an Exit menu item
			final JMenuItem menuFileExit = new JMenuItem(new AbstractAction("Exit")
			{
				private static final long serialVersionUID = 7173511775817785866L;
				
				public void actionPerformed(final ActionEvent e)
				{
					m_frame.shutdown();
				}
			});
			parentMenu.add(menuFileExit);
		}
	}
}
