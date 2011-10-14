package games.strategy.engine.framework.startup.login;

import games.strategy.engine.EngineVersion;
import games.strategy.net.ILoginValidator;
import games.strategy.net.ServerMessenger;
import games.strategy.util.MD5Crypt;
import games.strategy.util.Version;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * If we require a password, then we challenge the client with a salt value, the salt
 * being different for each login attempt. . The client hashes the password entered by
 * the user with this salt, and sends it back to us. This prevents the password from
 * travelling over the network in plain text, and also prevents someone listening on
 * the connection from getting enough information to log in (since the salt will change
 * on the next login attempt)
 * 
 * @author sgb
 */
public class ClientLoginValidator implements ILoginValidator
{
	public static final String SALT_PROPERTY = "Salt";
	public static final String PASSWORD_REQUIRED_PROPERTY = "Password Required";
	static final String YOU_HAVE_BEEN_BANNED = "The host has banned you from this game";
	static final String UNABLE_TO_OBTAIN_MAC = "Unable to obtain mac address";
	static final String INVALID_MAC = "Invalid mac address";
	
	// A hack, till I think of something better
	private static ClientLoginValidator s_instance;
	
	public static ClientLoginValidator getInstance()
	{
		return s_instance;
	}
	
	public ClientLoginValidator()
	{
		s_instance = this;
	}
	
	private String m_password;
	
	/**
	 * Set the password required for the game, or to null if no password is required.
	 * 
	 */
	public void setGamePassword(String password)
	{
		m_password = password;
	}
	
	@Override
	public Map<String, String> getChallengeProperties(String userName, SocketAddress remoteAddress)
	{
		Map<String, String> challengeProperties = new HashMap<String, String>();
		challengeProperties.put("Sever Version", EngineVersion.VERSION.toString());
		
		if (m_password != null)
		{
			/**
			 * Get a new random salt.
			 */
			
			String encryptedPassword = MD5Crypt.crypt(m_password);
			challengeProperties.put(SALT_PROPERTY, MD5Crypt.getSalt(MD5Crypt.MAGIC, encryptedPassword));
			challengeProperties.put(PASSWORD_REQUIRED_PROPERTY, Boolean.TRUE.toString());
		}
		else
		{
			challengeProperties.put(PASSWORD_REQUIRED_PROPERTY, Boolean.FALSE.toString());
		}
		
		return challengeProperties;
	}
	
	@Override
	public String verifyConnection(Map<String, String> propertiesSentToClient, Map<String, String> propertiesReadFromClient, String clientName, String hashedMac, SocketAddress remoteAddress)
	{
		String versionString = propertiesReadFromClient.get(ClientLogin.ENGINE_VERSION_PROPERTY);
		if (versionString == null || versionString.length() > 20 || versionString.trim().length() == 0)
			return "Invalid version " + versionString;
		
		// check for version
		Version clientVersion = new Version(versionString);
		if (!clientVersion.equals(EngineVersion.VERSION))
		{
			String error = "Client is using " + clientVersion + " but server requires version " + EngineVersion.VERSION;
			return error;
		}
		
		String realName = clientName.split(" ")[0];
		if (ServerMessenger.getInstance().IsUsernameMiniBanned(realName))
		{
			return YOU_HAVE_BEEN_BANNED;
		}
		String remoteIp = ((InetSocketAddress) remoteAddress).getAddress().getHostAddress();
		if (ServerMessenger.getInstance().IsIpMiniBanned(remoteIp))
		{
			return YOU_HAVE_BEEN_BANNED;
		}
		if (hashedMac == null)
		{
			return UNABLE_TO_OBTAIN_MAC;
		}
		if (hashedMac.length() != 28 || !hashedMac.startsWith(MD5Crypt.MAGIC + "MH$") || !hashedMac.matches("[0-9a-zA-Z$./]+"))
		{
			return INVALID_MAC; // Must have been tampered with
		}
		if (ServerMessenger.getInstance().IsMacMiniBanned(hashedMac))
		{
			return YOU_HAVE_BEEN_BANNED;
		}
		
		if (propertiesSentToClient.get(PASSWORD_REQUIRED_PROPERTY).equals(Boolean.TRUE.toString()))
		{
			String readPassword = propertiesReadFromClient.get(ClientLogin.PASSWORD_PROPERTY);
			if (readPassword == null)
			{
				return "No password";
			}
			
			if (!readPassword.equals(MD5Crypt.crypt(m_password, propertiesSentToClient.get(SALT_PROPERTY))))
			{
				try
				{
					// sleep on average 2 seconds
					// try to prevent flooding to guess the
					// password
					Thread.sleep((int) (4000 * Math.random()));
				} catch (InterruptedException e)
				{
					// ignore
				}
				
				return "Invalid password";
			}
		}
		
		return null;
	}
}
