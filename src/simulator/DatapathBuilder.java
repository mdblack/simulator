package simulator;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Scanner;
import java.util.Stack;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import com.sun.javafx.geom.Rectangle;

import simulator.CustomProcessor.CustomProcessorModule;

public class DatapathBuilder extends AbstractGUI
{
//	private ArrayList<Block> blocks;
//	private ArrayList<Bus> buses;
	public DatapathModule defaultModule;
	public double scaling;
	public int xshift=0,yshift=0;
	public int dpwidth=2000, dpheight=2000;
	public int gridsize=3;
//	public int blocknumber=1;
	public int defaultbits=1;

	private ToolComponent toolcomponent;
	private DrawingComponent drawingcomponent;
	private ModificationComponent modificationcomponent;
	private 	JScrollPane modificationScroll;

	private class ErrorEntry{int number; String error; public ErrorEntry(int number, String error){this.number=number; this.error=error;}}
	public ArrayList<ErrorEntry> errorlog;
	
	private String action="";
	private String component="";
	private int corner1x,corner1y,corner2x,corner2y,mousex,mousey;
	
	private Stack<String> undolog;
	public ArrayList<CustomProcessor.CustomProcessorModule> modules;
	private GUIComponent guiComponent;

	public DatapathBuilder(Computer computer)
	{
		super(computer,"Datapath Builder",
				computer.resolution.datapath.width,
				computer.resolution.datapath.height,true,false,false,true);
	
		undolog=new Stack<String>();
		modules=new ArrayList<CustomProcessorModule>();
		errorlog=new ArrayList<ErrorEntry>();
		defaultModule=new DatapathModule();
		undolog.push(dumpXML());
		scaling = computer.resolution.datapath.getScalingFactor();
		refresh();
	}

	public int getX(MouseEvent e)
	{
		int x=(int)((e.getX()-xshift)/scaling);
		x-=x%gridsize;
		return x;
	}
	public int getY(MouseEvent e)
	{
		int y=(int)((e.getY()-yshift)/scaling);
		y-=y%gridsize;
		return y;
	}

	JScrollPane toolscroll;
	public void reSize(int width, int height)
	{
		//setCanvasCoordinates(width, height);
		//setFrameCoordinates(width, height);
		
		// This is another place where we may be trying to scroll a pane/window but it 
		// hasn't yet been fully realized.  So check first.
		if (toolscroll == null) return;
		
		try {
			// Change the height of the main gui container and the tool scroll.
			guiComponent.setBounds(0, 0, width, height);
			if (toolscroll != null)
				toolscroll.setBounds(0,0,toolcomponent.width+computer.resolution.datapath.toolComponent.getScrollbarWidth(),height-computer.resolution.datapath.getStatusBarThickness());

			if (drawingcomponent != null) {
				drawingcomponent.restoreSize();
				drawingcomponent.scroll.revalidate();
				}
			
			if (modificationcomponent != null)
				modificationcomponent.restoreSize();
			
		} catch(Exception e) {}
		
		revalidate();
		repaint();
	}
	public void constructGUI(GUIComponent guiComponent) 
	{ 
		this.guiComponent = guiComponent;
		toolcomponent=new ToolComponent();
		toolscroll=new JScrollPane(toolcomponent);
		toolscroll.setBounds(0,0,toolcomponent.width+computer.resolution.datapath.toolComponent.getScrollbarWidth(),frameY-computer.resolution.datapath.getStatusBarThickness());
		guiComponent.add(toolscroll);
		drawingcomponent=new DrawingComponent();
		guiComponent.add(drawingcomponent.scroll);
		drawingcomponent.addMouseListener(new MouseListener(){
			public void mouseClicked(MouseEvent arg0) 
			{
				//on a mouse click, toggle the selection status of a block
				if (arg0.getButton()==MouseEvent.BUTTON1)
				{
					if (action.equals(""))
					{
						for (Block b:defaultModule.blocks)
						{
							if (b.doSelect(getX(arg0), getY(arg0)) && !b.selected)
								b.select();
							else if (b.doSelect(getX(arg0), getY(arg0)) && b.selected)
								b.unselect();
							
							//if it's an input pin, toggle value
							if (b.doSelect(getX(arg0), getY(arg0)) && computer.customProcessor!=null && b.type.equals("input pin"))
							{
								b.setValue(b.getValue()+1);
								propagateAll();
							}
						}
						for (Bus b:defaultModule.buses)
						{
							if (b.doSelect(getX(arg0), getY(arg0)) && !b.selected)
								b.select();
							else if (b.doSelect(getX(arg0), getY(arg0)) && b.selected)
								b.unselect();
						}
					}
					else if (action.equals("getpoint"))
					{
						mousex=getX(arg0);
						mousey=getY(arg0);
						clearAction();
						computer.datapathBuilder.toBack();
					}
					drawingcomponent.repaint();
				}
				
				//on button 3, unselect everybody
				//if button 3 is on top of a block, select it and open it for editing
				else if (arg0.getButton()==MouseEvent.BUTTON3)
				{
					clearAction();
					unselectAll();
					
					for (Block b:defaultModule.blocks)
					{
						if (b.doSelect(getX(arg0), getY(arg0)))
						{
							b.select();
							b.edit();
							return;
						}
					}
					for (Bus b:defaultModule.buses)
					{
						if (b.doSelect(getX(arg0), getY(arg0)))
						{
							b.select();
							b.edit();
							return;
						}
					}
				}
			}
			public void mouseEntered(MouseEvent arg0) {}
			public void mouseExited(MouseEvent arg0) 
			{
//				clearAction();
			}
			public void mousePressed(MouseEvent arg0) 
			{
				if (arg0.getButton()!=MouseEvent.BUTTON1) return;
				//when press, prepare to move
				if (action.equals("")&&arg0.getButton()==MouseEvent.BUTTON1)
				{
					mousex=getX(arg0);
					mousey=getY(arg0);
					action="move";
				}
				else if (action.equals("massselect"))
				{
					setStatusLabel("Drag and release to select blocks");
					corner1x=getX(arg0);
					corner1y=getY(arg0);
				}
				else if (action.equals("duplicate"))
				{
					setStatusLabel("Click on the top left of where you want to paste");
					duplicate(getX(arg0),getY(arg0));					
				}
				else if (action.equals("route"))
				{
					setStatusLabel("Release on the sink component");
					mousex=getX(arg0);
					mousey=getY(arg0);			
				}
				//user adds a new block
				else if (action.equals("place"))
				{
					//if not a bus, create the block at the coordinates and add it to the list
					if (component.equals("module"))
					{
						ModuleBlock newb=new ModuleBlock(drawingcomponent.tempblock.type, defaultModule, ((ModuleBlock)drawingcomponent.tempblock).absolutePath, ((ModuleBlock)drawingcomponent.tempblock).filename);
						newb.place(getX(arg0),getY(arg0));
						defaultModule.addBlock(newb);						
					}
					else if (!component.equals("bus"))
					{
//						Block newb=new Block(component,Integer.parseInt(toolcomponent.bitbox.getText()));
						Block newb=new Block(component,defaultbits,defaultModule);
						newb.place(getX(arg0),getY(arg0));
						defaultModule.addBlock(newb);
					}
					//it is a bus... initiate draw
					else if (component.equals("bus"))
					{
						//find out who is sourcing it
						Block b=null;
						Bus bu=null;
						for (Block bl:defaultModule.blocks)
							if (bl.getXExit(getX(arg0),getY(arg0))!=-1 && bl.getYExit(getX(arg0),getY(arg0))!=-1)
							{
								b=bl;
								break;
							}
						for (Bus bl: defaultModule.buses)
							if (bl.getXExit(getX(arg0),getY(arg0))!=-1 && bl.getYExit(getX(arg0),getY(arg0))!=-1)
							{
								bu=bl;
								break;
							}
						//a bus must have a source
						if (b==null && bu==null)
							return;

						setStatusLabel("Release at bus endpoint");
						//if it's sourced by a block, it will be vertical and start from the block's exit
						if (b!=null)
						{
							drawingcomponent.tempbus1=new Bus(b,defaultModule);
							drawingcomponent.tempbus1.isHorizontal=false;
							drawingcomponent.tempbus1.xcoor=b.getXExit(getX(arg0), getY(arg0));
							drawingcomponent.tempbus1.ycoor=b.getYExit(getX(arg0), getY(arg0));
							drawingcomponent.tempbus1.bits=b.getBits(drawingcomponent.tempbus1.xcoor);
							b.highlighted=true;
						}
						//if it's sourced by a bus, it will be perpendicular to the bus
						else
						{
							drawingcomponent.tempbus1=new Bus(bu,defaultModule);
							drawingcomponent.tempbus1.isHorizontal=!bu.isHorizontal;
							drawingcomponent.tempbus1.xcoor=bu.getXExit(getX(arg0), getY(arg0));
							drawingcomponent.tempbus1.ycoor=bu.getYExit(getX(arg0), getY(arg0));
							bu.highlighted=true;
						}
						//set up a second bus to turn corners
						drawingcomponent.tempbus1.xcoor2=drawingcomponent.tempbus1.xcoor;
						drawingcomponent.tempbus1.ycoor2=drawingcomponent.tempbus1.ycoor;
						drawingcomponent.tempbus2=new Bus(drawingcomponent.tempbus1,defaultModule);
						drawingcomponent.tempbus2.isHorizontal=!drawingcomponent.tempbus1.isHorizontal;
						if (b!=null)
						{
							drawingcomponent.tempbus2.xcoor=b.getXExit(getX(arg0), getY(arg0));
							drawingcomponent.tempbus2.ycoor=b.getYExit(getX(arg0), getY(arg0));
						}
						else
						{
							drawingcomponent.tempbus2.xcoor=bu.getXExit(getX(arg0), getY(arg0));
							drawingcomponent.tempbus2.ycoor=bu.getYExit(getX(arg0), getY(arg0));							
						}
						drawingcomponent.tempbus2.xcoor2=drawingcomponent.tempbus2.xcoor;
						drawingcomponent.tempbus2.ycoor2=drawingcomponent.tempbus2.ycoor;
					}
				}
				drawingcomponent.repaint();				
			}
			public void mouseReleased(MouseEvent arg0) 
			{
				if (arg0.getButton()!=MouseEvent.BUTTON1) return;
				//finished creating a bus?
				if (action.equals("place")&&component.equals("bus")&&drawingcomponent.tempbus1!=null)
				{
					//get all the coordinates
					if (drawingcomponent.tempbus1.isHorizontal)
					{
						drawingcomponent.tempbus1.xcoor2=getX(arg0);
						drawingcomponent.tempbus2.xcoor=getX(arg0);
						drawingcomponent.tempbus2.xcoor2=getX(arg0);
						drawingcomponent.tempbus2.ycoor2=getY(arg0);
					}
					else
					{
						drawingcomponent.tempbus1.ycoor2=getY(arg0);
						drawingcomponent.tempbus2.ycoor=getY(arg0);
						drawingcomponent.tempbus2.ycoor2=getY(arg0);
						drawingcomponent.tempbus2.xcoor2=getX(arg0);						
					}
					//check if either bus is invalid and diagonal
					if (drawingcomponent.tempbus1.xcoor!=drawingcomponent.tempbus1.xcoor2 && drawingcomponent.tempbus1.ycoor!=drawingcomponent.tempbus1.ycoor2)
					{
						clearBusAction();
						return;						
					}
					if (drawingcomponent.tempbus2.xcoor!=drawingcomponent.tempbus1.xcoor2 && drawingcomponent.tempbus1.ycoor!=drawingcomponent.tempbus2.ycoor2)
					{
						clearBusAction();
						return;						
					}
					//instantiate bus 1
					Bus b1=drawingcomponent.tempbus1;
					b1.place(drawingcomponent.tempbus1.xcoor, drawingcomponent.tempbus1.ycoor, drawingcomponent.tempbus1.xcoor2, drawingcomponent.tempbus1.ycoor2, drawingcomponent.tempbus1.input, 0);
					//if bus one is a single pixel, replace it with bus 2
					if (b1.xcoor==b1.xcoor2 && b1.ycoor==b1.ycoor2)
					{
						b1=drawingcomponent.tempbus2;
						b1.place(drawingcomponent.tempbus2.xcoor, drawingcomponent.tempbus2.ycoor, drawingcomponent.tempbus2.xcoor2, drawingcomponent.tempbus2.ycoor2, b1.number, 0);
						//if both buses are a single pixel, cancel the operation
						if (b1.xcoor==b1.xcoor2 && b1.ycoor==b1.ycoor2)
						{
							clearBusAction();
							return;
						}
					}

					//find out which block this new bus is sourcing
					for (Block bl:defaultModule.blocks)
					{
						if (bl.getXEntrance(b1.xcoor2,b1.ycoor2)!=-1 && bl.getYEntrance(b1.xcoor2,b1.ycoor2)!=-1)
						{
							b1.output=bl.number;
							b1.xcoor2=bl.getXEntrance(b1.xcoor2,b1.ycoor2);
							b1.ycoor2=bl.getYEntrance(b1.xcoor2,b1.ycoor2);
							clearBusAction();
							if (b1.xcoor!=b1.xcoor2 && b1.ycoor!=b1.ycoor2) return;
							defaultModule.addBlock(b1);
							return;
						}
					}
					//no block? then look for a bus
					for (Bus bu:defaultModule.buses)
					{
						//only source if it's an orphan bus
						if (bu.input!=0) continue;
						
						if (bu.getXEntrance(b1.xcoor2,b1.ycoor2)!=-1 && bu.getYEntrance(b1.xcoor2,b1.ycoor2)!=-1)
						{
							b1.output=bu.number;
							b1.xcoor2=bu.getXEntrance(b1.xcoor2,b1.ycoor2);
							b1.ycoor2=bu.getYEntrance(b1.xcoor2,b1.ycoor2);
							clearBusAction();
							if (b1.xcoor!=b1.xcoor2 && b1.ycoor!=b1.ycoor2) return;
							defaultModule.addBlock(b1);
							bu.input=b1.number;
							b1.output=bu.number;
							return;
						}
					}
					defaultModule.addBlock(b1);
						
					//only add bus2 if bus1 didn't hook up to a component, and it's bigger than 1 pixel
					Bus b2=new Bus(b1,defaultModule);
					b2.place(drawingcomponent.tempbus2.xcoor, drawingcomponent.tempbus2.ycoor, drawingcomponent.tempbus2.xcoor2, drawingcomponent.tempbus2.ycoor2, b1.number, 0);
					if (b2.xcoor==b2.xcoor2 && b2.ycoor==b2.ycoor2)
					{
						clearBusAction();
						return;
					}
					
					//look for a block, then a bus, for bus2 to connect to
					for (Block bl:defaultModule.blocks)
					{
						if (bl.getXEntrance(b2.xcoor2,b2.ycoor2)!=-1 && bl.getYEntrance(b2.xcoor2,b2.ycoor2)!=-1)
						{
							b2.output=bl.number;
							b2.xcoor2=bl.getXEntrance(b2.xcoor2,b2.ycoor2);
							b2.ycoor2=bl.getYEntrance(b2.xcoor2,b2.ycoor2);
							clearBusAction();
							if (b2.xcoor!=b2.xcoor2 && b2.ycoor!=b2.ycoor2) return;
							defaultModule.addBlock(b2);
							return;
						}
					}
					for (Bus bu:defaultModule.buses)
					{
						//only source if it's an orphan bus
						if (bu.input!=0) continue;
						
						if (bu.getXEntrance(b2.xcoor2,b2.ycoor2)!=-1 && bu.getYEntrance(b2.xcoor2,b2.ycoor2)!=-1)
						{
							b2.output=bu.number;
							b2.xcoor2=bu.getXEntrance(b2.xcoor2,b2.ycoor2);
							b2.ycoor2=bu.getYEntrance(b2.xcoor2,b2.ycoor2);
							clearBusAction();
							if (b2.xcoor!=b2.xcoor2 && b2.ycoor!=b2.ycoor2) return;
							defaultModule.addBlock(b2);
							bu.input=b2.number;
							return;
						}
					}
					//bus 2 goes nowhere, but add it anyway
					defaultModule.addBlock(b2);
					clearBusAction();
				}
				
				//route is called by the control unit to select two blocks
				else if (action.equals("route"))
				{
					action="";
					setStatusLabel("");
					Block block1=null,block2=null;
					for (Block b: defaultModule.blocks)
						if (b.doSelect(mousex, mousey))
							block1=b;
					for (Block b: defaultModule.blocks)
						if (b.doSelect(getX(arg0), getY(arg0)))
							block2=b;
					if (block1!=null && block2!=null)
					{
						computer.controlBuilder.controlControl.route(block1.name+" "+block2.name);
//						computer.controlBuilder.controlControl.edit.setText(block1.name+" "+block2.name);
//						computer.controlBuilder.toFront();
					}
				}				
				else if (action.equals("move"))
				{
					undolog.push(dumpXML());
					for (Block b:defaultModule.blocks)
					{
						if (b.selected)
						{
							b.xcoor+=(getX(arg0)-mousex);
							b.ycoor+=(getY(arg0)-mousey);
							b.xcoor2+=(getX(arg0)-mousex);
							b.ycoor2+=(getY(arg0)-mousey);
						}
					}
					for (Bus b:defaultModule.buses)
					{
						if (b.selected)
						{
							b.xcoor+=(getX(arg0)-mousex);
							b.ycoor+=(getY(arg0)-mousey);
							b.xcoor2+=(getX(arg0)-mousex);
							b.ycoor2+=(getY(arg0)-mousey);
						}
					}
					clearAction();
					drawingcomponent.repaint();
				}
				else if (action.equals("massselect"))
				{
					//make sure corner1 is top left and corner2 bottom right
					if (corner1x<getX(arg0))
					{
						corner2x=getX(arg0);
					}
					else
					{
						corner2x=corner1x;
						corner1x=getX(arg0);
					}
					if (corner1y<getY(arg0))
					{
						corner2y=getY(arg0);
					}
					else
					{
						corner2y=corner1y;
						corner1y=getY(arg0);
					}
					//go through all the pixels between corners and select blocks
					for(int x=corner1x; x<=corner2x; x++)
					{
						for (int y=corner1y; y<=corner2y; y++)
						{
							for (Block b:defaultModule.blocks)
							{
								if (b.doSelect(x,y) && !b.selected)
									b.select();
							}
							for (Bus b:defaultModule.buses)
							{
								if (b.doSelect(x,y) && !b.selected)
									b.select();
							}							
						}
					}
					clearAction();
				}
			}
			});
		drawingcomponent.addMouseMotionListener(new MouseMotionListener(){
			public void mouseDragged(MouseEvent arg0) 
			{
				//if dragging and a bus is started, show where the bus is going
				//if the bus looks like it will connect a component, highlight that component
				if (action.equals("place")&&component.equals("bus")&&drawingcomponent.tempbus1!=null)
				{
					if (drawingcomponent.tempbus1.isHorizontal)
					{
						drawingcomponent.tempbus1.xcoor2=getX(arg0);
						drawingcomponent.tempbus2.xcoor=getX(arg0);
						drawingcomponent.tempbus2.xcoor2=getX(arg0);
						drawingcomponent.tempbus2.ycoor2=getY(arg0);
					}
					else
					{
						drawingcomponent.tempbus1.ycoor2=getY(arg0);
						drawingcomponent.tempbus2.ycoor=getY(arg0);
						drawingcomponent.tempbus2.ycoor2=getY(arg0);
						drawingcomponent.tempbus2.xcoor2=getX(arg0);						
					}
					for(Block b:defaultModule.blocks)
					{
						if (b.getXEntrance(getX(arg0),getY(arg0))!=-1 && b.getYEntrance(getX(arg0),getY(arg0))!=-1)
							b.highlighted=true;
						else
							b.highlighted=false;
					}
					for(Bus bu:defaultModule.buses)
					{
						if (bu.input!=0) continue;
						if (bu.getXEntrance(getX(arg0),getY(arg0))!=-1 && bu.getYEntrance(getX(arg0),getY(arg0))!=-1)
							bu.highlighted=true;
						else
							bu.highlighted=false;
					}
					drawingcomponent.repaint();
				}
			}
			public void mouseMoved(MouseEvent arg0) 
			{
				if (action.equals("place")&&drawingcomponent.tempblock!=null)
				{
					drawingcomponent.tempblock.place(getX(arg0), getY(arg0));
					drawingcomponent.repaint();
				}
			}
			});
		drawingcomponent.addKeyListener(new KeyListener(){
			public void keyPressed(KeyEvent arg0) {
//				System.out.println(arg0.getKeyCode());
				if (arg0.getKeyCode()==KeyEvent.VK_DELETE)
				{
					delete();
					drawingcomponent.repaint();
				}
				if (arg0.getKeyCode()==KeyEvent.VK_ESCAPE)
				{
					unselectAll();
					clearAction();
					drawingcomponent.repaint();
				}
				if (arg0.getKeyCode()==KeyEvent.VK_B)
				{
					action="place";
					component="bus";
					setStatusLabel("Press the mouse on a block and drag to draw a bus");
					drawingcomponent.requestFocus();
				}
				if (arg0.getKeyCode()==KeyEvent.VK_Z)
				{
					undo();
					drawingcomponent.repaint();
				}
			}
			public void keyReleased(KeyEvent arg0) {}
			public void keyTyped(KeyEvent arg0) {}
			});
		drawingcomponent.setFocusable(true);

	}
	private void clearAction()
	{
		action="";
		component="";
		drawingcomponent.tempblock=null;
		drawingcomponent.tempbus1=null;
		drawingcomponent.tempbus2=null;
		setStatusLabel("");
		for(Block b:defaultModule.blocks)
		{
			b.highlighted=false;
		}
		for(Bus b:defaultModule.buses)
		{
			b.highlighted=false;
		}
		drawingcomponent.repaint();
	}
	private void clearBusAction()
	{
		drawingcomponent.tempblock=null;
		drawingcomponent.tempbus1=null;
		drawingcomponent.tempbus2=null;
		for(Block b:defaultModule.blocks)
		{
			b.highlighted=false;
		}
		for(Bus b:defaultModule.buses)
		{
			b.highlighted=false;
		}
		drawingcomponent.repaint();
		setStatusLabel("Press the mouse on a block and drag to draw a bus");
	}
	private void clearAll()
	{
		defaultModule.blocks=new ArrayList<Block>();
		defaultModule.buses=new ArrayList<Bus>();
		defaultModule.blocknumber=1;
		drawingcomponent.repaint();
	}
	public void unselectAll()
	{
		for (Block b:defaultModule.blocks)
			b.unselect();
		for (Bus b:defaultModule.buses)
			b.unselect();
		errorlog=new ArrayList<ErrorEntry>();
		repaint();
	}
	public boolean verify()
	{
		errorlog=new ArrayList<ErrorEntry>();
		for (Block b:defaultModule.blocks)
		{
			if (!b.verify())
				b.selected=true;
			else
				b.selected=false;
		}
		for (Bus b:defaultModule.buses)
		{
			if (!b.verify())
				b.selected=true;
			else
				b.selected=false;
		}
		if (errorlog.size()>0)
			setStatusLabel("There are errors in the datapath.  Bad blocks are highlighted.");
		else
			setStatusLabel("Success: no errors were found in the datapath");
		repaint();
		return errorlog.size()==0;
	}
	
