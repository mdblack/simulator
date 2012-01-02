package simulator;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

public class MemoryBlockGUI extends AbstractGUI implements AdjustmentListener
{
	public static final int BYTEHEIGHT=14;

	public static final int ADDRESS_WIDTH=BYTEHEIGHT*4;
	public int DATA_WIDTH;
	public static final int COMMENTARY_WIDTH=BYTEHEIGHT*15;
	private static final int SCROLLWIDTH=20;

	public static final int CODE=100,STACK=101,DATA=102,TABLE=103,VIDEO=104,NOTHING=105;

	private int address,type;
	private ByteOverlay overlay;
	public int lastIP=-1,IP=-1;
	private int drawaddress=0;
	private boolean addressEditing;

	private JScrollBar scrollBar;

	public MemoryBlockGUI(Computer computer, int type, int address)
	{
		super(computer,"Memory Contents",500,300,true,false,true,false);
		this.address=address;
		this.type=type;

		if (type==DATA)
		{
			overlay=new DataByteOverlay();
			frame.setTitle("Data Memory Contents");
		}
		else if (type==STACK)
		{
			overlay=new StackByteOverlay();
			frame.setTitle("Stack Contents");
		}
		else if (type==CODE)
		{
			overlay=new CodeByteOverlay();
			frame.setTitle("Instruction Memory Contents");
		}
		else
			overlay=new DefaultByteOverlay();

		DATA_WIDTH=overlay.max_data_width()*BYTEHEIGHT*2;

		refresh();
	}

	public void constructGUI(AbstractGUI.GUIComponent guicomponent)
	{
		scrollBar=new JScrollBar(JScrollBar.VERTICAL,address,height()/BYTEHEIGHT,0,PhysicalMemory.TOTAL_RAM_SIZE-1);
		scrollBar.addAdjustmentListener(this);
		if (!computer.computerGUI.singleFrame)
			scrollBar.setBounds(width()-SCROLLWIDTH,BUTTONROWSIZE,SCROLLWIDTH,height());
		else
			scrollBar.setBounds(ComputerGUI.XSIZE-ComputerGUI.BIGWIDTH-20-SCROLLWIDTH,BUTTONROWSIZE,SCROLLWIDTH,height());
		frame.add(scrollBar);
	}

	public void update(int address, String information, int size)
	{
		address=overlay.update(this.address, address, information, size);
		if (address!=-1)
		{
			this.address=address;
			if (!computer.debugMode && !computer.updateGUIOnPlay) return;
			scrollBar.setValue(address);
			guiComponent.paintImmediately(0,0,width(),height());
		}
	}
	public void update(int address)
	{
		update(address,"",-1);
	}

	public int width()
	{
		return ADDRESS_WIDTH+DATA_WIDTH+COMMENTARY_WIDTH+SCROLLWIDTH;
	}

	public void doPaint(Graphics g)
	{
		g.setFont(new Font("Dialog",0,BYTEHEIGHT-2));

		int drawaddress=address;

		for (int y=0; y<height()/BYTEHEIGHT; y++)
		{
			//figure out how many bytes we are drawing
			int bytes=overlay.getByteStep(drawaddress);

			//highlights
			g.setColor(overlay.highlightRow(drawaddress));
			g.fillRect(0,y*BYTEHEIGHT,width(),BYTEHEIGHT);
				
			//lines
			g.setColor(new Color(150,150,150));
			g.drawLine(0,y*BYTEHEIGHT,width(),y*BYTEHEIGHT);

			g.setColor(Color.BLACK);
			//address
			g.drawString(Integer.toHexString(drawaddress),0,(y+1)*BYTEHEIGHT-1);

			//data in hex
			for (int b=0; b<bytes; b++)
			{
				int data;
				if (overlay.highlightData(drawaddress+b)!=null)
				{
					g.setColor(overlay.highlightData(drawaddress+b));
					g.fillRect(ADDRESS_WIDTH+b*BYTEHEIGHT*3/2,y*BYTEHEIGHT+1,BYTEHEIGHT*3/2,BYTEHEIGHT-1);
				}
				g.setColor(Color.BLACK);
				//don't allocate memory if we don't need to
				if (!computer.physicalMemory.isInitialized(drawaddress+b))
					data=0;
				else
					data=computer.physicalMemory.getByte(drawaddress+b);
				String dataString=Integer.toHexString(0xf&(data>>4))+Integer.toHexString(0xf&data);
				g.drawString(dataString,ADDRESS_WIDTH+b*BYTEHEIGHT*3/2,(y+1)*BYTEHEIGHT-1);
			}

			//commentary
			g.drawString(overlay.getCommentary(drawaddress),ADDRESS_WIDTH+DATA_WIDTH,(y+1)*BYTEHEIGHT-1);

			drawaddress+=bytes;
		}
	}
	public void adjustmentValueChanged(AdjustmentEvent e)
	{
		if (scrollBar.getValue()==address+1)
		{
			address=address+overlay.getByteStep(address);
			scrollBar.setValue(address);
		}
		else if (scrollBar.getValue()==address-1)
		{
			//try to find the previous step, up to 16
			int i;
			for (i=1; i<16; i++)
				if (address-i+overlay.getByteStep(address-i)==address)
					break;
			//if can't find a previous step, just step 1 byte
			if (i==16)
				i=1;
			address=address-i;
			scrollBar.setValue(address);
		}
		else
			address=scrollBar.getValue();
		repaint();
	}

