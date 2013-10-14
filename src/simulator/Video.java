/*package simulator;

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
	static final int VRAM_SIZE=0x10000;

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
		
		vram = new byte[VRAM_SIZE];
		vramp1 = new byte[VRAM_SIZE];
		vramp2 = new byte[VRAM_SIZE];
		vramp3 = new byte[VRAM_SIZE];
		vramp4 = new byte[VRAM_SIZE];

		sequencerRegister=new int[256];
		graphicsRegister=new int[256];
		attributeRegister=new int[256];
		crtRegister=new int[256];

		computer.ioports.requestPorts(this,new int[]{0x3b4,0x3b5,0x3c0,0x3c1,0x3c4,0x3c5,0x3ce,0x3cf,0x3d4,0x3d5,0x3da},"Video",new String[]{"CRT Index","CRT Register","Attribute Index","Attribute Register","Sequencer Index","Sequencer Register","Graphics Index","Graphics Register","CRT Index","CRT Register","Attribute Flip-Flop"});
	}

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
			paintScreenGraphics640x480(g);
		else if ((graphicsRegister[6]&1)==0)
			paintScreenText80x25(g);
		else
			paintScreenGraphics640x200(g);
	}

	public void paintScreenText80x25(Graphics g)
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
				g.fillRect(x*9+VOFFSET,y*16+VOFFSET,9,16);
				//set foreground color
				g.setColor(textColor[(0xff&vram[my+mx+1])%16]);
				char c = (char)vram[my+mx];
				if (c>=128) c=' ';
				g.drawString(""+(char)c,x*9+VOFFSET,y*16+VOFFSET+13);
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

	public void paintScreenGraphics640x200(Graphics g)
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
				g.fillRect(x*1+VOFFSET,y*2+VOFFSET,1,2);
			}
		}
	}

	public void paintScreenGraphics640x480(Graphics g)
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
				g.fillRect(x*1+VOFFSET,y*1+VOFFSET,1,1);
			}
		}
	}

	
	public static final int TEXTMODE=0,V640480=1,V640200=2;
	public int videoResolution()
	{
		if ((graphicsRegister[6]&0x8)==0)		//640x480?
			return V640480;
		else if ((graphicsRegister[6]&1)==0)	//text
			return TEXTMODE;
		return V640200;							//default to 640x200
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
*/

package simulator;

import java.awt.*;
import java.awt.event.*;
import java.util.Scanner;
import java.lang.*;

public class Video extends IODevice
{
	//////WCP additions/////////////////////////
	/* Source: FreeVGA
	 * 
	 * There is a lot of redundancy in this implentation. I'm doing a lot of
	 * unnecessary bit manipulation. This might make it less efficient and pretty
	 * overbuilt, but should prevent errors sneaking in.
	 * 
	 * Interfaces should be implemented pretty accurately. I removed the system of
	 * keeping the registers in arrays, and instead gave them their own variables.
	 * Accessing them via register indices uses a getter and setter now.
	 * 
	 * One downside of this approach is that some registers aren't actually stored
	 * as bytes. In those cases, getters/setters use the actually boolean and byte
	 * values that compose the register. On reads, they are built from the
	 * attributes they represent. On writes, the input is dissected, and the
	 * attributes are stored. This is actually more accurate of how the chips work,
	 * but might make it difficult to display in the simulator's debugging GUI.
	 * 
	 * The code could be greatly improved by creating a RegisterGroup class, and
	 * extending it to make the attribute, graphics, etc register groups.
	 * 
	 */
	
	
	/*
	 * DAC Memory Structure
	 * 
	 * Quotes:
	 * "The VGA's DAC subsystem accepts an 8 bit input from the attribute subsystem
	 * and outputs an analog signal that is presented to the display circuitry.
	 * Internally it contains 256 18-bit memory locations that store 6 bits each
	 * of red, blue, and green signal levels which have values ranging from 0
	 * (minimum intensity) to 63 (maximum intensity.) The DAC hardware takes the
	 * 8-bit value from the attribute subsystem and uses it as an index into the
	 * 256 memory locations and obtains a red, green, and blue triad and produces
	 * the necessary output."
	 * 
	 * Implementation Notes:
	 *   The priority is to preserve the structure as clearly as possible, even at
	 * the expense of some efficiency. Therefore, instead of using integers,
	 * clever bitwise manipulation, or Color objects, the DAC memory will be a two
	 * dimensional array of bytes (256 rows, 3 columns). The bytes will be reduced
	 * to 6 bits each on retrieval. This is less efficient than simply storing
	 * Color objects, but means we can accurately recreate the DAC interface.
	 */
	 
	 byte[][] DAC_memory = new byte[256][3];
	 
