/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package games.strategy.triplea.ui;

import games.strategy.common.ui.BasicGameMenuBar;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.export.GameDataExporter;
import games.strategy.engine.data.properties.PropertiesUI;
import games.strategy.engine.framework.ClientGame;
import games.strategy.engine.framework.GameDataUtils;
import games.strategy.engine.framework.GameRunner2;
import games.strategy.engine.framework.startup.ui.MainFrame;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.engine.history.HistoryNode;
import games.strategy.engine.history.Round;
import games.strategy.engine.history.Step;
import games.strategy.engine.random.IRandomStats;
import games.strategy.engine.random.RandomStatsDetails;
import games.strategy.engine.sound.ClipPlayer;
import games.strategy.engine.stats.IStat;
import games.strategy.triplea.Dynamix_AI.Dynamix_AI;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.EndRoundDelegate;
import games.strategy.triplea.image.TileImageFactory;
import games.strategy.triplea.oddsCalculator.ta.OddsCalculatorDialog;
import games.strategy.triplea.printgenerator.SetupFrame;
import games.strategy.ui.IntTextField;
import games.strategy.util.EventThreadJOptionPane;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.LookAndFeel;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * 
 * Main menu for the triplea frame.
 * 
 * @author sgb
 * 
 */
@SuppressWarnings("serial")
public class TripleaMenu extends BasicGameMenuBar<TripleAFrame>
{
	private JCheckBoxMenuItem showMapDetails;
	private JCheckBoxMenuItem showMapBlends;
	
	TripleaMenu(TripleAFrame frame)
	{
		super(frame);
		setWidgetActivation();
	}
	
	private UIContext getUIContext()
	{
		return m_frame.getUIContext();
	}
	
	@Override
	protected void addGameSpecificHelpMenus(JMenu helpMenu)
	{
		addMoveHelpMenu(helpMenu);
	}
	
