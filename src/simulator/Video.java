package simulator;

import java.awt.*;
import java.awt.event.*;
import java.util.Scanner;

public class Video extends IODevice
{
	byte[] vram;
	byte[] vramp1;
	byte[] vramp2;
	byte[] vramp3;
	byte[] vramp4;
//	VideoComponent videocomponent;
//	VideoGUI videoGUI;
	VideoKeyListener videoKeyListener;
	Computer computer;

	byte latch_p1, latch_p2, latch_p3, latch_p4;

	static final int VWIDTH = 80*9;
	static final int VHEIGHT = 480;
	static final int VOFFSET =  0;	//was 40

	//VGA registers
	private int sequencerRegisterIndex, graphicsRegisterIndex, attributeRegisterIndex, crtRegisterIndex;
	private int[] sequencerRegister, graphicsRegister, attributeRegister, crtRegister;
	private boolean attributeRegisterFlipFlop;

 	public void updateVideoWrite(int address, byte b)
	{
		if (address>=0xb8000 && address<=0xbffff)
		{
			vram[address-0xb8000]=b;
			if (computer.videoGUI!=null) computer.videoGUI.repaint();
		}
		else if (address>=0xa0000 && address<=0xaffff && (graphicsRegister[6]&0x8)==0)
		{
			byte bitmask=(byte)graphicsRegister[8];
			int writemode=graphicsRegister[5]&3;
			
			if (writemode==0)
			{
			if((sequencerRegister[2]&1)!=0)
				vramp1[address-0xa0000]=(byte)((~bitmask&latch_p1)|(bitmask&b));
			if((sequencerRegister[2]&2)!=0)
				vramp2[address-0xa0000]=(byte)((~bitmask&latch_p2)|(bitmask&b));
			if((sequencerRegister[2]&4)!=0)
				vramp3[address-0xa0000]=(byte)((~bitmask&latch_p3)|(bitmask&b));
			if((sequencerRegister[2]&8)!=0)
				vramp4[address-0xa0000]=(byte)((~bitmask&latch_p4)|(bitmask&b));
			}
			else if (writemode==2)
			{
				byte b_p1=((b&1)==0)? 0:(byte)0xff;
				byte b_p2=((b&2)==0)? 0:(byte)0xff;
				byte b_p3=((b&4)==0)? 0:(byte)0xff;
				byte b_p4=((b&8)==0)? 0:(byte)0xff;
				vramp1[address-0xa0000]=(byte)((~bitmask&latch_p1)|(bitmask&b_p1));
				vramp2[address-0xa0000]=(byte)((~bitmask&latch_p2)|(bitmask&b_p2));
				vramp3[address-0xa0000]=(byte)((~bitmask&latch_p3)|(bitmask&b_p3));
				vramp4[address-0xa0000]=(byte)((~bitmask&latch_p4)|(bitmask&b_p4));
			}

			if (computer.videoGUI!=null) computer.videoGUI.repaint();
		}
	}

	public void updateVideoRead(int address)
	{
		if (address>=0xa0000 && address<=0xaffff)
		{
			if((sequencerRegister[2]&1)!=0)
				latch_p1=vramp1[address-0xa0000];
			if((sequencerRegister[2]&2)!=0)
				latch_p2=vramp2[address-0xa0000];
			if((sequencerRegister[2]&4)!=0)
				latch_p3=vramp3[address-0xa0000];
			if((sequencerRegister[2]&8)!=0)
				latch_p4=vramp4[address-0xa0000];
		}
	}

