package simulator;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Enumeration;

public class DatapathBuilder extends AbstractGUI
{
	private DatapathComponents datapathComponents;
	private String placing="";

	public int WIDTH=5000,HEIGHT=5000;
	public double scaling=1.6;
	public int gridsize=5;
	public DatapathBuilder(Computer computer)
	{
		super(computer,"Datapath Builder",800,800,true,true,false,true);
		datapathComponents=new DatapathComponents(computer);
		refresh();
	}
	public void closeGUI()
	{
		computer.datapathBuilder=null;
		if (computer.controlBuilder!=null) computer.controlBuilder.close();
	}

	public void place(String component)
	{
		if (component==null || component.equals("")) return;
		placing=new String(component);
		setStatusLabel("Click to place a "+component);
	}

	public void mouseClick(MouseEvent e)
	{
		if (placing.equals(""))
			return;
		if (placing.equals("bus"))
			return;
		if (placing.equals("move"))
			return;
		if (placing.equals("name"))
		{
			unselectAll();
			doSelect(getX(e),getY(e));
			name();
		}
		else if (placing.equals("value"))
		{
			unselectAll();
			doSelect(getX(e),getY(e));
			value();
		}
		else if (placing.equals("select"))
			doSelect(getX(e),getY(e));
		else
			new Block(placing,Integer.parseInt(datapathComponents.bitfield.getText())).place(getX(e),getY(e));
		repaint();
	}

	int mousex,mousey,mousedirection,tempmousex,tempmousey,entryblock,entryblockbits,exitblock,currentbus;
	Block selectedblock;

	public void mousePress(MouseEvent e)
	{
		if (placing.equals("bus"))
		{
			Block b=first;
			while(b!=null)
			{
				if (b.getXExit(getX(e),getY(e))!=-1 && b.getYExit(getX(e),getY(e))!=-1)
					break;
				b=b.next;
			}
			if (b==null)
				return;

			setStatusLabel("Release at bus endpoint");
			mousex=b.getXExit(getX(e),getY(e));
			mousey=b.getYExit(getX(e),getY(e));
			mousedirection=b.getExitDirection(getX(e),getY(e));
			entryblock=b.number;
			entryblockbits=b.bits;
			exitblock=0;
			selectedblock=b;
		}
		else if (placing.equals("move"))
		{
			setStatusLabel("Release at new location");
			mousex=getX(e);
			mousey=getY(e);
		}
	}

	public void mouseRelease(MouseEvent e)
	{
		resetHighlights();
		if (placing.equals("bus"))
		{
			if (mousex==getX(e) && mousey==getY(e)) return;

			Block b=first;
			while(b!=null)
			{
				if (b.getXEntrance(getX(e),getY(e))!=-1 && b.getYEntrance(getX(e),getY(e))!=-1)
					break;
				b=b.next;
			}
			if (b==null)
			{
				tempmousex=getX(e);
				tempmousey=getY(e);
				exitblock=0;
			}
			else
			{
				tempmousex=b.getXEntrance(getX(e),getY(e));
				tempmousey=b.getYEntrance(getX(e),getY(e));
				exitblock=b.number;

				if (b.type.equals("joiner"))
					selectedblock=b;
			}

			Block newb=null;
			if (mousedirection==1)
			{
				newb=new Block(placing,entryblockbits);
				newb.place(mousex,mousey,tempmousex,mousey,entryblock,exitblock);
			}
			else if (mousedirection==2)
			{
				newb=new Block(placing,entryblockbits);
				newb.place(mousex,mousey,mousex,tempmousey,entryblock,exitblock);
			}
			mousedirection=0;
			repaint();

			if (selectedblock!=null && selectedblock.type.equals("splitter") && selectedblock.number==entryblock)
			{
				statusEdit("List which bits the bus will carry: ",-1,false);
				placing="splitterout";
				currentbus=newb.number;
			}
			else if (selectedblock!=null && selectedblock.type.equals("joiner") && selectedblock.number==exitblock)
			{
				statusEdit("List which bits the bus will source: ",-1,false);
				placing="joinerin";
				currentbus=newb.number;
			}
			else
				setStatusLabel("Press the mouse on a block to draw a bus");
		}
		else if (placing.equals("move"))
		{
			Block b=first;
			while(b!=null)
			{
				if (b.selected)
				{
					b.xcoor+=(getX(e)-mousex);
					b.ycoor+=(getY(e)-mousey);
					b.xcoor2+=(getX(e)-mousex);
					b.ycoor2+=(getY(e)-mousey);
				}
				b=b.next;
			}
			repaint();
			setStatusLabel("Press the mouse to start moving blocks");
		}
	}

	public String[] controlOutputs()
	{
		Block b=first;
		int i=0;
		while(b!=null)
		{
			if (!b.controlOutputs().equals(""))
				i++;
			b=b.next;
		}
		String[] c=new String[i];
		b=first;
		i=0;
		while(b!=null)
		{
			if (!b.controlOutputs().equals(""))
			{
				c[i++]=b.controlOutputs();
			}
			b=b.next;
		}
		return c;
	}

	public String[] controlInputs()
	{
		Block b=first;
		int i=0;
		while(b!=null)
		{
			if (!b.controlInputs().equals(""))
				i++;
			b=b.next;
		}
		String[] c=new String[i];
		b=first;
		i=0;
		while(b!=null)
		{
			if (!b.controlInputs().equals(""))
			{
				c[i++]=b.controlInputs();
			}
			b=b.next;
		}
		return c;
	}

	public void mouseDrag(MouseEvent e)
	{
		if (!placing.equals("bus")) return;
		if (mousedirection==1)
		{
			tempmousex=getX(e);
			tempmousey=mousey;
		}
		else if (mousedirection==2)
		{
			tempmousey=getY(e);
			tempmousex=mousex;
		}

		Block b=first;
		while(b!=null)
		{
			if (b.getXEntrance(getX(e),getY(e))!=-1 && b.getYEntrance(getX(e),getY(e))!=-1)
				b.highlighted=true;
			else
				b.highlighted=false;
			b=b.next;
		}

		repaint();
	}

	public void placewire()
	{
		setStatusLabel("Press mouse on a block to draw a bus");
		placing="bus";
		mousedirection=0;
	}

	public void select()
	{
		placing="select";
		setStatusLabel("Click to select a block");
	}

	public void doname()
	{
		placing="name";
		setStatusLabel("Click a block to name it");
	}

	public void dovalue()
	{
		placing="value";
		setStatusLabel("Click a block to set its value");
	}

	public void name()
	{
		placing="name";
		statusEdit("Enter a name: ",-1,false);
	}

	public void value()
	{
		placing="value";
		statusEdit("Enter a value: ",-1,true);
	}

	public void statusEdited(String keys)
	{
		if (placing.equals("name"))
		{
			Block b=first;
			while(b!=null)
			{
				if (b.selected)
				{
					b.name=new String(keys);
				}
				b=b.next;
			}
			unselectAll();
			repaint();
			setStatusLabel("Click a block to name it");
		}
		else if (placing.equals("value"))
		{
			Block b=first;
			while(b!=null)
			{
				if (b.selected)
				{
					b.setValue(Long.parseLong(new String(keys),16));
				}
				b=b.next;
			}
			if (computer.customProcessor!=null)
				propagateAll();
			unselectAll();
			repaint();
			setStatusLabel("Click a block to set its value");
		}
		else if (placing.equals("splitterout")||placing.equals("joinerin"))
		{
			selectedblock.bus.put(new Integer(currentbus),keys);
			placing="";
			setStatusLabel("");
			Block b=first;
			while(b!=null)
			{
				if (b.number==currentbus)
					break;
				b=b.next;
			}
			b.bits=Integer.parseInt(keys.substring(0,keys.indexOf(":")))-Integer.parseInt(keys.substring(keys.indexOf(":")+1,keys.length()))+1;
			repaint();
		}
	}

