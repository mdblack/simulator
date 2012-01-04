package simulator;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

public class DiskSectorGUI extends AbstractGUI
{
	public int id;
	private boolean isFloppy;
	private int fileSystemType=0;	//0=unknown, 1=fat12, 2=fat16
	private int bytesPerSector=0;
	private int fatTables=0;
	private int sectorsPerCluster=0;
	private int sectorsPerFat=0;
	private int reservedSectorCount=0;
	private int directorySectors=0;
	private boolean bootable=false;
	private int totalSectors=0;
	private int bootOffset=0;
	private int fatOffset=0;
	private int directoryOffset=0;
	private int fileSystemOffset=0;
	private int partitionOffset=0;
	private int cylinders,heads,sectors;
	private String name;
	private Disk disk;
	private int[] sectorHighlightList=new int[0];
	private int currentTrack;
	private int currentSector;
	private int currentHead;
	private int currentOperation=-1;		//1=read,2=write,0=show last operation,-1=show nothing

	private static final int BLOCKWIDTH=20, BLOCKHEIGHT=10, SECTORLABELWIDTH=32;
	private static final int SECTOR_MBR=0;
	private static final int SECTOR_BOOT=1;
	private static final int SECTOR_FAT=2;
	private static final int SECTOR_DIRECTORY=3;
	private static final int SECTOR_MIDDLE_OF_FILE=4;
	private static final int SECTOR_END_OF_FILE=5;
	private static final int SECTOR_EMPTY=6;
	private static final int SECTOR_READ=7;
	private static final int SECTOR_WRITE=8;
	private static final int SECTOR_SELECT=9;

	public static final int W=600,H=500;
	
	private static final Color[] sectorColor=new Color[]
	{
		new Color(0,0,150),	//MBR
		new Color(0,0,255),	//BOOT
		new Color(255,0,255),	//FAT
		new Color(0,255,255),	//DIRECTORY
		new Color(100,100,0),	//MIDDLE_OF_FILE
		new Color(150,150,0),	//END_OF_FILE
		new Color(255,255,0),	//EMPTY
		new Color(0,255,0),	//READ
		new Color(255,0,0),	//WRITE
		new Color(0,0,0)	//SELECT
	};


	public DiskSectorGUI(Computer computer, int id)
	{
		super(computer,"",W,H,true,true,true,false);
		this.id=id;
		this.isFloppy=id<2;
	}
	
	public void closeGUI()
	{
		computer.sectorGUI[id]=null;
	}

	public void redraw(String name, int cylinders, int heads, int sectors, Disk disk)
	{
		this.cylinders=cylinders;
		this.heads=heads;
		this.sectors=sectors;
		this.name=name;
		this.disk=disk;

		frame.setTitle(name);
		decodeDiskParameters();

		refresh();
	}

	public void read(int sector)
	{
		if (sectors*heads==0) return;
		currentTrack=(sector/(sectors*heads));
		currentHead=((sector/sectors)%heads);
		currentSector=sector%sectors;
		currentOperation=1;

		repaint();
		updateSectorLabel("Reading from ",sector);
	}

	public void write(int sector)
	{
		if (sectors*heads==0) return;
		currentTrack=(sector/(sectors*heads));
		currentHead=((sector/sectors)%heads);
		currentSector=sector%sectors;
		currentOperation=2;

		repaint();
		updateSectorLabel("Writing to ",sector);
	}

	public int width()
	{
		return W-SECTORLABELWIDTH;
	}

	public int viewWidth()
	{
		return width()-SECTORLABELWIDTH;
	}

	public int height()
	{
		int numBlocks=sectors*heads*cylinders;
		return BLOCKHEIGHT*(numBlocks/(viewWidth()/BLOCKWIDTH)+1);
	}

	private void popupSectorBox(int sector)
	{
		new SectorBoxGUI(computer,sector);
	}

