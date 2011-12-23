package simulator;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.io.*;

public class FileTransferGUI extends AbstractGUI
{
	private static final int MARGIN=10,ROWSIZE=20,TEXTWIDTH=200,LABELWIDTH=140;
	private static final int WINDOWWIDTH=MARGIN*3+TEXTWIDTH+LABELWIDTH+LABELWIDTH;

	private JTextField infield,outfield,imagefield;
	private JTextArea inarea;
	private JButton choosebutton;
	private JButton transferbutton;
	private boolean doimport;
	private int type;

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

	private String[] filelist={};
	private JList fileList;
	private JScrollPane filePane;

	public FileTransferGUI(Computer computer, int type)
	{
		super(computer, type==4? "Compile": (type==3? "Run Program": (type==2? "Delete File":(type==0? "Import File":"Export File"))),WINDOWWIDTH,10+ROWSIZE*12,false,false,false,true);
		this.doimport=(type==0);
		this.type=type;
		refresh();
	}
	public void closeGUI()
	{
		computer.fileTransferGUI=null;
	}
	
	public void constructGUI(GUIComponent guicomponent)
	{
		JLabel l;
		l=new JLabel("Disk Image: ");
		imagefield=new JTextField();
		imagefield.setText(computer.bootgui.bootImageName);
		l.setBounds(MARGIN,10,LABELWIDTH,ROWSIZE);
		imagefield.setBounds(MARGIN+LABELWIDTH,10,TEXTWIDTH,ROWSIZE);
		guicomponent.add(l);
		guicomponent.add(imagefield);

		JButton loadbutton=new JButton("Load image");
		loadbutton.setBounds(MARGIN+LABELWIDTH+TEXTWIDTH+10,10,LABELWIDTH,ROWSIZE);
		loadbutton.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { readImage(); } } );
		guicomponent.add(loadbutton);


		if (type<=1)
			l=new JLabel("Internal filename:");
		else
			l=new JLabel("Filename:");

		infield=new JTextField();
		l.setBounds(MARGIN,10+ROWSIZE*4+(2*ROWSIZE*(doimport? 1:0)),LABELWIDTH,ROWSIZE);
		infield.setBounds(MARGIN+LABELWIDTH,10+ROWSIZE*4+(2*ROWSIZE*(doimport? 1:0)),TEXTWIDTH,ROWSIZE);
		guicomponent.add(l);
		guicomponent.add(infield);

		fileList=new JList(this.filelist);
		fileList.addListSelectionListener(new ListSelectionListener() { 
			public void valueChanged(ListSelectionEvent e) 
			{ 
				infield.setText((String)fileList.getSelectedValue()); 
				if (outfield!=null) 
					outfield.setText(infield.getText());
			}
		});
		filePane=new JScrollPane(fileList);
		filePane.setBounds(MARGIN+LABELWIDTH,10+ROWSIZE*2,TEXTWIDTH,ROWSIZE);
		guicomponent.add(filePane);
		filePane.setVisible(false);

		if (type<=1)
		{
			l=new JLabel("External filename:");
			outfield=new JTextField();
			l.setBounds(MARGIN,10+ROWSIZE*4+(2*ROWSIZE*(doimport? 0:1)),LABELWIDTH,ROWSIZE);
			outfield.setBounds(MARGIN+LABELWIDTH,10+ROWSIZE*4+(2*ROWSIZE*(doimport? 0:1)),TEXTWIDTH,ROWSIZE);
			guicomponent.add(l);
			guicomponent.add(outfield);
			choosebutton=new JButton("Choose");
			choosebutton.setBounds(MARGIN+LABELWIDTH+TEXTWIDTH+10,10+ROWSIZE*4+(2*ROWSIZE*(doimport? 0:1)),LABELWIDTH,ROWSIZE);
			choosebutton.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { chooseFile(); } } );
			guicomponent.add(choosebutton);
		}

		if (type<=1)
		{
			transferbutton=new JButton("Transfer");
			transferbutton.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doTransfer(); } } );
		}
		else if (type==2)
		{
			transferbutton=new JButton("Delete");
			transferbutton.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doDelete(); } } );
		}
		else if (type==3)
		{
			transferbutton=new JButton("Run");
			transferbutton.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doExecute(); } } );
		}
		else if (type==4)
		{
			transferbutton=new JButton("Compile");
			transferbutton.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doCompile(); } } );
		}
		transferbutton.setBounds(WINDOWWIDTH/3-TEXTWIDTH/4,10+ROWSIZE*8,TEXTWIDTH/2,ROWSIZE);
		guicomponent.add(transferbutton);
		JButton cancelbutton=new JButton("Cancel");
		cancelbutton.setBounds(2*WINDOWWIDTH/3-TEXTWIDTH/4,10+ROWSIZE*8,TEXTWIDTH/2,ROWSIZE);
		cancelbutton.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) 
			{
				frame.setVisible(false);
				if (computer.computerGUI.singleFrame)
					computer.computerGUI.removeComponent(computer.fileTransferGUI);
			} } );
		guicomponent.add(cancelbutton);
	}

	public void chooseFile()
	{
		JFileChooser fc = new JFileChooser();
		fc.setCurrentDirectory(new File("."));
		if (doimport) fc.showOpenDialog(null);
		else fc.showSaveDialog(null);
		outfield.setText(fc.getSelectedFile().getAbsolutePath());
		if (doimport) infield.setText(fc.getSelectedFile().getAbsolutePath());
	}

	public byte[] readExternalFile(String name)
	{
		byte[] buffer=null;
		try
		{
			RandomAccessFile f = new RandomAccessFile(name,"r");
			int file_size=(int)f.length();

			buffer=new byte[file_size];

			for(int i=0; i<file_size; i++)
				buffer[i]=(byte)f.read();

			f.close();
		}
		catch(IOException e)
		{
		}
		return buffer;
	}

	public void writeExternalFile(String name, byte[] buffer)
	{
		if (name==null || buffer==null) return;
		try
		{
			FileOutputStream f = new FileOutputStream(name);
			int file_size=buffer.length;

			for(int i=0; i<file_size; i++)
				f.write(buffer[i]&0xff);

			f.close();
		}
		catch(IOException e)
		{
		}
	}

	public byte[] readInternalFile(String fname)
	{
		if (fileSystemType==0)
			return null;

		byte[] directory = readDisk(imagefield.getText(),directoryOffset,directorySectors);

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
			//is this the one?
			String fullname=name+"."+extension;
			if (!fullname.toLowerCase().equals(fname.toLowerCase()))
				continue;
			//get the first sector
			int sector = (directory[i+0x1a]&0xff) | ((directory[i+0x1b]<<8)&0xff00);
			sector = (sector-2)*sectorsPerCluster+fileSystemOffset;

			//get all the sectors
			int[] sectors = findSectorsInFile(sector);

			//get the file size
			int size=0;
			size+=(directory[i+0x1c]&0xff);
			size+=((directory[i+0x1d]&0xff)<<8);
			size+=((directory[i+0x1e]&0xff)<<16);
			size+=((directory[i+0x1f]&0xff)<<24);

			int sizeread=0;
			//read the file
			byte[] thefile = new byte[size];
			for (int k=0; k<sectors.length; k++)
			{
				byte[] buffer=readDisk(imagefield.getText(),sectors[k],1);
				for (int j=0; j<512 && (sizeread++)<size; j++)
					thefile[k*512+j]=buffer[j];
			}
			return thefile;
		}

		return null;
	}

	public void writeInternalFile(String fname, byte[] buffer)
	{
		if (fileSystemType==0)
			return;
		fname=fname.toUpperCase();
		deleteFile(fname);

		byte[] directory = readDisk(imagefield.getText(),directoryOffset,directorySectors);
		byte[] fat = getfat();

		//find a free directory entry
		int i;
		for (i=0; i<directory.length; i+=0x20)
		{
			if (directory[i]==0 || (directory[i]&0xff)==0xe5)
				break;
		}
		if (i==directory.length)
			return;
		//set the name
		int j;
		for (j=0; j<8 && j<fname.length() && fname.charAt(j)!='.'; j++)
			directory[i+j]=(byte)fname.charAt(j);
		for (; j<8; j++)
			directory[i+j]=(byte)0x20;
		//set the extension
		for (j=0; j<fname.length(); j++)
			if (fname.charAt(j)=='.')
				break;
		j++;
		for (int k=0; k<3; k++)
		{
			if (j+k>=fname.length())
				directory[i+8+k]=(byte)0x20;
			else
				directory[i+8+k]=(byte)fname.charAt(j+k);
		}
		//set the file size
		directory[i+0x1c]=(byte)(buffer.length&0xff);
		directory[i+0x1d]=(byte)((buffer.length>>>8)&0xff);
		directory[i+0x1e]=(byte)((buffer.length>>>16)&0xff);
		directory[i+0x1f]=(byte)((buffer.length>>>24)&0xff);

		//find a free fat entry
		int fatentries;
		if (fileSystemType==1)
			fatentries=fat.length*2/3;
		else
			fatentries=fat.length/2;
		for (j=2; j<fatentries; j++)
			if (fatdecode(j,fat)==0)
				break;
		if (j==fatentries)
			return;
		//save it in the directory
		directory[i+0x1a]=(byte)(j&0xff);
		directory[i+0x1b]=(byte)((j>>>8)&0xff);

		//start saving the file
		for (int k=0; k<buffer.length; k+=512*sectorsPerCluster)
		{
			//copy the data over
			byte[] tmp = new byte[512*sectorsPerCluster];
			for (int l=0; l<512*sectorsPerCluster && l+k<buffer.length; l++)
				tmp[l]=buffer[l+k];
			writeDisk(imagefield.getText(),(j-2)*sectorsPerCluster+fileSystemOffset,tmp,sectorsPerCluster);

			//if this is the last round, mark the fat entry EOF
			if (k+512*sectorsPerCluster>=buffer.length)
				fatencode(j,fat,0xffff);
			//otherwise get a new entry
			else
			{
				int m;
				for (m=2; m<fatentries; m++)
					if (m!=j && fatdecode(m,fat)==0)
						break;
				if (m==fatentries)
					return;
				//point last entry to next
				fatencode(j,fat,m);
				j=m;
			}
		} 

		//write back the fat and directory
		writeDisk(imagefield.getText(),fatOffset,fat,sectorsPerFat);
		if (fatTables>1)
			writeDisk(imagefield.getText(),fatOffset+sectorsPerFat,fat,sectorsPerFat);
		writeDisk(imagefield.getText(),directoryOffset,directory,directorySectors);
	}

	public void deleteFile(String fname)
	{
		if (fileSystemType==0)
			return;

		byte[] directory = readDisk(imagefield.getText(),directoryOffset,directorySectors);

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
			//is this the one?
			String fullname=name+"."+extension;
			if (!fullname.toLowerCase().equals(fname.toLowerCase()))
				continue;
			//mark the file as deleted
			directory[i]=(byte)0xe5;
			//get the first sector
			int sector = (directory[i+0x1a]&0xff) | ((directory[i+0x1b]<<8)&0xff00);
			sector = (sector-2)*sectorsPerCluster+fileSystemOffset;

			//get all the sectors
			int[] sectors = findSectorsInFile(sector);

			//zero out all the fat entries
			byte[] fat = getfat();
			for (int k=0; k<sectors.length; k++)
				fatencode(fatentry(sectors[k]),fat,0);

			//write back the fat and directory
			writeDisk(imagefield.getText(),fatOffset,fat,sectorsPerFat);
			if (fatTables>1)
				writeDisk(imagefield.getText(),fatOffset+sectorsPerFat,fat,sectorsPerFat);
			writeDisk(imagefield.getText(),directoryOffset,directory,directorySectors);

			return;
		}
	}

	private byte[] getfat()
	{
		byte[] fat = readDisk(imagefield.getText(),fatOffset,sectorsPerFat);
		return fat;
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

	private int fatentry(int sector)
	{
		return (sector-fileSystemOffset)/sectorsPerCluster+2;
	}

	private void fatencode(int entry, byte[] fat, int newvalue)
	{
		if (fileSystemType==1)
		{
			if (entry%2==1)
			{
				fat[3*(entry-1)/2+1] = (byte)(((newvalue&0xf)<<4) | (fat[3*(entry-1)/2+1]&0x0f));
				fat[3*(entry-1)/2+2] = (byte)((newvalue&0xff0)>>>4);
			}
			else
			{
				fat[3*entry/2] = (byte)(newvalue&0xff);
				fat[3*entry/2+1] = (byte)((fat[3*entry/2+1]&0xf0)|((newvalue>>>8)&0xf));
			}
		}
		else if (fileSystemType==2)
		{
			fat[entry*2]=(byte)(newvalue&0xff);
			fat[entry*2+1]=(byte)((newvalue&0xff00)>>>8);
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

	public void doTransfer()
	{
		decodeDiskParameters();
		if(doimport)
			writeInternalFile(infield.getText(),readExternalFile(outfield.getText()));
		else
			writeExternalFile(outfield.getText(),readInternalFile(infield.getText()));

		frame.setVisible(false);
		if (computer.computerGUI.singleFrame)
			computer.computerGUI.removeComponent(computer.fileTransferGUI);
	}

	public void doDelete()
	{
		decodeDiskParameters();
		deleteFile(infield.getText());

		frame.setVisible(false);
		if (computer.computerGUI.singleFrame)
			computer.computerGUI.removeComponent(computer.fileTransferGUI);
	}

	public void doExecute()
	{
		if (computer.keyboardGUI!=null) computer.keyboardGUI.issueScript(infield.getText());

		frame.setVisible(false);
		if (computer.computerGUI.singleFrame)
			computer.computerGUI.removeComponent(computer.fileTransferGUI);
	}

	public void doCompile()
	{
		String script="";
		if (infield.getText().indexOf(".OBJ")!=-1)
			script="tlink "+infield.getText();
		else if (infield.getText().indexOf(".ASM")==-1)
			script="tcc "+infield.getText();
		else
			script="tasm "+infield.getText();
		if (computer.keyboardGUI!=null) computer.keyboardGUI.issueScript(script);

		frame.setVisible(false);
		if (computer.computerGUI.singleFrame)
			computer.computerGUI.removeComponent(computer.fileTransferGUI);
	}

	public void readImage()
	{
		decodeDiskParameters();

		byte[] directory = readDisk(imagefield.getText(),directoryOffset,directorySectors);
		String[] filelist=new String[10000];
		int filelistnumber=0;

		for (int i=0; i<directory.length; i+=0x20)
		{
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
			if (name=="")
				continue;
			String extension = ""+(char)directory[i+8]+(char)directory[i+9]+(char)directory[i+10];

			if (type==3 && !extension.equals("EXE") && !extension.equals("COM") && !extension.equals("BAT"))
				continue;
			if (type==4 && !extension.equals("ASM") && !extension.equals("C  ") && !extension.equals("OBJ"))
				continue;

			String fullname=name+"."+extension;
			filelist[filelistnumber++]=fullname;
		}
		this.filelist=new String[filelistnumber];
		for (int i=0; i<filelistnumber; i++)
			this.filelist[i]=filelist[i];

		fileList.setListData(filelist);
		filePane.setVisible(true);
	}

	private byte[] readDisk(String imagename, int sectornumber, int quantity)
	{
		byte[] buffer=new byte[512*quantity];
		try
		{
			RandomAccessFile f = new RandomAccessFile(imagename,"r");
			f.seek(sectornumber*512);
			for(int i=0; i<512*quantity; i++)
			{
				buffer[i]=(byte)f.read();
			}
			f.close();
		}
		catch(IOException e)
		{
		}
		return buffer;
	}

	private void writeDisk(String imagename, int sectornumber, byte[] buffer, int quantity)
	{
		if (buffer==null) return;
		try
		{
			RandomAccessFile f;
			f = new RandomAccessFile(imagename,"rw");
			f.seek(sectornumber*512);
			for (int i=0; i<512*quantity; i++)
				f.write(buffer[i]);
			f.close();
		}
		catch(IOException e)
		{
		}
	}

	private boolean isDiskFloppy(String imagename)
	{
		try
		{
			RandomAccessFile f = new RandomAccessFile(imagename,"r");
			if(f.length()<1500000)
			{
				f.close();
				return true;
			}
			f.close();
			return false;
		}
		catch(IOException e)
		{
		}
		return true;
	}

	private void decodeDiskParameters()
	{
		byte[] bootSector=readDisk(imagefield.getText(),0,1);
		if (!isDiskFloppy(imagefield.getText()))
		{
			//read the first partition offset from the MBR
			partitionOffset=(bootSector[0x1c6]&0xff) | ((bootSector[0x1c7]<<8)&0xff00) | ((bootSector[0x1c8]<<16)&0xff0000) | ((bootSector[0x1c9]<<24)&0xff000000);
			bootSector=readDisk(imagefield.getText(),partitionOffset,1);
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
		if (!isDiskFloppy(imagefield.getText()))
			bootOffset=partitionOffset;
		fatOffset=bootOffset+reservedSectorCount;
		directoryOffset=fatOffset+fatTables*sectorsPerFat;
		fileSystemOffset=directoryOffset+directorySectors;

		//find the file system type
		//first see if it tells us directly
		if ((char)bootSector[0x36]=='F' && (char)bootSector[0x37]=='A' && (char)bootSector[0x38]=='T' && (char)bootSector[0x39]=='1' && (char)bootSector[0x3A]=='6')
			fileSystemType=2;
		else if ((char)bootSector[0x36]=='F' && (char)bootSector[0x37]=='A' && (char)bootSector[0x38]=='T' && (char)bootSector[0x39]=='1' && (char)bootSector[0x3A]=='2')
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
	}

}
