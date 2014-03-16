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
package games.strategy.triplea.ui.screen;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.image.MapImage;
import games.strategy.triplea.image.TileImageFactory;
import games.strategy.triplea.ui.IUIContext;
import games.strategy.triplea.ui.MapData;
import games.strategy.triplea.util.Stopwatch;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Sean Bridges
 */
public interface IDrawable
{
	public Logger s_logger = Logger.getLogger(IDrawable.class.getName());
	public static final int BASE_MAP_LEVEL = 1;
	public static final int POLYGONS_LEVEL = 2;
	public static final int RELIEF_LEVEL = 3;
	public static final int OPTIONAL_EXTRA_TERRITORY_BORDERS_MEDIUM_LEVEL = 4;
	public static final int OPTIONAL_EXTRA_TERRITORY_BORDERS_HIGH_LEVEL = 18;
	public static final int CONVOY_LEVEL = 5;
	public static final int TERRITORY_EFFECT_LEVEL = 6;
	public static final int CAPITOL_MARKER_LEVEL = 8;
	public static final int VC_MARKER_LEVEL = 9;
	public static final int DECORATOR_LEVEL = 11;
	public static final int TERRITORY_TEXT_LEVEL = 13;
	public static final int BATTLE_HIGHLIGHT_LEVEL = 14;
	public static final int UNITS_LEVEL = 15;
	public static final int TERRITORY_OVERLAY_LEVEL = 16;
	
	
	/**
	 * This is for the optional extra territory borders. LOW means off
	 */
	public static enum OptionalExtraBorderLevel
	{
		LOW, MEDIUM, HIGH
	}
	
	/**
	 * Draw the tile
	 * 
	 * If the graphics are scaled, then unscaled and scaled will be non null.
	 * <p>
	 * 
	 * The affine transform will be set to the scaled version.
	 * 
	 * 
	 */
	public void draw(Rectangle bounds, GameData data, Graphics2D graphics, MapData mapData, AffineTransform unscaled, AffineTransform scaled);
	
	public int getLevel();
}


class DrawableComparator implements Comparator<IDrawable>
{
	public int compare(final IDrawable o1, final IDrawable o2)
	{
		return o1.getLevel() - o2.getLevel();
	}
}


class TerritoryNameDrawable implements IDrawable
{
	private final String m_territoryName;
	private final IUIContext m_uiContext;
	
	public TerritoryNameDrawable(final String territoryName, final IUIContext context)
	{
		this.m_territoryName = territoryName;
		this.m_uiContext = context;
	}
	
