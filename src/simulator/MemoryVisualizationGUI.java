package simulator;

import java.awt.Graphics;

public class MemoryVisualizationGUI extends AbstractGUI {

	private static final int BLOCKSIZE=0x100;		//256 bytes/block
//	private static final int TOTAL_BLOCKS=(int)(0x100000000l/BLOCKSIZE);
	private static final int TOTAL_BLOCKS=(int)(PhysicalMemory.TOTAL_RAM_SIZE/BLOCKSIZE);
	public static final int BLOCKWIDTH=10;
	public static final int BLOCKHEIGHT=6;

	public MemoryVisualizationGUI(Computer computer) {
		super(computer, "Memory Visualization", 100, 100, false, true,
				true,false);
		// TODO Auto-generated constructor stub
	}
	
	public void doPaint(Graphics g)
	{
		int xblocks=width()/BLOCKWIDTH;
		int yblocks=height()/BLOCKHEIGHT;
		for (int y=0; y<yblocks; y++)
		{
			int visibleStart = yblocks*scrollPane.getVerticalScrollBar().getValue()/scrollPane.getVerticalScrollBar().getMaximum();
			int visibleEnd = visibleStart + scrollPane.getVerticalScrollBar().getVisibleAmount()*yblocks/scrollPane.getVerticalScrollBar().getMaximum();

			if (y<visibleStart-5 || y>visibleEnd+5)
				continue;

			if(y*xblocks>=TOTAL_BLOCKS)
				break;

			for (int x=0; x<xblocks; x++)
			{
				//don't depict non-existent blocks
				int blockNumber=y*xblocks+x;
				if(blockNumber>=TOTAL_BLOCKS)
					break;


				g.fillRect(x*BLOCKWIDTH,y*BLOCKHEIGHT,BLOCKWIDTH-1,BLOCKHEIGHT-1);
			}
		}
	}

	@Override
	public void closeGUI() {
		// TODO Auto-generated method stub

	}

}
