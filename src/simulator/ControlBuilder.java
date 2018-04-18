package simulator;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import simulator.AbstractGUI.GUIComponent;
import simulator.DatapathBuilder.Block;
import simulator.DatapathBuilder.Bus;
import simulator.DatapathBuilder.DatapathModule;
import simulator.DatapathBuilder.DrawingComponent;
import simulator.DatapathBuilder.ToolComponent;

import java.io.*;
import java.util.ConcurrentModificationException;
import java.util.Scanner;
import java.util.ArrayList;

public class ControlBuilder extends AbstractGUI
{
	private static final int ROWHEIGHT=20,LABELWIDTH=100,TEXTWIDTH=50,MICROINSTRUCTIONWIDTH=200,GAPWIDTH=5;

	public ControlModule defaultControl;
	public ControlControl controlControl;
	public DrawingComponent drawingcomponent;
	public JScrollPane scroll;

	public ControlBuilder(Computer computer, DatapathBuilder.DatapathModule datapathModule)
	{
		super(computer,"Control Builder",800,800,true,false,false,false);
		computer.controlBuilder=this;
		defaultControl=new ControlModule(computer,datapathModule);

		refresh();

		defaultControl.makeSimpleControl();
	}

	public void constructGUI(GUIComponent guiComponent)
	{
		
		controlControl=new ControlControl();
		scroll=new JScrollPane(controlControl);
		scroll.setBounds(0,0,controlControl.width+20,frameY-STATUSSIZE);
		guiComponent.add(scroll);
		drawingcomponent=new DrawingComponent();
		drawingcomponent.addMouseListener(new MouseListener(){
			public void mouseClicked(MouseEvent arg0) {
				mouseClick(arg0);
			}
			public void mouseEntered(MouseEvent arg0) {}
			public void mouseExited(MouseEvent arg0) {}
			public void mousePressed(MouseEvent arg0) {}
			public void mouseReleased(MouseEvent arg0) {}});
		guiComponent.add(drawingcomponent.scroll);
	}

	public void reSize(int width, int height)
	{
		// This is another place where we may be trying to scroll a pane/window but it
		// hasn't yet been fully realized.  So check first.
		if (controlControl == null) return;

		try {
			// Change the height of the main gui container and the controlControl.
			guiComponent.setBounds(0, 0, width, height);
			controlControl.setBounds(0,0,controlControl.width+20,height-STATUSSIZE);
			scroll.setBounds(0,0,controlControl.width+20,height-STATUSSIZE);

			drawingcomponent.restoreSize();
			
		} catch(Exception e) {}

	}


	public class DrawingComponent extends JComponent
	{
		Block tempblock=null;
		Bus tempbus1=null,tempbus2=null;
		JScrollPane scroll;
		public DrawingComponent()
		{
			super();
			scroll=new JScrollPane(this);
			scroll.setBounds(controlControl.width+20,0,frameX-controlControl.width-20,frameY-STATUSSIZE);
		}
		public void restoreSize()
		{
			scroll.setBounds(controlControl.width+20,0,frameX-controlControl.width-20,frameY-STATUSSIZE);			
		}
		public Dimension getPreferredSize()
		{
			return new Dimension(width(),height());
		}
		public void paintComponent(Graphics g)
		{
			g.setColor(Color.WHITE);
			g.fillRect(0,0,width(),height());
			doPaint(g);
		}
		public void doPaint(Graphics g)
		{
			if (computer.controlBuilder==null) return;
			int rows=0;
			for (int i=0; i<defaultControl.controlPaths.size(); i++)
			{
				((ControlPath)defaultControl.controlPaths.elementAt(i)).doPaint(g,rows++);
				for (int j=0; j<((ControlPath)(defaultControl.controlPaths.elementAt(i))).controlStates.size(); j++)
				{
					((ControlState)((ControlPath)defaultControl.controlPaths.elementAt(i)).controlStates.elementAt(j)).doPaint(g,rows++);
					for (int k=0; k<((ControlState)(((ControlPath)(defaultControl.controlPaths.elementAt(i))).controlStates.elementAt(j))).controlInstructions.size(); k++)
					{
						((ControlInstruction)((ControlState)((ControlPath)defaultControl.controlPaths.elementAt(i)).controlStates.elementAt(j)).controlInstructions.elementAt(k)).doPaint(g,rows++);
					}
					for (int k=0; k<((ControlState)(((ControlPath)(defaultControl.controlPaths.elementAt(i))).controlStates.elementAt(j))).nextStates.size(); k++)
					{
						((NextState)((ControlState)((ControlPath)defaultControl.controlPaths.elementAt(i)).controlStates.elementAt(j)).nextStates.elementAt(k)).doPaint(g,rows++);
					}
				}
			}

//			if (computer.datapathBuilder!=null)
			{
				g.setColor(Color.BLACK);
				g.drawLine(defaultControl.field1width(),0,defaultControl.field1width(),height());
				g.drawLine(defaultControl.field1width()+defaultControl.field2width(),0,defaultControl.field1width()+defaultControl.field2width(),height());
			}
		}
	}
	
	public void makeSimpleControl()
	{
		defaultControl.makeSimpleControl();
	}

	public void closeGUI()
	{
		computer.controlBuilder=null;
	}
	
	public int width()
	{
		return defaultControl.width();
	}
	public int height()
	{
		return defaultControl.height();
	}

	public void mouseClick(MouseEvent e)
	{
		controlControl.unprompt();
		if (defaultControl.selectedRow==e.getY()/ROWHEIGHT)
			defaultControl.selectedRow=-1;
		else
			defaultControl.selectedRow=e.getY()/ROWHEIGHT;
		
		if (defaultControl.selectedRow>=0 && defaultControl.getSelectedVector()!=null)
			defaultControl.getSelectedVector().elementAt(defaultControl.getSelectedIndex()).doMouse();
		repaint();
	}



	public static abstract class ControlRow
	{
		public String name;
		public int type;
		public ControlModule module;
		public Computer computer;
		public ControlRow(Computer computer, String name, int type, ControlModule module)
		{
			this.computer=computer;
			this.module=module;
			this.name=name;
			this.type=type;
		}
		public abstract void doPaint(Graphics g, int row);
		public abstract String getXML();
		public abstract void parseXML(String[] elements);
		public abstract void remove();
		public abstract void rename(String s);
		public abstract void doMouse();
	}

	public static class ControlPath extends ControlRow
	{
		public Vector controlStates;
		public int row;
		private JLabel label;