	public void draw(final Rectangle bounds, final GameData data, final Graphics2D graphics, final MapData mapData, final AffineTransform unscaled, final AffineTransform scaled)
	{
		final Territory territory = data.getMap().getTerritory(m_territoryName);
		final TerritoryAttachment ta = TerritoryAttachment.get(territory);
		final boolean drawFromTopLeft = mapData.drawNamesFromTopLeft();
		final boolean showSeaNames = mapData.drawSeaZoneNames();
		final boolean showComments = mapData.drawComments();
		boolean drawComments = false;
		String commentText = null;
		if (territory.isWater())
		{
			// this is for special comments, like convoy zones, etc.
			if (ta != null && showComments)
			{
				if (ta.getConvoyRoute() && ta.getProduction() > 0 && ta.getOriginalOwner() != null)
				{
					drawComments = true;
					if (ta.getConvoyAttached().isEmpty())
						commentText = MyFormatter.defaultNamedToTextList(TerritoryAttachment.getWhatTerritoriesThisIsUsedInConvoysFor(territory, data)) + " " + ta.getOriginalOwner().getName()
									+ " Blockade Route";
					else
						commentText = MyFormatter.defaultNamedToTextList(ta.getConvoyAttached()) + " " + ta.getOriginalOwner().getName() + " Convoy Route";
				}
				else if (ta.getConvoyRoute())
				{
					drawComments = true;
					if (ta.getConvoyAttached().isEmpty())
						commentText = MyFormatter.defaultNamedToTextList(TerritoryAttachment.getWhatTerritoriesThisIsUsedInConvoysFor(territory, data)) + " Blockade Route";
					else
						commentText = MyFormatter.defaultNamedToTextList(ta.getConvoyAttached()) + " Convoy Route";
				}
				else if (ta.getProduction() > 0 && ta.getOriginalOwner() != null)
				{
					drawComments = true;
					final PlayerID originalOwner = ta.getOriginalOwner();
					commentText = originalOwner.getName() + " Convoy Center";
				}
			}
			if (!drawComments && !showSeaNames)
				return;
		}
		final Rectangle territoryBounds = mapData.getBoundingRect(territory);
		graphics.setFont(MapImage.getPropertyMapFont());
		graphics.setColor(MapImage.getPropertyTerritoryNameAndPUAndCommentcolor());
		final FontMetrics fm = graphics.getFontMetrics();
		int x;
		int y;
		// if we specify a placement point, use it
		// otherwise, put it in the center
		final Point namePlace = mapData.getNamePlacementPoint(territory);
		if (namePlace == null)
		{
			x = territoryBounds.x;
			y = territoryBounds.y;
			x += (int) territoryBounds.getWidth() >> 1;
			y += (int) territoryBounds.getHeight() >> 1;
			x -= fm.stringWidth(territory.getName()) >> 1;
			y += fm.getAscent() >> 1;
		}
		else
		{
			x = namePlace.x;
			y = namePlace.y;
		}
		// draw comments above names
		if (showComments && drawComments && commentText != null)
		{
			final Point place = mapData.getCommentMarkerLocation(territory);
			if (place != null)
				draw(bounds, graphics, place.x, place.y, null, commentText, drawFromTopLeft);
			else
				draw(bounds, graphics, x, y - fm.getHeight(), null, commentText, drawFromTopLeft);
		}
		// draw territory names
		if (mapData.drawTerritoryNames() && mapData.shouldDrawTerritoryName(m_territoryName))
		{
			if (!territory.isWater() || showSeaNames)
			{
				final Image nameImage = mapData.getTerritoryNameImages().get(territory.getName());
				draw(bounds, graphics, x, y, nameImage, territory.getName(), drawFromTopLeft);
			}
		}
		// draw the PUs.
		if (ta != null && ta.getProduction() > 0 && mapData.drawResources())
		{
			final Image img = m_uiContext.getPUImageFactory().getPUImage(ta.getProduction());
			final String prod = Integer.valueOf(ta.getProduction()).toString();
			final Point place = mapData.getPUPlacementPoint(territory);
			// if pu_place.txt is specified draw there
			if (place != null)
			{
				draw(bounds, graphics, place.x, place.y, img, prod, drawFromTopLeft);
			}
			else
			{
				// otherwise, draw under the territory name
				draw(bounds, graphics, x + ((fm.stringWidth(m_territoryName)) >> 1) - ((fm.stringWidth(prod)) >> 1), y + fm.getLeading() + fm.getAscent(), img, prod, drawFromTopLeft);
			}
		}
	}
	
	private void draw(final Rectangle bounds, final Graphics2D graphics, final int x, final int y, final Image img, final String prod, final boolean drawFromTopLeft)
	{
		int yNormal = y;
		if (img == null)
		{
			if (graphics.getFont().getSize() <= 0)
				return;
			if (drawFromTopLeft)
			{
				final FontMetrics fm = graphics.getFontMetrics();
				yNormal += fm.getHeight();
			}
			graphics.drawString(prod, x - bounds.x, yNormal - bounds.y);
		}
		else
		{
			// we want to be consistent
			// drawString takes y as the base line position
			// drawImage takes x as the top right corner
			if (!drawFromTopLeft)
				yNormal -= img.getHeight(null);
			graphics.drawImage(img, x - bounds.x, yNormal - bounds.y, null);
		}
	}
	
	public int getLevel()
	{
		return TERRITORY_TEXT_LEVEL;
	}
}


class VCDrawable implements IDrawable
{
	private final Territory m_location;
	
	public VCDrawable(final Territory location)
	{
		m_location = location;
	}
	
	public void draw(final Rectangle bounds, final GameData data, final Graphics2D graphics, final MapData mapData, final AffineTransform unscaled, final AffineTransform scaled)
	{
		final Point point = mapData.getVCPlacementPoint(m_location);
		graphics.drawImage(mapData.getVCImage(), point.x - bounds.x, point.y - bounds.y, null);
	}
	
	public int getLevel()
	{
		return VC_MARKER_LEVEL;
	}
}


class DecoratorDrawable implements IDrawable
{
	private final Point m_point;
	private final Image m_image;
	
