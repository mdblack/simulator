package simulator;

import java.awt.*;
import javax.swing.*;

import java.awt.event.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

public class BootGUI extends AbstractGUI
{
	public static final int DISKC_DEFAULT_CYLINDERS=306;
	public static final int DISKC_DEFAULT_HEADS=4;
	public static final int DISKC_DEFAULT_SECTORS=17;
	public static final int DISKD_DEFAULT_CYLINDERS=615;
	public static final int DISKD_DEFAULT_HEADS=6;
	public static final int DISKD_DEFAULT_SECTORS=17;
	public static final int CD_DEFAULT_CYLINDERS=2;
	public static final int CD_DEFAULT_HEADS=16;
	public static final int CD_DEFAULT_SECTORS=63;

	public static final int ROWHEIGHT=20;
	public static final int TEXTWIDTH=150;
	public static final int NTEXTWIDTH=50;
	public static final int BWIDTH=980,BHEIGHT=460;

	String[] deviceName;
	boolean[] deviceIncluded;
	boolean[] deviceGUI;
	boolean[] canInclude,hasGUI;
	boolean[] isCD;

	boolean[] diskIncluded,diskGUI,sectorGUI;
	String[] diskImage;
	String romImage, vromImage, memoryImage, datapathxml, controlxml;
	int memoryImageStart;
	int[] cylinders,heads,sectors;
	String bootImageName;

	JCheckBox[] includeBox;
	JTextField[] diskField;
	JTextField romField,vromField,memoryField,memoryStartField,datapathField,controlField;
	JCheckBox[] cdBox;
	JTextField[] cBox,hBox,sBox;
	JCheckBox singlestepbox, customprocessorbox, memoryimagebox;
	JTextField breakfield;
	JButton getStartedButton;

	boolean bootFromFloppy;
	boolean bootCustomProcessor=false;
	BootGUI bootgui;
	
	public void loadState(String state)
	{
		Scanner loader=new Scanner(state);
		for (int i=0; i<diskIncluded.length; i++)
			diskIncluded[i]=loader.nextInt()==1;
		for (int i=0; i<isCD.length; i++)
			isCD[i]=loader.nextInt()==1;
		for (int i=0; i<diskImage.length; i++)
			diskImage[i]=loader.next();
		for (int i=0; i<cylinders.length; i++)
			cylinders[i]=loader.nextInt();
		for (int i=0; i<heads.length; i++)
			heads[i]=loader.nextInt();
		for (int i=0; i<sectors.length; i++)
			sectors[i]=loader.nextInt();
		bootFromFloppy=loader.nextInt()==1;
		bootImageName=loader.next();
		bootImageName=bootImageName.substring(5,bootImageName.length());
		System.out.println("loaded bootgui");
	}
	
	public String saveState()
	{
		String state="";
		for (int i=0; i<diskIncluded.length; i++)
			state+=(diskIncluded[i]?1:0)+" ";
		for (int i=0; i<isCD.length; i++)
			state+=(isCD[i]?1:0)+" ";
		for (int i=0; i<diskImage.length; i++)
			state+=diskImage[i]+" ";
		for (int i=0; i<cylinders.length; i++)
			state+=cylinders[i]+" ";
		for (int i=0; i<heads.length; i++)
			state+=heads[i]+" ";
		for (int i=0; i<sectors.length; i++)
			state+=sectors[i]+" ";
		state+=(bootFromFloppy?1:0)+" boot:"+bootImageName;

		return state;
	}