	public void simulate()
	{
		if (!verify())
		{
			setStatusLabel("Can't simulate: there are errors in the datapath");
			return;
		}
		computer.customProcessor=new CustomProcessor(computer);
		this.toFront();
	}
	
	//duplicate all the selected pieces
	public void duplicate(int newx, int newy)
	{
		//save undo info
		undolog.push(dumpXML());

		ArrayList<Block> blockstoadd=new ArrayList<Block>();
		ArrayList<Bus> busestoadd=new ArrayList<Bus>();
		int basenumber=defaultModule.blocknumber;

		for (Bus b:defaultModule.buses)
		{
			if (b.selected)
			{
				Bus b2=new Bus(b);
				b2.number=b.number+basenumber;
				if (defaultModule.getPart(b2.input)!=null)
				{
					if (!defaultModule.getPart(b2.input).selected)
						b2.input=0;
					else
						b2.input+=basenumber;
				}
				if (defaultModule.getPart(b2.output)!=null)
				{
					if (!defaultModule.getPart(b2.output).selected)
						b2.output=0;
					else
						b2.output+=basenumber;
				}				
				busestoadd.add(b2);
			}
		}
		for (Block b:defaultModule.blocks)
		{
			if (b.selected)
			{
				Block b2;
				if (b.type.equals("module"))
					b2=new ModuleBlock((ModuleBlock)b);
				else
					b2=new Block(b);
				b2.number=b.number+basenumber;
				if (b2.bus!=null)
				{
					for (Enumeration<Integer> e=b.bus.keys(); e.hasMoreElements();)
					{
						Integer i=e.nextElement();
						if (defaultModule.getBus(i.intValue()).selected)
							b2.bus.put(new Integer(basenumber+i.intValue()),b2.bus.get(i));
						b2.bus.remove(i);
					}
				}
				if (b2.type.equals("module"))
				{
					for (ModuleBlock.Pin p: ((ModuleBlock)b2).inputpin)
					{
						if (!defaultModule.getBus(p.bus).selected)
							p.bus=-1;
						else
							p.bus+=basenumber;
					}
					for (ModuleBlock.Pin p: ((ModuleBlock)b2).outputpin)
					{
						if (!defaultModule.getBus(p.bus).selected)
							p.bus=-1;
						else
							p.bus+=basenumber;
					}
				}
				blockstoadd.add(b2);
			}
		}
		//find the top left
		int xcoor=blockstoadd.size()==0? busestoadd.size()==0? 0:busestoadd.get(0).xcoor : blockstoadd.get(0).xcoor;
		int ycoor=blockstoadd.size()==0? busestoadd.size()==0? 0:busestoadd.get(0).ycoor : blockstoadd.get(0).ycoor;
		for (Block b: blockstoadd)
		{
			if (b.xcoor<xcoor) xcoor=b.xcoor;
			if (b.ycoor<ycoor) ycoor=b.ycoor;
		}
		for (Bus b: busestoadd)
		{
			if (b.xcoor<xcoor) xcoor=b.xcoor;
			if (b.ycoor<ycoor) ycoor=b.ycoor;
		}
		int xshift=newx-xcoor; int yshift=newy-ycoor;
		for (Block b:blockstoadd)
		{
			b.xcoor+=xshift; b.xcoor2+=xshift; b.ycoor+=yshift; b.ycoor2+=yshift;
		}
		for (Bus b:busestoadd)
		{
			b.xcoor+=xshift; b.xcoor2+=xshift; b.ycoor+=yshift; b.ycoor2+=yshift;
		}		
		for (Block b:blockstoadd)
			defaultModule.blocks.add(b);
		for (Bus b:busestoadd)
			defaultModule.buses.add(b);
		defaultModule.blocknumber+=(blockstoadd.size()+busestoadd.size());
		this.repaint();
	}
	
	//remove a component
	public void delete()
	{
		//save undo info
		undolog.push(dumpXML());
		ArrayList<Block> removelist=new ArrayList<Block>();
		ArrayList<Bus> removebuslist=new ArrayList<Bus>();
		//make a remove list first so nothing is disrupted when removing
		for (Bus b: defaultModule.buses)
		{
			if (b.selected)
				removebuslist.add(b);		
		}
		for (Bus b: removebuslist)
		{			
			//if the bus is sourced by a splitter, remove bus from splitter list
			if (b.input!=0 && defaultModule.getBlock(b.input)!=null && defaultModule.getBlock(b.input).type.equals("splitter"))
			{
				Block splitter=(Block)b.getInputBlocks()[0];
				for (Enumeration e=splitter.bus.keys(); e.hasMoreElements();)
				{
					Integer splitterkey=(Integer)(e.nextElement());
					int i=splitterkey.intValue();

					if (b.number==i)
						splitter.bus.remove(splitterkey);
				}				
			}
			if (b.input!=0 && defaultModule.getBlock(b.input)!=null && defaultModule.getBlock(b.input).type.equals("module"))
				((ModuleBlock)defaultModule.getBlock(b.input)).deleteOutputBus(b.xcoor2);
			if (b.output!=0 && defaultModule.getBlock(b.output)!=null && defaultModule.getBlock(b.output).type.equals("module"))
				((ModuleBlock)defaultModule.getBlock(b.output)).deleteInputBus(b.xcoor);
			//now remove the bus
			defaultModule.buses.remove(b);
			//and unlink any buses sourcing or sinking this bus
			for (Bus bb: defaultModule.buses)
			{
				if (bb.input==b.number)
					bb.input=0;
				if (bb.output==b.number)
					bb.output=0;
			}
		}
		//make a remove list of blocks
		for (Block b:defaultModule.blocks)
		{
			if (b.selected)
				removelist.add(b);
		}
		//remove the blocks, and unlink any buses connected to them
		for (Block b: removelist)
		{
			defaultModule.blocks.remove(b);
			for (Bus bb: defaultModule.buses)
			{
				if (bb.input==b.number)
					bb.input=0;
				if (bb.output==b.number)
					bb.output=0;
			}
		}
		repaint();
	}
	public void clockAll()
	{
		defaultModule.clockAll();
		repaint();
	}
	public void resetClocks()
	{
		defaultModule.resetClocks();
	}	
	public void resetHighlights()
	{
		defaultModule.resetHighlights();
	}	
	public void resetAll()
	{
		defaultModule.resetAll();
		repaint();
	}
	public void propagateAll()
	{
		defaultModule.propagateAll();
	}

	public void doPaint(Graphics g) 
	{ 
		toolcomponent.repaint();
		drawingcomponent.repaint();
	}
	
	public String dumpXML()
	{
		return defaultModule.dumpXML();
	}
	public String[] controlOutputs()
	{
		return defaultModule.controlOutputs();
	}

	public String[] controlInputs()
	{
		return defaultModule.controlInputs();
	}

	public class ModificationComponent extends JComponent
	{
		int width=100;
		int height=computer.resolution.desktop.pane.height;
		JLabel[] itemlabel;
		JTextField[] itemfield;
		JButton saveChanges;

		final int NAME=1, BITS=2, DESCRIPTION=3, WIDTH=4, VALUE=5, STARTBIT=6, ENDBIT=7, INDEX=8, SOURCE=9;
		final String[] labels=new String[]{"","Name:","Bits:","Description:","Pixels wide:", "Value:","Low bit:","High bit:","Index:", "Sourced by: "};
		final int TYPES=10;
		
		int currentBlock;
		int currentBus;

