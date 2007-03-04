/*
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

/*
 * Logic to draw a route on a map.
 */

package games.strategy.triplea.ui;

import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;

import java.awt.Image;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.QuadCurve2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Draws a route on a map. This code is really ugly, bad and it barely works. It
 * should be rewritten.
 */

public class MapRouteDrawer
{
     //only static methods
    private MapRouteDrawer()
    {
    }

    /**
     * Draw m_route to the screen, do nothing if null.
     */
    public static void drawRoute(Graphics2D graphics, RouteDescription routeDescription, MapPanel view, MapData mapData)
    {
        
        AffineTransform original = graphics.getTransform();
        AffineTransform newTransform = new AffineTransform();
        newTransform.scale(view.getScale(), view.getScale());
        graphics.setTransform(newTransform);
        try
        {
        
            if(routeDescription == null)
                return;
            
            Route route = routeDescription.getRoute();
    
            if (route == null)
                return;
    
            graphics.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            graphics.setPaint(Color.red);
    
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    
            List territories = route.getTerritories();
    
            int numTerritories = territories.size();
            Point[] points = new Point[numTerritories];
            
            
            //find all the points for this route
            for (int i = 0; i < numTerritories; i++)
            {
                points[i] = (Point) mapData.getCenter((Territory) territories.get(i));
            }
    
            if(routeDescription.getStart() != null)
            {
                points[0] = routeDescription.getStart();
            }
            if(routeDescription.getEnd() != null && numTerritories > 1)
            {
                points[numTerritories -1] =  new Point(routeDescription.getEnd());
            }
    
            
            
            //adjust points for wrapping around the edge
            for (int i = 1; i < points.length; i++)
            {
                if (Math.abs(points[i].x - points[i - 1].x) > view.getImageWidth() / 2)
                {
                    if (points[i].x < points[i - 1].x)
                        points[i].x += view.getImageWidth();
                    else
                        points[i].x -= view.getImageWidth();
                }
            }
    
            int yOffset = view.getYOffset();
            int xOffset = view.getXOffset();
    
            List<Shape> shapes = new ArrayList<Shape>();
    
            for (int i = 0; i < points.length; i++)
            {
                if (i == 0 ||  i + 1 != points.length)
                {
                    Ellipse2D oval = new Ellipse2D.Double(points[i].x - 3 - xOffset, points[i].y - yOffset - 3, 6, 6);
                    shapes.add(oval);
                }
    
                if (i + 2 < points.length)
                {
                    drawCurvedLineWithNextPoint(graphics, points[i].x - xOffset, points[i].y - yOffset, points[i + 1].x - xOffset, points[i + 1].y - yOffset, points[i + 2].x - xOffset, points[i + 2].y - yOffset, shapes);
                } else if (i + 1 < points.length)
                {
                    drawLineSegment(graphics, points[i].x - xOffset, points[i].y - yOffset, points[i + 1].x - xOffset, points[i + 1].y - yOffset, shapes);
                }
    
            }
            double translate = -view.getImageWidth();
    
            for (int i = 0; i < shapes.size(); i++)
            {
                Shape shape = shapes.get(i);
    
                drawWithTranslate(graphics, shape, 0);
                
                if(mapData.scrollWrapX() )
                {
                    drawWithTranslate(graphics, shape, translate);
                    drawWithTranslate(graphics, shape, -translate);
                }
            }
    
            //draw the length of the move
            if (numTerritories > 1)
            {
    
                double textXOffset;
                double xDir = points[numTerritories - 1].x - points[numTerritories - 2].x;
                if (xDir > 0)
                    textXOffset = 2;
                else if (xDir == 0)
                    textXOffset = 0;
                else
                    textXOffset = -8;
    
                double textyOffset;
                double yDir = points[numTerritories - 1].y - points[numTerritories - 2].y;
                if (yDir > 0)
                    textyOffset = 2;
                else if (yDir == 0)
                    textyOffset = 0;
                else
                    textyOffset = -8;
    
                graphics.setColor(Color.YELLOW);
                graphics.setFont(new Font("Dialog", Font.BOLD, 18));
                String text = String.valueOf(numTerritories - 1);
                graphics.drawString(text, (float) (points[numTerritories - 1].x + textXOffset - xOffset), (float) (points[numTerritories - 1].y + textyOffset - yOffset));
                
                if(mapData.scrollWrapX())
                {
                    graphics.drawString(text, (float) (points[numTerritories - 1].x + textXOffset - xOffset + translate), (float) (points[numTerritories - 1].y + textyOffset - yOffset));
                    graphics.drawString(text, (float) (points[numTerritories - 1].x + textXOffset - xOffset - translate), (float) (points[numTerritories - 1].y + textyOffset - yOffset));
                }

                Image cursorImage = routeDescription.getCursorImage();
                if (cursorImage != null) 
                {
                    graphics.drawImage(cursorImage, (int) (points[numTerritories - 1].x + textXOffset - xOffset), (int) (points[numTerritories - 1].y + textyOffset - yOffset), null);
                    if(mapData.scrollWrapX())
                    {
                        graphics.drawImage(cursorImage, (int) (points[numTerritories - 1].x + textXOffset - xOffset + translate), (int) (points[numTerritories - 1].y + textyOffset - yOffset), null);
                        graphics.drawImage(cursorImage, (int) (points[numTerritories - 1].x + textXOffset - xOffset - translate), (int) (points[numTerritories - 1].y + textyOffset - yOffset), null);
                    }
                }
            }
        }
        finally
        {
            graphics.setTransform(original);
        }
    }

