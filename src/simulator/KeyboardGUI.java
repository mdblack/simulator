/*
KeyboardGUI.java
Michael Black, 6/10

Visual representation of a keyboard
*/

package simulator;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class KeyboardGUI extends AbstractGUI
{
	private static final int MARGIN=20,KGAP=3,BUFFER_WIDTH=80;

	private static final Color COLOR_KEY_RELEASED=new Color(220,220,220);
	private static final Color COLOR_KEY_PRESSED=new Color(150,150,150);

	public static final String[][] keyNames = new String[][]
	{
		{"ESC","F1","F2","F3","F4","F5","F6","F7","F8","F9","F10","F11","F12","SCRLL"},
		{"`","1","2","3","4","5","6","7","8","9","0","-","=","BS"},
		{"TAB","Q","W","E","R","T","Y","U","I","O","P","[","]","\\"},
		{"CAPS","A","S","D","F","G","H","J","K","L",";","'","RETURN"},
		{"SHIFT","Z","X","C","V","B","N","M",",",".","/","SHIFT","PG UP","PG DN"},
		{"CTRL","ALT","SPACE","LEFT","RIGHT","UP","DOWN","INS","DEL","HOME","END"}
	};
	public static final int[][] javaKeyCodes = new int[][]
	{	{KeyEvent.VK_ESCAPE,KeyEvent.VK_F1,KeyEvent.VK_F2,KeyEvent.VK_F3,KeyEvent.VK_F4,KeyEvent.VK_F5,KeyEvent.VK_F6,KeyEvent.VK_F7,KeyEvent.VK_F8,KeyEvent.VK_F9,KeyEvent.VK_F10,KeyEvent.VK_F11,KeyEvent.VK_F12,KeyEvent.VK_SCROLL_LOCK},
{KeyEvent.VK_BACK_QUOTE,KeyEvent.VK_1,KeyEvent.VK_2,KeyEvent.VK_3,KeyEvent.VK_4,KeyEvent.VK_5,KeyEvent.VK_6,KeyEvent.VK_7,KeyEvent.VK_8,KeyEvent.VK_9,KeyEvent.VK_0,KeyEvent.VK_MINUS,KeyEvent.VK_EQUALS,KeyEvent.VK_BACK_SPACE},
{KeyEvent.VK_TAB,KeyEvent.VK_Q,KeyEvent.VK_W,KeyEvent.VK_E,KeyEvent.VK_R,KeyEvent.VK_T,KeyEvent.VK_Y,KeyEvent.VK_U,KeyEvent.VK_I,KeyEvent.VK_O,KeyEvent.VK_P,KeyEvent.VK_OPEN_BRACKET,KeyEvent.VK_CLOSE_BRACKET,KeyEvent.VK_BACK_SLASH},
{KeyEvent.VK_CAPS_LOCK,KeyEvent.VK_A,KeyEvent.VK_S,KeyEvent.VK_D,KeyEvent.VK_F,KeyEvent.VK_G,KeyEvent.VK_H,KeyEvent.VK_J,KeyEvent.VK_K,KeyEvent.VK_L,KeyEvent.VK_SEMICOLON,KeyEvent.VK_QUOTE,KeyEvent.VK_ENTER},
{KeyEvent.VK_SHIFT,KeyEvent.VK_Z,KeyEvent.VK_X,KeyEvent.VK_C,KeyEvent.VK_V,KeyEvent.VK_B,KeyEvent.VK_N,KeyEvent.VK_M,KeyEvent.VK_COMMA,KeyEvent.VK_PERIOD,KeyEvent.VK_SLASH,KeyEvent.VK_SHIFT,KeyEvent.VK_PAGE_UP,KeyEvent.VK_PAGE_DOWN},
{KeyEvent.VK_CONTROL,KeyEvent.VK_ALT,KeyEvent.VK_SPACE,KeyEvent.VK_LEFT,KeyEvent.VK_RIGHT,KeyEvent.VK_UP,KeyEvent.VK_DOWN,KeyEvent.VK_INSERT,KeyEvent.VK_DELETE,KeyEvent.VK_HOME,KeyEvent.VK_END}
	};
	public static final byte[][] keyCodes = new byte[][]
	{
		{(byte)0x01,(byte)0x3b,(byte)0x3c,(byte)0x3d,(byte)0x3e,(byte)0x3f,(byte)0x40,(byte)0x41,(byte)0x42,(byte)0x43,(byte)0x44,(byte)0x57,(byte)0x58,(byte)0x46},
		{(byte)0x29,(byte)0x02,(byte)0x03,(byte)0x04,(byte)0x05,(byte)0x06,(byte)0x07,(byte)0x08,(byte)0x09,(byte)0x0a,(byte)0x0b,(byte)0x0c,(byte)0x0d,(byte)0x0e},
		{(byte)0x0f,(byte)0x10,(byte)0x11,(byte)0x12,(byte)0x13,(byte)0x14,(byte)0x15,(byte)0x16,(byte)0x17,(byte)0x18,(byte)0x19,(byte)0x1a,(byte)0x1b,(byte)0x2b},
		{(byte)0x3a,(byte)0x1e,(byte)0x1f,(byte)0x20,(byte)0x21,(byte)0x22,(byte)0x23,(byte)0x24,(byte)0x25,(byte)0x26,(byte)0x27,(byte)0x28,(byte)0x1c},
		{(byte)0x2a,(byte)0x2c,(byte)0x2d,(byte)0x2e,(byte)0x2f,(byte)0x30,(byte)0x31,(byte)0x32,(byte)0x33,(byte)0x34,(byte)0x35,(byte)0x36,(byte)0x49,(byte)0x51},
		{(byte)0x1d,(byte)0x38,(byte)0x39,(byte)0x4b,(byte)0x4d,(byte)0x48,(byte)0x50,(byte)0x52,(byte)0x53,(byte)0x47,(byte)0x4f}
	};

	public static int W=600,H=160;
	private int KEYBOARD_WIDTH=W-BUFFER_WIDTH;
	private int KEY_WIDTH_BASE=(KEYBOARD_WIDTH-2*MARGIN)/15;
	private int KEYBOARD_HEIGHT=H;
	private int KEY_HEIGHT=(KEYBOARD_HEIGHT-2*MARGIN)/7;
	private static final int NUMBER_OF_KEYS=keyNames[0].length+keyNames[1].length+keyNames[2].length+keyNames[3].length+keyNames[4].length+keyNames[5].length;

	private String status="";

//	private int slowModeSleepDuration=200;

	private Key[] keys = new Key[NUMBER_OF_KEYS];
	public KeyboardBufferComponent keyboardbuffercomponent;
	public KeyboardKeyListener keyboardKeyListener;

	public void closeGUI()
	{
		computer.keyboardGUI=null;
	}
	
	public KeyboardGUI(Computer computer)
	{
		super(computer,"Keyboard",W,H,computer.computerGUI.singleFrame? false:true,false,false, false);

		int k=0;
		for (int y=0; y<keyNames.length; y++)
		{
			for (int x=0; x<keyNames[y].length; x++)
			{
				boolean sticky=false;
				if (keyNames[y][x].equals("CTRL") || keyNames[y][x].equals("SHIFT") || keyNames[y][x].equals("ALT"))
					sticky=true;
				int width=1;
				if (keyNames[y][x].equals("SPACE"))
					width=4;
				if (keyNames[y][x].equals("RETURN"))
					width=2;
				keys[k]=new Key(keyNames[y][x],keyCodes[y][x],javaKeyCodes[y][x],sticky,width);
				if (x==0)
					keys[k].setPosition(MARGIN,MARGIN+y*(KEY_HEIGHT+KGAP));
				else
					keys[k].setPosition(keys[k-1].xend+KGAP,MARGIN+y*(KEY_HEIGHT+KGAP));
				k++;
			}
		}
		refresh();

		keyboardbuffercomponent=new KeyboardBufferComponent();

//		if (KEYBOARD_WIDTH>4*BUFFER_WIDTH)
	//	{
			JScrollPane bufferPane = new JScrollPane(keyboardbuffercomponent);
			add(bufferPane);
			guiComponent.setBounds(0,0,KEYBOARD_WIDTH,KEYBOARD_HEIGHT);
			bufferPane.setBounds(KEYBOARD_WIDTH,0,BUFFER_WIDTH,KEYBOARD_HEIGHT);
		//}
		//else
		//{
			//KEYBOARD_WIDTH+=BUFFER_WIDTH;
			//guiComponent.setBounds(0,BUTTONROWSIZE,KEYBOARD_WIDTH,KEYBOARD_HEIGHT);
		//}

		keyboardKeyListener=new KeyboardKeyListener();
		guiComponent.addKeyListener(keyboardKeyListener);
		guiComponent.setFocusTraversalKeysEnabled(false);
		guiComponent.requestFocus();
	}

	public void doPaint(Graphics g)
	{
		g.setColor(Color.WHITE);
		g.fillRect(0,0,KEYBOARD_WIDTH,KEYBOARD_HEIGHT);
		for (int i=0; i<keys.length; i++)
			keys[i].draw(g);
		setStatusLabel("");
	}

	public void repaint()
	{
		if (guiComponent!=null)
			guiComponent.repaint();
	}

	public void mousePress(MouseEvent e)
	{
		for(int i=0; i<keys.length; i++)
			keys[i].mousePress(e.getX(),e.getY());
		repaint();
	}

	public void mouseRelease(MouseEvent e)
	{
		for(int i=0; i<keys.length; i++)
			keys[i].mouseRelease(e.getX(),e.getY());
		repaint();
	}

	public void mouseMove(MouseEvent e)
	{
		String label="";
		for(int i=0; i<keys.length; i++)
		{
			label = keys[i].giveInformation(e.getX(),e.getY());
			if (!label.equals(""))
				break;
		}
		setStatusLabel(label);
	}

	public void setStatusLabel(String label)
	{
		if(status.equals(""))
			setDefaultStatus();
		else
			this.status=label;
		super.setStatusLabel(label);
	}

	//display the status and mode registers
	public void setDefaultStatus()
	{
		status="STATUS: ";
		if ((computer.keyboard.status&0x01)!=0)
			status+="output buffer full, ";
		if ((computer.keyboard.status&0x02)!=0)
			status+="input buffer full, ";
		if ((computer.keyboard.status&0x04)!=0)
			status+="self test successful, ";
		if ((computer.keyboard.status&0x08)!=0)
			status+="last command was a write, ";
		if ((computer.keyboard.status&0x10)==0)
			status+="locked, ";
		if ((computer.keyboard.status&0x40)!=0)
			status+="timeout, ";
		if ((computer.keyboard.status&0x80)!=0)
			status+="parity error, ";
		if ((computer.keyboard.mode&0x1)==0)
			status+="interrupts disabled, ";
		if ((computer.keyboard.mode&0x10)!=0)
			status+="disabled, ";
	}

	public void issueScript(String command)
	{
		for (int i=0; i<command.length(); i++)
		{
			String c = ""+command.charAt(i);
			c=c.toUpperCase();
			if (c.equals(" "))
				c="SPACE";
			if (c.equals("\n"))
				c="RETURN";

			//find corresponding key
			for (int j=0; j<keyNames.length; j++)
			{
				for (int k=0; k<keyNames[j].length; k++)
				{
					if (keyNames[j][k].equals(c))
					{
						//get code
						byte code = keyCodes[j][k];
						//press and release key
						computer.keyboard.keyPressed(code);
						computer.keyboard.keyReleased(code);
					}
				}
			}
			
			try{ Thread.sleep(100);} catch(Exception e){}
		}
		//issue ENTER
		computer.keyboard.keyPressed((byte)0x1c);
		computer.keyboard.keyReleased((byte)0x1c);
	}

	public class KeyboardKeyListener implements KeyListener
	{
		public void keyPressed(KeyEvent e)
		{
			int keycode=e.getKeyCode();

			//if pause key, switch to debug mode
			if (keycode==KeyEvent.VK_PAUSE)
			{
				System.out.println("Switching to debug mode");
				computer.computerGUI.pause();
//				computer.debugMode=true;
				return;
			}

			for (int i=0; i<keys.length; i++)
				keys[i].keyboardPress(keycode);
			repaint();
		}
		public void keyReleased(KeyEvent e)
		{
			int keycode=e.getKeyCode();
			for (int i=0; i<keys.length; i++)
				keys[i].keyboardRelease(keycode);
			repaint();
		}
		public void keyTyped(KeyEvent e) { }
	}

	public class KeyboardBufferComponent extends JComponent
	{
		int fontsize=BUFFER_WIDTH/6;

		public Dimension getPreferredSize()
		{
			return new Dimension(1,Keyboard.QUEUE_SIZE*(fontsize+1));
		}

		public void update()
		{
			this.repaint();
			sleep();
		}

		public void paintComponent(Graphics g)
		{
			g.setColor(Color.WHITE);
			g.fillRect(0,0,BUFFER_WIDTH,Keyboard.QUEUE_SIZE*(fontsize+1));
			g.setColor(Color.BLACK);
			g.drawLine(0,0,0,Keyboard.QUEUE_SIZE*(fontsize+1));

			for (int i=0; i<Keyboard.QUEUE_SIZE; i++)
				g.drawLine(0,(fontsize+1)*i,BUFFER_WIDTH,(fontsize+1)*i);

			if (computer.keyboard==null)
				return;

			int row=0;
			for (int p=computer.keyboard.buffer.readPosition; p!=computer.keyboard.buffer.writePosition; p=(p==Keyboard.QUEUE_SIZE-1)?0:p+1)
			{ 
				g.setFont(new Font("Dialog",Font.PLAIN,fontsize));
				g.setColor(Color.BLACK);

				String label="";
				//check if code is a regular key
				for (int i=0; i<keys.length; i++)
				{
					if ((keys[i].keyCode&0xff)==(computer.keyboard.buffer.data[p]&0x7f))
					{
						label=keys[i].label;
						g.setColor(Color.BLUE);
						break;
					}
					if ((keys[i].keyCode&0xff)==(computer.keyboard.buffer.data[p]|0x80))
					{
						label=keys[i].label;
						g.setColor(Color.YELLOW);
						break;
					}
				}
				if (label.equals(""))
				{
					int i=computer.keyboard.buffer.data[p]&0xff;
					switch(i)
					{
						case 0x20: label="mode"; break;
						case 0x60: case 0xd1: case 0xd2: label="write"; break;
						case 0xa1: label="version"; break;
						case 0xaa: label="selftest"; break;
						case 0xc0: label="inport"; break;
						case 0xd0: label="outport"; break;
						case 0xad: label="disable"; break;
						case 0xae: label="enable"; break;
						case 0xfe: label="resend"; break;
						case 0xfa: label="ack"; break;
						default: label=""+i; break;
					}
					g.setColor(Color.RED);
				}
				g.drawString(label,1,(fontsize+1)*(++row)-1);
			}
		}
	}

	public class Key
	{
		private String label;
		private byte keyCode;
		private int javaKeyCode;
		private boolean sticky;
		private int width;
		private boolean pressed;
		public int xstart,xend,ystart,yend;

		public Key(String label, byte keyCode, int javaKeyCode, boolean sticky, int width)
		{
			this.label=label;
			this.keyCode=keyCode;
			this.javaKeyCode=javaKeyCode;
			this.sticky=sticky;
			this.width=width;
			pressed=false;
		}
		public void setPosition(int x, int y)
		{
			xstart=x;
			xend=x+width*KEY_WIDTH_BASE;
			if (width>1)
				xend+=(width-1)*KGAP;
			ystart=y;
			yend=y+KEY_HEIGHT;
		}
		private boolean isKey(int x, int y)
		{
			if (x>=xstart && x<xend && y>=ystart && y<yend)
				return true;
			return false;
		}
		public void press()
		{
			pressed=true;
			if (computer.keyboard!=null)
			{
				computer.keyboard.keyPressed(keyCode);
				setStatusLabel(label+" pressed");
			}
		}
		public void release()
		{
			pressed=false;
			if (computer.keyboard!=null)
			{
				computer.keyboard.keyReleased(keyCode);
				setStatusLabel(label+" released");
			}
		}
		public void draw(Graphics g)
		{
			if(!pressed)
				g.setColor(COLOR_KEY_RELEASED);
			else
				g.setColor(COLOR_KEY_PRESSED);
			g.fillRect(xstart,ystart,xend-xstart,yend-ystart);
			g.setColor(Color.BLACK);
			g.drawRect(xstart,ystart,xend-xstart,yend-ystart);

			g.setColor(Color.BLACK);
			int fontsize=1+KEY_HEIGHT/2;
			int xoffset=2*fontsize/3*label.length()/2;
			int yoffset=fontsize/2;
			g.setFont(new Font("Dialog",Font.PLAIN,fontsize));
			g.drawString(label,xstart+((xend-xstart)/2)-xoffset,ystart+((yend-ystart)/2)+yoffset);
		}

		//called on all keys at once
		public void mousePress(int x, int y)
		{
			//if sticky and pressed, release it when another key is pressed
			if (sticky && pressed)
				release();
			else if (isKey(x,y))
				press();
		}
		//called on all keys at once
		public void mouseRelease(int x, int y)
		{
			if (!sticky && isKey(x,y))
				release();
		}
		//called on all keys at once
		public void keyboardPress(int keyCode)
		{
			if (this.javaKeyCode==keyCode)
				press();
		}
		//called on all keys at once
		public void keyboardRelease(int keyCode)
		{
			if (this.javaKeyCode==keyCode)
				release();
		}
		//called on all keys at once
		public String giveInformation(int x, int y)
		{
			if (isKey(x,y))
				return label+" key produces key code "+keyCode;
			else
				return "";
		}
	}
}
