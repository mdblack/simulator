/*
InterruptController.java
Michael Black, 6/10

This file is mostly taken from JPC (InterruptController.java, author Chris Dennis)
    JPC: A x86 PC Hardware Emulator for a pure Java Virtual Machine
    Release Version 2.0
    A project from the Physics Dept, The University of Oxford
    www-jpc.physics.ox.ac.uk

Simulates an 8259 interrupt controller
*/

package simulator;

import java.util.Scanner;

public class InterruptController extends IODevice
{
    private InterruptControllerElement master;
    private InterruptControllerElement slave;

    private Computer computer;

    /**
     * Constructs a <code>InterruptController</code> which will attach itself to
     * a <code>Processor</code> instance during the configuration stage.
     */
    public InterruptController(Computer computer)
    {
    	this.computer=computer;
    	master = new InterruptControllerElement(true);
    	slave = new InterruptControllerElement(false);
    	computer.ioports.requestPorts(this,ioPortsRequested(),"Interrupt");
    }

    public String saveState()
    {
    	return "InterruptController:"+master.saveState()+":"+slave.saveState();
    }
    
    public void loadState(String state)
    {
    	String[] states=state.split(":");
		if (!states[0].equals("InterruptController"))
		{
			System.out.println("Error in load state: InterruptController expected");
			return;
		}    	
    	master.loadState(states[1]);
    	slave.loadState(states[2]);
    }
    
    private void updateIRQ()
    {
	int slaveIRQ, masterIRQ;
	/* first look at slave irq */
	slaveIRQ = slave.getIRQ();
	if (slaveIRQ >= 0) {
	    /* if irq request by slave pic, signal Master PIC */
	    master.setIRQ(2,1);
	    master.setIRQ(2,0);
	}
	/* look at requested IRQ */
	masterIRQ = master.getIRQ();
	if(masterIRQ >= 0) {
	    computer.processor.raiseInterrupt();
	    if (computer.customProcessor!=null)
	    	computer.customProcessor.raiseInterrupt();
	    if (computer.ioGUI!=null)
	    	computer.ioGUI.interruptTriggered=true;
	}
    }

    /**
     * Set interrupt number <code>irqNumber</code> to level <code>level</code>.
     * @param irqNumber interrupt channel number.
     * @param level requested level.
     */
    public void setIRQ(int irqNumber, int level)
    {
	switch (irqNumber >>> 3) {
	case 0: //master
	    master.setIRQ(irqNumber & 7, level);
	    this.updateIRQ();
	    break;
	case 1: //slave
	    slave.setIRQ(irqNumber & 7, level);
	    this.updateIRQ();
	    break;
	default:
	}
    }

    /**
     * Return the highest priority interrupt request currently awaiting service
     * on this interrupt controller.  This is called by the processor emulation
     * once its <code>raiseInterrupt</code> method has been called to get the
     * correct interrupt vector value.
     * @return highest priority interrupt vector.
     */
    public int cpuGetInterrupt()
    {
	int masterIRQ, slaveIRQ;

	/* read the irq from the PIC */

	masterIRQ = master.getIRQ();
	if (masterIRQ >= 0) {
	    master.intAck(masterIRQ);
	    if (masterIRQ == 2) {
		slaveIRQ = slave.getIRQ();
		if (slaveIRQ >= 0) {
		    slave.intAck(slaveIRQ);
		} else {
		    /* spurious IRQ on slave controller */
		    slaveIRQ = 7;
		}
		this.updateIRQ();
		return slave.irqBase + slaveIRQ;
		//masterIRQ = slaveIRQ + 8;
	    } else {
		this.updateIRQ();
		return master.irqBase + masterIRQ;
	    }
	} else {
	    /* spurious IRQ on host controller */
	    masterIRQ = 7;
	    this.updateIRQ();
	    return master.irqBase + masterIRQ;
	}
    }

    private class InterruptControllerElement
    {
	private int lastInterruptRequestRegister; //edge detection
	private int interruptRequestRegister;
	private int interruptMaskRegister;
	private int interruptServiceRegister;

	private int priorityAdd; // highest IRQ priority
	private int irqBase;
	private boolean readRegisterSelect;
	private boolean poll;
	private boolean specialMask;
	private int initState;
	private boolean fourByteInit;
	private int elcr; //(elcr) PIIX3 edge/level trigger selection
	private int elcrMask;

	private boolean specialFullyNestedMode;

	private boolean autoEOI;
	private boolean rotateOnAutoEOI;

	private int[] ioPorts;
	
	public String saveState()
	{
		String state="";
		state+=lastInterruptRequestRegister+" ";
		state+=interruptRequestRegister+" ";
		state+=interruptMaskRegister+" ";
		state+=interruptServiceRegister+" ";
		state+=priorityAdd+" ";
		state+=irqBase+" ";
		state+=initState+" ";
		state+=elcr+" ";
		state+=elcrMask+" ";
		state+=(readRegisterSelect?1:0)+" ";
		state+=(poll?1:0)+" ";
		state+=(fourByteInit?1:0)+" ";
		state+=(specialFullyNestedMode?1:0)+" ";
		state+=(autoEOI?1:0)+" ";
		state+=(rotateOnAutoEOI?1:0);
		return state;
	}
	