		public ControlPath(Computer computer, String name, ControlModule controlModule, boolean autoGenerate)
		{
			super(computer,name,0,controlModule);
			controlStates=new Vector();
			if (autoGenerate)
				controlStates.add(new ControlState(computer,"start",controlModule, true));
			label=new JLabel("CONTROL PATH: "+name);
			label.setBounds(0,row*ROWHEIGHT+1,200,ROWHEIGHT-2);
			if (computer.controlBuilder!=null && module==computer.controlBuilder.defaultControl) computer.controlBuilder.drawingcomponent.add(label);
		}
		public void doMouse()
		{
			computer.controlBuilder.controlControl.prompt(name);
		}
		public void rename(String s)
		{
			name=s;
			label.setText("CONTROL PATH: "+name);
		}
		public void remove()
		{
			if (computer.controlBuilder!=null && module==computer.controlBuilder.defaultControl) computer.controlBuilder.drawingcomponent.remove(label);
			for (int i=0; i<controlStates.size(); i++)
				controlStates.elementAt(i).remove();
		}
		private void setComponentRow()
		{
			label.setBounds(0,row*ROWHEIGHT+1,200,ROWHEIGHT-2);
		}
		public void doPaint(Graphics g, int row)
		{
			this.row=row;
			setComponentRow();
			if (row==module.selectedRow)
				g.setColor(new Color(205,100,205));
			else if (module.highlightedRows[row])
				g.setColor(new Color(255,100,205));
			else
				g.setColor(new Color(255,150,255));
			g.fillRect(0,row*ROWHEIGHT,module.width(),ROWHEIGHT);
			g.setColor(Color.BLACK);
		}
		public String getXML()
		{
			String x="<control path>\n<pathname>"+name+"</pathname>\n";
			for (int i=0; i<controlStates.size(); i++)
				x+=((ControlState)(controlStates.elementAt(i))).getXML();
			x+="</control path>\n";
			return x;
		}
		public void parseXML(String[] elements)
		{
			if (!elements[0].equals("<control path>")) return;
			for (int i=1; i<elements.length-1; i++)
			{
				if (elements[i].equals("<pathname>"))
				{
					name=elements[i+1];
					label.setText("CONTROL PATH: "+name);
				}
				else if (elements[i].equals("<state>"))
				{
					int j;
					for (j=i; ;j++)
						if(elements[j].equals("</state>"))
							break;
					String[] m=new String[j-i+1];
					for (int k=i; k<=j; k++)
						m[k-i]=elements[k];
					ControlState n=new ControlState(computer,"",module,false);
					controlStates.add(n);
					n.parseXML(m);
				}
			}
		}
	}

	public static class ControlState extends ControlRow
	{
		public Vector controlInstructions;
		public Vector nextStates;

		public int row;
		private JLabel label;

		public ControlState(Computer computer, String name, ControlModule module, boolean autoGenerate)
		{
			super(computer,name,1,module);
			controlInstructions=new Vector();
			nextStates=new Vector();
			if (autoGenerate)
			{
				controlInstructions.add(new ControlInstruction(computer,"",module));
				nextStates.add(new NextState(computer,name,module));
			}
			label=new JLabel("STATE: "+name);
			label.setBounds(0,row*ROWHEIGHT+1,200,ROWHEIGHT-2);
			if (computer.controlBuilder!=null && module==computer.controlBuilder.defaultControl) computer.controlBuilder.drawingcomponent.add(label);
		}

		public void doMouse()
		{
			computer.controlBuilder.controlControl.prompt(name);
		}

		public void rename(String s)
		{
			name=s;
			label.setText("STATE: "+name);
		}
		public void remove()
		{
			if (computer.controlBuilder!=null && module==computer.controlBuilder.defaultControl) computer.controlBuilder.drawingcomponent.remove(label);
			for (int i=0; i<controlInstructions.size(); i++)
				controlInstructions.elementAt(i).remove();
			for (int i=0; i<nextStates.size(); i++)
				nextStates.elementAt(i).remove();
		}

		private void setComponentRow()
		{
			label.setBounds(0,row*ROWHEIGHT+1,200,ROWHEIGHT-2);
		}
		public void doPaint(Graphics g, int row)
		{
			this.row=row;
			setComponentRow();
			if (row==module.selectedRow)
				g.setColor(new Color(100,100,205));
			else if (row<module.highlightedRows.length&&module.highlightedRows[row])
				g.setColor(new Color(150,100,205));
			else
				g.setColor(new Color(150,150,255));
			g.fillRect(0,row*ROWHEIGHT,module.width(),ROWHEIGHT);
			g.setColor(Color.BLACK);
		}

		public String getXML()
		{
			String x="<state>\n<statename>"+name+"</statename>\n";
			for (int i=0; i<controlInstructions.size(); i++)
				x+=((ControlInstruction)(controlInstructions.elementAt(i))).getXML();
			for (int i=0; i<nextStates.size(); i++)
				x+=((NextState)(nextStates.elementAt(i))).getXML();
			x+="</state>\n";
			return x;
		}
		public void parseXML(String[] elements)
		{
			if (!elements[0].equals("<state>")) return;
			for (int i=1; i<elements.length-1; i++)
			{
				if (elements[i].equals("<statename>"))
				{
					name=elements[i+1];
					label.setText("STATE: "+name);
				}
				else if (elements[i].equals("<microinstruction>"))
				{
					int j;
					for (j=i; ;j++)
						if(elements[j].equals("</microinstruction>"))
							break;
					String[] m=new String[j-i+1];
					for (int k=i; k<=j; k++)
						m[k-i]=elements[k];
					ControlInstruction n=new ControlInstruction(computer,"",module);
					controlInstructions.add(n);
					n.parseXML(m);
				}
				else if (elements[i].equals("<nextstate>"))
				{
					int j;
					for (j=i; ;j++)
						if(elements[j].equals("</nextstate>"))
							break;
					String[] m=new String[j-i+1];
					for (int k=i; k<=j; k++)
						m[k-i]=elements[k];
					NextState n=new NextState(computer,"",module);
					nextStates.add(n);
					n.parseXML(m);
				}
			}
		}
	}

	public static class ControlInstruction extends ControlRow
	{
		public int row;
		public JList[] controlInputList;
		public JScrollPane[] controlInputListPane;
		public JLabel[] controlInputName;
		public JList[] controlOutputList;
		public JScrollPane[] controlOutputListPane;
		public JLabel[] controlOutputName;
		public JCheckBox[] controlOutputBox;
		private int[] controlOutputType;
		public JLabel controlInputLabel;
		DatapathBuilder.Part[] blockpath=null;

		public ArrayList<String> microcodes;
		
		public boolean valid=false;
		
		public void doMouse()
		{
			doHighlight();
			showDetails();
//			computer.controlBuilder.controlControl.edit.setVisible(true);
			computer.controlBuilder.controlControl.edit.setText("");
			computer.controlBuilder.controlControl.update.setText("Trace");
			computer.controlBuilder.controlControl.update.setVisible(true);
		}
		//illuminate all the block paths
		public void doHighlight()
		{
			String label="";
			if (module==computer.controlBuilder.defaultControl) computer.datapathBuilder.unselectAll();
			for (String microcode:microcodes)
			{
				String[] nameParts=microcode.split(" ");
				if (nameParts.length>=2)
				{
					blockpath=module.datapathModule.tracePath(nameParts[0], nameParts[nameParts.length-1]);
					if (blockpath!=null)
						for (int i=0; i<blockpath.length; i++)
							blockpath[i].selected=true;
					label+=nameParts[0]+"->"+nameParts[nameParts.length-1]+";  ";
				}
			}
			if (module==computer.controlBuilder.defaultControl) computer.datapathBuilder.repaint();
			computer.controlBuilder.setStatusLabel(label);
		}
		
