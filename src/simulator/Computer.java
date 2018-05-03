/*
Computer.java
Michael Black, 6/10


Computer builds the PC, starts it running
*/

package simulator;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.beans.PropertyVetoException;
import java.io.*;
import java.util.Scanner;
import javax.swing.*;

public class Computer {
    private static final int NICE_INTERVAL = 1000;  //pause for 1ms every n cycles so as not to overload the computer

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
    public ProcessorBuilder processorBuilder;
    public IOPorts ioports;
    public Trace trace;

    public boolean keyReady = false;
    public boolean oneScreen = false;
    public boolean updateGUIOnPlay = false;

    public int icount = 0;
    public boolean boot;
    public boolean debugMode = false;
    public boolean slowMode = false;

    public BootGUI bootgui;

    public Lock stepLock;
    public Lock cycleEndLock;

    public Computer computer;
    public ComputerApplet applet = null;

    public String args = "";
    public Resolution resolution;

    public String saveState() {
        System.out.println("saving state");
        String state = "";
        state += "Computer@" + icount + "@";
        state += "Settings@" + bootgui.saveState() + "@";
        state += "Processor@" + processor.saveState() + "@";
        state += "PhysicalMemory@" + physicalMemory.saveState() + "@";
        state += "LinearMemory@" + linearMemory.saveState() + "@";
        state += "InterruptController@" + interruptController.saveState() + "@";
        state += "Keyboard@" + keyboard.saveState() + "@";
        state += "CMOS@" + cmos.saveState() + "@";
        state += "HardDrive@" + harddrive.saveState() + "@";
        state += "Clock@" + clock.saveState() + "@";
        state += "Timer@" + timer.saveState() + "@";
        state += "Video@" + video.saveState() + "@";
        System.out.println("state saved");
        return state;
    }

    private int indexOf(String[] list, String key) {
        for (int i = 0; i < list.length; i++)
            if (list[i].equals(key)) return i;
        return -1;
    }

    private void loaderror(String deviceName) {
        System.out.println("No " + deviceName + " found in state file.  Using default device setting.");

    }

    public void loadState(String state) {
        int s = 0;
        System.out.println("loading state");
        String[] states = state.split("@");
        if (indexOf(states, "Settings") >= 0) bootgui.loadState(states[indexOf(states, "Settings") + 1]);
        else loaderror("Settings");
        physicalMemory = new PhysicalMemory(this);
        if (indexOf(states, "PhysicalMemory") >= 0)
            physicalMemory.loadState(states[indexOf(states, "PhysicalMemory") + 1]);
        else loaderror("PhysicalMemory");
        linearMemory = new LinearMemory(this);
        if (indexOf(states, "LinearMemory") >= 0) linearMemory.loadState(states[indexOf(states, "LinearMemory") + 1]);
        else loaderror("LinearMemory");
        ioports = new IOPorts(this);
        processor = new Processor(this);
        if (indexOf(states, "Processor") >= 0) processor.loadState(states[indexOf(states, "Processor") + 1]);
        else loaderror("Processor");
        interruptController = new InterruptController(this);
        if (indexOf(states, "InterruptController") >= 0)
            interruptController.loadState(states[indexOf(states, "InterruptController") + 1]);
        else loaderror("InterruptController");
        keyboard = new Keyboard(this);
        if (indexOf(states, "Keyboard") >= 0) keyboard.loadState(states[indexOf(states, "Keyboard") + 1]);
        else loaderror("Keyboard");
        dma1 = new DMA(this, false, true);
        dma2 = new DMA(this, false, false);
        floppy = new Floppy(this);
        harddrive = new HardDrive(this);
        if (indexOf(states, "HardDrive") >= 0) harddrive.loadState(states[indexOf(states, "HardDrive") + 1]);
        else loaderror("HardDrive");
        cmos = new CMOS(this);
        if (indexOf(states, "CMOS") >= 0) cmos.loadState(states[indexOf(states, "CMOS") + 1]);
        else loaderror("CMOS");
        clock = new Clock();
        if (indexOf(states, "Clock") >= 0) clock.loadState(states[indexOf(states, "Clock") + 1]);
        else loaderror("Clock");
        timer = new Timer(this);
        if (indexOf(states, "Timer") >= 0) timer.loadState(states[indexOf(states, "Timer") + 1]);
        else loaderror("Timer");
        serialport = new SerialPort(this);
        video = new Video(this);
        if (indexOf(states, "Video") >= 0) video.loadState(states[indexOf(states, "Video") + 1]);
        else loaderror("Video");

        if (indexOf(states, "Computer") >= 0) {
            Scanner loader = new Scanner(states[indexOf(states, "Computer") + 1]);
            icount = loader.nextInt();
        } else loaderror("Computer");

        if (videoGUI != null)
            videoGUI.repaint();
        if (keyboardGUI != null)
            keyboardGUI.repaint();

        System.out.println("state loaded");
    }

