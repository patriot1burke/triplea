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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.QuadCurve2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Draws a route on a map. This code is really ugly, bad and it barely works. It
 * should be rewritten.
 */
public class MapRouteDrawer
{
	// only static methods
	private MapRouteDrawer()
	{
	}
	
	/**
	 * Draw m_route to the screen, do nothing if null.
	 */
	public static void drawRoute(final Graphics2D graphics, final RouteDescription routeDescription, final MapPanel view, final MapData mapData, final String movementLeftForCurrentUnits)
	{
		final AffineTransform original = graphics.getTransform();
		final AffineTransform newTransform = new AffineTransform();
		newTransform.scale(view.getScale(), view.getScale());
		graphics.setTransform(newTransform);
		try
		{
			if (routeDescription == null)
				return;
			final Route route = routeDescription.getRoute();
			if (route == null)
				return;
			graphics.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			graphics.setPaint(Color.red);
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			final List<Territory> territories = route.getAllTerritories();
			final int numTerritories = territories.size();
			final Point[] points = new Point[numTerritories];
			// find all the points for this route
			for (int i = 0; i < numTerritories; i++)
			{
				points[i] = mapData.getCenter(territories.get(i));
			}
			if (routeDescription.getStart() != null)
			{
				points[0] = routeDescription.getStart();
			}
			if (routeDescription.getEnd() != null && numTerritories > 1)
			{
				points[numTerritories - 1] = new Point(routeDescription.getEnd());
			}
			// adjust points for wrapping around the edge
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
			final int yOffset = view.getYOffset();
			final int xOffset = view.getXOffset();
			final List<Shape> shapes = new ArrayList<Shape>();
			for (int i = 0; i < points.length; i++)
			{
				if (i == 0 || i + 1 != points.length)
				{
					final Ellipse2D oval = new Ellipse2D.Double(points[i].x - 3 - xOffset, points[i].y - yOffset - 3, 6, 6);
					shapes.add(oval);
				}
				if (i + 2 < points.length)
				{
					drawCurvedLineWithNextPoint(graphics, points[i].x - xOffset, points[i].y - yOffset, points[i + 1].x - xOffset, points[i + 1].y - yOffset, points[i + 2].x - xOffset,
								points[i + 2].y - yOffset, shapes);
				}
				else if (i + 1 < points.length)
				{
					drawLineSegment(graphics, points[i].x - xOffset, points[i].y - yOffset, points[i + 1].x - xOffset, points[i + 1].y - yOffset, shapes);
				}
			}
			final boolean scrollWrapX = mapData.scrollWrapX();
			final boolean scrollWrapY = mapData.scrollWrapY();
			final double translateX = -view.getImageWidth();
			final double translateY = -view.getImageHeight();
			for (int i = 0; i < shapes.size(); i++)
			{
				final Shape shape = shapes.get(i);
				drawWithTranslate(graphics, shape, 0, 0);
				if (scrollWrapX /*&& !scrollWrapY*/)
				{
					drawWithTranslate(graphics, shape, translateX, 0);
					drawWithTranslate(graphics, shape, -translateX, 0);
				}
				if (/*!scrollWrapX &&*/scrollWrapY)
				{
					drawWithTranslate(graphics, shape, 0, translateY);
					drawWithTranslate(graphics, shape, 0, -translateY);
				}
				if (scrollWrapX && scrollWrapY)
				{
					drawWithTranslate(graphics, shape, translateX, translateY);
					drawWithTranslate(graphics, shape, -translateX, -translateY);
				}
			}
			// draw the length of the move
			if (numTerritories > 1)
			{
				final double textXOffset;
				double cursorXOffset;
				final double xDir = points[numTerritories - 1].x - points[numTerritories - 2].x;
				if (xDir > 0)
				{
					textXOffset = 6;
					cursorXOffset = -10;
				}
				else if (xDir == 0)
				{
					textXOffset = 0;
					cursorXOffset = -5;
				}
				else
				{
					textXOffset = -14;
					cursorXOffset = 0;
				}
				final double textyOffset;
				double cursorYOffset;
				final double yDir = points[numTerritories - 1].y - points[numTerritories - 2].y;
				if (yDir > 0)
				{
					textyOffset = 18;
					cursorYOffset = -8;
				}
				else if (yDir == 0)
				{
					textyOffset = -24;
					cursorYOffset = -2;
				}
				else
				{
					textyOffset = -24;
					cursorYOffset = 0;
				}
				final String textRouteMovement = String.valueOf(numTerritories - 1);
				final String unitMovementLeft = (movementLeftForCurrentUnits == null || movementLeftForCurrentUnits.trim().length() <= 0 ? "" : "    /" + movementLeftForCurrentUnits);
				final BufferedImage movementImage = new BufferedImage(72, 24, BufferedImage.TYPE_INT_ARGB);
				final Graphics2D textG2D = movementImage.createGraphics();
				textG2D.setColor(Color.YELLOW);
				textG2D.setFont(new Font("Dialog", Font.BOLD, 20));
				textG2D.drawString(textRouteMovement, 0, 20);
				textG2D.setColor(new Color(33, 0, 127));
				textG2D.setFont(new Font("Dialog", Font.BOLD, 16));
				textG2D.drawString(unitMovementLeft, 0, 20);
				
				graphics.drawImage(movementImage, (int) (points[numTerritories - 1].x + textXOffset - xOffset), (int) (points[numTerritories - 1].y + textyOffset - yOffset), null);
				if (scrollWrapX) // && !scrollWrapY
				{
					graphics.drawImage(movementImage, (int) (points[numTerritories - 1].x + textXOffset - xOffset + translateX), (int) (points[numTerritories - 1].y + textyOffset - yOffset), null);
					graphics.drawImage(movementImage, (int) (points[numTerritories - 1].x + textXOffset - xOffset - translateX), (int) (points[numTerritories - 1].y + textyOffset - yOffset), null);
				}
				if (scrollWrapY)// &&!scrollWrapX
				{
					graphics.drawImage(movementImage, (int) (points[numTerritories - 1].x + textXOffset - xOffset), (int) (points[numTerritories - 1].y + textyOffset - yOffset + translateY), null);
					graphics.drawImage(movementImage, (int) (points[numTerritories - 1].x + textXOffset - xOffset), (int) (points[numTerritories - 1].y + textyOffset - yOffset - translateY), null);
				}
				if (scrollWrapX && scrollWrapY)
				{
					graphics.drawImage(movementImage, (int) (points[numTerritories - 1].x + textXOffset - xOffset + translateX),
								(int) (points[numTerritories - 1].y + textyOffset - yOffset + translateY), null);
					graphics.drawImage(movementImage, (int) (points[numTerritories - 1].x + textXOffset - xOffset - translateX),
								(int) (points[numTerritories - 1].y + textyOffset - yOffset - translateY), null);
				}
				/* graphics.drawString(text, (float) (points[numTerritories - 1].x + textXOffset - xOffset), (float) (points[numTerritories - 1].y + textyOffset - yOffset));
				if (scrollWrapX) //&& !scrollWrapY
				{
					graphics.drawString(text, (float) (points[numTerritories - 1].x + textXOffset - xOffset + translateX), (float) (points[numTerritories - 1].y + textyOffset - yOffset));
					graphics.drawString(text, (float) (points[numTerritories - 1].x + textXOffset - xOffset - translateX), (float) (points[numTerritories - 1].y + textyOffset - yOffset));
				}
				if (scrollWrapY)// &&!scrollWrapX
				{
					graphics.drawString(text, (float) (points[numTerritories - 1].x + textXOffset - xOffset), (float) (points[numTerritories - 1].y + textyOffset - yOffset + translateY));
					graphics.drawString(text, (float) (points[numTerritories - 1].x + textXOffset - xOffset), (float) (points[numTerritories - 1].y + textyOffset - yOffset - translateY));
				}
				if (scrollWrapX && scrollWrapY)
				{
					graphics.drawString(text, (float) (points[numTerritories - 1].x + textXOffset - xOffset + translateX), (float) (points[numTerritories - 1].y + textyOffset - yOffset + translateY));
					graphics.drawString(text, (float) (points[numTerritories - 1].x + textXOffset - xOffset - translateX), (float) (points[numTerritories - 1].y + textyOffset - yOffset - translateY));
				}*/
				
				final Image cursorImage = routeDescription.getCursorImage();
				if (cursorImage != null)
				{
					graphics.drawImage(cursorImage, (int) (points[numTerritories - 1].x + cursorXOffset - xOffset), (int) (points[numTerritories - 1].y + cursorYOffset - yOffset), null);
					if (scrollWrapX /*&& !scrollWrapY*/)
					{
						graphics.drawImage(cursorImage, (int) (points[numTerritories - 1].x + cursorXOffset - xOffset + translateX), (int) (points[numTerritories - 1].y + cursorYOffset - yOffset),
									null);
						graphics.drawImage(cursorImage, (int) (points[numTerritories - 1].x + cursorXOffset - xOffset - translateX), (int) (points[numTerritories - 1].y + cursorYOffset - yOffset),
									null);
					}
					if (/*!scrollWrapX &&*/scrollWrapY)
					{
						graphics.drawImage(cursorImage, (int) (points[numTerritories - 1].x + cursorXOffset - xOffset), (int) (points[numTerritories - 1].y + cursorYOffset - yOffset + translateY),
									null);
						graphics.drawImage(cursorImage, (int) (points[numTerritories - 1].x + cursorXOffset - xOffset), (int) (points[numTerritories - 1].y + cursorYOffset - yOffset - translateY),
									null);
					}
					if (scrollWrapX && scrollWrapY)
					{
						graphics.drawImage(cursorImage, (int) (points[numTerritories - 1].x + cursorXOffset - xOffset + translateX),
									(int) (points[numTerritories - 1].y + cursorXOffset - yOffset + translateY), null);
						graphics.drawImage(cursorImage, (int) (points[numTerritories - 1].x + cursorXOffset - xOffset - translateX),
									(int) (points[numTerritories - 1].y + cursorXOffset - yOffset - translateY), null);
					}
				}
			}
		} finally
		{
			graphics.setTransform(original);
		}
	}
	