	public void move()
	{
		placing="move";
		setStatusLabel("Press mouse to start moving blocks");
	}

	public void delete()
	{
		Block b=first;
		while(b!=null)
		{
			if (b.selected)
			{
				removeBlock(b);
				b=first;
			}
			else
				b=b.next;
		}
		repaint();
	}

	public int width()
	{
		return WIDTH;
	}
	public int height()
	{
		return HEIGHT;
	}

	public void doPaint(Graphics g)
	{
		paintAll(g);

		if (placing.equals("bus") && mousedirection!=0)
		{
			g.setColor(Color.YELLOW);
			drawLine(g,mousex,mousey,tempmousex,tempmousey);
		}
	}

	public void doSelect(int x, int y)
	{
		Block b=first;
		while(b!=null)
		{
			if (b.doSelect(x,y))
				b.selected=!b.selected;
			b=b.next;
		}
		repaint();
	}

	public Block first=null;
	public int blocknumber=1;
	private void addBlock(Block b)
	{
		if (first==null) first=b;
		else
		{
			Block last=first;
			while(last.next!=null)
				last=last.next;
			last.next=b;
		}
		b.number=blocknumber++;
	}
	private void removeBlock(Block b)
	{
		Block toremove=first;
		if (toremove==b)
		{
			first=first.next;
			return;
		}			
		while(toremove.next!=null && toremove.next!=b)
			toremove=toremove.next;
		if (toremove.next==b)
			toremove.next=toremove.next.next;
	}
	private void paintAll(Graphics g)
	{
		Block b=first;
		while(b!=null)
		{
			b.draw(g);
			b=b.next;
		}
	}
	private void unselectAll()
	{
		Block b=first;
		while(b!=null)
		{
			b.selected=false;
			b=b.next;
		}
		repaint();
	}
	public void clockAll()
	{
		Block b=first;
		while(b!=null)
		{
			b.doClock();
			b=b.next;
		}
		repaint();
	}
	public void resetClocks()
	{
		Block b=first;
		while(b!=null)
		{
			b.resetClock();
			b=b.next;
		}
	}	
	public void resetHighlights()
	{
		Block b=first;
		while(b!=null)
		{
			b.highlighted=false;
			b=b.next;
		}
	}	
	public void resetAll()
	{
		System.out.println("reset all called");
		Block b=first;
		while(b!=null)
		{
			if (!b.type.equals("ports")&&!b.type.equals("memory"))
				b.setValue(0);
			b=b.next;
		}
		repaint();
	}
	public void propagateAll()
	{
		Block b=first;
		int count=0;

		while(b!=null)
		{
			count++;
			b=b.next;
		}
		for (int i=0; i<count; i++)
		{
			b=first;
			while(b!=null)
			{
				b.doPropagate();
				b=b.next;
			}
		}
	}

	public void verify()
	{
		Block b=first;

		while(b!=null)
		{
			if (!b.verify())
				b.selected=true;
			else
				b.selected=false;
			b=b.next;
		}
		repaint();
	}

	public Block getBlock(int number)
	{
		Block b=first;
		while (b!=null)
		{
			if (b.number==number) return b;
			b=b.next;
		}
		return null;
	}

	public String dumpXML()
	{
		String xml="<processor>\n\n";
		Block b=first;
		while(b!=null)
		{
			xml+=b.getXML()+"\n";
			b=b.next;
		}
		xml+="</processor>\n";
		return xml;
	}

	public void drawRect(Graphics g, int a, int b, int c, int d)
	{
		g.drawRect((int)(a*scaling),(int)(b*scaling),(int)(c*scaling),(int)(d*scaling));
	}
	public void fillRect(Graphics g, int a, int b, int c, int d)
	{
		g.fillRect((int)(a*scaling),(int)(b*scaling),(int)(c*scaling),(int)(d*scaling));
	}
	public void drawLine(Graphics g, int a, int b, int c, int d)
	{
		g.drawLine((int)(a*scaling),(int)(b*scaling),(int)(c*scaling),(int)(d*scaling));
	}
	public void drawOval(Graphics g, int a, int b, int c, int d)
	{
		g.drawOval((int)(a*scaling),(int)(b*scaling),(int)(c*scaling),(int)(d*scaling));
	}
	public void drawString(Graphics g, String s, int a, int b)
	{
		g.drawString(s,(int)(a*scaling),(int)(b*scaling));
	}
	public int getX(MouseEvent e)
	{
		int x=(int)(e.getX()/scaling);
		x-=x%gridsize;
		return x;
	}
	public int getY(MouseEvent e)
	{
		int y=(int)(e.getY()/scaling);
		y-=y%gridsize;
		return y;
	}

	public class Block
	{
		public static final int MAX_BUSES_PER_BLOCK=32;
		public Block next=null;
		public String name="",description="";
		public int xcoor,ycoor,xcoor2,ycoor2;
		public int number;
		public int entryblock,exitblock;
		public boolean selected=false;

		public Hashtable<Integer,String> bus = new Hashtable<Integer,String>();
		public boolean highlighted=false;

		public static final int XSIZE=40,YSIZE=30;

		public String type;
		public int bits;

		public long value=0;
		private Hashtable<Integer,Long> regfilevalue=new Hashtable<Integer,Long>();

		public boolean clockSetting=false;
		public String operationSetting="";

		public Block(String type, int bits)
		{
			xcoor=-1;
			ycoor=-1;

			this.type=type;

			if (type.equals("memory"))
				name="memory";
			else if (type.equals("ports"))
				name="ports";
			if (type.equals("flag"))
				bits=1;
			this.bits=bits;
		}
		public Block getAddressInputBlock()
		{
			Block b=first;
			while(b!=null)
			{
				if (b.exitblock==number)
				{
					if (b.ycoor2!=ycoor)
						return b;
				}
				b=b.next;
			}
			return b;
		}
		public Block getDataInputBlock()
		{
			Block b=first;
			while(b!=null)
			{
				if (b.exitblock==number)
				{
					if (b.ycoor2==ycoor)
						return b;
				}
				b=b.next;
			}
			return b;
		}

