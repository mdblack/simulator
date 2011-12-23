package simulator;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.io.*;

public class ControlGUI extends AbstractGUI
{
	public JButton playButton,pauseButton,stepButton,fastPlayButton;
	private static final int BWIDTH=100,CWIDTH=450,CHEIGHT=150;
	private static final int INSTRUCTION_COUNT_UPDATE=50000;
	private JMenuBar menubar;
	public ControlGUI(Computer computer)
	{
		super(computer,"Simulator Control",CWIDTH,CHEIGHT,true,false,false,false);
		playButton=new JButton("Play");
		fastPlayButton=new JButton("Fast Play");
		pauseButton=new JButton("Pause");
		stepButton=new JButton("Step");
		playButton.setBounds(10,50,(CWIDTH-40)/4,30);
		fastPlayButton.setBounds(10+(CWIDTH-40)/4+10,50,(CWIDTH-40)/4,30);
		pauseButton.setBounds(10+2*(CWIDTH-40)/4+10+10,50,(CWIDTH-40)/4,30);
		stepButton.setBounds(10+3*(CWIDTH-40)/4+10+10+10,50,(CWIDTH-40)/4,30);
		playButton.addActionListener(new ControlButtonListener());
		fastPlayButton.addActionListener(new ControlButtonListener());
		pauseButton.addActionListener(new ControlButtonListener());
		stepButton.addActionListener(new ControlButtonListener());

		menubar=new JMenuBar();
		JMenu menuControl = new JMenu("Control");
		JMenu menuGUI = new JMenu("GUI");
		JMenu menuDisk = new JMenu("Disk");
		JMenu menuConstruct = new JMenu("Construct");

		JMenuItem datapath = new JMenuItem("Datapath");
		datapath.addActionListener(new MenuListener());
		menuConstruct.add(datapath);
		JMenuItem control = new JMenuItem("Control");
		control.addActionListener(new MenuListener());
		menuConstruct.add(control);
		JMenuItem customprocessor = new JMenuItem("Custom Processor");
		customprocessor.addActionListener(new MenuListener());
		menuConstruct.add(customprocessor);

		JMenuItem breakpoint = new JMenuItem("Set Breakpoints");
		JMenuItem reboot = new JMenuItem("Reboot");
		JMenuItem mt = new JMenuItem("Memory Transfer");
		JMenuItem exit = new JMenuItem("Exit");
		breakpoint.addActionListener(new MenuListener());
		reboot.addActionListener(new MenuListener());
		exit.addActionListener(new MenuListener());
		mt.addActionListener(new MenuListener());
		menuControl.add(breakpoint);
		menuControl.add(reboot);
		menuControl.add(mt);
		menuControl.add(exit);

		JMenuItem item;
		item = new JMenuItem("Processor");
		item.addActionListener(new MenuListener());
		menuGUI.add(item);
		item = new JMenuItem("Registers");
		item.addActionListener(new MenuListener());
		menuGUI.add(item);
		item = new JMenuItem("Memory");
		item.addActionListener(new MenuListener());
		menuGUI.add(item);
		item = new JMenuItem("Instruction Memory");
		item.addActionListener(new MenuListener());
		menuGUI.add(item);
		item = new JMenuItem("Stack");
		item.addActionListener(new MenuListener());
		menuGUI.add(item);
		item = new JMenuItem("I/O Ports");
		item.addActionListener(new MenuListener());
		menuGUI.add(item);
		item = new JMenuItem("Timer");
		item.addActionListener(new MenuListener());
		menuGUI.add(item);
		item = new JMenuItem("Serial Port");
		item.addActionListener(new MenuListener());
		menuGUI.add(item);
		item = new JMenuItem("Keyboard");
		item.addActionListener(new MenuListener());
		menuGUI.add(item);
		item = new JMenuItem("Video");
		item.addActionListener(new MenuListener());
		menuGUI.add(item);
		item = new JMenuItem("Disk A:");
		item.addActionListener(new MenuListener());
		menuGUI.add(item);
		item = new JMenuItem("Disk B:");
		item.addActionListener(new MenuListener());
		menuGUI.add(item);
		item = new JMenuItem("Disk C:");
		item.addActionListener(new MenuListener());
		menuGUI.add(item);
		item = new JMenuItem("Disk D:");
		item.addActionListener(new MenuListener());
		menuGUI.add(item);
		item = new JMenuItem("Sectors A:");
		item.addActionListener(new MenuListener());
		menuGUI.add(item);
		item = new JMenuItem("Sectors B:");
		item.addActionListener(new MenuListener());
		menuGUI.add(item);
		item = new JMenuItem("Sectors C:");
		item.addActionListener(new MenuListener());
		menuGUI.add(item);
		item = new JMenuItem("Sectors D:");
		item.addActionListener(new MenuListener());
		menuGUI.add(item);

		JMenuItem importitem = new JMenuItem("Import File");
		JMenuItem exportitem = new JMenuItem("Export File");
		JMenuItem deleteitem = new JMenuItem("Delete File");
		JMenuItem compileitem = new JMenuItem("Compile Program");
		JMenuItem runitem = new JMenuItem("Run Program");
		importitem.addActionListener(new MenuListener());
		exportitem.addActionListener(new MenuListener());
		deleteitem.addActionListener(new MenuListener());
		compileitem.addActionListener(new MenuListener());
		runitem.addActionListener(new MenuListener());
		menuDisk.add(importitem);
		menuDisk.add(exportitem);
		menuDisk.add(deleteitem);
		menuDisk.add(compileitem);
		menuDisk.add(runitem);
		JMenuItem floppya = new JMenuItem("Change Floppy A:");
		JMenuItem floppyb = new JMenuItem("Change Floppy B:");
		floppya.addActionListener(new MenuListener());
		floppyb.addActionListener(new MenuListener());
		menuDisk.add(floppya);
		menuDisk.add(floppyb);
		JMenuItem makedisk = new JMenuItem("Make Disk Image");
		makedisk.addActionListener(new MenuListener());
		menuDisk.add(makedisk);

		menubar.add(menuControl);
		menubar.add(menuGUI);
		menubar.add(menuDisk);
		menubar.add(menuConstruct);
	}

