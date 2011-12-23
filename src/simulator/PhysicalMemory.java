/*
PhysicalMemory.java
Michael Black, 6/10

Simulates the physical address space
*/
package simulator;
import java.io.*;
import java.net.*;
public class PhysicalMemory
{
	//total amount of physical memory after FFFFF
	public static int EXTENDED_RAM_SIZE=0xf80000;

	//total physical memory
	public static int TOTAL_RAM_SIZE=EXTENDED_RAM_SIZE+0x100000;

	//memory is handled in units of BLOCK_SIZE
	public static int BLOCK_SIZE=0x1000;
	public static int BLOCK_SIZE_BITS=12;
	public static int BLOCK_SIZE_OFFSET_MASK=0xfff;

	//this is the memory!
	//pointers to memory blocks
	private MemoryBlock[] memoryBlock;

	//for debugging, keep a record of recent stores
	public long storeRecord=0;
	public long loadRecord=0;
	public long addressRecord=0;
	private Computer computer;
	
	public PhysicalMemory(Computer computer)
	{
		this.computer=computer;
		//initialize memory array
		memoryBlock=new MemoryBlock[TOTAL_RAM_SIZE/BLOCK_SIZE];
		for (int i=0; i<TOTAL_RAM_SIZE/BLOCK_SIZE; i++)
			memoryBlock[i]=new MemoryBlock();

		//initialize extended RAM
		for (int i=0x100000; i<TOTAL_RAM_SIZE; i+=BLOCK_SIZE)
		{
			memoryBlock[i/BLOCK_SIZE].initialize();
			memoryBlock[i/BLOCK_SIZE].writeable=true;
		}

		//initialize base RAM
		for (int i=0; i<0xd0000; i+=BLOCK_SIZE)
		{
			memoryBlock[i/BLOCK_SIZE].initialize();
			memoryBlock[i/BLOCK_SIZE].writeable=true;
		}
	}

	public void loadBIOS(URL BIOS_image, int start, int stop) throws IOException
	{
		for (int i=start; i<=stop; i+=BLOCK_SIZE)
		{
			memoryBlock[i/BLOCK_SIZE].initialize();
			memoryBlock[i/BLOCK_SIZE].writeable=true;
		}			

		InputStream in=null;
		try
		{
			in=BIOS_image.openConnection().getInputStream();
			int c;
			int i=start;
			while((c=in.read())!=-1)
			{
				setByte(i++,(byte)c);
			}
		}
		finally
		{
			if(in!=null)
				in.close();
		}

		for (int i=start; i<=stop; i+=BLOCK_SIZE)
		{
			memoryBlock[i/BLOCK_SIZE].writeable=false;
		}
	}

	public void loadBIOS(String path_to_BIOS_image, int start, int stop) throws IOException
	{
		for (int i=start; i<=stop; i+=BLOCK_SIZE)
		{
			memoryBlock[i/BLOCK_SIZE].initialize();
			memoryBlock[i/BLOCK_SIZE].writeable=true;
		}			

		FileInputStream in=null;
		try
		{
			in=new FileInputStream(path_to_BIOS_image);
			int c;
			int i=start;
			while((c=in.read())!=-1)
			{
				setByte(i++,(byte)c);
			}
		}
		finally
		{
			if(in!=null)
				in.close();
		}

		for (int i=start; i<=stop; i+=BLOCK_SIZE)
		{
			memoryBlock[i/BLOCK_SIZE].writeable=false;
		}
	}

	public byte getByte(int address)
	{
		address = address & 0xffffff;

//		System.out.printf("Getting from address %x: %x\n",address,memoryBlock[address/BLOCK_SIZE].getByte(address%BLOCK_SIZE));
		byte value;
//		try
//		{
			value = memoryBlock[address>>>BLOCK_SIZE_BITS].getByte(address&BLOCK_SIZE_OFFSET_MASK);
//		}
//		catch(ArrayIndexOutOfBoundsException e)
//		{
//			return (byte)(-1);
//		}


		loadRecord=((loadRecord<<8)&~0xff)|(0xff&value);
		addressRecord=((addressRecord<<20)&~0xfffff)|(0xfffff&address);

		if(computer.video!=null)
			computer.video.updateVideoRead(address);

		return value;
	}
	public void setByte(int address, byte value)
	{
		address = address & 0xffffff;

//		System.out.printf("Setting address %x to %x\n",address,value);
		memoryBlock[address>>>BLOCK_SIZE_BITS].setByte(address&BLOCK_SIZE_OFFSET_MASK,value);

		if(computer.video!=null)
			computer.video.updateVideoWrite(address,value);

		storeRecord=((storeRecord<<8)&~0xff)|(0xff&value);
		addressRecord=((addressRecord<<24)&~0xffffff)|(0xffffff&address);
	}

	public short getWord(int address)
	{
		return (short)((getByte(address)&0xff)|((getByte(address+1)<<8)&0xff00));
	}
	public int getDoubleWord(int address)
	{
		return (getWord(address)&0xffff)|((getWord(address+2)<<16)&0xffff0000);
        }
	public long getQuadWord(int address)
	{
		return (getDoubleWord(address)&0xffffffffl)|((((long)getDoubleWord(address+4))<<32)&0xffffffff00000000l);
	}
	public void setWord(int address, short value)
	{
		setByte(address,(byte)value);
		setByte(address+1,(byte)(value>>8));
	}
	public void setDoubleWord(int address, int value)
	{
		setByte(address,(byte)value);
		setByte(address+1,(byte)(value>>8));
		setByte(address+2,(byte)(value>>16));
		setByte(address+3,(byte)(value>>24));
	}
	public void setQuadWord(int address, long value)
	{
		setByte(address,(byte)value);
		setByte(address+1,(byte)(value>>8));
		setByte(address+2,(byte)(value>>16));
		setByte(address+3,(byte)(value>>24));
		setByte(address+4,(byte)(value>>32));
		setByte(address+5,(byte)(value>>40));
		setByte(address+6,(byte)(value>>48));
		setByte(address+7,(byte)(value>>56));
	}

	public boolean isInitialized(int address)
	{
		if ((address>>>BLOCK_SIZE_BITS)>=memoryBlock.length)
			return false;
		return memoryBlock[address>>>BLOCK_SIZE_BITS].initialized;
	}

	public void dumpMemory(int address, int quantity)
	{
		for (int i=0; i<quantity; i++)
		{
			if (i%24==0)
				System.out.println();
			System.out.printf("%x ",getByte(address+i));
		}
	}

	private class MemoryBlock
	{
		byte[] rambyte;
		boolean initialized=false;
		boolean writeable=false;

		public MemoryBlock()
		{
		}

		//make the block active
		public void initialize()
		{
			rambyte=new byte[BLOCK_SIZE];
			for (int i=0; i<BLOCK_SIZE; i++)
				rambyte[i]=0;
			initialized=true;
		}

		public byte getByte(int offset)
		{
			if(!initialized)
				return (byte)(-1);
			else
				return rambyte[offset];
		}
		public void setByte(int offset, byte value)
		{
			if(initialized && writeable)
				rambyte[offset]=value;
		}
	}
}