	/* DAC Registers (aka Color Registers)
	 * Quote:
	 * "The DAC's primary host interface (there may be a secondary non-VGA
	 * compatible access method) is through a set of four external registers
	 * containing the DAC Write Address, the DAC Read Address, the DAC Data, and
	 * the DAC State fields. The DAC memory is accessed by writing an index value
	 * to the DAC Write Address field for write operations, and to the DAC Read
	 * Address field for read operations. Then reading or writing the DAC Data
	 * field, depending on the selected operation, three times in succession
	 * returns 3 bytes, each containing 6 bits of red, green, and blue intensity
	 * values, with red being the first value and blue being the last value
	 * read/written. The read or write index then automatically increments such
	 * that the next entry can be read without having to reprogramming the address.
	 * In this way, the entire DAC memory can be read or written in 768 consecutive
	 * I/O cycles to/from the DAC Data field. The DAC State field reports whether
	 * the DAC is setup to accept reads or writes next."
	 * 
	 * Notes:
	 *   VGA chips are annoyingly inconsistent. For example, you are *supposed* to
	 * write to the write register before doing a write to the data register,
	 * but you can. If you do, the result depends on the implementation.
	 *   I have no idea what should be done with the state register if a write is
	 * made to the data register while in read mode, or if I should even allow it.
	 */
	
	byte DAC_stateRegister;	// 2 bits. Also useless.
	byte DAC_memoryIndex;	// 8 bits
	byte DAC_colorIndex;	// 0: red, 1: green, 2: blue
	
	public void writeDACWriteRegister(byte b) {
		DAC_memoryIndex = b;
		DAC_stateRegister = (byte)3; // 0b11 indicates writing
	}
	
	public byte readDACWriteRegister() {
		return DAC_memoryIndex; // Inconsistent in many VGA chips
	}
	
	public void writeDACReadRegister(byte b) {
		DAC_memoryIndex = b;
		DAC_stateRegister = (byte)0; // 0b00 indicates writing
	}
	
	// No reading DAC's read register
	
	public void writeDACDataRegister(byte b) {
		DAC_memory[DAC_memoryIndex & 0xFF][DAC_colorIndex] = (byte)(b & 0x3F);
		DAC_colorIndex++;
		if (DAC_colorIndex == 3) {
			DAC_colorIndex = 0;
			DAC_memoryIndex++;
		}
	}
	
	public byte readDACDataRegister() {
		byte dataRegister = (byte)(DAC_memory[DAC_memoryIndex & 0xFF][DAC_colorIndex] & 0x3f);
		DAC_colorIndex++;
		if (DAC_colorIndex == 3) {
			DAC_colorIndex = 0;
			DAC_memoryIndex++;
		}
		return dataRegister;
	}
	
	public byte readDACStateRegister() {
		return DAC_stateRegister;
	}
	