	public void ioPortWriteByte(int address, byte data)
	{
		switch(address)
		{
			case 0x3b4: case 0x3d4:
				crtRegisterIndex=data; break;
			case 0x3b5: case 0x3d5:
				crtRegister[crtRegisterIndex]=data; break;
			case 0x3c0:
				if (!attributeRegisterFlipFlop) attributeRegisterIndex=data&0x3f;
				else attributeRegister[attributeRegisterIndex]=data;
				attributeRegisterFlipFlop=!attributeRegisterFlipFlop;
				break;
			case 0x3c4:
				sequencerRegisterIndex=data; break;
			case 0x3c5:
				sequencerRegister[sequencerRegisterIndex]=data; break;
			case 0x3ce:
				graphicsRegisterIndex=data; System.out.println("Indexing graphics control register "+data); break;
			case 0x3cf:
				graphicsRegister[graphicsRegisterIndex]=data; System.out.println("Writing "+data+" to graphics control register"); break;
		}
	}
	public byte ioPortReadByte(int address)
	{
		switch(address)
		{
			case 0x3b4: case 0x3d4:
				return (byte)crtRegisterIndex;
			case 0x3b5: case 0x3d5:
				return (byte)crtRegister[crtRegisterIndex];
			case 0x3c0:
				if (!attributeRegisterFlipFlop) return (byte)attributeRegisterIndex;
				else return 0;
			case 0x3c1:
				return (byte)attributeRegister[attributeRegisterIndex];
			case 0x3c4:
				return (byte)sequencerRegisterIndex;
			case 0x3c5:
				return (byte)sequencerRegister[sequencerRegisterIndex];
			case 0x3ce:
				return (byte)graphicsRegisterIndex;
			case 0x3cf:
				return (byte)graphicsRegister[graphicsRegisterIndex];
			case 0x3da:
				attributeRegisterFlipFlop=false; return 0;
			default:
				return 0;
		}
	}

	public void loadState(String state)
	{
		Scanner s=new Scanner(state);
		if (!s.next().equals("Video"))
		{
			System.out.println("Error in load state: Video expected");
			return;
		}
		for(int i=0; i<vram.length; i++)
			vram[i]=s.nextByte();
		for(int i=0; i<vramp1.length; i++)
			vramp1[i]=s.nextByte();
		for(int i=0; i<vramp2.length; i++)
			vramp2[i]=s.nextByte();
		for(int i=0; i<vramp3.length; i++)
			vramp3[i]=s.nextByte();
		for(int i=0; i<vramp4.length; i++)
			vramp4[i]=s.nextByte();
		for(int i=0; i<sequencerRegister.length; i++)
			sequencerRegister[i]=s.nextInt();
		for(int i=0; i<graphicsRegister.length; i++)
			graphicsRegister[i]=s.nextInt();
		for(int i=0; i<attributeRegister.length; i++)
			attributeRegister[i]=s.nextInt();
		for(int i=0; i<crtRegister.length; i++)
			crtRegister[i]=s.nextInt();
		latch_p1=s.nextByte();
		latch_p2=s.nextByte();
		latch_p3=s.nextByte();
		latch_p4=s.nextByte();
		sequencerRegisterIndex=s.nextInt();
		graphicsRegisterIndex=s.nextInt();
		attributeRegisterIndex=s.nextInt();
		crtRegisterIndex=s.nextInt();
		attributeRegisterFlipFlop=s.nextInt()==1;
	}
	
	public String saveState()
	{
		StringBuilder state=new StringBuilder();
		state.append("Video ");
		for(int i=0; i<vram.length; i++)
			state.append(vram[i]+" ");
		for(int i=0; i<vramp1.length; i++)
			state.append(vramp1[i]+" ");
		for(int i=0; i<vramp2.length; i++)
			state.append(vramp2[i]+" ");
		for(int i=0; i<vramp3.length; i++)
			state.append(vramp3[i]+" ");
		for(int i=0; i<vramp4.length; i++)
			state.append(vramp4[i]+" ");
		for (int i=0; i<sequencerRegister.length; i++)
			state.append(sequencerRegister[i]+" ");
		for (int i=0; i<graphicsRegister.length; i++)
			state.append(graphicsRegister[i]+" ");
		for (int i=0; i<attributeRegister.length; i++)
			state.append(attributeRegister[i]+" ");
		for (int i=0; i<crtRegister.length; i++)
			state.append(crtRegister[i]+" ");
		state.append(latch_p1+" ");
		state.append(latch_p2+" ");
		state.append(latch_p3+" ");
		state.append(latch_p4+" ");
		state.append(sequencerRegisterIndex+" ");
		state.append(graphicsRegisterIndex+" ");
		state.append(attributeRegisterIndex+" ");
		state.append(crtRegisterIndex+" ");
		state.append(attributeRegisterFlipFlop?1:0);

		return state.toString();
	}
	
