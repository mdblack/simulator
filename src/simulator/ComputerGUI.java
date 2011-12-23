package simulator;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

import javax.swing.*;

public class ComputerGUI
{
	JFrame computerFrame;
	boolean singleFrame;
	static int XSIZE=1200,YSIZE=700;
	static int BIGWIDTH=(int)(0.6*XSIZE),BIGHEIGHT=(int)(0.62*YSIZE);
	static int COMPONENTHEIGHT=YSIZE/2-20;
	ArrayList<AbstractGUI> component;
	//Container[] bigcomponent;
	ComputerGUIComponent computerGUIComponent,rightGUIComponent;
	Computer computer;

	public ComputerGUI(Computer computer, boolean singleFrame)
	{
		this.computer=computer;
		this.singleFrame=singleFrame;
		if(!singleFrame) return;
		
		component=new ArrayList<AbstractGUI>();

//		component=new Container[MAXCOMPONENTS];
//		bigcomponent=new Container[MAXCOMPONENTS];

		computerFrame = new JFrame("Simulator");
		computerGUIComponent=new ComputerGUIComponent(XSIZE,YSIZE);
		rightGUIComponent=new ComputerGUIComponent(XSIZE-BIGWIDTH,0);
		JScrollPane scrollpane = new JScrollPane(rightGUIComponent);
		scrollpane.setBounds(BIGWIDTH,0,XSIZE-BIGWIDTH,YSIZE);
		computerGUIComponent.setBounds(0,0,BIGWIDTH,YSIZE);

		if (computer.applet==null)
		{
			computerFrame.setSize(XSIZE,YSIZE);
			computerFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			computerFrame.setLayout(null);
			computerFrame.add(scrollpane);
			computerFrame.add(computerGUIComponent);
			computerFrame.setVisible(true);
		}
		else
		{
			computer.applet.panel.setLayout(null);
			computer.applet.panel.add(scrollpane);
			computer.applet.panel.add(computerGUIComponent);
			computer.applet.panel.revalidate();
		}
	}

	public void addComponent(AbstractGUI c)
	{
		if (!singleFrame) return;
		if (c instanceof ControlGUI) return;

		if (c instanceof KeyboardGUI)
		{
			Container x = c.frame.getContentPane();
			x.setBounds(0,BIGHEIGHT,c.frameX,c.frameY);
			
			for (AbstractGUI comp:component)
			{
				if (comp.bigScreen && comp.frameY>BIGHEIGHT)
				{
					comp.frameY=BIGHEIGHT;
					comp.frame.getContentPane().setBounds(0,0,comp.frameX,comp.frameY);
					comp.repaint();
				}
			}
			
			computerGUIComponent.add(x);
			x.validate();
			computerGUIComponent.revalidate();
			computerGUIComponent.repaint();			
			return;
		}
	
		if (c.bigScreen)
		{
			c.frame.getContentPane().setBounds(0,0,c.frameX,c.frameY);
			for (AbstractGUI comp:component)
			{
				if (comp.bigScreen)
					comp.frame.getContentPane().setVisible(false);
			}
			computerGUIComponent.add(c.frame.getContentPane());
			c.frame.getContentPane().validate();
			computerGUIComponent.revalidate();
			computerGUIComponent.repaint();
		}
		else
		{
			int ytop=0;
			for (AbstractGUI comp:component)
				if (!comp.bigScreen)
					ytop++;
			
			c.frame.getContentPane().setBounds(0,ytop*COMPONENTHEIGHT,getW(c),getH(c));
			rightGUIComponent.add(c.frame.getContentPane());
			rightGUIComponent.ysize+=COMPONENTHEIGHT;
			rightGUIComponent.revalidate();
			rightGUIComponent.repaint();
		}
		component.add(c);
	}

	public void removeComponent(AbstractGUI c)
	{
		if(!singleFrame) return;

		if (c instanceof KeyboardGUI)
		{
			for (AbstractGUI comp:component)
			{
				if (comp.bigScreen && comp.frameY==BIGHEIGHT)
				{
					comp.frameY=YSIZE;
					comp.frame.getContentPane().setBounds(0,0,comp.frameX,comp.frameY);
					comp.repaint();
				}
			}
			
			computerGUIComponent.remove(c.frame.getContentPane());
			computerGUIComponent.repaint();
			return;
		}
		
		if (!c.bigScreen)
		{
			int top=0;
			for (int i=0; i<component.size(); i++)
			{
				if (component.get(i)==c)
					rightGUIComponent.remove(c.frame.getContentPane());
				else if (!component.get(i).bigScreen)
				{
					component.get(i).frame.getContentPane().setBounds(0, top, getW(c), getH(c));
					top+=COMPONENTHEIGHT;
				}
			}
			rightGUIComponent.repaint();
			component.remove(c);
		}
		else
		{
			computerGUIComponent.remove(c.frame.getContentPane());
			component.remove(c);
			for (int i=component.size()-1; i>=0; i--)
			{
				if (component.get(i).bigScreen)
				{
					component.get(i).frame.getContentPane().setVisible(true);
					break;
				}
			}
			computerGUIComponent.revalidate();
			computerGUIComponent.repaint();
		}
	}

	public int getW(AbstractGUI c)
	{
		if (c instanceof KeyboardGUI)
			return BIGWIDTH;
		if (c.bigScreen)
			return BIGWIDTH;
		return XSIZE-BIGWIDTH-20;
	}

	public int getH(AbstractGUI c)
	{
		if (c instanceof KeyboardGUI)
			return YSIZE-BIGHEIGHT-80;
		if (!c.bigScreen)
			return COMPONENTHEIGHT;
		if (computer.keyboardGUI==null)
			return YSIZE-80;
		return BIGHEIGHT;
	}

	public class ComputerGUIComponent extends JComponent
	{
		int xsize,ysize;
		public ComputerGUIComponent(int xsize,int ysize)
		{
			this.xsize=xsize;
			this.ysize=ysize;
		}
		public Dimension getPreferredSize()
		{
			return new Dimension(xsize,ysize);
		}
		public void paintComponent(Graphics g)
		{
			g.setColor(new Color(255,255,255));
			g.fillRect(0,0,xsize,ysize);
		}
	}

}

