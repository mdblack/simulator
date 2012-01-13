package simulator;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.util.Scanner;

public class BootGUI extends AbstractGUI
{
	public static final String DISKA_DEFAULT_NAME="floppya.img";
	public static final String DISKB_DEFAULT_NAME="floppyb.img";
//	public static final String DISKC_DEFAULT_NAME="programming.img";
	public static final String DISKC_DEFAULT_NAME="l.iso";
	public static final String DISKD_DEFAULT_NAME="hd.img";
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
	public static final int BWIDTH=720,BHEIGHT=520;

	String[] deviceName;
	boolean[] deviceIncluded;
	boolean[] deviceGUI;
	boolean[] canInclude,hasGUI;
	boolean[] isCD;

	boolean[] diskIncluded,diskGUI,sectorGUI;
	String[] diskImage;
	int[] cylinders,heads,sectors;
	String bootImageName;

	JCheckBox[] includeBox,guiincludeBox;
	JTextField[] diskField;
	JTextField[] cField,hField,sField;
	JCheckBox[] includeDriveBox, diskguiincludeBox, sectorguiincludeBox, cdBox;
	JCheckBox singlestepbox;
	JTextField breakfield;

	boolean bootFromFloppy;
	BootGUI bootgui;
	
	public void loadState(String state)
	{
		Scanner loader=new Scanner(state);
		for (int i=0; i<deviceIncluded.length; i++)
			deviceIncluded[i]=loader.nextInt()==1;
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
		for (int i=0; i<deviceIncluded.length; i++)
			state+=(deviceIncluded[i]?1:0)+" ";
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


	public BootGUI(Computer computer, String[] devices, boolean[] canInclude, boolean[] hasGUI)
	{
		super(computer, "Boot",BWIDTH,BHEIGHT,false,true,false,true);
		bootgui=this;

		deviceName=devices;
		deviceIncluded=new boolean[devices.length];
		deviceGUI=new boolean[devices.length];
		diskIncluded=new boolean[4];
		diskGUI=new boolean[4];
		sectorGUI=new boolean[4];
		diskImage=new String[4];
		cylinders=new int[4];
		heads=new int[4];
		sectors=new int[4];
		isCD=new boolean[4];
		this.canInclude=canInclude;
		this.hasGUI=hasGUI;
		refresh();
	}
	
	public void closeGUI()
	{
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

	public boolean showGUI(String name)
	{
		for (int i=0; i<deviceName.length; i++)
		{
			if (deviceName[i].equals(name))
				return deviceGUI[i];
		}
		return false;
	}

	public void constructGUI(AbstractGUI.GUIComponent guicomponent)
	{
		includeBox=new JCheckBox[deviceName.length];
		guiincludeBox=new JCheckBox[deviceName.length];
		for (int i=0; i<deviceName.length; i++)
		{
			if (canInclude[i])
			{
				includeBox[i] = new JCheckBox();
				includeBox[i].setSelected(true);
				includeBox[i].setBounds(10,ROWHEIGHT+ROWHEIGHT*i+1,ROWHEIGHT-2,ROWHEIGHT-2);
				guicomponent.add(includeBox[i]);
			}

			if (hasGUI[i])
			{
				guiincludeBox[i] = new JCheckBox();
				guiincludeBox[i].setBounds(10+ROWHEIGHT+10+TEXTWIDTH+10,ROWHEIGHT+ROWHEIGHT*i+1,ROWHEIGHT-2,ROWHEIGHT-2);
				guicomponent.add(guiincludeBox[i]);

//				if (deviceName[i].equals("Keyboard") || computer.computerGUI.singleFrame)
				if (deviceName[i].equals("Keyboard"))
					guiincludeBox[i].setSelected(true);
				if (deviceName[i].equals("Video"))
					guiincludeBox[i].setSelected(true);
			}

			JLabel name = new JLabel(deviceName[i]);
			name.setBounds(10+ROWHEIGHT+10,ROWHEIGHT+ROWHEIGHT*i+1,TEXTWIDTH,ROWHEIGHT-2);			
			guicomponent.add(name);
		}
		diskField = new JTextField[4];
		includeDriveBox = new JCheckBox[4];
		diskguiincludeBox = new JCheckBox[4];
		sectorguiincludeBox = new JCheckBox[4];
		cdBox=new JCheckBox[2];
		cField=new JTextField[4];
		hField=new JTextField[4];
		sField=new JTextField[4];

		for (int i=0; i<4; i++)
		{
			includeDriveBox[i]=new JCheckBox();
			includeDriveBox[i].setSelected(true);
			includeDriveBox[i].setBounds(10,ROWHEIGHT*(deviceName.length+2+i)+1,ROWHEIGHT-2,ROWHEIGHT-2);
			guicomponent.add(includeDriveBox[i]);

			diskguiincludeBox[i]=new JCheckBox();
			diskguiincludeBox[i].setBounds(10+ROWHEIGHT+10+(TEXTWIDTH+10)*2+NTEXTWIDTH*3+30,ROWHEIGHT*(deviceName.length+2+i)+1,ROWHEIGHT-2,ROWHEIGHT-2);
			guicomponent.add(diskguiincludeBox[i]);

			sectorguiincludeBox[i]=new JCheckBox();
			sectorguiincludeBox[i].setBounds(10+ROWHEIGHT+10+(TEXTWIDTH+10)*2+NTEXTWIDTH*3+30+ROWHEIGHT+10,ROWHEIGHT*(deviceName.length+2+i)+1,ROWHEIGHT-2,ROWHEIGHT-2);
			guicomponent.add(sectorguiincludeBox[i]);
			
			if (i>=2)
			{
				cdBox[i-2]=new JCheckBox();
				cdBox[i-2].setBounds(10+TEXTWIDTH,ROWHEIGHT*(deviceName.length+2+i)+1,ROWHEIGHT-2,ROWHEIGHT-2);
				guicomponent.add(cdBox[i-2]);
				if (i==2)
					cdBox[i-2].setSelected(true);
			}

			JLabel name = new JLabel();
			if (i==0) name.setText("Floppy A:");
			else if (i==1) name.setText("Floppy B:");
			else if (i==2) name.setText("Hard Disk C:");
			else if (i==3) name.setText("Hard Disk D:");
			name.setBounds(10+ROWHEIGHT+10,ROWHEIGHT*(deviceName.length+2+i)+1,TEXTWIDTH,ROWHEIGHT-2);
			guicomponent.add(name);

			diskField[i]=new JTextField();
			diskField[i].setBounds(10+ROWHEIGHT+10+TEXTWIDTH+10,ROWHEIGHT*(deviceName.length+2+i)+1,TEXTWIDTH,ROWHEIGHT-2);
			if (i==0) diskField[i].setText(DISKA_DEFAULT_NAME);
			else if (i==1) diskField[i].setText(DISKB_DEFAULT_NAME);
			else if (i==2) diskField[i].setText(DISKC_DEFAULT_NAME);
			else if (i==3) diskField[i].setText(DISKD_DEFAULT_NAME);
			guicomponent.add(diskField[i]);

			cField[i]=new JTextField();
			cField[i].setBounds(10+ROWHEIGHT+10+(TEXTWIDTH+10)*2,ROWHEIGHT*(deviceName.length+2+i)+1,NTEXTWIDTH,ROWHEIGHT-2);
			if (i==0 || i==1) cField[i].setText("0");
			if (i==2) cField[i].setText(""+DISKC_DEFAULT_CYLINDERS);
			if (i==3) cField[i].setText(""+DISKD_DEFAULT_CYLINDERS);
			if (i>=2) guicomponent.add(cField[i]);
			hField[i]=new JTextField();
			hField[i].setBounds(10+ROWHEIGHT+10+(TEXTWIDTH+10)*2+NTEXTWIDTH+10,ROWHEIGHT*(deviceName.length+2+i)+1,NTEXTWIDTH,ROWHEIGHT-2);
			if (i==0 || i==1) hField[i].setText("0");
			if (i==2) hField[i].setText(""+DISKC_DEFAULT_HEADS);
			if (i==3) hField[i].setText(""+DISKD_DEFAULT_HEADS);
			if (i>=2) guicomponent.add(hField[i]);
			sField[i]=new JTextField();
			sField[i].setBounds(10+ROWHEIGHT+10+(TEXTWIDTH+10)*2+NTEXTWIDTH*2+20,ROWHEIGHT*(deviceName.length+2+i)+1,NTEXTWIDTH,ROWHEIGHT-2);
			if (i==0 || i==1) sField[i].setText("0");
			if (i==2) sField[i].setText(""+DISKC_DEFAULT_SECTORS);
			if (i==3) sField[i].setText(""+DISKD_DEFAULT_SECTORS);
			if (i>=2) guicomponent.add(sField[i]);
		}

		JLabel breakp = new JLabel("Initial breakpoint equation: ");
		breakp.setBounds(10,ROWHEIGHT*(deviceName.length+2+4+1),TEXTWIDTH+60,ROWHEIGHT-2);
		guicomponent.add(breakp);
		breakfield=new JTextField();
		breakfield.setBounds(10+TEXTWIDTH+60+10,ROWHEIGHT*(deviceName.length+2+4+1),TEXTWIDTH,ROWHEIGHT-2);
		guicomponent.add(breakfield);

		JLabel name = new JLabel("Start in single step mode? ");
		name.setBounds(10,ROWHEIGHT*(deviceName.length+2+4+1+1),TEXTWIDTH+60,ROWHEIGHT-2);		
		guicomponent.add(name);
		singlestepbox = new JCheckBox();
		singlestepbox.setBounds(10+TEXTWIDTH+60+10,ROWHEIGHT*(deviceName.length+2+4+1+1),ROWHEIGHT-2,ROWHEIGHT-2);
		guicomponent.add(singlestepbox);

		JButton boota = new JButton("Boot Floppy A:");
		boota.setBounds(10,ROWHEIGHT*(deviceName.length+2+4+1+1+1),BWIDTH/4-30,ROWHEIGHT);
		boota.addActionListener(new ButtonListener());
		guicomponent.add(boota);
		JButton bootc = new JButton("Boot Disk C:");
		bootc.setBounds(BWIDTH/4+10,ROWHEIGHT*(deviceName.length+2+4+1+1+1),BWIDTH/4-30,ROWHEIGHT);
		bootc.addActionListener(new ButtonListener());
		guicomponent.add(bootc);
		JButton bootb = new JButton("Boot No Disk");
		bootb.setBounds(2*BWIDTH/4+10,ROWHEIGHT*(deviceName.length+2+4+1+1+1),BWIDTH/4-30,ROWHEIGHT);
		bootb.addActionListener(new ButtonListener());
		guicomponent.add(bootb);
		JButton cancel = new JButton("Quit");
		cancel.setBounds(3*BWIDTH/4+20,ROWHEIGHT*(deviceName.length+2+4+1+1+1),BWIDTH/4-30,ROWHEIGHT);
		cancel.addActionListener(new ButtonListener());
		guicomponent.add(cancel);

		if (computer.applet==null) frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		//applet specific settings
		if (computer.applet!=null)
		{
			singlestepbox.setSelected(true);
			includeDriveBox[0].setSelected(true);
			includeDriveBox[1].setSelected(false);
			includeDriveBox[2].setSelected(false);
			includeDriveBox[3].setSelected(false);
		}
	}

	public void doPaint(Graphics g)
	{
		for (int i=0; i<deviceName.length; i++)
		{
			if (i%2==0) g.setColor(new Color(200,200,200));
			else g.setColor(new Color(255,255,255));
			g.fillRect(0,ROWHEIGHT+i*ROWHEIGHT,BWIDTH,ROWHEIGHT);
		}
		g.setColor(Color.BLACK);
		g.drawString("Include device",10,ROWHEIGHT-4);
		g.drawString("Show GUI",10+ROWHEIGHT+10+TEXTWIDTH+10,ROWHEIGHT-4);
		g.drawString("Include drive",10,ROWHEIGHT*(deviceName.length+2)+1-4);
		g.drawString("CD?",10+TEXTWIDTH,ROWHEIGHT*(deviceName.length+2)+1-4);
		g.drawString("Cylinders",10+ROWHEIGHT+10+(TEXTWIDTH+10)*2,ROWHEIGHT*(deviceName.length+2)+1-4);
		g.drawString("Heads",10+ROWHEIGHT+10+(TEXTWIDTH+10)*2+NTEXTWIDTH*1+10,ROWHEIGHT*(deviceName.length+2)+1-4);
		g.drawString("Sectors",10+ROWHEIGHT+10+(TEXTWIDTH+10)*2+NTEXTWIDTH*2+20,ROWHEIGHT*(deviceName.length+2)+1-4);
		g.drawString("GUI",10+ROWHEIGHT+10+(TEXTWIDTH+10)*2+NTEXTWIDTH*3+30,ROWHEIGHT*(deviceName.length+2)+1-4);
		g.drawString("Sector Block GUI",10+ROWHEIGHT+10+(TEXTWIDTH+10)*2+NTEXTWIDTH*3+30+ROWHEIGHT+10,ROWHEIGHT*(deviceName.length+2)+1-4);
	}

	public void updateCheckBoxes()
	{
		for (int i=0; i<deviceName.length; i++)
		{
			if (includeBox[i]!=null) deviceIncluded[i]=includeBox[i].isSelected();
			if (guiincludeBox[i]!=null) deviceGUI[i]=guiincludeBox[i].isSelected();
		}
		isCD[2]=cdBox[0].isSelected();
		isCD[3]=cdBox[1].isSelected();
		for (int i=0; i<4; i++)
		{
			diskIncluded[i]=includeDriveBox[i].isSelected();
			diskGUI[i]=diskguiincludeBox[i].isSelected();
			sectorGUI[i]=sectorguiincludeBox[i].isSelected();
			diskImage[i]=diskField[i].getText();
			cylinders[i]=Integer.parseInt(cField[i].getText());
			heads[i]=Integer.parseInt(hField[i].getText());
			sectors[i]=Integer.parseInt(sField[i].getText());
			
			if (i>2 && isCD[i])
			{
				cylinders[i]=CD_DEFAULT_CYLINDERS;
				heads[i]=CD_DEFAULT_HEADS;
				sectors[i]=CD_DEFAULT_SECTORS;
			}
		}
		if (!breakfield.getText().equals(""))
			computer.breakpointGUI=new BreakpointGUI(computer, breakfield.getText());
		
				
		if (singlestepbox.isSelected())
		{
			computer.debugMode=true;
			computer.controlGUI.stepButton.setEnabled(true);
			computer.controlGUI.playButton.setEnabled(true);
			computer.controlGUI.fastPlayButton.setEnabled(true);
			computer.controlGUI.pauseButton.setEnabled(false);
		}
		else
		{
			computer.debugMode=false;
			computer.controlGUI.stepButton.setEnabled(false);
			computer.controlGUI.playButton.setEnabled(false);
			computer.controlGUI.fastPlayButton.setEnabled(false);
			computer.controlGUI.pauseButton.setEnabled(true);
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
				frame.setVisible(false);
				if (computer.computerGUI.singleFrame)
					computer.computerGUI.removeComponent(bootgui);
				updateCheckBoxes();
				computer.stepLock.lockResume();
			}
			else if (e.getActionCommand().equals("Boot Disk C:"))
			{
				bootFromFloppy=false;
				bootImageName=diskField[2].getText();
				frame.setVisible(false);
				if (computer.computerGUI.singleFrame)
					computer.computerGUI.removeComponent(bootgui);
				updateCheckBoxes();

				computer.stepLock.lockResume();
			}
			else if (e.getActionCommand().equals("Boot No Disk"))
			{
				bootFromFloppy=false;
				bootImageName="";
				frame.setVisible(false);
				if (computer.computerGUI.singleFrame)
					computer.computerGUI.removeComponent(bootgui);
				singlestepbox.setSelected(true);
//				for (int i=0; i<deviceName.length; i++)
//					if (deviceName[i].equals("Keyboard"))
//						guiincludeBox[i].setSelected(false);

				updateCheckBoxes();
				computer.stepLock.lockResume();
			}
			else if (e.getActionCommand().equals("Quit"))
			{
				System.exit(0);
			}
		}
	}
}