	private void decodeDiskParameters()
	{
		byte[] bootSector=new byte[512];
		disk.read(0,bootSector,1);
		if (!isFloppy)
		{
			//read the first partition offset from the MBR
			partitionOffset=(bootSector[0x1c6]&0xff) | ((bootSector[0x1c7]<<8)&0xff00) | ((bootSector[0x1c8]<<16)&0xff0000) | ((bootSector[0x1c9]<<24)&0xff000000);
			disk.read(partitionOffset,bootSector,1);	//skip the MBR
		}
		if (bootSector[0x1fe]==0x55 && bootSector[0x1ff]==0xaa)
			bootable=true;
		bytesPerSector=(bootSector[0xb]&0xff) | ((bootSector[0xc]<<8)&0xff00);
		sectorsPerCluster=bootSector[0xd]&0xff;
		reservedSectorCount=(bootSector[0xe]&0xff) | ((bootSector[0xf]<<8)&0xff00);
		fatTables=bootSector[0x10]&0xff;
		directorySectors = (bootSector[0x11]&0xff) | ((bootSector[0x12]<<8)&0xff00);
		directorySectors = directorySectors * 0x20 / 0x200;
		sectorsPerFat = (bootSector[0x16]&0xff) | ((bootSector[0x17]<<8)&0xff00);
		totalSectors=(bootSector[0x13]&0xff) | ((bootSector[0x14]<<8)&0xff00);
		if (totalSectors==0)
			totalSectors=(bootSector[0x20]&0xff) | ((bootSector[0x21]<<8)&0xff00) | ((bootSector[0x22]<<16)&0xff0000) | ((bootSector[0x23]<<24)&0xff000000);

		bootOffset=0;
		if (!isFloppy)
			bootOffset=partitionOffset;
		fatOffset=bootOffset+reservedSectorCount;
		directoryOffset=fatOffset+fatTables*sectorsPerFat;
		fileSystemOffset=directoryOffset+directorySectors;

		//find the file system type
		//first see if it tells us directly
		if (bootSector[0x36]=='F' && bootSector[0x37]=='A' && bootSector[0x38]=='T' && bootSector[0x39]=='1' && bootSector[0x3A]=='6')
			fileSystemType=2;
		else if (bootSector[0x36]=='F' && bootSector[0x37]=='A' && bootSector[0x38]=='T' && bootSector[0x39]=='1' && bootSector[0x3A]=='2')
			fileSystemType=1;
		else
		{
			//calculate the cluster count
			int clusterCount=0;
			if (sectorsPerCluster!=0)
				clusterCount=2+(totalSectors-fileSystemOffset)/sectorsPerCluster;
			if (clusterCount>=2 && clusterCount<=0xff6)
				fileSystemType=1;
			else if (clusterCount>=0xff7 && clusterCount<=0xfff6)
				fileSystemType=2;
			else
				System.out.println("Can't identify file system type");
		}

		scourDirectories();

//		System.out.println("bytes per sector "+bytesPerSector);	
//		System.out.println("sectors per cluster "+sectorsPerCluster);
//		System.out.println("reserved sector count "+reservedSectorCount);	
//		System.out.println("fat tables "+fatTables);	
//		System.out.println("directory sectors "+directorySectors);
//		System.out.println("sectors per fat table "+sectorsPerFat);
//		System.out.println("total sectors "+totalSectors);	
//		System.out.println("file system type "+fileSystemType);	

	}

