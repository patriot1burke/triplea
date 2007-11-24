package games.strategy.engine.framework.startup.ui;

import games.strategy.engine.chat.ChatPanel;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.startup.mc.*;
import games.strategy.triplea.ui.ErrorHandler;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.border.*;

public class MainPanel extends JPanel implements Observer
{
    private GameSelectorPanel m_gameSelectorPanel;
    private JButton m_playButton;
    private JButton m_quitButton;
    private JButton m_cancelButton;
    
    private final GameSelectorModel m_gameSelectorModel;
    private SetupPanel m_gameSetupPanel;
    private JPanel m_gameSetupPanelHolder;
    private JPanel m_chatPanelHolder;
    private final SetupPanelModel m_gameTypePanelModel;
    
    private static final Dimension m_initialSize;
    
    static {
        if(GameRunner.isMac()) {
            m_initialSize = new Dimension(685,620);       
        } else {
            m_initialSize = new Dimension(625,550);        
        }
    }
     
    //private final Dimension m_initialSizeWithChat = new Dimension(500,650);
    private boolean m_isChatShowing;
    
    public MainPanel(SetupPanelModel typePanelModel)
    {   
        m_gameTypePanelModel = typePanelModel;
        m_gameSelectorModel = typePanelModel.getGameSelectorModel();
        createComponents();
        layoutComponents();
        setupListeners();
        setWidgetActivation();
        if(typePanelModel.getPanel() != null)
        {
            setGameSetupPanel(typePanelModel.getPanel());
        }

    }

    JButton getDefaultButton()
    {
        return m_playButton;
    }
    
    private void createComponents()
    {
        m_playButton = new JButton("Play");
        m_quitButton = new JButton("Quit");
        m_cancelButton = new JButton("Cancel");
        
        m_gameSelectorPanel = new GameSelectorPanel(m_gameSelectorModel);
        m_gameSelectorPanel.setBorder(new EtchedBorder());
        
        m_gameSetupPanelHolder = new JPanel();
        m_gameSetupPanelHolder.setLayout(new BorderLayout());
        
        m_chatPanelHolder = new JPanel();
        m_chatPanelHolder.setLayout(new BorderLayout());
        
    }

    private void layoutComponents()
    {
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setBorder(new EtchedBorder());
        buttonsPanel.setLayout(new FlowLayout( FlowLayout.CENTER ));
        buttonsPanel.add(m_playButton);
        buttonsPanel.add(m_quitButton);
        
        setLayout(new GridBagLayout());
        
        m_gameSetupPanelHolder.setLayout(new BorderLayout());        
        
        add(m_gameSelectorPanel, new GridBagConstraints(0,0,1,1,0,0,GridBagConstraints.WEST, GridBagConstraints.VERTICAL, new Insets(00,0,0,0), 0,0));
        add(m_gameSetupPanelHolder, new GridBagConstraints(1,0,1,1,1,1,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(00,0,0,0), 0,0));

        addChat();
        
        
        add(buttonsPanel, new GridBagConstraints(0,2, 2,1,0,0,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(00,0,0,0), 0,0));
        
        
        setPreferredSize(m_initialSize);
        
    }

    private void addChat()
    {
        m_chatPanelHolder.removeAll();
        remove(m_chatPanelHolder);
        
        ChatPanel chat = m_gameTypePanelModel.getPanel().getChatPanel();
        if(chat != null)
        {
            m_chatPanelHolder = new JPanel();
            m_chatPanelHolder.setLayout(new BorderLayout());
            m_chatPanelHolder.add(chat, BorderLayout.CENTER);
            add(m_chatPanelHolder, new GridBagConstraints(0,1,2,1,1,1,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(00,0,0,0), 0,0));
        }
        m_isChatShowing = chat != null;
    }
    
    public void setGameSetupPanel(SetupPanel panel)
    {
        if(m_gameSetupPanel != null)
        {
            m_gameSetupPanel.removeObserver(this);
            m_gameSetupPanelHolder.remove(panel);
        }
        
        m_gameSetupPanel = panel;
        m_gameSetupPanelHolder.removeAll();
        m_gameSetupPanelHolder.add(panel, BorderLayout.CENTER);
        panel.addObserver(this);
        setWidgetActivation();
        
        //add the cancel button if we are not choosing the type.
        if(!( panel instanceof MetaSetupPanel))
        {
            JPanel cancelPanel = new JPanel();
            
            cancelPanel.setBorder(new EmptyBorder(10,0,10,10));
            
            cancelPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
            
            
            
            createUserActionMenu(cancelPanel);
            
            cancelPanel.add(m_cancelButton);

            
            m_gameSetupPanelHolder.add(cancelPanel, BorderLayout.SOUTH);
        }
        
        boolean panelHasChat = (m_gameTypePanelModel.getPanel().getChatPanel() != null);
        if(panelHasChat != m_isChatShowing)
            addChat();

        invalidate();
        revalidate();
        
    }

    private void createUserActionMenu(JPanel cancelPanel)
    {
        if(m_gameSetupPanel.getUserActions() == null)
            return;
        
        //if we need this for something other than network, add a way to set it
        final JButton button = new JButton("Network...");
        
        
        button.addActionListener(new ActionListener()
        {
        
            public void actionPerformed(ActionEvent e)
            {
                JPopupMenu menu = new JPopupMenu();
        
                List<Action> actions = m_gameSetupPanel.getUserActions();
                if(actions != null && !actions.isEmpty())
                {
                    for(Action a : actions)
                    {
                        menu.add(a);
                    }
                }
                
                menu.show(button, 0 , button.getHeight());
        
            }
        
        });
        cancelPanel.add(button);
        
        
        
        
    }

    private void setupListeners()
    {
        m_gameTypePanelModel.addObserver(new Observer()
        {
        
            public void update(Observable o, Object arg)
            {
                setGameSetupPanel(m_gameTypePanelModel.getPanel());
            }
        
        });
        
        m_playButton.addActionListener(new ActionListener()
        {
        
            public void actionPerformed(ActionEvent e)
            {
                play();
        
            }
        
        });
        
        m_quitButton.addActionListener(new ActionListener()
        {
        
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    m_gameSetupPanel.cancel();
                }
                finally
                {
                    System.exit(0);
                }
            }
        });
        
        m_cancelButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                m_gameTypePanelModel.showSelectType();
            }
        
        });
        
        m_gameSelectorModel.addObserver(this);
    }
    
    private void play()
    {
        ErrorHandler.setGameOver(false);
        m_gameSetupPanel.preStartGame();
        m_gameTypePanelModel.getPanel().getLauncher().launch(this);
        m_gameSetupPanel.postStartGame();
        
    }

    private void setWidgetActivation()
    {
        
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
            
                public void run()
                {
                    setWidgetActivation();
                }
            
            });
            return;
        }
        
        if(m_gameSetupPanel != null)
        {
            m_playButton.setEnabled(m_gameSetupPanel.canGameStart());
        }
        else
        {
            m_playButton.setEnabled(false);
        }
    }
    

    
    public static void main(String[] args)
    {
        JFrame f = new JFrame();
        
        GameSelectorModel gameSelectorModel = new GameSelectorModel();
        SetupPanelModel model = new SetupPanelModel(gameSelectorModel);
        model.showSelectType();
        
        f.getContentPane().add(new MainPanel(model));
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        
        
        f.pack();
        f.setVisible(true);
    }

    public void update(Observable o, Object arg)
    {
        setWidgetActivation();
    }
    
}