	public void loadState(String state)
	{
		Scanner s=new Scanner(state);
		lastInterruptRequestRegister=s.nextInt();
		interruptRequestRegister=s.nextInt();
		interruptMaskRegister=s.nextInt();
		interruptServiceRegister=s.nextInt();
		priorityAdd=s.nextInt();
		irqBase=s.nextInt();
		initState=s.nextInt();
		elcr=s.nextInt();
		elcrMask=s.nextInt();
		readRegisterSelect=s.nextInt()==1;
		poll=s.nextInt()==1;
		fourByteInit=s.nextInt()==1;
		specialFullyNestedMode=s.nextInt()==1;
		autoEOI=s.nextInt()==1;
		rotateOnAutoEOI=s.nextInt()==1;
	}

	public InterruptControllerElement(boolean master)
	{
	    if (master == true) {
		ioPorts = new int[]{0x20, 0x21, 0x4d0};
		elcrMask = 0xf8;
	    } else {
		ioPorts = new int[]{0xa0, 0xa1, 0x4d1};
		elcrMask = 0xde;
	    }
	    reset();
	}

	private void reset()
	{
	    //zero all variables except elcrMask
	    lastInterruptRequestRegister = 0x0;
	    interruptRequestRegister = 0x0;
	    interruptMaskRegister = 0x0;
	    interruptServiceRegister = 0x0;
	    
	    priorityAdd = 0;
	    irqBase = 0x0;
	    readRegisterSelect = false;
	    poll = false;
	    specialMask = false;
	    autoEOI = false;
	    rotateOnAutoEOI = false;

	    specialFullyNestedMode = false;

	    initState = 0;
	    fourByteInit = false;

	    elcr = 0x0; //(elcr) PIIX3 edge/level trigger selection
	}

	public int[] ioPortsRequested()
	{
	    return ioPorts;
	}

	public int ioPortRead(int address)
	{
	    if(poll) {
		poll = false;
		return this.pollRead(address);
	    }
	    
	    if ((address & 1) == 0) {
		if (readRegisterSelect) {
		    return interruptServiceRegister;
		}

		return interruptRequestRegister;
	    }

	    return interruptMaskRegister;
	}

	public int elcrRead()
	{
	    return elcr;
	}

	public boolean ioPortWrite(int address, byte data) //t/f updateIRQ
	{
	    int priority, command, irq;
	    address &= 1;
	    if (address == 0) {
		if (0 != (data & 0x10)) 
                {
		    /* init */
		    this.reset();
		    computer.processor.clearInterrupt();
		    if (computer.customProcessor!=null)
		    	computer.customProcessor.clearInterrupt();

		    initState = 1;
		    fourByteInit = ((data & 1) != 0);
		} 
                else if (0 != (data & 0x08)) 
                {
		    if (0 != (data & 0x04))
			poll = true;
		    if (0 != (data & 0x02))
			readRegisterSelect = ((data & 0x01) != 0);
		    if (0 != (data & 0x40))
			specialMask = (((data >>> 5) & 1) != 0);
		} 
                else 
                {
		    command = data >>> 5;
		    switch(command) {
		    case 0:
		    case 4:
			rotateOnAutoEOI = ((command >>> 2) != 0);
			break;
		    case 1: // end of interrupt
		    case 5:
			priority = this.getPriority(interruptServiceRegister);
			if (priority != 8) {
			    irq = (priority + priorityAdd) & 7;
			    interruptServiceRegister &= ~(1 << irq);
			    if (command == 5)
				priorityAdd = (irq + 1) & 7;
			    return true;
			}
			break;
		    case 3:
			irq = data & 7;
			interruptServiceRegister &= ~(1 << irq);
			return true;
		    case 6:
			priorityAdd = (data + 1) & 7;
			return true;
		    case 7:
			irq = data & 7;
			interruptServiceRegister &= ~(1 << irq);
			priorityAdd = (irq + 1) & 7;
			return true;
		    default:
			/* no operation */
			break;
		    }
		}
	    } 
            else 
            {
		switch(initState) 
                {
		case 0:
		    /* normal mode */
		    interruptMaskRegister = data;
		    return true;
		case 1:
		    irqBase = data & 0xf8;
		    initState = 2;
		    break;
		case 2:
		    if (fourByteInit) {
			initState = 3;
		    } else {
			initState = 0;
		    }
		    break;
		case 3:
		    specialFullyNestedMode = (((data >>> 4) & 1) != 0);
		    autoEOI = (((data >>> 1) & 1) != 0);
		    initState = 0;
		    break;
		}
	    }
	    return false;
	}

	public void elcrWrite(int data)
	{
	    elcr = data & elcrMask;
	}