	public void doPaint(Graphics g)
	{
		byte[] fat=getfat();

		int xblocks = viewWidth()/BLOCKWIDTH;
		int yblocks = height()/BLOCKHEIGHT;
		for (int y=0; y<yblocks; y++)
		{
			//don't draw blocks that aren't visible
			int visibleStart = yblocks*scrollPane.getVerticalScrollBar().getValue()/scrollPane.getVerticalScrollBar().getMaximum();
			int visibleEnd = visibleStart + scrollPane.getVerticalScrollBar().getVisibleAmount()*yblocks/scrollPane.getVerticalScrollBar().getMaximum();

			if (y<visibleStart-5 || y>visibleEnd+5)
				continue;

			if(y*xblocks>=sectors*cylinders*heads)
				break;

			g.setColor(Color.BLACK);
			g.setFont(new Font("Dialog",0,10));
			g.drawString(Integer.toHexString(y*xblocks),0,y*BLOCKHEIGHT+BLOCKHEIGHT-2);

			for (int x=0; x<xblocks; x++)
			{
				//don't depict non-existent blocks
				int blockNumber=y*xblocks+x;
				if(blockNumber>=sectors*cylinders*heads)
					break;

				g.setColor(sectorColor[decodeSectorType(blockNumber,fat)]);

				//highlight sectors in list
				int i;
				for (i=0; i<sectorHighlightList.length; i++)
					if(blockNumber==sectorHighlightList[i])
						break;
				if (i!=sectorHighlightList.length)
					g.setColor(sectorColor[SECTOR_SELECT]);

				g.fillRect(SECTORLABELWIDTH+x*BLOCKWIDTH,y*BLOCKHEIGHT,BLOCKWIDTH-1,BLOCKHEIGHT-1);
			}
		}
	}

	private byte[] getfat()
	{
		byte[] fat=new byte[sectorsPerFat*512];
		disk.read(fatOffset,fat,sectorsPerFat);
		return fat;
	}

	private int decodeSectorType(int sector, byte[] fat)
	{
		if (sector==currentHead*sectors+currentSector+currentTrack*heads*sectors)
		{
			if(currentOperation==1)
				return SECTOR_READ;
			else if (currentOperation==2)
				return SECTOR_WRITE;
		}

		if (sector<bootOffset)
			return SECTOR_MBR;
		else if (sector<fatOffset)
			return SECTOR_BOOT;
		else if (sector<directoryOffset)
			return SECTOR_FAT;
		else if (sector<fileSystemOffset)
			return SECTOR_DIRECTORY;
		else
		{
			int entry = (sector-fileSystemOffset)/sectorsPerCluster + 2;
			int fatcode = fatdecode(entry,fat);

			if ((fatcode>=0xff8 && fileSystemType==1)||(fatcode>=0xfff8 && fileSystemType==2))
				return SECTOR_END_OF_FILE;
			else if (fatcode>=2)
				return SECTOR_MIDDLE_OF_FILE;
			else
				return SECTOR_EMPTY;
		}
	}

	private int fatdecode(int entry,byte[] fat)
	{
		int fatentry;

		if (fileSystemType==1)
		{
			if (entry%2==1)
				fatentry=((fat[3*(entry-1)/2+1]>>>4)&0xf) | ((fat[3*(entry-1)/2+2]<<4)&0xff0);
			else
				fatentry=((fat[3*entry/2])&0xff)|(((fat[3*entry/2+1]&0xf)<<8)&0xf00);
			return 0xfff&fatentry;
		}
		else if (fileSystemType==2)
		{
			fatentry=(fat[entry*2]&0xff) | ((fat[entry*2+1]<<8)&0xff00);
			return 0xffff&fatentry;
		}
		else return 0;
	}

	private static final int MAX_FILES=100000;
	private String[] fileList = new String[MAX_FILES];
	private int[] fileListStartSector = new int[MAX_FILES];
	private int fileCount=0;

	//find if the sector matches a file in the database of files
	private String findFile(int sector)
	{
		int i;
		for (i=0; i<fileCount; i++)
		{
			if (fileListStartSector[i]==sector)
				break;
		}
		if (i==fileCount)
			return "";
		else
			return fileList[i];
	}

	//search through the directory and build a database of files, indexed by starting sector
	private void scourDirectories()
	{
		fileCount=0;
		if (fileSystemType==0)
			return;

		byte[] rootDirectory = new byte[512*directorySectors];
		disk.read(directoryOffset,rootDirectory,directorySectors);
		scourDirectoriesRecursively(rootDirectory,"");
	}