	public void initStandardPalette() {
		int[] standard = new int[] {
			0x000000, 0x0000a8, 0x00a800, 0x00a8a8, 0xa80000, 0xa800a8, 0xa85400, 0xa8a8a8,
			0x545454, 0x5454fc, 0x54fc54, 0x54fcfc, 0xfc5454, 0xfc54fc, 0xfcfc54, 0xfcfcfc,
			0x000000, 0x141414, 0x202020, 0x2c2c2c, 0x383838, 0x444444, 0x505050, 0x606060,
			0x707070, 0x808080, 0x909090, 0xa0a0a0, 0xb4b4b4, 0xc8c8c8, 0xe0e0e0, 0xfcfcfc,
			0x0000fc, 0x4000fc, 0x7c00fc, 0xbc00fc, 0xfc00fc, 0xfc00bc, 0xfc007c, 0xfc0040,
			0xfc0000, 0xfc4000, 0xfc7c00, 0xfcbc00, 0xfcfc00, 0xbcfc00, 0x7cfc00, 0x40fc00,
			0x00fc00, 0x00fc40, 0x00fc7c, 0x00fcbc, 0x00fcfc, 0x00bcfc, 0x007cfc, 0x0040fc,
			0x7c7cfc, 0x9c7cfc, 0xbc7cfc, 0xdc7cfc, 0xfc7cfc, 0xfc7cdc, 0xfc7cbc, 0xfc7c9c,
			0xfc7c7c, 0xfc9c7c, 0xfcbc7c, 0xfcdc7c, 0xfcfc7c, 0xdcfc7c, 0xbcfc7c, 0x9cfc7c,
			0x7cfc7c, 0x7cfc9c, 0x7cfcbc, 0x7cfcdc, 0x7cfcfc, 0x7cdcfc, 0x7cbcfc, 0x7c9cfc,
			0xb4b4fc, 0xc4b4fc, 0xd8b4fc, 0xe8b4fc, 0xfcb4fc, 0xfcb4e8, 0xfcb4d8, 0xfcb4c4,
			0xfcb4b4, 0xfcc4b4, 0xfcd8b4, 0xfce8b4, 0xfcfcb4, 0xe8fcb4, 0xd8fcb4, 0xc4fcb4,
			0xb4fcb4, 0xb4fcc4, 0xb4fcd8, 0xb4fce8, 0xb4fcfc, 0xb4e8fc, 0xb4d8fc, 0xb4c4fc,
			0x000070, 0x1c0070, 0x380070, 0x540070, 0x700070, 0x700054, 0x700038, 0x70001c,
			0x700000, 0x701c00, 0x703800, 0x705400, 0x707000, 0x547000, 0x387000, 0x1c7000,
			0x007000, 0x00701c, 0x007038, 0x007054, 0x007070, 0x005470, 0x003870, 0x001c70,
			0x383870, 0x443870, 0x543870, 0x603870, 0x703870, 0x703860, 0x703854, 0x703844,
			0x703838, 0x704438, 0x705438, 0x706038, 0x707038, 0x607038, 0x547038, 0x447038,
			0x387038, 0x387044, 0x387054, 0x387060, 0x387070, 0x386070, 0x385470, 0x384470,
			0x505070, 0x585070, 0x605070, 0x685070, 0x705070, 0x705068, 0x705060, 0x705058,
			0x705050, 0x705850, 0x706050, 0x706850, 0x707050, 0x687050, 0x607050, 0x587050,
			0x507050, 0x507058, 0x507060, 0x507068, 0x507070, 0x506870, 0x506070, 0x505870,
			0x000040, 0x100040, 0x200040, 0x300040, 0x400040, 0x400030, 0x400020, 0x400010,
			0x400000, 0x401000, 0x402000, 0x403000, 0x404000, 0x304000, 0x204000, 0x104000,
			0x004000, 0x004010, 0x004020, 0x004030, 0x004040, 0x003040, 0x002040, 0x001040,
			0x202040, 0x282040, 0x302040, 0x382040, 0x402040, 0x402038, 0x402030, 0x402028,
			0x402020, 0x402820, 0x403020, 0x403820, 0x404020, 0x384020, 0x304020, 0x284020,
			0x204020, 0x204028, 0x204030, 0x204038, 0x204040, 0x203840, 0x203040, 0x202840,
			0x2c2c40, 0x302c40, 0x342c40, 0x3c2c40, 0x402c40, 0x402c3c, 0x402c34, 0x402c30,
			0x402c2c, 0x40302c, 0x40342c, 0x403c2c, 0x40402c, 0x3c402c, 0x34402c, 0x30402c,
			0x2c402c, 0x2c4030, 0x2c4034, 0x2c403c, 0x2c4040, 0x2c3c40, 0x2c3440, 0x2c3040,
			0x000000, 0x000000, 0x000000, 0x000000, 0x000000, 0x000000, 0x000000, 0x000000
		};
		for(int i=0;i<standard.length;i++) {
			byte b = (byte)( (standard[i] & 0xFF) >> 2);
			byte g = (byte)( ((standard[i] >> 8 ) & 0xFF) >> 2);
			byte r = (byte)( ((standard[i] >> 16 ) & 0xFF) >> 2);
			DAC_memory[i][0] = r;
			DAC_memory[i][1] = g;
			DAC_memory[i][2] = b;
		}
	}
	
	
	// This concludes the DAC system.
	
	
	/* External Registers (aka General Registers) */
	
	boolean VSYNCP; // Vertical Sync Polarity
	boolean HSYNCP; // Horizontal Sync Polarity
	boolean OEPS; // Odd/Even Page Select
	byte clock; // 2 bits
	boolean RAMEnable;
	boolean IOAS; // Input/Output Address Select
	
	byte featureControl; // 2 bits
	
	boolean VRetrace; // Vertical Retrace
	boolean displayDisabled;
	
	/* Miscellaneous Output Register
	 * Read at 3CC
	 * Write at 3C2
	 * 7: VSYNCP
	 * 6: HSYNCP
	 * 5: OEPS
	 * 4: Nothing
	 * 3, 2: Clock Select
	 * 1: RAM Enable
	 * 0: IOAS
	 * 
	 * Notes:
	 *   Yes, I know the bit shifting is ridiculous (especially with '1>>0'),
	 * but it makes it clearer that they refer to specific bits.
	 *   This would be nicer if I had binary literals.
	 */
	public void writeMiscellaneousOutputRegister(byte b) {
		VSYNCP		= (b>>7 & 1) == 1;
		HSYNCP		= (b>>6 & 1) == 1;
		OEPS		= (b>>5 & 1) == 1;
		clock		= (byte)(b>>2 & 3);
		RAMEnable	= (b>>1 & 1) == 1;
		IOAS		= (b>>0 & 1) == 1;
	}
	public byte readMiscellaneousOutputRegister() {
		byte register = (byte)0;
		if (VSYNCP)	register += (byte)(1<<7);
		if (HSYNCP)	register += (byte)(1<<6);
		if (OEPS)	register += (byte)(1<<5);
		register += (byte)((clock & 3)<< 2);
		if (RAMEnable)	register += (byte)(1<<1);
		if (IOAS)	register += (byte)(1<<0);
		return register;
	}
	
