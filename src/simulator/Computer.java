/*
Computer.java
Michael Black, 6/10


Computer builds the PC, starts it running
*/

package simulator;
import java.io.*;
import java.util.Scanner;

import javax.swing.JOptionPane;

public class Computer
{
	public Processor processor;
	public PhysicalMemory physicalMemory;
	public LinearMemory linearMemory;
	public Clock clock;
	public Keyboard keyboard;
	public CMOS cmos;
	public Floppy floppy;
	public DMA dma1, dma2;
	public InterruptController interruptController;
	public Timer timer;
	public Video video;
	public HardDrive harddrive;
	public SerialPort serialport;

	public ComputerGUI computerGUI;

	public ControlGUI controlGUI;
	public VideoGUI videoGUI;
	public ProcessorGUI processorGUI;
	public MemoryGUI memoryGUI;
	public KeyboardGUI keyboardGUI;
	public IOGUI ioGUI;
	public TimerGUI timerGUI;
	public RegisterGUI registerGUI;
	public BreakpointGUI breakpointGUI;
	public FileTransferGUI fileTransferGUI;
	public MemoryTransferGUI memoryTransferGUI;
	public SerialGUI serialGUI;
	public DiskGUI[] diskGUI = new DiskGUI[4];
	public DiskSectorGUI[] sectorGUI = new DiskSectorGUI[4];
	public MakeDiskGUI makeDiskGUI;
	public DatapathBuilder datapathBuilder;
	public ControlBuilder controlBuilder;
	public CustomProcessor customProcessor;
	public IOPorts ioports;
	public Trace trace;

	public boolean keyReady=false;
	public boolean oneScreen=false;
	public boolean updateGUIOnPlay=false;

	public int icount=0;
	public boolean boot;
	public boolean debugMode=false;
	public boolean slowMode=false;

	public BootGUI bootgui;

	public Lock stepLock;
	public Lock cycleEndLock;

	public Computer computer;
	public ComputerApplet applet=null;
	
	public String saveState()
	{
		System.out.println("saving state");
		String state="";
		state+="Computer@"+icount+"@";
		state+="Settings@"+bootgui.saveState()+"@";
		if (bootgui.includeDevice("Processor")) state+="Processor@"+processor.saveState()+"@";
		if (bootgui.includeDevice("Memory")) state+="PhysicalMemory@"+physicalMemory.saveState()+"@";
		if (bootgui.includeDevice("Memory")) state+="LinearMemory@"+linearMemory.saveState()+"@";
		if (bootgui.includeDevice("Interrupt Controller")) state+="InterruptController@"+interruptController.saveState()+"@";
		if (bootgui.includeDevice("Keyboard")) state+="Keyboard@"+keyboard.saveState()+"@";
		if (bootgui.includeDevice("CMOS")) state+="CMOS@"+cmos.saveState()+"@";
		if (bootgui.includeDevice("IDE Controller")) state+="HardDrive@"+harddrive.saveState()+"@";
		state+="Clock@"+clock.saveState()+"@";
		if (bootgui.includeDevice("Timer")) state+="Timer@"+timer.saveState()+"@";
		if (bootgui.includeDevice("Video")) state+="Video@"+video.saveState()+"@";
		System.out.println("state saved");
		return state;
	}
	
	private int indexOf(String[] list, String key)
	{
		for (int i=0; i<list.length; i++)
			if (list[i].equals(key)) return i;
		return -1;
	}
	
	private void loaderror(String deviceName)
	{
		System.out.println("No "+deviceName+" found in state file.  Using default device setting.");
	}
	
