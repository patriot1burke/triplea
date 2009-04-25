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

package util.image;

import games.strategy.engine.framework.GameRunner;
import games.strategy.triplea.ui.MapData;
import games.strategy.util.PointFileReaderWriter;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;
import javax.swing.JPanel;


public class AutoPlacementFinder
{

    private static int PLACEWIDTH  = 46;
    private static int PLACEHEIGHT = 46;
    private static MapData s_mapData;

    private static double percent;
    public static void main(String[] args)
    {
        if(args.length == 1)
        {
            percent = Double.parseDouble(args[0]);
            PLACEHEIGHT = (int) (percent * PLACEHEIGHT);
            PLACEWIDTH = (int) (percent * PLACEWIDTH);
        }
        calculate();
    }
    
    
    /**
       calculate()
       
       Will calculate the placements on the map automatically.
    
    */
    static void calculate()
    {
        Map<String, Collection> m_placements = new HashMap<String, Collection>();      //create hash map of placements
	String mapDir = getMapDirectory();  //ask user where the map is
	if(percent == 0)
    {
        try
        {
            File file = new File(GameRunner.getRootFolder() + File.separator + "maps" + File.separator + mapDir + File.separator + "map.properties");
            if(file.exists())
            {
                FileReader reader = new FileReader(file);
                LineNumberReader reader2 = new LineNumberReader(reader);
                int i = 0;
                while(true)
                {
                    reader2.setLineNumber(i);
                    String line = reader2.readLine();
                    if(line == null)
                        break;
                    if(line.contains("units.scale="))
                    {
                        int result = JOptionPane.showConfirmDialog(new JPanel(), "A map.properties file was found in the map's folder, do you want to use the file to supply the unit's scale?", "File Suggestion", 1);
                        if(result == 2)
                            return;
                        if(result == 0)
                        {
                            percent = Double.parseDouble(line.substring(line.indexOf("units.scale=") + 12).trim());
                            PLACEHEIGHT = (int) (percent * PLACEHEIGHT);
                            PLACEWIDTH = (int) (percent * PLACEWIDTH);
                            break;
                        }
                    }
                    i++;
                }
            }
        }
        catch(Exception ex){}
    }
    if(percent == 0)
    {
        try
        {
            String result = getUnitsScale();
            percent = Double.parseDouble(result.toLowerCase());
            PLACEHEIGHT = (int) (percent * PLACEHEIGHT);
            PLACEWIDTH = (int) (percent * PLACEWIDTH);
        }
        catch(Exception ex){}
    }
	if(mapDir == null)
        {
	    System.out.println("You need to specify a map name for this to work");
	    System.out.println("Shutting down");
	    System.exit(0);
	}
	
	
	try
	{
        s_mapData = new MapData(mapDir);       //makes TripleA read all the text data files for the map.
	}
	catch(NullPointerException npe)
	{
	    System.out.println("Caught Null Pointer Exception.");
	    System.out.println("Could be due to some missing text files");
	    npe.printStackTrace();
	    System.exit(0);
	}
	
	Iterator terrIter = s_mapData.getTerritories().iterator();
	
	System.out.println("Calculating, this may take a while...");
	
        while (terrIter.hasNext())
        {
            String name = (String)terrIter.next();
            List<Point> points;
	    
            if(s_mapData.hasContainedTerritory(name))
            {
                Set<Polygon> containedPolygons = new HashSet<Polygon>();
                Iterator containedIter = s_mapData.getContainedTerritory(name).iterator();
		
                while (containedIter.hasNext())
		{
                    String containedName = (String)containedIter.next();
                    containedPolygons.addAll(s_mapData.getPolygons(containedName));
                }

                points = getPlacementsStartingAtTopLeft(s_mapData.getPolygons(name),
                        s_mapData.getBoundingRect(name),
                        s_mapData.getCenter(name),
                        containedPolygons);
                m_placements.put(name, points);
            }
            else
            {
                points = getPlacementsStartingAtMiddle(s_mapData.getPolygons(name),
                        s_mapData.getBoundingRect(name),
                        s_mapData.getCenter(name));
                        m_placements.put(name, points);
            }
	    
            System.out.println(name + ": " + points.size());
        
	}//while

        try
        {
	    String fileName = new FileSave("Where To Save place.txt ?","place.txt").getPathString();
	    
	    if (fileName == null)
	    {
	        System.out.println("You chose not to save, Shutting down");
                System.exit(0);
	    }
	    
            PointFileReaderWriter.writeOneToMany(new FileOutputStream(fileName), m_placements);
	    System.out.println("Data written to :" + new File(fileName).getCanonicalPath());
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
	    System.exit(0);
	    
        }
	
	System.exit(0); //shut down program when done.
    }


   /**
	  we need the exact map name as indicated in the XML game file
	  ie. "revised" "classic" "pact_of_steel"
	  of course, without the quotes.
    */
    private static String getMapDirectory()
    {
	String mapDir = JOptionPane.showInputDialog(null, "Enter the map name (ie. folder name)");
	    
	if(mapDir != null)
	{
	    return mapDir;
	}
	else
	{
            return null;
        }
    }
    private static String getUnitsScale()
    {
        String unitsScale = JOptionPane.showInputDialog(null, "Enter the unit's scale (e.g. 0.5625)");

        if(unitsScale != null)
        {
            return unitsScale;
        }
        else
        {
            return null;
        }
    }