	/* Feature Control Register
	 * Read at 3CA
	 * Write at 3BA for mono, 3DA for color
	 */

	public void writeFeatureControlRegister(byte b) {
		featureControl = (byte)(b & 3);
	}
	
	public byte readFeatureControlRegister() {
		return (byte)(featureControl & 3);
	}

	/* Input Status #0 Register
	 * Read-only at 3C2
	 */
	// UNIMPLEMENTED
	
	/* Input Status #1 Register
	 * Read at 3BA for mono, 3DA for color
	 */
	// UNIMPLEMENTED
	
	// This concludes the external registers.
	
	
	/* Graphics Registers
	 * 
	 * Notes:
	 *   Storing the actual registers. Just easier in this case.
	 * 
	 */
	
	byte setOrResetRegister;		// 4 bits
	byte enableSetOrResetRegister;		// 4 bits
	byte colorCompareRegister;		// 4 bits
	byte dataRotateRegister;		// 5 bits
	byte readMapSelectRegister;		// 2 bits
	byte graphicsModeRegister;		// 7 bits
	byte miscellaneousGraphicsRegister;	// 4 bits
	byte colorDontCareRegister;		// 4 bits
	byte bitMaskRegister;			// 8 bits
	
	byte graphicsRegisterIndex;
	int graphicsRegisterLength = 9;
	
	public void writeDirectToGraphicsRegister(int index, byte b){
		switch(index) {
			case 0x00: setOrResetRegister = b; break;
			case 0x01: enableSetOrResetRegister = b; break;
			case 0x02: colorCompareRegister = b; break;
			case 0x03: dataRotateRegister = b; break;
			case 0x04: readMapSelectRegister = b; break;
			case 0x05: graphicsModeRegister = b; break;
			case 0x06: miscellaneousGraphicsRegister = b; break;
			case 0x07: colorDontCareRegister = b; break;
			case 0x08: bitMaskRegister = b;
		}
	}
	
	public void writeGraphicsRegister(byte b){
		writeDirectToGraphicsRegister(graphicsRegisterIndex & 0xFF, b);
	}
		
	public byte readDirectFromGraphicsRegister(int index){
		switch(index) {
			case 0x00: return setOrResetRegister;
			case 0x01: return enableSetOrResetRegister;
			case 0x02: return colorCompareRegister;
			case 0x03: return dataRotateRegister;
			case 0x04: return readMapSelectRegister;
			case 0x05: return graphicsModeRegister;
			case 0x06: return miscellaneousGraphicsRegister;
			case 0x07: return colorDontCareRegister;
			case 0x08: return bitMaskRegister;
			default:   return 0;
		}
	}
	
	public byte readGraphicsRegister(){
		return readDirectFromGraphicsRegister(graphicsRegisterIndex & 0xFF);
	}
	// This concludes the graphics registers.
	
	/* Sequencer Registers
	 * 
	 * Notes:
	 *   Storing the actual registers. Again, easier.
	 * 
	 */
	byte resetRegister;			// 2 bits
	byte clockingModeRegister;		// 6 bits
	byte mapMaskRegister;			// 4 bits
	byte characterMapSelectRegister;	// 6 bits
	byte sequencerMemoryModeRegister;	// 4 bits
	
	byte sequencerRegisterIndex;
	int sequencerRegisterLength = 5;
	
	public void writeDirectToSequencerRegister(int index, byte b) {
		switch(index) {
			case 0x00: resetRegister = b; break;
			case 0x01: clockingModeRegister = b; break;
			case 0x02: mapMaskRegister = b; break;
			case 0x03: characterMapSelectRegister = b; break;
			case 0x04: sequencerMemoryModeRegister = b;
		}
	}
		
	public void writeSequencerRegister(byte b) {
		writeDirectToSequencerRegister(sequencerRegisterIndex & 0xFF, b);
	}
	
	public byte readDirectFromSequencerRegister(int index) {
		switch(index) {
			case 0x00: return resetRegister;
			case 0x01: return clockingModeRegister;
			case 0x02: return mapMaskRegister;
			case 0x03: return characterMapSelectRegister;
			case 0x04: return sequencerMemoryModeRegister;
			default:   return 0;
		}
	}
	
	public byte readSequencerRegister() {
		return readDirectFromSequencerRegister(sequencerRegisterIndex & 0xFF);
	}
	// This concludes the sequencer registers.
	
	/* Attribute Registers */
	byte attributeAddressRegister;
	byte[] paletteRegisters = new byte[16];
	byte attributeModeControlRegister;
	byte overscanColorRegister;
	byte colorPlaneEnableRegister;
	byte horizontalPixelPanningRegister;
	byte colorSelectRegister;
	boolean attributeRegisterFlipFlop;
	
	int attributeRegisterLength = 0x15;
	