	public void constructGUI(AbstractGUI.GUIComponent guicomponent)
	{
		if (!computer.computerGUI.singleFrame)
		{
			guicomponent.add(playButton);
			guicomponent.add(fastPlayButton);
			guicomponent.add(pauseButton);
			guicomponent.add(stepButton);
			frame.setJMenuBar(menubar);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		}
		else
		{
			computer.computerGUI.computerGUIComponent.add(playButton);
			computer.computerGUI.computerGUIComponent.add(fastPlayButton);
			computer.computerGUI.computerGUIComponent.add(pauseButton);
			computer.computerGUI.computerGUIComponent.add(stepButton);
			playButton.setBounds(10,ComputerGUI.YSIZE-80,(ComputerGUI.XSIZE/2-40)/4,30);
			fastPlayButton.setBounds(10+(ComputerGUI.XSIZE/2-40)/4+10,ComputerGUI.YSIZE-80,(ComputerGUI.XSIZE/2-40)/4,30);
			pauseButton.setBounds(10+2*(ComputerGUI.XSIZE/2-40)/4+10+10,ComputerGUI.YSIZE-80,(ComputerGUI.XSIZE/2-40)/4,30);
			stepButton.setBounds(10+3*(ComputerGUI.XSIZE/2-40)/4+10+10+10,ComputerGUI.YSIZE-80,(ComputerGUI.XSIZE/2-40)/4,30);
			if (computer.applet==null)
			{
				computer.computerGUI.computerFrame.setJMenuBar(menubar);
				computer.computerGUI.computerFrame.setVisible(true);
			}
			else
			{
				computer.applet.setJMenuBar(menubar);
				computer.applet.validate();
//				computer.applet.getContentPane().add(menubar);
			}
		}
	}
	public void instructionCount()
	{
		if (computer.icount%INSTRUCTION_COUNT_UPDATE==0 || computer.debugMode)
		{
			if (!computer.computerGUI.singleFrame) setStatusLabel("Instruction count: "+computer.icount);
			else computer.computerGUI.computerFrame.setTitle("Simulator: "+computer.icount+" instructions");

//			System.out.println("Instruction count: "+computer.icount);
		}
	}
	public void pause()
	{
		computer.debugMode=true;
		computer.updateGUIOnPlay=true;
		stepButton.setEnabled(true);
		playButton.setEnabled(true);
		fastPlayButton.setEnabled(true);
		pauseButton.setEnabled(false);
	}
	public void play()
	{
		computer.debugMode=false;
		stepButton.setEnabled(false);
		playButton.setEnabled(false);
		fastPlayButton.setEnabled(false);
		pauseButton.setEnabled(true);

//		if (computer.applet==null)
			computer.stepLock.lockResume();
//		else
//			computer.computer.mainLoop();
	}
	public void step()
	{
//		if (computer.applet==null)
			computer.stepLock.lockResume();
//		else
//			computer.computer.mainLoop();
	}
	private class ControlButtonListener implements ActionListener
	{
		public void actionPerformed(ActionEvent e)
		{
			if (e.getActionCommand().equals("Play"))
			{
				computer.updateGUIOnPlay=true;
				play();
			}
			else if (e.getActionCommand().equals("Fast Play"))
			{
				computer.updateGUIOnPlay=false;
				play();
			}
			else if (e.getActionCommand().equals("Pause"))
				pause();
			else if (e.getActionCommand().equals("Step"))
				step();
		}
	}
	private class MenuListener implements ActionListener
	{
		public void changeFloppy(int drive)
		{
			JFileChooser fc = new JFileChooser();
			fc.setCurrentDirectory(new File("."));
			fc.showOpenDialog(null);
			File f = fc.getSelectedFile();
			if (f==null) return;
			String name=f.getAbsolutePath();
			if (drive==0)
				computer.floppy.changeDisk(new Disk(name),0);
			else
				computer.floppy.changeDisk(new Disk(name),1);
		}