		public boolean modifyControlInstruction(String name)
		{
			//decode the name: should consist of two elements: start block, end block, or no elements for user selection
			String[] nameParts=name.split(" ");
			if (nameParts.length>=2)
			{
				blockpath=module.datapathModule.tracePath(nameParts[0], nameParts[nameParts.length-1]);
				if (blockpath!=null)
				{
					if (module==computer.controlBuilder.defaultControl)
					{
						//highlight the blocks on the path
						computer.datapathBuilder.unselectAll();
						for (int i=0; i<blockpath.length; i++)
							blockpath[i].selected=true;
						computer.datapathBuilder.repaint();
					}
				}
				else
					return false;
				//set the muxes
				for (int i=0; i<blockpath.length; i++)
				{
					if(blockpath[i].type.equals("multiplexor"))
					{
						for (int j=0; j<controlOutputName.length; j++)
						{
							if (blockpath[i].name.equals(controlOutputName[j].getText()))
							{
								DatapathBuilder.Part[] muxinputs=blockpath[i].getInputBlocks();
								for (int k=0; k<muxinputs.length; k++)
								{
									if (muxinputs[k]==blockpath[i+1])
									{
										//is there a conflict?
										if (controlOutputList[j].getSelectedIndex()!=k+1 && controlOutputList[j].getSelectedIndex()!=0)
											return false;
										//set the mux
										controlOutputList[j].setSelectedIndex(k+1);
									}
								}
								
							}
						}
					}
				}
				//check the registers
				for (int j=0; j<controlOutputName.length; j++)
				{
					if (blockpath[0].name.equals(controlOutputName[j].getText()))
					{
						if (controlOutputBox[j]!=null)
							controlOutputBox[j].setSelected(true);
					}
				}
				microcodes.add(name);
				doHighlight();
				return true;
			}
			else
				return false;
		}
		
		public ControlInstruction(Computer computer, String name, ControlModule module)
		{
			super(computer,name,2,module);
//			if (computer.datapathBuilder==null)
//				return;
//System.out.println("new control instruction: "+name);			
			microcodes=new ArrayList<String>();
			
			//decode the name: should consist of two elements: start block, end block, or no elements for user selection
			String[] nameParts=name.split(" ");
			if (nameParts.length>=2)
				blockpath=module.datapathModule.tracePath(nameParts[0], nameParts[nameParts.length-1]);
			//don't add a row if there is an invalid path specified
			if (blockpath==null && !name.equals("")) 
				return;
			
			valid=true;
			
			String[] ci=module.datapathModule.controlOutputs();
			controlInputList=new JList[ci.length];
			controlInputListPane=new JScrollPane[ci.length];
			controlInputName=new JLabel[ci.length];
			
			for (int i=0; i<ci.length; i++)
			{
				int w=Integer.parseInt(ci[i].substring(0,ci[i].indexOf(" ")));
				String n=ci[i].substring(ci[i].indexOf(" ")+1,ci[i].length());
				controlInputName[i]=new JLabel(n);
				controlInputName[i].setBounds(GAPWIDTH+(LABELWIDTH+TEXTWIDTH)*i+TEXTWIDTH,row*ROWHEIGHT+1,LABELWIDTH-3,ROWHEIGHT-2);
				String[] v=new String[(int)Math.pow(2,w)+1];
				v[0]="X";
				for (int j=0; j<Math.pow(2,w); j++)
					v[1+j]=Integer.toHexString(j);
				controlInputList[i]=new JList(v);
				controlInputList[i].setSelectedIndex(0);
				controlInputListPane[i]=new JScrollPane(controlInputList[i]);
				controlInputListPane[i].setBounds(GAPWIDTH+(LABELWIDTH+TEXTWIDTH)*i,row*ROWHEIGHT+1,TEXTWIDTH-3,ROWHEIGHT-2);
				if (computer.controlBuilder!=null && module==computer.controlBuilder.defaultControl) computer.controlBuilder.drawingcomponent.add(controlInputName[i]);
				if (computer.controlBuilder!=null && module==computer.controlBuilder.defaultControl) computer.controlBuilder.drawingcomponent.add(controlInputListPane[i]);
			}
			controlInputLabel=new JLabel();
			controlInputLabel.setBounds(GAPWIDTH, row*ROWHEIGHT+1, (LABELWIDTH+TEXTWIDTH)*ci.length, ROWHEIGHT-2);
			if (computer.controlBuilder!=null && module==computer.controlBuilder.defaultControl) computer.controlBuilder.drawingcomponent.add(controlInputLabel);

			String[] co=module.datapathModule.controlInputs();

			controlOutputName=new JLabel[co.length];
			controlOutputList=new JList[co.length];
			controlOutputListPane=new JScrollPane[co.length];
			controlOutputBox=new JCheckBox[co.length];
			controlOutputType=new int[co.length];
			for (int i=0; i<co.length; i++)
			{
//System.out.println("control output: "+co[i]);
				int w=Integer.parseInt(co[i].substring(0,co[i].indexOf(" ")));
				String n=co[i].substring(co[i].indexOf(" ")+1,co[i].length());
				String t=n.substring(0,n.indexOf(" "));
				n=n.substring(n.indexOf(" ")+1,n.length());
				controlOutputName[i]=new JLabel(n);
				controlOutputName[i].setBounds(GAPWIDTH+(LABELWIDTH+TEXTWIDTH)*ci.length+GAPWIDTH*2+(LABELWIDTH+TEXTWIDTH)*i+TEXTWIDTH,row*ROWHEIGHT+1,LABELWIDTH-3,ROWHEIGHT-2);
				if (computer.controlBuilder!=null && module==computer.controlBuilder.defaultControl) computer.controlBuilder.drawingcomponent.add(controlOutputName[i]);
				if (t.equals("mux"))
				{
					String[] v=new String[w+1];
					for (int j=0; j<w; j++)
						v[j+1]=Integer.toHexString(j);
					v[0]="X";
					controlOutputList[i]=new JList(v);
					controlOutputList[i].setSelectedIndex(0);
					controlOutputListPane[i]=new JScrollPane(controlOutputList[i]);
					controlOutputListPane[i].setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
					controlOutputType[i]=1;
					
					if (blockpath!=null)
					{
						for (int j=0; j<blockpath.length; j++)
						{
							if (blockpath[j].name.equals(n))
							{
								DatapathBuilder.Part[] muxinputs=blockpath[j].getInputBlocks();
								for (int k=0; k<muxinputs.length; k++)
								{
									if (muxinputs[k]==blockpath[j+1])
										controlOutputList[i].setSelectedIndex(k+1);
								}
								break;
							}
						}
					}
				}
				else if (t.equals("alu"))
				{
					String[] v=new String[]{"X","+","-","*","/","AND","OR","XOR","NAND","NOR","XNOR","NOT","==","<","<=",">",">=",">>","<<","+1","-1","==0?","!=0?","0","IN1","IN2","NOP"};
					controlOutputList[i]=new JList(v);
					controlOutputList[i].setSelectedIndex(0);
					controlOutputListPane[i]=new JScrollPane(controlOutputList[i]);
					controlOutputListPane[i].setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
					controlOutputType[i]=2;
				}
				else
				{
					controlOutputBox[i]=new JCheckBox();
					controlOutputListPane[i]=new JScrollPane(controlOutputBox[i]);
					controlOutputListPane[i].setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
					controlOutputListPane[i].setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
					controlOutputType[i]=3;
				}
				if (computer.controlBuilder!=null && module==computer.controlBuilder.defaultControl) controlOutputListPane[i].setBounds(computer.controlBuilder.defaultControl.field1width()+GAPWIDTH+(LABELWIDTH+TEXTWIDTH)*i,row*ROWHEIGHT+1,TEXTWIDTH-3,ROWHEIGHT-2);
				if (computer.controlBuilder!=null && module==computer.controlBuilder.defaultControl) computer.controlBuilder.drawingcomponent.add(controlOutputListPane[i]);
			}
			modifyControlInstruction(name);
		}

