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

package games.strategy.engine.random;


import games.strategy.engine.EngineVersion;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;

/**
 * A pbem dice roller that reads its configuration from a properties file
 * 
 * 
 * @author sgb
 */
public class PropertiesDiceRoller implements IRemoteDiceServer
{
    
    private final Properties m_props;

    public PropertiesDiceRoller(Properties props)
    {
        m_props = props;
    }
    
    
    
    public String getName()
    {
        return m_props.getProperty("name");
    }
    
    public String toString()
    {
        return getName();
    }

    public boolean sendsEmail() 
    {       
        String property = m_props.getProperty("send.email");
        if(property == null) {
            return true;
        }
        return Boolean.valueOf(property);          
    }
    
    public String postRequest(String player1, String player2, int max, int numDice, String text, String gameID, String gameUUID) throws IOException
    {
        if(gameID.trim().length() == 0)
            gameID = "TripleA";
        String message = gameID + ":" + text;
        
        int maxLength = Integer.valueOf(m_props.getProperty("message.maxlength"));
        
        if (message.length() > maxLength)
            message = message.substring(0, maxLength -1);
                
        PostMethod post = new PostMethod(m_props.getProperty("path"));
        NameValuePair[] data = {
          new NameValuePair("numdice", "" + numDice),
          new NameValuePair("numsides", "" + max),
          new NameValuePair("modroll", "No"),
          new NameValuePair("numroll", "" + 1),
          new NameValuePair("subject", message),
          new NameValuePair("roller", player1),
          new NameValuePair("gm", player2),
          new NameValuePair("send", "true"),          
        };
        
        post.setRequestHeader("User-Agent", "triplea/" + EngineVersion.VERSION);
        
        //this is to allow a dice server to allow the user to request the emails for the game
        //rather than sending out email for each roll
        post.setRequestHeader("X-Triplea-Game-UUID", gameUUID);
        
        
        post.setRequestBody(data);
       
        HttpClient client = new HttpClient();
        try
        {
            String host = m_props.getProperty("host");
            int port = 80;
            if(m_props.getProperty("port") != null) {
                port = Integer.parseInt( m_props.getProperty("port"));
            }            
            client.getHostConfiguration().setHost(host, port);
            client.executeMethod(post);
            
            String result = post.getResponseBodyAsString();
           
            return result;
        }
        finally
        {
            post.releaseConnection();
        }
    }
    
    public String getWebText() 
    {
        return m_props.getProperty("web.text");
    }

    
   
    /**
     * 
     * @throws IOException
     *             if there was an error parsing the string
     */
    public int[] getDice(String string, int count) throws IOException, InvocationTargetException
    {
        String errorStartString = m_props.getProperty("error.start");
        String errorEndString = m_props.getProperty("error.end");
        //if the error strings are defined
        if(errorStartString != null && errorStartString.length() > 0 && errorEndString != null && errorEndString.length() > 0) 
        {
            int startIndex = string.indexOf(errorStartString);
            if(startIndex >= 0)
            {
                int endIndex = string.indexOf(errorEndString, (startIndex + errorStartString.length()));
                if(endIndex > 0)
                {
                    String error = string.substring(startIndex + errorStartString.length(), endIndex);
                    throw new InvocationTargetException( null, error);
                    
                }
            }
        }
        
        
        String rollStartString;
        String rollEndString;
        if (count == 1)
        {
            rollStartString =  m_props.getProperty("roll.single.start"); 
            rollEndString =  m_props.getProperty("roll.single.end"); 
        } else
        {
            
            rollStartString =  m_props.getProperty("roll.multiple.start"); 
            rollEndString =  m_props.getProperty("roll.multiple.end"); 
        }

        int startIndex = string.indexOf(rollStartString);
        if (startIndex == -1)
        {
            throw new IOException("Cound not find start index, text returned is:" + string);

        }
        startIndex += rollStartString.length();

        int endIndex = string.indexOf(rollEndString, startIndex);
        if (endIndex == -1)
        {
            throw new IOException("Cound not find end index");
        }

        StringTokenizer tokenizer = new StringTokenizer(string.substring(startIndex, endIndex), " ,", false);

        int[] rVal = new int[count];
        for (int i = 0; i < count; i++)
        {
            try
            {
                //-1 since we are 0 based
                rVal[i] = Integer.parseInt(tokenizer.nextToken()) - 1;
            } catch (NumberFormatException ex)
            {
                ex.printStackTrace();
                throw new IOException(ex.getMessage());
            }
        }

        return rVal;
    }
    
    
}

