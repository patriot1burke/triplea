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


/*
  @author George El-Haddad
  @email  nekromancer@users.sourceforge.net
 */

package util.image;

import java.io.*;
import javax.swing.*;

public class FileOpen 
{
	private final String ERR_MSG_1 = "Warning! Could not load the file!";
	
	private File file = null;

	/**
	   Default Constructor.
	   
	   @param java.lang.String title  the title of the JFileChooser
	   
	   @exception java.lang.Exception  ex
	   
	   Creates a file selection dialog starting at the current
	   working directory. Filters out all non-txt files and
	   handles possible file load errors.
	*/
	public FileOpen(String title)	 
	{
		this(title, ".txt", ".gif", ".png");
		
	}
	
	public FileOpen(String title, final String... extensions)  
	{
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle(title);
		chooser.setCurrentDirectory(new File(System.getProperties().getProperty("user.dir")));

		/*
		 * Show only text and gif files
		 */
		chooser.setFileFilter(new javax.swing.filechooser.FileFilter()
		{
			public boolean accept(File f)
			{
				if(f.isDirectory()) 
				{
					return true;
				}
				for(String ex : extensions) 
				{
					if(f.getName().endsWith(ex)) {
						return true;
					}					
				}
				return false;
			}

			public String getDescription()
			{
				StringBuffer buf = new StringBuffer();
				for(String ex : extensions) 
				{
					buf.append("*").append(ex).append(" ");					
				}
				return buf.toString();
			}
		});

		int result = chooser.showOpenDialog(null);

		if(result == JFileChooser.CANCEL_OPTION)
		{
			return;
		}

		try
		{
			file = chooser.getSelectedFile();   //get the file
		}
		catch(Exception ex)
		{
			JOptionPane.showMessageDialog(null,ERR_MSG_1,"Warning!", JOptionPane.WARNING_MESSAGE);
			file = null;
		}

	}//constructor
	
	
	/**
	   Returns the newly selected file.
	   Will return null if no file is selected.

	   @return java.io.File
	*/
	public File getFile()
	{
		return file;
	}


	/**
	   Returns the newly selected file.
	   Will return null if no file is selected.

	   @return java.lang.String
	*/
	public String getPathString()
	{
		if(file == null)
		{
			return null;
		}
		else
		{
			return file.getPath();
		}
	}

}//end class FileOpen