		public ModificationComponent(int block,int bus)
		{
			super();
			currentBlock=block; 
			currentBus=bus;
			width = computer.resolution.datapath.modificationComponent.width;
			int ctop=0;
			itemlabel=new JLabel[TYPES];
			itemfield=new JTextField[TYPES];
			int cwidth=width-10;

			
			for (int i=1; i<TYPES; i++)
			{
				itemlabel[i]=new JLabel(labels[i]);
				itemlabel[i].setFont(new Font("Dialog",Font.BOLD,computer.resolution.desktop.getFontSize()));
				itemfield[i]=new JTextField("");
				itemfield[i].setFont(new Font("Dialog",Font.PLAIN,computer.resolution.desktop.getFontSize()));
				itemfield[i].setText("");
				itemfield[i].setEnabled(true);
				itemfield[i].setVisible(false);
			}

			final Block b=defaultModule.getBlock(currentBlock);
			final Bus bu=defaultModule.getBus(currentBus);
			if(b!=null)
			{
				itemfield[NAME].setText(b.name);
				itemfield[DESCRIPTION].setText(b.description);
				itemfield[BITS].setText(""+b.bits);
				itemfield[WIDTH].setText(""+(b.xcoor2-b.xcoor));

				itemfield[NAME].setVisible(true);
				itemfield[DESCRIPTION].setVisible(true);
				itemfield[BITS].setVisible(true);
				itemfield[WIDTH].setVisible(true);
				
				if (computer.customProcessor!=null || b.type.equals("lookup table"))
				{
					itemfield[VALUE].setText(""+Long.toHexString(b.value));
					itemfield[VALUE].setVisible(true);
						if(b.type.equals("register file") || b.type.equals("memory") || b.type.equals("ports") || b.type.equals("lookup table"))
						{
							if (b.getAddressInputBlock().length>0)
								itemfield[INDEX].setText(Long.toHexString(b.getAddressInputBlock()[0].getValue()));
							else
								itemfield[INDEX].setText("0");
							itemfield[VALUE].setText(""+Long.toHexString(b.getValue(Integer.parseInt(itemfield[INDEX].getText(),16))));
							itemfield[INDEX].addKeyListener(new KeyListener(){
								public void keyTyped(KeyEvent arg0) {}
								public void keyReleased(KeyEvent arg0) {}
								public void keyPressed(KeyEvent arg0) 
								{
									if (arg0.getKeyCode()==KeyEvent.VK_ENTER)
									{
										itemfield[VALUE].setText(""+Long.toHexString(b.getValue(Integer.parseInt(itemfield[INDEX].getText(),16))));
									}
								}});
							itemfield[INDEX].setVisible(true);
						}
				}
			}
			else if (bu!=null)
			{
				itemfield[NAME].setText(bu.name);
				itemfield[DESCRIPTION].setText(bu.description);
				itemfield[BITS].setText(""+bu.bits);
				itemfield[VALUE].setText(""+Long.toHexString(bu.value));
				itemfield[SOURCE].setText(""+bu.input);

				itemfield[NAME].setVisible(true);
				itemfield[DESCRIPTION].setVisible(true);
				itemfield[BITS].setVisible(true);
				itemfield[VALUE].setVisible(true);
				itemfield[SOURCE].setVisible(true);
				
				if (bu.getInputBlocks().length>0 && bu.getInputBlocks()[0]!=null && bu.getInputBlocks()[0].type.equals("splitter"))
				{
					itemfield[STARTBIT].setVisible(true);
					itemfield[ENDBIT].setVisible(true);
					
					Block splitter=(Block)bu.getInputBlocks()[0];
					itemfield[ENDBIT].setText(""+(bu.bits-1));
					itemfield[STARTBIT].setText("0");						
					for (Enumeration e=splitter.bus.keys(); e.hasMoreElements();)
					{
						Integer splitterkey=(Integer)(e.nextElement());
						int i=splitterkey.intValue();

						if (bu.number==i)
						{
							String busstring=(String)splitter.bus.get(splitterkey);
							int b1=Integer.parseInt(busstring.substring(0,busstring.indexOf(":")));
							int b2=Integer.parseInt(busstring.substring(busstring.indexOf(":")+1,busstring.length()));
							itemfield[ENDBIT].setText(""+b1);
							itemfield[STARTBIT].setText(""+b2);
							break;
						}
					}
				}
			}
			
			if (currentBlock!=-1)
				itemlabel[0]=new JLabel(b.type+" "+b.number);
			else
				itemlabel[0]=new JLabel("Bus "+bu.number);
			itemlabel[0].setFont(new Font("Dialog",Font.BOLD,computer.resolution.desktop.getFontSize()));
			itemlabel[0].setBounds(5,ctop+=computer.resolution.datapath.modificationComponent.getButtonHeightAndSpace(),cwidth,computer.resolution.datapath.modificationComponent.getButtonHeight());
			add(itemlabel[0]);

			for (int i=1; i<TYPES; i++)
			{
				if (itemfield[i].isVisible())
				{
					itemlabel[i].setBounds(5,ctop+=computer.resolution.datapath.modificationComponent.getButtonHeightAndSpace(),cwidth,computer.resolution.datapath.modificationComponent.getButtonHeight());
					add(itemlabel[i]);
					itemfield[i].setBounds(5,ctop+=computer.resolution.datapath.modificationComponent.getButtonHeightAndSpace(),cwidth,computer.resolution.datapath.modificationComponent.getButtonHeight());
					add(itemfield[i]);
				}
			}

			saveChanges=new JButton("Update");
			saveChanges.setFont(new Font("Dialog",Font.PLAIN,computer.resolution.desktop.getFontSize()));
			saveChanges.setBounds(5,ctop+=computer.resolution.datapath.modificationComponent.getButtonHeightAndSpace(),cwidth,computer.resolution.datapath.modificationComponent.getButtonHeight());
			saveChanges.setVisible(true);
			saveChanges.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent arg0) {
					undolog.push(dumpXML());
					Block b=defaultModule.getBlock(currentBlock);
					Bus bu=defaultModule.getBus(currentBus);
					if(b!=null)
					{
						b.name=itemfield[NAME].getText();
						b.description=itemfield[DESCRIPTION].getText();
						b.bits=Integer.parseInt(itemfield[BITS].getText());
						b.xcoor2=Integer.parseInt(itemfield[WIDTH].getText())+b.xcoor;
						if (itemfield[VALUE].isVisible())
						{
							if(b.type.equals("register file") || b.type.equals("memory") || b.type.equals("ports") || b.type.equals("lookup table"))
								b.setValue(Integer.parseInt(itemfield[INDEX].getText(),16),Long.parseLong(itemfield[VALUE].getText(),16));
							else
								b.setValue(Long.parseLong(itemfield[VALUE].getText(),16));								
							propagateAll();
						}
					}
					else if (bu!=null)
					{
						bu.name=itemfield[NAME].getText();
						bu.description=itemfield[DESCRIPTION].getText();
						bu.bits=Integer.parseInt(itemfield[BITS].getText());
						bu.input=Integer.parseInt(itemfield[SOURCE].getText());
						
						if (itemfield[STARTBIT].isVisible())
						{
							Block splitter=(Block)bu.getInputBlocks()[0];
							for (Enumeration e=splitter.bus.keys(); e.hasMoreElements();)
							{
								Integer splitterkey=(Integer)(e.nextElement());
								int i=splitterkey.intValue();

								if (bu.number==i)
									splitter.bus.remove(splitterkey);
							}
							String busstring=itemfield[ENDBIT].getText()+":"+itemfield[STARTBIT].getText();
							splitter.bus.put(new Integer(bu.number), busstring);
							bu.bits=Integer.parseInt(itemfield[ENDBIT].getText())-Integer.parseInt(itemfield[STARTBIT].getText())+1;
							itemfield[BITS].setText(""+bu.bits);							
						}
						if (itemfield[VALUE].isVisible())
						{
							bu.setValue(Long.parseLong(itemfield[VALUE].getText(),16));
							propagateAll();
						}
					}
					defaultbits=Integer.parseInt(itemfield[BITS].getText());
//					unselectAll();
					drawingcomponent.repaint();
					drawingcomponent.requestFocus();
				}});
			
			height = computer.resolution.monitor.height > ctop ? computer.resolution.monitor.height : ctop;
			saveChanges.setEnabled(true);
			add(saveChanges);
			
			modificationScroll=new JScrollPane(this);

			guiComponent.add(modificationScroll);
			restoreSize();
			guiComponent.revalidate();
			
		}
		public void restoreSize() {
			modificationScroll.setBounds(toolscroll.getWidth(), 0,width + computer.resolution.datapath.toolComponent.getScrollbarWidth(),frameY-computer.resolution.datapath.getStatusBarThickness());
			drawingcomponent.setLeft(toolscroll.getWidth() + width + MARGIN);
			drawingcomponent.restoreSize();
		}
		public void paintComponent(Graphics g)
		{
			g.setColor(Color.WHITE);
			g.fillRect(0,0,width,height);
			g.setColor(Color.BLACK);
			g.drawLine(width,0,width,height);
		}
		public Dimension getPreferredSize()
		{
			return new Dimension(width,height);
		}
		public void dispose()
		{
			guiComponent.remove(modificationScroll);
			modificationcomponent=null;
			modificationScroll = null;
			drawingcomponent.resetLeft();
			drawingcomponent.restoreSize();
			guiComponent.revalidate();
		}
	}
	
	public class ToolComponent extends JComponent
	{
		int width=100;
		int height;
		int line1,line2;
		
		public Dimension getPreferredSize()
		{
			return new Dimension(width, height);
		}
		public ToolComponent()
		{
			super();
			int ctop=10;
			
			width = computer.resolution.datapath.toolComponent.width;
			
			JButton button;
			JLabel label;
			int cwidth=width-10;
						
			button=new JButton("Close");
			button.setFont(new Font("Dialog",Font.PLAIN,computer.resolution.datapath.toolComponent.getFontSize()));
			button.setBounds(5,ctop,cwidth,computer.resolution.datapath.toolComponent.getButtonHeight());
			button.setToolTipText("Close this window.  All unsaved work will be lost.");
			button.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent arg0) {
					close();
				}});
			add(button);
			ctop+=computer.resolution.datapath.toolComponent.getButtonHeightAndSpace();
			button=new JButton("Load");
			button.setFont(new Font("Dialog",Font.PLAIN,computer.resolution.datapath.toolComponent.getFontSize()));
			button.setBounds(5,ctop,cwidth,computer.resolution.datapath.toolComponent.getButtonHeight());
			button.setToolTipText("Load a previously saved datapath from an xml file");
			button.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent arg0) {
					doload();
				}});
			add(button);
			ctop+=computer.resolution.datapath.toolComponent.getButtonHeightAndSpace();
			button=new JButton("Save");
			button.setFont(new Font("Dialog",Font.PLAIN,computer.resolution.datapath.toolComponent.getFontSize()));
			button.setBounds(5,ctop,cwidth,computer.resolution.datapath.toolComponent.getButtonHeight());
			button.setToolTipText("Save the datapath as an xml file");
			button.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent arg0) {
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
					catch(Exception x)
					{
						x.printStackTrace();
					}
					drawingcomponent.requestFocus();
				}});
			add(button);
			ctop+=computer.resolution.datapath.toolComponent.getButtonHeightAndSpace();
			button=new JButton("Export");
			button.setFont(new Font("Dialog",Font.PLAIN,computer.resolution.datapath.toolComponent.getFontSize()));
			button.setBounds(5,ctop,cwidth,computer.resolution.datapath.toolComponent.getButtonHeight());
			button.setToolTipText("Save the datapath as an Arduino C program");
			button.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent arg0) {
					System.out.println((new ToArduino(dumpXML())).getC());
					JFileChooser fc = new JFileChooser();
					fc.setCurrentDirectory(new File("."));
					fc.showSaveDialog(null);
					File f = fc.getSelectedFile();
					if (f==null) return;
					String name=f.getAbsolutePath();
					try
					{
						PrintWriter p =new PrintWriter(name);
						p.println((new ToArduino(dumpXML())).getC());
						p.close();
					}
					catch(IOException x)
					{
						System.out.println("Error creating file "+name);
					}	
					catch(Exception x)
					{
						x.printStackTrace();
					}
					drawingcomponent.requestFocus();
				}});
			add(button);
			ctop+=computer.resolution.datapath.toolComponent.getButtonHeightAndSpace();
			button=new JButton("ExportVerilog");
			button.setFont(new Font("Dialog",Font.PLAIN,computer.resolution.datapath.toolComponent.getFontSize()));
			button.setBounds(5,ctop,cwidth,computer.resolution.datapath.toolComponent.getButtonHeight());
			button.setToolTipText("Save the datapath as a Verilog program");
			button.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent arg0) {
					System.out.println((new ToVerilog(dumpXML())).getVerilog());
					JFileChooser fc = new JFileChooser();
					fc.setCurrentDirectory(new File("."));
					fc.showSaveDialog(null);
					File f = fc.getSelectedFile();
					if (f==null) return;
					String name=f.getAbsolutePath();
					try
					{
						PrintWriter p =new PrintWriter(name);
						p.println((new ToVerilog(dumpXML())).getVerilog());
						p.close();
					}
					catch(IOException x)
					{
						System.out.println("Error creating file "+name);
					}	
					catch(Exception x)
					{
						x.printStackTrace();
					}
					drawingcomponent.requestFocus();
				}});
			add(button);
			ctop+=computer.resolution.datapath.toolComponent.getButtonHeightAndSpace();
			button=new JButton("Undo");
			button.setFont(new Font("Dialog",Font.PLAIN,computer.resolution.datapath.toolComponent.getFontSize()));
			button.setBounds(5,ctop,cwidth,computer.resolution.datapath.toolComponent.getButtonHeight());
			button.setToolTipText("Undo the last modification made to the datapath (also the Z key)");
			button.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent arg0) {
					undo();
					drawingcomponent.requestFocus();
				}});
			add(button);
			ctop+=computer.resolution.datapath.toolComponent.getButtonHeightAndSpace();
			button=new JButton("Unselect All");
			button.setFont(new Font("Dialog",Font.PLAIN,computer.resolution.datapath.toolComponent.getFontSize()));
			button.setBounds(5,ctop,cwidth,computer.resolution.datapath.toolComponent.getButtonHeight());
			button.setToolTipText("Unselect all datapath blocks (also done by right-clicking on an empty area)");
			button.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent arg0) {
					unselectAll();
					drawingcomponent.requestFocus();
				}});
			add(button);
			ctop+=computer.resolution.datapath.toolComponent.getButtonHeightAndSpace();
			button=new JButton("Mass Select");
			button.setFont(new Font("Dialog",Font.PLAIN,computer.resolution.datapath.toolComponent.getFontSize()));
			button.setBounds(5,ctop,cwidth,computer.resolution.datapath.toolComponent.getButtonHeight());
			button.setToolTipText("Select all blocks in a rectangular region");
			button.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent arg0) {
					action="massselect";
					drawingcomponent.requestFocus();
				}});
			add(button);
			ctop+=computer.resolution.datapath.toolComponent.getButtonHeightAndSpace();
			button=new JButton("Duplicate");
			button.setFont(new Font("Dialog",Font.PLAIN,computer.resolution.datapath.toolComponent.getFontSize()));
			button.setBounds(5,ctop,cwidth,computer.resolution.datapath.toolComponent.getButtonHeight());
			button.setToolTipText("Paste a new copy of all selected blocks");
			button.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent arg0) {
					action="duplicate";
					drawingcomponent.requestFocus();
				}});
			add(button);
			ctop+=computer.resolution.datapath.toolComponent.getButtonHeightAndSpace();
			button=new JButton("Delete");
			button.setFont(new Font("Dialog",Font.PLAIN,computer.resolution.datapath.toolComponent.getFontSize()));
			button.setBounds(5,ctop,cwidth,computer.resolution.datapath.toolComponent.getButtonHeight());
			button.setToolTipText("All selected blocks are removed (also DEL key)");
			button.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent arg0) {
					delete();
					drawingcomponent.requestFocus();
				}});
			add(button);
			ctop+=computer.resolution.datapath.toolComponent.getButtonHeightAndSpace();
			button=new JButton("Verify");
			button.setFont(new Font("Dialog",Font.PLAIN,computer.resolution.datapath.toolComponent.getFontSize()));
			button.setBounds(5,ctop,cwidth,computer.resolution.datapath.toolComponent.getButtonHeight());
			button.setToolTipText("Check for errors in the datapath.  Blocks with errors are selected.");
			button.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent arg0) {
					verify();
					drawingcomponent.requestFocus();
				}});
			add(button);
			ctop+=computer.resolution.datapath.toolComponent.getButtonHeightAndSpace();
			button=new JButton("Zoom In");
			button.setFont(new Font("Dialog",Font.PLAIN,computer.resolution.datapath.toolComponent.getFontSize()));
			button.setBounds(5,ctop,cwidth,computer.resolution.datapath.toolComponent.getButtonHeight());
			button.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent arg0) {
					scaling+=0.2;
					drawingcomponent.repaint();					
					drawingcomponent.requestFocus();
				}});
			add(button);
			ctop+=computer.resolution.datapath.toolComponent.getButtonHeightAndSpace();
			button=new JButton("Zoom Out");
			button.setFont(new Font("Dialog",Font.PLAIN,computer.resolution.datapath.toolComponent.getFontSize()));
			button.setBounds(5,ctop,cwidth,computer.resolution.datapath.toolComponent.getButtonHeight());
			button.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent arg0) {
					scaling-=0.2;
					if (scaling<1) scaling=1.0;
					drawingcomponent.repaint();					
					drawingcomponent.requestFocus();
				}});
			add(button);
			ctop+=computer.resolution.datapath.toolComponent.getButtonHeightAndSpace();
			button=new JButton("Control");
			button.setFont(new Font("Dialog",Font.PLAIN,computer.resolution.datapath.toolComponent.getFontSize()));
			button.setBounds(5,ctop,cwidth,computer.resolution.datapath.toolComponent.getButtonHeight());
			button.setToolTipText("Open up a new Control Builder window");
			button.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent arg0) {
					if (computer.controlBuilder==null)
					{
						computer.controlBuilder=new ControlBuilder(computer,computer.datapathBuilder.defaultModule);
					}
					else
					{
						setStatusLabel("Save and close the current Control to open another Control");
					}
				}});
			add(button);
			ctop+=computer.resolution.datapath.toolComponent.getButtonHeightAndSpace();
			button=new JButton("Simulate");
			button.setFont(new Font("Dialog",Font.BOLD,computer.resolution.datapath.toolComponent.getFontSize()));
			button.setBounds(5,ctop,cwidth,computer.resolution.datapath.toolComponent.getButtonHeight());
			button.setToolTipText("Start running your datapath.  If a datapath is already running, stop it.");
			button.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent arg0) {
					if (computer.customProcessor!=null)
					{
						computer.customProcessor=null;
						unselectAll();
						computer.datapathBuilder.repaint();
						computer.controlBuilder.repaint();
						setStatusLabel("Simulation stopped");
					}
					else
					{
						unselectAll();
						simulate();
						if (computer.customProcessor!=null)
							setStatusLabel("Simulation started");
					}
				}});
			add(button);
			ctop+=computer.resolution.datapath.toolComponent.getButtonHeightAndSpace();

			line1=ctop;
			ctop+=5;
			label=new JLabel("Place a new:");
			label.setFont(new Font("Dialog",Font.BOLD,computer.resolution.datapath.toolComponent.getFontSize()));
			label.setBounds(5,ctop,cwidth,computer.resolution.datapath.toolComponent.getButtonHeight());
			add(label);
			ctop+=computer.resolution.datapath.toolComponent.getButtonHeightAndSpace();
			
			int tooltipnumber=0;
			String[] tooltips=new String[]{"A set of wires that connect the output of one block to the input of another (also the B key)","merge several buses together to form a larger bus.  buses on the left form the high order bits of the output bus","extract a subset of wires from a bus to form a smaller bus","a storage unit that saves a value on each clock cycle.  registers can be enabled/disabled with the control unit, or with a one-bit enabler bus connecting to the side","a one-bit register","a table of registers.  an address bus connected to the side selects a register from the table","connect to physical memory. an address input bus connects to the side, data to the top and bottom","connect to the simulated I/O ports.  port is selected with an address bus connected to the side, data buses connects to the top and bottom.","routes one or more input buses to a single output bus.  selection can be done with a side-connected bus or from the control unit.  input buses are numbered from 0 starting at the left.","has one input bus of b width, and 2^b output buses.  one of the output buses is chosen based on the value at the input and set to 1.","2-input unit that can do various arithmetic operations. the operation is selected by the control unit.","transfer the value from a smaller bus to a larger bus, preserving the sign","source a constant hexadecimal value.  the name of the block is the value sourced","a simple ROM that holds a truth table.  the output is selected from a side address input.","sources a value chosen by the user at runtime","displays a value at runtime","connect a bus to the control unit.  the bus's value can be used to make conditional control statements","load a previously designed datapath/control into the datapath as a single block","draw a text label on the datapath"};
			for (String s:new String[]{"bus","joiner","splitter","register","flag","register file","memory","ports","multiplexor","decoder","ALU","extender","constant","lookup table","input pin","output pin","control","module","label"})
			{
				button=new JButton(s);
				button.setFont(new Font("Dialog",Font.PLAIN,computer.resolution.datapath.toolComponent.getFontSize()));
				button.setBounds(5,ctop,cwidth,computer.resolution.datapath.toolComponent.getButtonHeight());
				button.setToolTipText(tooltips[tooltipnumber++]);
				final String s2=s;
				button.addActionListener(new ActionListener(){
					public void actionPerformed(ActionEvent arg0) {
						action="place";
						component=s2;
						if (s2.equals("module"))
						{
							JFileChooser fc = new JFileChooser();
							fc.setDialogTitle("Choose a module file");
							fc.setCurrentDirectory(new File("."));
							fc.showOpenDialog(null);
							File f=fc.getSelectedFile();
							if (f==null) 
								{
								action="";
								return;
								}
		
							setStatusLabel("Click to place the module");
							drawingcomponent.tempblock=new ModuleBlock(component,defaultModule,f.getAbsolutePath(),f.getName());
						}
						else if (!s2.equals("bus"))
						{
							setStatusLabel("Click to place a new "+component);
							drawingcomponent.tempblock=new Block(component,defaultbits,defaultModule);
						}
						else
							setStatusLabel("Press the mouse on a block and drag to draw a bus");
						drawingcomponent.requestFocus();
					}});
				add(button);
				ctop+=computer.resolution.datapath.toolComponent.getButtonHeightAndSpace();				
			}

			line2=ctop;
			ctop+=5;
			label=new JLabel("Combinational:");
			label.setFont(new Font("Dialog",Font.BOLD,computer.resolution.datapath.toolComponent.getFontSize()));
			label.setBounds(5,ctop,cwidth,computer.resolution.datapath.toolComponent.getButtonHeight());
			add(label);
			ctop+=computer.resolution.datapath.toolComponent.getButtonHeightAndSpace();
			for (String s:new String[]{"adder","negate","increment","decrement","and","or","nand","nor","not","xor","equal-to","less-than","shift-left","shift-right"})
			{
				button=new JButton(s);
				button.setFont(new Font("Dialog",Font.PLAIN,computer.resolution.datapath.toolComponent.getFontSize()));
				button.setBounds(5,ctop,cwidth,computer.resolution.datapath.toolComponent.getButtonHeight());
				final String s2="combinational-"+s;
				button.addActionListener(new ActionListener(){
					public void actionPerformed(ActionEvent arg0) {
						action="place";
						component=s2;
						setStatusLabel("Click to place a new "+component.substring(14));
						drawingcomponent.tempblock=new Block(component,defaultbits,defaultModule);
						drawingcomponent.requestFocus();
					}});
				add(button);
				ctop+=computer.resolution.datapath.toolComponent.getButtonHeightAndSpace();				
			}
			height = ctop;
		}
		public void paintComponent(Graphics g)
		{
			g.setColor(Color.WHITE);
			g.fillRect(0,0,width,height);
			g.setColor(Color.BLACK);
			g.drawLine(width,0,width,height);
			g.drawLine(0,line1,width,line1);
			g.drawLine(0,line2,width,line2);
		}
	}
	
	public class DrawingComponent extends JComponent
	{
		Block tempblock=null;
		Bus tempbus1=null,tempbus2=null;
		JScrollPane scroll;
		public DrawingComponent()
		{
			super();
			resetLeft();
			scroll=new JScrollPane(this);
			//scroll.getVerticalScrollBar().setPreferredSize(new Dimension(44, 0));
			restoreSize();
			scroll.getHorizontalScrollBar().setValue(dpwidth/2);
			scroll.getVerticalScrollBar().setValue(dpheight/2);
			scroll.getHorizontalScrollBar().addAdjustmentListener(new AdjustmentListener(){
				public void adjustmentValueChanged(AdjustmentEvent arg0) {
					if (arg0.getValue()==scroll.getHorizontalScrollBar().getMaximum()-scroll.getHorizontalScrollBar().getVisibleAmount())
					{
						int place=scroll.getHorizontalScrollBar().getValue();
						dpwidth+=1000;
						repaint();
						drawingcomponent.scroll.revalidate();
						scroll.getHorizontalScrollBar().setValue(place);
					}
				}});
			scroll.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener(){
				public void adjustmentValueChanged(AdjustmentEvent arg0) {
					if (arg0.getValue()==scroll.getVerticalScrollBar().getMaximum()-scroll.getVerticalScrollBar().getVisibleAmount())
					{
						int place=scroll.getVerticalScrollBar().getValue();
						dpheight+=1000;
						repaint();
						drawingcomponent.scroll.revalidate();
						scroll.getVerticalScrollBar().setValue(place);
					}
				}});
		}
		public void restoreSize() {
			scroll.setBounds(left,0,frameX-left,frameY-computer.resolution.datapath.getStatusBarThickness());			
		}
		int left;
		public void setLeft(int left) {
			//this.left = left;
			this.left = toolscroll.getWidth();
			if (modificationScroll != null) 
				if (modificationScroll.isVisible())
					this.left += modificationScroll.getWidth();
		}
		public void resetLeft() {
			//this.left = toolscroll.getWidth();
			setLeft(toolscroll.getWidth());
		}
		public Dimension getPreferredSize()
		{
			return new Dimension(dpwidth,dpheight);
		}
		public void paintComponent(Graphics g)
		{
			g.setColor(Color.WHITE);
			g.fillRect(0,0,dpwidth,dpheight);
			paintAllBlocks(g);
		}
		private void paintAllBlocks(Graphics g)
		{
			try{
			for (Block b:defaultModule.blocks)
				b.draw(g);
			for (Bus b:defaultModule.buses)
				b.draw(g);
			if (action.equals("place") && tempblock!=null)
				tempblock.draw(g);
			if (action.equals("place") && tempbus1!=null)
				tempbus1.draw(g);
			if (action.equals("place") && tempbus2!=null)
				tempbus2.draw(g);
			}catch(ConcurrentModificationException e){}
		}
	}
	
	public void closeGUI() 
	{
		computer.datapathBuilder=null;
		if (computer.controlBuilder!=null) computer.controlBuilder.close();
	}

	//asks the user to select a point on the datapath
	public void getpoint(String s)
	{
		setStatusLabel(s);
		action="getpoint";
		computer.datapathBuilder.toFront();
	}
	
	//opens a dialog box and asks user to select an xml file
	public void doload()
	{
		JFileChooser fc = new JFileChooser();
		fc.setCurrentDirectory(new File("."));
		fc.showOpenDialog(null);
		File f=fc.getSelectedFile();
		if (f==null) return;
		doload(f.getAbsolutePath(),defaultModule);
	}

	//load a datapath file into the given module
	public void doload(String name,DatapathModule module)
	{
		if (module==defaultModule) undolog.push(dumpXML());
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
		catch(Exception x)
		{
			x.printStackTrace();
		}

		if (module==defaultModule) undolog.push(dumpXML());
//		if (module==defaultModule) clearAll();
		module.basenumber=module.blocknumber;
		
		DatapathXMLParse xmlParse=new DatapathXMLParse(xml,module);
		for (int i=1; i<=xmlParse.highestBlockNumber(); i++)
			xmlParse.constructBlock(i);
		
//		module.blocknumber+=module.basenumber;
//		module.basenumber+=10000;
//		module.blocknumber+=xmlParse.highestBlockNumber()+1;
		
		drawingcomponent.repaint();		
		drawingcomponent.requestFocus();			
	}		
		

	
	public class Bus extends Part
	{
		public int input;
		public int bits;
		public int output;
		public boolean isHorizontal;
		private DatapathModule module;
		
		public Bus(Bus b)
		{
			module=b.module;
			output=b.output; input=b.input; bits=b.bits;
			xcoor=b.xcoor; ycoor=b.ycoor; xcoor2=b.xcoor2; ycoor2=b.ycoor2;
			isHorizontal=b.isHorizontal;
			description=b.description;
			name=b.name;
			type=b.type;
		}
		public Bus(Block inputblock, DatapathModule module)
		{
			this.module=module;
			output=0;
			input=inputblock.number;
			bits=inputblock.bits;
			type="bus";
		}
		public Bus(Bus inputblock, DatapathModule module)
		{
			this.module=module;
			output=0;
			input=inputblock.number;
			bits=inputblock.bits;
			type="bus";
		}
		public Bus(int bits, DatapathModule module)
		{
			this.module=module;
			output=0;
			input=0;
			this.bits=bits;
			type="bus";
		}
		public long getValue()
		{
			return value&((long)Math.pow(2,bits)-1l);
		}
		public void setValue(long val)
		{
			this.value=val&((long)Math.pow(2,bits)-1l);
		}
		public Part[] getInputBlocks()
		{
			Part[] blist=new Part[1];
			blist[0]=module.getPart(input);
			return blist;
		}
		public String getErrorString()
		{
			for (ErrorEntry e:errorlog)
				if (e.number==number)
					return "bus "+e.number+": "+e.error;
			return "";
		}
		public void doPropagate()
		{
			if (input==0)
				error("no input to bus");

			for (Block b:module.blocks)
			{
				if (input==b.number && !b.type.equals("splitter") && !b.type.equals("module") && !b.type.equals("decoder"))
				{
					if (b.getValue()!=value)
						highlighted=true;
					setValue(b.getValue());
				}
			}
			for (Bus b:module.buses)
			{
				if (input==b.number)
				{
					if (b.getValue()!=value)
						highlighted=true;
					setValue(b.getValue());
				}
			}
		}
		private void error(String message)
		{
			System.out.println("Error in bus "+number+": "+message);
//			System.exit(0);
		}
		public int getXExit(int x, int y)
		{
			if (!doSelect(x,y))
				return -1;
			if (ycoor==ycoor2)
				return x;
			if (xcoor==xcoor2)
				return xcoor;
			return -1;
		}
		public int getYExit(int x, int y)
		{
			if (!doSelect(x,y))
				return -1;
			if (ycoor==ycoor2)
				return ycoor;
			if (xcoor==xcoor2)
				return y;
			return -1;
		}
		public boolean doSelect(int x, int y)
		{
			int precision=3;
			if (xcoor==xcoor2 && (x-xcoor)>=-precision && (x-xcoor)<=precision && ((y>=ycoor && y<=ycoor2)||(y>=ycoor2 && y<=ycoor))) return true;
			if (ycoor==ycoor2 && (y-ycoor)>=-precision && (y-ycoor)<=precision && ((x>=xcoor && x<=xcoor2)||(x>=xcoor2 && x<=xcoor))) return true;
			return false;
		}
		public int getXEntrance(int x, int y)
		{
			if (!doSelect(x,y))
				return -1;
			if (ycoor==ycoor2)
				return x;
			if (xcoor==xcoor2)
				return xcoor;
			return -1;
		}
		public int getYEntrance(int x, int y)
		{
			if (!doSelect(x,y))
				return -1;
			if (ycoor==ycoor2)
				return ycoor;
			if (xcoor==xcoor2)
				return y;
			return -1;
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
		private void drawLine(Graphics g, int a, int b, int c, int d)
		{
			g.drawLine((int)(a*scaling),(int)(b*scaling),(int)(c*scaling),(int)(d*scaling));
		}
		private void drawString(Graphics g, String s, int a, int b)
		{
			g.drawString(s,(int)(a*scaling),(int)(b*scaling));
		}
		public void draw(Graphics g)
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
			if (!isHorizontal)
			{
				drawLine(g,xcoor-3,(ycoor+ycoor2)/2+3,xcoor+3,(ycoor+ycoor2)/2-3);
				drawString(g,""+bits,xcoor+1,(ycoor+ycoor2)/2+3);
				drawString(g,getErrorString(),xcoor+1,(ycoor+ycoor2)/2+9);
			}
			else
			{
				drawLine(g,(xcoor+xcoor2)/2-3,ycoor+3,(xcoor+xcoor2)/2+3,ycoor-3);
				drawString(g,""+bits,(xcoor+xcoor2)/2+3,ycoor-1);
				drawString(g,getErrorString(),(xcoor+xcoor2)/2+6,ycoor-1);
			}
		}		
		public void place(int x1, int y1, int x2, int y2, int entb, int exb)
		{
			xcoor=x1;
			ycoor=y1;
			xcoor2=x2;
			ycoor2=y2;
			input=entb;
			output=exb;
			if (xcoor==xcoor2)
				isHorizontal=false;
			else
				isHorizontal=true;
		}
		public String getXML()
		{
			String xml = "<bus>\n<number>"+number+"</number>\n<name>"+name+"</name>\n<bits>"+bits+"</bits>\n<xcoordinate>"+xcoor+"</xcoordinate>\n<ycoordinate>"+ycoor+"</ycoordinate>\n";
				xml+="<xcoordinate2>"+xcoor2+"</xcoordinate2>\n<ycoordinate2>"+ycoor2+"</ycoordinate2>\n"+"<description>"+description+"</description>\n";
				xml+="<entry>"+input+"</entry>\n";
				xml+="<exit>"+output+"</exit>\n";
			xml+="</bus>\n";
			return xml;
		}
		public void unselect()
		{
			selected=false;
			if (modificationcomponent!=null)
				modificationcomponent.dispose();
		}
		
		public void edit()
		{
			modificationcomponent=new ModificationComponent(-1,number);	
		}
		
		public void select()
		{
			selected=true;
		}
		public boolean verify()
		{
			//all buses have a valid input and output
			if (input==0 || (module.getBlock(input)==null && module.getBus(input)==null))
			{
				errorlog.add(new ErrorEntry(number,"nobody is sourcing this bus"));
				return false;
			}
			if (output!=0 && module.getBlock(output)==null && module.getBus(output)==null) 
			{
				errorlog.add(new ErrorEntry(number,"the bus claims to be sourcing component "+output+", but "+output+" doesn't exist"));
				return false;
			}
			if (input==output)
			{
				errorlog.add(new ErrorEntry(number,"infinite loop"));
				return false;
			}
			if (output==0)
			{
			for (Bus b:module.buses)
				{
					if (b.input==number)
						return true;
				}
				errorlog.add(new ErrorEntry(number,"the bus doesn't source any component"));
				return false;
			}
			return true;
		}
		public void fix()
		{
			if (input!=0 && module.getBlock(input)!=null && !module.getBlock(input).type.equals("splitter")&& !module.getBlock(input).type.equals("decoder"))
				bits=module.getBlock(input).bits;
			if (input!=0 && module.getBus(input)!=null)
				bits=module.getBus(input).bits;
		}
	}
	
	public class ModuleBlock extends Block
	{
		String absolutePath, filename, datapathname, controlname;
		ArrayList<Pin> inputpin;
		ArrayList<Pin> outputpin;
		CustomProcessor.CustomProcessorModule customProcessorModule;
		public ModuleBlock(ModuleBlock b)
		{
			super(b);
			absolutePath=b.absolutePath;
			filename=b.filename;
			datapathname=b.datapathname;
			controlname=b.controlname;
			inputpin=new ArrayList<Pin>();
			for (Pin pin:b.inputpin)
				inputpin.add(new Pin(pin));
			for (Pin pin:b.outputpin)
				outputpin.add(new Pin(pin));
			if(b.customProcessorModule!=null)
				instantiate();
		}
		public ModuleBlock(String type, DatapathModule module, String absolutePath, String filename)
		{
			super(type,0,module);
			this.absolutePath=absolutePath;
			this.filename=filename;
			inputpin=new ArrayList();
			outputpin=new ArrayList();
			
			String xml="";
			try
			{
				Scanner mscan=new Scanner(new FileReader(absolutePath));
				while (mscan.hasNextLine())
					xml+=mscan.nextLine()+"\n";
			}
			catch(IOException e)
			{
				e.printStackTrace();
				clearAction();
				return;
			}
			catch(Exception x)
			{
				x.printStackTrace();
				clearAction();
				return;				
			}
			DatapathXMLParse xmlparse=new DatapathXMLParse(xml,null);
			datapathname=xmlparse.xmlParts[xmlparse.find("<datapath>",0)+1];
			controlname=xmlparse.xmlParts[xmlparse.find("<control>",0)+1];
			
			int ptr=0;
			while(true)
			{
				if ((ptr=xmlparse.find("<input pin>", ptr))==-1)
					break;
				ptr++;
				inputpin.add(new Pin(xmlparse.xmlParts[xmlparse.find("<name>", ptr)+1],Integer.parseInt(xmlparse.xmlParts[xmlparse.find("<number>", ptr)+1]),Integer.parseInt(xmlparse.xmlParts[xmlparse.find("<bits>", ptr)+1]),true));
			}
			ptr=0;
			while(true)
			{
				if ((ptr=xmlparse.find("<output pin>", ptr))==-1)
					break;
				ptr++;
				outputpin.add(new Pin(xmlparse.xmlParts[xmlparse.find("<name>", ptr)+1],Integer.parseInt(xmlparse.xmlParts[xmlparse.find("<number>", ptr)+1]),Integer.parseInt(xmlparse.xmlParts[xmlparse.find("<bits>", ptr)+1]),false));
			}
		}
		
		public String getXML()
		{
			String xml = "<module>\n<number>"+number+"</number>\n<name>"+name+"</name>\n<xcoordinate>"+xcoor+"</xcoordinate>\n<ycoordinate>"+ycoor+"</ycoordinate>\n";
				xml+="<xcoordinate2>"+xcoor2+"</xcoordinate2>\n<ycoordinate2>"+ycoor2+"</ycoordinate2>\n"+"<description>"+description+"</description>\n";
				xml+="<absolutepath>"+absolutePath+"</absolutepath>\n<filename>"+filename+"</filename>\n";
				for (int i=0; i<inputpin.size(); i++)
				{
					xml+="<inputpin>"+inputpin.get(i).bus+"</inputpin>\n";
				}
				for (int i=0; i<outputpin.size(); i++)
				{
					xml+="<outputpin>"+outputpin.get(i).bus+"</outputpin>\n";
				}
				xml+="</module>\n";
			return xml;
		}		
		
		private class Pin
		{
			String name;
			int numberInModule;
			int bits;
			boolean isInput;
			int bus;
			long value;
			public Pin(String name, int number, int bits, boolean isInput)
			{
				this.name=name; this.numberInModule=number; this.bits=bits; this.isInput=isInput;
				bus=-1;
			}
			public Pin(Pin p)
			{
				name=p.name; numberInModule=p.numberInModule; bits=p.bits;
				isInput=p.isInput; bus=p.bus;
			}
		}
		static final int PINSPACE=24;
		//determines the image size of the component
		public void place(int x, int y)
		{
			xcoor=x;
			ycoor=y;
			xcoor2=x+2+PINSPACE*Math.max(inputpin.size(), outputpin.size());
			ycoor2=y+YSIZE;
		}
		public void draw(Graphics g)
		{
			//draw the block's image
			g.setColor(new Color(255,228,181));
			fillRect(g,xcoor,ycoor+4,xcoor2-xcoor,ycoor2-ycoor-8);
			setSelectedColor(g);
			drawRect(g,xcoor,ycoor+4,xcoor2-xcoor,ycoor2-ycoor-8);
			for (int i=0; i<inputpin.size(); i++)
				drawLine(g,xcoor+PINSPACE*i,ycoor,xcoor+PINSPACE*i,ycoor+4);
			for (int i=0; i<outputpin.size(); i++)
				drawLine(g,xcoor+PINSPACE*i,ycoor2,xcoor+PINSPACE*i,ycoor2-4);
			
			g.setColor(Color.BLACK);
			g.setFont(new Font("Dialog",Font.PLAIN,(int)(8*scaling)));
			if (!name.equals(""))
			{
				drawString(g,name,xcoor+5,ycoor2-12);
			}
			g.setFont(new Font("Dialog",Font.PLAIN,(int)(4*scaling)));
			for (int i=0; i<inputpin.size(); i++)
				drawString(g,inputpin.get(i).name+": "+inputpin.get(i).bits,xcoor+PINSPACE*(i)+2,ycoor+9);
			for (int i=0; i<outputpin.size(); i++)
				drawString(g,outputpin.get(i).name+": "+outputpin.get(i).bits,xcoor+PINSPACE*(i)+2,ycoor2-5);

			g.setFont(new Font("Dialog",Font.PLAIN,(int)(8*scaling)));
			if (computer.customProcessor!=null)
			{
				for (int i=0; i<inputpin.size(); i++)
				{
					g.setColor(new Color(50,150,50));
					fillRect(g,xcoor+i*PINSPACE+2,ycoor-9,(inputpin.get(i).bits/4+1)*7,12);
					g.setColor(Color.WHITE);
					drawString(g,Long.toHexString(inputpin.get(i).value),xcoor+i*PINSPACE+2,ycoor+3);
				}
				for (int i=0; i<outputpin.size(); i++)
				{
					g.setColor(new Color(50,150,50));
					fillRect(g,xcoor+i*PINSPACE+2,ycoor2,(outputpin.get(i).bits/4+1)*7,12);
					g.setColor(Color.WHITE);
					drawString(g,Long.toHexString(outputpin.get(i).value),xcoor+i*PINSPACE+2,ycoor2+12);
				}
			}
			g.setColor(Color.RED);
			drawString(g,getErrorString(),xcoor+3,ycoor-1);				
		}
		public int getBits(int x)
		{
			return outputpin.get((x-xcoor)/PINSPACE).bits;
		}
		public int getXEntrance(int x, int y)
		{
			if (!doSelect(x,y))
				return -1;
			if ((x-xcoor)%PINSPACE<PINSPACE/3 && (x-xcoor)/PINSPACE<inputpin.size() && (x-xcoor)/PINSPACE>=0)
				return x;
			return -1;
		}
		public int getXExit(int x, int y)
		{
			if (!doSelect(x,y))
				return -1;
			if ((x-xcoor)%PINSPACE<PINSPACE/3 && (x-xcoor)/PINSPACE<outputpin.size() && (x-xcoor)/PINSPACE>=0)
				return x;
			return -1;
		}
		public int getYEntrance(int x, int y)
		{
			if (!doSelect(x,y))
				return -1;
			return ycoor;
		}
		public int getYExit(int x, int y)
		{
			if (!doSelect(x,y))
				return -1;
			return ycoor2;
		}
		public void instantiate() 
		{
			customProcessorModule=new CustomProcessor.CustomProcessorModule(computer, datapathname, controlname);
			modules.add(customProcessorModule);
			customProcessorModule.active=true;
		}
		public void addInputBus(int number, int xcoor) 
		{
			inputpin.get((xcoor-this.xcoor)/PINSPACE).bus=number;			
		}
		public void addOutputBus(int number, int xcoor) 
		{
			outputpin.get((xcoor-this.xcoor)/PINSPACE).bus=number;			
		}
		public void deleteInputBus(int xcoor) 
		{
			inputpin.get((xcoor-this.xcoor)/PINSPACE).bus=-1;			
		}
		public void deleteOutputBus(int xcoor) 
		{
			outputpin.get((xcoor-this.xcoor)/PINSPACE).bus=-1;			
		}
		public boolean verify()
		{
			for (Pin pin:inputpin)
			{
				if(pin.bus==-1)
					{
					errorlog.add(new ErrorEntry(number,"all input pins must have buses"));
					return false;
					}
				if(pin.bits!=defaultModule.getBus(pin.bus).bits)
					{
					errorlog.add(new ErrorEntry(number,"input bus doesn't have the same number of bits as the pin it is sourcing"));
					return false;
					}
			}
			try
			{
				new FileReader(datapathname);
				new FileReader(controlname);
			}
			catch(IOException e)
			{
				e.printStackTrace();
				errorlog.add(new ErrorEntry(number,"couldn't open the module xml files"));
				return false;
			}
			catch(Exception e)
			{
				e.printStackTrace();
				errorlog.add(new ErrorEntry(number,"couldn't open the module xml files"));
				return false;
			}
			return true;
		}
		public void doPropagate()
		{
			for (Pin pin:inputpin)
			{
				if (pin.bus!=-1)
				{
					pin.value=module.getBus(pin.bus).getValue();
					customProcessorModule.datapath.getBlock(pin.numberInModule).setValue(pin.value);
				}
			}
			customProcessorModule.datapath.propagateAll();
			for (Pin pin:outputpin)
			{
				if (pin.bus!=-1)
				{
					pin.value=customProcessorModule.datapath.getBlock(pin.numberInModule).getValue();
					module.getBus(pin.bus).setValue(pin.value);
				}
			}
		}
	}
	
	//Block models all datapath components except Buses
	public class Block extends Part
	{
		public static final int MAX_BUSES_PER_BLOCK=32;

		public static final int XSIZE=40,YSIZE=30;

		public int bits;

		private Hashtable<Integer,Long> regfilevalue=new Hashtable<Integer,Long>();
		public Hashtable<Integer,String> bus = new Hashtable<Integer,String>();

		//will this be clocked on the next cycle?
		public boolean clockSetting=false;
		public String operationSetting="";
		
		protected DatapathModule module;
		
		//photocopy the block
		public Block(Block b)
		{
			module=b.module;
			xcoor=b.xcoor; ycoor=b.ycoor; xcoor2=b.xcoor2; ycoor2=b.ycoor2;
			bits=b.bits;
			name=b.name;
			type=b.type;
			description=b.description;
			if (b.regfilevalue!=null)
				regfilevalue=(Hashtable<Integer, Long>) b.regfilevalue.clone();
			if (b.bus!=null)
				bus=(Hashtable<Integer, String>) b.bus.clone();
		}
		
		public Block(String type, int bits, DatapathModule module)
		{
			this.module=module;
			
			xcoor=-1;
			ycoor=-1;

			this.type=type;

			//memory and ports get their own name by default
			if (type.equals("memory"))
			{
				name="memory";
			}
			else if (type.equals("ports"))
			{
				name="ports";
			}
			
			//some units can only source 1 bit
			if (type.equals("flag")||type.equals("combinational-less-than")||type.equals("combinational-equal-to"))
				bits=1;
			this.bits=bits;
		}
		
		public void unselect()
		{
			selected=false;
			if (modificationcomponent!=null)
				modificationcomponent.dispose();
		}
		
		public int getBits(int xcoor)
		{
			if (type.equals("decoder"))
				return 1;
			return bits;
		}
		
		public void edit()
		{
			modificationcomponent=new ModificationComponent(number,-1);	
		}
		
		public void select()
		{
			selected=true;
		}
		
		//return an array of all buses providing index inputs (horizontal) to the block
		public Bus[] getAddressInputBlock()
		{
			ArrayList<Bus> addressbus=new ArrayList<Bus>();
			for (Bus b:module.buses)
			{
				if (b.output==number && b.isHorizontal)
				{
					addressbus.add(b);
				}
			}
			return addressbus.toArray(new Bus[0]);
		}
		//return an array of all buses providing data inputs (vertical)
		public Bus[] getDataInputBlock()
		{
			ArrayList<Bus> databus=new ArrayList<Bus>();
			for (Bus b:module.buses)
			{
				if (b.output==number && !b.isHorizontal)
				{
					databus.add(b);
				}
			}
			return databus.toArray(new Bus[0]);
		}
		//return an array of all output buses
		public Bus[] getDataOutputBlock()
		{
			ArrayList<Bus> databus=new ArrayList<Bus>();
			for (Bus b:module.buses)
			{
				if (b.input==number)
				{
					databus.add(b);
				}
			}
			return databus.toArray(new Bus[0]);
		}

		//return an array of all input buses (basically address+data)
		public Part[] getInputBlocks()
		{
			ArrayList<Part> blist=new ArrayList<Part>();
			for (Bus b:module.buses)
				if (b.output==number && b.ycoor2==ycoor)
					blist.add(b);
			return blist.toArray(new Part[0]);
		}

		//if another bus is connected to a joiner input, increase the joiner's bits 
		public void updateJoinerBits()
		{
			bits=0;
			Bus[] b=getDataInputBlock();
			for (int i=0; i<b.length; i++)
				bits+=b[i].bits;
			drawingcomponent.repaint();
		}
		
		//called to set the data value of the component
		//regfiles, memory, and ports must be indexed; lookup tables can't be changed
		public void setValue(long val)
		{
			if (type.equals("register file"))
			{
				if (getAddressInputBlock().length==0 || getAddressInputBlock().length>=2) 
				{
					error("only one address input for reg file");
					return;
				}
				int addr=(int)getAddressInputBlock()[0].getValue();
				this.regfilevalue.put(new Integer(addr),new Long(val&((long)Math.pow(2,bits)-1l)));
			}
			else if (type.equals("memory"))
			{
				if (getAddressInputBlock().length==0 || getAddressInputBlock().length>=2) 
				{
					error("only one address input for memory");
					return;
				}
				int addr=(int)getAddressInputBlock()[0].getValue();
				for (int i=0; i<bits; i+=8)
					computer.physicalMemory.setByte(addr+i,(byte)((val>>>i)&0xff));
			}
			else if (type.equals("ports"))
			{
				if (getAddressInputBlock().length==0 || getAddressInputBlock().length>=2) 
				{
					error("only one address input for port");
					return;
				}
				int addr=(int)getAddressInputBlock()[0].getValue();
				for (int i=0; i<bits; i+=8)
					computer.ioports.ioPortWriteByte(addr+i,(byte)((val>>>i)&0xff));
			}
			else if (type.equals("lookup table")){}
			else
				this.value=val&((long)Math.pow(2,bits)-1l);
		}
		//called to set a value of an indexed component manually
		public void setValue(int addr, long val)
		{
			if (type.equals("register file"))
			{
				this.regfilevalue.put(new Integer(addr),new Long(val&((long)Math.pow(2,bits)-1l)));
			}
			else if (type.equals("memory"))
			{
				for (int i=0; i<bits; i+=8)
					computer.physicalMemory.setByte(addr+i,(byte)((val>>>i)&0xff));
			}
			else if (type.equals("ports"))
			{
				for (int i=0; i<bits; i+=8)
					computer.ioports.ioPortWriteByte(addr+i,(byte)((val>>>i)&0xff));
			}
			else if (type.equals("lookup table"))
			{
				this.regfilevalue.put(new Integer(addr),new Long(val&((long)Math.pow(2,bits)-1l)));
			}
			else
				this.value=val&((long)Math.pow(2,bits)-1l);			
		}
		public long getValue(int addr)
		{
			if (type.equals("register file")||type.equals("lookup table"))
			{
				if (regfilevalue.get(new Integer(addr))==null) return 0;
				return ((Long)regfilevalue.get(new Integer(addr))).longValue()&((long)Math.pow(2,bits)-1l);
			}			
			else if (type.equals("memory"))
			{
				long val=0;
				for (int i=0; i<bits; i+=8)
					val+=(long)computer.physicalMemory.getByte(addr+i)<<(long)i;
				return val&((long)Math.pow(2,bits)-1l);
			}
			else if (type.equals("ports"))
			{
				long val=0;
				for (int i=0; i<bits; i+=8)
					val+=(long)computer.ioports.ioPortReadByte(addr+i)<<(long)i;
				return val&((long)Math.pow(2,bits)-1l);				
			}
			else
				return 0;
		}
		public long getValue()
		{
			if (type.equals("register file")||type.equals("lookup table"))
			{
				if (getAddressInputBlock().length==0 || getAddressInputBlock().length>=2) 
				{
					error("only one address input for reg file");
					return 0;
				}
				int addr=(int)getAddressInputBlock()[0].getValue();
				if (regfilevalue.get(new Integer(addr))==null) return 0;
				return ((Long)regfilevalue.get(new Integer(addr))).longValue()&((long)Math.pow(2,bits)-1l);
			}
			else if (type.equals("memory"))
			{
				if (getAddressInputBlock().length==0 || getAddressInputBlock().length>=2) 
				{
					error("only one address input for memory");
					return 0;
				}
				int addr=(int)getAddressInputBlock()[0].getValue();
				long val=0;
				for (int i=0; i<bits; i+=8)
					val+=(long)computer.physicalMemory.getByte(addr+i)<<(long)i;
				return val&((long)Math.pow(2,bits)-1l);
			}
			else if (type.equals("ports"))
			{
				if (getAddressInputBlock().length==0 || getAddressInputBlock().length>=2) 
				{
					error("only one address input for ports");
					return 0;
				}
				int addr=(int)getAddressInputBlock()[0].getValue();
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
		//called by the processor unit on each clock cycle
		public void doClock()
		{
			if (!clockSetting) return;
			Bus[] input=getDataInputBlock();
			if (input.length==0 || input.length>=2)
			{
				error("clocking register without a single input bus");
				setValue(0);
				return;
			}
			if (type.equals("register")||type.equals("flag"))
			{
				highlighted=true;
				setValue(input[0].getValue());
			}
			else if (type.equals("register file") || type.equals("ports") || type.equals("memory"))
			{
				highlighted=true;
				setValue(input[0].getValue());
			}
		}
		//go through each combinational unit and route data through it
		public void doPropagate()
		{
			if (type.equals("adder")||type.equals("combinational-adder"))
			{
				Bus[] bs=getDataInputBlock();
				if (bs.length<1)
				{
					error("adder needs at least one input");
					return;
				}
				long v=0;
				for (int i=0; i<bs.length; i++)
					v+=bs[i].getValue();
				setValue(v&0xffffffffffffffffl);					
			}
			else  if (type.equals("combinational-and"))
			{
				Bus[] bs=getDataInputBlock();
				if (bs.length<1)
				{
					error("and needs at least one input");
					return;
				}
				long v=bs[0].getValue();
				for (int i=1; i<bs.length; i++)
					v&=bs[i].getValue();
				setValue(v);					
			}
			else  if (type.equals("combinational-or"))
			{
				Bus[] bs=getDataInputBlock();
				if (bs.length<1)
				{
					error("or needs at least one input");
					return;
				}
				long v=bs[0].getValue();
				for (int i=1; i<bs.length; i++)
					v|=bs[i].getValue();
				setValue(v);					
			}
			else  if (type.equals("combinational-nor"))
			{
				Bus[] bs=getDataInputBlock();
				if (bs.length<1)
				{
					error("nor needs at least one input");
					return;
				}
				long v=bs[0].getValue();
				for (int i=1; i<bs.length; i++)
					v|=bs[i].getValue();
				setValue(~v);					
			}
			else  if (type.equals("combinational-nand"))
			{
				Bus[] bs=getDataInputBlock();
				if (bs.length<1)
				{
					error("and needs at least one input");
					return;
				}
				long v=bs[0].getValue();
				for (int i=1; i<bs.length; i++)
					v&=bs[i].getValue();
				setValue(~v);					
			}
			else  if (type.equals("combinational-xor"))
			{
				Bus[] bs=getDataInputBlock();
				if (bs.length<1)
				{
					error("xor needs at least one input");
					return;
				}
				long v=bs[0].getValue();
				for (int i=1; i<bs.length; i++)
					v^=bs[i].getValue();
				setValue(v);					
			}
			else  if (type.equals("combinational-not"))
			{
				Bus[] bs=getDataInputBlock();
				if (bs.length!=1)
				{
					error("not needs one input");
					return;
				}
				long v=bs[0].getValue();
				setValue(~v);					
			}
			else if (type.equals("combinational-negate"))
			{
				Bus[] bs=getDataInputBlock();
				if (bs.length!=1)
				{
					error("negate needs one input");
					return;
				}
				long v=bs[0].getValue();
				setValue(-v);					
			}
			else  if (type.equals("combinational-increment"))
			{
				Bus[] bs=getDataInputBlock();
				if (bs.length!=1)
				{
					error("increment needs one input");
					return;
				}
				long v=bs[0].getValue();
				setValue((v+1)&0xffffffffffffffffl);					
			}
			else  if (type.equals("combinational-decrement"))
			{
				Bus[] bs=getDataInputBlock();
				if (bs.length!=1)
				{
					error("decrement needs one input");
					return;
				}
				long v=bs[0].getValue();
				setValue((v-1)&0xffffffffffffffffl);					
			}
			else  if (type.equals("combinational-less-than"))
			{
				Bus[] bs=getDataInputBlock();
				if (bs.length!=2)
				{
					error("less-than needs two inputs");
					return;
				}
				setValue(bs[0].getValue()<bs[1].getValue()?1l:0);					
			}
			else  if (type.equals("combinational-equal-to"))
			{
				Bus[] bs=getDataInputBlock();
				if (bs.length!=2)
				{
					error("equal-to needs two inputs");
					return;
				}
				setValue(bs[0].getValue()==bs[1].getValue()?1l:0);					
			}
			else  if (type.equals("combinational-shift-right"))
			{
				Bus[] bs=getDataInputBlock();
				if (bs.length!=1 && bs.length!=2)
				{
					error("shift needs one or two inputs");
					return;
				}
				if (bs.length==1)
					setValue(bs[0].getValue()>>>1);
				else
					setValue(bs[0].getValue()>>>bs[1].getValue());
			}
			else  if (type.equals("combinational-shift-left"))
			{
				Bus[] bs=getDataInputBlock();
				if (bs.length!=1 && bs.length!=2)
				{
					error("shift needs one or two inputs");
					return;
				}
				if (bs.length==1)
					setValue(bs[0].getValue()<<1);
				else
					setValue(bs[0].getValue()<<bs[1].getValue());
			}
			else if (type.equals("ALU"))
			{
				Bus[] bs=getDataInputBlock();
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
			else if (type.equals("multiplexor")&&getAddressInputBlock().length==0)
			{
				Bus[] bs=getDataInputBlock();
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
							Bus tmpblock=bs[j];
							bs[j]=bs[j+1];
							bs[j+1]=tmpblock;
						}
					}
				}
				if (operationSetting.equals(""))
					setValue(bs[0].getValue());
				else
					setValue(bs[Integer.parseInt(operationSetting,16)].getValue());
			}
			else if (type.equals("data_multiplexor") || (type.equals("multiplexor")&&getAddressInputBlock().length!=0))
			{
				if (getAddressInputBlock().length==0)
				{
					error("data mux needs an address input");
					return;
				}
				int addr=(int)getAddressInputBlock()[0].getValue();
//				System.out.println("propagate "+type+" "+number+" "+addr);
				Bus[] bs=getDataInputBlock();
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
							Bus tmpblock=bs[j];
							bs[j]=bs[j+1];
							bs[j+1]=tmpblock;
						}
					}
				}
				if (addr>=bs.length) return;
				setValue(bs[addr].getValue());
			}
			else if (type.equals("control") || type.equals("extender"))
			{
				Bus[] input=getDataInputBlock();
				if (input.length==0) 
				{
					error("control/extender needs an input");
					return;
				}
				setValue(input[0].getValue());
			}
			else if (type.equals("output pin"))
			{
				Bus[] input=getDataInputBlock();
				if (input.length==0) 
				{
					error("output pin needs an input");
					return;
				}
				setValue(input[0].getValue());
			}
			else if (type.equals("decoder"))
			{
				setValue(((Bus)(getInputBlocks()[0])).getValue());
				for (int i=0; i<getDataOutputBlock().length; i++)
				{
					int buspin=(int)Math.pow(2,bits)-(getDataOutputBlock()[i].xcoor-xcoor)/gridsize-1;
					if (buspin==getValue())
						getDataOutputBlock()[i].setValue(1);
					else
						getDataOutputBlock()[i].setValue(0);
				}
			}
			else if (type.equals("splitter"))
			{
				if (getInputBlocks().length==0) 
				{
					error("splitter needs an input");
					return;
				}
				setValue(((Bus)(getInputBlocks()[0])).getValue());
				for (Enumeration e=bus.keys(); e.hasMoreElements();)
				{
					int i=((Integer)e.nextElement()).intValue();

					for (Bus b:module.buses)
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
					}
				}
			}
			else if (type.equals("joiner"))
			{
				Bus[] input=getDataInputBlock();
				if (input.length<1)
				{
					error("joiner needs an input");
					return;
				}
				for (int i=0; i<input.length; i++)
				{
					for (int j=0; j<input.length-1; j++)
					{
						if (input[j].xcoor>input[j+1].xcoor)
						{
							Bus tmpblock=input[j];
							input[j]=input[j+1];
							input[j+1]=tmpblock;
						}
					}
				}
				long v=0;
				v=input[0].getValue();
				for (int i=1; i<input.length; i++)
				{
					v=v<<input[i].bits;
					v=v|input[i].getValue();
				}
				setValue(v);
			}
			else if (type.equals("lookup table"))
			{
				value=getValue();
			}
			else if (type.equals("register file")||type.equals("memory")||type.equals("ports")||type.equals("constant"))
			{
				value=getValue();
			}
			else if (type.equals("register")||type.equals("flag"))
			{
				if (getAddressInputBlock().length>0)
					clockSetting=getAddressInputBlock()[0].getValue()!=0;
			}
		}

		private void error(String message)
		{
			System.out.println("Error in block "+number+": "+message);
//			System.exit(0);
		}

		//place the component on the graphical layout
		public void place(int x, int y, int x2, int y2)
		{
			if (x2==0 && y2==0)
				place(x,y);
			else
			{
				xcoor=x; ycoor=y; xcoor2=x2; ycoor2=y2;
			}
		}
		//determines the image size of the component
		public void place(int x, int y)
		{
			xcoor=x;
			ycoor=y;
			if (type.equals("register")||type.equals("lookup table")||type.equals("ALU")||type.equals("adder")||type.equals("register file")||type.equals("memory")||type.equals("ports"))
			{
				xcoor2=x+XSIZE;
				ycoor2=y+YSIZE;
			}
			else if (type.equals("decoder"))
			{
				xcoor2=x+XSIZE;
				ycoor2=y+YSIZE/2;
				if (Math.pow(2, bits)*gridsize>XSIZE)
					xcoor2=x+(int)Math.pow(2, bits)*gridsize;
			}
			else if (type.equals("flag")||type.equals("constant"))
			{
				xcoor2=x+XSIZE/2;
				ycoor2=y+YSIZE/2;
			}
			else if (type.equals("splitter")||type.equals("joiner"))
			{
				xcoor2=x+XSIZE;
				ycoor2=y+YSIZE/3;
			}
			else if (type.equals("input pin")||type.equals("output pin"))
			{
				xcoor2=x+XSIZE/3;
				ycoor2=y+YSIZE/3;
			}
			else if (type.equals("combinational-not")||type.equals("combinational-negate"))
			{
				xcoor2=x+XSIZE/2;
				ycoor2=y+YSIZE/2;				
			}
			else
			{
				xcoor2=x+XSIZE;
				ycoor2=y+YSIZE/2;
			}
		}
		public void delete()
		{
			module.blocks.remove(this);
		}
		public String getErrorString()
		{
			for (ErrorEntry e:errorlog)
				if (e.number==number)
					return "block "+e.number+": "+e.error;
			return "";
		}
		//does the block appear at (x,y)?
		public boolean doSelect(int x, int y)
		{
			if (x>=xcoor && y>=ycoor && x<=xcoor2 && y<=ycoor2) return true;
			return false;
		}
		//if the user clicked at x,y, does that connect to an exit line for the block?
		//if so, return the precise x coordinate for the block's exit
		public int getXExit(int x, int y)
		{
			if (!doSelect(x,y))
				return -1;
			if (type.equals("register")||type.equals("memory")||type.equals("ports")||type.equals("register file")||type.equals("lookup table")||type.equals("ALU")||type.equals("")||type.equals("data_multiplexor")||type.equals("multiplexor")||type.equals("joiner")||type.equals("inhibitor")||type.equals("extender")||type.equals("lookup table"))
				return (xcoor+xcoor2)/2;
			if (type.length()>=14 && type.substring(0,14).equals("combinational-"))
				return (xcoor+xcoor2)/2;
			if (type.equals("flag")||type.equals("constant"))
				return (xcoor+xcoor2)/2;
			if (type.equals("decoder")||type.equals("splitter"))
				return x;
			if (type.equals("input pin"))
				return (xcoor+xcoor2)/2;
			return -1;
		}
		//if the user clicked at x,y, does that connect to an exit line for the block?
		//if so, return the precise y coordinate for the block's exit
		//in almost all cases, this is simply the bottom of the block
		public int getYExit(int x, int y)
		{
			if (!doSelect(x,y))
				return -1;
			return ycoor2;
		}
		//if the user clicked at x,y, does that connect to an input line for the block?
		//if so, return the precise x coordinate for the block's input
		//some components have both address and data inputs which appear in different places
		public int getXEntrance(int x, int y)
		{
			if (!doSelect(x,y))
				return -1;
			if (type.equals("lookup table")||type.equals("extender")||type.equals("decoder")||type.equals("inhibitor")||type.equals("splitter")||type.equals("joiner")||type.equals("control"))
				return x;
			if ((type.equals("ALU")||type.equals("adder"))&&x>xcoor&&x<xcoor+(xcoor2-xcoor)/2-(xcoor2-xcoor)/5)
				return x;
			if ((type.equals("ALU")||type.equals("adder"))&&x<xcoor2&&x>xcoor+(xcoor2-xcoor)/2+(xcoor2-xcoor)/5)
				return x;
			if ((type.equals("register")||type.equals("flag")||type.equals("register file")||type.equals("memory")||type.equals("ports")||type.equals("multiplexor")||type.equals("data_multiplexor"))&&y<ycoor+(ycoor2-ycoor)/4)
				return x;
			if ((type.equals("register")||type.equals("flag")||type.equals("register file")||type.equals("memory")||type.equals("ports")||type.equals("multiplexor")||type.equals("data_multiplexor")||type.equals("lookup table"))&&y>=ycoor+(ycoor2-ycoor)/3&&x<xcoor+(xcoor2-xcoor)/3)
				return xcoor;
			if ((type.equals("register")||type.equals("flag")||type.equals("register file")||type.equals("memory")||type.equals("ports")||type.equals("multiplexor")||type.equals("data_multiplexor")||type.equals("lookup table"))&&y>=ycoor+(ycoor2-ycoor)/3&&x>xcoor+(xcoor+xcoor2)/2-(xcoor2-xcoor)/3)
				return xcoor2;
			if (type.equals("output pin"))
				return x;
			if (((type.length()>=14 && type.substring(0,14).equals("combinational-"))))
				return x;
			return -1;
		}
		//if the user clicked at x,y, does that connect to an input line for the block?
		//if so, return the precise y coordinate for the block's input
		//some components have both address and data inputs which appear in different places
		public int getYEntrance(int x, int y)
		{
			if (!doSelect(x,y))
				return -1;
			if (type.equals("extender")||type.equals("decoder")||type.equals("inhibitor")||type.equals("splitter")||type.equals("joiner")||type.equals("ALU")||type.equals("adder")||type.equals("control"))
				return ycoor;
			if (((type.length()>=14 && type.substring(0,14).equals("combinational-"))))
				return ycoor;
			if ((type.equals("register")||type.equals("flag")||type.equals("register file")||type.equals("memory")||type.equals("ports")||type.equals("multiplexor")||type.equals("data_multiplexor"))&&y<=ycoor+(ycoor2-ycoor)/3)
				return ycoor;
			if ((type.equals("register")||type.equals("flag")||type.equals("register file")||type.equals("memory")||type.equals("ports")||type.equals("multiplexor")||type.equals("data_multiplexor")||type.equals("lookup table"))&&y>=ycoor+(ycoor2-ycoor)/3)
				return y;
			if (type.equals("output pin"))
				return ycoor;
			return -1;
		}

		//the block's edge is either BLACK (normal), RED (selected), or orangeish (highlighted)
		protected void setSelectedColor(Graphics g)
		{
			if (selected)
				g.setColor(Color.RED);
			else if (highlighted)
				g.setColor(new Color(255,50,0));
			else
				g.setColor(Color.BLACK);
		}
		//various methods for drawing components
		//all of these are scaled so that you can zoom in or out
		protected void drawRect(Graphics g, int a, int b, int c, int d)
		{
			g.drawRect((int)(a*scaling+xshift),(int)(b*scaling+yshift),(int)(c*scaling),(int)(d*scaling));
		}
		protected void fillRect(Graphics g, int a, int b, int c, int d)
		{
			g.fillRect((int)(a*scaling+xshift),(int)(b*scaling+yshift),(int)(c*scaling),(int)(d*scaling));
		}
		protected void drawLine(Graphics g, int a, int b, int c, int d)
		{
			g.drawLine((int)(a*scaling+xshift),(int)(b*scaling+yshift),(int)(c*scaling+xshift),(int)(d*scaling+yshift));
		}
		private void drawOval(Graphics g, int a, int b, int c, int d)
		{
			g.drawOval((int)(a*scaling+xshift),(int)(b*scaling+yshift),(int)(c*scaling),(int)(d*scaling));
		}
		private void fillOval(Graphics g, int a, int b, int c, int d)
		{
			g.fillOval((int)(a*scaling+xshift),(int)(b*scaling+yshift),(int)(c*scaling),(int)(d*scaling));
		}
		private void drawArc(Graphics g, int a, int b, int c, int d, int e, int f)
		{
			g.drawArc((int)(a*scaling+xshift),(int)(b*scaling+yshift),(int)(c*scaling),(int)(d*scaling),e,f);
		}
		protected void drawString(Graphics g, String s, int a, int b)
		{
			g.drawString(s,(int)(a*scaling+xshift),(int)(b*scaling+yshift));
		}
		public void draw(Graphics g)
		{
			//draw the block's image
			if (type.equals("register"))
			{
				g.setColor(new Color(200,200,255));
				fillRect(g,xcoor,ycoor,xcoor2-xcoor,ycoor2-ycoor);
				setSelectedColor(g);
				drawRect(g,xcoor,ycoor,xcoor2-xcoor,ycoor2-ycoor);
			}
			else if (type.equals("label"))
			{
				g.setColor(new Color(189,183,107));
				fillRect(g,xcoor,ycoor,xcoor2-xcoor,ycoor2-ycoor);
				setSelectedColor(g);
				drawRect(g,xcoor,ycoor,xcoor2-xcoor,ycoor2-ycoor);
				g.setColor(Color.WHITE);
				drawString(g,name,xcoor+5,ycoor2-3);
			}
			else if (type.equals("flag"))
			{
				setSelectedColor(g);
				drawRect(g,xcoor,ycoor,(xcoor2-xcoor),(ycoor2-ycoor));
				//flag is up
				if (getValue()!=0)
				{
					//staff
					g.setColor(Color.BLACK);
					drawLine(g,xcoor+(xcoor2-xcoor)/3,ycoor+2,xcoor+(xcoor2-xcoor)/3,ycoor2-2);
					g.setColor(Color.RED);
					drawLine(g,xcoor+(xcoor2-xcoor)/3,ycoor+2,xcoor2-2,(ycoor+ycoor2)/2);
					drawLine(g,xcoor+(xcoor2-xcoor)/3,(ycoor+ycoor2)/2,xcoor2-2,(ycoor+ycoor2)/2);
				}
				//flag is down
				else
				{
					g.setColor(Color.BLACK);
					drawLine(g,xcoor+2,ycoor+(ycoor2-ycoor)/3,xcoor2-2,ycoor+(ycoor2-ycoor)/3);
					g.setColor(Color.RED);
					drawLine(g,xcoor2-2,ycoor+(ycoor2-ycoor)/3,(xcoor+xcoor2)/2,ycoor2-2);
					drawLine(g,(xcoor+xcoor2)/2,ycoor+(ycoor2-ycoor)/3,(xcoor+xcoor2)/2,ycoor2-2);
				}
			}
			else if (type.equals("lookup table"))
			{
				setSelectedColor(g);
				drawRect(g,xcoor,ycoor,(xcoor2-xcoor),(ycoor2-ycoor));
				drawLine(g,xcoor+5,ycoor+(ycoor2-ycoor)/4,xcoor+(xcoor2-xcoor)-5,ycoor+(ycoor2-ycoor)/4);
				drawLine(g,xcoor+(xcoor2-xcoor)/2,ycoor+(ycoor2-ycoor)/4,xcoor+(xcoor2-xcoor)/2,ycoor+(ycoor2-ycoor)-(ycoor2-ycoor)/4);
			}
			else if (type.equals("ALU")||type.equals("adder"))
			{
				setSelectedColor(g);
				int x=xcoor+(xcoor2-xcoor)/2,y=ycoor+(ycoor2-ycoor)/2;
				drawLine(g,x-(xcoor2-xcoor)/2,y-(ycoor2-ycoor)/2,x-(xcoor2-xcoor)/2+(xcoor2-xcoor)/5,y+(ycoor2-ycoor)/2);	//left
				drawLine(g,x+(xcoor2-xcoor)/2,y-(ycoor2-ycoor)/2,x+(xcoor2-xcoor)/2-(xcoor2-xcoor)/5,y+(ycoor2-ycoor)/2);	//right
				drawLine(g,x-(xcoor2-xcoor)/2+(xcoor2-xcoor)/5,y+(ycoor2-ycoor)/2,x+(xcoor2-xcoor)/2-(xcoor2-xcoor)/5,y+(ycoor2-ycoor)/2);	//bottom
				drawLine(g,x-(xcoor2-xcoor)/8,y-(ycoor2-ycoor)/2,x,y-(ycoor2-ycoor)/3);		//left notch
				drawLine(g,x+(xcoor2-xcoor)/8,y-(ycoor2-ycoor)/2,x,y-(ycoor2-ycoor)/3);		//right notch
				drawLine(g,x-(xcoor2-xcoor)/2,y-(ycoor2-ycoor)/2,x-(xcoor2-xcoor)/8,y-(ycoor2-ycoor)/2);	//left top
				drawLine(g,x+(xcoor2-xcoor)/2,y-(ycoor2-ycoor)/2,x+(xcoor2-xcoor)/8,y-(ycoor2-ycoor)/2);	//right top
			}
			else if (type.equals("memory")||type.equals("ports")||type.equals("register file"))
			{
				if (type.equals("register file"))
					g.setColor(new Color(200,200,255));
				else if (type.equals("memory"))
					g.setColor(new Color(255,200,255));
				else if (type.equals("ports"))
					g.setColor(new Color(200,255,220));
				fillRect(g,xcoor,ycoor,(xcoor2-xcoor),(ycoor2-ycoor));
				g.setColor(Color.BLACK);
				drawLine(g,xcoor,ycoor+(ycoor2-ycoor)/3,xcoor+(xcoor2-xcoor),ycoor+(ycoor2-ycoor)/3);
				drawLine(g,xcoor,ycoor+2*(ycoor2-ycoor)/3,xcoor+(xcoor2-xcoor),ycoor+2*(ycoor2-ycoor)/3);
				setSelectedColor(g);
				drawRect(g,xcoor,ycoor,(xcoor2-xcoor),(ycoor2-ycoor));
			}
			else if (type.equals("extender")||type.equals("inhibitor"))
			{
				setSelectedColor(g);
				drawOval(g,xcoor,ycoor,(xcoor2-xcoor),(ycoor2-ycoor));
			}
			else if (type.equals("joiner"))
			{
				setSelectedColor(g);
				drawLine(g,xcoor,ycoor,(xcoor+xcoor2)/2,ycoor2);
				drawLine(g,xcoor,ycoor,xcoor2,ycoor);
				drawLine(g,xcoor2,ycoor,(xcoor+xcoor2)/2,ycoor2);
			}
			else if (type.equals("splitter"))
			{
				setSelectedColor(g);
				drawLine(g,xcoor,ycoor2,(xcoor+xcoor2)/2,ycoor);
				drawLine(g,xcoor,ycoor2,xcoor2,ycoor2);
				drawLine(g,xcoor2,ycoor2,(xcoor+xcoor2)/2,ycoor);
			}
			else if (type.equals("control"))
			{
				g.setColor(new Color(150,0,0));
				fillRect(g,xcoor,ycoor,(xcoor2-xcoor),(ycoor2-ycoor));
				setSelectedColor(g);
				drawRect(g,xcoor,ycoor,(xcoor2-xcoor),(ycoor2-ycoor));
			}
			else if (type.equals("multiplexor")||type.equals("data_multiplexor"))
			{
				setSelectedColor(g);
				drawLine(g,xcoor,ycoor,xcoor+(xcoor2-xcoor),ycoor);
				drawLine(g,xcoor+(xcoor2-xcoor)/5,ycoor+(ycoor2-ycoor),xcoor+(xcoor2-xcoor)-(xcoor2-xcoor)/5,ycoor+(ycoor2-ycoor));
				drawLine(g,xcoor,ycoor,xcoor+(xcoor2-xcoor)/5,ycoor+(ycoor2-ycoor));
				drawLine(g,xcoor+(xcoor2-xcoor),ycoor,xcoor+(xcoor2-xcoor)-(xcoor2-xcoor)/5,ycoor+(ycoor2-ycoor));
			}
			else if (type.equals("decoder"))
			{
				setSelectedColor(g);
				drawLine(g,xcoor,ycoor+(ycoor2-4-ycoor),xcoor+(xcoor2-xcoor),ycoor+(ycoor2-4-ycoor));
				drawLine(g,xcoor+(xcoor2-xcoor)/5,ycoor,xcoor+(xcoor2-xcoor)-(xcoor2-xcoor)/5,ycoor);
				drawLine(g,xcoor,ycoor+(ycoor2-4-ycoor),xcoor+(xcoor2-xcoor)/5,ycoor);
				drawLine(g,xcoor+(xcoor2-xcoor),ycoor+(ycoor2-4-ycoor),xcoor+(xcoor2-xcoor)-(xcoor2-xcoor)/5,ycoor);
				for (int i=0; i<Math.pow(2, bits); i++)
					drawLine(g,xcoor+i*gridsize,ycoor2,xcoor+i*gridsize,ycoor2-4);
			}
			else if (type.equals("constant"))
			{
				setSelectedColor(g);
				drawOval(g,xcoor,ycoor,(xcoor2-xcoor),(ycoor2-ycoor));
			}
			else if (type.equals("input pin"))
			{
				if (getValue()==0)
					g.setColor(Color.RED);
				else
					g.setColor(Color.GREEN);
				fillOval(g,xcoor,ycoor,xcoor2-xcoor,2*(ycoor2-ycoor)/3);
				setSelectedColor(g);
				drawLine(g,(xcoor+xcoor2)/2,ycoor2-(ycoor2-ycoor)/3,(xcoor+xcoor2)/2,ycoor2);
				drawOval(g,xcoor,ycoor,xcoor2-xcoor,2*(ycoor2-ycoor)/3);
			}
			else if (type.equals("output pin"))
			{
				if (getValue()==0)
					g.setColor(Color.RED);
				else
					g.setColor(Color.GREEN);
				fillOval(g,xcoor,ycoor+(ycoor2-ycoor)/3,xcoor2-xcoor,2*(ycoor2-ycoor)/3);
				setSelectedColor(g);
				drawLine(g,xcoor,ycoor,xcoor2,ycoor);
				drawLine(g,(xcoor+xcoor2)/2,ycoor+(ycoor2-ycoor)/3,(xcoor+xcoor2)/2,ycoor);
				drawOval(g,xcoor,ycoor+(ycoor2-ycoor)/3,xcoor2-xcoor,2*(ycoor2-ycoor)/3);
			}
			else if (type.equals("combinational-and"))
			{
				setSelectedColor(g);
				drawArc(g,xcoor,ycoor-(ycoor2-ycoor),(xcoor2-xcoor),(ycoor2-ycoor)*2,0,-180);
				drawLine(g,xcoor,ycoor,xcoor2,ycoor);
			}
			else if (type.equals("combinational-nand"))
			{
				setSelectedColor(g);
				drawArc(g,xcoor,ycoor-((ycoor2-4)-ycoor),(xcoor2-xcoor),((ycoor2-4)-ycoor)*2,0,-180);
				drawLine(g,xcoor,ycoor,xcoor2,ycoor);
				drawOval(g,xcoor+(xcoor2-xcoor)/2-2,ycoor2-4,4,4);
			}
			else if (type.equals("combinational-nor"))
			{
				setSelectedColor(g);
				drawArc(g,xcoor,ycoor-((ycoor2-4)-ycoor),(xcoor2-xcoor),((ycoor2-4)-ycoor)*2,0,-180);
				drawArc(g,xcoor,ycoor-2,(xcoor2-xcoor),4,0,-180);
				drawOval(g,xcoor+(xcoor2-xcoor)/2-2,ycoor2-4,4,4);
			}
			else if (type.equals("combinational-not"))
			{
				setSelectedColor(g);				
				drawOval(g,xcoor+(xcoor2-xcoor)/2-2,ycoor2-4,4,4);
				drawLine(g,xcoor,ycoor,xcoor2,ycoor);
				drawLine(g,xcoor,ycoor,(xcoor+xcoor2)/2,ycoor2-4);
				drawLine(g,(xcoor+xcoor2)/2,ycoor2-4,xcoor2,ycoor);
			}
			else if (type.equals("combinational-or"))
			{
				setSelectedColor(g);
				drawArc(g,xcoor,ycoor-(ycoor2-ycoor),(xcoor2-xcoor),(ycoor2-ycoor)*2,0,-180);
				drawArc(g,xcoor,ycoor-2,(xcoor2-xcoor),4,0,-180);
			}
			else if (type.equals("combinational-xor"))
			{
				setSelectedColor(g);
				drawArc(g,xcoor,ycoor-(ycoor2-ycoor-2),(xcoor2-xcoor),(ycoor2-ycoor-2)*2,0,-180);
				drawArc(g,xcoor,ycoor-2+2,(xcoor2-xcoor),4,0,-180);
				drawArc(g,xcoor,ycoor-2,(xcoor2-xcoor),4,0,-180);
			}
			else if (((type.length()>=14 && type.substring(0,14).equals("combinational-"))))
			{
				setSelectedColor(g);
				drawRect(g,xcoor,ycoor,(xcoor2-xcoor),(ycoor2-ycoor));
				String subtype=type.substring(14);
			}
			g.setColor(Color.BLACK);
			g.setFont(new Font("Dialog",Font.PLAIN,(int)(8*scaling)));
			if (!name.equals(""))
			{
				if (type.equals("constant"))
					drawString(g,name,xcoor+5,ycoor+10);
				else if (!type.equals("label"))
//					drawString(g,name,xcoor+5,ycoor+16);
					drawString(g,name,xcoor+5,ycoor2-4);
			}
			//label the number of bits
			g.setColor(new Color(0,150,0));
			if (!type.equals("bus")&&!type.equals("extender")&&!type.equals("joiner")&&!type.equals("constant")&&!type.equals("label"))
				drawString(g,""+bits,xcoor+1,ycoor-1);
			else if (type.equals("extender")||type.equals("joiner"))
				drawString(g,""+bits,xcoor+1,ycoor+(ycoor2-ycoor)+12);
			else if (type.equals("constant"))
				drawString(g,""+bits,xcoor+1,ycoor+(ycoor2-ycoor)+12);
			//each splitter output gets its own bit label
			if (type.equals("splitter"))
			{
				for (Enumeration e=bus.keys(); e.hasMoreElements();)
				{
					int i=((Integer)e.nextElement()).intValue();
					for (Bus b:module.buses)
					{
						if (b.number==i)
							drawString(g,(String)bus.get(new Integer(i)),b.xcoor,ycoor+(ycoor2-ycoor)/3+12);
					}
				}
			}
			//label the current value
			if (computer.customProcessor!=null)
			{
				if (type.equals("register")||type.equals("register file")||type.equals("memory")||type.equals("ports")||type.equals("lookup table"))
				{
					g.setColor(new Color(0,150,0));
					fillRect(g,xcoor-7,ycoor+YSIZE+12-9,(bits/4+1)*7,12);
					g.setColor(Color.WHITE);
					drawString(g,Long.toHexString(value),xcoor-7,ycoor+YSIZE+12);

					if (getAddressInputBlock().length>0)
					{
						g.setColor(new Color(100,100,0));
						fillRect(g,xcoor-(bits/4+1)*7-2,ycoor+YSIZE/2-12,(bits/4+1)*7,12);
						g.setColor(Color.WHITE);
						drawString(g,Long.toHexString(getAddressInputBlock()[0].getValue()),xcoor-(bits/4+1)*7,ycoor+YSIZE/2-2);
					}
				}
				else if (type.equals("adder")||type.equals("ALU"))
				{
					g.setColor(new Color(50,150,50));
					fillRect(g,xcoor-7,ycoor+YSIZE+12-9,(bits/4+1)*7,12);
					g.setColor(Color.WHITE);
					drawString(g,Long.toHexString(value),xcoor-7,ycoor+YSIZE+12);
				}
				else if ((type.length()>=14 && type.substring(0,14).equals("combinational-")))
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

					if (type.equals("data_multiplexor")||getAddressInputBlock().length!=0)
					{
						g.setColor(new Color(100,100,0));
						fillRect(g,xcoor-(bits/4+1)*7-2,ycoor+YSIZE/2-12,(bits/4+1)*7,12);
						g.setColor(Color.WHITE);
						drawString(g,Long.toHexString(getAddressInputBlock()[0].getValue()),xcoor-(bits/4+1)*7,ycoor+YSIZE/2-2);
					}
				}
				else if (type.equals("constant")||type.equals("flag")||type.equals("input pin")||type.equals("output pin"))
				{
					g.setColor(new Color(0,150,0));
					fillRect(g,xcoor-7,ycoor+YSIZE/2+12-9,(bits/4+1)*7,12);
					g.setColor(Color.WHITE);
					drawString(g,Long.toHexString(value),xcoor-7,ycoor+YSIZE/2+12);
					if (getAddressInputBlock().length>0)
					{
						g.setColor(new Color(100,100,0));
						fillRect(g,xcoor-(bits/4+1)*7-2,ycoor+YSIZE/2-12,(bits/4+1)*7,12);
						g.setColor(Color.WHITE);
						drawString(g,Long.toHexString(getAddressInputBlock()[0].getValue()),xcoor-(bits/4+1)*7,ycoor+YSIZE/2-2);
					}
				}
				else if (type.equals("bus") && xcoor==xcoor2)
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
				}
			}
			g.setColor(Color.RED);
			drawString(g,getErrorString(),xcoor+3,ycoor-1);				
		}
		public String getXML()
		{
			String xml = "<"+type+">\n<number>"+number+"</number>\n<name>"+name+"</name>\n<bits>"+bits+"</bits>\n<xcoordinate>"+xcoor+"</xcoordinate>\n<ycoordinate>"+ycoor+"</ycoordinate>\n";
				xml+="<xcoordinate2>"+xcoor2+"</xcoordinate2>\n<ycoordinate2>"+ycoor2+"</ycoordinate2>\n<description>"+description+"</description>\n";
			if (type.equals("register"))
			{
				if(getAddressInputBlock().length==1)
				{
					xml+="<enable>"+getAddressInputBlock()[0].number+"</enable>\n";
				}
			}
			if (type.equals("memory")||type.equals("register file")||type.equals("ports")||type.equals("lookup table")||type.equals("multiplexor")||type.equals("data_multiplexor"))
			{
				if(getAddressInputBlock().length==1)
				{
					xml+="<address>"+getAddressInputBlock()[0].number+"</address>\n";
				}				
			}
			if (type.equals("splitter"))
			{
				for (Enumeration e=bus.keys(); e.hasMoreElements();)
				{
					int i=((Integer)e.nextElement()).intValue();
					xml+="<line "+i+">"+(String)bus.get(new Integer(i))+"</line>\n";
				}
			}
			if (type.equals("lookup table"))
			{
				for (Enumeration e=regfilevalue.keys(); e.hasMoreElements();)
				{
					Integer a=(Integer)e.nextElement();
					Long v=regfilevalue.get(a);
					xml+="<value "+a.intValue()+">"+Long.toHexString(v.longValue())+"</value>\n";
				}
			}
			xml+="</"+type+">\n";
			return xml;
		}

		public String controlInputs()
		{
			if ((type.equals("register")||type.equals("flag"))&&getAddressInputBlock().length==0)
				return "1 clock "+name;
			else if (type.equals("register file")||type.equals("ports")||type.equals("memory"))
				return "1 clock "+name;
			else if (type.equals("ALU"))
				return "1 alu "+name;
			else if (type.equals("multiplexor")&&getAddressInputBlock().length==0)
			{
				int i=0;
				for (Bus b:module.buses)
				{
					if (b.output==number)
						i++;
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
			//one input bus, input bus has same width
			if (type.equals("register")||type.equals("flag"))
			{
				if (getDataInputBlock().length!=1)
				{
					errorlog.add(new ErrorEntry(number,"must have one and only one input"));
					return false;
				}
				if (getDataInputBlock()[0].bits!=bits)
				{
					errorlog.add(new ErrorEntry(number,"input bus must have the same number of bits"));
					return false;
				}
				if (getAddressInputBlock().length>1)
				{
					errorlog.add(new ErrorEntry(number,"can't have multiple clock enable inputs"));
					return false;
				}
				if (getAddressInputBlock().length==1 && getAddressInputBlock()[0].bits!=1) 
				{
					errorlog.add(new ErrorEntry(number,"clock enable input can only have 1 bit"));
					return false;
				}
				if (name.equals("")) 
					{
					errorlog.add(new ErrorEntry(number,"needs a name"));
					return false;
					}
				return true;
			}
			//one input bus, same width, one address bus
			else if (type.equals("register file"))
			{
				if (getDataInputBlock().length!=1)
				{
					errorlog.add(new ErrorEntry(number,"must have one and only one data input"));
					return false;
				}
				if (getDataInputBlock()[0].bits!=bits)
				{
					errorlog.add(new ErrorEntry(number,"input bus must have the same number of bits"));
					return false;
				}
				if (name.equals("")) 
				{
					errorlog.add(new ErrorEntry(number,"needs a name"));
					return false;
				}
				if (getAddressInputBlock().length!=1) 
				{
					errorlog.add(new ErrorEntry(number,"needs an address input"));
					return false;
				}
				return true;
			}
			//zero/one input bus, input bus has same width, one address bus
			else if (type.equals("memory")||type.equals("ports")||type.equals("lookup table"))
			{
				if (getDataInputBlock().length>1)
				{
					errorlog.add(new ErrorEntry(number,"can't have more than one data input"));
					return false;
				}
				if (getDataInputBlock().length>0 && getDataInputBlock()[0].bits!=bits)
				{
					errorlog.add(new ErrorEntry(number,"input bus must have the same number of bits"));
					return false;
				}
				if (getAddressInputBlock().length!=1) 
				{
					errorlog.add(new ErrorEntry(number,"needs an address input"));
					return false;
				}
				return true;
			}
			//one/two input bus, input bus has same width, one output bus, bus has same width
			else if (type.equals("ALU"))
			{
				if (getDataInputBlock().length<1 || getDataInputBlock().length>2 || getDataInputBlock()[0].bits!=bits || (getDataInputBlock().length>1 && getDataInputBlock()[1].bits!=bits))
				{
					errorlog.add(new ErrorEntry(number,"needs one or two input buses with the same number of bits as the ALU"));
					return false;
				}
				if (name.equals("")) 
				{
					errorlog.add(new ErrorEntry(number,"needs a name"));
					return false;
				}
				return true;
			}
			//two input bus, same width, one output bus
			else if (type.equals("adder"))
			{
				if (getDataInputBlock().length!=2 || getDataInputBlock()[0].bits!=bits || getDataInputBlock()[1].bits!=bits) 
					{
					errorlog.add(new ErrorEntry(number,"needs two input buses with the same number of bits as the ALU"));
					return false;
					}
				return true;
			}
			//one or more input bus, same width
			else if (type.equals("combinational-adder")||type.equals("combinational-and")||type.equals("combinational-or")||type.equals("combinational-nand")||type.equals("combinational-nor")||type.equals("combinational-xor"))
			{
				if (getDataInputBlock().length<1) 
					{
					errorlog.add(new ErrorEntry(number,"needs an input bus"));
					return false;
					}
				for (int i=0; i<getDataInputBlock().length; i++) 
					if (getDataInputBlock()[i].bits!=bits)
						{
						errorlog.add(new ErrorEntry(number,"input bus must have the same number of bits"));
						return false;
						}
				return true;
			}
			//one input bus, same width
			else if (type.equals("combinational-not")||type.equals("combinational-negate")||type.equals("combinational-increment")||type.equals("combinational-decrement"))
			{
				if (getDataInputBlock().length!=1)
					{
					errorlog.add(new ErrorEntry(number,"must have one and only one input bus"));
					return false;
					}
				for (int i=0; i<getDataInputBlock().length; i++) 
					if (getDataInputBlock()[i].bits!=bits)
						{
						errorlog.add(new ErrorEntry(number,"input bus must have the same number of bits"));
						return false;
						}
				return true;
			}
			//two input buses, same width as each other, size=1
			else  if (type.equals("combinational-less-than")||type.equals("combinational-equal-to"))
			{
				if (getDataInputBlock().length!=2)
					{
					errorlog.add(new ErrorEntry(number,"must have exactly two input buses"));
					return false;
					}
				if (getDataInputBlock()[0].bits!=getDataInputBlock()[1].bits) 
					{
					errorlog.add(new ErrorEntry(number,"input buses must have the same number of bits"));
					return false;
					}
				if (bits!=1)
					{
					errorlog.add(new ErrorEntry(number,"can only have one bit"));
					return false;
					}
				return true;
			}
			//one or two input buses, same width
			else  if (type.equals("combinational-shift-right")||type.equals("combinational-shift-left"))
			{
				if (getDataInputBlock().length!=1 && getDataInputBlock().length!=2)
					{
					errorlog.add(new ErrorEntry(number,"must have one or two input buses"));
					return false;
					}
				for (int i=0; i<getDataInputBlock().length; i++) 
				{
					if (getDataInputBlock()[i].bits!=bits) 
						{
						errorlog.add(new ErrorEntry(number,"input buses must have the same width"));
						return false;
						}
				}
				return true;
			}
			//one output bus, one/more input bus, bus has valid width, no leftover bits
			else if (type.equals("joiner"))
			{
				if (getDataInputBlock().length<1) {
					errorlog.add(new ErrorEntry(number,"must have at least one input bus"));
					return false;
				}
				return true;
			}
			//one input bus, same width, one/more output bus, bus has valid width
			else if (type.equals("splitter"))
			{
				if (getDataInputBlock().length!=1 || getDataInputBlock()[0].bits!=bits) 
					{
					errorlog.add(new ErrorEntry(number,"must have one input bus with the same number of bits"));
					return false;
					}
				return true;
			}
			//one input bus, same width, one/more output bus, bus has valid width, all outputs are one bit
			else if (type.equals("decoder"))
			{
				if (getDataInputBlock().length!=1 || getDataInputBlock()[0].bits!=bits) 
					{
					errorlog.add(new ErrorEntry(number,"must have one input bus with the same number of bits"));
					return false;
					}
				for (int i=0; i<getDataOutputBlock().length; i++)
					if (getDataOutputBlock()[i].bits!=1)
					{
						errorlog.add(new ErrorEntry(number,"all output buses can only have one bit"));
						return false;
					}
				return true;
			}
			
			//one/more input bus, same width
			else if (type.equals("multiplexor"))
			{
				if (getDataInputBlock().length<1 || getDataInputBlock()[0].bits!=bits) 
					{
					errorlog.add(new ErrorEntry(number,"must have at least one input bus with the same number of bits"));
					return false;
					}
				if (name.equals("")) 
					{
					errorlog.add(new ErrorEntry(number,"needs a name"));
					return false;
					}
				return true;
			}
			//one/more input bus, same width, one address bus
			else if (type.equals("data_multiplexor"))
			{
				if (getDataInputBlock().length<1 || getDataInputBlock()[0].bits!=bits) 
					{
					errorlog.add(new ErrorEntry(number,"must have at least one data input with the same number of bits"));
					return false;
					}
				if (getAddressInputBlock()==null) 
					{
					errorlog.add(new ErrorEntry(number,"must have a selector input"));
					return false;
					}
				return true;
			}
			//one output bus, bus has valid width
			else if (type.equals("constant"))
			{
				if (getDataInputBlock().length!=0) 
					{
					errorlog.add(new ErrorEntry(number,"constants can't have inputs"));
					return false;
					}
				if (name.equals("")||name.matches("[^0-9a-fA-F]+"))
					{
					errorlog.add(new ErrorEntry(number,"name must be a valid hexadecimal value"));
					return false;
					}
				return true;
			}
			else if (type.equals("extender"))
			{
				if (getDataInputBlock().length!=1) 
				{
				errorlog.add(new ErrorEntry(number,"must have one input"));
				return false;
				}
				return true;
			}
			//one input bus, same width
			else if (type.equals("control"))
			{
				if (getDataInputBlock().length!=1 || getDataInputBlock()[0].bits!=bits) 
					{
					errorlog.add(new ErrorEntry(number,"must have one input with the same number of bits"));
					return false;
					}
				if (name.equals(""))
				{
					errorlog.add(new ErrorEntry(number,"needs a name"));
					return false;
				}
				return true;
			}
			//one input bus, same width
			else if (type.equals("output pin"))
			{
				if (getDataInputBlock().length!=1 || getDataInputBlock()[0].bits!=bits) 
					{
					errorlog.add(new ErrorEntry(number,"needs one input bus with the same number of bits"));
					return false;
					}
				return true;				
			}
			else if (type.equals("input pin"))
				return true;
			else if (type.equals("label"))
				return true;
			else
			{
				errorlog.add(new ErrorEntry(number,"isn't a recognized component"));
				return false;
			}
		}
	}
	public void undo()
	{
		if (undolog.size()==0)
			return;
		String newstate=undolog.pop();
		clearAll();
		
		int undosize=undolog.size();
		
		DatapathXMLParse xmlParse=new DatapathXMLParse(newstate,defaultModule);
		for (int i=1; i<=xmlParse.highestBlockNumber(); i++)
			xmlParse.constructBlock(i);
		
		int undosize2=undolog.size();
		for (int i=0; i<undosize2-undosize; i++)
			undolog.pop();
	}
	
	//read in a datapath xml file and break it down
	public class DatapathXMLParse
	{ 
		//list of tokens and contents
		String[] xmlParts;
		//the module into which the new blocks will be placed
		DatapathModule module;
		
		public DatapathXMLParse(String xml, DatapathModule module)
		{
			this.module=module;
			ArrayList<String> parts=new ArrayList<String>();
			int c=0;
			String tag="";

			//first break up everything by <>
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

//			for (int i=0; i<parts.size(); i++)
//				System.out.println(xmlParts[i]);
		}
		
		//find the next instance of token in the list
		public int find(String token, int starting)
		{
			for (int i=starting; i<xmlParts.length; i++)
			{
				if (xmlParts[i].equals(token))
						return i;
			}
			return -1;
		}

		//true is s only contains whitespace
		private boolean isWhiteSpace(String s)
		{
			for (int i=0; i<s.length(); i++)
			{
				if (s.charAt(i)!=' '&&s.charAt(i)!='\t'&&s.charAt(i)!='\n')
					return false;
			}
			return true;
		}

		//find where block "number" occurs in the xml, and extract all of its fields into a big array
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

		//given a token, return its contents
		public String extractField(String[] block, String field)
		{
			for (int i=0; i<block.length; i++)
			{
				if (block[i].equals(field))
					return block[i+1];
			}
			return null;
		}

		//return the number of the highest-numbered block
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

		//given a block number, construct it and place it in the module
		public void constructBlock(int number)
		{
			//first get its parts
			String[] block=extractBlock(number);
			if (block==null) return;
			
			//get the bits field
			int bits=0;
			if (extractField(block,"<bits>")!=null)
				bits=Integer.parseInt(extractField(block,"<bits>"));
			String type=block[0].substring(1,block[0].length()-1);

			//two possibilities: it's a block or a bus
			Block b=null;
			Bus bu=null;
			if (type.equals("bus"))
				bu=new Bus(bits,module);
			else if (type.equals("module"))
			{
				String absolutepath=extractField(block,"<absolutepath>");
				String filename=extractField(block,"<filename>");
				b=new ModuleBlock("module",module,absolutepath,filename);
			}
			else
				b=new Block(type,bits,module);
			//get the location coordinates, entry and exit buses
			int x=Integer.parseInt(extractField(block,"<xcoordinate>"));
			int y=Integer.parseInt(extractField(block,"<ycoordinate>"));
			int x2=0,y2=0,entry=0,exit=0;
			if (extractField(block,"<xcoordinate2>")!=null)
				x2=Integer.parseInt(extractField(block,"<xcoordinate2>"));
			if (extractField(block,"<ycoordinate2>")!=null)
				y2=Integer.parseInt(extractField(block,"<ycoordinate2>"));
			if (extractField(block,"<description>")!=null && !extractField(block,"<description>").equals("</description>"))
			{
				if (b==null)
					bu.description=extractField(block,"<description>");
				else
					b.description=extractField(block,"<description>");
			}
			if (extractField(block,"<entry>")!=null)
			{
				entry=Integer.parseInt(extractField(block,"<entry>"));
				if (entry!=0) entry+=module.basenumber;
			}
			if (extractField(block,"<exit>")!=null)
			{
				exit=Integer.parseInt(extractField(block,"<exit>"));
				if (exit!=0) exit+=module.basenumber;
			}
			//we now can create it
			if (type.equals("bus"))
			{
				bu.place(x,y,x2,y2,entry,exit);
				module.addBlock(bu);
			}
			else
			{
				b.place(x,y,x2,y2);
				module.addBlock(b);
			}
			//if it's a splitter, link on the output buses
			if (type.equals("splitter"))
			{
				for (int i=0; i<block.length; i++)
				{
					if (block[i].length()>6 && block[i].substring(0,6).equals("<line "))
					{
						int j=Integer.parseInt(block[i].substring(6,block[i].length()-1));
						if (j!=0)
							j+=module.basenumber;
						b.bus.put(new Integer(j),block[i+1]);
					}
				}
			}
			//if it's a lookup table, populate its entries
			if (type.equals("lookup table"))
			{
				for (int i=0; i<block.length; i++)
				{
					if (block[i].length()>7 && block[i].substring(0,7).equals("<value "))
					{
						int j=Integer.parseInt(block[i].substring(7,block[i].length()-1));
						b.regfilevalue.put(new Integer(j),Long.parseLong(block[i+1],16));
					}
				}				
			}
			//if it's a module, link up the input and output buses
			if (type.equals("module"))
			{
				int ip=0,op=0;
				for (int i=0; i<block.length; i++)
				{
					if (block[i].equals("<inputpin>"))
					{
						int j=Integer.parseInt(block[i+1]);
						if (j!=0) j+=module.basenumber;
						((ModuleBlock)b).inputpin.get(ip++).bus=j;
					}
					if (block[i].equals("<outputpin>"))
					{
						int j=Integer.parseInt(block[i+1]);
						if (j!=0) j+=module.basenumber;
						((ModuleBlock)b).outputpin.get(op++).bus=j;
					}
				}
			}
			//give the block a number and a name
			if (b!=null)
			{
				b.number=module.basenumber+number;
				module.blocknumber=b.number+1;
				b.name=extractField(block,"<name>");
				if (b.name.equals("</name>"))
					b.name="";
			}
			else
			{
				bu.number=module.basenumber+number;
				module.blocknumber=bu.number+1;
				bu.name=extractField(block,"<name>");
				if (bu.name.equals("</name>"))
					bu.name="";				
			}
		}
	}
	public abstract class Part
	{
		public String name="",description="";
		public int xcoor,ycoor,xcoor2,ycoor2;
		public int number;
		public boolean selected=false;
		public boolean highlighted=false;
		public long value=0;		
		public abstract Part[] getInputBlocks();
		public String type;
	}
	public void placeroute()
	{
		computer.datapathBuilder.toFront();
		setStatusLabel("Press mouse on the source component");
		action="route";
	}

	public DatapathModule loadDatapathModule(String filename)
	{
		DatapathModule dmodule=new DatapathModule();
		doload(filename, dmodule);
		return dmodule;
	}
	
	public class DatapathModule
	{
		public ArrayList<Block> blocks;
		public ArrayList<Bus> buses;	
		public int blocknumber=1;
//		private Stack<String> undolog;
		public int basenumber=0;
		
		public DatapathModule()
		{
			blocks=new ArrayList<Block>();
			buses=new ArrayList<Bus>();
			blocknumber=1;			
		}
		
		public Block createBlock(String name, int bits)
		{
			return new Block(name,bits,this);
		}
		public Bus createBus(Block b)
		{
			return new Bus(b, this);
		}
		public Bus createBus(Bus b)
		{
			return new Bus(b, this);
		}
		
		public void addBlock(Block b)
		{
			if (this==defaultModule) undolog.push(dumpXML());
			b.number=blocknumber++;
			if (b.type.length()>14 && b.type.substring(0,14).equals("combinational-"))
				b.name=b.type.substring(14)+b.number;
			else
				b.name=b.type+b.number;
			if (b.type.equals("constant"))
				b.name="0";
			if (b.type.equals("module"))
			{
				b.name=((ModuleBlock)b).filename+b.number;
				((ModuleBlock)b).instantiate();
			}
			blocks.add(b);
		}
		public void addBlock(Bus b)
		{
			if (this==defaultModule) undolog.push(dumpXML());
			b.number=blocknumber++;
			buses.add(b);
			if (b.output!=0 && getBlock(b.output)!=null && getBlock(b.output).type.equals("joiner"))
				getBlock(b.output).updateJoinerBits();
			if (b.output!=0 && getBlock(b.output)!=null && getBlock(b.output).type.equals("module"))
				((ModuleBlock)getBlock(b.output)).addInputBus(b.number,b.xcoor);
			if (b.input!=0 && getBlock(b.input)!=null && getBlock(b.input).type.equals("module"))
				((ModuleBlock)getBlock(b.input)).addOutputBus(b.number,b.xcoor);
		}
		public Block getBlock(int number)
		{
			if (number<=0) return null;
			for (Block b:blocks)
				if (b.number==number) return b;
			return null;
		}
		public Block getBlock(String name)
		{
			for (Block b:blocks)
				if (b.name.equals(name)) return b;
			return null;
		}
		public Bus getBus(int number)
		{
			if (number<=0) return null;
			for (Bus b:buses)
				if (b.number==number) return b;
			return null;
		}
		public Part getPart(int number)
		{
			if (number<=0) return null;
			for (Block b:blocks)
				if (b.number==number) return b;
			for (Bus b:buses)
				if (b.number==number) return b;
			return null;		
		}

		public void resetAll()
		{
			for (Block b:blocks)
				if (!b.type.equals("ports")&&!b.type.equals("memory"))
					b.setValue(0);
			for (Bus b:buses)
				b.setValue(0);
		}
		public void clockAll()
		{
			for (Block b:blocks)
				b.doClock();
		}
		public void resetClocks()
		{
			for (Block b:blocks)
				b.resetClock();
		}	
		public void resetHighlights()
		{
			for (Block b:blocks)
				b.highlighted=false;
			for (Bus b:buses)
				b.highlighted=false;
		}	
		public void propagateAll()
		{
			for (int i=0; i<blocks.size()+buses.size(); i++)
			{
			for (Block b:blocks)
				b.doPropagate();
			for (Bus b:buses)
				b.doPropagate();
			}
		}
		public void fixbuses()
		{
			for (int i=0; i<buses.size(); i++)
				for (Bus b:buses)
					b.fix();
		}
		//find the shortest sequence of wires and muxes leading from one block to another
		public Part[] tracePath(String inputBlockName, String outputBlockName)
		{
			Block inputBlock = getBlock(inputBlockName);
			Block outputBlock = getBlock(outputBlockName);
			return tracePath(inputBlock,outputBlock);
		}
		public Part[] tracePath(Block inputBlock, Block outputBlock)
		{
			//queue for the BFS
			ArrayList<Part> searchq=new ArrayList<Part>();
			//all blocks sourced by inputBlock
			ArrayList<Part> connectedq=new ArrayList<Part>();
			//for each sourced block, who sources it -- defines a tree
			ArrayList<Part> sourceq=new ArrayList<Part>();
			searchq.add(inputBlock);
			boolean foundit=false;
			while(!foundit)
			{
				//check if we couldn't find a connection
				if (searchq.size()==0)
					break;
				Part currentBlock=searchq.remove(0);
				for (Block b:blocks)
				{
					//if it's not a single-input stateless block, it can't be along the trace path
					if (b!=outputBlock && !b.type.equals("multiplexor")&&!b.type.equals("extender")&&!b.type.equals("splitter"))
						continue;
					//go through the block's inputs, see if currentBlock is among them
					Part[] outputsInputs=b.getInputBlocks();
					boolean ontree=false;
					for (int i=0; i<outputsInputs.length; i++)
						if (outputsInputs[i]==currentBlock)
							ontree=true;
					if (!ontree) continue;
					searchq.add(b);
					connectedq.add(b);
					sourceq.add(currentBlock);
					if (b==outputBlock)
					{
						foundit=true;
						break;
					}
				}
				for (Bus b:buses)
				{
					//go through the block's inputs, see if currentBlock is among them
					Part[] outputsInputs=b.getInputBlocks();
					boolean ontree=false;
					for (int i=0; i<outputsInputs.length; i++)
						if (outputsInputs[i]==currentBlock)
							ontree=true;
					if (!ontree) continue;
					searchq.add(b);
					connectedq.add(b);
					sourceq.add(currentBlock);
				}
			}
			//there is no path? quit
			if (!foundit)
				return null;
			//now let's construct our path
			ArrayList<Part> pathq=new ArrayList<Part>();
			//start at the end
			Part currentBlock=outputBlock;
			while(currentBlock!=inputBlock)
			{
				pathq.add(currentBlock);
				currentBlock=sourceq.get(connectedq.indexOf(currentBlock));
			}
			//add the input block on
			pathq.add(currentBlock);
			Part[] ret=new Part[pathq.size()];
			for (int i=0; i<pathq.size(); i++)
				ret[i]=pathq.get(i);
			return ret;
		}
		public String[] controlOutputs()
		{
			int i=0;
			for (Block b:blocks)
			{
				if (!b.controlOutputs().equals(""))
					i++;
			}
			String[] c=new String[i];
			i=0;
			for (Block b:blocks)
			{
				if (!b.controlOutputs().equals(""))
				{
					c[i++]=b.controlOutputs();
				}
			}
			return c;
		}

		public String[] controlInputs()
		{
			int i=0;
			for (Block b:blocks)
			{
				if (!b.controlInputs().equals(""))
					i++;
			}
			String[] c=new String[i];
			i=0;
			for (Block b:blocks)
			{
				if (!b.controlInputs().equals(""))
				{
					c[i++]=b.controlInputs();
				}
			}
			return c;
		}
		public String dumpXML()
		{
			String xml="<processor>\n\n";
			for (Block b:blocks)
			{
				xml+=b.getXML()+"\n";
			}
			for (Bus b:buses)
			{
				xml+=b.getXML()+"\n";
			}
			xml+="</processor>\n";
			return xml;
		}
	}
}


