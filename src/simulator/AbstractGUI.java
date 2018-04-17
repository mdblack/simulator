package simulator;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;

import java.awt.event.*;

public abstract class AbstractGUI extends JInternalFrame
{
	public static final int STATUSSIZE=50;
	public static final int BUTTONROWSIZE=30;
	public static final int MARGIN=20;
	public static final int MAX_X=10000,MAX_Y=7000;

	int frameX, frameY;		//size of the display area
	int canvasX, canvasY;	//size of the full canvas
	
	public GUIComponent guiComponent;
	public StatusComponent statusComponent;
	public ButtonRowComponent buttonRowComponent;
	public JScrollPane scrollPane;

	public boolean slowMode=false;
	public int slowModeSleepDuration=300;

	private boolean scrollpane, statusbar, buttonrow;
	public boolean bigScreen;
	
	protected Computer computer;
	private AbstractGUI thisgui=this;

	public AbstractGUI(Computer computer, String title, int canvaswidth, int canvasheight, boolean statusbar, boolean scrollpane, boolean buttonrow, boolean bigScreen)
	{
		super(title, true, true, true, true);
		
		this.computer=computer;
		
		this.scrollpane=scrollpane;
		this.statusbar=statusbar;
		this.buttonrow=buttonrow;
		this.bigScreen=bigScreen;

		setCanvasCoordinates(canvaswidth, canvasheight);
		setFrameCoordinates(canvasX, canvasY);
		
//		if (computer.computerGUI.singleFrame)
//		{
//			frameX=computer.computerGUI.getW(this);
//			frameY=computer.computerGUI.getH(this);
//		}
		if (frameX>computer.computerGUI.XSIZE-MARGIN)
			frameX=ComputerGUI.XSIZE-MARGIN;
		
		if (frameY>computer.computerGUI.MAINSIZE-MARGIN)
			frameY=ComputerGUI.MAINSIZE-MARGIN;

		setSize(frameX,frameY);
		addInternalFrameListener(new GUIWindowListener());
		addComponentListener(new GUIWindowListener());

		setLayout(null);

		if (statusbar)
		{
			int topy=frameY-STATUSSIZE;
			
			statusComponent = new StatusComponent();
			statusComponent.setBounds(0,topy,frameX,STATUSSIZE);
			statusComponent.setLabel("");
			add(statusComponent);
		}
		
		/*
		 * The borders might be different on various devices so instead of using whatever the
		 * default it, force it to be a line border.
		 */
		
		try {
			Border frameBorder=getBorder();
			
			frameBorder = ((javax.swing.border.CompoundBorder)frameBorder).getInsideBorder();
			setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.black), frameBorder));
		} catch (ClassCastException e) {
			setBorder(BorderFactory.createLineBorder(Color.black, 1));
		}

	}

	public void setCanvasCoordinates(int canvaswidth, int canvasheight) {
		canvasX=canvaswidth;
		canvasY=canvasheight;
	}
	public void setFrameCoordinates(int canvasX, int canvasY) {
		frameX=(canvasX<=MAX_X-10? canvasX:MAX_X-10);
		frameY=(canvasY+STATUSSIZE<=MAX_Y? canvasY+STATUSSIZE:MAX_Y);
	}
	
	//override these methods
	public void constructGUI(GUIComponent guiComponent) { }
	public void doPaint(Graphics g) { }
	public int width() { return canvasX; }
	public int height() { return canvasY; }
	public void mouseMove(MouseEvent e) { }
	public void mouseDrag(MouseEvent e) { }
	public void mouseClick(MouseEvent e) { }
	public void mousePress(MouseEvent e) { }
	public void mouseRelease(MouseEvent e) { }
	public void mouseExit() { }
	public void statusEdited(String keys) { }
	public void reSize(int width, int height){ }
	public abstract void closeGUI();

	//these methods are called externally
	public void repaint()
	{
//		if (!computer.updateGUIOnPlay && !computer.debugMode && !(this instanceof VideoGUI))
//			return;
		if (guiComponent!=null)
		{
			if (!slowMode)
				guiComponent.repaint();
			else
				guiComponent.paintImmediately(0,0,canvasX,canvasY);
		}
		sleep();
	}
	public void refresh()
	{
		setSize(frameX+10,frameY);
		guiComponent=new GUIComponent();
		add(guiComponent);
		int topy=0;
		int height=statusbar? frameY-topy-STATUSSIZE:frameY-topy;
		if (scrollpane)
		{
			scrollPane = new JScrollPane(guiComponent);
			scrollPane.setBounds(0,topy,frameX,height);
			add(scrollPane);
		}
		else
		{
			guiComponent.setBounds(0,topy,frameX,height);
			add(guiComponent);
		}
		guiComponent.addMouseListener(new GUIMouseListener());
		guiComponent.addMouseMotionListener(new GUIMouseMotionListener());
		guiComponent.repaint();

		computer.computerGUI.addComponent(this);

		guiComponent.paintImmediately(0,0,canvasX,canvasY);
		if (statusbar) guiComponent.addKeyListener(statusComponent.getKeyListener());
		guiComponent.requestFocus();
	}
	public void sleep()
	{
		if (slowMode)
		{
			try { Thread.sleep(slowModeSleepDuration); } catch(Exception e) { }
		}
	}
	public void setStatusLabel(String label)
	{
		if(statusComponent!=null)
			statusComponent.setLabel(label);
	}

	public void statusEdit(String baselabel, int exactlength, boolean hexonly)
	{
		statusComponent.edit(baselabel,exactlength,hexonly);
	}
	public void statusEdit(String baselabel, boolean hexonly)
	{
		statusComponent.edit(baselabel,-1,hexonly);
	}
	public void statusEdit(String baselabel)
	{
		statusComponent.edit(baselabel,-1,false);
	}

	public class GUIComponent extends JComponent
	{
		public GUIComponent()
		{
			super();
			constructGUI(this);
		}
		public Dimension getPreferredSize()
		{
			return new Dimension(width(),height());
		}
		public void paintComponent(Graphics g)
		{
			g.setColor(Color.WHITE);
//			g.fillRect(0,0,canvasX,canvasY);
			g.fillRect(0,0,width(),height());
			doPaint(g);
		}
	}

	public class GUIMouseListener implements MouseListener
	{
		public void mouseEntered(MouseEvent e) { }
		public void mousePressed(MouseEvent e)
		{
			mousePress(e);
		}
		public void mouseReleased(MouseEvent e)
		{
			mouseRelease(e);
		}
		public void mouseClicked(MouseEvent e)
		{
			guiComponent.requestFocus();
			mouseClick(e);
		}
		public void mouseExited(MouseEvent e)
		{
			mouseExit();
		}
	}

	public class GUIMouseMotionListener implements MouseMotionListener
	{
		public void mouseMoved(MouseEvent e)
		{
			mouseMove(e);
		}
		public void mouseDragged(MouseEvent e) 
		{ 
			mouseDrag(e);
		}
	}

	public class ButtonRowComponent extends JComponent
	{
		JButton closeButton,slowButton;
		public static final int BUTTONWIDTH=150;

		public ButtonRowComponent()
		{
			super();
			closeButton=new JButton("Close");
			closeButton.setBounds(10,5,(frameX-30)/2,BUTTONROWSIZE-10);
			closeButton.addActionListener(new ButtonListener());

			this.add(closeButton);
			slowButton=new JButton("Slow speed");
			slowButton.setBounds(frameX/2+10,5,(frameX-30)/2,BUTTONROWSIZE-10);
			slowButton.addActionListener(new ButtonListener());

			this.add(slowButton);
		}

		public void paintComponent(Graphics g)
		{
			g.setColor(Color.WHITE);
			g.fillRect(0,0,frameX,BUTTONROWSIZE);
			g.setColor(Color.BLACK);
			g.drawLine(0,BUTTONROWSIZE-1,frameX,BUTTONROWSIZE-1);
		}

		private class ButtonListener implements ActionListener
		{
			public void actionPerformed(ActionEvent e)
			{
				if (e.getActionCommand().equals("Close"))
				{
					close();
				}
				else if (e.getActionCommand().equals("Slow speed"))
				{
					buttonRowComponent.slowButton.setText("Normal speed");
					slowMode=true;
					guiComponent.requestFocus();
				}
				else if (e.getActionCommand().equals("Normal speed"))
				{
					buttonRowComponent.slowButton.setText("Slow speed");
					slowMode=false;
					guiComponent.requestFocus();
				}
			}
		}
	}
	
	public void close()
	{
		if (!computer.debugMode) computer.cycleEndLock.lockWait();
		closeGUI();

		computer.computerGUI.removeComponent(this);
	}

	public class StatusComponent extends JComponent
	{
		private String statusLabel="";
		private String baseLabel="";
		private String keys="";
		private boolean keyListen,hexonly;
		private int exactlength;

		public StatusComponent()
		{
			keyListen=false;
		}

		public void setLabel(String label)
		{
			if(!keyListen)
			{
				statusLabel=label;
				if (!computer.updateGUIOnPlay && !computer.debugMode)
					return;
				this.repaint();
			}
		}
		public void paintComponent(Graphics g)
		{
			g.setColor(Color.WHITE);
			g.fillRect(0,0,frameX,STATUSSIZE);
			g.setColor(Color.BLACK);
			g.drawLine(0,0,frameX,0);

			int fontSize=12;
			g.setFont(new Font("Dialog",Font.BOLD,fontSize));
			g.drawString(statusLabel,STATUSSIZE-MARGIN,fontSize);
		}

		public void edit(String baseLabel, int exactlength, boolean hexonly)
		{
			this.baseLabel=baseLabel;
			this.exactlength=exactlength;
			this.hexonly=hexonly;
			keyListen=true;
			statusLabel=baseLabel;
			keys="";
			this.repaint();
			guiComponent.requestFocus();
		}

		public void cancelEdit()
		{
			statusLabel="";
			this.repaint();
		}

		public void commitEdit()
		{
			statusLabel="";
			this.repaint();
			statusEdited(keys);
		}

		public StatusKeyListener getKeyListener()
		{
			return new StatusKeyListener();
		}

		public class StatusKeyListener implements KeyListener
		{
			public void keyPressed(KeyEvent e) { }
			public void keyReleased(KeyEvent e) { }
			public void keyTyped(KeyEvent e)
			{
				if(!keyListen) return;
				keyListen=false;

				char key=e.getKeyChar();

				if(((byte)key)==27)
					cancelEdit();
				else if(((byte)key)==10)
				{
					if (exactlength!=-1 && keys.length()<exactlength)
						cancelEdit();
					else
						commitEdit();
				}
				else if (((byte)key)==8)
				{
					keyListen=true;
					if(keys.length()>0)
					{
						keys=keys.substring(0,keys.length()-1);
						statusLabel=baseLabel+keys;
						repaint();
					}
				}
				else
				{
					keyListen=true;
					if (exactlength==-1 || keys.length()<exactlength)
					{
						if(!hexonly || ((key>='0' && key<='9')||(key>='A' && key<='F')||(key>='a'&&key<='f')))
						{
							keys=keys+key;
							statusLabel=baseLabel+keys;
							repaint();
						}
					}
				}
			}
		}
	}

	public class GUIWindowListener implements InternalFrameListener, ComponentListener
	{
		public void internalFrameActivated(InternalFrameEvent e) {}
		public void internalFrameClosed(InternalFrameEvent e) 
		{
			close();	
		}
		public void internalFrameClosing(InternalFrameEvent e) {}
		public void internalFrameDeactivated(InternalFrameEvent e) {}
		public void internalFrameDeiconified(InternalFrameEvent e) {}
		public void internalFrameIconified(InternalFrameEvent e) {}
		public void internalFrameOpened(InternalFrameEvent e) {}
		public void componentHidden(ComponentEvent e) {}
		public void componentMoved(ComponentEvent e) {}
		public void componentResized(ComponentEvent e) 
		{
			frameX=thisgui.getWidth();
			frameY=thisgui.getHeight();
			
			/*
			 * As the window components are being created, this method will be called to resize
			 * based on the components.  This means that some components might not yet exist so
			 * we need to check before doing anything with them.
			 */
			if (scrollpane)
			{
				int topy=0,height=statusbar? frameY-topy-STATUSSIZE:frameY-topy;
				
				scrollPane.setBounds(0,topy,frameX-scrollPane.getVerticalScrollBar().getWidth()-5,height-scrollPane.getHorizontalScrollBar().getHeight()-MARGIN);
			}
			if (statusbar && statusComponent != null)   // Make sure we have a status bar AND the pane exists
			{
				int topy=frameY-STATUSSIZE;
				statusComponent.setBounds(0,topy,frameX,STATUSSIZE);
			}
			
			reSize(frameX,frameY);
			thisgui.repaint();
		}
		public void componentShown(ComponentEvent e) {}
	}	
}
