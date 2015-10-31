package simulator;
import java.awt.*;
import java.awt.event.MouseEvent;

public class VideoGUI extends AbstractGUI
{
	public VideoGUI(Computer computer)
	{
		super(computer,"The Screen",Video.VWIDTH,Video.VHEIGHT,true,true,false,true);
		refresh();
		computer.video.setupGUI(this);
	}
	public void closeGUI()
	{
		computer.videoGUI=null;
	}
	public void doPaint(Graphics g)
	{
		computer.video.paintScreen(g);
	}
	public int width() 
	{
		return Video.VWIDTH;
	}
	public int height() 
	{
		return Video.VHEIGHT;
	}
	public void mouseMove(MouseEvent e)
	{
		generateStatusLabel(e.getX()-Video.VOFFSET,e.getY()-Video.VOFFSET);
	}
	public void mouseClick(MouseEvent e)
	{
		if (e.getButton()==MouseEvent.BUTTON2 && computer.video.videoResolution()==Video.TEXTMODE)
		{
			int address=(2*(((e.getY()-Video.VOFFSET)/16)*80+((e.getX()-Video.VOFFSET)/9))+0xb8000);
			computer.memoryGUI.dataFrame=new MemoryBlockGUI(computer,MemoryBlockGUI.DATA,address);
		}
		
	}
	private void generateStatusLabel(int pixelx, int pixely)
	{
		String s="";
		if (computer.video.videoResolution()==Video.TEXTMODE)
		{
			s+="80x25 TEXT MODE:  ";
			s+="row "+(pixely/16);
			s+=" column "+(pixelx/9);
			s+=" memory address: "+Integer.toHexString(2*((pixely/16)*80+(pixelx/9))+0xb8000);
		}
		else if (computer.video.videoResolution()==Video.V640480)
		{
			s+="640x480 GRAPHICS MODE:  ";
			s+="row "+pixely;
			s+=" column "+pixelx;
		}
		else
			s+="Unsupported graphics mode";
		
		setStatusLabel(s);
	}
}