	public void writeDirectToAttributeRegister(int index, byte b) {
		switch(index) {
			case 0x10: attributeModeControlRegister = b; break;
			case 0x11: overscanColorRegister = b; break;
			case 0x12: colorPlaneEnableRegister = b; break;
			case 0x13: horizontalPixelPanningRegister = b; break;
			case 0x14: colorSelectRegister = b; break;
			default:
				if (index <= 0x0F) paletteRegisters[index] = b;
		}
	}
	
	public void writeAttributeRegister(byte b) {
		int attributeIndex = attributeAddressRegister & 0x31;
		writeDirectToAttributeRegister(attributeIndex, b);
	}
	
	public byte readDirectFromAttributeRegister(int index) {
		switch(index) {
			case 0x10: return attributeModeControlRegister;
			case 0x11: return overscanColorRegister;
			case 0x12: return colorPlaneEnableRegister;
			case 0x13: return horizontalPixelPanningRegister;
			case 0x14: return colorSelectRegister;
			default:
				if (index <= 0x0F) return paletteRegisters[index];
		}
		return 0;
	}
	
	public byte readAttributeRegister() {
		return readDirectFromAttributeRegister(attributeAddressRegister & 0x31);
	}
	// This concludes the attribute registers.
	
	/* CRT Controller Registers */
	byte horizontalTotalRegister;
	byte endHorizontalDisplayRegister;
	byte startHorizontalBlankingRegister;
	byte endHorizontalBlankingRegister;
	byte startHorizontalRetraceRegister;
	byte endHorizontalRetraceRegister;
	byte verticalTotalRegister;
	byte overflowRegister;
	byte presetRowScanRegister;
	byte maximumScanLineRegister;
	byte cursorStartRegister;
	byte cursorEndRegister;
	byte startAddressHighRegister;
	byte startAddressLowRegister;
	byte cursorLocationHighRegister;
	byte cursorLocationLowRegister;
	byte verticalRetraceStartRegister;
	byte verticalRetraceEndRegister;
	byte verticalDisplayEndRegister;
	byte offsetRegister;
	byte underlineLocationRegister;
	byte startVerticalBlankingRegister;
	byte endVerticalBlanking;
	byte crtcModeControlRegister;
	byte lineCompareRegister;
	
	byte crtcRegisterIndex;
	int crtcRegisterLength = 0x19;
	
	//For loading state
	public void writeDirectToCRTCRegister(int index, byte b) {
		switch (index) {
			case 0x00: horizontalTotalRegister = b; break;
			case 0x01: endHorizontalDisplayRegister = b; break;
			case 0x02: startHorizontalBlankingRegister = b; break;
			case 0x03: endHorizontalBlankingRegister = b; break;
			case 0x04: startHorizontalRetraceRegister = b; break;
			case 0x05: endHorizontalRetraceRegister = b; break;
			case 0x06: verticalTotalRegister = b; break;
			case 0x07: overflowRegister = b; break;
			case 0x08: presetRowScanRegister = b; break;
			case 0x09: maximumScanLineRegister = b; break;
			case 0x0A: cursorStartRegister = b; break;
			case 0x0B: cursorEndRegister = b; break;
			case 0x0C: startAddressHighRegister = b; break;
			case 0x0D: startAddressLowRegister = b; break;
			case 0x0E: cursorLocationHighRegister = b; break;
			case 0x0F: cursorLocationLowRegister = b; break;
			case 0x10: verticalRetraceStartRegister = b; break;
			case 0x11: verticalRetraceEndRegister = b; break;
			case 0x12: verticalDisplayEndRegister = b; break;
			case 0x13: offsetRegister = b; break;
			case 0x14: underlineLocationRegister = b; break;
			case 0x15: startVerticalBlankingRegister = b; break;
			case 0x16: endVerticalBlanking = b; break;
			case 0x17: crtcModeControlRegister = b; break;
			case 0x18: lineCompareRegister = b;
		}
	}
	
	public void writeCRTCRegister(byte b) {
		int index = crtcRegisterIndex & 0xFF;
		// If "CRTC Registers Protect Enable" is set, write is restricted.
		if (index <= 0x07 && (verticalRetraceEndRegister & 0x80) != 0) {
			if (index == 0x07) overflowRegister |= (b & 16);
			return;
		}
		writeDirectToCRTCRegister(index, b);
	}
	
	
	public byte readDirectFromCRTCRegister(int index) {
		switch(index) {
			case 0x00: return horizontalTotalRegister;
			case 0x01: return endHorizontalDisplayRegister;
			case 0x02: return startHorizontalBlankingRegister;
			case 0x03: return endHorizontalBlankingRegister;
			case 0x04: return startHorizontalRetraceRegister;
			case 0x05: return endHorizontalRetraceRegister;
			case 0x06: return verticalTotalRegister;
			case 0x07: return overflowRegister;
			case 0x08: return presetRowScanRegister;
			case 0x09: return maximumScanLineRegister;
			case 0x0A: return cursorStartRegister;
			case 0x0B: return cursorEndRegister;
			case 0x0C: return startAddressHighRegister;
			case 0x0D: return startAddressLowRegister;
			case 0x0E: return cursorLocationHighRegister;
			case 0x0F: return cursorLocationLowRegister;
			case 0x10: return verticalRetraceStartRegister;
			case 0x11: return verticalRetraceEndRegister;
			case 0x12: return verticalDisplayEndRegister;
			case 0x13: return offsetRegister;
			case 0x14: return underlineLocationRegister;
			case 0x15: return startVerticalBlankingRegister;
			case 0x16: return endVerticalBlanking;
			case 0x17: return crtcModeControlRegister;
			case 0x18: return lineCompareRegister;
			default:   return 0;
		}
	}
	