    public Computer(ComputerApplet applet) {
        this.applet = applet;
//		oneScreen=true;

        startComputer();
    }

    public Computer() {
        this.args = "";
        startComputer();
    }

    public Computer(String args) {
        this.args = args;
        startComputer();
    }

    public void startComputer() {
        computer = this;
        initialize();
        initializeGUIs();
        new Thread(new Runnable() {
            public void run() {
                mainLoop();
            }
        }).start();
    }

    public void initialize() {
        stepLock = new Lock();
        cycleEndLock = new Lock();
        resolution = new Resolution();

        computerGUI = new ComputerGUI(computer);

        bootgui = new BootGUI(computer, new String[]{"Processor", "Memory", "BIOS ROM", "VGA ROM", "Registers", "I/O Ports", "Video", "Keyboard", "Floppy Controller", "Interrupt Controller", "IDE Controller", "CMOS", "Timer", "DMA Controller", "Serial Port"});

//		if (args.equals(""))
//			stepLock.lockWait();
        if (args.indexOf(".img") != -1) {
            computer.computerGUI.menubar.setVisible(true);
            bootgui.setVisible(false);
            bootgui.bootFromFloppy = true;
            bootgui.diskField[0].setText(args);
            bootgui.bootImageName = bootgui.diskField[0].getText();
            computer.computerGUI.removeComponent(bootgui);
            bootgui.updateCheckBoxes();
        } else if (args.indexOf(".xml") != -1) {
            bootgui.bootFromFloppy = false;
            bootgui.bootImageName = "";
            computer.computerGUI.menubar.setVisible(true);
            bootgui.setVisible(false);
            computer.computerGUI.removeComponent(bootgui);
            bootgui.singlestepbox.setSelected(true);
            bootgui.datapathField.setText(args);
            bootgui.bootCustomProcessor = true;
            bootgui.updateCheckBoxes();
        } else
            stepLock.lockWait();
    }

    public void mainLoop() {
        while (true) {
//			if (customProcessor!=null && customProcessor.defaultModule.active)
//				customProcessor.defaultModule.doCycle();
            if (customProcessor != null)
                customProcessor.doCycle();
            else
                cycle();
            if (debugMode)
                stepLock.lockWait();
            if (applet != null && applet.suspended) {
                System.out.println("suspending applet");
                applet.appletLock.lockWait();
            }
        }
    }