	public DecoratorDrawable(final Point point, final Image image)
	{
		super();
		m_point = point;
		m_image = image;
	}
	
	public void draw(final Rectangle bounds, final GameData data, final Graphics2D graphics, final MapData mapData, final AffineTransform unscaled, final AffineTransform scaled)
	{
		graphics.drawImage(m_image, m_point.x - bounds.x, m_point.y - bounds.y, null);
	}
	
	public int getLevel()
	{
		return DECORATOR_LEVEL;
	}
}


class CapitolMarkerDrawable implements IDrawable
{
	private final String m_player;
	private final String m_location;
	private final IUIContext m_uiContext;
	
	public CapitolMarkerDrawable(final PlayerID player, final Territory location, final IUIContext uiContext)
	{
		super();
		if (player == null)
		{
			throw new IllegalStateException("no player for capitol:" + location);
		}
		m_player = player.getName();
		m_location = location.getName();
		m_uiContext = uiContext;
	}
	
	public void draw(final Rectangle bounds, final GameData data, final Graphics2D graphics, final MapData mapData, final AffineTransform unscaled, final AffineTransform scaled)
	{
		// Changed back to use Large flags
		final Image img = m_uiContext.getFlagImageFactory().getLargeFlag(data.getPlayerList().getPlayerID(m_player));
		final Point point = mapData.getCapitolMarkerLocation(data.getMap().getTerritory(m_location));
		graphics.drawImage(img, point.x - bounds.x, point.y - bounds.y, null);
	}
	
	public int getLevel()
	{
		return CAPITOL_MARKER_LEVEL;
	}
}


abstract class MapTileDrawable implements IDrawable
{
	protected boolean m_noImage = false;
	protected final int m_x;
	protected final int m_y;
	protected final IUIContext m_uiContext;
	protected boolean m_unscaled;
	
	public MapTileDrawable(final int x, final int y, final IUIContext uiContext)
	{
		m_x = x;
		m_y = y;
		m_uiContext = uiContext;
		m_unscaled = false;
	}
	
	public abstract MapTileDrawable getUnscaledCopy();
	
	protected abstract Image getImage();
	
	public void draw(final Rectangle bounds, final GameData data, final Graphics2D graphics, final MapData mapData, final AffineTransform unscaled, final AffineTransform scaled)
	{
		final Image img = getImage();
		if (img == null)
			return;
		final Object oldRenderingValue = graphics.getRenderingHint(RenderingHints.KEY_RENDERING);
		final Object oldAlphaValue = graphics.getRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION);
		final Object oldInterpolationValue = graphics.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
		graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
		graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
		graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		// the tile images are already scaled
		if (unscaled != null)
			graphics.setTransform(unscaled);
		final Stopwatch drawStopWatch = new Stopwatch(s_logger, Level.FINEST, "drawing tile images");
		graphics.drawImage(img, m_x * TileManager.TILE_SIZE - bounds.x, m_y * TileManager.TILE_SIZE - bounds.y, null);
		drawStopWatch.done();
		if (unscaled != null)
			graphics.setTransform(scaled);
		if (oldAlphaValue == null)
			graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_DEFAULT);
		else
			graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, oldAlphaValue);
		if (oldRenderingValue == null)
			graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_DEFAULT);
		else
			graphics.setRenderingHint(RenderingHints.KEY_RENDERING, oldRenderingValue);
		if (oldInterpolationValue == null)
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		else
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldInterpolationValue);
	}
}


class ReliefMapDrawable extends MapTileDrawable
{
	public ReliefMapDrawable(final int x, final int y, final IUIContext context)
	{
		super(x, y, context);
	}
	
	@Override
	public MapTileDrawable getUnscaledCopy()
	{
		final ReliefMapDrawable copy = new ReliefMapDrawable(m_x, m_y, m_uiContext);
		copy.m_unscaled = true;
		return copy;
	}
	
	@Override
	protected Image getImage()
	{
		if (m_noImage)
			return null;
		if (!TileImageFactory.getShowReliefImages())
			return null;
		Image rVal;
		if (m_unscaled)
			rVal = m_uiContext.getTileImageFactory().getUnscaledUncachedReliefTile(m_x, m_y);
		else
			rVal = m_uiContext.getTileImageFactory().getReliefTile(m_x, m_y);
		if (rVal == null)
			m_noImage = true;
		return rVal;
	}
	