	public Video(Computer computer)
	{
		this.computer=computer;
		
		vram = new byte[0x10000];
		vramp1 = new byte[0x10000];
		vramp2 = new byte[0x10000];
		vramp3 = new byte[0x10000];
		vramp4 = new byte[0x10000];

		sequencerRegister=new int[256];
		graphicsRegister=new int[256];
		attributeRegister=new int[256];
		crtRegister=new int[256];

		computer.ioports.requestPorts(this,new int[]{0x3b4,0x3b5,0x3c0,0x3c1,0x3c4,0x3c5,0x3ce,0x3cf,0x3d4,0x3d5,0x3da},"Video",new String[]{"CRT Index","CRT Register","Attribute Index","Attribute Register","Sequencer Index","Sequencer Register","Graphics Index","Graphics Register","CRT Index","CRT Register","Attribute Flip-Flop"});

//		videoGUI=new VideoGUI();
/*

		JFrame f = new JFrame();
		f.setSize(VWIDTH+VOFFSET+VOFFSET,VHEIGHT+VOFFSET+VOFFSET+VOFFSET);
		videocomponent=new VideoComponent();
		f.add(videocomponent);
		if (computer.applet==null) f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setTitle("The Screen");
		f.addKeyListener(new VideoKeyListener());

		if (!computer.computerGUI.singleFrame)
			f.setVisible(true);
		else
			computer.computerGUI.addComponent(videocomponent);*/
	}
/*	public class VideoComponent extends JComponent
	{
		public void paintComponent(Graphics g)
		{
			g.setColor(Color.BLACK);
			g.fillRect(0,0,VWIDTH+VOFFSET+VOFFSET,VHEIGHT+VOFFSET+VOFFSET+VOFFSET);
			if ((graphicsRegister[6]&0x8)==0)
				paintScreenGraphics640x480(g,VOFFSET,VOFFSET);
			else if ((graphicsRegister[6]&1)==0)
				paintScreenText80x25(g,VOFFSET,VOFFSET);
			else
				paintScreenGraphics640x200(g,VOFFSET,VOFFSET);

		}
	}*/

	public void setupGUI(VideoGUI videoGUI)
	{
		videoKeyListener=new VideoKeyListener();
		videoGUI.guiComponent.addKeyListener(videoKeyListener);
		videoGUI.guiComponent.setFocusTraversalKeysEnabled(false);
		videoGUI.guiComponent.requestFocus();
	}

	public void paintScreen(Graphics g)
	{
		g.setColor(Color.BLACK);
		g.fillRect(0,0,VWIDTH+VOFFSET+VOFFSET,VHEIGHT+VOFFSET+VOFFSET+VOFFSET);
		if ((graphicsRegister[6]&0x8)==0)
			paintScreenGraphics640x480(g,VOFFSET,VOFFSET);
		else if ((graphicsRegister[6]&1)==0)
			paintScreenText80x25(g,VOFFSET,VOFFSET);
		else
			paintScreenGraphics640x200(g,VOFFSET,VOFFSET);
	}

	public void paintScreenText80x25(Graphics g, int XOFFSET, int YOFFSET)
	{
		g.setFont(new Font("monospaced", 0, 12));
		for (int y=0; y<25; y++)
		{
			for (int x=0; x<80; x++)
			{
				//calculate text coordinates in memory
				int my=y*80*2;
				int mx=(x*2)%(80*2);

				//set background color
				g.setColor(textColor[((0xff&vram[my+mx+1])>>>4)&7]);
				g.fillRect(x*9+XOFFSET,y*16+YOFFSET,9,16);
				//set foreground color
				g.setColor(textColor[(0xff&vram[my+mx+1])%16]);
				char c = (char)vram[my+mx];
				if (c>=128) c=' ';
				g.drawString(""+(char)c,x*9+XOFFSET,y*16+YOFFSET+13);
			}
		}
	}