	public void loadState(String state)
	{
		int s=0;
		System.out.println("loading state");
		String[] states=state.split("@");
		if (indexOf(states,"Settings")>=0) bootgui.loadState(states[indexOf(states,"Settings")+1]); else loaderror("Settings");
		if (bootgui.includeDevice("Memory")) { physicalMemory=new PhysicalMemory(this); if (indexOf(states,"PhysicalMemory")>=0) physicalMemory.loadState(states[indexOf(states,"PhysicalMemory")+1]); else loaderror("PhysicalMemory");}
		if (bootgui.includeDevice("Memory")) { linearMemory=new LinearMemory(this); if (indexOf(states,"LinearMemory")>=0) linearMemory.loadState(states[indexOf(states,"LinearMemory")+1]); else loaderror("LinearMemory");}
		  if (bootgui.includeDevice("I/O Ports")) ioports=new IOPorts(this);
		if (bootgui.includeDevice("Processor")) { processor=new Processor(this); if (indexOf(states,"Processor")>=0) processor.loadState(states[indexOf(states,"Processor")+1]); else loaderror("Processor");}
		if (bootgui.includeDevice("Interrupt Controller")) { interruptController=new InterruptController(this); if (indexOf(states,"InterruptController")>=0) interruptController.loadState(states[indexOf(states,"InterruptController")+1]); else loaderror("InterruptController");}
		if (bootgui.includeDevice("Keyboard")) { keyboard=new Keyboard(this); if (indexOf(states,"Keyboard")>=0) keyboard.loadState(states[indexOf(states,"Keyboard")+1]); else loaderror("Keyboard");}
		  if (bootgui.includeDevice("DMA Controller")) dma1 = new DMA(this,false,true);
		  if (bootgui.includeDevice("DMA Controller")) dma2 = new DMA(this,false,false);
		  if (bootgui.includeDevice("Floppy Controller")) floppy = new Floppy(this);
		if (bootgui.includeDevice("IDE Controller")) { harddrive=new HardDrive(this); if (indexOf(states,"HardDrive")>=0) harddrive.loadState(states[indexOf(states,"HardDrive")+1]); else loaderror("HardDrive");}
		if (bootgui.includeDevice("CMOS")) { cmos=new CMOS(this); if (indexOf(states,"CMOS")>=0) cmos.loadState(states[indexOf(states,"CMOS")+1]); else loaderror("CMOS");}
		clock=new Clock(); if (indexOf(states,"Clock")>=0) clock.loadState(states[indexOf(states,"Clock")+1]); else loaderror("Clock");
		if (bootgui.includeDevice("Timer")) { timer=new Timer(this); if (indexOf(states,"Timer")>=0) timer.loadState(states[indexOf(states,"Timer")+1]); else loaderror("Timer");}
		  if (bootgui.includeDevice("Serial Port")) serialport = new SerialPort(this);
		if (bootgui.includeDevice("Video")) { video=new Video(this); if (indexOf(states,"Video")>=0) video.loadState(states[indexOf(states,"Video")+1]); else loaderror("Video");}
		
		if (indexOf(states,"Computer")>=0) 
		{
			Scanner loader=new Scanner(states[indexOf(states,"Computer")+1]);
			icount=loader.nextInt();
		}
		else loaderror("Computer");
		
		if (videoGUI!=null)
			videoGUI.repaint();
		if (keyboardGUI!=null)
			keyboardGUI.repaint();
		if (controlGUI!=null)
			controlGUI.repaint();
		
		System.out.println("state loaded");
	}
	
	public Computer(ComputerApplet applet)
	{
		this.applet=applet;
		oneScreen=true;

		startComputer();
	}

	public Computer()
	{
		startComputer();
	}
	
	public void startComputer()
	{
		computer=this;
		initialize();
		initializeGUIs();
		controlGUI.refresh();
		new Thread(new Runnable(){public void run(){
			mainLoop(); }}).start();
	}

	public void initialize()
	{
		stepLock=new Lock();
		cycleEndLock=new Lock();

		computerGUI=new ComputerGUI(computer,oneScreen);

		controlGUI=new ControlGUI(computer);

		bootgui = new BootGUI(computer,
new String[]{"Processor","Memory","BIOS ROM","VGA ROM","Registers","I/O Ports","Video","Keyboard","Floppy Controller","Interrupt Controller","IDE Controller","CMOS","Timer","DMA Controller","Serial Port"},
new boolean[]{true,true,true,true,false,true,true,true,true,true,true,true,true,true,true},
new boolean[]{true,true,false,false,true,true,true,true,false,false,false,false,true,false,true}
);
		stepLock.lockWait();
	}