	private void scourDirectoriesRecursively(byte[] directory, String path)
	{
		for (int i=0; i<directory.length; i+=0x20)
		{
			//get the name
			String name="";
			if (directory[i]==0)
					break;
			if ((directory[i]&0xff)==0xe5 || directory[i]==0x2e)
					continue;
			for (int j=0; j<8; j++)
			{
				if (directory[i+j]==0x20)
					break;
				name=name+(char)directory[i+j];
			}
			//skip if it's an invalid entry
			if (name=="")
				continue;
			String extension = ""+(char)directory[i+8]+(char)directory[i+9]+(char)directory[i+10];
			//get the first sector
			int sector = (directory[i+0x1a]&0xff) | ((directory[i+0x1b]<<8)&0xff00);
			sector = (sector-2)*sectorsPerCluster+fileSystemOffset;
			//save it
			if (fileCount>=MAX_FILES)
				return;
			fileList[fileCount]=path;
			if (path!="")
				fileList[fileCount]+="\\";
			fileList[fileCount]+=name;
			if ((directory[i+0x0b]&0x10)==0)
				fileList[fileCount]+="."+extension;
			fileListStartSector[fileCount]=sector;
			fileCount++;
			//if it's not a subdirectory, continue to the next file
			if ((directory[i+0x0b]&0x10)==0)
				continue;
			//load the subdirectory
			path=path+"\\"+name;
			int[] dirSectors = findSectorsInFile(sector);
			byte[] dir = new byte[512];
			for (int j=0; j<dirSectors.length; j++)
			{
				//for each sector of the directory, call again
				disk.read(dirSectors[j],dir,1);
				scourDirectoriesRecursively(dir,path);
			}
		}
	}

	//given a sector, find all the other sectors comprising the file
	private int[] findSectorsInFile(int sector)
	{
		if (sector<fileSystemOffset)
			return new int[0];
		byte[] fat = getfat();
		int entry = (sector-fileSystemOffset)/sectorsPerCluster + 2;
		int e = fatdecode(entry,fat);
		if (e<2)
			return new int[0];
		while(true)
		{
			boolean found=false;
			e=2;
			while((fileSystemType==1 && fat.length>3*e/2+1) || (fileSystemType==2 && fat.length>2*e+1))
			{
				if (fatdecode(e,fat)==entry)
				{
					found=true;
					break;
				}
				e++;
			}
			if (!found)
				break;
			entry=e;
		}
		int[] entries = new int[totalSectors];
		int entryCount=0;
		entries[entryCount++]=entry;
		while(true)
		{
			e=fatdecode(entry,fat);
			if (fileSystemType==1 && e>=0xff8)
				break;
			if (fileSystemType==2 && e>=0xfff8)
				break;
			entry=e;
			entries[entryCount++]=entry;
		}
		int[] sectorsInFile = new int[entryCount*sectorsPerCluster];
		for (int i=0; i<entryCount; i++)
			for (int j=0; j<sectorsPerCluster; j++)
				sectorsInFile[i*sectorsPerCluster+j]=(entries[i]-2)*sectorsPerCluster+fileSystemOffset+j;
		return sectorsInFile;
	}

	private void updateSectorLabel(String label, int block)
	{
		String sectorLabel=label;
		sectorHighlightList=findSectorsInFile(block);
		if (sectorHighlightList.length>0)
			sectorLabel+=findFile(sectorHighlightList[0]);
		else if (block<bootOffset)
			sectorLabel+="Master Boot Record";
		else if (block<fatOffset)
			sectorLabel+="Boot Loader";
		else if (block<directoryOffset)
			sectorLabel+="File Allocation Table";
		else if (block<fileSystemOffset)
			sectorLabel+="Root Directory";
		else
			sectorLabel+="Empty Sector";
		setStatusLabel(sectorLabel);
	}

	public void mouseExit(MouseEvent e) 
	{
		sectorHighlightList=new int[0];
		repaint();
		setStatusLabel("");
	}

	public void mouseClick(MouseEvent e)
	{
		int x=e.getX();
		int y=e.getY();
		if (x<SECTORLABELWIDTH || x>=viewWidth()+SECTORLABELWIDTH)
			return;
		if (y<0 || y>=height())
			return;

		int blockx=(x-SECTORLABELWIDTH)/BLOCKWIDTH;
		int blocky=y/BLOCKHEIGHT;
		int block=blocky*(viewWidth()/BLOCKWIDTH)+blockx;
		if (block>=sectors*heads*cylinders)
			return;
		popupSectorBox(block);
	}

