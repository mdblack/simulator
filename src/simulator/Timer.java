/*
Timer.java
Michael Black, 6/10

This file is inspired by JPC (IntervalTimer.java, author Chris Dennis)
    JPC: A x86 PC Hardware Emulator for a pure Java Virtual Machine
    Release Version 2.0
    A project from the Physics Dept, The University of Oxford
    www-jpc.physics.ox.ac.uk

Simulates an 8254 timer chip
*/

package simulator;

public class Timer extends IODevice
{
	public Channel[] channels;
	private InterruptController irqDevice;
	private Clock clock;
	private Computer computer;

	public Timer(Computer computer, int irq, int ioPortBase)
	{
		this.computer=computer;
		this.clock=computer.clock;
		this.irqDevice=computer.interruptController;

		channels=new Channel[3];
		for(int i=0; i<channels.length; i++)
			channels[i]=new Channel(i);
		channels[0].setIRQ(irq);

		computer.ioports.requestPorts(this,new int[]{ioPortBase, ioPortBase+1, ioPortBase+2, ioPortBase+3},"Timer",new String[]{"Channel 0","Channel 1","Channel 2","Control"});

	}

	public byte ioPortReadByte(int address)
	{
		if ((address&0x3)<3)
		{
			int retval=channels[address&0x3].read();
//			System.out.printf("Reading %x from timer at address %x\n",retval,address);
//			return (byte)channels[address&0x3].read();
			return (byte)retval;
		}
		else
			return (byte)0xff;
	}
	public void ioPortWriteByte(int address, byte data)
	{
		address=address&3;

//		System.out.printf("Writing %x to timer at address %x\n",data,address);


		if (address<3)
			//data write
			channels[address].write(data&0xff);
		else
		{
			//control write
			int channel=(data&0xff)>>>6;
			if (channel<3)
				channels[channel].writeControl(data&0xff);
			else
			{
				while ((data & (2<<channel)) !=0)
					channels[channel].readBack(data&0xff);
			}
		}
	}

	public class Channel implements Clock.ClockedDevice
	{
		private static final int RW_STATE_LSB=1;
		private static final int RW_STATE_MSB=2;
		private static final int RW_STATE_WORD=3;
		private static final int RW_STATE_WORD_2=4;
		public static final int MODE_INTERRUPT_ON_TERMINAL_COUNT=0;
		public static final int MODE_HARDWARE_RETRIGGERABLE_ONE_SHOT=1;
		public static final int MODE_RATE_GENERATOR=2;
		public static final int MODE_SQUARE_WAVE=3;
		public static final int MODE_SOFTWARE_TRIGGERED_STROBE=4;
		public static final int MODE_HARDWARE_TRIGGERED_STROBE=5;

		public int countValue, outputLatch, inputLatch, countLatched, status, readState, writeState, rwMode, mode, bcd;
		private boolean statusLatched, nullCount;

		private long countStartTime, nextTransitionTimeValue;
		private int irq=-1;
		private int id;

		private boolean enabled;
		private long expireTime;

		public Channel(int id)
		{
			this.id=id;

			mode=MODE_SQUARE_WAVE;
			loadCount(0);
			nullCount=true;
			enabled=false;
			clock.registerDevice(this);
		}

		public void setIRQ(int irq)
		{
			this.irq=irq;
		}

