package simulator;

import java.awt.*;
import java.awt.event.*;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

import javax.imageio.ImageIO;
import javax.swing.*;

import simulator.DatapathBuilder.DatapathModule;

public class ComputerGUI
{
	
	private static final int INSTRUCTION_COUNT_UPDATE=50000;

	Computer computer;
	JDesktopPane dframe;
	boolean singleFrame=true;
	JTextField statusfield;
	JPanel buttonpanel,statuspanel;
	
    public ComputerGUI(Computer computer)
	{
		this.computer=computer;
		constructMenuBar();
		dframe=new ComputerDesktopPane();
		setDFrameBounds();
		buttonpanel=new JPanel();
		setButtonPanelBounds();
		generateButtons(buttonpanel);
		statuspanel=new JPanel();
		setStatusPanelBounds();
		statuspanel.setLayout(null);
		statusfield=new JTextField();
		setStatusFieldBounds();
		statusfield.setFont(statusfield.getFont().deriveFont(statusfield.getFont().getStyle() ^ Font.BOLD));
		statuspanel.add(statusfield);
		
		if (computer.applet==null)
		{
			final JFrame computerFrame = new JFrame("Simulator v1.5");
			computerFrame.setSize(computer.resolution.desktopPanelWidth,computer.resolution.desktopWindowHeight);
			computerFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			computerFrame.setLayout(null);

			computerFrame.add(statuspanel);
			computerFrame.add(buttonpanel);
			computerFrame.add(dframe);
			computerFrame.setJMenuBar(menubar);
			computerFrame.setVisible(true);
			computerFrame.addComponentListener(new ComponentListener(){
				public void componentHidden(ComponentEvent arg0) {}
				public void componentMoved(ComponentEvent arg0) {}
				public void componentResized(ComponentEvent arg0) 
				{ 
					computer.resolution.setDesktopDimensions(computerFrame.getWidth(), computerFrame.getHeight());

					setDFrameBounds();
					setStatusPanelBounds();
					setStatusFieldBounds();
					setButtonPanelBounds();
				}
				public void componentShown(ComponentEvent arg0) {}
				});
		}
		else
		{
			computer.applet.panel.setLayout(null);
			computer.applet.panel.add(buttonpanel);
			computer.applet.panel.add(dframe);
			computer.applet.setJMenuBar(constructMenuBar());
			computer.applet.panel.revalidate();
		}
	}
	public void setMenubarVisible(boolean visible) {
		menubar.setVisible(visible);
		setDFrameBounds();
		setStatusPanelBounds();
		setStatusFieldBounds();
		setButtonPanelBounds();
	}
	private int getMenubarOffset() {
		if (menubar.isVisible())
			return menubar.getHeight();
		return 0;
	}
    private void setDFrameBounds() {
		dframe.setBounds(0,0,computer.resolution.desktopWindowWidth,computer.resolution.desktopPanelHeight - getMenubarOffset());
    }
    private void setStatusPanelBounds() {
		statuspanel.setBounds(0,computer.resolution.desktopPanelHeight-getMenubarOffset(),computer.resolution.desktopPanelWidth,computer.resolution.statusHeight);
    }
    private void setStatusFieldBounds() {
		statusfield.setBounds(0,0,computer.resolution.desktopWindowWidth,computer.resolution.statusHeight);
    }
    private void setButtonPanelBounds() {
		buttonpanel.setBounds(0,computer.resolution.desktopPanelHeight+computer.resolution.statusHeight-getMenubarOffset(),computer.resolution.desktopPanelWidth,computer.resolution.buttonHeight);
    }
	JMenuBar menubar;
	private JMenuBar constructMenuBar()
	{
		menubar=new JMenuBar();
		JMenu menuControl = new JMenu("Control");
		JMenu menuGUI = new JMenu("GUI");
		JMenu menuDisk = new JMenu("Disk");
		JMenu menuConstruct = new JMenu("Construct");

		for (String s:new String[]{"Set Breakpoints","Instruction Trace","Reboot","Memory Transfer","Save Snapshot","Load Snapshot","Exit"})
		{
			JMenuItem item=new JMenuItem(s);
			item.addActionListener(new MenuListener());
			menuControl.add(item);
		}
		for (String s:new String[]{"Processor","Registers","Memory","Instruction Memory","Stack","I/O Ports","Timer","Serial Port","Keyboard","Video","Disk A:","Disk B:","Disk C:","Disk D:","Sectors A:","Sectors B:","Sectors C:","Sectors D:"})
		{
			JMenuItem item=new JMenuItem(s);
			item.addActionListener(new MenuListener());
			menuGUI.add(item);
		}
		for (String s:new String[]{"Edit File","Import File","Export File","Change Floppy A:","Change Floppy B:","Make Disk Image"})
		{
			JMenuItem item=new JMenuItem(s);
			item.addActionListener(new MenuListener());
			menuDisk.add(item);
		}
		for (String s:new String[]{"Processor Builder","Datapath","Create Module"})
		{
			JMenuItem item=new JMenuItem(s);
			item.addActionListener(new MenuListener());
			menuConstruct.add(item);
		}

		menubar.add(menuControl);
		menubar.add(menuGUI);
		menubar.add(menuDisk);
		menubar.add(menuConstruct);
		return menubar;
	}
	