	private int pollRead(int address)
	{
	    int ret = this.getIRQ();
	    if (ret < 0) {
		InterruptController.this.updateIRQ();
		return 0x07;
	    }
	    
	    if (0 != (address >>> 7)) {
		InterruptController.this.masterPollCode();
	    }
	    interruptRequestRegister &= ~(1 << ret);
	    interruptServiceRegister &= ~(1 << ret);
	    if (0 != (address >>> 7) || ret != 2)
		InterruptController.this.updateIRQ();
	    return ret;
	}

	public void setIRQ(int irqNumber, int level)
	{
		if (computer.ioGUI!=null)
		{
			computer.ioGUI.lastInterrupt=irqNumber;
			computer.ioGUI.interruptRequested=true;
		}
		
	    int mask;
	    mask = (1 << irqNumber);
	    if(0 != (elcr & mask)) {
		/* level triggered */
		if (0 != level) {
		    interruptRequestRegister |= mask;
		    lastInterruptRequestRegister |= mask;
		} else {
		    interruptRequestRegister &= ~mask;
		    lastInterruptRequestRegister &= ~mask;
		}
	    } else {
		/* edge triggered */
		if (0 != level) {
		    if ((lastInterruptRequestRegister & mask) == 0) {
			interruptRequestRegister |= mask;
		    }
		    lastInterruptRequestRegister |= mask;
		} else {
		    lastInterruptRequestRegister &= ~mask;
		}
	    }
	}

	private int getPriority(int mask)
	{
	    if ((0xff & mask) == 0) {
		return 8;
	    }
	    int priority = 0;
	    while ((mask & (1 << ((priority + priorityAdd) & 7))) == 0) {
		priority++;
	    }
	    return priority;
	}

	public int getIRQ()
	{
	    int mask, currentPriority, priority;
	    
	    mask = interruptRequestRegister & ~interruptMaskRegister;
	    priority = this.getPriority(mask);

	    if (priority == 8) {
		return -1;
	    }
	    /* compute current priority. If special fully nested mode on
	       the master, the IRQ coming from the slave is not taken into
	       account for the priority computation. */
	    mask = interruptServiceRegister;
	    if (specialFullyNestedMode && this.isMaster()) {
		mask &= ~(1 << 2);
	    }
	    currentPriority = this.getPriority(mask);

	    if (priority < currentPriority) {
		/* higher priority found: an irq should be generated */
		return (priority + priorityAdd) & 7;
	    } else {
		return -1;
	    }
	}

	private void intAck(int irqNumber)
	{
	    if (autoEOI) {
		if (rotateOnAutoEOI)
		    priorityAdd = (irqNumber + 1) & 7;
	    } else {
		interruptServiceRegister |= (1 << irqNumber);
	    }
	    /* We don't clear a level sensitive interrupt here */
	    if (0 == (elcr & (1 << irqNumber)))
		interruptRequestRegister &= ~(1 << irqNumber);
	}

	private boolean isMaster()
	{
            return InterruptController.this.master == this;
	}
        
	public String toString()
	{
	    if (isMaster()) {
		return (InterruptController.this).toString() + ": [Master Element]";
	    } else {
		return (InterruptController.this).toString() + ": [Slave  Element]";
	    }
	}
    }

    public int[] ioPortsRequested()
    {
	int[] masterIOPorts = master.ioPortsRequested();
	int[] slaveIOPorts = slave.ioPortsRequested();

	int[] temp = new int[masterIOPorts.length + slaveIOPorts.length];
	System.arraycopy(masterIOPorts, 0, temp, 0, masterIOPorts.length);
	System.arraycopy(slaveIOPorts, 0, temp, masterIOPorts.length, slaveIOPorts.length);

	return temp;
    }

    public byte ioPortReadByte(int address)
    {
	switch (address) {
	case 0x20:
	case 0x21:
	    return (byte)(0xff & master.ioPortRead(address));
	case 0xa0:
	case 0xa1:
	    return (byte)(0xff & slave.ioPortRead(address));
	case 0x4d0:
	    return (byte)(0xff & master.elcrRead());
	case 0x4d1:
	    return (byte)(0xff & slave.elcrRead());
	default:
	}
	return (byte)0;
    }
    public void ioPortWriteByte(int address, byte data)
    {
	switch (address) {
	case 0x20:
	case 0x21:
	    if (master.ioPortWrite(address, (byte)data))
		this.updateIRQ();
	    break;
	case 0xa0:
	case 0xa1:
	    if (slave.ioPortWrite(address, (byte)data))
		this.updateIRQ();
	    break;
	case 0x4d0:
	    master.elcrWrite(data);
	    break;
	case 0x4d1:
	    slave.elcrWrite(data);
	    break;
	default:
	}
    }

    private void masterPollCode()
    {
	master.interruptServiceRegister &= ~(1 << 2);
	master.interruptRequestRegister &= ~(1 << 2);
    }

}

