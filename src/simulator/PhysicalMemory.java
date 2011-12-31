/*
PhysicalMemory.java
Michael Black, 6/10

Simulates the physical address space
*/
package simulator;
import java.io.*;
import java.net.*;
import java.util.Scanner;
public class PhysicalMemory implements MemoryDevice
{
	//total amount of physical memory after FFFFF
	public static int EXTENDED_RAM_SIZE=0x1000000;

	//total physical memory
	public static int TOTAL_RAM_SIZE=EXTENDED_RAM_SIZE+0x100000;

	//memory is handled in units of BLOCK_SIZE
	public static int BASE_BLOCK_SIZE=0x1000;
	public static int EXTENDED_BLOCK_SIZE=0x10000;
	public static int BASE_BLOCK_SIZE_BITS=12;
	public static int EXTENDED_BLOCK_SIZE_BITS=16;
	public static int BASE_BLOCK_SIZE_OFFSET_MASK=0xfff;
	public static int EXTENDED_BLOCK_SIZE_OFFSET_MASK=0xffff;

	public static int BASE_BLOCKS=0x100;
	public static int EXTENDED_BLOCKS=0x10000-0x10;	//enough to populate all 4G addressable RAM
	
	//this is the memory!
	//pointers to memory blocks
	private MemoryBlock[] baseMemoryBlock;
	private MemoryBlock[] extendedMemoryBlock;

	private Computer computer;
	
	public String saveState()
	{
		String state="";
		
		for (int i=0; i<baseMemoryBlock.length; i++)
		{
			if (baseMemoryBlock[i].initialized)
			{
				String s="";
				s+=i+" "+(baseMemoryBlock[i].writeable?1:0)+" ";
				for (int j=0; j<baseMemoryBlock[i].rambyte.length; j++)
					s+=baseMemoryBlock[i].rambyte[j]+" ";
				state+=s;
				System.out.println("saving block "+i);
			}
		}
		state+=":";
		for (int i=0; i<extendedMemoryBlock.length; i++)
		{
			if (extendedMemoryBlock[i].initialized)
			{
				String s="";
				s+=i+" "+(extendedMemoryBlock[i].writeable?1:0)+" ";
				for (int j=0; j<extendedMemoryBlock[i].rambyte.length; j++)
					s+=extendedMemoryBlock[i].rambyte[j]+" ";
				state+=s;
				System.out.println("saving block "+i);
			}
		}		
		return state;
	}
	
	public void loadState(String state)
	{
		String[] states=state.split(":");
		Scanner loader=new Scanner(states[0]);
		while(loader.hasNextInt())
		{
			int index=loader.nextInt();
			baseMemoryBlock[index].writeable=loader.nextInt()==1;
			baseMemoryBlock[index].initialize();
			for (int j=0; j<baseMemoryBlock[index].rambyte.length; j++)
				baseMemoryBlock[index].rambyte[j]=loader.nextByte();
			System.out.println("loaded block "+index);
		}
		loader=new Scanner(states[1]);
		while(loader.hasNextInt())
		{
			int index=loader.nextInt();
			extendedMemoryBlock[index].writeable=loader.nextInt()==1;
			extendedMemoryBlock[index].initialize();
			for (int j=0; j<extendedMemoryBlock[index].rambyte.length; j++)
				extendedMemoryBlock[index].rambyte[j]=loader.nextByte();
			System.out.println("loaded block "+index);
		}
	}
	
	public PhysicalMemory(Computer computer)
	{
		this.computer=computer;
		//initialize memory array
		baseMemoryBlock=new MemoryBlock[BASE_BLOCKS];
		for (int i=0; i<BASE_BLOCKS; i++)
			baseMemoryBlock[i]=new MemoryBlock(BASE_BLOCK_SIZE);
			
		extendedMemoryBlock=new MemoryBlock[EXTENDED_BLOCKS];
		for (int i=0; i<EXTENDED_BLOCKS; i++)
		{
			extendedMemoryBlock[i]=new MemoryBlock(EXTENDED_BLOCK_SIZE);
			extendedMemoryBlock[i].writeable=true;
		}

		//initialize base RAM
		for (int i=0; i<0xd0000/BASE_BLOCK_SIZE; i++)
		{
			baseMemoryBlock[i].initialize();
			baseMemoryBlock[i].writeable=true;
		}
	}

	public void loadBIOS(URL BIOS_image, int start, int stop) throws IOException
	{
		for (int i=start; i<=stop; i+=BASE_BLOCK_SIZE)
		{
			baseMemoryBlock[i/BASE_BLOCK_SIZE].initialize();
			baseMemoryBlock[i/BASE_BLOCK_SIZE].writeable=true;
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

		for (int i=start; i<=stop; i+=BASE_BLOCK_SIZE)
		{
			baseMemoryBlock[i/BASE_BLOCK_SIZE].writeable=false;
		}
	}

	public void loadBIOS(String path_to_BIOS_image, int start, int stop) throws IOException
	{
		for (int i=start; i<=stop; i+=BASE_BLOCK_SIZE)
		{
			baseMemoryBlock[i/BASE_BLOCK_SIZE].initialize();
			baseMemoryBlock[i/BASE_BLOCK_SIZE].writeable=true;
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

		for (int i=start; i<=stop; i+=BASE_BLOCK_SIZE)
		{
			baseMemoryBlock[i/BASE_BLOCK_SIZE].writeable=false;
		}
	}

	public byte getByte(int address)
	{
		if (address>=0 && address<0x100000)
		{
			if(computer.video!=null)
				computer.video.updateVideoRead(address);
			return baseMemoryBlock[address>>>BASE_BLOCK_SIZE_BITS].getByte(address&BASE_BLOCK_SIZE_OFFSET_MASK);
		}
		else
			return extendedMemoryBlock[((address>>>EXTENDED_BLOCK_SIZE_BITS)&0xffff)-0x10].getByte(address&EXTENDED_BLOCK_SIZE_OFFSET_MASK);
	}
	public void setByte(int address, byte value)
	{
		if (address>=0 && address<0x100000)
		{
			baseMemoryBlock[address>>>BASE_BLOCK_SIZE_BITS].setByte(address&BASE_BLOCK_SIZE_OFFSET_MASK,value);
			if(computer.video!=null)
				computer.video.updateVideoWrite(address,value);
		}
		else
			extendedMemoryBlock[((address>>>EXTENDED_BLOCK_SIZE_BITS)&0xffff)-0x10].setByte(address&EXTENDED_BLOCK_SIZE_OFFSET_MASK,value);
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
		if (address>=0 && address<0x100000)
			return baseMemoryBlock[address>>>BASE_BLOCK_SIZE_BITS].initialized;
		else
			return extendedMemoryBlock[((address>>>EXTENDED_BLOCK_SIZE_BITS)&0xffff)-0x10].initialized;
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

		int BLOCK_SIZE;
		
		public MemoryBlock(int BLOCK_SIZE)
		{
			this.BLOCK_SIZE=BLOCK_SIZE;
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
//				return (byte)(-1);
				initialize();
			return rambyte[offset];
		}
		public void setByte(int offset, byte value)
		{
			if (!writeable) return;
			if (!initialized)
				initialize();
			rambyte[offset]=value;
		}
	}
}