		public boolean isUnconditional()
		{
			for (int i=0; i<controlInputList.length; i++)
				if (controlInputList[i].getSelectedIndex()>0)
					return false;
			return true;
		}
		private void generateControlInputLabel()
		{
			String label="IF ";
			for (int i=0; i<controlInputList.length; i++)
				if (controlInputList[i].getSelectedIndex()>0)
				{
					if (!label.equals("IF "))
						label+=" AND ";
					label+=controlInputName[i].getText()+"==";
					label+=controlInputList[i].getSelectedValue();
				}
			if (label.equals("IF "))
				label="DEFAULT";
			controlInputLabel.setText(label);
		}
		
		public void showDetails()
		{
			for (int i=0; i<controlInputList.length; i++)
			{
				controlInputListPane[i].setVisible(true);
				controlInputName[i].setVisible(true);
			}
			controlInputLabel.setVisible(false);
		}
		public void hideDetails()
		{
			for (int i=0; i<controlInputList.length; i++)
			{
				controlInputListPane[i].setVisible(false);
				controlInputName[i].setVisible(false);
			}
			generateControlInputLabel();
			controlInputLabel.setVisible(true);
		}

		public void rename(String s){}
		
		public void remove()
		{
			for (int i=0; i<controlInputListPane.length; i++)
				if (computer.controlBuilder!=null && module==computer.controlBuilder.defaultControl) computer.controlBuilder.drawingcomponent.remove(controlInputListPane[i]);
			for (int i=0; i<controlOutputListPane.length; i++)
				if (computer.controlBuilder!=null && module==computer.controlBuilder.defaultControl) computer.controlBuilder.drawingcomponent.remove(controlOutputListPane[i]);
			for (int i=0; i<controlInputName.length; i++)
				if (computer.controlBuilder!=null && module==computer.controlBuilder.defaultControl) computer.controlBuilder.drawingcomponent.remove(controlInputName[i]);
			for (int i=0; i<controlOutputName.length; i++)
				if (computer.controlBuilder!=null && module==computer.controlBuilder.defaultControl) computer.controlBuilder.drawingcomponent.remove(controlOutputName[i]);
			if (computer.controlBuilder!=null && module==computer.controlBuilder.defaultControl) 		 computer.controlBuilder.drawingcomponent.remove(controlInputLabel);
		}

		private void setComponentRow()
		{
//			if (computer.datapathBuilder==null) return;
			for (int i=0; i<controlInputList.length; i++)
				controlInputListPane[i].setBounds(GAPWIDTH+(LABELWIDTH+TEXTWIDTH)*i,row*ROWHEIGHT+1,TEXTWIDTH-3,ROWHEIGHT-2);
			for (int i=0; i<controlInputName.length; i++)
				controlInputName[i].setBounds(GAPWIDTH+(LABELWIDTH+TEXTWIDTH)*i+TEXTWIDTH,row*ROWHEIGHT+1,LABELWIDTH-3,ROWHEIGHT-2);
			for (int i=0; i<controlOutputListPane.length; i++)
				if (computer.controlBuilder!=null && module==computer.controlBuilder.defaultControl) controlOutputListPane[i].setBounds(computer.controlBuilder.defaultControl.field1width()+GAPWIDTH+(LABELWIDTH+TEXTWIDTH)*i,row*ROWHEIGHT+1,TEXTWIDTH-3,ROWHEIGHT-2);
			for (int i=0; i<controlOutputName.length; i++)
				if (computer.controlBuilder!=null && module==computer.controlBuilder.defaultControl) controlOutputName[i].setBounds(computer.controlBuilder.defaultControl.field1width()+GAPWIDTH+(LABELWIDTH+TEXTWIDTH)*i+TEXTWIDTH,row*ROWHEIGHT+1,LABELWIDTH-3,ROWHEIGHT-2);
			if (computer.controlBuilder!=null && module==computer.controlBuilder.defaultControl) controlInputLabel.setBounds(GAPWIDTH, row*ROWHEIGHT+1, (LABELWIDTH+TEXTWIDTH)*computer.controlBuilder.defaultControl.field1width(), ROWHEIGHT-2);
		}

		public void doPaint(Graphics g, int row)
		{
			this.row=row;
			setComponentRow();
			if (row==module.selectedRow)
				g.setColor(new Color(205,205,100));
			else if (row<module.highlightedRows.length && module.highlightedRows[row])
				g.setColor(new Color(255,205,100));
			else
				g.setColor(new Color(255,255,150));
			g.fillRect(0,row*ROWHEIGHT,computer.controlBuilder.defaultControl.width(),ROWHEIGHT);
			if (row!=module.selectedRow)
				hideDetails();
		}

		public boolean isActive()
		{
//			if (computer.datapathBuilder==null) return false;

			for (int i=0; i<controlInputList.length; i++)
			{
				String s=(String)controlInputList[i].getSelectedValue();
				if (s==null || s.equals("") || s.equals("X"))
					continue;
				
				DatapathBuilder.Block b=module.datapathModule.getBlock(controlInputName[i].getText());
				if (Long.toHexString(b.getValue()).equals(s))
					continue;
				return false;
			}
			return true;
		}

		public void doInstruction()
		{
//			if (computer.datapathBuilder==null) return;
			for (int i=0; i<controlOutputList.length; i++)
			{
				String s=null;
				if (controlOutputList[i]!=null)
				{
					s=(String)controlOutputList[i].getSelectedValue();
					if (s==null || s.equals(""))
						s="";
				}

				DatapathBuilder.Block b=module.datapathModule.getBlock(controlOutputName[i].getText());
				if (s==null && controlOutputBox[i].isSelected())
					b.clockSetting=true;
				else if (s!=null && !s.equals("X")&&!s.equals(""))
					b.operationSetting=s;
			}
		}