	public byte readCRTCRegister() {
		return readDirectFromCRTCRegister(crtcRegisterIndex & 0xFF);
	}
	// This concludes the CRT controller registers.
	
	
	/* Internal Methods: */
	 
	public Color getDACColor(byte index) {
		return getDACColor(index & 0xFF);
	}
	
	public Color getDACColor(int index) {
		index &= 0xFF; // Reduce to 8 bits, which is between 0 and 255.
		int r = DAC_memory[index][0] & 0x3F; // 0x3F == 63, or 0b00111111
		int g = DAC_memory[index][1] & 0x3F;
		int b = DAC_memory[index][2] & 0x3F;
		return new Color((float)r/63, (float)g/63, (float)b/63); // 63 is "maximum intensity".
	}
	
	
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
	static final int VRAM_SIZE=0x10000;


	public void updateVideoWrite(int address, byte b) {
		if (address>=0xb8000 && address<=0xbffff) {
			vram[address-0xb8000]=b;
			if (computer.videoGUI!=null)
				computer.videoGUI.repaint();
		}
		else if (address>=0xa0000 && address<=0xaffff && (miscellaneousGraphicsRegister & 0x8) == 0) {
			byte bitmask = bitMaskRegister;
			int writemode = graphicsModeRegister & 3;
			int rotateCount = dataRotateRegister & 7;
			int logicalOperation = (dataRotateRegister>>3)&3;
			if (writemode == 0) {
				/* Quote:
				 * "In this mode, the host data is first rotated as per the Rotate Count field,
				 * then the Enable Set/Reset mechanism selects data from this or the Set/Reset
				 * field. Then the selected Logical Operation is performed on the resulting
				 * data and the data in the latch register. Then the Bit Mask field is used to
				 * select which bits come from the resulting data and which come from the latch
				 * register. Finally, only the bit planes enabled by the Memory Plane Write
				 * Enable field are written to memory."
				 */
				b = (byte)(b>>>rotateCount | (byte)(b<<(8 - rotateCount)));
				byte b_p1=((enableSetOrResetRegister&1)==0)? b:(((setOrResetRegister&1) == 0)? 0:(byte)0xff);
				byte b_p2=((enableSetOrResetRegister&2)==0)? b:(((setOrResetRegister&2) == 0)? 0:(byte)0xff);
				byte b_p3=((enableSetOrResetRegister&4)==0)? b:(((setOrResetRegister&4) == 0)? 0:(byte)0xff);
				byte b_p4=((enableSetOrResetRegister&8)==0)? b:(((setOrResetRegister&8) == 0)? 0:(byte)0xff);
				switch(logicalOperation) {
					case 1:
						b_p1 &= latch_p1;
						b_p2 &= latch_p2;
						b_p3 &= latch_p3;
						b_p4 &= latch_p4;
						break;
					case 2:
						b_p1 |= latch_p1;
						b_p2 |= latch_p2;
						b_p3 |= latch_p3;
						b_p4 |= latch_p4;
						break;
					case 3:
						b_p1 ^= latch_p1;
						b_p2 ^= latch_p2;
						b_p3 ^= latch_p3;
						b_p4 ^= latch_p4;
				}
				if((mapMaskRegister & 1)!=0)
					vramp1[address-0xa0000]=(byte)((~bitmask & latch_p1)|(bitmask & b_p1));
				if((mapMaskRegister & 2)!=0)
					vramp2[address-0xa0000]=(byte)((~bitmask & latch_p2)|(bitmask & b_p2));
				if((mapMaskRegister & 4)!=0)
					vramp3[address-0xa0000]=(byte)((~bitmask & latch_p3)|(bitmask & b_p3));
				if((mapMaskRegister & 8)!=0)
					vramp4[address-0xa0000]=(byte)((~bitmask & latch_p4)|(bitmask & b_p4));
			}
			else if (writemode == 1) {
				/* Quote:
				 * "In this mode, data is transferred directly from the 32 bit latch register
				 * to display memory, affected only by the Memory Plane Write Enable field.
				 * The host data is not used in this mode."
				 */
				if((mapMaskRegister & 1)!=0)
					vramp1[address-0xa0000]=latch_p1;
				if((mapMaskRegister & 2)!=0)
					vramp2[address-0xa0000]=latch_p2;
				if((mapMaskRegister & 4)!=0)
					vramp3[address-0xa0000]=latch_p3;
				if((mapMaskRegister & 8)!=0)
					vramp4[address-0xa0000]=latch_p4;
			}
			else if (writemode == 2) {
				byte b_p1=((b&1)==0)? 0:(byte)0xff;
				byte b_p2=((b&2)==0)? 0:(byte)0xff;
				byte b_p3=((b&4)==0)? 0:(byte)0xff;
				byte b_p4=((b&8)==0)? 0:(byte)0xff;
				switch(logicalOperation) {
					case 1:
						b_p1 &= latch_p1;
						b_p2 &= latch_p2;
						b_p3 &= latch_p3;
						b_p4 &= latch_p4;
						break;
					case 2:
						b_p1 |= latch_p1;
						b_p2 |= latch_p2;
						b_p3 |= latch_p3;
						b_p4 |= latch_p4;
						break;
					case 3:
						b_p1 ^= latch_p1;
						b_p2 ^= latch_p2;
						b_p3 ^= latch_p3;
						b_p4 ^= latch_p4;
				}
				if((mapMaskRegister & 1)!=0)
					vramp1[address-0xa0000]=(byte)((~bitmask & latch_p1)|(bitmask & b_p1));
				if((mapMaskRegister & 2)!=0)
					vramp2[address-0xa0000]=(byte)((~bitmask & latch_p2)|(bitmask & b_p2));
				if((mapMaskRegister & 4)!=0)
					vramp3[address-0xa0000]=(byte)((~bitmask & latch_p3)|(bitmask & b_p3));
				if((mapMaskRegister & 8)!=0)
					vramp4[address-0xa0000]=(byte)((~bitmask & latch_p4)|(bitmask & b_p4));
			}
			else if (writemode == 3) {
				byte b_p1=((setOrResetRegister&1)==0)? 0:(byte)0xff;
				byte b_p2=((setOrResetRegister&2)==0)? 0:(byte)0xff;
				byte b_p3=((setOrResetRegister&4)==0)? 0:(byte)0xff;
				byte b_p4=((setOrResetRegister&8)==0)? 0:(byte)0xff;
				b = (byte)(b>>>rotateCount | (byte)(b<<(8 - rotateCount)));
				b &= bitmask;
				if((mapMaskRegister & 1)!=0)
					vramp1[address-0xa0000]=(byte)((~b & latch_p1)|(b & b_p1));
				if((mapMaskRegister & 2)!=0)
					vramp2[address-0xa0000]=(byte)((~b & latch_p2)|(b & b_p2));
				if((mapMaskRegister & 4)!=0)
					vramp3[address-0xa0000]=(byte)((~b & latch_p3)|(b & b_p3));
				if((mapMaskRegister & 8)!=0)
					vramp4[address-0xa0000]=(byte)((~b & latch_p4)|(b & b_p4));
			}

			if (computer.videoGUI!=null)
				computer.videoGUI.repaint();
		}
	}

