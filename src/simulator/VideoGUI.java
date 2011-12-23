package simulator;
import java.awt.*;

public class VideoGUI extends AbstractGUI
{
	public VideoGUI(Computer computer)
	{
		super(computer,"The Screen",Video.VWIDTH,Video.VHEIGHT,false,true,false,true);
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
}