		public String getXML()
		{
			String x="<microinstruction>\n";
			for (int i=0; i<controlInputList.length; i++)
			{
				x+="<input "+controlInputName[i].getText()+">";
				if (controlInputList[i].getSelectedValue()==null)
					x+="X";
				else
					x+=controlInputList[i].getSelectedValue();
				x+="</input>\n";
			}
			for (int i=0; i<controlOutputList.length; i++)
			{
				if (controlOutputType[i]==1 && controlOutputList[i].getSelectedValue()!=null)
				{
					x+="<mux "+controlOutputName[i].getText()+">"+controlOutputList[i].getSelectedValue()+"</mux>\n";
				}
				else if (controlOutputType[i]==2 && controlOutputList[i].getSelectedValue()!=null)
				{
					x+="<alu "+controlOutputName[i].getText()+">"+controlOutputList[i].getSelectedValue()+"</alu>\n";
				}
				else if (controlOutputType[i]==3 && controlOutputBox[i].isSelected())
				{
					x+="<reg "+controlOutputName[i].getText()+">"+"</reg>\n";
				}
			}
			x+="</microinstruction>\n";
			return x;
		}

		public void parseXML(String[] elements)
		{
			if (!elements[0].equals("<microinstruction>")) return;
			for (int i=1; i<elements.length-1; i++)
			{
				if (elements[i].length()>7 && elements[i].substring(0,7).equals("<input "))
				{
					String name=elements[i].substring(7,elements[i].length()-1);
					for (int j=0; j<controlInputName.length; j++)
					{
						if (controlInputName[j].getText().equals(name))
						{
							controlInputList[j].setSelectedValue(elements[i+1],true);
						}
					}
				}
				else if (elements[i].length()>5 && elements[i].substring(0,5).equals("<mux "))
				{
					String name=elements[i].substring(5,elements[i].length()-1);
					for (int j=0; j<controlOutputName.length; j++)
					{
						if (controlOutputName[j].getText().equals(name))
						{
							controlOutputList[j].setSelectedValue(elements[i+1],true);
						}
					}
				}
				else if (elements[i].length()>5 && elements[i].substring(0,5).equals("<alu "))
				{
					String name=elements[i].substring(5,elements[i].length()-1);
					for (int j=0; j<controlOutputName.length; j++)
					{
						if (controlOutputName[j].getText().equals(name))
						{
							controlOutputList[j].setSelectedValue(elements[i+1],true);
						}
					}
				}
				else if (elements[i].length()>5 && elements[i].substring(0,5).equals("<reg "))
				{
					String name=elements[i].substring(5,elements[i].length()-1);
					for (int j=0; j<controlOutputName.length; j++)
					{
						if (controlOutputName[j].getText().equals(name))
						{
							controlOutputBox[j].setSelected(true);
						}
					}
				}
			}
		}
	}

	public static class NextState extends ControlRow
	{
//		public String state;
		public int row;

		public JList[] controlInputList;
		public JScrollPane[] controlInputListPane;
		public JLabel[] controlInputName;
		private JLabel nextStateLabel;
		private JLabel controlInputLabel;

		public NextState(Computer computer, String name, ControlModule module)
		{
			super(computer,name,3,module);
//			this.state=name;
//			if (computer.datapathBuilder!=null)
//			{
				String[] ci=module.datapathModule.controlOutputs();
				controlInputList=new JList[ci.length];
				controlInputListPane=new JScrollPane[ci.length];
				controlInputName=new JLabel[ci.length];
				for (int i=0; i<ci.length; i++)
				{
					int w=Integer.parseInt(ci[i].substring(0,ci[i].indexOf(" ")));
					String n=ci[i].substring(ci[i].indexOf(" ")+1,ci[i].length());
					controlInputName[i]=new JLabel(n);
					controlInputName[i].setBounds(GAPWIDTH+(LABELWIDTH+TEXTWIDTH)*i+TEXTWIDTH,row*ROWHEIGHT+1,LABELWIDTH-3,ROWHEIGHT-2);
					String[] v=new String[(int)Math.pow(2,w)+1];
					v[0]="X";
					for (int j=0; j<Math.pow(2,w); j++)
						v[1+j]=Integer.toHexString(j);
					controlInputList[i]=new JList(v);
					controlInputList[i].setSelectedIndex(0);

					controlInputListPane[i]=new JScrollPane(controlInputList[i]);
					controlInputListPane[i].setBounds(GAPWIDTH+(LABELWIDTH+TEXTWIDTH)*i,row*ROWHEIGHT+1,TEXTWIDTH-3,ROWHEIGHT-2);
					if (computer.controlBuilder!=null && module==computer.controlBuilder.defaultControl) computer.controlBuilder.drawingcomponent.add(controlInputName[i]);
					if (computer.controlBuilder!=null && module==computer.controlBuilder.defaultControl) computer.controlBuilder.drawingcomponent.add(controlInputListPane[i]);
				}
//			}
			controlInputLabel=new JLabel();
			controlInputLabel.setBounds(GAPWIDTH, row*ROWHEIGHT+1, (LABELWIDTH+TEXTWIDTH)*ci.length, ROWHEIGHT-2);
			if (computer.controlBuilder!=null && module==computer.controlBuilder.defaultControl) computer.controlBuilder.drawingcomponent.add(controlInputLabel);

			nextStateLabel=new JLabel("NEXT STATE: "+name);
			if (controlInputListPane!=null)
				if (computer.controlBuilder!=null && module==computer.controlBuilder.defaultControl) nextStateLabel.setBounds(GAPWIDTH+computer.controlBuilder.defaultControl.field1width(),row*ROWHEIGHT+1,computer.controlBuilder.defaultControl.field2width()-GAPWIDTH*2,ROWHEIGHT-2);
			if (computer.controlBuilder!=null && module==computer.controlBuilder.defaultControl) computer.controlBuilder.drawingcomponent.add(nextStateLabel);
		}

		public void doMouse()
		{
			showDetails();
			computer.controlBuilder.controlControl.prompt(name);
		}

		private void generateControlInputLabel()
		{
			String label="IF ";
			for (int i=0; i<controlInputList.length; i++)
				if (controlInputList[i].getSelectedIndex()>0)
				{
					if (!label.equals("IF "))
						label+=" AND ";
					label+=controlInputName[i].getText()+"==";
					label+=controlInputList[i].getSelectedValue();
				}
			if (label.equals("IF "))
				label="DEFAULT";
			controlInputLabel.setText(label);
		}
		
		public void showDetails()
		{
			for (int i=0; i<controlInputList.length; i++)
			{
				controlInputListPane[i].setVisible(true);
				controlInputName[i].setVisible(true);
			}
			controlInputLabel.setVisible(false);
		}
		public void hideDetails()
		{
			for (int i=0; i<controlInputList.length; i++)
			{
				controlInputListPane[i].setVisible(false);
				controlInputName[i].setVisible(false);
			}
			generateControlInputLabel();
			controlInputLabel.setVisible(true);
		}

		public void rename(String s)
		{
			name=s;
//			state=s;
			nextStateLabel.setText("NEXT STATE: "+name);
		}
		