	public int getLevel()
	{
		return RELIEF_LEVEL;
	}
}


class BaseMapDrawable extends MapTileDrawable
{
	public BaseMapDrawable(final int x, final int y, final IUIContext uiContext)
	{
		super(x, y, uiContext);
	}
	
	@Override
	public MapTileDrawable getUnscaledCopy()
	{
		final BaseMapDrawable copy = new BaseMapDrawable(m_x, m_y, m_uiContext);
		copy.m_unscaled = true;
		return copy;
	}
	
	@Override
	protected Image getImage()
	{
		if (m_noImage)
			return null;
		Image rVal;
		if (m_unscaled)
			rVal = m_uiContext.getTileImageFactory().getUnscaledUncachedBaseTile(m_x, m_y);
		else
			rVal = m_uiContext.getTileImageFactory().getBaseTile(m_x, m_y);
		if (rVal == null)
			m_noImage = true;
		return rVal;
	}
	
	public int getLevel()
	{
		return BASE_MAP_LEVEL;
	}
}


// Rewritten class to use country markers rather than shading for Convoy Centers/Routes.
class ConvoyZoneDrawable implements IDrawable
{
	private final String m_player;
	private final String m_location;
	private final IUIContext m_uiContext;
	
	public ConvoyZoneDrawable(final PlayerID player, final Territory location, final IUIContext uiContext)
	{
		super();
		m_player = player.getName();
		m_location = location.getName();
		m_uiContext = uiContext;
	}
	
	public void draw(final Rectangle bounds, final GameData data, final Graphics2D graphics, final MapData mapData, final AffineTransform unscaled, final AffineTransform scaled)
	{
		Image img;
		if (mapData.useNation_convoyFlags())
			img = m_uiContext.getFlagImageFactory().getConvoyFlag(data.getPlayerList().getPlayerID(m_player));
		else
			img = m_uiContext.getFlagImageFactory().getFlag(data.getPlayerList().getPlayerID(m_player));
		final Point point = mapData.getConvoyMarkerLocation(data.getMap().getTerritory(m_location));
		graphics.drawImage(img, point.x - bounds.x, point.y - bounds.y, null);
	}
	
	public int getLevel()
	{
		return CAPITOL_MARKER_LEVEL;
	}
}


// Class to use 'Faded' country markers for Kamikaze Zones.
class KamikazeZoneDrawable implements IDrawable
{
	private final String m_location;
	private final IUIContext m_uiContext;
	
	public KamikazeZoneDrawable(final PlayerID player, final Territory location, final IUIContext uiContext2)
	{
		super();
		m_location = location.getName();
		m_uiContext = uiContext2;
	}
	
	public void draw(final Rectangle bounds, final GameData data, final Graphics2D graphics, final MapData mapData, final AffineTransform unscaled, final AffineTransform scaled)
	{
		// Change so only original owner gets the kamikazi zone marker
		final Territory terr = data.getMap().getTerritory(m_location);
		final TerritoryAttachment ta = TerritoryAttachment.get(terr);
		PlayerID owner = null;
		if (games.strategy.triplea.Properties.getKamikazeSuicideAttacksDoneByCurrentTerritoryOwner(data))
		{
			owner = terr.getOwner();
			if (owner == null)
				owner = PlayerID.NULL_PLAYERID;
		}
		else
		{
			if (ta == null)
			{
				owner = PlayerID.NULL_PLAYERID;
			}
			else
			{
				owner = ta.getOriginalOwner();
				if (owner == null)
					owner = PlayerID.NULL_PLAYERID;
			}
		}
		final Image img = m_uiContext.getFlagImageFactory().getFadedFlag(owner);
		final Point point = mapData.getKamikazeMarkerLocation(data.getMap().getTerritory(m_location));
		graphics.drawImage(img, point.x - bounds.x, point.y - bounds.y, null);
	}
	
	public int getLevel()
	{
		return CAPITOL_MARKER_LEVEL;
	}
}


class BlockadeZoneDrawable implements IDrawable
{
	private final String m_location;
	
	// private final UIContext m_uiContext;
	public BlockadeZoneDrawable(final Territory location, final IUIContext uiContext)
	{
		super();
		m_location = location.getName();
		// m_uiContext = uiContext;
	}
	
