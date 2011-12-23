package simulator;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import java.util.Scanner;
import java.util.ArrayList;

public class ControlBuilder extends AbstractGUI
{
	private static final int ROWHEIGHT=20,LABELWIDTH=100,TEXTWIDTH=50,MICROINSTRUCTIONWIDTH=200,GAPWIDTH=5;

	public ControlControl controlControl;

	public Vector controlPaths;

	public int selectedRow=-1;
	public boolean[] highlightedRows=new boolean[1];

	public ControlBuilder(Computer computer)
	{
		super(computer,"Control Builder",800,800,true,true,false,false);
		controlControl=new ControlControl(computer);
		controlPaths=new Vector();

		refresh();
	}

	public int field1width()
	{
		int w=GAPWIDTH+(LABELWIDTH+TEXTWIDTH)*computer.datapathBuilder.controlOutputs().length+GAPWIDTH;
		if (w<200)
			w=200;
		return w;
	}

	public int field2width()
	{
		int w=GAPWIDTH+(LABELWIDTH+TEXTWIDTH)*computer.datapathBuilder.controlInputs().length+GAPWIDTH;
		if (w<200)
			w=200;
		return w;
	}

	public int field3width()
	{
		return MICROINSTRUCTIONWIDTH;
	}

	public int width()
	{
		if (computer.datapathBuilder!=null)
			return field1width()+field2width()+field3width();
		return 1000;
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

	public void mouseClick(MouseEvent e)
	{
		if (selectedRow==e.getY()/ROWHEIGHT)
			selectedRow=-1;
		else
			selectedRow=e.getY()/ROWHEIGHT;
		repaint();
	}

	public void doPaint(Graphics g)
	{
		int rows=0;
		for (int i=0; i<controlPaths.size(); i++)
		{
			((ControlPath)controlPaths.elementAt(i)).doPaint(g,rows++);
			for (int j=0; j<((ControlPath)(controlPaths.elementAt(i))).controlStates.size(); j++)
			{
				((ControlState)((ControlPath)controlPaths.elementAt(i)).controlStates.elementAt(j)).doPaint(g,rows++);
				for (int k=0; k<((ControlState)(((ControlPath)(controlPaths.elementAt(i))).controlStates.elementAt(j))).controlInstructions.size(); k++)
				{
					((ControlInstruction)((ControlState)((ControlPath)controlPaths.elementAt(i)).controlStates.elementAt(j)).controlInstructions.elementAt(k)).doPaint(g,rows++);
				}
				for (int k=0; k<((ControlState)(((ControlPath)(controlPaths.elementAt(i))).controlStates.elementAt(j))).nextStates.size(); k++)
				{
					((NextState)((ControlState)((ControlPath)controlPaths.elementAt(i)).controlStates.elementAt(j)).nextStates.elementAt(k)).doPaint(g,rows++);
				}
			}
		}

		if (computer.datapathBuilder!=null)
		{
			g.setColor(Color.BLACK);
			g.drawLine(field1width(),0,field1width(),height());
			g.drawLine(field1width()+field2width(),0,field1width()+field2width(),height());
		}
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
				ControlPath n=new ControlPath("");
				controlPaths.add(n);
				n.parseXML(m);
			}
		}
	}

	public abstract class ControlRow
	{
		public String name;
		public int type;
		public ControlRow(String name, int type)
		{
			this.name=name;
			this.type=type;
		}
		public abstract void doPaint(Graphics g, int row);
		public abstract String getXML();
		public abstract void parseXML(String[] elements);
		public abstract void remove();
	}

	public class ControlPath extends ControlRow
	{
		public Vector controlStates;
		public int row;
		private JLabel label;

		public ControlPath(String name)
		{
			super(name,0);
			controlStates=new Vector();
			label=new JLabel("CONTROL PATH: "+name);
			label.setBounds(0,row*ROWHEIGHT+1,200,ROWHEIGHT-2);
			guiComponent.add(label);
		}
		public void remove()
		{
			guiComponent.remove(label);
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
			if (row==selectedRow)
				g.setColor(new Color(205,100,205));
			else if (highlightedRows[row])
				g.setColor(new Color(255,100,205));
			else
				g.setColor(new Color(255,150,255));
			g.fillRect(0,row*ROWHEIGHT,width(),ROWHEIGHT);
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
					ControlState n=new ControlState("");
					controlStates.add(n);
					n.parseXML(m);
				}
			}
		}
	}

	public class ControlState extends ControlRow
	{
		public Vector controlInstructions;
		public Vector nextStates;

		public int row;
		private JLabel label;

		public ControlState(String name)
		{
			super(name,1);
			controlInstructions=new Vector();
			nextStates=new Vector();
			label=new JLabel("STATE: "+name);
			label.setBounds(0,row*ROWHEIGHT+1,200,ROWHEIGHT-2);
			guiComponent.add(label);
		}

		public void remove()
		{
			guiComponent.remove(label);
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
			if (row==selectedRow)
				g.setColor(new Color(100,100,205));
			else if (highlightedRows[row])
				g.setColor(new Color(150,100,205));
			else
				g.setColor(new Color(150,150,255));
			g.fillRect(0,row*ROWHEIGHT,width(),ROWHEIGHT);
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
					ControlInstruction n=new ControlInstruction("");
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
					NextState n=new NextState("");
					nextStates.add(n);
					n.parseXML(m);
				}
			}
		}
	}

	public class ControlInstruction extends ControlRow
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
		private boolean highlight;

		public ControlInstruction(String name)
		{
			super(name,2);
			if (computer.datapathBuilder!=null)
			{
				String[] ci=computer.datapathBuilder.controlOutputs();
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
					computer.controlBuilder.guiComponent.add(controlInputName[i]);
					computer.controlBuilder.guiComponent.add(controlInputListPane[i]);
				}

				String[] co=computer.datapathBuilder.controlInputs();
				controlOutputName=new JLabel[co.length];
				controlOutputList=new JList[co.length];
				controlOutputListPane=new JScrollPane[co.length];
				controlOutputBox=new JCheckBox[co.length];
				controlOutputType=new int[co.length];
				for (int i=0; i<co.length; i++)
				{
					int w=Integer.parseInt(co[i].substring(0,co[i].indexOf(" ")));
					String n=co[i].substring(co[i].indexOf(" ")+1,co[i].length());
					String t=n.substring(0,n.indexOf(" "));
					n=n.substring(n.indexOf(" ")+1,n.length());
					controlOutputName[i]=new JLabel(n);
					controlOutputName[i].setBounds(GAPWIDTH+(LABELWIDTH+TEXTWIDTH)*ci.length+GAPWIDTH*2+(LABELWIDTH+TEXTWIDTH)*i+TEXTWIDTH,row*ROWHEIGHT+1,LABELWIDTH-3,ROWHEIGHT-2);
					computer.controlBuilder.guiComponent.add(controlOutputName[i]);
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
					controlOutputListPane[i].setBounds(field1width()+GAPWIDTH+(LABELWIDTH+TEXTWIDTH)*i,row*ROWHEIGHT+1,TEXTWIDTH-3,ROWHEIGHT-2);
					computer.controlBuilder.guiComponent.add(controlOutputListPane[i]);
				}
			}
		}

		public void remove()
		{
			for (int i=0; i<controlInputListPane.length; i++)
				computer.controlBuilder.guiComponent.remove(controlInputListPane[i]);
			for (int i=0; i<controlOutputListPane.length; i++)
				computer.controlBuilder.guiComponent.remove(controlOutputListPane[i]);
			for (int i=0; i<controlInputName.length; i++)
				computer.controlBuilder.guiComponent.remove(controlInputName[i]);
			for (int i=0; i<controlOutputName.length; i++)
				computer.controlBuilder.guiComponent.remove(controlOutputName[i]);
		}

		private void setComponentRow()
		{
			if (computer.datapathBuilder==null) return;
			for (int i=0; i<controlInputList.length; i++)
				controlInputListPane[i].setBounds(GAPWIDTH+(LABELWIDTH+TEXTWIDTH)*i,row*ROWHEIGHT+1,TEXTWIDTH-3,ROWHEIGHT-2);
			for (int i=0; i<controlInputName.length; i++)
				controlInputName[i].setBounds(GAPWIDTH+(LABELWIDTH+TEXTWIDTH)*i+TEXTWIDTH,row*ROWHEIGHT+1,LABELWIDTH-3,ROWHEIGHT-2);
			for (int i=0; i<controlOutputListPane.length; i++)
				controlOutputListPane[i].setBounds(field1width()+GAPWIDTH+(LABELWIDTH+TEXTWIDTH)*i,row*ROWHEIGHT+1,TEXTWIDTH-3,ROWHEIGHT-2);
			for (int i=0; i<controlOutputName.length; i++)
				controlOutputName[i].setBounds(field1width()+GAPWIDTH+(LABELWIDTH+TEXTWIDTH)*i+TEXTWIDTH,row*ROWHEIGHT+1,LABELWIDTH-3,ROWHEIGHT-2);
		}

		public void doPaint(Graphics g, int row)
		{
			this.row=row;
			setComponentRow();
			if (row==selectedRow)
				g.setColor(new Color(205,205,100));
			else if (highlightedRows[row])
				g.setColor(new Color(255,205,100));
			else
				g.setColor(new Color(255,255,150));
			g.fillRect(0,row*ROWHEIGHT,width(),ROWHEIGHT);
		}

		public boolean isActive()
		{
			if (computer.datapathBuilder==null) return false;

			for (int i=0; i<controlInputList.length; i++)
			{
				String s=(String)controlInputList[i].getSelectedValue();
				if (s==null || s.equals("") || s.equals("X"))
					continue;
				
				DatapathBuilder.Block b=computer.datapathBuilder.first;
				while(b!=null && !b.name.equals(controlInputName[i].getText()))
					b=b.next;
				if (Long.toHexString(b.getValue()).equals(s))
					continue;
				return false;
			}
			return true;
		}

		public void doInstruction()
		{
			if (computer.datapathBuilder==null) return;

			for (int i=0; i<controlOutputList.length; i++)
			{
				String s=null;
				if (controlOutputList[i]!=null)
				{
					s=(String)controlOutputList[i].getSelectedValue();
					if (s==null || s.equals(""))
						s="";
				}

				DatapathBuilder.Block b=computer.datapathBuilder.first;
				while(b!=null && !b.name.equals(controlOutputName[i].getText()))
					b=b.next;
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

	public class NextState extends ControlRow
	{
		public String state;
		public int row;

		public JList[] controlInputList;
		public JScrollPane[] controlInputListPane;
		public JLabel[] controlInputName;
		private JLabel nextStateLabel;

		public NextState(String name)
		{
			super(name,3);
			this.state=name;
			if (computer.datapathBuilder!=null)
			{
				String[] ci=computer.datapathBuilder.controlOutputs();
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
					computer.controlBuilder.guiComponent.add(controlInputName[i]);
					computer.controlBuilder.guiComponent.add(controlInputListPane[i]);
				}
			}
			nextStateLabel=new JLabel("NEXT STATE: "+name);
			if (controlInputListPane!=null)
				nextStateLabel.setBounds(GAPWIDTH+field1width(),row*ROWHEIGHT+1,field2width()-GAPWIDTH*2,ROWHEIGHT-2);
			computer.controlBuilder.guiComponent.add(nextStateLabel);
		}

		public void remove()
		{
			for (int i=0; i<controlInputListPane.length; i++)
				computer.controlBuilder.guiComponent.remove(controlInputListPane[i]);
			for (int i=0; i<controlInputName.length; i++)
				computer.controlBuilder.guiComponent.remove(controlInputName[i]);
			computer.controlBuilder.guiComponent.remove(nextStateLabel);
		}

		public boolean isActive()
		{
			if (computer.datapathBuilder==null) return false;

			for (int i=0; i<controlInputList.length; i++)
			{
				String s=(String)controlInputList[i].getSelectedValue();
				if (s==null || s.equals("") || s.equals("X"))
					continue;
				
				DatapathBuilder.Block b=computer.datapathBuilder.first;
				while(b!=null && !b.name.equals(controlInputName[i].getText()))
					b=b.next;
				if (Long.toHexString(b.value).equals(s))
					continue;
				return false;
			}
			return true;
		}

		private void setComponentRow()
		{
			if (computer.datapathBuilder==null) return;
			for (int i=0; i<controlInputList.length; i++)
				controlInputListPane[i].setBounds(GAPWIDTH+(LABELWIDTH+TEXTWIDTH)*i,row*ROWHEIGHT+1,TEXTWIDTH-3,ROWHEIGHT-2);
			for (int i=0; i<controlInputName.length; i++)
				controlInputName[i].setBounds(GAPWIDTH+(LABELWIDTH+TEXTWIDTH)*i+TEXTWIDTH,row*ROWHEIGHT+1,LABELWIDTH-3,ROWHEIGHT-2);
			nextStateLabel.setBounds(GAPWIDTH+field1width(),row*ROWHEIGHT+1,field2width()-GAPWIDTH*2,ROWHEIGHT-2);
		}

		public void doPaint(Graphics g, int row)
		{
			this.row=row;
			setComponentRow();
			if (row==selectedRow)
				g.setColor(new Color(100,205,100));
			else if (highlightedRows[row])
				g.setColor(new Color(150,205,100));
			else
				g.setColor(new Color(150,255,150));
			g.fillRect(0,row*ROWHEIGHT,width(),ROWHEIGHT);
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
			x+="<nextstatename>"+state+"</nextstatename>\n";
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
					state=elements[i+1];
					nextStateLabel.setText("NEXT STATE: "+state);
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

	public class Vector
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

	private class ControlControl extends AbstractGUI
	{
		private static final int WIDTH=300,HEIGHT=400;
		public JTextField microField,controlPathField,stateField,nextStateField;

		public ControlControl(Computer computer)
		{
			super(computer,"Control Builder Control",WIDTH,HEIGHT,false,true,false,false);
			refresh();
		}
		public void constructGUI(GUIComponent g)
		{
			JButton button = new JButton("Close");
			button.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e)
			{
				frame.setVisible(false);
				computer.controlBuilder.frame.setVisible(false);
				if (computer.computerGUI.singleFrame)
				{
					computer.computerGUI.removeComponent(computer.controlBuilder.controlControl);
					computer.computerGUI.removeComponent(computer.controlBuilder);
				}
			} } );
			button.setBounds(10+200,20+30*0,90,20);
			g.add(button);
			button = new JButton("Save");
			button.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e)
			{
				System.out.println(getXML());
				JFileChooser fc = new JFileChooser();
				fc.setCurrentDirectory(new File("."));
				fc.showSaveDialog(null);
				File f = fc.getSelectedFile();
				if (f==null) return;
				String name=f.getAbsolutePath();
				try
				{
					PrintWriter p =new PrintWriter(name);
					p.println(getXML());
					p.close();
				}
				catch(IOException x)
				{
					System.out.println("Error creating file "+name);
				}
			} } );
			button.setBounds(10,20+30*0,90,20);
			g.add(button);
			button = new JButton("Load");
			button.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e)
			{
				JFileChooser fc = new JFileChooser();
				fc.setCurrentDirectory(new File("."));
				fc.showOpenDialog(null);
				File f = fc.getSelectedFile();
				if (f==null) return;
				String name=f.getAbsolutePath();
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

				ControlXMLParse xmlParse=new ControlXMLParse(xml);
				parseXML(xmlParse.xmlParts);
				computer.controlBuilder.guiComponent.revalidate();
				computer.controlBuilder.repaint();
			} } );
			button.setBounds(10+100,20+30*0,90,20);
			g.add(button);

			button = new JButton("New Control Path");
			button.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e)
			{
				if (selectedRow==-1)
					controlPaths.add(new ControlPath(controlPathField.getText()));
				else
				{
					if(getSelectedVector().lastElement().type==0)
						getSelectedVector().addAfter(new ControlPath(controlPathField.getText()),getSelectedIndex());
				}
				computer.controlBuilder.guiComponent.revalidate();
				computer.controlBuilder.repaint();
				controlPathField.setText(increment(controlPathField.getText()));
			} } );
			button.setBounds(10,20+30*1,180,20);
			g.add(button);
			controlPathField=new JTextField("0");
			controlPathField.setBounds(10+190,20+30*1,220,20);
			g.add(controlPathField);
			button = new JButton("New State");
			button.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e)
			{
				if (selectedRow==-1)
					((ControlPath)controlPaths.lastElement()).controlStates.add(new ControlState(stateField.getText()));
				else if(getSelectedVector().lastElement().type==1)
					getSelectedVector().addAfter(new ControlState(stateField.getText()),getSelectedIndex());
				else if (getSelectedVector().lastElement().type==0)
					((ControlPath)getSelectedVector().elementAt(getSelectedIndex())).controlStates.addFirst(new ControlState(stateField.getText()));
				computer.controlBuilder.guiComponent.revalidate();
				computer.controlBuilder.repaint();
				stateField.setText(increment(stateField.getText()));
			} } );
			button.setBounds(10,20+30*2,180,20);
			g.add(button);
			stateField=new JTextField("0");
			stateField.setBounds(10+190,20+30*2,220,20);
			g.add(stateField);
			button = new JButton("Microcode");
			button.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e)
			{
				if (selectedRow==-1)
					((ControlState)((ControlPath)controlPaths.lastElement()).controlStates.lastElement()).controlInstructions.add(new ControlInstruction(microField.getText()));
				else if(getSelectedVector().lastElement().type==2)
					getSelectedVector().addAfter(new ControlInstruction(microField.getText()),getSelectedIndex());
				else if (getSelectedVector().lastElement().type==1)
					((ControlState)getSelectedVector().elementAt(getSelectedIndex())).controlInstructions.addFirst(new ControlInstruction(microField.getText()));
				computer.controlBuilder.guiComponent.revalidate();
				computer.controlBuilder.repaint();
			} } );
			button.setBounds(10,20+30*3,180,20);
			g.add(button);
			microField=new JTextField("");
			microField.setBounds(10+190,20+30*3,220,20);
			g.add(microField);
			button = new JButton("Next State");
			button.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e)
			{
				if (selectedRow==-1)
					((ControlState)((ControlPath)controlPaths.lastElement()).controlStates.lastElement()).nextStates.add(new NextState(nextStateField.getText()));
				else if(getSelectedVector().lastElement().type==3)
					getSelectedVector().addAfter(new NextState(nextStateField.getText()),getSelectedIndex());
				else if (getSelectedVector().lastElement().type==1)
					((ControlState)getSelectedVector().elementAt(getSelectedIndex())).nextStates.addFirst(new NextState(nextStateField.getText()));
				computer.controlBuilder.guiComponent.revalidate();
				computer.controlBuilder.repaint();
				nextStateField.setText(increment(nextStateField.getText()));
			} } );
			button.setBounds(10,20+30*4,180,20);
			g.add(button);
			nextStateField=new JTextField("1");
			nextStateField.setBounds(10+190,20+30*4,220,20);
			g.add(nextStateField);
			button = new JButton("Delete");
			button.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e)
			{
				if (selectedRow!=-1)
				{
					getSelectedVector().delete(getSelectedIndex());
					selectedRow=-1;
					computer.controlBuilder.guiComponent.revalidate();
					computer.controlBuilder.repaint();
				}
			} } );
			button.setBounds(10,20+30*5,120,20);
			g.add(button);
		}
		private String increment(String s)
		{
			String t,u;
			if (s.indexOf(" ")!=-1)
			{
				t = s.substring(0,s.indexOf(" "));
				u = s.substring(s.indexOf(" "),s.length());
			}
			else
			{
				return s;
			}
			return (Integer.parseInt(t)+1)+u;
		}
	}
	public class ControlXMLParse
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

			for (int i=0; i<parts.size(); i++)
				System.out.println(xmlParts[i]);
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
}
