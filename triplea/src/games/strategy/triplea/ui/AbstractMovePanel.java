package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.gamePlayer.IPlayerBridge;
import games.strategy.triplea.delegate.UndoableMove;
import games.strategy.triplea.delegate.dataObjects.MoveDescription;
import games.strategy.triplea.delegate.remote.IMoveDelegate;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/**
 * 
 * @author Erik von der Osten (based on original move panel by Sean Bridges)
 * @version 1.0
 */
@SuppressWarnings("serial")
public abstract class AbstractMovePanel extends ActionPanel
{
    
    private static final String s_MOVE_PANEL_CANCEL = "movePanel.cancel";
    private static final Logger s_logger = Logger.getLogger(MovePanel.class.getName());
    private static final int s_entryPadding = 15;
    private TripleAFrame m_frame;
    private boolean m_listening = false;
    private JLabel m_actionLabel = new JLabel();
    private MoveDescription m_moveMessage;
    private List<UndoableMove> m_undoableMoves;
    private final AbstractAction m_doneMove = new AbstractAction("Done")
    {
        public void actionPerformed(ActionEvent e)
        {
            if (m_undoableMovesPanel.getCountOfMovesMade() == 0)
            {
                int rVal = JOptionPane.showConfirmDialog(JOptionPane.getFrameForComponent(AbstractMovePanel.this), "Are you sure you dont want to move?", "End Move", JOptionPane.YES_NO_OPTION);
                if (rVal != JOptionPane.YES_OPTION)
                {
                    return;
                }
                
            }
            
            m_moveMessage = null;
            release();
            
        }
    };
    private final Action m_DONE_MOVE_ACTION = new WeakAction("Done", m_doneMove);
    private final Action m_cancelMove = new AbstractAction("Cancel")
    {
        public void actionPerformed(ActionEvent e)
        {
            cancelMoveAction();
            
            m_frame.clearStatusMessage();
            this.setEnabled(false);
            m_CANCEL_MOVE_ACTION.setEnabled(false);
        }
    };
    /*
     * sub-classes method for cancel handling
     */
    abstract protected void cancelMoveAction();
    private final AbstractAction m_CANCEL_MOVE_ACTION = new WeakAction("Cancel", m_cancelMove);
    private UndoableMovesPanel m_undoableMovesPanel;
    private IPlayerBridge m_bridge; 
    
    public AbstractMovePanel(GameData data, MapPanel map, TripleAFrame frame)
    {
        super(data, map);
        m_frame = frame;
        m_CANCEL_MOVE_ACTION.setEnabled(false);
        m_undoableMovesPanel = new UndoableMovesPanel(data, this);
        m_undoableMoves = Collections.emptyList();
    }
    
    // m_frame methods
    protected final void clearStatusMessage()
    {
        m_frame.clearStatusMessage();
    }
    protected final void setStatusErrorMessage(final String message)
    {
        m_frame.setStatusErrorMessage(message);
    }
    protected final void setStatusWarningMessage(final String message)
    {
        m_frame.setStatusWarningMessage(message);
    }

    protected final boolean getListening()
    {
        return m_listening;
    }

    protected final void setMoveMessage(MoveDescription message)
    {
        m_moveMessage = message;        
    }
    
    protected final List<UndoableMove> getUndoableMoves()
    {
        return m_undoableMoves;        
    }
    
    protected final void enableCancelButton()
    {
        m_CANCEL_MOVE_ACTION.setEnabled(true);
    }

    /**
     * @return m_bridge.getGameData()
     */
    protected final GameData getGameData() 
    {
        return m_bridge.getGameData();
    }
    private IMoveDelegate getDelegate()
    {
        return (IMoveDelegate) m_bridge.getRemote();
    }
    
    protected final void updateMoves()
    {
        m_undoableMoves = getDelegate().getMovesMade();
        m_undoableMovesPanel.setMoves(m_undoableMoves);
    }
    
    public final void cancelMove()
    {
        m_CANCEL_MOVE_ACTION.actionPerformed(null);
    }
    
    public final void undoMove(int moveIndex)
    {
        // clean up any state we may have
        m_CANCEL_MOVE_ACTION.actionPerformed(null);
        undoMoveSpecific();
        
        // undo the move
        String error = getDelegate().undoMove(moveIndex);
        if (error != null)
        {
            JOptionPane.showMessageDialog(getTopLevelAncestor(), error, "Could not undo move", JOptionPane.ERROR_MESSAGE);
        }
        else
        {
            updateMoves();
        }
        
    }
    /*
     * sub-classes method for undo handling
     */
    abstract protected void undoMoveSpecific();
    
    protected final void cleanUp()
    {
        
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                s_logger.fine("cleanup");
                
                if (!m_listening)
                {
                    throw new IllegalStateException("Not listening");
                }
                m_listening = false;
                
                cleanUpSpecific();
                
                m_bridge = null;
                
                m_CANCEL_MOVE_ACTION.setEnabled(false);
                JComponent rootPane = getRootPane();
                if (rootPane != null)
                {
                    rootPane.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), null);
                }
                
                removeAll();
                REFRESH.run();
                
            }
            
        });
        
    }
    /*
     * sub-classes method for clean-up
     */
    abstract protected void cleanUpSpecific();
    
    public final void setActive(boolean active)
    {
        super.setActive(active);
        SwingUtilities.invokeLater(new Runnable()
        {
            
            public void run()
            {
                m_CANCEL_MOVE_ACTION.actionPerformed(null);
            }
            
        });
        
    }
    
    protected final void display(final PlayerID id, final String actionLabel)
    {
        
        super.display(id);
        
        SwingUtilities.invokeLater(new Runnable()
        {
            
            public void run()
            {
                removeAll();
                m_actionLabel.setText(id.getName() + actionLabel);
                add(leftBox(m_actionLabel));
                add(leftBox(new JButton(m_CANCEL_MOVE_ACTION)));
                add(leftBox(new JButton(m_DONE_MOVE_ACTION)));
                add(Box.createVerticalStrut(s_entryPadding));
                
                add(m_undoableMovesPanel);
                add(Box.createGlue());
                
                SwingUtilities.invokeLater(REFRESH);
                
            }
            
        });
        
    }
    
    private final JComponent leftBox(JComponent c)
    {
        Box b = new Box(BoxLayout.X_AXIS);
        b.add(c);
        b.add(Box.createHorizontalGlue());
        return b;
    }
    
    protected final void setUp(final IPlayerBridge bridge)
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                s_logger.fine("setup");
                
                setUpSpecific();
                m_bridge = bridge;
                updateMoves();
                
                if (m_listening)
                {
                    throw new IllegalStateException("Not listening");
                }
                m_listening = true;
                
                String key = s_MOVE_PANEL_CANCEL;
                getRootPane().getActionMap().put(key, m_CANCEL_MOVE_ACTION);
                getRootPane().getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), key);
            }
            
        });
        
    }
    /*
     * sub-classes method for set-up
     */
    abstract protected void setUpSpecific();
    
    public final MoveDescription waitForMove(IPlayerBridge bridge)
    {
        setUp(bridge);
        
        waitForRelease();
        
        cleanUp();
        
        MoveDescription rVal = m_moveMessage;
        m_moveMessage = null;
        return rVal;
        
    }
}
