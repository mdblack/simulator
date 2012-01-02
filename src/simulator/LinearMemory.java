//LinearMemory
//shamelessly copied from Java PC's Linear Memory

package simulator;

public class LinearMemory implements MemoryDevice
{
	Computer computer;
	
	//page block size is 4096
	private static final int BLOCK_OFFSET_MASK=0xfff;
	private static final int PAGE_NUMBER_SHIFT=12;
	private static final int PAGES=1<<(32-PAGE_NUMBER_SHIFT);

	public boolean isSupervisor,globalPagesEnabled,pagingDisabled,writeProtectUserPages,pageSizeExtensions,pageCacheEnabled;
	public int directoryBaseAddress=0;
	public int lastPageFaultAddress=0;
	
	//four TLB tables
	private PageTableEntry[] readSupervisorPageTable, readUserPageTable, writeSupervisorPageTable, writeUserPageTable;

	public LinearMemory(Computer computer)
	{
		this.computer=computer;
		pagingDisabled=true;
		flush();
	}
	
	//reading (and writing) bytes works like this:
	//call getPhysicalPageRead on the virtual address to get the physical page base 
	//extract the block offset, add it to the the physical page base, and do the read
	public byte getByte(int address) 
	{
		return computer.physicalMemory.getByte(getPhysicalPageRead(address)|(address & BLOCK_OFFSET_MASK));
	}
	public void setByte(int address, byte value) 
	{
		computer.physicalMemory.setByte(getPhysicalPageWrite(address)|(address & BLOCK_OFFSET_MASK),value);
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
	
	static int quantity=0;
	private class PageTableEntry
	{
		int physicalBaseAddress;
		public PageTableEntry(int address)
		{
			physicalBaseAddress=address;
System.out.println("new page entry: "+address+" "+(quantity++));
		}
	}

	//given a virtual page number, get the physical base address
	private int getPhysicalPageRead(int virtualAddress)
	{
		int virtualPageIndex=virtualAddress>>>PAGE_NUMBER_SHIFT;
		//if paging is disabled, the virtual address becomes the physical address
		if (pagingDisabled)
			return virtualPageIndex<<PAGE_NUMBER_SHIFT;
		//two direct mapped page tables in the TLB: supervisor and user
		if (isSupervisor)
		{
			//if the page table doesn't exist, initialize it
			//initially, all the entries will be NULL
			if (readSupervisorPageTable==null)
				readSupervisorPageTable=new PageTableEntry[PAGES];
			//get the entry's base address
			try
			{
				return readSupervisorPageTable[virtualPageIndex].physicalBaseAddress;
			}
			catch(NullPointerException e){}
			//if the entry was NULL, construct it and repeat
			return validateTLBEntryRead(virtualAddress);
		}
		else
		{
			if (readUserPageTable==null)
				readUserPageTable=new PageTableEntry[PAGES];
			try
			{
				return readUserPageTable[virtualPageIndex].physicalBaseAddress;
			}
			catch(NullPointerException e){}
			return validateTLBEntryRead(virtualAddress);
		}
	}
	//same as read, but with the write page tables
	private int getPhysicalPageWrite(int virtualAddress)
	{
		int virtualPageIndex=virtualAddress>>>PAGE_NUMBER_SHIFT;
		if (pagingDisabled)
			return virtualPageIndex<<PAGE_NUMBER_SHIFT;
		if (isSupervisor)
		{
			if (writeSupervisorPageTable==null)
				writeSupervisorPageTable=new PageTableEntry[PAGES];
			try
			{
				return writeSupervisorPageTable[virtualPageIndex].physicalBaseAddress;
			}
			catch(NullPointerException e){}
			return validateTLBEntryWrite(virtualAddress);
		}
		else
		{
			if (writeUserPageTable==null)
				writeUserPageTable=new PageTableEntry[PAGES];
			try
			{
				return writeUserPageTable[virtualPageIndex].physicalBaseAddress;
			}
			catch(NullPointerException e){}
			return validateTLBEntryWrite(virtualAddress);
		}
	}

	private int validateTLBEntryRead(int virtualAddress)
	{
//		System.out.println("validate TLB entry read "+ virtualAddress);
		int virtualPageIndex=virtualAddress>>>PAGE_NUMBER_SHIFT;
		lastPageFaultAddress=virtualAddress;
		
		//virtual address is divided up as follows:
		//[10 bits: directory entry][10 bits: page table entry][12 bits: block offset]
		
		//get the information location in the page table directory
		//determined by the upper 10 bits. each directory entry is 4 bytes.
		int directoryAddress=directoryBaseAddress | (0xfff & ((virtualPageIndex>>>10)*4));
		int directoryInformation=computer.physicalMemory.getDoubleWord(directoryAddress);
		//is the directory there?
		if ((directoryInformation&1)==0) panic("directory isn't there");

		//extract information about the page table directory
		boolean directoryIsUser = (4&directoryInformation)!=0;
		//extract the page table: determined by the middle 10 bits
		int pageTableEntryAddress=(directoryInformation&0xfffff000) | ((virtualPageIndex*4)&0xfff);
		int pageTableEntry=computer.physicalMemory.getDoubleWord(pageTableEntryAddress);
		//is it there?
		if ((pageTableEntry&1)==0) panic("page table entry isn't there");
		boolean tableIsUser = (4&pageTableEntry)!=0;
		//is a user accessing a supervisor page?
		if ((!tableIsUser || !directoryIsUser) && !isSupervisor) panic("user trying to access a supervisor page");
		//set bit 17 to 1
		if ((pageTableEntry&0x20)==0)
		{
			pageTableEntry|=0x20;
			computer.physicalMemory.setDoubleWord(pageTableEntryAddress, pageTableEntry);
		}
		int physicalBaseAddress=pageTableEntry&0xfffff000;
		//if page caching is disabled, don't make a TLB entry.  just return the physical base address.
		if (!pageCacheEnabled)
			return physicalBaseAddress;

		//save the entry in the TLB
		if (isSupervisor)
			readSupervisorPageTable[virtualPageIndex]=new PageTableEntry(physicalBaseAddress);
		else
			readUserPageTable[virtualPageIndex]=new PageTableEntry(physicalBaseAddress);
		return physicalBaseAddress;
	}
	
	private int validateTLBEntryWrite(int virtualAddress)
	{
//		System.out.println("validate TLB entry write "+ virtualAddress);
		int virtualPageIndex=virtualAddress>>>PAGE_NUMBER_SHIFT;
		lastPageFaultAddress=virtualAddress;
		
		//virtual address is divided up as follows:
		//[10 bits: directory entry][10 bits: page table entry][12 bits: block offset]
		
		//get the information location in the page table directory
		//determined by the upper 10 bits. each directory entry is 4 bytes.
		int directoryAddress=directoryBaseAddress | (0xfff & ((virtualPageIndex>>>10)*4));
		int directoryInformation=computer.physicalMemory.getDoubleWord(directoryAddress);
		//is the directory there?
		if ((directoryInformation&1)==0) panic("directory isn't there");

		//extract information about the page table directory
		boolean directoryIsUser = (4&directoryInformation)!=0;
		//extract the page table: determined by the middle 10 bits
		int pageTableEntryAddress=(directoryInformation&0xfffff000) | ((virtualPageIndex*4)&0xfff);
		int pageTableEntry=computer.physicalMemory.getDoubleWord(pageTableEntryAddress);
		//is it there?
		if ((pageTableEntry&1)==0) panic("page table entry isn't there");
		boolean tableIsUser = (4&pageTableEntry)!=0;
		//is a user accessing a supervisor page?
		if ((!tableIsUser || !directoryIsUser) && !isSupervisor) panic("user trying to access a supervisor page");
		//set bits 17 and 18 to 1
		if ((pageTableEntry&0x60)!=0)
		{
			pageTableEntry|=0x60;
			computer.physicalMemory.setDoubleWord(pageTableEntryAddress, pageTableEntry);
		}
		int physicalBaseAddress=pageTableEntry&0xfffff000;
		//if page caching is disabled, don't make a TLB entry.  just return the physical base address.
		if (!pageCacheEnabled)
			return physicalBaseAddress;

		//save the entry in the TLB
		if (isSupervisor)
			writeSupervisorPageTable[virtualPageIndex]=new PageTableEntry(physicalBaseAddress);
		else
			writeUserPageTable[virtualPageIndex]=new PageTableEntry(physicalBaseAddress);
		return physicalBaseAddress;
	}
	
	public void setSupervisor(boolean value)
	{
		isSupervisor=value;
	}
	public void setPagingEnabled(boolean value)
	{
		pagingDisabled=!value;
		flush();
	}
	public void setPageCacheEnabled(boolean value)
	{
		pageCacheEnabled=value;
	}
	public void setPageSizeExtensionsEnabled(boolean value)
	{
		pageSizeExtensions=value;
		flush();
		if (value)
			panic("implement page size extensions");
	}
	//global pages are not purged from the TLB when the page directory base address is changed
	//right now, just ignore them
	public void setGlobalPagesEnabled(boolean value)
	{
		if (!globalPagesEnabled)
		{
			globalPagesEnabled=value;
			flush();
		}
	}
	public void setWriteProtectUserPages(boolean value)
	{
		if (value)
		{
			panic("implement write protect user pages");
		}
		writeProtectUserPages=value;
	}
	//sets the location of the page table directory in physical memory
	public void setPageDirectoryBaseAddress(int address)
	{
		directoryBaseAddress=address&0xfffff000;
		flush();
	}
	//obliterate the TLB page tables
	public void flush()
	{
		readUserPageTable=null;
		readSupervisorPageTable=null;
		writeUserPageTable=null;
		writeSupervisorPageTable=null;
	}
	private void panic(String message)
	{
		System.out.println("PANIC in linear memory: "+message);
		//System.exit(0);
	}
}

/*

import java.util.HashSet;
import java.util.Set;

import simulator.Processor.Processor_Exception;

public class LinearMemory implements MemoryDevice 
{
	Computer computer;
	
	private byte[] pageSize;
	private final Set<Integer> nonGlobalPages;
	public int baseAddress,lastAddress;
	public boolean isSupervisor,globalPagesEnabled,pagingDisabled,writeProtectUserPages,pageSizeExtensions,pageCacheEnabled;
	
	private static final byte FOUR_K=(byte)0;
	private static final byte FOUR_M=(byte)1;
	
	private static final int BLOCK_SIZE=4*1024;
	private static final int BLOCK_MASK=BLOCK_SIZE-1;
	private static final int INDEX_MASK=~(BLOCK_MASK);
	private static final int INDEX_SHIFT=12;
	private static final int INDEX_SIZE=1<<(32-INDEX_SHIFT);
	
	private MemoryDevice[] readUserIndex, readSupervisorIndex, writeUserIndex, writeSupervisorIndex, readIndex, writeIndex;
	
	public LinearMemory(Computer computer)
	{
		this.computer=computer;
		baseAddress=0;
		lastAddress=0;
		pagingDisabled=true;
		globalPagesEnabled=false;
		writeProtectUserPages=false;
		pageSizeExtensions=false;
		
		nonGlobalPages=new HashSet<Integer>();
		
		pageSize=new byte[INDEX_SIZE];
		for (int i=0; i<INDEX_SIZE; i++)
			pageSize[i]=FOUR_K;
	}

	private MemoryDevice[] createReadIndex()
	{
		if (isSupervisor)
			return(readIndex=readSupervisorIndex=new MemoryDevice[INDEX_SIZE]);
		else
			return(readIndex=readUserIndex=new MemoryDevice[INDEX_SIZE]);
	}

	private MemoryDevice[] createWriteIndex()
	{
		if (isSupervisor)
			return(writeIndex=writeSupervisorIndex=new MemoryDevice[INDEX_SIZE]);
		else
			return(writeIndex=writeUserIndex=new MemoryDevice[INDEX_SIZE]);
	}
	
	private void setReadIndexValue(int index, MemoryDevice value)
	{
		if (readIndex==null)
			createReadIndex()[index]=value;
		else
			readIndex[index]=value;
	}
	private void setWriteIndexValue(int index, MemoryDevice value)
	{
		if (writeIndex==null)
			createWriteIndex()[index]=value;
		else
			writeIndex[index]=value;
	}
	private MemoryDevice getReadIndexValue(int index)
	{
		if (readIndex==null)
			return createReadIndex()[index];
		else
			return readIndex[index];
	}
	private MemoryDevice getWriteIndexValue(int index)
	{
		if (writeIndex==null)
			return createWriteIndex()[index];
		else
			return writeIndex[index];
	}
	
	public void setSupervisor(boolean value)
	{
		isSupervisor=value;
		if (isSupervisor)
		{
			readIndex=readSupervisorIndex;
			writeIndex=writeSupervisorIndex;
		}
		else
		{
			readIndex=readUserIndex;
			writeIndex=writeUserIndex;
		}
	}
	
	public void setPagingEnabled(boolean value)
	{
		pagingDisabled=!value;
		flush();
	}
	public void setPageCacheEnabled(boolean value)
	{
		pageCacheEnabled=value;
	}
	public void setPageSizeExtensionsEnabled(boolean value)
	{
		pageSizeExtensions=value;
		flush();
	}
	public void setGlobalPagesEnabled(boolean value)
	{
		if (!globalPagesEnabled)
		{
			globalPagesEnabled=value;
			flush();
		}
	}
	public void setWriteProtectUserPages(boolean value)
	{
		if (value)
		{
			if (writeSupervisorIndex!=null)
				for (int i=0; i<INDEX_SIZE; i++)
					nullIndex(writeSupervisorIndex,i);
		}
		writeProtectUserPages=value;
	}
	public void flush()
	{
		for (int i=0; i<INDEX_SIZE; i++)
			pageSize[i]=FOUR_K;
		nonGlobalPages.clear();
		readUserIndex=null;
		readSupervisorIndex=null;
		writeUserIndex=null;
		writeSupervisorIndex=null;
	}
	private void nullIndex(MemoryDevice[] array, int index)
	{
		if (array!=null)
			array[index]=null;
	}
	private void partialFlush()
	{
		if (globalPagesEnabled)
		{
			for (Integer value: nonGlobalPages)
			{
				int index=value.intValue();
				nullIndex(readSupervisorIndex,index);
				nullIndex(writeSupervisorIndex,index);
				nullIndex(readUserIndex,index);
				nullIndex(writeUserIndex,index);
				pageSize[index]=FOUR_K;
			}
			nonGlobalPages.clear();
		}
		else
			flush();
	}
	public void setPageDirectoryBaseAddress(int address)
	{
		baseAddress=address&0xfffff000;
		partialFlush();
	}
	public void invalidateTLBEntry(int offset)
	{
		int index=offset>>>INDEX_SHIFT;
		if (pageSize[index]==FOUR_K)
		{
			nullIndex(readSupervisorIndex,index);
			nullIndex(writeSupervisorIndex,index);
			nullIndex(readUserIndex,index);
			nullIndex(writeUserIndex,index);
			nonGlobalPages.remove(Integer.valueOf(index));
		}
		else
		{
			index&=0xffc00;
			for (int i=0; i<1024; i++,index++)
			{
				nullIndex(readSupervisorIndex,index);
				nullIndex(writeSupervisorIndex,index);
				nullIndex(readUserIndex,index);
				nullIndex(writeUserIndex,index);
				nonGlobalPages.remove(Integer.valueOf(index));				
			}
		}
	}
	
	private class PhysicalBlock implements MemoryDevice
	{
		int base;
		public PhysicalBlock(int base)
		{
			this.base=base;
		}
		public byte getByte(int address) 
		{
			return computer.physicalMemory.getByte(base+address);
		}
		public int getDoubleWord(int address) 
		{
			return computer.physicalMemory.getDoubleWord(base+address);
		}
		public long getQuadWord(int address) 
		{
			return computer.physicalMemory.getQuadWord(base+address);
		}
		public short getWord(int address) 
		{
			return computer.physicalMemory.getWord(base+address);
		}
		public void setByte(int address, byte value) 
		{
			computer.physicalMemory.setByte(base+address, value);
		}
		public void setDoubleWord(int address, int value) 
		{
			computer.physicalMemory.setDoubleWord(base+address, value);
		}
		public void setQuadWord(int address, long value) 
		{
			computer.physicalMemory.setQuadWord(base+address, value);
		}
		public void setWord(int address, short value) 
		{
			computer.physicalMemory.setWord(base+address, value);
		}
		
	}
	
	private MemoryDevice validateTLBEntryRead(int offset)
	{
		System.out.println("validate TLB entry read "+ offset);
		int idx=offset>>>INDEX_SHIFT;
		if (pagingDisabled)
		{
			setReadIndexValue(idx,new PhysicalBlock(offset));
			return readIndex[idx];
		}
		lastAddress=offset;
		int directoryAddress=baseAddress|(0xffc&(offset>>>20));
		int directoryRawBits=computer.physicalMemory.getDoubleWord(directoryAddress);
		boolean directoryPresent=(0x1 & directoryRawBits)!=0;
		if (!directoryPresent)
		{
			if (isSupervisor)
				panic("not present rs");
			else
				panic("not present ru");
		}
		boolean directoryGlobal=globalPagesEnabled&&((0x100&directoryRawBits)!=0);
		boolean directoryUser=(0x4&directoryRawBits)!=0;
		boolean directoryIs4MegPage=((0x80&directoryRawBits)!=0)&&pageSizeExtensions;
		if (directoryIs4MegPage)
		{
			if (!directoryUser&&!isSupervisor)
				panic("protection violation ru");
			if ((directoryRawBits&0x20)==0)
			{
				directoryRawBits|=0x20;
				computer.physicalMemory.setDoubleWord(directoryAddress,directoryRawBits);
			}
			int fourMegPageStartAddress=0xffc00000&directoryRawBits;
			if (!pageCacheEnabled)
				return new PhysicalBlock(fourMegPageStartAddress|(offset&0x3fffff));
			int tableIndex=(0xffc00000&offset)>>>12;
			for (int i=0; i<1024; i++)
			{
				MemoryDevice m=new PhysicalBlock(fourMegPageStartAddress);
				fourMegPageStartAddress+=BLOCK_SIZE;
				pageSize[tableIndex]=FOUR_M;
				setReadIndexValue(tableIndex++,m);
				if (directoryGlobal)
					continue;
				nonGlobalPages.add(Integer.valueOf(i));
			}
			return readIndex[idx];
		}
		else
		{
			int directoryBaseAddress=directoryRawBits&0xfffff000;
			int tableAddress=directoryBaseAddress|((offset>>>10)&0xffc);
			int tableRawBits=computer.physicalMemory.getDoubleWord(tableAddress);
			boolean tablePresent=(0x1&tableRawBits)!=0;
			if (!tablePresent)
			{
				if (isSupervisor)
					panic("not present rs");
				else
					panic("not present ru");
			}
			boolean tableGlobal=globalPagesEnabled&&((0x100&tableRawBits)!=0);
			boolean tableUser=(0x4&tableRawBits)!=0;
			boolean pageIsUser=tableUser&&directoryUser;
			if (!pageIsUser&&!isSupervisor)
				panic("protection violation ru");
			if ((tableRawBits&0x20)==0)
			{
				tableRawBits|=0x20;
				computer.physicalMemory.setDoubleWord(tableAddress,tableRawBits);
			}
			int fourKStartAddress=tableRawBits&0xfffff000;
			if (!pageCacheEnabled)
				return new PhysicalBlock(fourKStartAddress);
			pageSize[idx]=FOUR_K;
			if (!tableGlobal)
				nonGlobalPages.add(Integer.valueOf(idx));
			setReadIndexValue(idx,new PhysicalBlock(fourKStartAddress));
			return readIndex[idx];
		}
	}

	private MemoryDevice validateTLBEntryWrite(int offset)
	{
		System.out.println("validate TLB entry write "+ offset);
		int idx=offset>>>INDEX_SHIFT;
		if (pagingDisabled)
		{
			setWriteIndexValue(idx,new PhysicalBlock(offset));
			return writeIndex[idx];
		}
		lastAddress=offset;
		int directoryAddress=baseAddress|(0xffc&(offset>>>20));
		int directoryRawBits=computer.physicalMemory.getDoubleWord(directoryAddress);
		boolean directoryPresent=(0x1 & directoryRawBits)!=0;
		if (!directoryPresent)
		{
			if (isSupervisor)
				panic("not present ws");
			else
				panic("not present wu");
		}
		boolean directoryGlobal=globalPagesEnabled&&((0x100&directoryRawBits)!=0);
		boolean directoryUser=(0x4&directoryRawBits)!=0;
		boolean directoryReadWrite=(0x2&directoryRawBits)!=0;
		boolean directoryIs4MegPage=((0x80&directoryRawBits)!=0)&&pageSizeExtensions;
		if (directoryIs4MegPage)
		{
			if (directoryUser)
			{
				if(!directoryReadWrite)
				{
					if(isSupervisor)
					{
						if (writeProtectUserPages)
							panic("protection violation ws");
					}
					else
						panic("protection violation wu");
				}
			}
			else
			{
				if (directoryReadWrite)
				{
					if (!isSupervisor)
						panic("protection violation wu");
				}
				else
				{
					if (!isSupervisor)
						panic("protection violation wu");
					else
						panic("protection violation ws");
				}
			}
			
			if ((directoryRawBits&0x60)!=0x60)
			{
				directoryRawBits|=0x60;
				computer.physicalMemory.setDoubleWord(directoryAddress,directoryRawBits);
			}
			int fourMegPageStartAddress=0xffc00000&directoryRawBits;
			if (!pageCacheEnabled)
				return new PhysicalBlock(fourMegPageStartAddress|(offset&0x3fffff));
			int tableIndex=(0xffc00000&offset)>>>12;
			for (int i=0; i<1024; i++)
			{
				MemoryDevice m=new PhysicalBlock(fourMegPageStartAddress);
				fourMegPageStartAddress+=BLOCK_SIZE;
				pageSize[tableIndex]=FOUR_M;
				setWriteIndexValue(tableIndex++,m);
				if (directoryGlobal)
					continue;
				nonGlobalPages.add(Integer.valueOf(i));
			}
			return writeIndex[idx];
		}
		else
		{
			int directoryBaseAddress=directoryRawBits&0xfffff000;
			int tableAddress=directoryBaseAddress|((offset>>>10)&0xffc);
			int tableRawBits=computer.physicalMemory.getDoubleWord(tableAddress);
			boolean tablePresent=(0x1&tableRawBits)!=0;
			if (!tablePresent)
			{
				if (isSupervisor)
					panic("not present ws");
				else
					panic("not present wu");
			}
			boolean tableGlobal=globalPagesEnabled&&((0x100&tableRawBits)!=0);
			boolean tableReadWrite=(0x2&tableRawBits)!=0;
			boolean tableUser=(0x4&tableRawBits)!=0;
			boolean pageIsUser=tableUser&&directoryUser;
			boolean pageIsReadWrite=tableReadWrite||directoryReadWrite;			
			if (pageIsUser)
				pageIsReadWrite=tableReadWrite&&directoryReadWrite;
			if (pageIsUser)
			{
				if (!pageIsReadWrite)
				{
					if (isSupervisor)
					{
						if (writeProtectUserPages)
							panic("protection violation ws");
					}
					else
						panic("protection violation wu");
				}
			}
			else
			{
				if (pageIsReadWrite)
				{
					if (!isSupervisor)
						panic("protection violation wu");
				}
				else
				{
					if (isSupervisor)
						panic("protection violation ws");
					else
						panic("protection violation wu");
				}
			}
			if ((tableRawBits&0x60)!=0x60)
			{
				tableRawBits|=0x60;
				computer.physicalMemory.setDoubleWord(tableAddress,tableRawBits);
			}
			int fourKStartAddress=tableRawBits&0xfffff000;
			if (!pageCacheEnabled)
				return new PhysicalBlock(fourKStartAddress);
			pageSize[idx]=FOUR_K;
			if (!tableGlobal)
				nonGlobalPages.add(Integer.valueOf(idx));
			setWriteIndexValue(idx,new PhysicalBlock(fourKStartAddress));
			return writeIndex[idx];
		}
	}

	private void panic(String message)
	{
		System.out.println("PANIC in linear memory: "+message);
		System.exit(0);
	}
	
	public byte getByte(int address) 
	{
		try
		{
			int offset=address>>>INDEX_SHIFT;
			return getReadIndexValue(offset).getByte(address & BLOCK_MASK);
		}
		catch(NullPointerException e){}
		catch(Processor_Exception e){}
		return validateTLBEntryRead(address).getByte(address&BLOCK_MASK);
	}
	public short getWord(int address) 
	{
		try
		{
			int offset=address>>>INDEX_SHIFT;
			return getReadIndexValue(offset).getWord(address & BLOCK_MASK);
		}
		catch(NullPointerException e){}
		catch(Processor_Exception e){}
		MemoryDevice m=validateTLBEntryRead(address);
		return validateTLBEntryRead(address).getWord(address&BLOCK_MASK);
	}
	
	public int getDoubleWord(int address) 
	{
		try
		{
			int offset=address>>>INDEX_SHIFT;
			return getReadIndexValue(offset).getDoubleWord(address & BLOCK_MASK);
		}
		catch(NullPointerException e){}
		catch(Processor_Exception e){}
		MemoryDevice m=validateTLBEntryRead(address);
		return validateTLBEntryRead(address).getDoubleWord(address&BLOCK_MASK);
	}

	public long getQuadWord(int address) 
	{
		try
		{
			int offset=address>>>INDEX_SHIFT;
			return getReadIndexValue(offset).getQuadWord(address & BLOCK_MASK);
		}
		catch(NullPointerException e){}
		catch(Processor_Exception e){}
		MemoryDevice m=validateTLBEntryRead(address);
		return validateTLBEntryRead(address).getQuadWord(address&BLOCK_MASK);
	}

	public void setByte(int address, byte value) 
	{
		try
		{
			int offset=address>>>INDEX_SHIFT;
			getWriteIndexValue(offset).setByte(address & BLOCK_MASK,value);
		}
		catch(NullPointerException e){}
		catch(Processor_Exception e){}
		validateTLBEntryWrite(address).setByte(address&BLOCK_MASK,value);
	}

	public void setDoubleWord(int address, int value) 
	{
		try
		{
			int offset=address>>>INDEX_SHIFT;
			getWriteIndexValue(offset).setDoubleWord(address & BLOCK_MASK,value);
		}
		catch(NullPointerException e){}
		catch(Processor_Exception e){}
		validateTLBEntryWrite(address).setDoubleWord(address&BLOCK_MASK,value);
	}

	public void setQuadWord(int address, long value) 
	{
		try
		{
			int offset=address>>>INDEX_SHIFT;
			getWriteIndexValue(offset).setQuadWord(address & BLOCK_MASK,value);
		}
		catch(NullPointerException e){}
		catch(Processor_Exception e){}
		validateTLBEntryWrite(address).setQuadWord(address&BLOCK_MASK,value);
	}

	public void setWord(int address, short value) 
	{
		try
		{
			int offset=address>>>INDEX_SHIFT;
			getWriteIndexValue(offset).setWord(address & BLOCK_MASK,value);
		}
		catch(NullPointerException e){}
		catch(Processor_Exception e){}
		validateTLBEntryWrite(address).setWord(address&BLOCK_MASK,value);
	}
}*/