	public void draw(final Rectangle bounds, final GameData data, final Graphics2D graphics, final MapData mapData, final AffineTransform unscaled, final AffineTransform scaled)
	{
		// Find blockade.png from misc folder
		final Point point = mapData.getBlockadePlacementPoint(data.getMap().getTerritory(m_location));
		graphics.drawImage(mapData.getBlockadeImage(), point.x - bounds.x, point.y - bounds.y, null);
	}
	
	public int getLevel()
	{
		return CAPITOL_MARKER_LEVEL;
	}
}


class TerritoryEffectDrawable implements IDrawable
{
	private final TerritoryEffect m_effect;
	private final Point m_point;
	
	public TerritoryEffectDrawable(final TerritoryEffect te, final Point point)
	{
		super();
		m_effect = te;
		m_point = point;
	}
	
	public void draw(final Rectangle bounds, final GameData data, final Graphics2D graphics, final MapData mapData, final AffineTransform unscaled, final AffineTransform scaled)
	{
		graphics.drawImage(mapData.getTerritoryEffectImage(m_effect.getName()), m_point.x - bounds.x, m_point.y - bounds.y, null);
	}
	
	public int getLevel()
	{
		return TERRITORY_EFFECT_LEVEL;
	}
}


class OptionalExtraTerritoryBordersDrawable implements IDrawable
{
	private final String m_territoryName;
	private final OptionalExtraBorderLevel m_level;
	
	public OptionalExtraTerritoryBordersDrawable(final String territoryName, final OptionalExtraBorderLevel level)
	{
		m_territoryName = territoryName;
		m_level = level;
	}
	
	public void draw(final Rectangle bounds, final GameData data, final Graphics2D graphics, final MapData mapData, final AffineTransform unscaled, final AffineTransform scaled)
	{
		final Territory territory = data.getMap().getTerritory(m_territoryName);
		final List<Polygon> polys = mapData.getPolygons(territory);
		final Iterator<Polygon> iter2 = polys.iterator();
		while (iter2.hasNext())
		{
			Polygon polygon = iter2.next();
			// if we dont have to draw, dont
			if (!polygon.intersects(bounds) && !polygon.contains(bounds))
				continue;
			// use a copy since we will move the polygon
			polygon = new Polygon(polygon.xpoints, polygon.ypoints, polygon.npoints);
			polygon.translate(-bounds.x, -bounds.y);
			graphics.setColor(Color.BLACK);
			graphics.drawPolygon(polygon);
		}
	}
	
	public int getLevel()
	{
		if (m_level == OptionalExtraBorderLevel.HIGH)
			return OPTIONAL_EXTRA_TERRITORY_BORDERS_HIGH_LEVEL;
		return OPTIONAL_EXTRA_TERRITORY_BORDERS_MEDIUM_LEVEL;
	}
}


class SeaZoneOutlineDrawable implements IDrawable
{
	private final String m_territoryName;
	
	public SeaZoneOutlineDrawable(final String territoryName)
	{
		m_territoryName = territoryName;
	}
	
	public void draw(final Rectangle bounds, final GameData data, final Graphics2D graphics, final MapData mapData, final AffineTransform unscaled, final AffineTransform scaled)
	{
		final Territory territory = data.getMap().getTerritory(m_territoryName);
		final List<Polygon> polys = mapData.getPolygons(territory);
		final Iterator<Polygon> iter2 = polys.iterator();
		while (iter2.hasNext())
		{
			Polygon polygon = iter2.next();
			// if we dont have to draw, dont
			if (!polygon.intersects(bounds) && !polygon.contains(bounds))
				continue;
			// use a copy since we will move the polygon
			polygon = new Polygon(polygon.xpoints, polygon.ypoints, polygon.npoints);
			polygon.translate(-bounds.x, -bounds.y);
			graphics.setColor(Color.BLACK);
			graphics.drawPolygon(polygon);
		}
	}
	
	public int getLevel()
	{
		return POLYGONS_LEVEL;
	}
}