		public void remove()
		{
			for (int i=0; i<controlInputListPane.length; i++)
				if (computer.controlBuilder!=null && module==computer.controlBuilder.defaultControl) computer.controlBuilder.drawingcomponent.remove(controlInputListPane[i]);
			for (int i=0; i<controlInputName.length; i++)
				if (computer.controlBuilder!=null && module==computer.controlBuilder.defaultControl) computer.controlBuilder.drawingcomponent.remove(controlInputName[i]);
			if (computer.controlBuilder!=null && module==computer.controlBuilder.defaultControl) computer.controlBuilder.drawingcomponent.remove(nextStateLabel);
			if (computer.controlBuilder!=null && module==computer.controlBuilder.defaultControl) 		 computer.controlBuilder.drawingcomponent.remove(controlInputLabel);
		}

		public boolean isActive()
		{
//			if (computer.datapathBuilder==null) return false;

			for (int i=0; i<controlInputList.length; i++)
			{
				String s=(String)controlInputList[i].getSelectedValue();
				if (s==null || s.equals("") || s.equals("X"))
					continue;
				
				DatapathBuilder.Block b=module.datapathModule.getBlock(controlInputName[i].getText());
				if (Long.toHexString(b.value).equals(s))
					continue;
				return false;
			}
			return true;
		}

		private void setComponentRow()
		{
//			if (computer.datapathBuilder==null) return;
			for (int i=0; i<controlInputList.length; i++)
				controlInputListPane[i].setBounds(GAPWIDTH+(LABELWIDTH+TEXTWIDTH)*i,row*ROWHEIGHT+1,TEXTWIDTH-3,ROWHEIGHT-2);
			for (int i=0; i<controlInputName.length; i++)
				controlInputName[i].setBounds(GAPWIDTH+(LABELWIDTH+TEXTWIDTH)*i+TEXTWIDTH,row*ROWHEIGHT+1,LABELWIDTH-3,ROWHEIGHT-2);
			if (computer.controlBuilder!=null && module==computer.controlBuilder.defaultControl) nextStateLabel.setBounds(GAPWIDTH+computer.controlBuilder.defaultControl.field1width(),row*ROWHEIGHT+1,computer.controlBuilder.defaultControl.field2width()-GAPWIDTH*2,ROWHEIGHT-2);
			if (computer.controlBuilder!=null && module==computer.controlBuilder.defaultControl) controlInputLabel.setBounds(GAPWIDTH, row*ROWHEIGHT+1, (LABELWIDTH+TEXTWIDTH)*computer.controlBuilder.defaultControl.field1width(), ROWHEIGHT-2);
		}

		public void doPaint(Graphics g, int row)
		{
			this.row=row;
			setComponentRow();
			if (row==module.selectedRow)
				g.setColor(new Color(100,205,100));
			else if (row<module.highlightedRows.length && module.highlightedRows[row])
				g.setColor(new Color(150,205,100));
			else
				g.setColor(new Color(150,255,150));
			g.fillRect(0,row*ROWHEIGHT,computer.controlBuilder.defaultControl.width(),ROWHEIGHT);
			if (row!=module.selectedRow)
				hideDetails();
		}

		public String getXML()
		{
			String x="<nextstate>\n";
			for (int i=0; i<controlInputList.length; i++)
			{
				x+="<input "+controlInputName[i].getText()+">";
				if (controlInputList[i].getSelectedValue()==null)
					x+="X";
				else
					x+=controlInputList[i].getSelectedValue();
				x+="</input>\n";
			}
			x+="<nextstatename>"+name+"</nextstatename>\n";
			x+="</nextstate>\n";
			return x;
		}

		public void parseXML(String[] elements)
		{
			if (!elements[0].equals("<nextstate>")) return;
			for (int i=1; i<elements.length-1; i++)
			{
				if (elements[i].equals("<nextstatename>"))
				{
					name=elements[i+1];
					nextStateLabel.setText("NEXT STATE: "+name);
				}
				else if (elements[i].length()>7 && elements[i].substring(0,7).equals("<input "))
				{
					String name=elements[i].substring(7,elements[i].length()-1);
					for (int j=0; j<controlInputName.length; j++)
					{
						if (controlInputName[j].getText().equals(name))
						{
							controlInputList[j].setSelectedValue(elements[i+1],true);
						}
					}
				}
			}
		}
	}

	public static class Vector
	{
		VectorElement first=null;
		public Vector()
		{
			first=null;
		}
		public void add(ControlRow o)
		{
			VectorElement e=first;
			if (e==null)
				first=new VectorElement(o);
			else
			{
				while(e.nextElement!=null)
					e=e.nextElement;
				e.nextElement=new VectorElement(o);
			}
		}
		public void delete(int index)
		{
			VectorElement e=first;
			if (e==null)
				return;
			elementAt(index).remove();
			if (index==0)
				first=first.nextElement;
			else
				elementNumber(index-1).nextElement=elementNumber(index).nextElement;
		}
		public void addAfter(ControlRow o, int index)
		{
			VectorElement x=new VectorElement(o);
			x.nextElement=elementNumber(index+1);
			elementNumber(index).nextElement=x;
		}
		public void addFirst(ControlRow o)
		{
			VectorElement e=new VectorElement(o);
			e.nextElement=first;
			first=e;
		}
		public int size()
		{
			int length=0;
			VectorElement e=first;
			while(e!=null)
			{
				length++;
				e=e.nextElement;
			}
			return length;
		}
		public ControlRow elementAt(int i)
		{
			int length=0;
			VectorElement e=first;
			while(e!=null && length!=i)
			{
				length++;
				e=e.nextElement;
			}
			return e.element;
		}
		public VectorElement elementNumber(int i)
		{
			int length=0;
			VectorElement e=first;
			while(e!=null && length!=i)
			{
				length++;
				e=e.nextElement;
			}
			return e;
		}
		public ControlRow lastElement()
		{
			VectorElement e=first;
			if (e==null)
				return null;
			while(e.nextElement!=null)
			{
				e=e.nextElement;
			}
			return e.element;
		}

		private class VectorElement
		{
			ControlRow element;
			VectorElement nextElement;
			public VectorElement(ControlRow o)
			{
				element=o;
				nextElement=null;
			}
		}
	}
	
	public class ControlControl extends JComponent
	{
		int width=100, height=2000;
		int line1,line2;
		String pathname="path 0";
		String statename="state 0";
		
		JTextField edit;
		JButton update;
		