	private static void drawWithTranslate(final Graphics2D graphics, final Shape shape, final double translateX, final double translateY)
	{
		if (shape instanceof Ellipse2D.Double)
		{
			Ellipse2D.Double elipse = (Ellipse2D.Double) shape;
			elipse = new Ellipse2D.Double(elipse.x + translateX, elipse.y + translateY, elipse.width, elipse.height);
			graphics.draw(elipse);
		}
		if (shape instanceof Polygon)
		{
			((Polygon) shape).translate((int) translateX, (int) translateY);
			graphics.fill(shape);
			((Polygon) shape).translate((int) -translateX, (int) -translateY);
		}
		if (shape instanceof Line2D)
		{
			final Line2D line = (Line2D) shape;
			final Point2D p1 = new Point2D.Double(line.getP1().getX() + translateX, line.getP1().getY() + translateY);
			final Point2D p2 = new Point2D.Double(line.getP2().getX() + translateX, line.getP2().getY() + translateY);
			graphics.draw(new Line2D.Double(p1, p2));
		}
		if (shape instanceof QuadCurve2D)
		{
			QuadCurve2D.Double curve = (QuadCurve2D.Double) shape;
			curve = new QuadCurve2D.Double(curve.x1 + translateX, curve.y1 + translateY, curve.ctrlx + translateX, curve.ctrly + translateY, curve.x2 + translateX, curve.y2 + translateY);
			graphics.draw(curve);
		}
	}
	