		public int read()
		{
			if(statusLatched)
			{
				statusLatched=false;
				return status;
			}
			if(countLatched==RW_STATE_LSB)
			{
				countLatched=0;
				return outputLatch&0xff;
			}
			if(countLatched==RW_STATE_MSB)
			{
				countLatched=0;
				return (outputLatch>>>8)&0xff;
			}
			if(countLatched==RW_STATE_WORD)
			{
				countLatched=RW_STATE_WORD_2;
				return outputLatch&0xff;
			}
			if(countLatched==RW_STATE_WORD_2)
			{
				countLatched=0;
				return (outputLatch>>>8)&0xff;
			}
			if(readState==RW_STATE_LSB)
				return getCount()&0xff;
			if(readState==RW_STATE_MSB)
				return (getCount()>>>8)&0xff;
			if(readState==RW_STATE_WORD)
			{
				readState=RW_STATE_WORD_2;
				return getCount()&0xff;
			}
			if(readState==RW_STATE_WORD_2)
			{
				readState=RW_STATE_WORD;
				return (getCount()>>>8)&0xff;
			}

			return getCount()&0xff;
		}
		public void readBack(int data)
		{
			if ((data&0x20)==0)
				latchCount();
			if ((data&0x10)==0)
				latchStatus();
		}
		private void latchCount()
		{
			if(countLatched!=0)
			{
				outputLatch=getCount();
				countLatched=rwMode;
			}
		}
		private void latchStatus()
		{
			if(!statusLatched)
			{
				status=((getOut(clock.getTime())<<7) | (nullCount? 0x40:0x00) | (rwMode << 4) | (mode << 1) | bcd);
				statusLatched=true;
			}
		}
		public void write(int data)
		{
			if (writeState==RW_STATE_MSB)
			{
				nullCount=true;
				loadCount((0xff & data)<<8);
			}
			else if (writeState==RW_STATE_WORD)
			{
				inputLatch=data;
				writeState=RW_STATE_WORD_2;
			}
			else if (writeState==RW_STATE_WORD_2)
			{
				nullCount=true;
				loadCount((0xff & inputLatch)|((0xff&data)<<8));
				writeState=RW_STATE_WORD;
			}
			else
			{
				//RW_STATE_LSB
				nullCount=true;
				loadCount(0xff&data);
			}
		}
		public void writeControl(int data)
		{
			int access = (data>>>4)&3;

			if(access==0)
				latchCount();
			else
			{
				nullCount=true;
				rwMode=access;
				readState=access;
				writeState=access;
				mode=(data>>>1)&7;
				bcd=(data&1);

				if (computer.timerGUI!=null) computer.timerGUI.updateTimer(id);
			}
		}
		private void loadCount(int value)
		{
			nullCount=false;
			if(value==0)
				value=0x10000;
			countStartTime=clock.getTime();
			countValue=value;
			irqTimerUpdate(countStartTime);

			if (computer.timerGUI!=null) computer.timerGUI.startTimer(id,countValue);
		}
		private void irqTimerUpdate(long currentTime)
		{
			if(irq==-1) return;

			long newExpireTime = getNextTransitionTime(currentTime);
			int irqLevel = getOut(currentTime);
//System.out.printf("CLOCK TICK %x %x %x %x\n",irq,irqLevel,currentTime,newExpireTime);
			if (computer.timerGUI!=null) computer.timerGUI.interrupt(id,irq,irqLevel);
			irqDevice.setIRQ(irq, irqLevel);
			nextTransitionTimeValue = newExpireTime;
			if (newExpireTime!=-1)
			{
				expireTime=newExpireTime;
				enabled=true;
			}
			else
			{
				enabled=false;
				if (computer.timerGUI!=null) computer.timerGUI.endTimer(id);
			}
		}
		private int getOut(long currentTime)
		{
			long now=currentTime-countStartTime;

			if (mode==MODE_HARDWARE_RETRIGGERABLE_ONE_SHOT)
			{
				if (now<countValue)
					return 1;
				return 0;
			}
			if (mode==MODE_RATE_GENERATOR)
			{
				if (((now % countValue)==0)&&(now!=0))
					return 1;
				return 0;
			}
			if (mode == MODE_SQUARE_WAVE)
			{
				if ((now%countValue)<((countValue+1)>>>1))
					return 1;
				return 0;
			}
			if (mode == MODE_SOFTWARE_TRIGGERED_STROBE || mode == MODE_HARDWARE_TRIGGERED_STROBE)
			{
				if (now==countValue)
					return 1;
				return 0;
			}
			//MODE_INTERRUPT_ON_TERMINAL_COUNT
			if (now>=countValue)
				return 1;
			return 0;
		}
		private int getCount()
		{
			long now=clock.getTime()-countStartTime;
			if (mode == MODE_INTERRUPT_ON_TERMINAL_COUNT || mode==MODE_HARDWARE_RETRIGGERABLE_ONE_SHOT ||
				mode==MODE_SOFTWARE_TRIGGERED_STROBE || mode==MODE_HARDWARE_TRIGGERED_STROBE)
				return (int)((countValue-now)&0xffffl);
			if (mode == MODE_SQUARE_WAVE)
				return (int)(countValue-((2*now)%countValue));
			return (int)(countValue-(now%countValue));
		}


		private long getNextTransitionTime(long currentTime)
		{
			long nextTime;
			long now=currentTime-countStartTime;
			if (mode==MODE_RATE_GENERATOR)
			{
				if (now==0)
					nextTime=currentTime+countValue;
				else if (now%countValue==0)
					nextTime=currentTime+1;
				else
					nextTime=currentTime+countValue-1;
			}
			else if (mode==MODE_SQUARE_WAVE)
			{
				if (now%countValue < ((countValue+1)>>>1))
					nextTime=currentTime+((countValue+1)>>>1);
				else
					nextTime=currentTime+countValue-((countValue+1)>>>1);
			}
			else if (mode == MODE_SOFTWARE_TRIGGERED_STROBE || mode == MODE_HARDWARE_TRIGGERED_STROBE)
			{
				if (now==0)
					nextTime=currentTime+countValue;
				else if (now==countValue)
					nextTime=currentTime+1;
				else
					return -1;
			}
			else
			{
				//MODE_INTERRUPT_ON_TERMINAL_COUNT or MODE_HARDWARE_RETRIGGERABLE_ONE_SHOT
				if (now<countValue)
					nextTime=countValue;
				else
					return -1;
			}
//System.out.printf("get next transition %x,%x,%x,%x\n",mode,now,currentTime,nextTime);
			return nextTime;
		}

		//called from Clock on each clock cycle
		public void onClockTick(long time)
		{
			if(!enabled) return;
			if (computer.timerGUI!=null) computer.timerGUI.clockTick();
			if (time>=expireTime)
			{
				enabled=false;
				irqTimerUpdate(time);
			}
		}
	}
}