abstract class TerritoryDrawable
{
	protected final void draw(final Rectangle bounds, final Graphics2D graphics, final MapData mapData, final AffineTransform unscaled, final AffineTransform scaled, final Territory territory,
				final Paint territoryPaint)
	{
		final List<Polygon> polys = mapData.getPolygons(territory);
		for (Polygon polygon : polys)
		{
			// if we dont have to draw, dont
			if (!polygon.intersects(bounds) && !polygon.contains(bounds))
				continue;
			// use a copy since we will move the polygon
			polygon = new Polygon(polygon.xpoints, polygon.ypoints, polygon.npoints);
			polygon.translate(-bounds.x, -bounds.y);
			graphics.setPaint(territoryPaint);
			graphics.fillPolygon(polygon);
			graphics.setColor(Color.BLACK);
			graphics.drawPolygon(polygon);
		}
	}
}


class BattleDrawable extends TerritoryDrawable implements IDrawable
{
	private final String m_territoryName;
	
	public BattleDrawable(final String territoryName)
	{
		m_territoryName = territoryName;
	}
	
	public void draw(final Rectangle bounds, final GameData data, final Graphics2D graphics, final MapData mapData, final AffineTransform unscaled, final AffineTransform scaled)
	{
		final Set<PlayerID> players = new HashSet<PlayerID>();
		for (final Unit u : data.getMap().getTerritory(m_territoryName).getUnits())
		{
			if (!TripleAUnit.get(u).getSubmerged())
				players.add(u.getOwner());
		}
		final Territory territory = data.getMap().getTerritory(m_territoryName);
		PlayerID attacker = null;
		boolean draw = false;
		for (final PlayerID p : players)
		{
			if (!territory.isWater())
			{
				if (data.getRelationshipTracker().isAtWar(p, territory.getOwner()))
				{
					attacker = p;
					draw = true;
					break;
				}
				else
				{
					// O(n^2), but n is usually 2, and almost always < 10
					for (final PlayerID p2 : players)
					{
						if (data.getRelationshipTracker().isAtWar(p, p2))
						{
							draw = true;
							break;
						}
					}
				}
			}
			else
			{
				// O(n^2), but n is usually 2, and almost always < 10
				for (final PlayerID p2 : players)
				{
					if (data.getRelationshipTracker().isAtWar(p, p2))
					{
						draw = true;
						break;
					}
				}
			}
		}
		if (draw)
		{
			Color stripeColor;
			if (attacker == null || territory.isWater())
				stripeColor = Color.RED.brighter();
			else
			{
				stripeColor = mapData.getPlayerColor(attacker.getName());
			}
			final Paint paint = new GradientPaint(0 - (float) bounds.getX(), 0 - (float) bounds.getY(),
						// (float) (tBounds.getX() - bounds.getX()),
						// (float) (tBounds.getY() - bounds.getY()),
						new Color(stripeColor.getRed(), stripeColor.getGreen(), stripeColor.getBlue(), 120),
						// (float) (tBounds.getX() - bounds.getX() + tBounds.getWidth()) ,
						// (float) (tBounds.getY() - bounds.getY() + tBounds.getHeight()),
						30 - (float) bounds.getX(), 50 - (float) bounds.getY(), new Color(0, 0, 0, 0), true);
			// newColor = new Color(255,120,120);
			// graphics.setStroke(new BasicStroke(6));
			// new TerritoryOverLayDrawable(Color.RED, m_territoryName, OP.DRAW).draw(bounds, data, graphics, mapData, unscaled, scaled);
			super.draw(bounds, graphics, mapData, unscaled, scaled, territory, paint);
		}
	}
	
	public int getLevel()
	{
		return BATTLE_HIGHLIGHT_LEVEL;
	}
}


class LandTerritoryDrawable extends TerritoryDrawable implements IDrawable
{
	private final String m_territoryName;
	
	public LandTerritoryDrawable(final String territoryName)
	{
		m_territoryName = territoryName;
	}
	
	public void draw(final Rectangle bounds, final GameData data, final Graphics2D graphics, final MapData mapData, final AffineTransform unscaled, final AffineTransform scaled)
	{
		final Territory territory = data.getMap().getTerritory(m_territoryName);
		Color territoryColor;
		final TerritoryAttachment ta = TerritoryAttachment.get(territory);
		if (ta != null && ta.getIsImpassible())
		{
			territoryColor = mapData.impassibleColor();
		}
		else
		{
			territoryColor = mapData.getPlayerColor(territory.getOwner().getName());
		}
		draw(bounds, graphics, mapData, unscaled, scaled, territory, territoryColor);
	}
	
	public int getLevel()
	{
		return POLYGONS_LEVEL;
	}
}
