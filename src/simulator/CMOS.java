/*
CMOS.java
Michael Black, 6/10

Simulates the CMOS and Real Time Clock
*/
package simulator;

import java.util.Scanner;

public class CMOS extends IODevice
{
	private byte[] cmosdata;
	private byte cmosindex=0;

	private Computer computer;
	
	public void loadState(String state)
	{
		Scanner loader=new Scanner(state);
		for (int i=0; i<cmosdata.length; i++)
			cmosdata[i]=loader.nextByte();
		cmosindex=loader.nextByte();
	}
	public String saveState()
	{
		String state="";
		for (int i=0; i<cmosdata.length; i++)
			state+=cmosdata[i]+" ";
		state+=cmosindex;
		return state;
	}
	
	public CMOS(Computer computer)
	{
		this.computer=computer;
		Processor cpu=computer.processor;
		
		cmosdata=new byte[128];

		//set base memory size to 640k (IBM-PC max)
		cmosdata[0x15] = (byte)640;
		cmosdata[0x16] = (byte)(640>>8);

		//set extended memory size
		int ramsize = (PhysicalMemory.EXTENDED_RAM_SIZE);
		int n = ramsize/1024-1024;
		if (n>65535)
			n=65535;
		cmosdata[0x17] = (byte) n;
		cmosdata[0x18] = (byte) (n>>>8);
		cmosdata[0x30] = (byte) n;
		cmosdata[0x31] = (byte) (n>>>8);

		//set extended memory size above 16M
		if (ramsize <= 16*1024*1024)
			n=0;
		else
			n=(ramsize/65536)-(16*1024*1024)/65536;
		if (n>65535)
			n=65535;
		cmosdata[0x34] = (byte) n;
		cmosdata[0x35] = (byte) (n>>>8);

		//boot from a floppy disk
		//(if we wanted to boot from hard disk, then 2.  cd, 3)
		if (computer.bootgui.bootFromFloppy)
			cmosdata[0x3d] = (byte) 0x1;
		else
		{
			if (computer.bootgui.isCD[2])
				cmosdata[0x3d]=(byte)3;
			else
				cmosdata[0x3d] = (byte) 0x2;
		}
		
		//detect floppy drives
		cmosdata[0x10] = (byte) 0x0;

		cmosdata[0x10] = (byte) 0x00;
		cmosdata[0x14] = (byte) 0x00;
		if(computer.floppy!=null && computer.floppy.drives[0]!=null && computer.floppy.getDriveType(0)==Floppy.DriveType.DRIVE_144)
		{
			cmosdata[0x14] |= (byte)0x1;
			cmosdata[0x10] |= (byte)0x40;
		}
		if(computer.floppy!=null && computer.floppy.drives[0]!=null && computer.floppy.getDriveType(0)==Floppy.DriveType.DRIVE_120)
		{
			cmosdata[0x14] |= (byte)0x1;
			cmosdata[0x10] |= (byte)0x20;
		}
		if(computer.floppy!=null && computer.floppy.drives[1]!=null && computer.floppy.getDriveType(1)==Floppy.DriveType.DRIVE_144)
		{
			cmosdata[0x14] |= (byte)0x40;
			cmosdata[0x10] |= (byte)0x4;
		}
		if(computer.floppy!=null && computer.floppy.drives[1]!=null && computer.floppy.getDriveType(1)==Floppy.DriveType.DRIVE_120)
		{
			cmosdata[0x14] |= (byte)0x40;
			cmosdata[0x10] |= (byte)0x2;
		}

		//detect hard drive
		if (computer.harddrive==null)
			cmosdata[0x12]=0;
		else
		{
			cmosdata[0x12]=(byte)0xf0;
			cmosdata[0x19]=(byte)47;
			cmosdata[0x1a]=(byte)47;
			if (computer.harddrive.drive[0]!=null)
			{
				cmosdata[0x1b]=(byte)computer.harddrive.drive[0].getCylinders();
				cmosdata[0x1c]=(byte)(computer.harddrive.drive[0].getCylinders()>>>8);
				cmosdata[0x1d]=(byte)computer.harddrive.drive[0].getHeads();
				cmosdata[0x1e]=(byte)0xff;
				cmosdata[0x1f]=(byte)0xff;
				cmosdata[0x20]=(byte)(0xc0|((computer.harddrive.drive[0].getHeads()>8)?0x8:0));
				cmosdata[0x21]=(byte)computer.harddrive.drive[0].getCylinders();
				cmosdata[0x22]=(byte)(computer.harddrive.drive[0].getCylinders()>>>8);
				cmosdata[0x23]=(byte)computer.harddrive.drive[0].getSectors();
			}
			if (computer.harddrive.drive[1]!=null)
			{
				cmosdata[0x24]=(byte)computer.harddrive.drive[1].getCylinders();
				cmosdata[0x25]=(byte)(computer.harddrive.drive[1].getCylinders()>>>8);
				cmosdata[0x26]=(byte)computer.harddrive.drive[1].getHeads();
				cmosdata[0x27]=(byte)0xff;
				cmosdata[0x28]=(byte)0xff;
				cmosdata[0x29]=(byte)(0xc0|((computer.harddrive.drive[1].getHeads()>8)?0x8:0));
				cmosdata[0x2a]=(byte)computer.harddrive.drive[1].getCylinders();
				cmosdata[0x2b]=(byte)(computer.harddrive.drive[1].getCylinders()>>>8);
				cmosdata[0x2c]=(byte)computer.harddrive.drive[1].getSectors();
			}
		}
		computer.ioports.requestPorts(this,new int[]{0x70,0x71},"CMOS",new String[]{"Select","Data"});
	}

	public byte ioPortReadByte(int address)
	{
		//can only read from port 0x71
		if (address == 0x70)
			return (byte)0xff;
		return cmosdata[cmosindex];
	}
	public void ioPortWriteByte(int address, byte data)
	{
		//writes to 0x70 set which CMOS entry 0x71 reads from
		if (address == 0x70)
		{
			cmosindex = (byte) (data & 0x7f);
		}
		//writes to 0x71 set the CMOS
		else
		{
			cmosdata[cmosindex] = data;
		}
	}
}