    private static void drawWithTranslate(Graphics2D graphics, Shape shape, double translate)
    {
        
        
        if (shape instanceof Ellipse2D.Double)
        {
            Ellipse2D.Double elipse = (Ellipse2D.Double) shape;
            elipse = new Ellipse2D.Double(elipse.x + translate, elipse.y, elipse.width, elipse.height);
            graphics.draw(elipse);
        }
        if (shape instanceof Polygon)
        {
            ((Polygon) shape).translate((int) translate, 0);
            graphics.fill(shape);

            ((Polygon) shape).translate((int)-translate, 0);
        }
        if (shape instanceof Line2D)
        {
            Line2D line = (Line2D) shape;
            Point2D p1 = new Point2D.Double(line.getP1().getX() + translate, line.getP1().getY());
            Point2D p2 = new Point2D.Double(line.getP2().getX() + translate, line.getP2().getY());
            graphics.draw(new Line2D.Double(p1, p2));
        }
        if (shape instanceof QuadCurve2D)
        {
            QuadCurve2D.Double curve = (QuadCurve2D.Double) shape;
            curve = new QuadCurve2D.Double(curve.x1 + translate, curve.y1, curve.ctrlx + translate, curve.ctrly, curve.x2 + translate, curve.y2);
            graphics.draw(curve);

        }
    }

    /**
     * (x,y) - the first point to draw from (xx, yy) - the point to draw too
     * (xxx, yyy) - the next point that the line segment will be drawn to
     */
    private static void drawCurvedLineWithNextPoint(Graphics2D graphics, double x, double y, double xx, double yy, double xxx, double yyy, List<Shape> shapes)
    {
        final int maxControlLength = 150;
        double controlDiffx = xx - xxx;
        double controlDiffy = yy - yyy;

        if (Math.abs(controlDiffx) > maxControlLength || Math.abs(controlDiffy) > maxControlLength)
        {
            double ratio = 0.0;
            try
            {
                ratio = Math.abs(controlDiffx / controlDiffy);
            } catch (ArithmeticException ex)
            {
                ratio = 1000;
            }

            if (Math.abs(controlDiffx) > Math.abs(controlDiffy))
            {
                controlDiffx = controlDiffx < 0 ? -maxControlLength : maxControlLength;
                controlDiffy = controlDiffy < 0 ? (int) (-maxControlLength / ratio) : (int) (maxControlLength / ratio);
            } else
            {
                controlDiffy = controlDiffy < 0 ? -maxControlLength : maxControlLength;
                controlDiffx = controlDiffx < 0 ? (int) (-maxControlLength * ratio) : (int) (maxControlLength * ratio);

            }

        }

        double controlx = xx + controlDiffx;
        double controly = yy + controlDiffy;

        QuadCurve2D.Double curve = new QuadCurve2D.Double(x, y, controlx, controly, xx, yy);
        shapes.add(curve);
    }

    //http://www.experts-exchange.com/Programming/Programming_Languages/Java/Q_20627343.html
    private static void drawLineSegment(Graphics2D graphics, int x, int y, int xx, int yy, List<Shape> shapes)
    {
        double arrowWidth = 12.0f;
        double theta = 0.7f;
        int[] xPoints = new int[3];
        int[] yPoints = new int[3];
        int[] vecLine = new int[2];
        int[] vecLeft = new int[2];
        double fLength;
        double th;
        double ta;
        double baseX, baseY;

        xPoints[0] = xx;
        yPoints[0] = yy;

        // build the line vector
        vecLine[0] = (int) xPoints[0] - x;
        vecLine[1] = (int) yPoints[0] - y;

        // build the arrow base vector - normal to the line
        vecLeft[0] = -vecLine[1];
        vecLeft[1] = vecLine[0];

        // setup length parameters
        fLength = (double) Math.sqrt(vecLine[0] * vecLine[0] + vecLine[1] * vecLine[1]);
        th = arrowWidth / (2.0f * fLength);
        ta = arrowWidth / (2.0f * ((double) Math.tan(theta) / 2.0f) * fLength);

        // find the base of the arrow
        baseX = (xPoints[0] - ta * vecLine[0]);
        baseY = (yPoints[0] - ta * vecLine[1]);

        // build the points on the sides of the arrow
        xPoints[1] = (int) (baseX + th * vecLeft[0]);
        yPoints[1] = (int) (baseY + th * vecLeft[1]);
        xPoints[2] = (int) (baseX - th * vecLeft[0]);
        yPoints[2] = (int) (baseY - th * vecLeft[1]);

        //draw an arrow
        Shape line = new Line2D.Double(x, y, (int) baseX, (int) baseY);
        shapes.add(line);

        //TODO - put this back
        Polygon poly = new Polygon( xPoints,  yPoints, 3);
        shapes.add(poly);
    }

}