	public BootGUI(Computer computer, String[] devices)
	{
		super(computer, "Boot",BWIDTH,BHEIGHT,false,true,false,true);
		bootgui=this;
		
		deviceName=devices;
		diskIncluded=new boolean[4];
		diskImage=new String[4];
		cylinders=new int[4];
		heads=new int[4];
		sectors=new int[4];
		isCD=new boolean[4];
		diskField=new JTextField[4];

		diskIncluded[0]=false;
		diskIncluded[1]=false;
		diskIncluded[2]=false;
		diskIncluded[3]=false;
		romImage="resource/bios.bin";
		vromImage="resource/vgabios.bin";
		memoryImage="";
		datapathxml="";
		controlxml="";
		
		try
		{
			FileReader fr=new FileReader("settings.txt");
			Scanner scan=new Scanner(fr);
			while(true)
			{
				if (!scan.hasNext())
					break;
				String type=scan.next();
				if (type.equals("DiskA"))
				{
					diskIncluded[0]=true;
					diskImage[0]=scan.next();
				}
				if (type.equals("DiskB"))
				{
					diskIncluded[1]=true;
					diskImage[1]=scan.next();
				}
				if (type.equals("DiskC"))
				{
					diskIncluded[2]=true;
					diskImage[2]=scan.next();
					isCD[2]=scan.nextInt()==1;
					cylinders[2]=scan.nextInt();
					heads[2]=scan.nextInt();
					sectors[2]=scan.nextInt();
				}
				if (type.equals("DiskD"))
				{
					diskIncluded[3]=true;
					diskImage[3]=scan.next();
					isCD[3]=scan.nextInt()==1;
					cylinders[3]=scan.nextInt();
					heads[3]=scan.nextInt();
					sectors[3]=scan.nextInt();
				}
				if (type.equals("ROM"))
				{
					romImage=scan.next();
				}
				if (type.equals("VideoROM"))
				{
					vromImage=scan.next();
				}
				if (type.equals("MemoryContents"))
				{
					memoryImage=scan.next();
					memoryImageStart=Integer.parseInt(scan.next(),16);
				}
				if (type.equals("CustomProcessor"))
				{
					try{
					datapathxml=scan.next();
					controlxml=scan.next();
					}catch(java.util.NoSuchElementException e){}
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		computer.computerGUI.menubar.setVisible(false);
		refresh();
	}
	
	public void closeGUI()
	{
		computer.computerGUI.menubar.setVisible(true);
	}

	public void close()
	{
		System.exit(0);
	}
	
	public int width()
	{
		return BWIDTH;
	}

	public int height()
	{
		return BHEIGHT;
	}

	public boolean includeDevice(String name)
	{
		for (int i=0; i<deviceName.length; i++)
		{
			if (deviceName[i].equals(name))
				return deviceIncluded[i];
		}
		return false;
	}

	public void constructGUI(AbstractGUI.GUIComponent guicomponent)
	{
		JLabel label;
		JButton button;
		
		int row=ROWHEIGHT;
		label=new JLabel("Storage Devices:");
		label.setBounds(10,row,200,ROWHEIGHT-2);
		guicomponent.add(label);
		row+=ROWHEIGHT;
		
		getStartedButton=new JButton("Make me some disks");
		getStartedButton.setBounds(10,row,200,ROWHEIGHT-2);
		getStartedButton.addActionListener(new ActionListener(){
					public void actionPerformed(ActionEvent arg0) {
						computer.bootgui.setVisible(false);
						String title="Are you sure?";
						String message="This will create two fresh disk images\n floppya.img and harddiskc.img\n  and save them to your home directory.  \nIf you already have files with those names, they will be overwritten.  \n\nThe operation may take a few minutes.  \n\nPress YES to proceed.";
						int reply = JOptionPane.showConfirmDialog(null, message, title, JOptionPane.YES_NO_OPTION);
						if (reply==JOptionPane.YES_OPTION)
						{
							makefreshdisks();
							JOptionPane.showMessageDialog(null, "Disk images are ready.\nPress BOOT FLOPPY A: to get started, \nand wait until you see an A:> prompt.");
						}
						computer.bootgui.setVisible(true);
					}});
		guicomponent.add(getStartedButton);
		row+=ROWHEIGHT;		

		
		includeBox=new JCheckBox[4];
		cdBox=new JCheckBox[4];
		cBox=new JTextField[4];
		hBox=new JTextField[4];
		sBox=new JTextField[4];
		for (int i=0; i<4; i++)
		{
			int column=10;
			includeBox[i] = new JCheckBox();
			includeBox[i].setSelected(diskIncluded[i]);
			includeBox[i].setBounds(column,row,20,ROWHEIGHT-2);
			column+=30;
			guicomponent.add(includeBox[i]);
			label=new JLabel(new String[]{"Floppy Drive A:","Floppy Drive B:","Hard Drive/CD C:","Hard Drive/CD D:"}[i]);
			label.setBounds(column,row,120,ROWHEIGHT-2);
			guicomponent.add(label);
			column+=140;

			diskField[i]=new JTextField();
			diskField[i].setBounds(column,row,200,ROWHEIGHT-2);
			column+=210;
			diskField[i].setText(diskImage[i]);
			guicomponent.add(diskField[i]);
			
			button=new JButton("Choose");
			button.setBounds(column,row,90,ROWHEIGHT-2);
			final int disknumber=i;
			button.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent arg0) {
					choosedisk(disknumber);
				}});
			guicomponent.add(button);
			column+=95;
			button=new JButton("Create");
			button.setBounds(column,row,85,ROWHEIGHT-2);
			final int disknumber2=i;
			button.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent arg0) {
					editdisk(disknumber2);
				}});
			guicomponent.add(button);
			column+=95;
			
			if (i>=2)
			{
				label=new JLabel("CD?");
				label.setBounds(column,row,30,ROWHEIGHT-2);
				guicomponent.add(label);
				column+=40;
				
				cdBox[i]=new JCheckBox();
				cdBox[i].setBounds(column,row,20,ROWHEIGHT-2);
				cdBox[i].setSelected(isCD[i]);
				guicomponent.add(cdBox[i]);
				column+=30;
				
				label=new JLabel("Cylinders");
				label.setBounds(column,row,75,ROWHEIGHT-2);
				guicomponent.add(label);
				column+=80;
				cBox[i]=new JTextField();
				cBox[i].setBounds(column,row,30,ROWHEIGHT-2);
				cBox[i].setText(""+cylinders[i]);
				guicomponent.add(cBox[i]);
				column+=40;
				
				label=new JLabel("Heads");
				label.setBounds(column,row,50,ROWHEIGHT-2);
				guicomponent.add(label);
				column+=55;
				hBox[i]=new JTextField();
				hBox[i].setBounds(column,row,30,ROWHEIGHT-2);
				hBox[i].setText(""+heads[i]);
				guicomponent.add(hBox[i]);
				column+=40;
				
				label=new JLabel("Sectors");
				label.setBounds(column,row,60,ROWHEIGHT-2);
				guicomponent.add(label);
				column+=65;
				sBox[i]=new JTextField();
				sBox[i].setBounds(column,row,30,ROWHEIGHT-2);
				sBox[i].setText(""+sectors[i]);
				guicomponent.add(sBox[i]);
				column+=40;
			}
			
			row+=ROWHEIGHT;
		}
		row+=ROWHEIGHT;
		
		label=new JLabel("Memory Images:");
		label.setBounds(10,row,200,ROWHEIGHT-2);
		guicomponent.add(label);
		row+=ROWHEIGHT;
		
		label=new JLabel("ROM BIOS");
		label.setBounds(10,row,150,ROWHEIGHT-2);
		guicomponent.add(label);
		romField=new JTextField();
		romField.setText(romImage);
		romField.setBounds(160,row,200,ROWHEIGHT-2);
		guicomponent.add(romField);
		row+=ROWHEIGHT;
		label=new JLabel("Video ROM BIOS");
		label.setBounds(10,row,150,ROWHEIGHT-2);
		guicomponent.add(label);
		vromField=new JTextField();
		vromField.setText(vromImage);
		vromField.setBounds(160,row,200,ROWHEIGHT-2);
		guicomponent.add(vromField);
		row+=ROWHEIGHT;
		memoryimagebox=new JCheckBox();
		memoryimagebox.setBounds(10,row,20,ROWHEIGHT-2);
		memoryimagebox.setSelected(!memoryImage.equals(""));
		guicomponent.add(memoryimagebox);
		label=new JLabel("Load memory image");
		label.setBounds(30,row,180,ROWHEIGHT-2);
		guicomponent.add(label);
		memoryField=new JTextField();
		memoryField.setText(memoryImage);
		memoryField.setBounds(190,row,200,ROWHEIGHT-2);
		guicomponent.add(memoryField);
		button=new JButton("Choose");
		button.setBounds(400,row,100,ROWHEIGHT-2);
		button.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent arg0) {
				editmemory();
			}});
		guicomponent.add(button);
		label=new JLabel("Address");
		label.setBounds(510,row,60,ROWHEIGHT-2);
		guicomponent.add(label);
		memoryStartField=new JTextField();
		memoryStartField.setText(Integer.toHexString(memoryImageStart));
		memoryStartField.setBounds(580,row,100,ROWHEIGHT-2);
		guicomponent.add(memoryStartField);
		row+=ROWHEIGHT;
		
		row+=ROWHEIGHT;
		
		label=new JLabel("Custom Processor:");
		label.setBounds(10,row,150,ROWHEIGHT-2);
		guicomponent.add(label);
		customprocessorbox = new JCheckBox();
		customprocessorbox.setBounds(160,row,20,ROWHEIGHT-2);
		customprocessorbox.setSelected(!datapathxml.equals("") && !controlxml.equals(""));
		guicomponent.add(customprocessorbox);
		row+=ROWHEIGHT;
		label=new JLabel("Datapath:");
		label.setBounds(10,row,100,ROWHEIGHT-2);
		guicomponent.add(label);
		datapathField=new JTextField();
		datapathField.setText(datapathxml);
		datapathField.setBounds(120,row,200,ROWHEIGHT-2);
		guicomponent.add(datapathField);
		button=new JButton("Choose");
		button.setBounds(330,row,100,ROWHEIGHT-2);
		button.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent arg0) {
				editdatapath();
			}});
		guicomponent.add(button);
		row+=ROWHEIGHT;
		label=new JLabel("Control:");
		label.setBounds(10,row,100,ROWHEIGHT-2);
		guicomponent.add(label);
		controlField=new JTextField();
		controlField.setText(controlxml);
		controlField.setBounds(120,row,200,ROWHEIGHT-2);
		guicomponent.add(controlField);
		button=new JButton("Choose");
		button.setBounds(330,row,100,ROWHEIGHT-2);
		button.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent arg0) {
				editcontrol();
			}});
		guicomponent.add(button);
		row+=ROWHEIGHT;
		row+=ROWHEIGHT;

		label = new JLabel("Start in single step mode? ");
		label.setBounds(10,row,200,ROWHEIGHT-2);		
		guicomponent.add(label);
		singlestepbox = new JCheckBox();
		singlestepbox.setBounds(220,row,50,ROWHEIGHT-2);
		guicomponent.add(singlestepbox);
		row+=ROWHEIGHT;
		row+=ROWHEIGHT;
		row+=ROWHEIGHT;

		JButton boota = new JButton("Boot Floppy A:");
		boota.setBounds(10,row,BWIDTH/5-30,ROWHEIGHT);
		boota.addActionListener(new ButtonListener());
		guicomponent.add(boota);
		JButton bootc = new JButton("Boot Disk C:");
		bootc.setBounds(BWIDTH/5+10,row,BWIDTH/5-30,ROWHEIGHT);
		bootc.addActionListener(new ButtonListener());
		guicomponent.add(bootc);
		JButton bootb = new JButton("Boot No Disk");
		bootb.setBounds(2*BWIDTH/5+10,row,BWIDTH/5-30,ROWHEIGHT);
		bootb.addActionListener(new ButtonListener());
		guicomponent.add(bootb);
		JButton bootcp = new JButton("Processor Design");
		bootcp.setBounds(3*BWIDTH/5+10,row,BWIDTH/5-30,ROWHEIGHT);
		bootcp.addActionListener(new ButtonListener());
		guicomponent.add(bootcp);
		JButton cancel = new JButton("Quit");
		cancel.setBounds(4*BWIDTH/5+20,row,BWIDTH/5-30,ROWHEIGHT);
		cancel.addActionListener(new ButtonListener());
		guicomponent.add(cancel);

		if (computer.applet==null) setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
	}

	private int diskediting=-1;
	private void editdisk(int number)
	{
		new MakeDiskGUI(computer);
		diskediting=number;
	}
	private void makefreshdisks()
	{
		MakeDiskGUI make=new MakeDiskGUI(computer,false);
		make.setVisible(false);
		diskediting=0;
		make.constructImage("template_floppy_freedos.img", "floppya.img");
		diskediting=2;
		make.constructImage("template_306_4_17_programming.img", "harddiskc.img");
		includeBox[3].setSelected(false);		
		includeBox[1].setSelected(false);
	}
	public void editdisk(String name, int c, int h, int s)
	{
		if (diskediting==-1) return;
		diskField[diskediting].setText(name);
		if (diskediting>=2)
		{
			cylinders[diskediting]=c;
			cBox[diskediting].setText(""+c);
			heads[diskediting]=h;
			hBox[diskediting].setText(""+h);
			sectors[diskediting]=s;
			sBox[diskediting].setText(""+s);
			cdBox[diskediting].setSelected(false);
		}
		includeBox[diskediting].setSelected(true);
	}
	
	private void choosedisk(int i)
	{
		JFileChooser fc = new JFileChooser();
		fc.setCurrentDirectory(new File("."));
		fc.showOpenDialog(null);
		File f=fc.getSelectedFile();
		if (f==null) 
		{
			diskField[i].setText(""); 
			includeBox[i].setSelected(false);
		}
		else 
		{
			diskField[i].setText(f.getAbsolutePath());
			includeBox[i].setSelected(true);
		}
	}
	private void editdatapath()
	{
		JFileChooser fc = new JFileChooser();
		fc.setCurrentDirectory(new File("."));
		fc.showOpenDialog(null);
		File f=fc.getSelectedFile();
		if (f==null) datapathField.setText("");
		else datapathField.setText(f.getAbsolutePath());		
	}
	
	private void editcontrol()
	{
		JFileChooser fc = new JFileChooser();
		fc.setCurrentDirectory(new File("."));
		fc.showOpenDialog(null);
		File f=fc.getSelectedFile();
		if (f==null) controlField.setText("");
		else controlField.setText(f.getAbsolutePath());				
	}
	
	private void editmemory()
	{
		JFileChooser fc = new JFileChooser();
		fc.setCurrentDirectory(new File("."));
		fc.showOpenDialog(null);
		File f=fc.getSelectedFile();
		if (f==null) memoryField.setText("");
		else memoryField.setText(f.getAbsolutePath());				
	}
	
	public void doPaint(Graphics g)
	{
		for (int i=0; i<20; i++)
		{
			if (i%2==0) g.setColor(new Color(200,200,200));
			else g.setColor(new Color(255,255,255));
			g.fillRect(0,ROWHEIGHT+i*ROWHEIGHT,BWIDTH,ROWHEIGHT);
		}
	}

	public void updateCheckBoxes()
	{
		if (includeBox[0].isSelected())
		{
			diskIncluded[0]=true;
			diskImage[0]=diskField[0].getText();
		}
		if (includeBox[1].isSelected())
		{
			diskIncluded[1]=true;
			diskImage[1]=diskField[1].getText();
		}
		if (includeBox[2].isSelected())
		{
			diskIncluded[2]=true;
			diskImage[2]=diskField[2].getText();
			isCD[2]=cdBox[2].isSelected();
			if (isCD[2])
			{
				cylinders[2]=CD_DEFAULT_CYLINDERS;
				heads[2]=CD_DEFAULT_HEADS;
				sectors[2]=CD_DEFAULT_SECTORS;
			}
			else
			{
				cylinders[2]=Integer.parseInt(cBox[2].getText());
				heads[2]=Integer.parseInt(hBox[2].getText());
				sectors[2]=Integer.parseInt(sBox[2].getText());
			}
		}
		if (includeBox[3].isSelected())
		{
			diskIncluded[3]=true;
			diskImage[3]=diskField[3].getText();
			isCD[3]=cdBox[3].isSelected();
			if (isCD[3])
			{
				cylinders[3]=CD_DEFAULT_CYLINDERS;
				heads[3]=CD_DEFAULT_HEADS;
				sectors[3]=CD_DEFAULT_SECTORS;
			}
			else
			{
				cylinders[3]=Integer.parseInt(cBox[3].getText());
				heads[3]=Integer.parseInt(hBox[3].getText());
				sectors[3]=Integer.parseInt(sBox[3].getText());
			}
		}
		romImage=romField.getText();
		vromImage=vromField.getText();
		if (memoryimagebox.isSelected())
		{
			memoryImage=memoryField.getText();
			memoryImageStart=Integer.parseInt(memoryStartField.getText(),16);
		}
		else
		{
			memoryImage="";
		}
		datapathxml=datapathField.getText();
		controlxml=controlField.getText();

		try
		{
			PrintWriter pw=new PrintWriter("settings.txt");
			if (diskIncluded[0])
				pw.println("DiskA "+diskImage[0]);
			if (diskIncluded[1])
				pw.println("DiskB "+diskImage[1]);
			if (diskIncluded[2])
				pw.println("DiskC "+diskImage[2]+" "+(isCD[2]?1:0)+" "+cylinders[2]+" "+heads[2]+" "+sectors[2]);
			if (diskIncluded[3])
				pw.println("DiskD "+diskImage[3]+" "+(isCD[3]?1:0)+" "+cylinders[3]+" "+heads[3]+" "+sectors[3]);
			pw.println("ROM "+romImage);
			pw.println("VideoROM "+vromImage);
			if (!memoryImage.equals(""))
				pw.println("MemoryContents "+memoryImage+" "+Integer.toHexString(memoryImageStart));
			if (customprocessorbox.isSelected())
				pw.println("CustomProcessor "+datapathxml+" "+controlxml);
			pw.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		if (singlestepbox.isSelected())
		{
			computer.debugMode=true;
			computer.computerGUI.stepButton.setEnabled(true);
			computer.computerGUI.playButton.setEnabled(true);
			computer.computerGUI.fastPlayButton.setEnabled(true);
			computer.computerGUI.pauseButton.setEnabled(false);
		}
		else
		{
			computer.debugMode=false;
			computer.computerGUI.stepButton.setEnabled(false);
			computer.computerGUI.playButton.setEnabled(false);
			computer.computerGUI.fastPlayButton.setEnabled(false);
			computer.computerGUI.pauseButton.setEnabled(true);
		}
	}

	private class ButtonListener implements ActionListener
	{
		public void actionPerformed(ActionEvent e)
		{
			if (e.getActionCommand().equals("Boot Floppy A:"))
			{
				bootFromFloppy=true;
				bootImageName=diskField[0].getText();
				setVisible(false);
				computer.computerGUI.menubar.setVisible(true);
				computer.computerGUI.removeComponent(bootgui);
				updateCheckBoxes();
				computer.stepLock.lockResume();
			}
			else if (e.getActionCommand().equals("Boot Disk C:"))
			{
				bootFromFloppy=false;
				bootImageName=diskField[2].getText();
				setVisible(false);
				computer.computerGUI.menubar.setVisible(true);
				computer.computerGUI.removeComponent(bootgui);
				updateCheckBoxes();

				computer.stepLock.lockResume();
			}
			else if (e.getActionCommand().equals("Boot No Disk"))
			{
				bootFromFloppy=false;
				bootImageName="";
				setVisible(false);
				computer.computerGUI.menubar.setVisible(true);
				computer.computerGUI.removeComponent(bootgui);
				singlestepbox.setSelected(true);

				updateCheckBoxes();
				computer.stepLock.lockResume();
			}
			else if (e.getActionCommand().equals("Processor Design"))
			{
				bootFromFloppy=false;
				bootImageName="";
				setVisible(false);
				computer.computerGUI.menubar.setVisible(true);
				computer.computerGUI.removeComponent(bootgui);
				singlestepbox.setSelected(true);

				updateCheckBoxes();
				bootCustomProcessor=true;
				computer.stepLock.lockResume();
			}
			else if (e.getActionCommand().equals("Quit"))
			{
				System.exit(0);
			}
		}
	}
}