		public void setValue(long val)
		{
			if (type.equals("register file"))
			{
				if (getAddressInputBlock()==null) 
				{
					error("no address input for reg file");
					return;
				}
				int addr=(int)getAddressInputBlock().getValue();
				this.regfilevalue.put(new Integer(addr),new Long(val&((long)Math.pow(2,bits)-1l)));
			}
			else if (type.equals("memory"))
			{
				if (getAddressInputBlock()==null) 
				{
					error("no address input for memory");
					return;
				}
				int addr=(int)getAddressInputBlock().getValue();
				for (int i=0; i<bits; i+=8)
					computer.physicalMemory.setByte(addr+i,(byte)((val>>>i)&0xff));
			}
			else if (type.equals("ports"))
			{
				if (getAddressInputBlock()==null) 
				{
					error("no address input for port");
					return;
				}
				int addr=(int)getAddressInputBlock().getValue();
				for (int i=0; i<bits; i+=8)
					computer.ioports.ioPortWriteByte(addr+i,(byte)((val>>>i)&0xff));
			}
			else
				this.value=val&((long)Math.pow(2,bits)-1l);
		}
		public long getValue()
		{
			if (type.equals("register file"))
			{
				int addr=(int)getAddressInputBlock().getValue();
				if (getAddressInputBlock()==null) return 0;

				if (regfilevalue.get(new Integer(addr))==null) return 0;
				return ((Long)regfilevalue.get(new Integer(addr))).longValue()&((long)Math.pow(2,bits)-1l);
			}
			else if (type.equals("memory"))
			{
				if (getAddressInputBlock()==null) return 0;
				int addr=(int)getAddressInputBlock().getValue();
				long val=0;
				for (int i=0; i<bits; i+=8)
					val+=(long)computer.physicalMemory.getByte(addr+i)<<(long)i;
				return val&((long)Math.pow(2,bits)-1l);
			}
			else if (type.equals("ports"))
			{
				if (getAddressInputBlock()==null) return 0;
				int addr=(int)getAddressInputBlock().getValue();
				long val=0;
				for (int i=0; i<bits; i+=8)
					val+=(long)computer.ioports.ioPortReadByte(addr+i)<<(long)i;
				return val&((long)Math.pow(2,bits)-1l);
			}
			else if (type.equals("constant"))
			{
				return Long.parseLong(name,16);
			}
			else
				return value&((long)Math.pow(2,bits)-1l);
		}
		public void resetClock()
		{
			clockSetting=false;
		}
		public void doClock()
		{
			if (!clockSetting) return;
			if (type.equals("register"))
			{
				if (getInputBlocks().length==0)
				{
					error("clocking register without an input bus");
					setValue(0);
					return;
				}
				System.out.println("Storing "+getInputBlocks()[0].getValue()+" into register "+name);
				highlighted=true;
				setValue(getInputBlocks()[0].getValue());
			}
			else if (type.equals("register file") || type.equals("ports") || type.equals("memory"))
			{
				if (getDataInputBlock()==null)
				{
					error("clocking register without an input bus");
					setValue(0);
					return;
				}
				System.out.println("Storing "+getDataInputBlock().getValue()+" into "+name);
				highlighted=true;
				setValue(getDataInputBlock().getValue());
			}
		}
		public void doPropagate()
		{
			if (type.equals("bus"))
			{
				if (entryblock==0)
					error("no input to bus");

				Block b=first;
				while (b!=null)
				{
					if (entryblock==b.number && !b.type.equals("splitter"))
					{
						if (b.getValue()!=value)
						{
							System.out.println("Moving value "+b.getValue()+" from "+entryblock+" to "+number);
							highlighted=true;
						}
						setValue(b.getValue());
					}
					b=b.next;
				}
			}
			else if (type.equals("adder"))
			{
				Block[] bs=getInputBlocks();
				if (bs.length!=2)
				{
					error("adder needs two input buses");
					return;
				}
				setValue(bs[0].getValue()+bs[1].getValue());					
			}
			else if (type.equals("ALU"))
			{
				Block[] bs=getInputBlocks();
				if (bs.length<1)
				{
					error("ALU needs at least one input bus");
					return;
				}
				long v1,v2;
				if (bs[0].xcoor<bs[1].xcoor)
				{
					v1=bs[0].getValue();
					v2=bs[1].getValue();
				}
				else
				{
					v1=bs[1].getValue();
					v2=bs[0].getValue();
				}
				if (operationSetting.equals("+"))
					setValue(v1+v2);
				else if (operationSetting.equals("-"))
					setValue(v1-v2);
				else if (operationSetting.equals("*"))
					setValue(v1*v2);
				else if (operationSetting.equals("/"))
					setValue(v1/v2);
				else if (operationSetting.equals("AND"))
					setValue(v1&v2);
				else if (operationSetting.equals("OR"))
					setValue(v1|v2);
				else if (operationSetting.equals("XOR"))
					setValue(v1^v2);
				else if (operationSetting.equals("XNOR"))
					setValue(~(v1^v2));
				else if (operationSetting.equals("NAND"))
					setValue(~(v1&v2));
				else if (operationSetting.equals("NOR"))
					setValue(~(v1|v2));
				else if (operationSetting.equals("NOT"))
					setValue(~v1);
				else if (operationSetting.equals("<<"))
					setValue(v1<<v2);
				else if (operationSetting.equals(">>"))
					setValue(v1>>>v2);
				else if (operationSetting.equals("=="))
					setValue(v1==v2? 1l:0);
				else if (operationSetting.equals("==0?"))
					setValue(v1==0? 1l:0);
				else if (operationSetting.equals("!=0?"))
					setValue(v1==0? 0:1l);
				else if (operationSetting.equals("!="))
					setValue(v1!=v2? 1l:0);
				else if (operationSetting.equals("<"))
					setValue(v1<v2? 1l:0);
				else if (operationSetting.equals("<="))
					setValue(v1<=v2? 1l:0);
				else if (operationSetting.equals(">"))
					setValue(v1>v2? 1l:0);
				else if (operationSetting.equals(">="))
					setValue(v1>=v2? 1l:0);
				else if (operationSetting.equals("+1"))
					setValue(v1+1l);
				else if (operationSetting.equals("-1"))
					setValue(v1-1l);
				else if (operationSetting.equals("0"))
					setValue(0);
				else if (operationSetting.equals("IN1"))
					setValue(v1);
				else if (operationSetting.equals("IN2"))
					setValue(v2);
				else if (operationSetting.equals("NOP"))
					setValue(v1);
			}
			else if (type.equals("multiplexor"))
			{
				if (operationSetting.equals(""))
				{
//					System.out.println("mux "+number+" has no setting");
					return;
				}
				if (operationSetting.equals("X"))
				{
					return;
				}
				Block[] bs=getInputBlocks();
				if (bs.length<1)
				{
					error("mux needs an input");
					return;
				}
				for (int i=0; i<bs.length; i++)
				{
					for (int j=0; j<bs.length-1; j++)
					{
						if (bs[j].xcoor>bs[j+1].xcoor)
						{
							Block tmpblock=bs[j];
							bs[j]=bs[j+1];
							bs[j+1]=tmpblock;
						}
					}
				}
//				System.out.println("setting mux "+number+" to input "+Integer.parseInt(operationSetting,16));
				setValue(bs[Integer.parseInt(operationSetting,16)].getValue());
			}
			else if (type.equals("data_multiplexor"))
			{
				int addr=(int)getAddressInputBlock().getValue();
				if (getAddressInputBlock()==null)
				{
					error("mux needs an input");
					return;
				}
				Block[] bs=getInputBlocks();
				if (bs.length<1)
				{
					error("mux needs an input");
					return;
				}
				for (int i=0; i<bs.length; i++)
				{
					for (int j=0; j<bs.length-1; j++)
					{
						if (bs[j].xcoor>bs[j+1].xcoor)
						{
							Block tmpblock=bs[j];
							bs[j]=bs[j+1];
							bs[j+1]=tmpblock;
						}
					}
				}
				setValue(bs[addr].getValue());
			}
			else if (type.equals("control"))
			{
				if (getInputBlocks().length==0) 
				{
					error("control needs an input");
					return;
				}
				setValue(getInputBlocks()[0].getValue());
			}
			else if (type.equals("splitter"))
			{
				if (getInputBlocks().length==0) 
				{
					error("splitter needs an input");
					return;
				}
				setValue(getInputBlocks()[0].getValue());
				for (Enumeration e=bus.keys(); e.hasMoreElements();)
				{
					int i=((Integer)e.nextElement()).intValue();

					Block b=first;
					while(b!=null)
					{
						if (b.number==i)
						{
							String busstring=(String)bus.get(new Integer(i));
							int b1=Integer.parseInt(busstring.substring(0,busstring.indexOf(":")));
							int b2=Integer.parseInt(busstring.substring(busstring.indexOf(":")+1,busstring.length()));
							long v=getValue();
							v=v>>>(long)b2;
							v=v&(long)(Math.pow(2,b1-b2+1)-1);
							b.setValue(v);
						}
						b=b.next;
					}
				}
			}
			else if (type.equals("joiner"))
			{
				long v=0;
				for (Enumeration e=bus.keys(); e.hasMoreElements();)
				{
					int i=((Integer)e.nextElement()).intValue();

					Block b=first;
					while(b!=null)
					{
						if (b.number==i)
						{
							String busstring=(String)bus.get(new Integer(i));
							int b1=Integer.parseInt(busstring.substring(0,busstring.indexOf(":")));
							int b2=Integer.parseInt(busstring.substring(busstring.indexOf(":")+1,busstring.length()));
							v=v+((b.getValue()&(long)(Math.pow(2,b1-b2+1)-1))<<(long)b2);
						}
						b=b.next;
					}
				}
				setValue(v);
			}
			else if (type.equals("extender"))
			{
				if (getInputBlocks().length==0) 
				{
					error("extender needs an input");
					return;
				}
				setValue(getInputBlocks()[0].getValue());
			}
			else if (type.equals("register file")||type.equals("memory")||type.equals("ports")||type.equals("constant"))
			{
				value=getValue();
			}
		}