	public void mouseClick(MouseEvent e)
	{
		int x=e.getX();
		int y=e.getY();
		if (y<0 || y>=height())
			return;

		if (e.getButton()==MouseEvent.BUTTON3 && type==CODE)
		{
			int by=y/BYTEHEIGHT;

			drawaddress=address;
			for (int i=0; i<by; i++)
				drawaddress=drawaddress+overlay.getByteStep(drawaddress);

			if (computer.breakpointGUI==null)
				computer.breakpointGUI=new BreakpointGUI(computer,"( instruction_address == "+Integer.toHexString(drawaddress)+" ) . ");
			else
				computer.breakpointGUI.setEquation("( instruction_address == "+Integer.toHexString(drawaddress)+" ) . ");
			setStatusLabel("Breakpoint set at "+Integer.toHexString(drawaddress));
			return;
		}

		if (x>=ADDRESS_WIDTH+DATA_WIDTH)
			return;
		if (x<ADDRESS_WIDTH)
		{
			addressEditing=true;
			statusEdit("Go to address: ",-1,true);
			return;
		}

		int by=y/BYTEHEIGHT;
		int bx=(x-ADDRESS_WIDTH)/(3*BYTEHEIGHT/2);

		drawaddress=address;
		for (int i=0; i<by; i++)
			drawaddress=drawaddress+overlay.getByteStep(drawaddress);

		if(bx>overlay.getByteStep(drawaddress))
			return;

		drawaddress+=bx;
		String baseLabel="New value for "+Integer.toHexString(drawaddress)+": ";
		addressEditing=false;
		statusEdit(baseLabel,2,true);
	}

	public void statusEdited(String keys)
	{
		keys=keys.toLowerCase();
		if (addressEditing)
		{
			this.address=Integer.parseInt(keys,16);
			scrollBar.setValue(address);
			guiComponent.paintImmediately(0,0,width(),height());
			return;
		}

		computer.physicalMemory.setByte(drawaddress,(byte)Integer.parseInt(keys,16));
		if (computer.memoryGUI.codeFrame!=null) computer.memoryGUI.codeFrame.repaint();
		if (computer.memoryGUI.stackFrame!=null) computer.memoryGUI.stackFrame.repaint();
		if (computer.memoryGUI.dataFrame!=null) computer.memoryGUI.dataFrame.repaint();
		if (computer.memoryGUI.defaultFrame!=null) computer.memoryGUI.defaultFrame.repaint();
		if (computer.memoryGUI.interruptFrame!=null) computer.memoryGUI.interruptFrame.repaint();
		drawaddress++;
		String baseLabel="New value for "+Integer.toHexString(drawaddress)+": ";
		statusEdit(baseLabel,2,true);
	}

	private abstract class ByteOverlay
	{
		public static final int NONE=0,EAX=1,EBX=2,ECX=3,EDX=4,ESP=5,EBP=6,ESI=7,EDI=8,EIP=9;
		public static final int CS=10,SS=20,DS=30,ES=40,FS=50,GS=60;

		public abstract int max_data_width();

		public abstract int getByteStep(int address);
		public Color highlightRow(int address)
		{
			return Color.WHITE;
		}
		public Color highlightData(int address)
		{
			return null;
		}
		public abstract String getCommentary(int address);
		public int update(int address, int newaddress, String information, int size)
		{
			return -1;
		}
	}

	private class DefaultByteOverlay extends ByteOverlay
	{
		public static final int DEFAULT_BYTE_WIDTH=8;

		public int max_data_width()
		{
			return 8;
		}

		public int getByteStep(int address)
		{
			return DEFAULT_BYTE_WIDTH;
		}
		public String getCommentary(int address)
		{
			String ascii="";
			for (int i=0; i<DEFAULT_BYTE_WIDTH; i++)
			{
				char c = (char)computer.physicalMemory.getByte(address+i);
				if(c<' ') c='.';
				else if(c>'z') c='.';
				ascii=ascii+c+" ";
			}
			return ascii;
		}
	}

	private class CodeByteOverlay extends ByteOverlay
	{
		int decodedAddress=-1;
		Processor decoder;
		int lastIP=-1;

