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
/*
 * @author George El-Haddad
 * 
 * @email nekromancer@users.sourceforge.net
 */
package util.image;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

public class FileSave
{
	private File file = null;
	
	/**
	 * Default Constructor.
	 * 
	 * Creates a file selection dialog starting at the current
	 * working directory. The user will specify what directory
	 * or folder they want their files to be saved in.
	 * 
	 * @param java
	 *            .lang.String title the title of the JFileChooser
	 * @param java
	 *            .lang.String name a recomended name
	 * @exception java.lang.Exception
	 *                ex
	 */
	public FileSave(final String title, final String name)
	{
		this(title, name, new File(System.getProperties().getProperty("user.dir")));
	}
	
	public FileSave(final String title, final String name, final File currentDirectory)
	{
		this(title, name, currentDirectory, JFileChooser.DIRECTORIES_ONLY, null, new javax.swing.filechooser.FileFilter()
		{
			@Override
			public boolean accept(final File f)
			{
				return f.isDirectory();
			}
			
			@Override
			public String getDescription()
			{
				return "Folder To Save In";
			}
		});
	}
	
	public FileSave(final String title, final int JFileChooserFileSelectionMode, final File selectedFile, final File currentDirectory)
	{
		this(title, null, currentDirectory, JFileChooserFileSelectionMode, selectedFile, null);
	}
	
	public FileSave(final String title, final String name, final File currentDirectory, final int JFileChooserFileSelectionMode, final File selectedFile, final FileFilter fileFilter)
	{
		final JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooserFileSelectionMode);
		chooser.setDialogTitle(title);
		if (selectedFile != null)
			chooser.setSelectedFile(selectedFile);
		chooser.setCurrentDirectory(((currentDirectory == null || !currentDirectory.exists()) ? new File(System.getProperties().getProperty("user.dir")) : currentDirectory));
		if (fileFilter != null)
			chooser.setFileFilter(fileFilter);
		// show the file chooser dialog
		final int r = chooser.showSaveDialog(null);
		if (r == JFileChooser.APPROVE_OPTION)
		{
			if (name != null)
			{
				file = new File(chooser.getSelectedFile().getPath() + File.separator + name);
			}
			else
			{
				file = new File(chooser.getSelectedFile().getPath());
			}
		}
	}
	
	/**
	 * File getFile()
	 * 
	 * Returns the directory path as
	 * a File object.
	 * 
	 * @return java.io.File
	 */
	public File getFile()
	{
		return file;
	}
	
	/**
	 * String getPathString()
	 * 
	 * Returns the directory path as
	 * as string.
	 * 
	 * @return java.lang.String
	 */
	public String getPathString()
	{
		if (file == null)
		{
			return null;
		}
		else
		{
			return file.getPath();
		}
	}
}// end class FileSave