		private void error(String message)
		{
			System.out.println("Error in block "+number+": "+message);
			System.exit(0);
		}

		public Block[] getInputBlocks()
		{
			Block b=first;
			int count=0;
			while(b!=null)
			{
				if (b.exitblock==number && b.ycoor2==ycoor)
					count++;
				b=b.next;
			}
			Block[] bs=new Block[count];
			count=0;
			b=first;
			while(b!=null)
			{
				if (b.exitblock==number && b.ycoor2==ycoor)
				{
					bs[count++]=b;
				}
				b=b.next;
			}
			return bs;
		}

		public void place(int x, int y)
		{
			xcoor=x;
			ycoor=y;
			addBlock(this);
		}
		public void place(int x1, int y1, int x2, int y2, int entb, int exb)
		{
			xcoor=x1;
			ycoor=y1;
			xcoor2=x2;
			ycoor2=y2;
			entryblock=entb;
			exitblock=exb;
			addBlock(this);
		}
		public void delete()
		{
			removeBlock(this);
		}
		public boolean doSelect(int x, int y)
		{
			if (type.equals("flag") && x>=xcoor && x<=xcoor+XSIZE/2 && y>=ycoor && y<=ycoor+YSIZE/2) return true;
			else if (type.equals("constant") && x>=xcoor && x<=xcoor+XSIZE/2 && y>=ycoor && y<=ycoor+YSIZE/2) return true;
			else if (type.equals("joiner") && x>=xcoor && x<=xcoor+XSIZE && y>=ycoor && y<=ycoor+YSIZE/3) return true;
			else if (type.equals("splitter") && x>=xcoor && x<=xcoor+XSIZE && y>=ycoor && y<=ycoor+YSIZE/3) return true;
			else if (type.equals("decoder") && x>=xcoor && x<=xcoor+XSIZE && y>=ycoor && y<=ycoor+YSIZE/3) return true;
			else if (type.equals("multiplexor") && x>=xcoor && x<=xcoor+XSIZE && y>=ycoor && y<=ycoor+YSIZE/3) return true;
			else if (type.equals("data_multiplexor") && x>=xcoor && x<=xcoor+XSIZE && y>=ycoor && y<=ycoor+YSIZE/3) return true;
			else if (type.equals("extender") && x>=xcoor && x<=xcoor+XSIZE && y>=ycoor && y<=ycoor+YSIZE/3) return true;
			else if (type.equals("inhibitor") && x>=xcoor && x<=xcoor+XSIZE && y>=ycoor && y<=ycoor+YSIZE/3) return true;
			else if (type.equals("control") && x>=xcoor && x<=xcoor+XSIZE && y>=ycoor && y<=ycoor+YSIZE/3) return true;
			else if (type.equals("bus") && xcoor==xcoor2 && (x-xcoor)>=-2 && (x-xcoor)<=2 && ((y>=ycoor && y<=ycoor2)||(y>=ycoor2 && y<=ycoor))) return true;
			else if (type.equals("bus") && ycoor==ycoor2 && (y-ycoor)>=-2 && (y-ycoor)<=2 && ((x>=xcoor && x<=xcoor2)||(x>=xcoor2 && x<=xcoor))) return true;
			else if ((type.equals("adder")||type.equals("ALU")||type.equals("register")||type.equals("register file")||type.equals("memory")||type.equals("ports")||type.equals("lookup table")) && x>=xcoor && x<=xcoor+XSIZE && y>=ycoor && y<=ycoor+YSIZE) return true;
			return false;
		}
		public int getXExit(int x, int y)
		{
			if (!doSelect(x,y))
				return -1;
			if (type.equals("bus") && ycoor==ycoor2)
				return x;
			if (type.equals("bus") && xcoor==xcoor2)
				return xcoor;
			if (type.equals("register")||type.equals("memory")||type.equals("ports")||type.equals("register file")||type.equals("lookup table")||type.equals("ALU")||type.equals("adder")||type.equals("multiplexor")||type.equals("data_multiplexor")||type.equals("joiner")||type.equals("inhibitor")||type.equals("extender")||type.equals("control"))
				return xcoor+XSIZE/2;
			if (type.equals("flag")||type.equals("constant"))
				return xcoor+XSIZE/4;
			if (type.equals("decoder")||type.equals("splitter"))
				return x;
			return -1;
		}
		public int getYExit(int x, int y)
		{
			if (!doSelect(x,y))
				return -1;
			if (type.equals("bus") && ycoor==ycoor2)
				return ycoor;
			if (type.equals("bus") && xcoor==xcoor2)
				return y;
			if (type.equals("register")||type.equals("memory")||type.equals("ports")||type.equals("register file")||type.equals("lookup table")||type.equals("ALU")||type.equals("adder"))
				return ycoor+YSIZE;
			if (type.equals("flag")||type.equals("constant"))
				return ycoor+YSIZE/2;
			if (type.equals("decoder")||type.equals("splitter")||type.equals("multiplexor")||type.equals("data_multiplexor")||type.equals("extender")||type.equals("inhibitor")||type.equals("joiner")||type.equals("control"))
				return ycoor+YSIZE/3;
			return -1;
		}
		public int getXEntrance(int x, int y)
		{
			if (!doSelect(x,y))
				return -1;
			if (type.equals("bus") && ycoor==ycoor2)
				return x;
			if (type.equals("bus") && xcoor==xcoor2)
				return xcoor;
			if (type.equals("register")||type.equals("lookup table")||type.equals("extender")||type.equals("decoder")||type.equals("inhibitor")||type.equals("splitter")||type.equals("flag")||type.equals("joiner")||type.equals("multiplexor")||type.equals("data_multiplexor")||type.equals("control"))
				return x;
			if ((type.equals("ALU")||type.equals("adder"))&&x>xcoor&&x<xcoor+XSIZE/2-XSIZE/5)
				return x;
			if ((type.equals("ALU")||type.equals("adder"))&&x<xcoor+XSIZE&&x>xcoor+XSIZE/2+XSIZE/5)
				return x;
			if ((type.equals("register file")||type.equals("memory")||type.equals("ports"))&&y<ycoor+YSIZE/4)
				return x;
			if ((type.equals("register file")||type.equals("memory")||type.equals("ports")||type.equals("data_multiplexor"))&&y>=ycoor+YSIZE/3&&x<xcoor+XSIZE/3)
				return xcoor;
			if ((type.equals("register file")||type.equals("memory")||type.equals("ports")||type.equals("data_multiplexor"))&&y>=ycoor+YSIZE/3&&x>xcoor+XSIZE-XSIZE/3)
				return xcoor+XSIZE;
			return -1;
		}
		public int getYEntrance(int x, int y)
		{
			if (!doSelect(x,y))
				return -1;
			if (type.equals("bus") && ycoor==ycoor2)
				return ycoor;
			if (type.equals("bus") && xcoor==xcoor2)
				return y;
			if (type.equals("register")||type.equals("lookup table")||type.equals("extender")||type.equals("decoder")||type.equals("inhibitor")||type.equals("splitter")||type.equals("flag")||type.equals("joiner")||type.equals("multiplexor")||type.equals("data_multiplexor")||type.equals("data_multiplexor")||type.equals("ALU")||type.equals("adder")||type.equals("control"))
				return ycoor;
			if ((type.equals("register file")||type.equals("memory")||type.equals("ports"))&&y<ycoor+YSIZE/4)
				return ycoor;
			if ((type.equals("register file")||type.equals("memory")||type.equals("ports")||type.equals("data_multiplexor"))&&y>=ycoor+YSIZE/3)
				return y;
			return -1;
		}
		public int getExitDirection(int x, int y)
		{
			if (type.equals("bus") && xcoor==xcoor2)
				return 1;
			else
				return 2;
		}