		public int max_data_width()
		{
			return 4;
		}
		private void decode(int address)
		{
			decoder=new Processor(computer);
			decoder.cs.setValue(address>>>4);
			decoder.eip.setValue(address & 0xf);
			decoder.constructProcessorGUICode();
			decoder.fetchQueue.fetch();
			decoder.decodeInstruction(decoder.cs.getDefaultSize());
			while (decoder.codesHandled<decoder.codeLength)
				decoder.processorGUICode.pushMicrocode(decoder.getCode(),0,0,0,0,false);
			decodedAddress=address;
		}
		public int getByteStep(int address)
		{
			if (address!=decodedAddress)
				decode(address);
			return decoder.fetchQueue.instructionLength();
		}
		public String getCommentary(int address)
		{
			if (address!=decodedAddress)
				decode(address);
			return decoder.processorGUICode.constructName();
		}
		public Color highlightRow(int address)
		{
			if (address == computer.processor.cs.address(computer.processor.eip.getValue()))
				return new Color(100,100,250);
			else if (computer.memoryGUI!=null && computer.memoryGUI.codeFrame!=null && address == computer.memoryGUI.codeFrame.lastIP)
				return new Color(200,200,250);
			else if (computer.memoryGUI!=null && address==computer.memoryGUI.lastCodeWrite)
				return new Color(250,100,100);
			else if (computer.memoryGUI!=null && address==computer.memoryGUI.lastCodeRead)
				return new Color(100,250,100);

			return Color.WHITE;
		}
		public int update(int address, int newaddress, String information, int size)
		{
			if (newaddress!=computer.memoryGUI.codeFrame.lastIP)
				return -1;
			return computer.memoryGUI.codeFrame.lastIP;
		}
	}

	private class StackByteOverlay extends ByteOverlay
	{
		public static final int MAX_INFO=10000;
		String[] stackInfoList=new String[MAX_INFO];
		int[] stackInfoListAddress=new int[MAX_INFO];
		int[] stackInfoListSize=new int[MAX_INFO];
		int infoSize=0;
		
		public int max_data_width()
		{
			return 4;
		}
		public int getByteStep(int address)
		{
			for (int i=0; i<infoSize; i++)
			{
				if (stackInfoListAddress[i]==address)
					return stackInfoListSize[i];
			}
			return 1;
		}
		public String getCommentary(int address)
		{
			for (int i=0; i<infoSize; i++)
			{
				if (stackInfoListAddress[i]==address)
					return stackInfoList[i];
			}
			return "";
		}
		public Color highlightRow(int address)
		{
			if (address==computer.memoryGUI.lastStackWrite)
				return new Color(250,100,100);
			else if (address==computer.memoryGUI.lastStackRead)
				return new Color(100,250,100);
			else if (address == computer.processor.ss.address(computer.processor.esp.getValue()))
				return new Color(100,100,250);
			else
				return Color.WHITE;
		}
		public int update(int address, int newaddress, String information, int size)
		{
			if (!information.equals("") && infoSize<MAX_INFO)
			{
				int i;
				for (i=0; i<infoSize; i++)
					if (stackInfoListAddress[i]==newaddress)
						break;
				if (i<infoSize)
					stackInfoList[i]=information;
				else
				{
					stackInfoList[infoSize]=information;
					stackInfoListAddress[infoSize]=newaddress;
					stackInfoListSize[infoSize]=size;
					infoSize++;
				}
			}
			return newaddress;
		}
	}

	private class DataByteOverlay extends DefaultByteOverlay
	{
		public Color highlightRow(int address)
		{
			if (address/DEFAULT_BYTE_WIDTH==computer.memoryGUI.lastDataRead/DEFAULT_BYTE_WIDTH)
				return new Color(50,150,50);
			if (address/DEFAULT_BYTE_WIDTH==computer.memoryGUI.lastDataWrite/DEFAULT_BYTE_WIDTH)
				return new Color(150,50,50);
			if (address/DEFAULT_BYTE_WIDTH==computer.memoryGUI.lastExtraRead/DEFAULT_BYTE_WIDTH)
				return new Color(50,250,50);
			if (address/DEFAULT_BYTE_WIDTH==computer.memoryGUI.lastExtraWrite/DEFAULT_BYTE_WIDTH)
				return new Color(250,50,50);
			return Color.WHITE;
		}
		public Color highlightData(int address)
		{
			if (address==computer.memoryGUI.lastDataRead)
				return Color.WHITE;
			if (address==computer.memoryGUI.lastDataWrite)
				return Color.WHITE;
			if (address==computer.memoryGUI.lastExtraRead)
				return Color.WHITE;
			if (address==computer.memoryGUI.lastExtraWrite)
				return Color.WHITE;
			return highlightRow(address);
		}

		public int update(int address, int newaddress, String information, int size)
		{
			return newaddress / DEFAULT_BYTE_WIDTH * DEFAULT_BYTE_WIDTH;
		}
	}
}

