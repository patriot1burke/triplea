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
 * GUID.java
 * 
 * Created on January 4, 2002, 2:33 PM
 */
package games.strategy.net;

import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.rmi.dgc.VMID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 
 * A globally unique id. <br>
 * Backed by a java.rmi.dgc.VMID.
 * 
 * Written across the network often, so this class is
 * externalizable to increase effeciency
 * 
 * @author Sean Bridges
 * @see java.rmi.dgc.VMID
 */
public class GUID implements Externalizable
{
	private static final long serialVersionUID = 8426441559602874190L;
	// this prefix is unique across vms
	private static VMID vm_prefix = new java.rmi.dgc.VMID();
	// the local identifier
	// this coupled with the unique vm prefix comprise
	// our unique id
	private static AtomicInteger s_lastID = new AtomicInteger();
	private int m_id;
	private VMID m_prefix;
	
	public GUID()
	{
		m_id = s_lastID.getAndIncrement();
		m_prefix = vm_prefix;
		// handle wrap around if needed
		if (m_id < 0)
		{
			vm_prefix = new VMID();
			s_lastID = new AtomicInteger();
		}
	}
	
	@Override
	public boolean equals(final Object o)
	{
		if (o == null)
			return false;
		if (!(o instanceof GUID))
			return false;
		final GUID other = (GUID) o;
		if (other == this)
			return true;
		return this.m_id == other.m_id && (other.m_prefix == this.m_prefix || other.m_prefix.equals(this.m_prefix));
	}
	
	@Override
	public int hashCode()
	{
		return m_id ^ m_prefix.hashCode();
	}
	
	@Override
	public String toString()
	{
		return "GUID:" + m_prefix + ":" + m_id;
	}
	
	public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException
	{
		m_id = in.readInt();
		m_prefix = (VMID) in.readObject();
	}
	
	public void writeExternal(final ObjectOutput out) throws IOException
	{
		out.writeInt(m_id);
		out.writeObject(m_prefix);
	}
	
	public static void main(final String[] args) throws IOException
	{
		System.out.println(new GUID().toString());
		final ByteArrayOutputStream sink = new ByteArrayOutputStream();
		final ObjectOutputStream out = new ObjectOutputStream(sink);
		for (int i = 0; i < 1000; i++)
		{
			out.writeObject(new GUID());
		}
		out.close();
		System.out.println("1000 ids is:" + sink.toByteArray().length + " bytes");
	}
}