		private void setSelectedColor(Graphics g)
		{
			if (selected)
				g.setColor(Color.RED);
			else if (highlighted)
				g.setColor(new Color(255,50,0));
			else
				g.setColor(Color.BLACK);
		}

		public void draw(Graphics g)
		{
			if (type.equals("register"))
			{
				g.setColor(new Color(200,200,255));
				fillRect(g,xcoor,ycoor,XSIZE,YSIZE);
				setSelectedColor(g);
				drawRect(g,xcoor,ycoor,XSIZE,YSIZE);
			}
			else if (type.equals("flag"))
			{
				setSelectedColor(g);
				drawRect(g,xcoor,ycoor,XSIZE/2,YSIZE/2);
				if (getValue()!=0)
				{
					drawLine(g,xcoor+XSIZE/6,ycoor+2,xcoor+XSIZE/6,ycoor+YSIZE/2-2);
					g.setColor(Color.RED);
					drawLine(g,xcoor+XSIZE/6,ycoor+2,xcoor+XSIZE/2-2,ycoor+((YSIZE/2-4)/6)+2);
					drawLine(g,xcoor+XSIZE/6,ycoor+((YSIZE/2-4)/3)+2,xcoor+XSIZE/2-5,ycoor+((YSIZE/2-4)/6)+2);
				}
				else
				{
					drawLine(g,xcoor+2,ycoor+YSIZE/6,xcoor+XSIZE/2-2,ycoor+YSIZE/6);
					g.setColor(Color.RED);
					drawLine(g,xcoor+2,ycoor+YSIZE/6,xcoor+((XSIZE/2-4)/6)+2,ycoor+YSIZE/2-2);
					drawLine(g,xcoor+((XSIZE/2-4)/3)+2,ycoor+YSIZE/6,xcoor+((XSIZE/2-4)/6)+2,ycoor+YSIZE/2-5);
				}
			}
			else if (type.equals("lookup table"))
			{
				setSelectedColor(g);
				drawRect(g,xcoor,ycoor,XSIZE,YSIZE);
				drawLine(g,xcoor+5,ycoor+YSIZE/4,xcoor+XSIZE-5,ycoor+YSIZE/4);
				drawLine(g,xcoor+XSIZE/2,ycoor+YSIZE/4,xcoor+XSIZE/2,ycoor+YSIZE-YSIZE/4);
			}
			else if (type.equals("ALU")||type.equals("adder"))
			{
				setSelectedColor(g);
				int x=xcoor+XSIZE/2,y=ycoor+YSIZE/2;
				drawLine(g,x-XSIZE/2,y-YSIZE/2,x-XSIZE/2+XSIZE/5,y+YSIZE/2);	//left
				drawLine(g,x+XSIZE/2,y-YSIZE/2,x+XSIZE/2-XSIZE/5,y+YSIZE/2);	//right
				drawLine(g,x-XSIZE/2+XSIZE/5,y+YSIZE/2,x+XSIZE/2-XSIZE/5,y+YSIZE/2);	//bottom
				drawLine(g,x-XSIZE/8,y-YSIZE/2,x,y-YSIZE/3);		//left notch
				drawLine(g,x+XSIZE/8,y-YSIZE/2,x,y-YSIZE/3);		//right notch
				drawLine(g,x-XSIZE/2,y-YSIZE/2,x-XSIZE/8,y-YSIZE/2);	//left top
				drawLine(g,x+XSIZE/2,y-YSIZE/2,x+XSIZE/8,y-YSIZE/2);	//right top
			}
			else if (type.equals("memory")||type.equals("ports")||type.equals("register file"))
			{
				if (type.equals("register file"))
					g.setColor(new Color(200,200,255));
				else if (type.equals("memory"))
					g.setColor(new Color(255,200,255));
				else if (type.equals("ports"))
					g.setColor(new Color(200,255,220));
				fillRect(g,xcoor,ycoor,XSIZE,YSIZE);
				g.setColor(Color.BLACK);
				drawLine(g,xcoor,ycoor+YSIZE/3,xcoor+XSIZE,ycoor+YSIZE/3);
				drawLine(g,xcoor,ycoor+2*YSIZE/3,xcoor+XSIZE,ycoor+2*YSIZE/3);
				setSelectedColor(g);
				drawRect(g,xcoor,ycoor,XSIZE,YSIZE);
			}
			else if (type.equals("extender")||type.equals("inhibitor"))
			{
				setSelectedColor(g);
				drawOval(g,xcoor,ycoor,XSIZE,YSIZE/3);
			}
			else if (type.equals("joiner")||type.equals("splitter"))
			{
				setSelectedColor(g);
				drawRect(g,xcoor,ycoor,XSIZE,YSIZE/3);
			}
			else if (type.equals("control"))
			{
				g.setColor(new Color(150,0,0));
				fillRect(g,xcoor,ycoor,XSIZE,YSIZE/3);
				setSelectedColor(g);
				drawRect(g,xcoor,ycoor,XSIZE,YSIZE/3);
			}
			else if (type.equals("multiplexor")||type.equals("data_multiplexor"))
			{
				setSelectedColor(g);
				drawLine(g,xcoor,ycoor,xcoor+XSIZE,ycoor);
				drawLine(g,xcoor+XSIZE/5,ycoor+YSIZE/3,xcoor+XSIZE-XSIZE/5,ycoor+YSIZE/3);
				drawLine(g,xcoor,ycoor,xcoor+XSIZE/5,ycoor+YSIZE/3);
				drawLine(g,xcoor+XSIZE,ycoor,xcoor+XSIZE-XSIZE/5,ycoor+YSIZE/3);
			}
			else if (type.equals("decoder"))
			{
				setSelectedColor(g);
				drawLine(g,xcoor,ycoor+YSIZE/3,xcoor+XSIZE,ycoor+YSIZE/3);
				drawLine(g,xcoor+XSIZE/5,ycoor,xcoor+XSIZE-XSIZE/5,ycoor);
				drawLine(g,xcoor,ycoor+YSIZE/3,xcoor+XSIZE/5,ycoor);
				drawLine(g,xcoor+XSIZE,ycoor+YSIZE/3,xcoor+XSIZE-XSIZE/5,ycoor);
			}
			else if (type.equals("bus"))
			{
				setSelectedColor(g);
				drawLine(g,xcoor,ycoor,xcoor2,ycoor2);
				if (xcoor==xcoor2 && ycoor2>ycoor)
				{
					drawLine(g,xcoor-1,ycoor2-1,xcoor+1,ycoor2-1);
					drawLine(g,xcoor-2,ycoor2-2,xcoor+2,ycoor2-2);
					drawLine(g,xcoor-2,ycoor2-2,xcoor,ycoor2);
					drawLine(g,xcoor+2,ycoor2-2,xcoor,ycoor2);
				}
				else if (xcoor==xcoor2 && ycoor2<ycoor)
				{
					drawLine(g,xcoor-1,ycoor2+1,xcoor+1,ycoor2+1);
					drawLine(g,xcoor-2,ycoor2+2,xcoor+2,ycoor2+2);
					drawLine(g,xcoor-2,ycoor2+2,xcoor,ycoor2);
					drawLine(g,xcoor+2,ycoor2+2,xcoor,ycoor2);
				}
				else if (ycoor==ycoor2 && xcoor2>xcoor)
				{
					drawLine(g,xcoor2-1,ycoor2-1,xcoor2-1,ycoor2+1);
					drawLine(g,xcoor2-2,ycoor2-2,xcoor2-2,ycoor2+2);
					drawLine(g,xcoor2-2,ycoor2-2,xcoor2,ycoor2);
					drawLine(g,xcoor2-2,ycoor2+2,xcoor2,ycoor2);
				}
				else if (ycoor==ycoor2 && xcoor2<xcoor)
				{
					drawLine(g,xcoor2+1,ycoor2-1,xcoor2+1,ycoor2+1);
					drawLine(g,xcoor2+2,ycoor2-2,xcoor2+2,ycoor2+2);
					drawLine(g,xcoor2+2,ycoor2-2,xcoor2,ycoor2);
					drawLine(g,xcoor2+2,ycoor2+2,xcoor2,ycoor2);
				}
			}
			else if (type.equals("constant"))
			{
				setSelectedColor(g);
				drawOval(g,xcoor,ycoor,XSIZE/2,YSIZE/2);
			}
			g.setColor(Color.BLACK);
			g.setFont(new Font("Dialog",Font.PLAIN,(int)(8*scaling)));
			if (!name.equals(""))
			{
				if (type.equals("constant"))
					drawString(g,name,xcoor+5,ycoor+10);
				else
					drawString(g,name,xcoor+5,ycoor+16);
			}
			g.setColor(new Color(0,150,0));
			if (!type.equals("lookup table")&&!type.equals("bus")&&!type.equals("extender")&&!type.equals("joiner")&&!type.equals("constant"))
				drawString(g,""+bits,xcoor+1,ycoor-1);
			else if (type.equals("extender")||type.equals("joiner"))
				drawString(g,""+bits,xcoor+1,ycoor+YSIZE/3+12);
			else if (type.equals("constant"))
				drawString(g,""+bits,xcoor+1,ycoor+YSIZE/2+12);
			else if (type.equals("lookup table"))
				drawString(g,""+bits,xcoor+1,ycoor+YSIZE+12);
			else if (type.equals("bus") && xcoor==xcoor2)
			{
				drawLine(g,xcoor-3,(ycoor+ycoor2)/2+3,xcoor+3,(ycoor+ycoor2)/2-3);
				drawString(g,""+bits,xcoor+1,(ycoor+ycoor2)/2+3);
			}
			else if (type.equals("bus") && ycoor==ycoor2)
			{
				drawLine(g,(xcoor+xcoor2)/2-3,ycoor+3,(xcoor+xcoor2)/2+3,ycoor-3);
				drawString(g,""+bits,(xcoor+xcoor2)/2+3,ycoor-1);
			}
			if (type.equals("joiner"))
			{
				for (Enumeration e=bus.keys(); e.hasMoreElements();)
				{
					int i=((Integer)e.nextElement()).intValue();
					
					Block b=first;
					while (b!=null)
					{
						if (b.number==i) break;
						b=b.next;
					}
					if (b!=null)
						drawString(g,(String)bus.get(new Integer(i)),b.xcoor,ycoor-1);
				}
			}
			if (type.equals("splitter"))
			{
				for (Enumeration e=bus.keys(); e.hasMoreElements();)
				{
					int i=((Integer)e.nextElement()).intValue();
					Block b=first;
					while (b!=null)
					{
						if (b.number==i) break;
						b=b.next;
					}
					if (b!=null)
						drawString(g,(String)bus.get(new Integer(i)),b.xcoor,ycoor+YSIZE/3+12);
				}
			}
			if (computer.customProcessor!=null)
			{
				if (type.equals("register")||type.equals("register file")||type.equals("memory")||type.equals("ports"))
				{
					g.setColor(new Color(0,150,0));
					fillRect(g,xcoor-7,ycoor+YSIZE+12-9,(bits/4+1)*7,12);
					g.setColor(Color.WHITE);
					drawString(g,Long.toHexString(value),xcoor-7,ycoor+YSIZE+12);

					if (!type.equals("register"))
					{
						g.setColor(new Color(100,100,0));
						fillRect(g,xcoor-(bits/4+1)*7-2,ycoor+YSIZE/2-12,(bits/4+1)*7,12);
						g.setColor(Color.WHITE);
						drawString(g,Long.toHexString(getAddressInputBlock().getValue()),xcoor-(bits/4+1)*7,ycoor+YSIZE/2-2);
					}
				}
				else if (type.equals("adder")||type.equals("ALU"))
				{
					g.setColor(new Color(50,150,50));
					fillRect(g,xcoor-7,ycoor+YSIZE+12-9,(bits/4+1)*7,12);
					g.setColor(Color.WHITE);
					drawString(g,Long.toHexString(value),xcoor-7,ycoor+YSIZE+12);
				}
				else if (type.equals("multiplexor")||type.equals("data_multiplexor")||type.equals("joiner")||type.equals("control"))
				{
					g.setColor(new Color(50,150,50));
					fillRect(g,xcoor-7,ycoor+YSIZE/3+12-9,(bits/4+1)*7,12);
					g.setColor(Color.WHITE);
					drawString(g,Long.toHexString(value),xcoor-7,ycoor+YSIZE/3+12);

					if (type.equals("data_multiplexor"))
					{
						g.setColor(new Color(100,100,0));
						fillRect(g,xcoor-(bits/4+1)*7-2,ycoor+YSIZE/2-12,(bits/4+1)*7,12);
						g.setColor(Color.WHITE);
						drawString(g,Long.toHexString(getAddressInputBlock().getValue()),xcoor-(bits/4+1)*7,ycoor+YSIZE/2-2);
					}
				}
				else if (type.equals("constant")||type.equals("flag"))
				{
					g.setColor(new Color(0,150,0));
					fillRect(g,xcoor-7,ycoor+YSIZE/2+12-9,(bits/4+1)*7,12);
					g.setColor(Color.WHITE);
					drawString(g,Long.toHexString(value),xcoor-7,ycoor+YSIZE/2+12);
				}
/*				else if (type.equals("bus") && xcoor==xcoor2)
				{
					g.setColor(new Color(50,150,50));
					g.fillRect(xcoor-7,(ycoor+ycoor2)/2+12-9,(bits/4+1)*7,12);
					g.setColor(Color.WHITE);
					g.drawString(Long.toHexString(value),xcoor-7,(ycoor+ycoor2)/2+12);
				}
				else if (type.equals("bus") && ycoor==ycoor2)
				{
					g.setColor(new Color(50,150,50));
					g.fillRect((xcoor+xcoor2)/2-3,ycoor-14,(bits/4+1)*7,12);
					g.setColor(Color.WHITE);
					g.drawString(Long.toHexString(value),(xcoor+xcoor2)/2-3,ycoor-14+12);
				}*/
			}
		}
		public String getXML()
		{
			String xml = "<"+type+">\n<number>"+number+"</number>\n<name>"+name+"</name>\n<bits>"+bits+"</bits>\n<xcoordinate>"+xcoor+"</xcoordinate>\n<ycoordinate>"+ycoor+"</ycoordinate>\n";
			if (type.equals("bus"))
				xml+="<xcoordinate2>"+xcoor2+"</xcoordinate2>\n<ycoordinate2>"+ycoor2+"</ycoordinate2>\n";
			if (type.equals("bus"))
			{
				xml+="<entry>"+entryblock+"</entry>\n";
				xml+="<exit>"+exitblock+"</exit>\n";
			}
			if (type.equals("joiner")||type.equals("splitter"))
			{
				for (Enumeration e=bus.keys(); e.hasMoreElements();)
				{
					int i=((Integer)e.nextElement()).intValue();
					xml+="<line "+i+">"+(String)bus.get(new Integer(i))+"</line>\n";
				}
			}
			xml+="</"+type+">\n";
			return xml;
		}