	public void paintScreenGraphics320x200(Graphics g, int XOFFSET, int YOFFSET)
	{
		for (int y=0; y<200; y++)
		{
			for (int x=0; x<320; x++)
			{
				//calculate pixel coordinates in memory
				int my;
				if(y%2==0)
					my=(y/2)*(320/4);
				else
					my=(y/2)*(320/4)+8192;
				int mx=(x/4)%(320/4);
				//extract pixel
				int p = ((vram[my+mx]&0xff)>>>(0x3-(x&0x3)))&0x3;
				//determine color
				//assume palette 0 for now, background black
				Color c = new Color[]{Color.BLACK, Color.GREEN, Color.RED, Color.YELLOW}[p];
				//color pixel
				g.setColor(c);
				int SCALING=2;
				g.fillRect(x*SCALING+XOFFSET,y*SCALING+YOFFSET,SCALING,SCALING);
			}
		}
	}

	public void paintScreenGraphics640x200(Graphics g, int XOFFSET, int YOFFSET)
	{
		for (int y=0; y<200; y++)
		{
			for (int x=0; x<640; x++)
			{
				//calculate pixel coordinates in memory
				int my;
				if(y%2==0)
					my=(y/2)*(640/8);
				else
					my=(y/2)*(640/8)+8192;
				int mx=(x/8)%(640/8);
				//extract pixel
				int p = ((vram[my+mx]&0xff)>>>(0x7-(x&0x7)))&0x1;
				//color is either white or black
				if (p==0)
					g.setColor(Color.BLACK);
				else
					g.setColor(Color.WHITE);
				g.fillRect(x*1+XOFFSET,y*2+YOFFSET,1,2);
			}
		}
	}

	public void paintScreenGraphics640x480(Graphics g, int XOFFSET, int YOFFSET)
	{
		for (int y=0; y<480; y++)
		{
			for (int x=0; x<640; x++)
			{
				//calculate pixel coordinates in memory
				int my = y*(640/8);
				int mx = (x/8)%(640/8);
				int p1 = ((vramp1[my+mx]&0xff)>>>(0x7-(x&0x7)))&1;
				int p2 = ((vramp2[my+mx]&0xff)>>>(0x7-(x&0x7)))&1;
				int p3 = ((vramp3[my+mx]&0xff)>>>(0x7-(x&0x7)))&1;
				int p4 = ((vramp4[my+mx]&0xff)>>>(0x7-(x&0x7)))&1;
				int p=p4*8+p3*4+p2*2+p1;
				g.setColor(textColor[p]);
				g.fillRect(x*1+XOFFSET,y*1+YOFFSET,1,1);
			}
		}
	}

	private static final Color[] textColor=new Color[]
	{
		new Color(0,0,0),new Color(0,0,0xaa),new Color(0,0xaa,0),new Color(0,0xaa,0xaa),new Color(0xaa,0,0),new Color(0xaa,0,0xaa),new Color(0xaa,0x55,0),new Color(0xaa,0xaa,0xaa),new Color(0x55,0x55,0x55),new Color(0x55,0x55,0xff),new Color(0x55,0xff,0x55),new Color(0x55,0xff,0xff),new Color(0xff,0x55,0x55),new Color(0xff,0x55,0xff),new Color(0xff,0xff,0x55),new Color(0xff,0xff,0xff)
	};

	public class VideoKeyListener implements KeyListener
	{
		public void keyPressed(KeyEvent e)
		{
			if (computer.keyboardGUI!=null)
				computer.keyboardGUI.keyboardKeyListener.keyPressed(e);
		}
		public void keyReleased(KeyEvent e)
		{
			if (computer.keyboardGUI!=null)
				computer.keyboardGUI.keyboardKeyListener.keyReleased(e);
		}
		public void keyTyped(KeyEvent e) { }
	}
}
