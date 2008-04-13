/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package games.strategy.triplea.ui.screen;

import games.strategy.engine.data.GameData;
import games.strategy.thread.LockUtil;
import games.strategy.triplea.ui.MapData;
import games.strategy.triplea.util.Stopwatch;
import games.strategy.ui.Util;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.lang.ref.SoftReference;
import java.util.*;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.*;

/**
 * 
 *
 *
 * @author Sean Bridges
 */
public class Tile
{
    private static final boolean DRAW_DEBUG = false;
    private static final Logger s_logger = Logger.getLogger(Tile.class.getName());
    
    //allow the gc to implement memory management
    private SoftReference<Image> m_imageRef;
    private boolean m_isDirty = true;
    private final Rectangle m_bounds;
    private final int m_x;
    private final int m_y;
    
    private final double m_scale;
    
    private final Lock m_lock = new ReentrantLock();
    
    private final List<IDrawable> m_contents = new ArrayList<IDrawable>();
    
    public Tile(final Rectangle bounds, int x, int y, final double scale)
    {
        //s_logger.log(Level.FINER, "Tile created for:" + bounds);
        m_bounds = bounds;
        m_x = x;
        m_y = y;
        m_scale = scale;
        
    }
     
    public boolean isDirty()
    {
        LockUtil.acquireLock(m_lock);
        try
        {
            return m_isDirty || m_imageRef == null || m_imageRef.get() == null;
        }
        finally 
        {
            LockUtil.releaseLock(m_lock);
        }
    }
    
    public Image getImage(GameData data, MapData mapData) 
    {
        LockUtil.acquireLock(m_lock) ;
        try
        {
        
            if(m_imageRef == null)
            {
                m_imageRef = new SoftReference<Image>(createBlankImage());
                m_isDirty = true;
            }
            
            Image image = m_imageRef.get();
            if(image == null)
            {
                image = createBlankImage();
                m_imageRef = new SoftReference<Image>(image);
                m_isDirty = true;
            }
            
            if(m_isDirty)
            {
                Graphics2D g = (Graphics2D) image.getGraphics();
                
                draw(g, data, mapData);
                g.dispose();
            }
            
            return image;
        }
        finally 
        {
            LockUtil.releaseLock(m_lock);
        }

        
    }

    private BufferedImage createBlankImage()
    {
        return Util.createImage((int) ( m_bounds.getWidth() * m_scale), (int) (m_bounds.getHeight() * m_scale), false);
    }
    
    
    /**
     * This image may be null, and it may not reflect our current drawables.  Use getImage() to get
     * a correct image
     * 
     * @return the image we currently have.
     * 
     */
    public Image getRawImage()
    {
        if(m_imageRef == null)
            return null;
        return m_imageRef.get();
    }
    
    private void draw(Graphics2D g, GameData data, MapData mapData)
    {
        AffineTransform unscaled = g.getTransform();
        AffineTransform scaled;
        if(m_scale != 1)
        {
            scaled = new AffineTransform();
            scaled.scale(m_scale, m_scale);
            g.setTransform(scaled);
        }
        else
        {
            scaled = unscaled;
        }
        
        
        Stopwatch stopWatch = new Stopwatch(s_logger, Level.FINEST, "Drawing Tile at" + m_bounds);
        
        //clear
        g.setColor(Color.BLACK);
        g.fill(new Rectangle(0,0,TileManager.TILE_SIZE, TileManager.TILE_SIZE));
     
        Collections.sort(m_contents, new DrawableComparator());
        Iterator<IDrawable> iter = m_contents.iterator();
    
        while (iter.hasNext())
        {
            IDrawable drawable = iter.next();
            drawable.draw(m_bounds, data, g, mapData, unscaled, scaled);
        }
        m_isDirty = false;
        
        
        //draw debug graphics
        if(DRAW_DEBUG)
        {
            g.setColor(Color.PINK);
            Rectangle r = new Rectangle(1,1,TileManager.TILE_SIZE - 2, TileManager.TILE_SIZE -2);
            g.setStroke(new BasicStroke(1));
            g.draw(r);
            g.setFont(new Font("Ariel", Font.BOLD, 25));
            g.drawString(m_x + " " + m_y, 40,40);
        }
        
        stopWatch.done();
        
    }
    
    public void addDrawables(Collection<IDrawable> drawables)
    {
        LockUtil.acquireLock(m_lock);
        try
        {
            m_contents.addAll(drawables);
            m_isDirty = true;
        }
        finally 
        {
            LockUtil.releaseLock(m_lock);
        }

    }
    
    public void addDrawable(IDrawable d)
    {
        LockUtil.acquireLock(m_lock); 
        try
        {
            m_contents.add(d);
            m_isDirty = true;
        }
        finally 
        {
            LockUtil.releaseLock(m_lock);
        }

    }
    
    public void removeDrawable(IDrawable d)
    {
        LockUtil.acquireLock(m_lock);
        try
        {
            m_contents.remove(d);
            m_isDirty = true;
        }
        finally 
        {
            LockUtil.releaseLock(m_lock);
        }
    }
    
    public void removeDrawables(Collection c)
    {
        LockUtil.acquireLock(m_lock); 
        try
        {
            m_contents.removeAll(c);
            m_isDirty = true;
        }
        finally 
        {
            LockUtil.releaseLock(m_lock);
        }

    }
    
    public void clear()
    {
        LockUtil.acquireLock(m_lock); 
        try
        {
            m_contents.clear();
            m_isDirty = true;
        }
        finally 
        {
            LockUtil.releaseLock(m_lock);
        }

    }
    
    public List<IDrawable> getDrawables()
    {
        LockUtil.acquireLock(m_lock);
        try
        {      
            return new ArrayList<IDrawable>( m_contents);
        }
        finally 
        {
            LockUtil.releaseLock(m_lock);
        }

    }
    
    public Rectangle getBounds()
    {
        return m_bounds;
    }
    
    public int getX()
    {
        return m_x;
    }
    
    public int getY()
    {
        return m_y;
    }

    public Lock getLock()
    {
        return m_lock;
    }

    
}