	public void addComponent(AbstractGUI c)
	{
		try{
		dframe.add(c);
		c.show();
		} catch(IllegalArgumentException e){e.printStackTrace();}
	}
	public void removeComponent(AbstractGUI c)
	{
		if (c==null) return;
		dframe.remove(c);
		dframe.repaint();
	}
	
	public JButton playButton,pauseButton,stepButton,fastPlayButton;
	private void generateButtons(JPanel panel)
	{
		playButton=new JButton("Play");
		fastPlayButton=new JButton("Fast Play");
		pauseButton=new JButton("Pause");
		stepButton=new JButton("Step");
		playButton.addActionListener(new ControlButtonListener());
		fastPlayButton.addActionListener(new ControlButtonListener());
		pauseButton.addActionListener(new ControlButtonListener());
		stepButton.addActionListener(new ControlButtonListener());
		panel.add(playButton);
		panel.add(fastPlayButton);
		panel.add(pauseButton);
		panel.add(stepButton);
		playButton.setEnabled(false);
		pauseButton.setEnabled(false);
		stepButton.setEnabled(false);
		fastPlayButton.setEnabled(false);
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
	public void instructionCount()
	{
		if (computer.icount%INSTRUCTION_COUNT_UPDATE==0 || computer.debugMode || computer.updateGUIOnPlay)
		{
//			if (!computer.computerGUI.singleFrame) setStatusLabel("Instruction count: "+computer.icount);
			if (!computer.debugMode)
				statusfield.setText("RUNNING "+computer.icount+" instructions");
			else
				statusfield.setText("PAUSED "+computer.icount+" instructions");

//			System.out.println("Instruction count: "+computer.icount);
			paint();
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
		statusfield.setText("PAUSED "+computer.icount+" instructions");
	}
	public void play()
	{
		computer.debugMode=false;
		stepButton.setEnabled(false);
		playButton.setEnabled(false);
		fastPlayButton.setEnabled(false);
		pauseButton.setEnabled(true);

		computer.stepLock.lockResume();
		statusfield.setText("RUNNING "+computer.icount+" instructions");
	}
	public void step()
	{
		computer.stepLock.lockResume();
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
			pause();
			if (e.getActionCommand().equals("Reboot"))
			{
				computer.processor.reset();
				new MemoryVisualizationGUI(computer);
			}
			else if (e.getActionCommand().equals("Set Breakpoints"))
			{
				computer.breakpointGUI=new BreakpointGUI(computer);
			}
			else if (e.getActionCommand().equals("Instruction Trace"))
			{
				if (!computer.debugMode) computer.cycleEndLock.lockWait();
				computer.trace=new Trace(computer);
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
			else if (e.getActionCommand().equals("Edit File"))
			{
				computer.fileTransferGUI=new FileTransferGUI(computer,2);
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
				if(computer.diskGUI[1]!=null) computer.diskGUI[1].close();
				computer.diskGUI[1] = new DiskGUI(computer,1);
				computer.floppy.drives[1].refreshGUI();
			}
			else if (e.getActionCommand().equals("Disk C:"))
			{
//				if (!computer.debugMode && computer.applet==null) computer.cycleEndLock.lockWait();
				if (!computer.debugMode) computer.cycleEndLock.lockWait();
				if(computer.diskGUI[2]!=null) computer.diskGUI[2].close();
				computer.diskGUI[2] = new DiskGUI(computer,2);
				computer.harddrive.drive[0].refreshGUI();
			}
			else if (e.getActionCommand().equals("Disk D:"))
			{
//				if (!computer.debugMode && computer.applet==null) computer.cycleEndLock.lockWait();
				if (!computer.debugMode) computer.cycleEndLock.lockWait();
				if(computer.diskGUI[3]!=null) computer.diskGUI[3].close();
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
			else if (e.getActionCommand().equals("Processor Builder"))
			{
				computer.processorBuilder=new ProcessorBuilder(computer);
			}
			else if (e.getActionCommand().equals("Datapath"))
				if (computer.datapathBuilder==null)
				{
					computer.datapathBuilder=new DatapathBuilder(computer);
				}
				else
				{
					computer.datapathBuilder.setStatusLabel("Save and close current Datapath to open another Datapath");
				}
			else if (e.getActionCommand().equals("Control"))
			{
				if (computer.datapathBuilder!=null)
					computer.controlBuilder=new ControlBuilder(computer,computer.datapathBuilder.defaultModule);
			}
			else if (e.getActionCommand().equals("Custom Processor"))
			{
//				if (computer.datapathBuilder!=null)
//					if (computer.datapathBuilder!=null && computer.controlBuilder!=null)
					computer.customProcessor=new CustomProcessor(computer);
			}
			else if (e.getActionCommand().equals("Close Custom Processor"))
			{
					computer.customProcessor=null;
					computer.datapathBuilder.repaint();
					computer.controlBuilder.repaint();
			}
			else if (e.getActionCommand().equals("Create Module"))
			{
				JFileChooser fc = new JFileChooser("Choose Datapath");
				fc.setDialogTitle("Choose a datapath file");
				fc.setCurrentDirectory(new File("."));
				fc.showOpenDialog(null);
				File f=fc.getSelectedFile();
				if (f==null) return;
				String name=f.getAbsolutePath();

				String xml="";
				try
				{
					FileReader r=new FileReader(name);
					
					Scanner s=new Scanner(r);
					while(s.hasNextLine())
						xml+=s.nextLine()+" ";
					s.close();
				}
				catch(IOException x)
				{
					System.out.println("Error reading file "+name);
				}
				
				fc = new JFileChooser("Choose Control");
				fc.setDialogTitle("Choose a control file");
				fc.setCurrentDirectory(new File("."));
				fc.showOpenDialog(null);
				f=fc.getSelectedFile();
				if (f==null) return;
				String cname=f.getAbsolutePath();
				
				fc = new JFileChooser("Choose Module File");
				fc.setDialogTitle("Name your module file");
				fc.setCurrentDirectory(new File("."));
				fc.showSaveDialog(null);
				f=fc.getSelectedFile();
				if (f==null) return;
				String mname=f.getAbsolutePath();
				
				CustomProcessor.createModule(name,cname,mname,xml);
			}
			
			else if (e.getActionCommand().equals("Save Snapshot"))
			{
				if (!computer.debugMode) computer.cycleEndLock.lockWait();
				
				String state=computer.saveState();
				
				try
				{
					PrintWriter p=new PrintWriter("state.txt");
					p.print(state);
					p.close();
				}
				catch(Exception ex)
				{
					System.out.println("Couldn't save to state.txt");
				}
			}
			else if (e.getActionCommand().equals("Load Snapshot"))
			{
				if (!computer.debugMode) computer.cycleEndLock.lockWait();

				try
				{
					byte[] buffer=new byte[(int)new File("state.txt").length()];
					BufferedInputStream f=new BufferedInputStream(new FileInputStream("state.txt"));
					f.read(buffer);
					f.close();
					computer.loadState(new String(buffer));
				}
				catch(Exception ex)
				{
					ex.printStackTrace();
					System.out.println("Couldn't load from state.txt");
				}
			}
		}
	}

	private class ComputerDesktopPane extends JDesktopPane implements MouseListener,MouseMotionListener
	{
		String mouseOverText="";
		int mouseOverX=0,mouseOverY=0;
		ImageIcon backgroundimage;
		public ComputerDesktopPane()
		{
			super();
			this.addMouseListener(this);
			this.addMouseMotionListener(this);
			try{
//				image=new ImageIcon(computer.getClass().getClassLoader().getResource("resource/Motherboard3.png"));
				backgroundimage=new ImageIcon(computer.getClass().getResource("/resource/Motherboard3.png"));
			}catch(Exception e){e.printStackTrace();}
		}
		public void paintComponent(Graphics g)
			{
/*				// paint motherboard vector
				File imgpath = new File("Motherboard2.png");
				try {
					Image motherboardimg = ImageIO.read(imgpath);
					g.drawImage(motherboardimg, 0, 0, null);
				} 
				catch (Exception e) {
					e.printStackTrace();
				}
			*/
			try{
				g.drawImage(backgroundimage.getImage(),0,0,null);
				showPorts(g);
				showMouseOver(g);
			}catch(Exception e){e.printStackTrace();}
				
			}	
		private void showMouseOver(Graphics g)
		{
			if (mouseOverText.equals("")) return;
			g.setColor(Color.WHITE);
			g.fillRect(mouseOverX,mouseOverY,200,35);
			g.setColor(Color.BLACK);
			g.drawRect(mouseOverX,mouseOverY,200,35);
			g.drawString(mouseOverText,mouseOverX+5,mouseOverY+30);
		}
		private void showPorts(Graphics g)
		{
			if (computer.memoryGUI!=null && (computer.memoryGUI.memoryRead||computer.memoryGUI.memoryWrite||computer.memoryGUI.romRead))
			{
				int p;
				Color c;
				if (computer.memoryGUI.memoryRead)
				{
					c=Color.GREEN;
					drawlines(g,new int[]{this.PROCESSOR,this.BUSCORNER,this.BUSMEMORY,this.MEMORY},c);
				}
				else if (computer.memoryGUI.memoryWrite)
				{
					c=Color.RED;	
					drawlines(g,new int[]{this.PROCESSOR,this.BUSCORNER,this.BUSMEMORY,this.MEMORY},c);
				}
				else
				{
					c=Color.GREEN;
					drawlines(g,new int[]{this.PROCESSOR,this.BUSCORNER,this.BUSA,this.ROM},c);
				}
			}
			if (computer.ioGUI!=null && computer.ioGUI.interruptTriggered)
			{
				drawlines(g,new int[]{this.INTERRUPTC,this.INTERRUPTD},Color.RED);
			}
			if (computer.ioGUI!=null && (computer.ioGUI.portRead||computer.ioGUI.portWrite))
			{
				int p;
				Color c;
				if (computer.ioGUI.portRead)
				{
					p=computer.ioGUI.portLastRead;
					c=Color.GREEN;
				}
				else
				{
					p=computer.ioGUI.portLastWrite;
					c=Color.RED;	
				}
				switch(p)
				{
				case 0x20: case 0x21: case 0xa0: case 0xa1:	case 0x4d0: case 0x4d1:	//interrupt controller
					drawlines(g,new int[]{this.PROCESSOR,this.INTERRUPTA,this.INTERRUPTB},c);
					break;
				case 0x40: case 0x41: case 0x42: case 0x43:	//timer
					drawlines(g,new int[]{this.PROCESSOR,this.BUSCORNER,this.BUSA,this.BUSTIMER,this.TIMER},c);
					break;
				case 0x60: case 0x64:	//keyboard
					drawlines(g,new int[]{this.PROCESSOR,this.BUSCORNER,this.BUSA,this.BUSB,this.BUSKEYBOARD,this.KEYBOARD},c);
					break;
				case 0x70: case 0x71:	//cmos
					drawlines(g,new int[]{this.PROCESSOR,this.BUSCORNER,this.BUSA,this.ROM},c);
					break;
				case 0x1f0: case 0x1f1: case 0x1f2: case 0x1f3: case 0x1f4: case 0x1f5: case 0x1f6: case 0x1f7: case 0x3f6:	//ide
					drawlines(g,new int[]{this.PROCESSOR,this.BUSCORNER,this.BUSA,this.BUSIDE,this.IDE},c);
					break;
				case 0x3b4: case 0x3b5: case 0x3c0: case 0x3c1: case 0x3c4: case 0x3c5: case 0x3ce: case 0x3cf:	case 0x3d4: case 0x3d5: case 0x3da: //video
					drawlines(g,new int[]{this.PROCESSOR,this.BUSCORNER,this.BUSA,this.BUSB,this.VIDEOCONTROLLER},c);
					break;
				case 0x3f1: case 0x3f2: case 0x3f3: case 0x3f4: case 0x3f5: case 0x3f7:	//floppy
					drawlines(g,new int[]{this.PROCESSOR,this.BUSCORNER,this.BUSFLOPPY,this.FLOPPY},c);
					break;
				case 0x3f8: case 0x3f9: case 0x3fa: case 0x3fb: case 0x3fc: case 0x3fd: case 0x3fe: case 0x3ff:	//serial
					drawlines(g,new int[]{this.PROCESSOR,this.BUSCORNER,this.BUSA,this.BUSB,this.BUSUART,this.UART},c);
					break;
				}
			}
			if (computer.ioGUI!=null && computer.ioGUI.interruptRequested)
			{
				int irq=computer.ioGUI.lastInterrupt;
				switch(irq)
				{
				case 0:	//timer
					drawlines(g,new int[]{this.TIMER,this.BUSTIMER,this.BUSA,this.BUSCORNER,this.INTERRUPTA,this.INTERRUPTB},Color.YELLOW);
					break;
				case 1:	//keyboard
					drawlines(g,new int[]{this.KEYBOARD,this.BUSKEYBOARD,this.BUSB,this.BUSA,this.BUSCORNER,this.INTERRUPTA,this.INTERRUPTB},Color.YELLOW);
					break;
				case 4:	//serial
					drawlines(g,new int[]{this.UART,this.BUSUART,this.BUSB,this.BUSA,this.BUSCORNER,this.INTERRUPTA,this.INTERRUPTB},Color.YELLOW);
					break;
				case 6: //floppy
					drawlines(g,new int[]{this.FLOPPY,this.BUSFLOPPY,this.BUSCORNER,this.INTERRUPTA,this.INTERRUPTB},Color.YELLOW);
					break;
				case 14:	//ide
					drawlines(g,new int[]{this.IDE,this.BUSIDE,this.BUSA,this.BUSCORNER,this.INTERRUPTA,this.INTERRUPTB},Color.YELLOW);
					break;
				}
			}
		}
		private void drawlines(Graphics g, int[] points, Color c)
		{
			g.setColor(c);
			for (int i=0; i<points.length-1; i++)
			{
				//horizontal?
				if (guipoints[points[i]]!=guipoints[points[i+1]])
				{
					g.drawLine(guipoints[points[i]],guipoints[points[i]+1],guipoints[points[i+1]],guipoints[points[i+1]+1]);
					g.drawLine(guipoints[points[i]],1+guipoints[points[i]+1],guipoints[points[i+1]],1+guipoints[points[i+1]+1]);
					g.drawLine(guipoints[points[i]],-1+guipoints[points[i]+1],guipoints[points[i+1]],-1+guipoints[points[i+1]+1]);
				}
				else
				{
					g.drawLine(guipoints[points[i]],guipoints[points[i]+1],guipoints[points[i+1]],guipoints[points[i+1]+1]);					
					g.drawLine(1+guipoints[points[i]],guipoints[points[i]+1],1+guipoints[points[i+1]],guipoints[points[i+1]+1]);					
					g.drawLine(-1+guipoints[points[i]],guipoints[points[i]+1],-1+guipoints[points[i+1]],guipoints[points[i+1]+1]);					
				}
			}
		}
		private final int[] guipoints=new int[]{936,292,936,341,852,341,936,509,636,509,636,495,353,509,227,509,178,509,227,311,125,311,227,255,333,255,227,200,99,200,158,200,321,200,444,200,543,200,158,158,321,158,444,158,543,158,227,158,321,118,321,91,444,118,444,91,373,329,403,329,665,329,775,329,775,91,373,255,407,255,657,255,694,255,694,155,694,91,618,155,618,91,353,351,831,321,831,291};
		private final int PROCESSOR=0,INTERRUPTA=2,INTERRUPTB=4,BUSCORNER=6,BUSMEMORY=8,MEMORY=10,BUSFLOPPY=12,BUSA=14,ROM=16,BUSTIMER=18,TIMER=20,BUSIDE=22,IDE=24,BUSB=26,VIDEOCONTROLLER=28,BUSETHERNET=30,BUSKEYBOARD=32,BUSUART=34,BUSUSB=36,ETHERNET=38,KEYBOARD=40,UART=42,USB=44,SOUND=46,KEYBOARDA=48,KEYBOARDB=50,SERIALA=52,SERIALB=54,FLOPPYA=56,FLOPPYB=58,FLOPPYC=60,FLOPPYD=62,FLOPPYE=64,IDEA=66,IDEB=68,IDEC=70,IDED=72,IDEE=74,CD=76,HARDA=78,HARDB=80,FLOPPY=82,INTERRUPTC=84,INTERRUPTD=86;
		private final int[][] guirects=new int[][]{{24,27,99,68},{125,19,185,65},{204,20,248,60},{275,21,319,65},{321,23,357,63},{400,17,489,71},{519,23,566,65},{591,13,644,80},{666,21,722,72},{745,18,803,75},{833,18,887,71},{924,35,977,62},{20,154,98,231},{140,115,178,158},{206,118,245,158},{302,118,342,157},{425,118,465,158},{523,118,560,156},{796,110,978,288},{834,170,886,185},{867,222,882,257},{891,222,905,256},{933,174,944,195},{333,236,373,275},{406,237,656,269},{33,284,124,333},{334,310,373,349},{403,307,663,351},{812,321,852,364},{37,388,96,460},{51,491,179,529},{402,387,875,493},{411,524,864,561},{928,528,980,558}};
		private final int RVIDEO=0,RETHERNET=1,RAUDIO=2,RKEYBOARD=3,RMOUSE=4,RSERIAL=5,RUSB=6,RHARD=7,RCD=8,RFLOPPY=9,RSNAPSHOT=10,RPOWER=11,RVIDEOCONTROLLER=12,RETHERNETCONTROLLER=13,RSOUNDCARD=14,RKEYBOARDCONTROLLER=15,RUART=16,RUSBCONTROLLER=17,RPROCESSOR=18,RREGISTERS=19,RCACHE=20,RMMU=21,RALU=22,RIDECONTROLLER=23,RIDEBUS=24,RTIMER=25,RFLOPPYCONTROLLER=26,RFLOPPYBUS=27,RINTERRUPTCONTROLLER=28,RBATTERY=29,RROM=30,RMEMORY=31,RPCI=32,RCLOCK=33;
		public void mouseDragged(MouseEvent arg0) {}
		public void mouseMoved(MouseEvent arg0) 
		{
			int x=arg0.getX(),y=arg0.getY();
			mouseOverText="";
			for (int i=33; i>=0; i--)
			{
				if (x>=guirects[i][0] && x<=guirects[i][2] && y>=guirects[i][1] && y<=guirects[i][3])
				{
					mouseOverX=guirects[i][0]+20;
					mouseOverY=guirects[i][3]+10;
					switch(i)
					{
					case RVIDEO:
//						mouseOverText="Video";
						break;
					}
				}
			}
			dframe.repaint();
		}
		public void mouseClicked(MouseEvent arg0) 
		{
			int x=arg0.getX(),y=arg0.getY();
			for (int i=33; i>=0; i--)
			{
				if (x>=guirects[i][0] && x<=guirects[i][2] && y>=guirects[i][1] && y<=guirects[i][3])
				{
					pause();
					switch(i)
					{
					case RVIDEO:
						if (!computer.debugMode) computer.cycleEndLock.lockWait();
						if (computer.videoGUI!=null) computer.videoGUI.close();
						else computer.videoGUI=new VideoGUI(computer);
						break;
					case RKEYBOARD: case RKEYBOARDCONTROLLER:
						if (!computer.debugMode) computer.cycleEndLock.lockWait();
						if (computer.keyboardGUI!=null) computer.keyboardGUI.close();
						else computer.keyboardGUI=new KeyboardGUI(computer);
						break;
					case RSERIAL: case RUART:
						if (!computer.debugMode) computer.cycleEndLock.lockWait();
						if (computer.serialGUI!=null) computer.serialGUI.close();
						computer.serialGUI=new SerialGUI(computer);
						break;
					case RHARD:
						if(computer.sectorGUI[2]!=null) computer.sectorGUI[2].close();
						computer.sectorGUI[2] = new DiskSectorGUI(computer,2);
						computer.harddrive.drive[0].refreshGUI();
						if(computer.sectorGUI[3]!=null) computer.sectorGUI[3].close();
						computer.sectorGUI[3] = new DiskSectorGUI(computer,3);
						computer.harddrive.drive[1].refreshGUI();
						break;
					case RCD:
						if(computer.sectorGUI[2]!=null) computer.sectorGUI[2].close();
						computer.sectorGUI[2] = new DiskSectorGUI(computer,2);
						computer.harddrive.drive[0].refreshGUI();
						if(computer.sectorGUI[3]!=null) computer.sectorGUI[3].close();
						computer.sectorGUI[3] = new DiskSectorGUI(computer,3);
						computer.harddrive.drive[1].refreshGUI();
						break;
					case RFLOPPY:
						if (!computer.debugMode) computer.cycleEndLock.lockWait();
						if(computer.sectorGUI[0]!=null) computer.sectorGUI[0].close();
						computer.sectorGUI[0] = new DiskSectorGUI(computer,0);
						computer.floppy.drives[0].refreshGUI();
						if(computer.sectorGUI[1]!=null) computer.sectorGUI[1].close();
						computer.sectorGUI[1] = new DiskSectorGUI(computer,1);
						computer.floppy.drives[1].refreshGUI();
						break;
					case RSNAPSHOT:
						if (arg0.getButton()!=MouseEvent.BUTTON1)
						{
							if (!computer.debugMode) computer.cycleEndLock.lockWait();
							
							String state=computer.saveState();
							
							try
							{
								PrintWriter p=new PrintWriter("state.txt");
								p.print(state);
								p.close();
							}
							catch(Exception ex)
							{
								System.out.println("Couldn't save to state.txt");
							}
						}
						else
						{
							if (!computer.debugMode) computer.cycleEndLock.lockWait();

							try
							{
								byte[] buffer=new byte[(int)new File("state.txt").length()];
								BufferedInputStream f=new BufferedInputStream(new FileInputStream("state.txt"));
								f.read(buffer);
								f.close();
								computer.loadState(new String(buffer));
							}
							catch(Exception ex)
							{
								ex.printStackTrace();
								System.out.println("Couldn't load from state.txt");
							}
						}
						break;
					case RPOWER:
						computer.processor.reset();
						break;
					case RREGISTERS:
						if (!computer.debugMode) computer.cycleEndLock.lockWait();
						if (computer.registerGUI!=null) computer.registerGUI.close();
						computer.registerGUI=new RegisterGUI(computer);
						return;
					case RPROCESSOR:
						if (!computer.debugMode) computer.cycleEndLock.lockWait();
						if (computer.processorGUI!=null) computer.processorGUI.close();
						computer.processorGUI=new ProcessorGUI(computer);
						break;
					case RTIMER:
						if (!computer.debugMode) computer.cycleEndLock.lockWait();
						if (computer.timerGUI!=null) computer.timerGUI.close();
						computer.timerGUI=new TimerGUI(computer);
						break;
					case RROM:
						if (!computer.debugMode) computer.cycleEndLock.lockWait();
						if (computer.memoryGUI==null) computer.memoryGUI=new MemoryGUI(computer);
						computer.memoryGUI.codeFrame=new MemoryBlockGUI(computer,MemoryBlockGUI.CODE,0xf0000);
						break;
					case RMEMORY:
						if (!computer.debugMode) computer.cycleEndLock.lockWait();
						if (computer.memoryGUI!=null) computer.memoryGUI.close();
						computer.memoryGUI=new MemoryGUI(computer);
						break;
					case RPCI:
						if (!computer.debugMode) computer.cycleEndLock.lockWait();
						if (computer.ioGUI!=null) computer.ioGUI.close();
						computer.ioGUI=new IOGUI(computer);
						break;
					}
				}
			}
		}
		public void mouseEntered(MouseEvent arg0) {}
		public void mouseExited(MouseEvent arg0) {}
		public void mousePressed(MouseEvent arg0) {}
		public void mouseReleased(MouseEvent arg0) {}
	}
	private boolean paintNext=true;
	public void paint()
	{
		
//		dframe.repaint();
		if (paintNext||(computer.ioGUI!=null &&(computer.ioGUI.portRead||computer.ioGUI.portWrite||computer.ioGUI.interruptRequested||computer.ioGUI.interruptTriggered)))
		{
			dframe.paintImmediately(0, 0, computer.resolution.desktopPanelWidth,computer.resolution.desktopPanelHeight);
			paintNext=false;
		}
		else if (computer.memoryGUI!=null && (computer.memoryGUI.memoryRead||computer.memoryGUI.memoryWrite||computer.memoryGUI.romRead))
		{
		}
//		else
//			dframe.repaint();
		if (computer.ioGUI!=null && (computer.ioGUI.portRead||computer.ioGUI.portWrite||computer.ioGUI.interruptRequested||computer.ioGUI.interruptTriggered))
		{
			computer.ioGUI.portRead=false;
			computer.ioGUI.portWrite=false;
			computer.ioGUI.interruptRequested=false;
			computer.ioGUI.interruptTriggered=false;
			paintNext=true;
		}
		if (computer.memoryGUI!=null && (computer.memoryGUI.memoryRead||computer.memoryGUI.memoryWrite||computer.memoryGUI.romRead))
		{
			computer.memoryGUI.memoryRead=false;
			computer.memoryGUI.memoryWrite=false;
			computer.memoryGUI.romRead=false;
//			paintNext=true;
		}
	}
}