	/**
	 * (x,y) - the first point to draw from (xx, yy) - the point to draw too
	 * (xxx, yyy) - the next point that the line segment will be drawn to
	 */
	private static void drawCurvedLineWithNextPoint(final Graphics2D graphics, final double x, final double y, final double xx, final double yy, final double xxx, final double yyy,
				final List<Shape> shapes)
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
			} catch (final ArithmeticException ex)
			{
				ratio = 1000;
			}
			if (Math.abs(controlDiffx) > Math.abs(controlDiffy))
			{
				controlDiffx = controlDiffx < 0 ? -maxControlLength : maxControlLength;
				controlDiffy = controlDiffy < 0 ? (int) (-maxControlLength / ratio) : (int) (maxControlLength / ratio);
			}
			else
			{
				controlDiffy = controlDiffy < 0 ? -maxControlLength : maxControlLength;
				controlDiffx = controlDiffx < 0 ? (int) (-maxControlLength * ratio) : (int) (maxControlLength * ratio);
			}
		}
		final double controlx = xx + controlDiffx;
		final double controly = yy + controlDiffy;
		final QuadCurve2D.Double curve = new QuadCurve2D.Double(x, y, controlx, controly, xx, yy);
		shapes.add(curve);
	}
	
	// http://www.experts-exchange.com/Programming/Programming_Languages/Java/Q_20627343.html
	private static void drawLineSegment(final Graphics2D graphics, final int x, final int y, final int xx, final int yy, final List<Shape> shapes)
	{
		final double arrowWidth = 12.0f;
		final double theta = 0.7f;
		final int[] xPoints = new int[3];
		final int[] yPoints = new int[3];
		final int[] vecLine = new int[2];
		final int[] vecLeft = new int[2];
		double fLength;
		double th;
		double ta;
		double baseX, baseY;
		xPoints[0] = xx;
		yPoints[0] = yy;
		// build the line vector
		vecLine[0] = xPoints[0] - x;
		vecLine[1] = yPoints[0] - y;
		// build the arrow base vector - normal to the line
		vecLeft[0] = -vecLine[1];
		vecLeft[1] = vecLine[0];
		// setup length parameters
		fLength = Math.sqrt(vecLine[0] * vecLine[0] + vecLine[1] * vecLine[1]);
		th = arrowWidth / (2.0f * fLength);
		ta = arrowWidth / (2.0f * (Math.tan(theta) / 2.0f) * fLength);
		// find the base of the arrow
		baseX = (xPoints[0] - ta * vecLine[0]);
		baseY = (yPoints[0] - ta * vecLine[1]);
		// build the points on the sides of the arrow
		xPoints[1] = (int) (baseX + th * vecLeft[0]);
		yPoints[1] = (int) (baseY + th * vecLeft[1]);
		xPoints[2] = (int) (baseX - th * vecLeft[0]);
		yPoints[2] = (int) (baseY - th * vecLeft[1]);
		// draw an arrow
		final Shape line = new Line2D.Double(x, y, (int) baseX, (int) baseY);
		shapes.add(line);
		// TODO - put this back
		final Polygon poly = new Polygon(xPoints, yPoints, 3);
		shapes.add(poly);
	}
}