	public void updateVideoRead(int address) {
		if (address>=0xa0000 && address<=0xaffff) {
			if((mapMaskRegister&1)!=0)
				latch_p1=vramp1[address-0xa0000];
			if((mapMaskRegister&2)!=0)
				latch_p2=vramp2[address-0xa0000];
			if((mapMaskRegister&4)!=0)
				latch_p3=vramp3[address-0xa0000];
			if((mapMaskRegister&8)!=0)
				latch_p4=vramp4[address-0xa0000];
		}
	}

	public void ioPortWriteByte(int address, byte data) {
		switch(address) {
			case 0x3b4:
			case 0x3d4:
				// IOAS determines CRT read/write addresses
				if( (address == 0x3d4) != IOAS) break;
				crtcRegisterIndex = data;
				break;
			case 0x3b5:
			case 0x3d5:
				if( (address == 0x3d5) != IOAS) break;
				writeCRTCRegister(data);
				break;
			case 0x3c0:
				if (!attributeRegisterFlipFlop)
					attributeAddressRegister = (byte)(data & 0x3f);
				else
					writeAttributeRegister(data);
				attributeRegisterFlipFlop = !attributeRegisterFlipFlop;
				break;
			case 0x3c4: sequencerRegisterIndex = data; break;
			case 0x3c5: writeSequencerRegister(data); break;
			case 0x3c7: writeDACReadRegister(data); break;
			case 0x3c8: writeDACWriteRegister(data); break;
			case 0x3c9: writeDACDataRegister(data); break;
			case 0x3ce:
				graphicsRegisterIndex = data;
				System.out.println("Indexing graphics control register "+data);
				break;
			case 0x3cf:
				writeGraphicsRegister(data);
				System.out.println("Writing "+data+" to graphics control register");
				break;
		}
	}

	
	public byte ioPortReadByte(int address){
		switch (address) {
			case 0x3b4:
			case 0x3d4:
				// IOAS determines CRT read/write addresses
				if( (address == 0x3d4) != IOAS) break;
				return crtcRegisterIndex;
			case 0x3b5:
			case 0x3d5:
				if( (address == 0x3d5) != IOAS) break;
				return readCRTCRegister();
			case 0x3c0:
				if (!attributeRegisterFlipFlop)
					return attributeAddressRegister;
				else
					return 0;
			case 0x3c1: return readAttributeRegister();
			case 0x3c4: return sequencerRegisterIndex;
			case 0x3c5: return readSequencerRegister();
			case 0x3c7: return readDACStateRegister();
			case 0x3c8: return readDACWriteRegister();
			case 0x3c9: return readDACDataRegister();
			case 0x3ce: return graphicsRegisterIndex;
			case 0x3cf: return readGraphicsRegister();
			case 0x3da:
				attributeRegisterFlipFlop = false;
				return 0;
		}
		return 0;
	}