		public String controlInputs()
		{
			if (type.equals("register")||type.equals("flag")||type.equals("register file")||type.equals("ports")||type.equals("memory"))
				return "1 clock "+name;
			else if (type.equals("ALU"))
				return "1 alu "+name;
			else if (type.equals("multiplexor"))
			{
				int i=0;
				Block b=first;
				while(b!=null)
				{
					if (b.type.equals("bus")&&b.exitblock==number)
						i++;
					b=b.next;
				}
				return ""+i+" mux "+name;
			}
			return "";
		}

		public String controlOutputs()
		{
			if (type.equals("control"))
				return ""+bits+" "+name;
			else
				return "";
		}

		public boolean verify()
		{
			//all buses have a valid input and output
			if (type.equals("bus"))
			{
				if (entryblock==0 || getBlock(entryblock)==null) return false;
				if (exitblock!=0 && getBlock(exitblock)==null) return false;
				if (exitblock==0)
				{
					Block b=first;
					while(b!=null)
					{
						if (b.entryblock==number)
							break;
						b=b.next;
					}
					if (b==null) return false;
				}
				return true;
			}
			//one input bus, input bus has same width
			else if (type.equals("register")||type.equals("flag"))
			{
				if (getInputBlocks().length!=1 || getInputBlocks()[0].bits!=bits) return false;
				if (name.equals("")) return false;
				return true;
			}
			//one input bus, same width, one address bus
			else if (type.equals("register_file"))
			{
				if (getInputBlocks().length!=1 || getInputBlocks()[0].bits!=bits) return false;
				if (name.equals("")) return false;
				if (getAddressInputBlock()==null) return false;
				return true;
			}
			//zero/one input bus, input bus has same width, one address bus
			else if (type.equals("memory")||type.equals("ports"))
			{
				if (getInputBlocks().length>1 || (getInputBlocks().length==1 && getInputBlocks()[0].bits!=bits)) return false;
				if (name.equals("")) return false;
				if (getAddressInputBlock()==null) return false;
				return true;
			}
			//one/two input bus, input bus has same width, one output bus, bus has same width
			else if (type.equals("ALU"))
			{
				if (getInputBlocks().length<1 || getInputBlocks()[0].bits!=bits) return false;
				if (name.equals("")) return false;
				return true;
			}
			//two input bus, same width, one output bus
			else if (type.equals("adder"))
			{
				if (getInputBlocks().length!=2 || getInputBlocks()[0].bits!=bits || getInputBlocks()[1].bits!=bits) return false;
				return true;
			}
			//one output bus, one/more input bus, bus has valid width, no leftover bits
			else if (type.equals("joiner"))
			{
				if (getInputBlocks().length<1) return false;
				return true;
			}
			//one input bus, same width, one/more output bus, bus has valid width
			else if (type.equals("splitter"))
			{
				if (getInputBlocks().length<1 || getInputBlocks()[0].bits!=bits) return false;
				return true;
			}
			//one/more input bus, same width
			else if (type.equals("multiplexor"))
			{
				if (getInputBlocks().length<1 || getInputBlocks()[0].bits!=bits) return false;
				if (name.equals("")) return false;
				return true;
			}
			//one/more input bus, same width, one address bus
			else if (type.equals("data_multiplexor"))
			{
				if (getInputBlocks().length<1 || getInputBlocks()[0].bits!=bits) return false;
				if (getAddressInputBlock()==null) return false;
				return true;
			}
			//one output bus, bus has valid width
			else if (type.equals("constant"))
			{
				if (getInputBlocks().length!=0) return false;
				if (name.equals("")||name.matches("[^0-9]+")) return false;
				return true;
			}
			//one input bus, same width
			else if (type.equals("control"))
			{
				if (getInputBlocks().length!=1 || getInputBlocks()[0].bits!=bits) return false;
				return true;
			}
			else
				return false;

		}
	}

