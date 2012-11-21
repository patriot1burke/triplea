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
package games.strategy.engine.lobby.server.userDB;

import games.strategy.util.Tuple;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utilitiy class to create/read/delete banned usernames (there is no update).
 * 
 * @author Wisconsin
 */
public class BannedUsernameController
{
	private static final Logger s_logger = Logger.getLogger(BannedUsernameController.class.getName());
	
	/**
	 * Ban the username permanently
	 */
	public void addBannedUsername(final String username)
	{
		addBannedUsername(username, null);
	}
	
	/**
	 * Ban the given username. If banTill is not null, the ban will expire when banTill is reached.
	 * <p>
	 * 
	 * If this username is already banned, this call will update the ban_end.
	 */
	public void addBannedUsername(final String username, final Date banTill)
	{
		if (isUsernameBanned(username).getFirst())
		{
			removeBannedUsername(username);
		}
		Timestamp banTillTs = null;
		if (banTill != null)
		{
			banTillTs = new Timestamp(banTill.getTime());
		}
		s_logger.fine("Banning username:" + username);
		final Connection con = Database.getConnection();
		try
		{
			final PreparedStatement ps = con.prepareStatement("insert into banned_usernames (username, ban_till) values (?, ?)");
			ps.setString(1, username);
			ps.setTimestamp(2, banTillTs);
			ps.execute();
			ps.close();
			con.commit();
		} catch (final SQLException sqle)
		{
			if (sqle.getErrorCode() == 30000)
			{
				// this is ok
				// the username is banned as expected
				s_logger.info("Tried to create duplicate banned username:" + username + " error:" + sqle.getMessage());
				return;
			}
			s_logger.log(Level.SEVERE, "Error inserting banned username:" + username, sqle);
			throw new IllegalStateException(sqle.getMessage());
		} finally
		{
			DbUtil.closeConnection(con);
		}
	}
	
	public void removeBannedUsername(final String username)
	{
		s_logger.fine("Removing banned username:" + username);
		final Connection con = Database.getConnection();
		try
		{
			final PreparedStatement ps = con.prepareStatement("delete from banned_usernames where username = ?");
			ps.setString(1, username);
			ps.execute();
			ps.close();
			con.commit();
		} catch (final SQLException sqle)
		{
			s_logger.log(Level.SEVERE, "Error deleting banned username:" + username, sqle);
			throw new IllegalStateException(sqle.getMessage());
		} finally
		{
			DbUtil.closeConnection(con);
		}
	}
	
	/**
	 * Is the given username banned? This may have the side effect of removing from the
	 * database any username's whose ban has expired
	 */
	public Tuple<Boolean, Timestamp> isUsernameBanned(final String username)
	{
		boolean found = false;
		boolean expired = false;
		Timestamp banTill = null;
		final String sql = "select username, ban_till from banned_usernames where username = ?";
		final Connection con = Database.getConnection();
		try
		{
			final PreparedStatement ps = con.prepareStatement(sql);
			ps.setString(1, username);
			final ResultSet rs = ps.executeQuery();
			found = rs.next();
			// If the ban has expired, allow the username
			if (found)
			{
				banTill = rs.getTimestamp(2);
				if (banTill != null && banTill.getTime() < System.currentTimeMillis())
				{
					s_logger.fine("Ban expired for:" + username);
					expired = true;
				}
			}
			rs.close();
			ps.close();
		} catch (final SQLException sqle)
		{
			s_logger.info("Error for testing banned username existence:" + username + " error:" + sqle.getMessage());
			throw new IllegalStateException(sqle.getMessage());
		} finally
		{
			DbUtil.closeConnection(con);
		}
		if (expired)
		{
			removeBannedUsername(username);
			return new Tuple<Boolean, Timestamp>(false, banTill);
		}
		return new Tuple<Boolean, Timestamp>(found, banTill);
	}
}
