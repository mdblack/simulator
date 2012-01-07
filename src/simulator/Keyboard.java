package simulator;

import java.util.Scanner;

public class Keyboard extends IODevice
{
	public static final int QUEUE_SIZE=256;

	public KeyboardBuffer buffer;
	public int mode;
	public byte status;
	private byte commandWrite;

	public int keyboardWriteCommand=0;	//this is for the gui
	private Computer computer;

	public Keyboard(Computer computer)
	{
		this.computer=computer;
		mode=0x01;	//enable keyboard interrupts
		status=(byte)0x18;	//keyboard is unlocked and last command was a write
		buffer=new KeyboardBuffer();
		computer.ioports.requestPorts(this, new int[]{0x60,0x64},"Keyboard",new String[]{"Data","Command"});
	}
	
	public String saveState()
	{
		String state="";
		state+="Keyboard ";
		state+=mode+" ";
		state+=status+" ";
		state+=commandWrite+" ";
		state+=keyboardWriteCommand+" ";

		for (int i=0; i<buffer.data.length; i++)
			state+=buffer.data[i]+" ";
		state+=buffer.readPosition+" ";
		state+=buffer.writePosition+" ";
		state+=buffer.length+" ";
		state+=(buffer.mutex?1:0);
		return state;
	}
	
	public void loadState(String state)
	{
		Scanner s=new Scanner(state);
		if (!s.next().equals("Keyboard"))
		{
			System.out.println("Error in load state: Keyboard expected");
			return;
		}

		mode=s.nextInt();
		status=s.nextByte();
		commandWrite=s.nextByte();
		keyboardWriteCommand=s.nextInt();
		for (int i=0; i<buffer.data.length; i++)
			buffer.data[i]=s.nextByte();
		buffer.readPosition=s.nextInt();
		buffer.writePosition=s.nextInt();
		buffer.length=s.nextInt();
		buffer.mutex=s.nextInt()==1;
	}

	public byte ioPortReadByte(int address)
	{
		//data port
		if (address==0x60)
			return readData();
		//status port
		else
			return (byte)(0xff & status);
	}
	public void ioPortWriteByte(int address, byte data)
	{
		//data write
		if (address==0x60)
			writeData(data);
		//keyboard control command
		else
			writeCommand(data);
	}
	private byte readData()
	{
		byte data=buffer.readData();
		updateIRQ();
		return data;
	}
	private void writeData(byte data)
	{
		switch(commandWrite)
		{
			case 0:
				writeKeyboard(data);
				break;
			case (byte)0x60:	//write mode
				mode=0xff&data;
				updateIRQ();
				break;
			case (byte)0xd2:	//write to buffer
				buffer.writeData(data);
				break;
		}
		commandWrite=(byte)0;
	}
	private void writeCommand(byte data)
	{
		switch(data)
		{
			case (byte)0x20:	//read mode
				buffer.writeData((byte)mode);
				break;
			case (byte)0x60: case (byte)0xd1: case (byte)0xd2:	//writes
				commandWrite=data;
				break;
			case (byte)0xaa:	//self test
				//say that self test is successful
				status=(byte)(status|0x04);	
				buffer.writeData((byte)0x55);
				break;
			case (byte)0xab:	//keyboard test
				buffer.writeData((byte)0);
				break;
			case (byte)0xad:	//disable keyboard
				mode |= 0x10;
				updateIRQ();
				break;
			case (byte)0xae:	//enable keyboard
				mode &= ~0x10;
				updateIRQ();
				break;
			case (byte)0xc0:	//read input port
				buffer.writeData((byte)0);
				break;
			case (byte)0xd0:	//read output port
				data=(byte)1;
				//is the output buffer full?
				if ((status&0x01)!=0)
					data|=0x10;
				buffer.writeData(data);
				break;
			case (byte)0xfe:	//reset machine
				computer.processor.reset();
				break;
		}
	}
	private void writeKeyboard(byte data)
	{
		switch(data)
		{
			case 0:
				//ack
				buffer.writeData((byte)0xfa);
				break;
			case (byte)5:
				//nack - resend
				buffer.writeData((byte)0xfe);
				break;
			case (byte)0xf2:	//get id
				buffer.writeData((byte)0xfa);	//ack
				buffer.writeData((byte)0xab);
				buffer.writeData((byte)0x83);
				break;
			case (byte)0xee:	//echo
				buffer.writeData((byte)0xee);
				break;
			case (byte)0xed: case (byte)0xf3:	//set the LEDs or the rate
				keyboardWriteCommand=data;
				buffer.writeData((byte)0xfa);	//ack
				break;
			case (byte)0xff:
				buffer.writeData((byte)0xfa);	//ack
				buffer.writeData((byte)0xaa);	//power on reset
				break;
			default:
				buffer.writeData((byte)0xfa);	//ack
				break;
		}
	}
	private void updateIRQ()
	{
		int level=0;
		status=(byte)(status&~((byte)0x01));	//set buffer to not full
		buffer.downMutex();
		if (buffer.length!=0)		//chars waiting
		{
			status=(byte)(status | 0x01);	//set to full
			//check if interrupts enabled and isn't disabled
//System.out.println("Keyboard mode is "+mode);
			if ((mode&0x01)!=0 && (mode&0x10)==0)
				level=1;
		}
		buffer.upMutex();
		computer.interruptController.setIRQ(1,level);
	}
	public class KeyboardBuffer
	{
		public byte[] data;
		public int readPosition;
		public int writePosition;
		public int length;
		public boolean mutex;

		public KeyboardBuffer()
		{
			data=new byte[QUEUE_SIZE];
			readPosition=writePosition=length=0;
			mutex=false;
		}
		public byte readData()
		{
			//if the queue is empty, return the last key pressed
			if (length==0)
			{
				if(readPosition==0)
					return data[QUEUE_SIZE-1];
				else
					return data[readPosition-1];
			}
			downMutex();
			byte value=data[readPosition++];
			if (readPosition==QUEUE_SIZE)
				readPosition=0;
			length--;
			computer.interruptController.setIRQ(1,0);
			if (computer.keyboardGUI!=null)
				computer.keyboardGUI.keyboardbuffercomponent.update();
			upMutex();
			return value;
		}
		public void writeData(byte data)
		{
			downMutex();
			this.data[writePosition++]=data;
			if (writePosition==QUEUE_SIZE)
				writePosition=0;
			length++;
			if (computer.keyboardGUI!=null)
				computer.keyboardGUI.keyboardbuffercomponent.update();
			upMutex();
			updateIRQ();
		}
		public synchronized void downMutex()
		{
			while(mutex) { try { Thread.sleep(1); } catch(Exception e) { } }
			mutex=true;
		}
		public void upMutex()
		{
			mutex=false;
		}
	}
	//called externally from the UI
	public void keyPressed(byte scancode)
	{
		buffer.writeData((byte)(scancode&0x7f));
	}
	public void keyReleased(byte scancode)
	{
		buffer.writeData((byte)(scancode|0x80));
	}
}
