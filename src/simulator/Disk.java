/*
Disk.java
Michael Black, 6/10

Loads and handles a disk image
*/

package simulator;

import java.io.*;
import java.net.*;

public class Disk
{
	public static final int SECTOR_SIZE = 512;
	public static final int MAX_IMAGE_SIZE = 40000000;
    
	public static enum Type {HARDDRIVE, CDROM, FLOPPY};

	long file_size=0;
	String path_to_disk_image=null;

	byte[] diskContents;	//used only in applet

	public Disk()
	{
	}

	private static class TempQueue
	{
		byte b;
		TempQueue next;
		static TempQueue first;
		static TempQueue last;
		static int length=0;

		public TempQueue(byte b)
		{
			this.b=b;
		}
		public static void push(byte b)
		{
			if (first==null)
			{
				first=new TempQueue(b);
				last=first;
			}
			else
			{
				last.next=new TempQueue(b);
				last=last.next;
			}
			length++;
		}
		public static byte pop()
		{
			byte p=first.b;
			first=first.next;
			return p;
		}
		public static int getLength()
		{
			return length;
		}
		public static void reset()
		{
			length=0;
			first=null;
			last=null;
		}
	}

	public Disk(URL disk_url)
	{
System.out.println(""+disk_url);
		TempQueue.reset();
		try
		{
			InputStream in=disk_url.openConnection().getInputStream();
			int c;
			while((c=in.read())!=-1)
			{
				TempQueue.push((byte)c);
			}
			file_size=TempQueue.getLength();
			System.out.println(file_size);
					diskContents=new byte[TempQueue.getLength()];
					for (int i=0; i<TempQueue.getLength(); i++)
						diskContents[i]=TempQueue.pop();		
		}
		catch(NullPointerException e)
		{
			System.out.println("Error accessing disk image "+path_to_disk_image);
//			System.exit(0);
		}
		catch(IOException e)
		{
			System.out.println("Error accessing disk image "+path_to_disk_image);
//			System.exit(0);
		}
	}


	public Disk(String path_to_disk_image)
	{
		try
		{
			RandomAccessFile f = new RandomAccessFile(path_to_disk_image,"r");
			file_size=f.length();
			f.close();
		}
		catch(IOException e)
		{
			System.out.println("Error accessing disk image "+path_to_disk_image);
//			System.exit(0);
		}
		this.path_to_disk_image=path_to_disk_image;
	}

	public int read(int sectorNumber, byte[] buffer, int size)
	{
		if (diskContents!=null)
			return readArray(sectorNumber, buffer, size);
		if (path_to_disk_image==null)
			return -1;
		FileInputStream in=null;
		try
		{
			in=new FileInputStream(path_to_disk_image);
			int c;
			in.skip(sectorNumber*SECTOR_SIZE);
			for(int i=0; i<size*SECTOR_SIZE; i++)
				buffer[i]=(byte)in.read();
			in.close();
		}
		catch(IOException e)
		{
			System.out.println("Error reading disk image "+path_to_disk_image);
			System.out.println(sectorNumber);
//			System.exit(0);
		}
		return sectorNumber*SECTOR_SIZE;
	}

	public int readArray(int sectorNumber, byte[] buffer, int size)
	{
		for (int i=0; i<size*SECTOR_SIZE; i++)
			buffer[i]=diskContents[sectorNumber*SECTOR_SIZE+i];
		return sectorNumber*SECTOR_SIZE;
	}

	public int write(int sectorNumber, byte[] buffer, int size)
	{
		if (diskContents!=null)
			return readArray(sectorNumber, buffer, size);
		if(path_to_disk_image==null)
			return -1;

		RandomAccessFile out=null;
		try
		{
			out=new RandomAccessFile(path_to_disk_image,"rw");
			out.seek(sectorNumber*SECTOR_SIZE);
			for (int i=0; i<size*SECTOR_SIZE; i++)
				out.write(buffer[i]);
			out.close();
		}
		catch(IOException e)
		{
			System.out.println("Error writing to disk image "+path_to_disk_image);
//			System.exit(0);
		}
		return 0;
	}

	public int writeArray(int sectorNumber, byte[] buffer, int size)
	{
		for (int i=0; i<size*SECTOR_SIZE; i++)
			diskContents[sectorNumber*SECTOR_SIZE+i]=buffer[i];
		return 0;
	}

	public long getTotalSize()
	{
		return file_size;
	}
	public boolean isInserted()
	{
		return (path_to_disk_image!=null || diskContents!=null);
	}
	public boolean isReadOnly()
	{
		return false;
	}
	public int getCylinders()
	{
		return -1;
	}
	public int getHeads()
	{
		return -1;
	}
	public int getSectors()
	{
		return -1;
	}
	public Type getType()
	{
		return Type.FLOPPY;
	}
}