	public void mainLoop()
	{
		while(true)
		{
			if (customProcessor!=null && customProcessor.active)
				customProcessor.doCycle();
			else
				cycle();
			if (debugMode)
				stepLock.lockWait();
			if (applet!=null && applet.suspended)
			{
				System.out.println("suspending applet");
				applet.appletLock.lockWait();
			}
		}
	}

	public void initializeGUIs()
	{
		clock = new Clock();

		if (bootgui.diskGUI[0]) diskGUI[0]=new DiskGUI(computer,0);
		if (bootgui.diskGUI[1]) diskGUI[1]=new DiskGUI(computer,1);
		if (bootgui.diskGUI[2]) diskGUI[2]=new DiskGUI(computer,2);
		if (bootgui.diskGUI[3]) diskGUI[3]=new DiskGUI(computer,3);

		if (bootgui.sectorGUI[0]) sectorGUI[0]=new DiskSectorGUI(computer,0);
		if (bootgui.sectorGUI[1]) sectorGUI[1]=new DiskSectorGUI(computer,1);
		if (bootgui.sectorGUI[2]) sectorGUI[2]=new DiskSectorGUI(computer,2);
		if (bootgui.sectorGUI[3]) sectorGUI[3]=new DiskSectorGUI(computer,3);

		if (bootgui.includeDevice("Memory")) physicalMemory = new PhysicalMemory(computer);
		if (bootgui.includeDevice("Memory")) linearMemory = new LinearMemory(computer);
		try
		{
			if (bootgui.includeDevice("BIOS ROM")) physicalMemory.loadBIOS(computer.getClass().getClassLoader().getResource("resource/bios.bin"),0xf0000,0xfffff);
			if (bootgui.includeDevice("VGA ROM")) physicalMemory.loadBIOS(computer.getClass().getClassLoader().getResource("resource/vgabios.bin"),0xc0000,0xcffff);
		}
		catch(IOException e)
		{
			System.out.println("Error loading BIOS image: "+e);
			JOptionPane.showMessageDialog(null, "Cannot load the BIOS image");
		}
		catch(NullPointerException e)
		{
			System.out.println("Error loading BIOS image: "+e);
			JOptionPane.showMessageDialog(null, "Cannot load the BIOS image");
		}
		if (bootgui.includeDevice("I/O Ports")) ioports=new IOPorts(this);
		if (bootgui.includeDevice("Processor")) processor = new Processor(this);
		if (bootgui.includeDevice("Interrupt Controller")) interruptController = new InterruptController(this);
		if (bootgui.includeDevice("Keyboard")) keyboard = new Keyboard(this);
		if (bootgui.includeDevice("DMA Controller")) dma1 = new DMA(this,false,true);
		if (bootgui.includeDevice("DMA Controller")) dma2 = new DMA(this,false,false);
		if (bootgui.includeDevice("Floppy Controller")) floppy = new Floppy(this);
		if (bootgui.includeDevice("IDE Controller")) harddrive = new HardDrive(this);
		if (bootgui.includeDevice("CMOS")) cmos = new CMOS(this);
		if (bootgui.includeDevice("Timer")) timer = new Timer(this);
		if (bootgui.includeDevice("Serial Port")) serialport = new SerialPort(this);
		if (bootgui.includeDevice("Video")) video = new Video(this);

		if (bootgui.showGUI("Processor")) processorGUI=new ProcessorGUI(this);
		if (bootgui.showGUI("Registers")) registerGUI=new RegisterGUI(this);
		if (bootgui.showGUI("I/O Ports")) ioGUI=new IOGUI(this);
		if (bootgui.showGUI("Timer")) timerGUI=new TimerGUI(this);
		if (bootgui.showGUI("Memory")) memoryGUI=new MemoryGUI(this);
		if (bootgui.showGUI("Video")) videoGUI=new VideoGUI(this);
		if (bootgui.showGUI("Serial Port")) serialGUI=new SerialGUI(this);
		if (bootgui.showGUI("Keyboard")) keyboardGUI=new KeyboardGUI(this);
	}

