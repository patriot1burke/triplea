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
package games.strategy.engine.lobby.server;

import games.strategy.engine.message.IRemote;
import games.strategy.net.INode;

import java.util.Date;

public interface IModeratorController extends IRemote
{
	/**
	 * Boot the given INode from the network.
	 * <p>
	 * 
	 * This method can only be called by admin users.
	 * 
	 */
	public void boot(INode node);
	
	/**
	 * Ban the username of the given INode.
	 */
	public void banUsername(INode node, Date banExpires);
	
	/**
	 * Ban the ip of the given INode.
	 */
	public void banIp(INode node, Date banExpires);
	
	/**
	 * Ban the mac of the given INode.
	 */
	public void banMac(INode node, Date banExpires);
	
	/**
	 * Ban the mac.
	 */
	public void banMac(final INode node, final String hashedMac, final Date banExpires);
	
	/**
	 * Mute the username of the given INode.
	 */
	public void muteUsername(INode node, Date muteExpires);
	
	/**
	 * Mute the ip of the given INode.
	 */
	public void muteIp(INode node, Date muteExpires);
	
	/**
	 * Mute the mac of the given INode.
	 */
	public void muteMac(INode node, Date muteExpires);
	
	/**
	 * Get list of people in the game.
	 */
	public String getHostConnections(INode node);
	
	/**
	 * Remote get chat log of a headless host bot.
	 */
	public String getChatLogHeadlessHostBot(INode node, String hashedPassword, String salt);
	
	/**
	 * Remote mute player in a headless host bot.
	 */
	public String mutePlayerHeadlessHostBot(INode node, String playerNameToBeBooted, int minutes, String hashedPassword, String salt);
	
	/**
	 * Remote boot player in a headless host bot.
	 */
	public String bootPlayerHeadlessHostBot(INode node, String playerNameToBeBooted, String hashedPassword, String salt);
	
	/**
	 * Remote ban player in a headless host bot.
	 */
	public String banPlayerHeadlessHostBot(INode node, String playerNameToBeBanned, int hours, String hashedPassword, String salt);
	
	/**
	 * Remote stop game of a headless host bot.
	 */
	public String stopGameHeadlessHostBot(INode node, String hashedPassword, String salt);
	
	/**
	 * Remote shutdown of a headless host bot.
	 */
	public String shutDownHeadlessHostBot(INode node, String hashedPassword, String salt);
	
	/**
	 * For use with a password for the bot.
	 */
	public String getHeadlessHostBotSalt(INode node);
	
	/**
	 * Reset the password of the given user. Returns null if the password was updated without error.
	 * <p>
	 * 
	 * You cannot change the password of an anonymous node, and you cannot change the password for an admin user.
	 * <p>
	 */
	public String setPassword(INode node, String hashedPassword);
	
	public String getInformationOn(INode node);
	
	/**
	 * Is the current user an admin.
	 */
	public boolean isAdmin();
	
	/**
	 * Is this node an admin.
	 * 
	 * @param node
	 * @return
	 */
	public boolean isPlayerAdmin(final INode node);
}