	private class DatapathComponents extends AbstractGUI
	{
		private static final int WIDTH=300,HEIGHT=400;
		private JList componentlist;
		private String[] components={"register","memory","ports","register file","flag","ALU","adder","joiner","splitter","extender","multiplexor","data_multiplexor","decoder","inhibitor","lookup table","constant","control"};
		public JTextField bitfield;
		private DatapathComponents thisdatapathcomponents;
		public DatapathComponents(Computer computer)
		{
			super(computer,"Datapath Components",WIDTH,HEIGHT,false,true,false,false);
			thisdatapathcomponents=this;
			refresh();
		}
		public void closeGUI()
		{
			computer.datapathBuilder=null;
			if (computer.controlBuilder!=null) computer.controlBuilder.close();
		}
		public void constructGUI(GUIComponent g)
		{
			JButton button = new JButton("Close");
			button.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e)
			{
				frame.setVisible(false);
				computer.datapathBuilder.frame.setVisible(false);
				if (computer.computerGUI.singleFrame)
				{
					computer.computerGUI.removeComponent(computer.datapathBuilder);
					computer.computerGUI.removeComponent(thisdatapathcomponents);
				}
			} } );
			button.setBounds(10+200,20+30*0,90,20);
			g.add(button);
			button = new JButton("Save");
			button.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e)
			{
				System.out.println(dumpXML());
				JFileChooser fc = new JFileChooser();
				fc.setCurrentDirectory(new File("."));
				fc.showSaveDialog(null);
				File f = fc.getSelectedFile();
				if (f==null) return;
				String name=f.getAbsolutePath();
				try
				{
					PrintWriter p =new PrintWriter(name);
					p.println(dumpXML());
					p.close();
				}
				catch(IOException x)
				{
					System.out.println("Error creating file "+name);
				}
			} } );
			button.setBounds(10,20+30*0,90,20);
			g.add(button);	
			button = new JButton("Load");
			button.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e)
			{
				JFileChooser fc = new JFileChooser();
				fc.setCurrentDirectory(new File("."));
				fc.showOpenDialog(null);
				File f = fc.getSelectedFile();
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

				DatapathXMLParse xmlParse=new DatapathXMLParse(xml);
				for (int i=1; i<=xmlParse.highestBlockNumber(); i++)
					xmlParse.constructBlock(i);
				computer.datapathBuilder.repaint();
			} } );
			button.setBounds(10+100,20+30*0,90,20);
			g.add(button);
			button = new JButton("Control");
			button.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e)
			{
				computer.controlBuilder=new ControlBuilder(computer);			
			} } );
			button.setBounds(10+100,20+30*1,90,20);
			g.add(button);
			button = new JButton("Verify");
			button.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e)
			{
				verify();
			} } );
			button.setBounds(10,20+30*1,90,20);
			g.add(button);

			button = new JButton("Place");
			button.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e)
			{
				computer.datapathBuilder.place((String)componentlist.getSelectedValue());
			} } );
			button.setBounds(10,20+30*2,90,20);
			g.add(button);
			componentlist = new JList(components);
			JScrollPane scrollpane=new JScrollPane(componentlist);
			scrollpane.setBounds(10+100,20+30*2,200,20);
			g.add(scrollpane);
			bitfield = new JTextField("8");
			bitfield.setBounds(10+100+210,20+30*2,40,20);
			g.add(bitfield);
			JLabel label=new JLabel("bits:");
			label.setBounds(10+100+210,20+30*2-22,40,20);
			g.add(label);

			button = new JButton("Place Bus");
			button.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e)
			{
				computer.datapathBuilder.placewire();
			} } );
			button.setBounds(10,20+30*3,150,20);
			g.add(button);
			button = new JButton("Select Block");
			button.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e)
			{
				computer.datapathBuilder.select();
			} } );
			button.setBounds(10,20+30*4,150,20);
			g.add(button);
			button = new JButton("Unselect All");
			button.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e)
			{
				computer.datapathBuilder.unselectAll();
			} } );
			button.setBounds(10+160,20+30*4,150,20);
			g.add(button);
			button = new JButton("Delete");
			button.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e)
			{
				computer.datapathBuilder.delete();
			} } );
			button.setBounds(10,20+30*5,90,20);
			g.add(button);
			button = new JButton("Move");
			button.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e)
			{
				computer.datapathBuilder.move();
			} } );
			button.setBounds(10+100,20+30*5,90,20);
			g.add(button);
			button = new JButton("Name");
			button.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e)
			{
				computer.datapathBuilder.doname();
			} } );
			button.setBounds(10+200,20+30*5,90,20);
			g.add(button);
			button = new JButton("Set Value");
			button.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e)
			{
				computer.datapathBuilder.dovalue();
			} } );
			button.setBounds(10,20+30*6,150,20);
			g.add(button);
			button = new JButton("Zoom In");
			button.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e)
			{
				scaling+=0.2;
				computer.datapathBuilder.repaint();
			} } );
			button.setBounds(10,20+30*7,150,20);
			g.add(button);
			button = new JButton("Zoom Out");
			button.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e)
			{
				scaling-=0.2;
				if (scaling<1) scaling=1.0;
				computer.datapathBuilder.repaint();
			} } );
			button.setBounds(10+160,20+30*7,150,20);
			g.add(button);
		}
	}

	public class DatapathXMLParse
	{
		String[] xmlParts;
		public DatapathXMLParse(String xml)
		{
			ArrayList<String> parts=new ArrayList<String>();
			int c=0;
			String tag="";

			for (c=0; c<xml.length(); c++)
			{
				if (xml.charAt(c)=='<')
				{
					if (!isWhiteSpace(tag))
						parts.add(tag);
					tag="<";
				}
				else if (xml.charAt(c)=='>')
				{
					tag+=">";
					parts.add(tag);
					tag="";
				}
				else
					tag+=xml.charAt(c);
			}

			xmlParts=new String[parts.size()];
			for (int i=0; i<parts.size(); i++)
				xmlParts[i]=(String)parts.get(i);

			for (int i=0; i<parts.size(); i++)
				System.out.println(xmlParts[i]);
		}

		private boolean isWhiteSpace(String s)
		{
			for (int i=0; i<s.length(); i++)
			{
				if (s.charAt(i)!=' '&&s.charAt(i)!='\t'&&s.charAt(i)!='\n')
					return false;
			}
			return true;
		}

		public String[] extractBlock(int number)
		{
			int i,j;
			for(i=0; i<xmlParts.length; i++)
			{
				if (xmlParts[i].equals("<number>") && Integer.parseInt(xmlParts[i+1])==number)
					break;
			}
			if (i==xmlParts.length)
				return null;
			for (j=i-1; ; j++)
			{
				if (xmlParts[j].equals("</"+xmlParts[i-1].substring(1,xmlParts[i-1].length())))
					break;
			}
			String[] block=new String[j-i+2];
			for (int k=i-1; k<=j; k++)
				block[k-(i-1)]=xmlParts[k];
			return block;
		}

		public String extractField(String[] block, String field)
		{
			for (int i=0; i<block.length; i++)
			{
				if (block[i].equals(field))
					return block[i+1];
			}
			return null;
		}

		public int highestBlockNumber()
		{
			int number=0;
			for(int i=0; i<xmlParts.length; i++)
			{
				if (xmlParts[i].equals("<number>") && Integer.parseInt(xmlParts[i+1])>number)
					number=Integer.parseInt(xmlParts[i+1]);
			}
			return number;
		}

		public void constructBlock(int number)
		{
			String[] block=extractBlock(number);
			if (block==null) return;
			int bits=Integer.parseInt(extractField(block,"<bits>"));
			String type=block[0].substring(1,block[0].length()-1);

			Block b=new Block(type,bits);
			int x=Integer.parseInt(extractField(block,"<xcoordinate>"));
			int y=Integer.parseInt(extractField(block,"<ycoordinate>"));
			int x2=0,y2=0,entry=0,exit=0;
			if (extractField(block,"<xcoordinate2>")!=null)
				x2=Integer.parseInt(extractField(block,"<xcoordinate2>"));
			if (extractField(block,"<ycoordinate2>")!=null)
				y2=Integer.parseInt(extractField(block,"<ycoordinate2>"));
			if (extractField(block,"<entry>")!=null)
				entry=Integer.parseInt(extractField(block,"<entry>"));
			if (extractField(block,"<exit>")!=null)
				exit=Integer.parseInt(extractField(block,"<exit>"));
			if (type.equals("bus"))
				b.place(x,y,x2,y2,entry,exit);
			else
				b.place(x,y);
			if (type.equals("joiner")||type.equals("splitter"))
			{
				for (int i=0; i<block.length; i++)
				{
					if (block[i].length()>6 && block[i].substring(0,6).equals("<line "))
					{
						int j=Integer.parseInt(block[i].substring(6,block[i].length()-1));
						b.bus.put(new Integer(j),block[i+1]);
					}
				}
			}
			b.number=number;
			blocknumber=number+1;
			b.name=extractField(block,"<name>");
			if (b.name.equals("</name>"))
				b.name="";
		}
	}
}