	public void mouseMove(MouseEvent e)
	{
		int x=e.getX();
		int y=e.getY();
		if (x<SECTORLABELWIDTH || x>=viewWidth()+SECTORLABELWIDTH)
			return;
		if (y<0 || y>=height()+MARGIN)
			return;

		int blockx=(x-SECTORLABELWIDTH)/BLOCKWIDTH;
		int blocky=y/BLOCKHEIGHT;
		int block=blocky*(viewWidth()/BLOCKWIDTH)+blockx;
		if (block>=sectors*heads*cylinders)
			return;
		repaint();
		updateSectorLabel("",block);
	}

	private class SectorBoxGUI extends AbstractGUI
	{
		static final int ROWHEIGHT=20;
		static final int BYTEWIDTH=8;
		static final int ADDRESSWIDTH=40;

		int selectedSector;
		byte[] sector;

		public SectorBoxGUI(Computer computer, int selectedSector)
		{
			super(computer,"Sector "+Integer.toHexString(selectedSector),480,300,true,true,true,false);
			this.selectedSector=selectedSector;
			sector = new byte[512];
			disk.read(selectedSector,sector,1);
			refresh();
		}
		
		public void closeGUI()
		{
			computer.sectorGUI[id]=null;
		}

		public int width()
		{
			return ADDRESSWIDTH+BYTEWIDTH*ROWHEIGHT*3/2+10+BYTEWIDTH*ROWHEIGHT;
		}
		public int height()
		{
			return ROWHEIGHT*(512/BYTEWIDTH);
		}

		public void doPaint(Graphics g)
		{
			g.setFont(new Font("Dialog",0,12));

			//step in increments of BYTEWIDTH
			for (int y=0; y<512/BYTEWIDTH; y++)
			{
				if (y%2==0) g.setColor(new Color(220,220,220));
				else g.setColor(Color.WHITE);
				g.fillRect(0,y*ROWHEIGHT,width(),ROWHEIGHT);
				
				g.setColor(Color.BLACK);
				g.drawString(""+Integer.toHexString(y*BYTEWIDTH)+": ",10,y*ROWHEIGHT+ROWHEIGHT-3);
				for (int x=0; x<BYTEWIDTH; x++)
				{
					String dataString=Integer.toHexString(0xf&(sector[x+y*8]>>4))+Integer.toHexString(0xf&sector[x+y*8]);
					g.drawString(dataString,ADDRESSWIDTH+x*ROWHEIGHT*3/2,y*ROWHEIGHT+ROWHEIGHT-3);

					//print ascii
					char c3=(char)sector[x+y*8];
					if(c3<' ') c3='.';
					else if(c3>'z') c3='.';
					g.drawString(""+c3,ADDRESSWIDTH+BYTEWIDTH*ROWHEIGHT*3/2+10+x*ROWHEIGHT,y*ROWHEIGHT+ROWHEIGHT-3);
				}
			}
		}

		int drawaddress=0;

		public void mouseClick(MouseEvent e)
		{
			int x=e.getX();
			int y=e.getY();
			if (y<0 || y>=height() || x<ADDRESSWIDTH || x>=ADDRESSWIDTH+BYTEWIDTH*ROWHEIGHT*3/2)
				return;

			int by=y/ROWHEIGHT;
			int bx=(x-ADDRESSWIDTH)/(3*ROWHEIGHT/2);

			drawaddress=BYTEWIDTH*(y/ROWHEIGHT)+bx;
			String baseLabel="New value for "+Integer.toHexString(drawaddress)+": ";
			statusEdit(baseLabel,2,true);
		}

		public void statusEdited(String keys)
		{
			keys=keys.toLowerCase();
			sector[drawaddress]=(byte)Integer.parseInt(keys,16);
			
			disk.write(selectedSector,sector,1);
			repaint();

			drawaddress++;
			if (drawaddress>=512)
				return;

			String baseLabel="New value for "+Integer.toHexString(drawaddress)+": ";
			statusEdit(baseLabel,2,true);
		}
	}
}