    /**
       java.util.List getPlacementsStartingAtMiddle(java.util.Collection, java.awt.Rectangle, java.awt.Point)
       
       @param java.util.Collection
       @param java.awt.Rectangle
       @param java.awt.Point
       
       @return java.util.List
    */
    static List<Point> getPlacementsStartingAtMiddle(Collection<Polygon> countryPolygons, Rectangle bounding, Point center)
    {
        List<Rectangle2D> placementRects  = new ArrayList<Rectangle2D>();
        List<Point> placementPoints = new ArrayList<Point>();

        Rectangle2D place = new Rectangle2D.Double(center.x, center.y, PLACEHEIGHT, PLACEWIDTH);
        int x = center.x - (PLACEHEIGHT / 2);
        int y = center.y - (PLACEWIDTH / 2);
        int step = 1;

        for(int i = 0; i < 2 * Math.max(bounding.width , bounding.height ); i++)
        {
            for(int j = 0; j < Math.abs(step); j++)
            {
                if (step > 0)
		{
                    x++;
                }
		else
		{
                    x--;
                }
		isPlacement(countryPolygons, Collections.<Polygon>emptySet(), placementRects, placementPoints, place, x, y);
            }
	    
            for(int j = 0; j < Math.abs(step); j++)
            {
                if (step > 0)
		{
                    y++;
                }
		else
		{
                    y--;
                }
		isPlacement(countryPolygons, Collections.<Polygon>emptySet(), placementRects, placementPoints, place, x, y);
            }

            step = -step;
	    
            if (step > 0)
	    {
                step++;
            }
	    else
	    { 
                step--;
            }
	    
            //System.out.println("x:" + x + " y:" + y);  // For Debugging
        }


        if(placementPoints.isEmpty())
        {
            int defaultx =center.x - (PLACEHEIGHT / 2);
            int defaulty = center.y - (PLACEWIDTH / 2);
            placementPoints.add(new Point(defaultx, defaulty));
        }

        return placementPoints;
    }



    /**
       java.util.List getPlacementsStartingAtTopLeft(java.util.Collection, java.awt.Rectangle, java.awt.Point, java.util.Collection)
       
       @param java.util.Collection
       @param java.awt.Rectangle
       @param java.awt.Point
       @param java.util.Collection
   
       @return java.util.List
    */
    static List<Point> getPlacementsStartingAtTopLeft(Collection<Polygon> countryPolygons,  Rectangle bounding, Point center,  Collection<Polygon> containedCountryPolygons)
    {
        List<Rectangle2D> placementRects = new ArrayList<Rectangle2D>();
        List<Point> placementPoints = new ArrayList<Point>();

        Rectangle2D place = new Rectangle2D.Double(center.x, center.y, PLACEHEIGHT, PLACEWIDTH);
	
        for(int x = bounding.x; x  < bounding.width + bounding.x; x++)
        {
            for (int y = bounding.y; y < bounding.height + bounding.y; y++)
            {
                isPlacement(countryPolygons, containedCountryPolygons, placementRects, placementPoints, place, x, y);
            }
        }

        if(placementPoints.isEmpty())
        {
            int defaultx =center.x - (PLACEHEIGHT / 2);
            int defaulty = center.y - (PLACEWIDTH / 2);
            placementPoints.add(new Point(defaultx, defaulty));
        }

        return placementPoints;
    }



    /**
       isPlacement(java.util.Collection, java.util.Collection, java.util.List, java.util.List, java.awt.geom.Rectangle2D, java.lang.int, java.lang.int)
       
      @param java.util.Collection countryPolygons
      @param java.util.Collection containedCountryPolygons  polygons of countries contained with ourselves
      @param java.util.List placementRects
      @param java.util.List placementPoints 
      @param java.awt.geom.Rectangle2D place
      @param java.lang.int x 
      @param java.lang.int y
    */
    private static void isPlacement(Collection<Polygon> countryPolygons, Collection<Polygon> containedCountryPolygons,  List<Rectangle2D> placementRects, List<Point> placementPoints, Rectangle2D place, int x, int y)
    {
        place.setFrame(x,y,PLACEWIDTH, PLACEHEIGHT);
        if(containedIn(place, countryPolygons) &&
           ! intersectsOneOf(place, placementRects) &&
	   
           //make sure it is not in or intersects the contained country
	   
           (!containedIn(place, containedCountryPolygons) && ! intersectsOneOf(place, containedCountryPolygons))
         )
	{
            placementPoints.add(new Point( (int) place.getX(), (int) place.getY()));
            Rectangle2D newRect = new Rectangle2D.Double();
            newRect.setFrame(place);
            placementRects.add(newRect);
        
	}//if
    }



   /**
       java.lang.boolean containedIn(java.awt.geom.Rectangle2D, java.util.Collection)
       
       Function to test if the given 2D rectangle
       is contained in any of the given shapes
       in the collection.
       
       @param java.awt.geom.Rectangle2D r
       @param java.util.Collection      shapes
    
    */
    public static boolean containedIn(Rectangle2D r, Collection<Polygon> shapes)
    {
        Iterator<Polygon> iter = shapes.iterator();
	
        while (iter.hasNext())
        {
            Shape item = iter.next();
	    
            if(item.contains(r))
	    {
                return true;
	    }
        }
	
        return false;
    }



    /**
       java.lang.boolean intersectsOneOf(java.awt.geom.Rectangle2D, java.util.Collection)
       
       Function to test if the given 2D rectangle
       intersects any of the shapes given in the
       collection.
       
       @param java.awt.geom.Rectangle2D r
       @param java.util.Collection      shapes
    
    */
    public static boolean intersectsOneOf(Rectangle2D r, Collection<? extends Shape> shapes)
    {
        if (shapes.isEmpty())
        {
            return false;
        }

        Iterator<? extends Shape> iter = shapes.iterator();

        while (iter.hasNext())
        {
            Shape item = iter.next();

            if (item.intersects(r))
            {
                return true;
            }
        }

        return false;
    }

}//end class AutoPlacementFinder