		public Dimension getPreferredSize()
		{
			return new Dimension(width,height);
		}
		public ControlControl()
		{
			super();
			int ctop=10;
			
			JButton button;
			JLabel label;
			int fontSize=10;
			int cwidth=width-10;
						
			button=new JButton("Close");
			button.setFont(new Font("Dialog",Font.PLAIN,fontSize));
			button.setBounds(5,ctop,cwidth,20);
			button.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent arg0) {
					close();
				}});
			add(button);
			ctop+=25;
			button=new JButton("Save");
			button.setFont(new Font("Dialog",Font.PLAIN,fontSize));
			button.setBounds(5,ctop,cwidth,20);
			button.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent arg0) {
					System.out.println(defaultControl.getXML());
					JFileChooser fc = new JFileChooser();
					fc.setCurrentDirectory(new File("."));
					fc.showSaveDialog(null);
					File f = fc.getSelectedFile();
					if (f==null) return;
					String name=f.getAbsolutePath();
					try
					{
						PrintWriter p =new PrintWriter(name);
						p.println(defaultControl.getXML());
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
				}});
			add(button);
			ctop+=25;
			button=new JButton("Load");
			button.setFont(new Font("Dialog",Font.PLAIN,fontSize));
			button.setBounds(5,ctop,cwidth,20);
			button.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent arg0) {
					doload();
				}});
			add(button);
			ctop+=25;
			button=new JButton("Undo");
			button.setFont(new Font("Dialog",Font.PLAIN,fontSize));
			button.setBounds(5,ctop,cwidth,20);
			button.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent arg0) {
				}});
			add(button);
			ctop+=25;
			button=new JButton("Delete");
			button.setFont(new Font("Dialog",Font.PLAIN,fontSize));
			button.setBounds(5,ctop,cwidth,20);
			button.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent arg0) {
					if (defaultControl.selectedRow!=-1)
					{
						defaultControl.getSelectedVector().delete(defaultControl.getSelectedIndex());
						defaultControl.selectedRow=-1;
						computer.controlBuilder.drawingcomponent.revalidate();
						computer.controlBuilder.repaint();
					}
				}});
			add(button);
			ctop+=25;
			button=new JButton("Verify");
			button.setFont(new Font("Dialog",Font.PLAIN,fontSize));
			button.setBounds(5,ctop,cwidth,20);
			button.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent arg0) {
				}});
			add(button);
			ctop+=25;
			line1=ctop;
			ctop+=5;
			
			button=new JButton("New Path");
			button.setFont(new Font("Dialog",Font.PLAIN,fontSize));
			button.setBounds(5,ctop,cwidth,20);
			button.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent arg0) {
					if (defaultControl.selectedRow==-1 || defaultControl.getSelectedVector()==null)
						defaultControl.controlPaths.add(new ControlPath(computer,pathname,defaultControl,true));
					else
					{
						if(defaultControl.getSelectedVector().lastElement().type==0)
							defaultControl.getSelectedVector().addAfter(new ControlPath(computer,pathname,defaultControl,true),defaultControl.getSelectedIndex());
					}
					computer.controlBuilder.drawingcomponent.revalidate();
					computer.controlBuilder.repaint();
					pathname=increment(pathname);
				}});
			add(button);
			ctop+=25;

			button=new JButton("New State");
			button.setFont(new Font("Dialog",Font.PLAIN,fontSize));
			button.setBounds(5,ctop,cwidth,20);
			button.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent arg0) {
					if (defaultControl.selectedRow==-1 || defaultControl.getSelectedVector()==null)
						((ControlPath)defaultControl.controlPaths.lastElement()).controlStates.add(new ControlState(computer,statename,defaultControl,true));
					else if(defaultControl.getSelectedVector().lastElement().type==1)
						defaultControl.getSelectedVector().addAfter(new ControlState(computer,statename,defaultControl,true),defaultControl.getSelectedIndex());
					else if (defaultControl.getSelectedVector().lastElement().type==0)
						((ControlPath)defaultControl.getSelectedVector().elementAt(defaultControl.getSelectedIndex())).controlStates.addFirst(new ControlState(computer,statename,defaultControl,true));
					computer.controlBuilder.drawingcomponent.revalidate();
					computer.controlBuilder.repaint();
					statename=increment(statename);
				}});
			add(button);
			ctop+=25;
			
			button=new JButton("New Instruction");
			button.setFont(new Font("Dialog",Font.PLAIN,fontSize));
			button.setBounds(5,ctop,cwidth,20);
			button.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent arg0) {
					ControlInstruction c=new ControlInstruction(computer,"",defaultControl);
					if (c.valid)
					{
						if (defaultControl.selectedRow==-1)
						{
							((ControlState)((ControlPath)defaultControl.controlPaths.lastElement()).controlStates.lastElement()).controlInstructions.add(c);
						}
						else if(defaultControl.getSelectedVector().lastElement().type==2)
						{
							defaultControl.getSelectedVector().addAfter(c,defaultControl.getSelectedIndex());
						}
						else if (defaultControl.getSelectedVector().lastElement().type==1)
						{
							((ControlState)defaultControl.getSelectedVector().elementAt(defaultControl.getSelectedIndex())).controlInstructions.addFirst(c);
						}
						computer.controlBuilder.drawingcomponent.revalidate();
						computer.controlBuilder.repaint();
					}
				}});
			add(button);
			ctop+=25;
			
			button=new JButton("New Next Instruction");
			button.setFont(new Font("Dialog",Font.PLAIN,fontSize));
			button.setBounds(5,ctop,cwidth,20);
			button.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent arg0) {
					String defaultname="state 0";
					//if no row is selected, add the row to the last state
					if (defaultControl.getSelectedVector()==null)
						((ControlState)((ControlPath)defaultControl.controlPaths.lastElement()).controlStates.lastElement()).nextStates.add(new NextState(computer,defaultname,defaultControl));
					// if a next state is selected, add the row under it
					else if(defaultControl.getSelectedVector().lastElement().type==3)
					{
						defaultControl.getSelectedVector().addAfter(new NextState(computer,defaultname,defaultControl),defaultControl.getSelectedIndex());
					}
					// if a state row is selected, add the row before the other next states
						
					else if (defaultControl.getSelectedVector().lastElement().type==1)
					{
						((ControlState)defaultControl.getSelectedVector().elementAt(defaultControl.getSelectedIndex())).nextStates.addFirst(new NextState(computer,defaultname,defaultControl));
					}
					computer.controlBuilder.drawingcomponent.revalidate();
					computer.controlBuilder.repaint();
				}});
			add(button);
			ctop+=25;
			line2=ctop;
			ctop+=15;
			edit=new JTextField();
			edit.setBounds(5,ctop,cwidth,20);
			add(edit);
			ctop+=25;
			update=new JButton("Update");
			update.setBounds(5,ctop,cwidth,20);
			update.setFont(new Font("Dialog",Font.PLAIN,fontSize));
			update.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent arg0) {
					ControlRow r=defaultControl.getSelectedVector().elementAt(defaultControl.getSelectedIndex());
					if (r.type==0 || r.type==1 || r.type==3)
					{
						r.rename(edit.getText());
						drawingcomponent.revalidate();
					}
					else if (r.type==2)
					{
						computer.datapathBuilder.placeroute();
					}
//					unprompt();
				}});
			add(update);
			ctop+=25;
			
			unprompt();
		}
		public void prompt(String n)
		{
			edit.setText(n);
			edit.setVisible(true);
			update.setVisible(true);
		}
		public void unprompt()
		{
			update.setText("Update");
			edit.setVisible(false);
			update.setVisible(false);
			setStatusLabel("");
		}
		public void route(String name)
		{
			edit.setText(name);
			ControlRow r=defaultControl.getSelectedVector().elementAt(defaultControl.getSelectedIndex());
			((ControlInstruction)r).modifyControlInstruction(edit.getText());
			computer.controlBuilder.toFront();
		}
		private String increment(String s)
		{
			String t,u;
			if (s.indexOf(" ")!=-1)
			{
				t = s.substring(0,s.indexOf(" "));
				u = s.substring(s.indexOf(" ")+1,s.length());
			}
			else
			{
				return s;
			}
			return t+" "+(Integer.parseInt(u)+1);
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
	public void doload()
	{
		JFileChooser fc = new JFileChooser();
		fc.setCurrentDirectory(new File("."));
		fc.showOpenDialog(null);
		File f = fc.getSelectedFile();
		if (f==null) return;
		clear();
		doload(f.getAbsolutePath(),defaultControl);
		computer.controlBuilder.guiComponent.revalidate();
		computer.controlBuilder.repaint();		
	}
	public void clear()
	{
		drawingcomponent.removeAll();
		drawingcomponent.revalidate();
		defaultControl=new ControlModule(computer,defaultControl.datapathModule);
	}
	
	public static void doload(String name,ControlModule module)
	{
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

		ControlXMLParse xmlParse=new ControlXMLParse(xml);
		module.parseXML(xmlParse.xmlParts);
	}
	public static class ControlXMLParse
	{
		private int MAXPARTS=10000;

		String[] xmlParts;
		public ControlXMLParse(String xml)
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

//			for (int i=0; i<parts.size(); i++)
//				System.out.println(xmlParts[i]);
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
	}
	public static ControlModule loadControlModule(Computer computer, String filename, DatapathBuilder.DatapathModule dmodule)
	{
		ControlModule cmodule=new ControlModule(computer,dmodule);
		doload(filename, cmodule);
		return cmodule;
	}
	public static class ControlModule
	{
		public Vector controlPaths;

		public int selectedRow=-1;
		public boolean[] highlightedRows=new boolean[1];
		public DatapathBuilder.DatapathModule datapathModule;
		public Computer computer;

		public ControlModule(Computer computer, DatapathBuilder.DatapathModule module)
		{
			this.computer=computer;
			datapathModule=module;
			if (datapathModule==null) return;
			controlPaths=new Vector();
		}
		public void makeSimpleControl()
		{
			ControlPath path=new ControlPath(computer,"path",this,true);
			controlPaths.add(path);
			ControlInstruction inst=((ControlInstruction)((ControlState)path.controlStates.elementAt(0)).controlInstructions.elementAt(0));
			for (int i=0; i<inst.controlOutputBox.length; i++)
			{
				if (inst.controlOutputBox[i]!=null)
					inst.controlOutputBox[i].setSelected(true);
			}
			for (int i=0; i<inst.controlOutputList.length; i++)
			{
				if (inst.controlOutputList[i]!=null)
					inst.controlOutputList[i].setSelectedIndex(0);			
			}
			height();
		}
		public int width()
		{
			if (computer.datapathBuilder!=null)
				return field1width()+field2width()+field3width();
			return 1000;
		}
		public int field1width()
		{
			int w=GAPWIDTH+(LABELWIDTH+TEXTWIDTH)*datapathModule.controlOutputs().length+GAPWIDTH;
			if (w<200)
				w=200;
			return w;
		}

		public int field2width()
		{
			int w=GAPWIDTH+(LABELWIDTH+TEXTWIDTH)*datapathModule.controlInputs().length+GAPWIDTH;
			if (w<200)
				w=200;
			return w;
		}

		public int field3width()
		{
			return MICROINSTRUCTIONWIDTH;
		}

		public int height()
		{
			int rows=0;
			for (int i=0; i<controlPaths.size(); i++)
			{
				rows++;
				for (int j=0; j<((ControlPath)(controlPaths.elementAt(i))).controlStates.size(); j++)
				{
					rows++;
					for (int k=0; k<((ControlState)(((ControlPath)(controlPaths.elementAt(i))).controlStates.elementAt(j))).controlInstructions.size(); k++)
					{
						rows++;
					}
					for (int k=0; k<((ControlState)(((ControlPath)(controlPaths.elementAt(i))).controlStates.elementAt(j))).nextStates.size(); k++)
					{
						rows++;
					}
				}
			}
			if (rows!=highlightedRows.length)
				highlightedRows=new boolean[rows];
			return rows*ROWHEIGHT;
		}
		public void resetHighlights()
		{
			highlightedRows=new boolean[highlightedRows.length];
		}
		public Vector getSelectedVector()
		{
			if (selectedRow==-1) return null;
			
			int row=0;
			for (int i=0; i<controlPaths.size(); i++)
			{
				if (row==selectedRow) return controlPaths;
				row++;
				for (int j=0; j<((ControlPath)(controlPaths.elementAt(i))).controlStates.size(); j++)
				{
					if(row==selectedRow) return ((ControlPath)controlPaths.elementAt(i)).controlStates;
					row++;
					for (int k=0; k<((ControlState)(((ControlPath)(controlPaths.elementAt(i))).controlStates.elementAt(j))).controlInstructions.size(); k++)
					{
						if (row==selectedRow) return ((ControlState)((ControlPath)controlPaths.elementAt(i)).controlStates.elementAt(j)).controlInstructions;
						row++;
					}
					for (int k=0; k<((ControlState)(((ControlPath)(controlPaths.elementAt(i))).controlStates.elementAt(j))).nextStates.size(); k++)
					{
						if (row==selectedRow) return ((ControlState)((ControlPath)controlPaths.elementAt(i)).controlStates.elementAt(j)).nextStates;
						row++;
					}
				}
			}
			return null;
		}

		public int getSelectedIndex()
		{
			if (selectedRow==-1) return -1;
			
			int row=0;
			for (int i=0; i<controlPaths.size(); i++)
			{
				if (row==selectedRow) return i;
				row++;
				for (int j=0; j<((ControlPath)(controlPaths.elementAt(i))).controlStates.size(); j++)
				{
					if(row==selectedRow) return j;
					row++;
					for (int k=0; k<((ControlState)(((ControlPath)(controlPaths.elementAt(i))).controlStates.elementAt(j))).controlInstructions.size(); k++)
					{
						if (row==selectedRow) return k;
						row++;
					}
					for (int k=0; k<((ControlState)(((ControlPath)(controlPaths.elementAt(i))).controlStates.elementAt(j))).nextStates.size(); k++)
					{
						if (row==selectedRow) return k;
						row++;
					}
				}
			}
			return -1;
		}

		public String getXML()
		{
			String x="<control>\n";
			for (int i=0; i<controlPaths.size(); i++)
				x+=((ControlPath)(controlPaths.elementAt(i))).getXML();
			x+="</control>\n";
			return x;
		}

		public void parseXML(String[] elements)
		{
			if (!elements[0].equals("<control>")) return;
			for (int i=1; i<elements.length-1; i++)
			{
				if (elements[i].equals("<control path>"))
				{
					int j;
					for (j=i; ;j++)
						if(elements[j].equals("</control path>"))
							break;
					String[] m=new String[j-i+1];
					for (int k=i; k<=j; k++)
						m[k-i]=elements[k];
					ControlPath n=new ControlPath(computer,"",this,false);
					controlPaths.add(n);
					n.parseXML(m);
				}
			}
		}
	}
}
