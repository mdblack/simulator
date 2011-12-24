/*
Computer.java
Michael Black, 6/10


Computer builds the PC, starts it running
*/

package simulator;
import java.io.*;
import java.awt.*;

import javax.swing.JOptionPane;

public class Computer
{
	public Processor processor;
	public PhysicalMemory physicalMemory;
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

	private byte key=0;
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
		if (bootgui.includeDevice("Timer")) timer = new Timer(this,0,0x40);
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

		if (registerGUI!=null)
		{
			if (debugMode || updateGUIOnPlay)
				processor.printRegisters();
		}

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
}