    public void initializeGUIs() {
        clock = new Clock();

        physicalMemory = new PhysicalMemory(computer);
        linearMemory = new LinearMemory(computer);
        try {
            physicalMemory.loadBIOS(computer.getClass().getClassLoader().getResource(bootgui.romImage), 0xf0000, 0xfffff);
            physicalMemory.loadBIOS(computer.getClass().getClassLoader().getResource(bootgui.vromImage), 0xc0000, 0xcffff);
        } catch (IOException e) {
            System.out.println("Error loading BIOS image: " + e);
            JOptionPane.showMessageDialog(null, "Cannot load the BIOS image");
        } catch (NullPointerException e) {
            System.out.println("Error loading BIOS image: " + e);
            JOptionPane.showMessageDialog(null, "Cannot load the BIOS image");
        } catch (Exception e) {
            e.printStackTrace();
        }
        ioports = new IOPorts(this);
        processor = new Processor(this);
        interruptController = new InterruptController(this);
        keyboard = new Keyboard(this);
        dma1 = new DMA(this, false, true);
        dma2 = new DMA(this, false, false);
        floppy = new Floppy(this);
        harddrive = new HardDrive(this);
        cmos = new CMOS(this);
        timer = new Timer(this);
        serialport = new SerialPort(this);
        video = new Video(this);

		if (!bootgui.memoryImage.equals(""))
		{
			try {
				MemoryTransferGUI.load(bootgui.memoryImage, bootgui.memoryImageStart, this);
			} catch (NullPointerException e) {
				final JPanel panel = new JPanel();

				JOptionPane.showMessageDialog(panel, "Memory could not be loaded. Check your file path: " +
								bootgui.memoryImage, "Warning",
						JOptionPane.WARNING_MESSAGE);
			}
		}
		
		if (!bootgui.datapathxml.equals(""))
		{
			datapathBuilder=new DatapathBuilder(this);
			datapathBuilder.doload(bootgui.datapathxml, datapathBuilder.defaultModule);
			if (!bootgui.controlxml.equals(""))
			{
				controlBuilder=new ControlBuilder(this,datapathBuilder.defaultModule);
				controlBuilder.clear();
				ControlBuilder.doload(bootgui.controlxml,controlBuilder.defaultControl);
				if (bootgui.bootCustomProcessor)
				{
					customProcessor=new CustomProcessor(this);
				}
			}
		}
		if (!bootgui.bootCustomProcessor)
		{
			memoryGUI=new MemoryGUI(this);
			keyboardGUI=new KeyboardGUI(this);
			videoGUI=new VideoGUI(this);
			ioGUI=new IOGUI(this);
			try { memoryGUI.setIcon(true); } catch (PropertyVetoException e) {}
			try { keyboardGUI.setIcon(true); } catch (PropertyVetoException e) {}
			try { ioGUI.setIcon(true); } catch (PropertyVetoException e) {}
			keyboardGUI.setVisible(false);
			ioGUI.setVisible(false);

        } else {
            if (datapathBuilder == null)
                datapathBuilder = new DatapathBuilder(this);
        }
    }

    public void cycle() {
        //advance timers
        clock.cycle();

        //handle keyboard
        //this only pertains the the terminal keyboard.  the gui calls keyboard directly
        if (trace != null)
            trace.newTraceEntry(icount);
        if (debugMode)
            System.out.printf("Cycle count %x:\n", icount);
        //execute an instruction
        processor.executeAnInstruction();


        //handle incoming interrupts
        processor.processInterrupts();

        if (registerGUI != null)
            registerGUI.readRegisters();

        if (trace != null) {
            trace.addRegisters(processor);
            trace.closeTraceEntry();
        }



/*		if (registerGUI!=null)
		{
			if (debugMode || updateGUIOnPlay)
				processor.printRegisters();
		}
*/

        icount++;

        computerGUI.instructionCount();

        if (breakpointGUI != null) {
            if (breakpointGUI.atBreakpoint()) {
                computerGUI.pause();
                computerGUI.statusfield.setText("BREAKPOINT REACHED: " + icount + " instructions");
                if (registerGUI != null)
                    registerGUI.readRegisters();
                if (!breakpointGUI.useropened)
                    breakpointGUI.close();
            }
        }

        if (computer.icount % NICE_INTERVAL == 0 || computer.updateGUIOnPlay) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
            }
        }

        cycleEndLock.lockResume();

    }

    public static void main(String[] args) throws IOException {
        if (args.length == 0)
            new Computer();
        else
            new Computer(args[0]);
    }

    public static class Lock {
        public void lockWait() {
            synchronized (this) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                }
            }
        }

        public void lockResume() {
            synchronized (this) {
                this.notify();
            }
        }
    }
}