	public void cycle()
	{
		//advance timers
		clock.cycle();

		//handle keyboard
		//this only pertains the the terminal keyboard.  the gui calls keyboard directly
		if (trace!=null)
			trace.newTraceEntry(icount);
		if (debugMode)
			System.out.printf("Cycle count %x:\n",icount);
		//execute an instruction
		processor.executeAnInstruction();

		//TODO: hope to get rid of this call eventually
//		magic();
		
		//handle incoming interrupts
		processor.processInterrupts();

//this was just moved: verify that it's right
		if (breakpointGUI!=null)
		{
			if (breakpointGUI.atBreakpoint())
				controlGUI.pause();
		}

		if (registerGUI!=null)
			registerGUI.readRegisters();

/*		if (registerGUI!=null)
		{
			if (debugMode || updateGUIOnPlay)
				processor.printRegisters();
		}
*/
		if (trace!=null)
		{
			trace.addRegisters(processor);
			trace.closeTraceEntry();
		}
		
		icount++;
		
		controlGUI.instructionCount();		
		cycleEndLock.lockResume();

	}

	public static void main(String[] args) throws IOException
	{
		new Computer();
	}

	public static class Lock
	{
		public void lockWait()
		{
			synchronized(this)
			{
				try
				{
					this.wait();
				}
				catch(InterruptedException e)
				{
				}
			}
		}
		public void lockResume()
		{
			synchronized(this)
			{
				this.notify();
			}
		}
	}
	
	//HACK
	//"magically" change values so that the trace matches JPC's
	//that way I can find the legitimate divergences
	//specific to l.iso (ttylinux)
	private void magic()
	{
/*		if (icount<3000000 && computer.processor.eip.getValue()==0x520 && computer.processor.edx.getValue()==0x177 && computer.processor.eax.getValue()==0x111101ff)
			computer.processor.eax.setValue(0x11110100);
		if (icount<3000000 && computer.processor.eip.getValue()==0x520 && computer.processor.edx.getValue()==0x172 && computer.processor.eax.getValue()==0x111101ff)
			computer.processor.eax.setValue(0x11110101);
		if (icount<3000000 && computer.processor.eip.getValue()==0x520 && computer.processor.edx.getValue()==0x173 && computer.processor.eax.getValue()==0x111101ff)
			computer.processor.eax.setValue(0x11110101);
		if (icount<3000000 && computer.processor.eip.getValue()==0x520 && computer.processor.edx.getValue()==0x174 && computer.processor.eax.getValue()==0x111101ff)
			computer.processor.eax.setValue(0x11110114);
		if (icount<3000000 && computer.processor.eip.getValue()==0x520 && computer.processor.edx.getValue()==0x175 && computer.processor.eax.getValue()==0x111101ff)
			computer.processor.eax.setValue(0x111101eb);
*/		
		switch(icount)
		{
		//real mode changes - mostly devices
		case 317797: computer.processor.eax.setLower8Value(6); break;
		case 317860: computer.processor.eax.setValue(0xf002); break;
		case 317928: computer.processor.eax.setValue(0x51); break;
		case 317946: computer.processor.eax.setValue(0x34); break;
		case 317964: computer.processor.eax.setValue(0x14); break;
		case 318039: computer.processor.eax.setValue(0x11111234); break;
		case 427261: computer.processor.eax.setValue(0x11119200); break;
		case 447086: computer.processor.eax.setValue(0x11110309); break;
		case 466536: computer.processor.eax.setValue(0x11110367); break;
		case 594973: computer.processor.eax.setValue(0x11119200); break;
		case 678123: computer.processor.eax.setValue(0x11119f00); break;
		case 678130: computer.processor.eax.setValue(0x11119f00); break;

		
		//on a lodsb to memory e0010 (virtual c0e0010), jpc reports FF
		//this apparently happened while jpc was in real mode
		case 185785984:
			for (int addr=0xe0000; addr<=0xeffff; addr++)
				computer.physicalMemory.setByte(addr, (byte)0xff);
			break;
		}
	}
}