	/**
	 * @param parentMenu
	 */
	private void addMoveHelpMenu(JMenu parentMenu)
	{
		parentMenu.add(new AbstractAction("Movement help...")
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// html formatted string
				String hints =
							"<b> Selecting Units</b><br><br>" +
										"Left click on a unit stack to select 1 unit.<br>" +
										"CTRL-Left click on a unit stack to select all units in the stack.<br>" +
										"Shift-Left click on a unit to select all units in the territory.<br>" +
										"Right click on a unit stack to un select one unit in the stack.<br>" +
										"CTRL-Right click on a unit stack to un select all units in the stack.<br>" +
										"Right click somewhere not on a unit stack to un select the last selected unit.<br>" +
										"CTRL-Right click somewhere not on a unit stack to un select all units.<br>" +
										"<br>" +
										"<b> Selecting Territories</b><br><br>" +
										"After selecting units Left click on a territory to move units to that territory.<br>" +
										"CTRL-Left click on a territory to select the territory as a way point.<br><br>";
				JEditorPane editorPane = new JEditorPane();
				editorPane.setEditable(false);
				editorPane.setContentType("text/html");
				editorPane.setText(hints);
				
				JScrollPane scroll = new JScrollPane(editorPane);
				
				JOptionPane.showMessageDialog(m_frame, scroll, "Movement Help", JOptionPane.PLAIN_MESSAGE);
			}
		});
	}
	
	@Override
	protected void createGameSpecificMenus(JMenuBar menuBar)
	{
		createViewMenu(menuBar);
		createGameMenu(menuBar);
		createExportMenu(menuBar);
	}
	
	/**
	 * @param menuBar
	 */
	private void createGameMenu(JMenuBar menuBar)
	{
		JMenu menuGame = new JMenu("Game");
		menuBar.add(menuGame);
		
		addEditMode(menuGame);
		menuGame.add(m_frame.getShowGameAction());
		menuGame.add(m_frame.getShowHistoryAction());
		menuGame.add(m_frame.getShowMapOnlyAction());
		
		addShowVerifiedDice(menuGame);
		addEnableSound(menuGame);
		
		menuGame.addSeparator();
		addGameOptionsMenu(menuGame);
		addPoliticsMenu(menuGame);
		addShowEnemyCasualties(menuGame);
		addShowAIBattles(menuGame);
		addChangeDynamixAISettings(menuGame);
		addAISleepDuration(menuGame);
		addShowDiceStats(menuGame);
		addRollDice(menuGame);
		addBattleCalculatorMenu(menuGame);
		
	}
	
	private void createExportMenu(JMenuBar menuBar)
	{
		JMenu menuGame = new JMenu("Export");
		menuBar.add(menuGame);
		
		addExportXML(menuGame);
		addExportStats(menuGame);
		addExportStatsFull(menuGame);
		addExportSetupCharts(menuGame);
		addSaveScreenshot(menuGame);
		
	}
	
	/**
	 * @param menuBar
	 */
	private void createViewMenu(JMenuBar menuBar)
	{
		JMenu menuView = new JMenu("View");
		menuBar.add(menuView);
		
		addZoomMenu(menuView);
		addUnitSizeMenu(menuView);
		addShowUnits(menuView);
		addLockMap(menuView);
		addMapSkinsMenu(menuView);
		addShowMapDetails(menuView);
		addShowMapBlends(menuView);
		addChatTimeMenu(menuView);
		addShowCommentLog(menuView);
		// The menuItem to turn TabbedProduction on or off
		addTabbedProduction(menuView);
		addShowGameUuid(menuView);
		addSetLookAndFeel(menuView);
	}
	
	private boolean isJavaGreatThan5()
	{
		String version = System.getProperties().getProperty("java.version");
		return version.indexOf("1.5") == -1;
	}
	
	private void addSetLookAndFeel(JMenu menuView)
	{
		menuView.add(new AbstractAction("Set Look and Feel...")
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Map<String, String> lookAndFeels = new LinkedHashMap<String, String>();
				lookAndFeels.put("Default", UIManager.getSystemLookAndFeelClassName());
				lookAndFeels.put("Metal", MetalLookAndFeel.class.getName());
				lookAndFeels.put("Platform Independent", UIManager.getCrossPlatformLookAndFeelClassName());
				
				if (isJavaGreatThan5())
				{
					
					try
					{
						List<String> substanceLooks = Arrays.asList(new String[] {
									"org.jvnet.substance.skin.SubstanceAutumnLookAndFeel",

									"org.jvnet.substance.skin.SubstanceChallengerDeepLookAndFeel",
									"org.jvnet.substance.skin.SubstanceCremeCoffeeLookAndFeel",
									"org.jvnet.substance.skin.SubstanceCremeLookAndFeel",
									"org.jvnet.substance.skin.SubstanceDustCoffeeLookAndFeel",
									"org.jvnet.substance.skin.SubstanceDustLookAndFeel",
									"org.jvnet.substance.skin.SubstanceEmeraldDuskLookAndFeel",
									"org.jvnet.substance.skin.SubstanceMagmaLookAndFeel",
									"org.jvnet.substance.skin.SubstanceMistAquaLookAndFeel",
									"org.jvnet.substance.skin.SubstanceModerateLookAndFeel",
									"org.jvnet.substance.skin.SubstanceNebulaLookAndFeel",
									"org.jvnet.substance.skin.SubstanceRavenGraphiteGlassLookAndFeel",
									"org.jvnet.substance.skin.SubstanceRavenGraphiteLookAndFeel",
									"org.jvnet.substance.skin.SubstanceRavenLookAndFeel",
									"org.jvnet.substance.skin.SubstanceTwilightLookAndFeel",
						});
						
						for (String s : substanceLooks)
						{
							@SuppressWarnings("rawtypes")
							Class c = Class.forName(s);
							LookAndFeel lf = (LookAndFeel) c.newInstance();
							lookAndFeels.put(lf.getName(), s);
						}
					} catch (Exception t)
					{
						t.printStackTrace();
					}
					
				}
				
				JList list = new JList(new Vector<String>(lookAndFeels.keySet()));
				
				String currentKey = null;
				for (String s : lookAndFeels.keySet())
				{
					String currentName = UIManager.getLookAndFeel().getClass().getName();
					if (lookAndFeels.get(s).equals(currentName))
					{
						currentKey = s;
						break;
					}
				}
				list.setSelectedValue(currentKey, false);
				
				if (JOptionPane.showConfirmDialog(m_frame, list) == JOptionPane.OK_OPTION)
				{
					String selectedValue = (String) list.getSelectedValue();
					if (selectedValue == null)
					{
						return;
					}
					if (selectedValue.equals(currentKey))
					{
						return;
					}
					
					GameRunner2.setDefaultLookAndFeel(lookAndFeels.get(selectedValue));
					EventThreadJOptionPane.showMessageDialog(m_frame, "The look and feel will update when you restart TripleA");
				}
			}
		});
	}
	
	private void addShowGameUuid(JMenu menuView)
	{
		menuView.add(new AbstractAction("Game UUID...")
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				String id = (String) getData().getProperties().get(GameData.GAME_UUID);
				JTextField text = new JTextField();
				text.setText(id);
				JPanel panel = new JPanel();
				panel.setLayout(new GridBagLayout());
				panel.add(new JLabel("Game UUID:"), new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
				panel.add(text, new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
				
				JOptionPane.showOptionDialog(JOptionPane.getFrameForComponent(TripleaMenu.this), panel, "Game UUID", JOptionPane.OK_OPTION, JOptionPane.INFORMATION_MESSAGE, null,
							new String[] { "OK" }, "OK");
				
			}
		});
		
	}
	
	private void addChatTimeMenu(JMenu parentMenu)
	{
		final JCheckBoxMenuItem chatTimeBox = new JCheckBoxMenuItem("Show Chat Times");
		
		chatTimeBox.addActionListener(new ActionListener()
		{
			
			@Override
			public void actionPerformed(ActionEvent e)
			{
				m_frame.setShowChatTime(chatTimeBox.isSelected());
				
			}
			
		});
		
		chatTimeBox.setSelected(false);
		parentMenu.add(chatTimeBox);
		
		chatTimeBox.setEnabled(MainFrame.getInstance().getChat() != null);
		
	}
	
	/**
	 * @param parentMenu
	 */
	private void addEditMode(JMenu parentMenu)
	{
		JCheckBoxMenuItem editMode = new JCheckBoxMenuItem("Enable Edit Mode");
		editMode.setModel(m_frame.getEditModeButtonModel());
		
		parentMenu.add(editMode);
	}
	
	private void addZoomMenu(JMenu menuGame)
	{
		Action mapZoom = new AbstractAction("Map Zoom...")
		{
			
			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				final SpinnerNumberModel model = new SpinnerNumberModel();
				model.setMaximum(100);
				model.setMinimum(15);
				model.setStepSize(1);
				model.setValue((int) (m_frame.getMapPanel().getScale() * 100));
				JSpinner spinner = new JSpinner(model);
				
				JPanel panel = new JPanel();
				panel.setLayout(new BorderLayout());
				panel.add(new JLabel("Choose Map Scale Percentage"), BorderLayout.NORTH);
				panel.add(spinner, BorderLayout.CENTER);
				
				JPanel buttons = new JPanel();
				JButton fitWidth = new JButton("Fit Width");
				buttons.add(fitWidth);
				JButton fitHeight = new JButton("Fit Height");
				buttons.add(fitHeight);
				JButton reset = new JButton("Reset");
				buttons.add(reset);
				
				panel.add(buttons, BorderLayout.SOUTH);
				
				fitWidth.addActionListener(new ActionListener()
				{
					
					@Override
					public void actionPerformed(ActionEvent e)
					{
						double screenWidth = m_frame.getMapPanel().getWidth();
						double mapWidth = m_frame.getMapPanel().getImageWidth();
						double ratio = screenWidth / mapWidth;
						ratio = Math.max(0.15, ratio);
						ratio = Math.min(1, ratio);
						
						model.setValue((int) (ratio * 100));
					}
					
				});
				
				fitHeight.addActionListener(new ActionListener()
				{
					
					@Override
					public void actionPerformed(ActionEvent e)
					{
						double screenHeight = m_frame.getMapPanel().getHeight();
						double mapHeight = m_frame.getMapPanel().getImageHeight();
						double ratio = screenHeight / mapHeight;
						ratio = Math.max(0.15, ratio);
						model.setValue((int) (ratio * 100));
						
					}
					
				});
				
				reset.addActionListener(new ActionListener()
				{
					@Override
					public void actionPerformed(ActionEvent e)
					{
						model.setValue(100);
					}
				});
				
				int result = JOptionPane.showOptionDialog(m_frame, panel, "Choose Map Scale", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, new String[] { "OK", "Cancel" }, 0);
				if (result != 0)
					return;
				
				Number value = (Number) model.getValue();
				m_frame.setScale(value.doubleValue());
			}
			
		};
		
		menuGame.add(mapZoom);
		
	}
	
	/**
	 * @param parentMenu
	 */
	private void addShowVerifiedDice(JMenu parentMenu)
	{
		Action showVerifiedDice = new AbstractAction("Show Verified Dice..")
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				new VerifiedRandomNumbersDialog(m_frame.getRootPane()).setVisible(true);
			}
		};
		if (getGame() instanceof ClientGame)
			parentMenu.add(showVerifiedDice);
	}
	
	/**
	 * @param parentMenu
	 */
	private void addSaveScreenshot(JMenu parentMenu)
	{
		parentMenu.add(m_frame.getSaveScreenshotAction());
	}
	
	/**
	 * @param parentMenu
	 */
	private void addShowCommentLog(JMenu parentMenu)
	{
		JCheckBoxMenuItem showCommentLog = new JCheckBoxMenuItem("Show Comment Log");
		showCommentLog.setModel(m_frame.getShowCommentLogButtonModel());
		
		parentMenu.add(showCommentLog);
	}
	
	/**
	 * @param parentMenu
	 */
	private void addShowEnemyCasualties(JMenu parentMenu)
	{
		final JCheckBoxMenuItem showEnemyCasualties = new JCheckBoxMenuItem("Confirm Enemy Casualties");
		showEnemyCasualties.setSelected(BattleDisplay.getShowEnemyCasualtyNotification());
		showEnemyCasualties.addActionListener(new AbstractAction()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				BattleDisplay.setShowEnemyCasualtyNotification(showEnemyCasualties.isSelected());
			}
		});
		
		parentMenu.add(showEnemyCasualties);
	}
	
	private void addTabbedProduction(JMenu parentMenu)
	{
		final JCheckBoxMenuItem tabbedProduction = new JCheckBoxMenuItem("Show Production Tabs");
		tabbedProduction.setSelected(PurchasePanel.isTabbedProduction());
		tabbedProduction.addActionListener(new AbstractAction()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				PurchasePanel.setTabbedProduction(tabbedProduction.isSelected());
			}
			
		});
		
		parentMenu.add(tabbedProduction);
	}
	
	/**
	 * @param menuGame
	 */
	private void addGameOptionsMenu(JMenu menuGame)
	{
		if (!getGame().getData().getProperties().getEditableProperties().isEmpty())
		{
			AbstractAction optionsAction = new AbstractAction("View Game Options...")
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					PropertiesUI ui = new PropertiesUI(getGame().getData().getProperties(), false);
					JOptionPane.showMessageDialog(m_frame, ui, "Game options", JOptionPane.PLAIN_MESSAGE);
				}
			};
			
			menuGame.add(optionsAction);
			
		}
	}
	
	/**
	 * Add a Politics Panel button to the game menu, this panel will show the
	 * current political landscape as a reference, no actions on this panel.
	 * 
	 * @param menuGame
	 */
	private void addPoliticsMenu(JMenu menuGame)
	{
		AbstractAction politicsAction = new AbstractAction("Show Politics Panel")
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				PoliticalStateOverview ui = new PoliticalStateOverview(getData(),getUIContext());
				JOptionPane.showMessageDialog(m_frame,ui,"Politics Panel", JOptionPane.PLAIN_MESSAGE);
			}
		};
		menuGame.add(politicsAction);

	}
	
	/**
	 * @param parentMenu
	 */
	private void addEnableSound(JMenu parentMenu)
	{
		final JCheckBoxMenuItem soundCheckBox = new JCheckBoxMenuItem("Enable Sound");
		
		soundCheckBox.setSelected(!ClipPlayer.getInstance().getBeSilent());
		// temporarily disable sound
		
		soundCheckBox.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				ClipPlayer.getInstance().setBeSilent(!soundCheckBox.isSelected());
			}
		});
		parentMenu.add(soundCheckBox);
	}
	
	private void addShowUnits(JMenu parentMenu)
	{
		final JCheckBoxMenuItem showUnitsBox = new JCheckBoxMenuItem("Show Units");
		showUnitsBox.setSelected(true);
		showUnitsBox.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				boolean tfselected = showUnitsBox.isSelected();
				getUIContext().setShowUnits(tfselected);
				// games.strategy.triplea.ui.screen.TileManager.store(tfselected);
				m_frame.getMapPanel().resetMap();
			}
		});
		parentMenu.add(showUnitsBox);
	}
	
	private void addLockMap(JMenu parentMenu)
	{
		final JCheckBoxMenuItem lockMapBox = new JCheckBoxMenuItem("Lock Map");
		lockMapBox.setSelected(getUIContext().getLockMap());
		lockMapBox.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				getUIContext().setLockMap(lockMapBox.isSelected());
			}
		});
		parentMenu.add(lockMapBox);
	}
	
	private void addShowAIBattles(JMenu parentMenu)
	{
		final JCheckBoxMenuItem showAIBattlesBox = new JCheckBoxMenuItem("Show Battles Between AIs");
		showAIBattlesBox.setSelected(getUIContext().getShowBattlesBetweenAIs());
		showAIBattlesBox.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				getUIContext().setShowBattlesBetweenAIs(showAIBattlesBox.isSelected());
			}
		});
		parentMenu.add(showAIBattlesBox);
	}
	
	private void addChangeDynamixAISettings(JMenu parentMenu)
	{
		boolean areThereDynamixAIs = false;
		Set<IGamePlayer> players = (m_frame).GetLocalPlayers();
		for (IGamePlayer player : players)
		{
			if (player instanceof Dynamix_AI)
				areThereDynamixAIs = true;
		}
		
		if (areThereDynamixAIs)
		{
			Dynamix_AI.ClearAIInstancesMemory();
			Dynamix_AI.Initialize(m_frame);
			for (IGamePlayer player : players)
			{
				if (player instanceof Dynamix_AI)
					Dynamix_AI.AddDynamixAIIntoAIInstancesMemory((Dynamix_AI) player);
			}
			parentMenu.addSeparator();
			parentMenu.add(new AbstractAction("Change Dynamix AI Settings")
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					Dynamix_AI.ShowSettingsWindow();
				}
			});
			parentMenu.addSeparator();
		}
	}
	
	private void addAISleepDuration(JMenu parentMenu)
	{
		final JMenuItem AISleepDurationBox = new JMenuItem("AI Pause Duration...");
		AISleepDurationBox.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				final IntTextField text = new IntTextField(0, 10000);
				text.setText(String.valueOf(UIContext.getAIPauseDuration()));
				JPanel panel = new JPanel();
				panel.setLayout(new GridBagLayout());
				panel.add(new JLabel("AI Pause Duration (ms):"), new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
				panel.add(text, new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
				JOptionPane.showOptionDialog(JOptionPane.getFrameForComponent(TripleaMenu.this), panel, "Set AI Pause Duration", JOptionPane.OK_OPTION, JOptionPane.INFORMATION_MESSAGE, null,
							new String[] { "OK" }, "OK");
				try
				{
					UIContext.setAIPauseDuration(Integer.parseInt(text.getText()));
				}
					catch (Exception ex)
				{
				}
			}
		});
		parentMenu.add(AISleepDurationBox);
	}
	
	/**
	 * @param parentMenu
	 */
	private void addShowDiceStats(JMenu parentMenu)
	{
		Action showDiceStats = new AbstractAction("Show Dice Stats...")
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				IRandomStats randomStats = (IRandomStats) getGame().getRemoteMessenger().getRemote(IRandomStats.RANDOM_STATS_REMOTE_NAME);
				
				RandomStatsDetails stats = randomStats.getRandomStats();
				
				JPanel panel = new JPanel();
				BoxLayout layout = new BoxLayout(panel, BoxLayout.Y_AXIS);
				panel.setLayout(layout);
				
				Iterator<Integer> iter = new TreeSet<Integer>(stats.getData().keySet()).iterator();
				while (iter.hasNext())
				{
					Integer key = iter.next();
					int value = stats.getData().getInt(key);
					JLabel label = new JLabel(key + " was rolled " + value + " times");
					panel.add(label);
				}
				panel.add(new JLabel("  "));
				DecimalFormat format = new DecimalFormat("#0.000");
				panel.add(new JLabel("Average roll : " + format.format(stats.getAverage())));
				panel.add(new JLabel("Median : " + format.format(stats.getMedian())));
				panel.add(new JLabel("Variance : " + format.format(stats.getVariance())));
				panel.add(new JLabel("Standard Deviation : " + format.format(stats.getStdDeviation())));
				panel.add(new JLabel("Total rolls : " + stats.getTotal()));
				
				JOptionPane.showMessageDialog(m_frame, panel, "Random Stats", JOptionPane.INFORMATION_MESSAGE);
				
			}
		};
		parentMenu.add(showDiceStats);
	}
	
	private void addRollDice(JMenu parentMenu)
	{
		final JMenuItem RollDiceBox = new JMenuItem("Roll Dice...");
		RollDiceBox.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				final IntTextField numberOfText = new IntTextField(0, 100);
				final IntTextField diceSidesText = new IntTextField(1, 200);
				numberOfText.setText(String.valueOf(0));
				diceSidesText.setText(String.valueOf(getGame().getData().getDiceSides()));
				JPanel panel = new JPanel();
				panel.setLayout(new GridBagLayout());
				panel.add(new JLabel("Number of Dice to Roll: "), new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 20), 0, 0));
				panel.add(new JLabel("Sides on the Dice: "), new GridBagConstraints(2, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 20, 0, 10), 0, 0));
				panel.add(numberOfText, new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 20), 0, 0));
				panel.add(diceSidesText, new GridBagConstraints(2, 1, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 20, 0, 10), 0, 0));
				JOptionPane.showOptionDialog(JOptionPane.getFrameForComponent(TripleaMenu.this), panel, "Roll Dice", JOptionPane.OK_OPTION, JOptionPane.INFORMATION_MESSAGE, null,
							new String[] { "OK" }, "OK");
				try
				{
					int numberOfDice = Integer.parseInt(numberOfText.getText());
					if (numberOfDice > 0)
					{
						int diceSides = Integer.parseInt(diceSidesText.getText());
						int[] dice = getGame().getRandomSource().getRandom(diceSides, numberOfDice, "Rolling Dice, no effect on game.");
						JPanel panelDice = new JPanel();
						BoxLayout layout = new BoxLayout(panelDice, BoxLayout.Y_AXIS);
						panelDice.setLayout(layout);
						JLabel label = new JLabel("Rolls (no effect on game): ");
						panelDice.add(label);
						String diceString = "";
						for (int i = 0; i < dice.length; i++)
							diceString += String.valueOf(dice[i] + 1) + ((i == dice.length - 1) ? "" : ", ");
						JTextField diceList = new JTextField(diceString);
						diceList.setEditable(false);
						panelDice.add(diceList);
						JOptionPane.showMessageDialog(m_frame, panelDice, "Dice Rolled", JOptionPane.INFORMATION_MESSAGE);
					}
				}
					catch (Exception ex)
				{
				}
			}
		});
		parentMenu.add(RollDiceBox);
	}
	
	private void addBattleCalculatorMenu(JMenu menuGame)
	{
		Action showBattleMenu = new AbstractAction("Battle Calculator...")
		{
			
			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				OddsCalculatorDialog.show(m_frame, null);
			}
			
		};
		
		menuGame.add(showBattleMenu);
		
	}
	
	private void addExportXML(JMenu parentMenu)
	{
		Action exportXML = new AbstractAction("Export game.xml file (Beta)...")
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				exportXMLFile();
				
			}
			
			/**
             *
             */
			private void exportXMLFile()
			{
				JFileChooser chooser = new JFileChooser();
				chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
				File rootDir = new File(System.getProperties().getProperty("user.dir"));
				
				DateFormat format = new SimpleDateFormat("yyyy_MM_dd");
				String defaultFileName = "xml_" + format.format(new Date()) + "_" + getData().getGameName() + "_round_" + getData().getSequence().getRound() + ".xml";
				
				chooser.setSelectedFile(new File(rootDir, defaultFileName));
				
				if (chooser.showSaveDialog(m_frame) != JOptionPane.OK_OPTION)
					return;
				
				GameData data = getData();
				
				GameDataExporter exporter = new games.strategy.engine.data.export.GameDataExporter(data);
				String xmlFile = exporter.getXML();
				try
				{
					FileWriter writer = new FileWriter(chooser.getSelectedFile());
					try
					{
						writer.write(xmlFile);
					}
					finally
					{
						writer.close();
					}
				} catch (IOException e1)
				{
					e1.printStackTrace();
				}
			}
		};
		parentMenu.add(exportXML);
		
	}
	
	/**
	 * @param parentMenu
	 */
	private void addExportStatsFull(JMenu parentMenu)
	{
		Action showDiceStats = new AbstractAction("Export Full Game Stats...")
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				createAndSaveStats(true);
			}
		};
		parentMenu.add(showDiceStats);
	}
	
	/**
	 * @param parentMenu
	 */
	private void addExportStats(JMenu parentMenu)
	{
		Action showDiceStats = new AbstractAction("Export Short Game Stats...")
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				createAndSaveStats(false);
			}
		};
		parentMenu.add(showDiceStats);
	}
	
	/**
     *
     */
	private void createAndSaveStats(boolean showPhaseStats)
	{
		StatPanel statPanel = m_frame.getStatPanel();
		JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		File rootDir = new File(System.getProperties().getProperty("user.dir"));
		
		DateFormat format = new SimpleDateFormat("yyyy_MM_dd");
		String defaultFileName = "stats_" + format.format(new Date()) + "_" + getData().getGameName() + "_round_" + getData().getSequence().getRound() + (showPhaseStats ? "_full" : "_short") + ".csv";
		
		chooser.setSelectedFile(new File(rootDir, defaultFileName));
		
		if (chooser.showSaveDialog(m_frame) != JOptionPane.OK_OPTION)
			return;
		
		GameData clone;
		
		getData().acquireReadLock();
		try
		{
			clone = GameDataUtils.cloneGameData(getData());
		} finally
		{
			getData().releaseReadLock();
		}
		IStat[] stats = statPanel.getStats();
		// extended stats covers stuff that doesn't show up in the game stats menu bar, like custom resources or tech tokens or # techs, etc.
		IStat[] statsExtended = statPanel.getStatsExtended(getData());
		
		String[] alliances = statPanel.getAlliances().toArray(new String[statPanel.getAlliances().size()]);
		PlayerID[] players = statPanel.getPlayers().toArray(new PlayerID[statPanel.getPlayers().size()]);
		
		// its important here to translate the player objects into our game data
		// the players for the stat panel are only relevant with respect to
		// the game data they belong to
		for (int i = 0; i < players.length; i++)
		{
			players[i] = clone.getPlayerList().getPlayerID(players[i].getName());
		}
		
		StringBuilder text = new StringBuilder(1000);
		
		text.append(defaultFileName + ",");
		text.append("\n");
		
		text.append("TripleA Engine Version: ,");
		text.append(games.strategy.engine.EngineVersion.VERSION.toString() + ",");
		text.append("\n");
		
		text.append("Game Name: ,");
		text.append(getData().getGameName() + ",");
		text.append("\n");
		
		text.append("Game Version: ,");
		text.append(getData().getGameVersion() + ",");
		text.append("\n");
		text.append("\n");
		
		text.append("Current Round: ,");
		text.append(getData().getSequence().getRound() + ",");
		text.append("\n");
		
		text.append("Number of Players: ,");
		text.append(statPanel.getPlayers().size() + ",");
		text.append("\n");
		
		text.append("Number of Alliances: ,");
		text.append(statPanel.getAlliances().size() + ",");
		text.append("\n");
		
		text.append("\n");
		text.append("Turn Order: ,");
		text.append("\n");
		
		List<PlayerID> playerOrderList = new ArrayList<PlayerID>();
		playerOrderList.addAll(getData().getPlayerList().getPlayers());
		Collections.shuffle(playerOrderList);
		Collections.sort(playerOrderList, new PlayerOrderComparator(getData()));
		
		Set<PlayerID> playerOrderSetNoDuplicates = new LinkedHashSet<PlayerID>(playerOrderList);
		
		Iterator<PlayerID> playerOrderIterator = playerOrderSetNoDuplicates.iterator();
		while (playerOrderIterator.hasNext())
		{
			PlayerID currentPlayerID = playerOrderIterator.next();
			text.append(currentPlayerID.getName() + ",");
			Iterator<String> allianceName = getData().getAllianceTracker().getAlliancesPlayerIsIn(currentPlayerID).iterator();
			while (allianceName.hasNext())
			{
				text.append(allianceName.next() + ",");
			}
			text.append("\n");
		}
		
		text.append("\n");
		text.append("Winners: ,");
		EndRoundDelegate delegateEndRound = (EndRoundDelegate) getData().getDelegateList().getDelegate("endRound");
		if (delegateEndRound != null && delegateEndRound.getWinners() != null)
		{
			for (PlayerID p : delegateEndRound.getWinners())
			{
				text.append(p.getName() + ",");
			}
		}
		else
			text.append("none yet; game not over,");
		text.append("\n");
		
		text.append("\n");
		text.append("Resource Chart: ,");
		text.append("\n");
		Iterator<Resource> resourceIterator = getData().getResourceList().getResources().iterator();
		while (resourceIterator.hasNext())
		{
			text.append(resourceIterator.next().getName() + ",");
			text.append("\n");
		}
		
		// if short, we won't both showing production and unit info
		if (showPhaseStats)
		{
			text.append("\n");
			text.append("Production Rules: ,");
			text.append("\n");
			text.append("Name,Result,Quantity,Cost,Resource,\n");
			Iterator<ProductionRule> purchaseOptionsIterator = getData().getProductionRuleList().getProductionRules().iterator();
			while (purchaseOptionsIterator.hasNext())
			{
				ProductionRule pr = purchaseOptionsIterator.next();
				text.append(pr.getName() + ","
							+ pr.getResults().keySet().iterator().next().getName() + ","
							+ pr.getResults().getInt(pr.getResults().keySet().iterator().next()) + ","
							+ pr.getCosts().getInt(pr.getCosts().keySet().iterator().next()) + ","
							+ pr.getCosts().keySet().iterator().next().getName() + ",");
				text.append("\n");
			}
			
			text.append("\n");
			text.append("Unit Types: ,");
			text.append("\n");
			text.append("Name,Listed Abilities\n");
			Iterator<UnitType> allUnitsIterator = getData().getUnitTypeList().iterator();
			while (allUnitsIterator.hasNext())
			{
				String toModify = UnitAttachment.get(allUnitsIterator.next()).toString().replaceFirst("UnitType called ", "").replaceFirst(" with:", "").replaceAll("games.strategy.engine.data.", "")
							.replaceAll("\n", ";").replaceAll(",", ";");
				toModify = toModify.replaceAll("  ", ",");
				toModify = toModify.replaceAll(", ", ",").replaceAll(" ,", ",");
				text.append(toModify);
				text.append("\n");
			}
		}
		
		text.append("\n");
		text.append((showPhaseStats ? "Full Stats (includes each phase that had activity)," : "Short Stats (only shows first phase with activity per player per round),"));
		text.append("\n");
		text.append("Turn Stats: ,");
		text.append("\n");
		
		text.append("Round,Player Turn,Phase Name,");
		
		for (int i = 0; i < stats.length; i++)
		{
			for (int j = 0; j < players.length; j++)
			{
				text.append(stats[i].getName()).append(" ");
				text.append(players[j].getName());
				text.append(",");
				
			}
			
			for (int j = 0; j < alliances.length; j++)
			{
				text.append(stats[i].getName()).append(" ");
				text.append(alliances[j]);
				text.append(",");
			}
		}
		
		for (int i = 0; i < statsExtended.length; i++)
		{
			for (int j = 0; j < players.length; j++)
			{
				text.append(statsExtended[i].getName()).append(" ");
				text.append(players[j].getName());
				text.append(",");
				
			}
			
			for (int j = 0; j < alliances.length; j++)
			{
				text.append(statsExtended[i].getName()).append(" ");
				text.append(alliances[j]);
				text.append(",");
			}
		}
		text.append("\n");
		clone.getHistory().gotoNode(clone.getHistory().getLastNode());
		
		@SuppressWarnings("rawtypes")
		Enumeration nodes = ((DefaultMutableTreeNode) clone.getHistory().getRoot()).preorderEnumeration();
		
		PlayerID currentPlayer = null;
		
		int round = 0;
		while (nodes.hasMoreElements())
		{
			// we want to export on change of turn
			
			HistoryNode element = (HistoryNode) nodes.nextElement();
			
			if (element instanceof Round)
				round++;
			
			if (!(element instanceof Step))
				continue;
			
			Step step = (Step) element;
			
			if (step.getPlayerID() == null || step.getPlayerID().isNull())
				continue;
			
			// this is to stop from having multiple entries for each players turn.
			if (!showPhaseStats)
			{
				if (step.getPlayerID() == currentPlayer)
					continue;
			}
			
			currentPlayer = step.getPlayerID();
			
			clone.getHistory().gotoNode(element);
			
			String playerName = step.getPlayerID() == null ? "" : step.getPlayerID().getName() + ": ";
			String stepName = step.getStepName();
			// copied directly from TripleAPlayer, will probably have to be updated in the future if more delegates are made
			if (stepName.endsWith("Bid"))
				stepName = "Bid";
			else if (stepName.endsWith("Tech"))
				stepName = "Tech";
			else if (stepName.endsWith("TechActivation"))
				stepName = "TechActivation";
			else if (stepName.endsWith("Purchase"))
				stepName = "Purchase";
			else if (stepName.endsWith("NonCombatMove"))
				stepName = "NonCombatMove";
			else if (stepName.endsWith("Move"))
				stepName = "Move";
			else if (stepName.endsWith("Battle"))
				stepName = "Battle";
			else if (stepName.endsWith("BidPlace"))
				stepName = "BidPlace";
			else if (stepName.endsWith("Place"))
				stepName = "Place";
			else if (stepName.endsWith("Politics"))
				stepName = "Politics";
			else if (stepName.endsWith("EndTurn"))
				stepName = "EndTurn";
			else
				stepName = "";
			
			text.append(round).append(",").append(playerName).append(",").append(stepName).append(",");
			
			for (int i = 0; i < stats.length; i++)
			{
				for (int j = 0; j < players.length; j++)
				{
					text.append(stats[i].getFormatter().format(stats[i].getValue(players[j], clone)));
					text.append(",");
					
				}
				for (int j = 0; j < alliances.length; j++)
				{
					text.append(stats[i].getFormatter().format(stats[i].getValue(alliances[j], clone)));
					text.append(",");
				}
			}
			
			for (int i = 0; i < statsExtended.length; i++)
			{
				for (int j = 0; j < players.length; j++)
				{
					text.append(statsExtended[i].getFormatter().format(statsExtended[i].getValue(players[j], clone)));
					text.append(",");
					
				}
				for (int j = 0; j < alliances.length; j++)
				{
					text.append(statsExtended[i].getFormatter().format(statsExtended[i].getValue(alliances[j], clone)));
					text.append(",");
				}
			}
			text.append("\n");
		}
		
		try
		{
			FileWriter writer = new FileWriter(chooser.getSelectedFile());
			try
			{
				writer.write(text.toString());
			} finally
			{
				writer.close();
			}
		} catch (IOException e1)
		{
			e1.printStackTrace();
		}
	}
	
	private void setWidgetActivation()
	{
		showMapDetails.setEnabled(getUIContext().getMapData().getHasRelief());
	}
	
	/**
	 * @param menuGame
	 */
	private void addShowMapDetails(JMenu menuGame)
	{
		showMapDetails = new JCheckBoxMenuItem("Show Map Details");
		
		showMapDetails.setSelected(TileImageFactory.getShowReliefImages());
		
		showMapDetails.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (TileImageFactory.getShowReliefImages() == showMapDetails.isSelected())
					return;
				
				TileImageFactory.setShowReliefImages(showMapDetails.isSelected());
				Thread t = new Thread("Triplea : Show map details thread")
				{
					@Override
					public void run()
					{
						yield();
						m_frame.getMapPanel().updateCountries(getData().getMap().getTerritories());
						
					}
				};
				t.start();
				
			}
		});
		menuGame.add(showMapDetails);
	}
	
	/**
	 * @param menuGame
	 */
	private void addShowMapBlends(JMenu menuGame)
	{
		showMapBlends = new JCheckBoxMenuItem("Show Map Blends");
		
		if (getUIContext().getMapData().getHasRelief() && showMapDetails.isEnabled() && showMapDetails.isSelected())
		{
			showMapBlends.setEnabled(true);
			showMapBlends.setSelected(TileImageFactory.getShowMapBlends());
		}
		else
		{
			showMapBlends.setSelected(false);
			showMapBlends.setEnabled(false);
		}
		
		showMapBlends.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (TileImageFactory.getShowMapBlends() == showMapBlends.isSelected())
				{
					return;
				}
				
				TileImageFactory.setShowMapBlends(showMapBlends.isSelected());
				TileImageFactory.setShowMapBlendMode(m_frame.getUIContext().getMapData().getMapBlendMode());
				TileImageFactory.setShowMapBlendAlpha(m_frame.getUIContext().getMapData().getMapBlendAlpha());
				Thread t = new Thread("Triplea : Show map Blends thread")
				{
					@Override
					public void run()
					{
						m_frame.setScale(m_frame.getUIContext().getScale() * 100);
						
						yield();
						m_frame.getMapPanel().updateCountries(getData().getMap().getTerritories());
						
					}
				};
				t.start();
				
			}
		});
		menuGame.add(showMapBlends);
	}
	
	/**
	 * @param menuGame
	 */
	private void addMapSkinsMenu(JMenu menuGame)
	{
		// beagles Mapskin code
		// creates a sub menu of radiobuttons for each available mapdir
		
		JMenuItem mapMenuItem;
		JMenu mapSubMenu = new JMenu("Map Skins");
		ButtonGroup mapButtonGroup = new ButtonGroup();
		
		menuGame.add(mapSubMenu);
		
		final Map<String, String> skins = UIContext.getSkins(m_frame.getGame().getData());
		for (final String key : skins.keySet())
		{
			mapMenuItem = new JRadioButtonMenuItem(key);
			
			mapButtonGroup.add(mapMenuItem);
			mapSubMenu.add(mapMenuItem);
			mapSubMenu.setEnabled(skins.size() > 1);
			
			if (skins.get(key).equals(UIContext.getMapDir()))
				mapMenuItem.setSelected(true);
			
			mapMenuItem.addActionListener(new ActionListener()
					{
						@Override
						public void actionPerformed(ActionEvent e)
						{
							try
							{
								m_frame.updateMap(skins.get(key));
								if (m_frame.getUIContext().getMapData().getHasRelief())
									{
										showMapDetails.setSelected(true);
									}
									setWidgetActivation();
								} catch (Exception se)
								{
									se.printStackTrace();
									JOptionPane.showMessageDialog(m_frame, se.getMessage(), "Error Changing Map Skin2", JOptionPane.OK_OPTION);
								}
								
							}// else
							
					}// actionPerformed
					);
		}
		
	}
	
	/**
	 * @param parentMenu
	 */
	private void addExportSetupCharts(JMenu parentMenu)
	{
		JMenuItem menuFileExport = new JMenuItem(new AbstractAction(
					"Export Setup Charts...")
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				final JFrame frame = new JFrame("Export Setup Files");
				frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				
				GameData data = m_frame.getGame().getData();
				GameData clonedGameData;
				data.acquireReadLock();
				try
				{
					
					clonedGameData = GameDataUtils.cloneGameData(data);
					
				} finally
				{
					data.releaseReadLock();
				}
				JComponent newContentPane = new SetupFrame(clonedGameData);
				newContentPane.setOpaque(true); // content panes must be opaque
				frame.setContentPane(newContentPane);
				
				// Display the window.
				frame.pack();
				frame.setLocationRelativeTo(m_frame);
				frame.setVisible(true);
				m_frame.getUIContext().addShutdownWindow(frame);
			}
		});
		parentMenu.add(menuFileExport);
	}
	
	private void addUnitSizeMenu(JMenu parentMenu)
	{
		
		final NumberFormat s_decimalFormat = new DecimalFormat("00.##");
		
		// This is the action listener used
		class UnitSizeAction extends AbstractAction
		{
			private final double m_scaleFactor;
			
			public UnitSizeAction(double scaleFactor)
			{
				m_scaleFactor = scaleFactor;
				putValue(Action.NAME, s_decimalFormat.format(m_scaleFactor * 100) + "%");
			}
			
			@Override
			public void actionPerformed(ActionEvent e)
			{
				getUIContext().setUnitScaleFactor(m_scaleFactor);
				m_frame.getMapPanel().resetMap();
			}
		}
		
		JMenu unitSizeMenu = new JMenu();
		unitSizeMenu.setText("Unit Size");
		ButtonGroup unitSizeGroup = new ButtonGroup();
		JRadioButtonMenuItem radioItem125 = new JRadioButtonMenuItem(new UnitSizeAction(1.25));
		
		JRadioButtonMenuItem radioItem100 = new JRadioButtonMenuItem(new UnitSizeAction(1.0));
		JRadioButtonMenuItem radioItem87 = new JRadioButtonMenuItem(new UnitSizeAction(0.875));
		JRadioButtonMenuItem radioItem83 = new JRadioButtonMenuItem(new UnitSizeAction(0.8333));
		JRadioButtonMenuItem radioItem75 = new JRadioButtonMenuItem(new UnitSizeAction(0.75));
		JRadioButtonMenuItem radioItem66 = new JRadioButtonMenuItem(new UnitSizeAction(0.6666));
		JRadioButtonMenuItem radioItem56 = new JRadioButtonMenuItem(new UnitSizeAction(0.5625));
		JRadioButtonMenuItem radioItem50 = new JRadioButtonMenuItem(new UnitSizeAction(0.5));
		
		unitSizeGroup.add(radioItem125);
		unitSizeGroup.add(radioItem100);
		unitSizeGroup.add(radioItem87);
		unitSizeGroup.add(radioItem83);
		unitSizeGroup.add(radioItem75);
		unitSizeGroup.add(radioItem66);
		unitSizeGroup.add(radioItem56);
		unitSizeGroup.add(radioItem50);
		
		radioItem100.setSelected(true);
		
		// select the closest to to the default size
		@SuppressWarnings("rawtypes")
		Enumeration enum1 = unitSizeGroup.getElements();
		boolean matchFound = false;
		while (enum1.hasMoreElements())
		{
			JRadioButtonMenuItem menuItem = (JRadioButtonMenuItem) enum1.nextElement();
			UnitSizeAction action = (UnitSizeAction) menuItem.getAction();
			if (Math.abs(action.m_scaleFactor - getUIContext().getUnitImageFactory().getScaleFactor()) < 0.01)
			{
				menuItem.setSelected(true);
				matchFound = true;
				break;
			}
		}
		
		if (!matchFound)
			System.err.println("default unit size does not match any menu item");
		
		unitSizeMenu.add(radioItem125);
		unitSizeMenu.add(radioItem100);
		unitSizeMenu.add(radioItem87);
		unitSizeMenu.add(radioItem83);
		unitSizeMenu.add(radioItem75);
		unitSizeMenu.add(radioItem66);
		unitSizeMenu.add(radioItem56);
		unitSizeMenu.add(radioItem50);
		
		parentMenu.add(unitSizeMenu);
	}
	
}