		public void actionPerformed(ActionEvent e)
		{
			if (e.getActionCommand().equals("Reboot"))
			{
				computer.processor.reset();
			}
			else if (e.getActionCommand().equals("Set Breakpoints"))
			{
				computer.breakpointGUI=new BreakpointGUI(computer);
			}
			else if (e.getActionCommand().equals("Exit"))
			{
				System.exit(0);
			}
			else if (e.getActionCommand().equals("Change Floppy A:"))
			{
				changeFloppy(0);
			}
			else if (e.getActionCommand().equals("Change Floppy B:"))
			{
				changeFloppy(1);
			}
			else if (e.getActionCommand().equals("Import File"))
			{
				computer.fileTransferGUI=new FileTransferGUI(computer,0);
			}
			else if (e.getActionCommand().equals("Export File"))
			{
				computer.fileTransferGUI=new FileTransferGUI(computer,1);
			}
			else if (e.getActionCommand().equals("Delete File"))
			{
				computer.fileTransferGUI=new FileTransferGUI(computer,2);
			}
			else if (e.getActionCommand().equals("Run Program"))
			{
				computer.fileTransferGUI=new FileTransferGUI(computer,3);
			}
			else if (e.getActionCommand().equals("Compile Program"))
			{
				computer.fileTransferGUI=new FileTransferGUI(computer,4);
			}
			else if (e.getActionCommand().equals("Memory Transfer"))
			{
				computer.memoryTransferGUI=new MemoryTransferGUI(computer);
			}
			else if (e.getActionCommand().equals("Make Disk Image"))
			{
				computer.makeDiskGUI=new MakeDiskGUI(computer);
			}
			else if (e.getActionCommand().equals("Exit"))
			{
				System.exit(0);
			}
			else if (e.getActionCommand().equals("Processor"))
			{
//				if (!computer.debugMode && computer.applet==null) computer.cycleEndLock.lockWait();
				if (!computer.debugMode) computer.cycleEndLock.lockWait();
				if (computer.processorGUI!=null) computer.processorGUI.close();
				computer.processorGUI=new ProcessorGUI(computer);
			}
			else if (e.getActionCommand().equals("Memory"))
			{
//				if (!computer.debugMode && computer.applet==null) computer.cycleEndLock.lockWait();
				if (!computer.debugMode) computer.cycleEndLock.lockWait();
				if (computer.memoryGUI!=null) computer.memoryGUI.close();
				computer.memoryGUI=new MemoryGUI(computer);
			}
			else if (e.getActionCommand().equals("Instruction Memory"))
			{
//				if (!computer.debugMode && computer.applet==null) computer.cycleEndLock.lockWait();
				if (!computer.debugMode) computer.cycleEndLock.lockWait();
				if (computer.memoryGUI==null) computer.memoryGUI=new MemoryGUI(computer);
				computer.memoryGUI.codeFrame=new MemoryBlockGUI(computer,MemoryBlockGUI.CODE,computer.processor.cs.address(computer.processor.eip.getValue()));
			}
			else if (e.getActionCommand().equals("Stack"))
			{
//				if (!computer.debugMode && computer.applet==null) computer.cycleEndLock.lockWait();
				if (!computer.debugMode) computer.cycleEndLock.lockWait();
				if (computer.memoryGUI==null) computer.memoryGUI=new MemoryGUI(computer);
				computer.memoryGUI.stackFrame=new MemoryBlockGUI(computer,MemoryBlockGUI.STACK,computer.processor.ss.address(computer.processor.esp.getValue()));
			}
			else if (e.getActionCommand().equals("Registers"))
			{
//				if (!computer.debugMode && computer.applet==null) computer.cycleEndLock.lockWait();
				if (!computer.debugMode) computer.cycleEndLock.lockWait();
				if (computer.registerGUI!=null) computer.registerGUI.close();
				computer.registerGUI=new RegisterGUI(computer);
			}
			else if (e.getActionCommand().equals("I/O Ports"))
			{
//				if (!computer.debugMode && computer.applet==null) computer.cycleEndLock.lockWait();
				if (!computer.debugMode) computer.cycleEndLock.lockWait();
				if (computer.ioGUI!=null) computer.ioGUI.close();
				computer.ioGUI=new IOGUI(computer);
			}
			else if (e.getActionCommand().equals("Timer"))
			{
//				if (!computer.debugMode && computer.applet==null) computer.cycleEndLock.lockWait();
				if (!computer.debugMode) computer.cycleEndLock.lockWait();
				if (computer.timerGUI!=null) computer.timerGUI.close();
				computer.timerGUI=new TimerGUI(computer);
			}
			else if (e.getActionCommand().equals("Serial Port"))
			{
//				if (!computer.debugMode && computer.applet==null) computer.cycleEndLock.lockWait();
				if (!computer.debugMode) computer.cycleEndLock.lockWait();
				if (computer.serialGUI!=null) computer.serialGUI.close();
				computer.serialGUI=new SerialGUI(computer);
			}
			else if (e.getActionCommand().equals("Keyboard"))
			{
//				if (!computer.debugMode && computer.applet==null) computer.cycleEndLock.lockWait();
				if (!computer.debugMode) computer.cycleEndLock.lockWait();
				if (computer.keyboardGUI!=null) computer.keyboardGUI.close();
				else computer.keyboardGUI=new KeyboardGUI(computer);
			}
			else if (e.getActionCommand().equals("Video"))
			{
//				if (!computer.debugMode && computer.applet==null) computer.cycleEndLock.lockWait();
				if (!computer.debugMode) computer.cycleEndLock.lockWait();
				if (computer.videoGUI!=null) computer.videoGUI.close();
				else computer.videoGUI=new VideoGUI(computer);
			}
			else if (e.getActionCommand().equals("Disk A:"))
			{
//				if (!computer.debugMode && computer.applet==null) computer.cycleEndLock.lockWait();
				if (!computer.debugMode) computer.cycleEndLock.lockWait();
				if(computer.diskGUI[0]!=null) computer.diskGUI[0].close();
				computer.diskGUI[0] = new DiskGUI(computer,0);
				computer.floppy.drives[0].refreshGUI();
			}
			else if (e.getActionCommand().equals("Disk B:"))
			{
//				if (!computer.debugMode && computer.applet==null) computer.cycleEndLock.lockWait();
				if (!computer.debugMode) computer.cycleEndLock.lockWait();
				if(computer.diskGUI[1]!=null) computer.diskGUI[0].close();
				computer.diskGUI[1] = new DiskGUI(computer,1);
				computer.floppy.drives[1].refreshGUI();
			}
			else if (e.getActionCommand().equals("Disk C:"))
			{
//				if (!computer.debugMode && computer.applet==null) computer.cycleEndLock.lockWait();
				if (!computer.debugMode) computer.cycleEndLock.lockWait();
				if(computer.diskGUI[2]!=null) computer.diskGUI[0].close();
				computer.diskGUI[2] = new DiskGUI(computer,2);
				computer.harddrive.drive[0].refreshGUI();
			}
			else if (e.getActionCommand().equals("Disk D:"))
			{
//				if (!computer.debugMode && computer.applet==null) computer.cycleEndLock.lockWait();
				if (!computer.debugMode) computer.cycleEndLock.lockWait();
				if(computer.diskGUI[3]!=null) computer.diskGUI[0].close();
				computer.diskGUI[3] = new DiskGUI(computer,3);
				computer.harddrive.drive[1].refreshGUI();
			}
			else if (e.getActionCommand().equals("Sectors A:"))
			{
//				if (!computer.debugMode && computer.applet==null) computer.cycleEndLock.lockWait();
				if (!computer.debugMode) computer.cycleEndLock.lockWait();
				if(computer.sectorGUI[0]!=null) computer.sectorGUI[0].close();
				computer.sectorGUI[0] = new DiskSectorGUI(computer,0);
				computer.floppy.drives[0].refreshGUI();
			}
			else if (e.getActionCommand().equals("Sectors B:"))
			{
//				if (!computer.debugMode && computer.applet==null) computer.cycleEndLock.lockWait();
				if (!computer.debugMode) computer.cycleEndLock.lockWait();
				if(computer.sectorGUI[2]!=null) computer.sectorGUI[1].close();
				computer.sectorGUI[1] = new DiskSectorGUI(computer,1);
				computer.floppy.drives[1].refreshGUI();
			}
			else if (e.getActionCommand().equals("Sectors C:"))
			{
//				if (!computer.debugMode && computer.applet==null) computer.cycleEndLock.lockWait();
				if (!computer.debugMode) computer.cycleEndLock.lockWait();
				if(computer.sectorGUI[2]!=null) computer.sectorGUI[2].close();
				computer.sectorGUI[2] = new DiskSectorGUI(computer,2);
				computer.harddrive.drive[0].refreshGUI();
			}
			else if (e.getActionCommand().equals("Sectors D:"))
			{
//				if (!computer.debugMode && computer.applet==null) computer.cycleEndLock.lockWait();
				if (!computer.debugMode) computer.cycleEndLock.lockWait();
				if(computer.sectorGUI[3]!=null) computer.sectorGUI[3].close();
				computer.sectorGUI[3] = new DiskSectorGUI(computer,3);
				computer.harddrive.drive[1].refreshGUI();
			}
			else if (e.getActionCommand().equals("Datapath"))
			{
				computer.datapathBuilder=new DatapathBuilder(computer);
			}
			else if (e.getActionCommand().equals("Control"))
			{
				if (computer.datapathBuilder!=null)
					computer.controlBuilder=new ControlBuilder(computer);
			}
			else if (e.getActionCommand().equals("Custom Processor"))
			{
				if (computer.datapathBuilder!=null && computer.controlBuilder!=null)
					computer.customProcessor=new CustomProcessor(computer);
			}
		}
	}
}