	public void loadState(String state) {
		Scanner s=new Scanner(state);
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
		for(int i=0; i<sequencerRegisterLength; i++)
			writeDirectToSequencerRegister(i, s.nextByte());
		for(int i=0; i<graphicsRegisterLength; i++)
			writeDirectToGraphicsRegister(i, s.nextByte());
		for(int i=0; i<attributeRegisterLength; i++)
			writeDirectToAttributeRegister(i, s.nextByte());
		for(int i=0; i<crtcRegisterLength; i++)
			writeDirectToCRTCRegister(i, s.nextByte());
		latch_p1=s.nextByte();
		latch_p2=s.nextByte();
		latch_p3=s.nextByte();
		latch_p4=s.nextByte();
		sequencerRegisterIndex=s.nextByte();
		graphicsRegisterIndex=s.nextByte();
		attributeAddressRegister=s.nextByte();
		crtcRegisterIndex=s.nextByte();
		attributeRegisterFlipFlop=s.nextByte()==1;
		
		writeMiscellaneousOutputRegister(s.nextByte());
		writeFeatureControlRegister(s.nextByte());
		
		for(int i=0;i<256;i++) {
			for(int j=0;j<3;j++)
				DAC_memory[i][j] = s.nextByte();
		}
		DAC_stateRegister = s.nextByte();
		DAC_memoryIndex = s.nextByte();
		DAC_colorIndex = s.nextByte();
	}
	
	public String saveState()
	{
		StringBuilder state=new StringBuilder();
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
		for (int i=0; i<sequencerRegisterLength; i++)
			state.append(readDirectFromSequencerRegister(i)+" ");
		for (int i=0; i<graphicsRegisterLength; i++)
			state.append(readDirectFromGraphicsRegister(i)+" ");
		for (int i=0; i<attributeRegisterLength; i++)
			state.append(readDirectFromAttributeRegister(i)+" ");
		for (int i=0; i<crtcRegisterLength; i++)
			state.append(readDirectFromCRTCRegister(i)+" ");
		state.append(latch_p1+" ");
		state.append(latch_p2+" ");
		state.append(latch_p3+" ");
		state.append(latch_p4+" ");
		state.append(sequencerRegisterIndex+" ");
		state.append(graphicsRegisterIndex+" ");
		state.append(attributeAddressRegister+" ");
		state.append(crtcRegisterIndex+" ");
		state.append((attributeRegisterFlipFlop?1:0) + " ");
		
		state.append(readMiscellaneousOutputRegister() + " ");
		state.append(readFeatureControlRegister() + " ");
		
		for(int i=0;i<256;i++) {
			for(int j=0;j<3;j++)
				state.append(DAC_memory[i][j] + " ");
		}
		state.append(DAC_stateRegister + " ");
		state.append(DAC_memoryIndex + " ");
		state.append(DAC_colorIndex + "");

		return state.toString();
	}
	
	public Video(Computer computer)
	{
		this.computer=computer;
		
		vram = new byte[VRAM_SIZE];
		vramp1 = new byte[VRAM_SIZE];
		vramp2 = new byte[VRAM_SIZE];
		vramp3 = new byte[VRAM_SIZE];
		vramp4 = new byte[VRAM_SIZE];

		initStandardPalette();

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
		if ((miscellaneousGraphicsRegister&0x8)==0)
			paintScreenGraphics640x480(g,VOFFSET,VOFFSET);
		else if ((miscellaneousGraphicsRegister&1)==0)
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
	
	// Never called?
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
	public static final int TEXTMODE=0,V640480=1,V640200=2;
	public int videoResolution()
	{
		if ((miscellaneousGraphicsRegister&0x8)==0)		//640x480?
			return V640480;
		else if ((miscellaneousGraphicsRegister&1)==0)	//text
			return TEXTMODE;
		return V640200;							//default to 640x200
	}

}