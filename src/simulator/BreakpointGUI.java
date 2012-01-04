package simulator;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import javax.swing.event.*;

public class BreakpointGUI extends AbstractGUI
{
	public static final int BREAKPOINT_NUMBER=10;
	public static final int ROWHEIGHT=20;
	public static final int BUTTONWIDTH=120;
	public static final int FIELDWIDTH=100;

	JList[] typeBox, entityBox, comparisonBox;
	JTextField[] entityField, numberField;
	JCheckBox[] activeBox;

	String equation="";

	static final String[] TYPES = new String[] {"inst count","register","flag","memory","port","interrupt"};
	static final String[][] ENTITIES = new String[][] {new String[]{},new String[]{"EIP","EAX","EBX","ECX","EDX","ESP","EBP","ESI","EDI","CS","SS","DS","ES","FS","GS","CR0","CR2","CR3","IDTR","GDTR","LDTR","TSS"},
new String[]{"carry","zero","sign","parity","overflow","auxcarry","direction","interrupt"},new String[]{},new String[]{},new String[]{}};

	static final String[][] COMPARISONS = new String[][] {
new String[]{"==","<",">",">=","<="},
new String[]{"changed","==","!=","<",">",">=","<="},
new String[]{"changed","set","clear"},
new String[]{"changed","==","!=","<",">",">=","<="},
new String[]{"changed","==","!=","<",">",">=","<="},
new String[]{},
new String[]{"==","<",">",">=","<="}};

	static final boolean[] ENTITYLISTPRESENT = new boolean[] {false,true,true,false,false,false};
	static final boolean[] ENTITYFIELDPRESENT = new boolean[] {false,false,false,true,true,true};
	static final boolean[] COMPARISONLISTPRESENT = new boolean[] {true,true,true,true,true,false};
	static final boolean[] NUMBERFIELDPRESENT = new boolean[] {true,true,false,true,true,false};

	JScrollPane[][] listpane;

	public BreakpointGUI(Computer computer)
	{
		super(computer, "Breakpoints",700,400,true,true,false,false);

		typeBox=new JList[BREAKPOINT_NUMBER];
		entityBox=new JList[BREAKPOINT_NUMBER];
		entityField=new JTextField[BREAKPOINT_NUMBER];
		comparisonBox=new JList[BREAKPOINT_NUMBER];
		numberField=new JTextField[BREAKPOINT_NUMBER];
		activeBox=new JCheckBox[BREAKPOINT_NUMBER];
		listpane=new JScrollPane[BREAKPOINT_NUMBER][3];

		refresh();
	}

	public BreakpointGUI(Computer computer, String initialEquation)
	{
		super(computer, "Breakpoints",700,400,true,true,false,false);

		typeBox=new JList[BREAKPOINT_NUMBER];
		entityBox=new JList[BREAKPOINT_NUMBER];
		entityField=new JTextField[BREAKPOINT_NUMBER];
		comparisonBox=new JList[BREAKPOINT_NUMBER];
		numberField=new JTextField[BREAKPOINT_NUMBER];
		activeBox=new JCheckBox[BREAKPOINT_NUMBER];
		listpane=new JScrollPane[BREAKPOINT_NUMBER][3];

		if (initialEquation.charAt(initialEquation.length()-1)!=' ')
			initialEquation+=" ";
		equation=initialEquation+". ";
		setStatusLabel(equation);

		refresh();
	}
	
	public void closeGUI()
	{
		computer.breakpointGUI=null;
	}

	public void editEquation()
	{
		String baseLabel="Enter breakpoint equation: ";
		guiComponent.requestFocus();
		statusEdit(baseLabel);
	}

	public void statusEdited(String initialEquation)
	{
		if (initialEquation.equals(""))
		{
			equation="";
			return;
		}
		if (initialEquation.charAt(initialEquation.length()-1)!=' ')
			initialEquation+=" ";
		equation=initialEquation+". ";
		setStatusLabel(equation);
	}

	public void setEquation(String eq)
	{
		equation=eq;
		setStatusLabel(equation);
	}

	public void constructGUI(AbstractGUI.GUIComponent guicomponent)
	{
		JButton setButton=new JButton("Set");
		JButton clearButton=new JButton("Clear");
		JButton editButton=new JButton("Equation");
		JButton closeButton=new JButton("Close");
		setButton.setBounds(10,(BREAKPOINT_NUMBER+1)*ROWHEIGHT+1,BUTTONWIDTH,ROWHEIGHT-2);
		clearButton.setBounds(10+BUTTONWIDTH+10,(BREAKPOINT_NUMBER+1)*ROWHEIGHT+1,BUTTONWIDTH,ROWHEIGHT-2);
		editButton.setBounds(10+2*(BUTTONWIDTH+10),(BREAKPOINT_NUMBER+1)*ROWHEIGHT+1,BUTTONWIDTH,ROWHEIGHT-2);
		closeButton.setBounds(10+3*(BUTTONWIDTH+10),(BREAKPOINT_NUMBER+1)*ROWHEIGHT+1,BUTTONWIDTH-40,ROWHEIGHT-2);
		setButton.addActionListener(new ButtonListener());
		clearButton.addActionListener(new ButtonListener());
		editButton.addActionListener(new ButtonListener());
		closeButton.addActionListener(new ButtonListener());
		guicomponent.add(setButton);
		guicomponent.add(clearButton);
		guicomponent.add(editButton);
		guicomponent.add(closeButton);

		for (int i=0; i<BREAKPOINT_NUMBER; i++)
		{
			activeBox[i]=new JCheckBox();
			activeBox[i].setBounds(10,i*ROWHEIGHT+1,ROWHEIGHT,ROWHEIGHT-2);
			guicomponent.add(activeBox[i]);

			typeBox[i]=new JList(TYPES);
			listpane[i][0]=new JScrollPane(typeBox[i]);
			listpane[i][0].setBounds(10+ROWHEIGHT+10,i*ROWHEIGHT+1,FIELDWIDTH,ROWHEIGHT-2);
			typeBox[i].addListSelectionListener(new BreakpointListSelectionListener(i));
			guicomponent.add(listpane[i][0]);

			entityBox[i]=new JList();
			listpane[i][1]=new JScrollPane(entityBox[i]);
			listpane[i][1].setBounds(10+ROWHEIGHT+10+FIELDWIDTH+10,i*ROWHEIGHT+1,FIELDWIDTH,ROWHEIGHT-2);
			guicomponent.add(listpane[i][1]);
			listpane[i][1].setVisible(false);

			entityField[i]=new JTextField();
			entityField[i].setBounds(10+ROWHEIGHT+10+FIELDWIDTH+10,i*ROWHEIGHT+1,FIELDWIDTH,ROWHEIGHT-2);
			guicomponent.add(entityField[i]);
			entityField[i].setVisible(false);

			comparisonBox[i]=new JList();
			listpane[i][2]=new JScrollPane(comparisonBox[i]);
			listpane[i][2].setBounds(10+ROWHEIGHT+10+(FIELDWIDTH+10)*2,i*ROWHEIGHT+1,FIELDWIDTH,ROWHEIGHT-2);
			guicomponent.add(listpane[i][2]);
			listpane[i][2].setVisible(false);

			numberField[i]=new JTextField();
			numberField[i].setBounds(10+ROWHEIGHT+10+(FIELDWIDTH+10)*3,i*ROWHEIGHT+1,FIELDWIDTH,ROWHEIGHT-2);
			guicomponent.add(numberField[i]);
			numberField[i].setVisible(false);
		}
		if (equation.equals("")) setStatusLabel("No breakpoint set");
	}

	public void doPaint(Graphics g)
	{
		for (int i=0; i<BREAKPOINT_NUMBER; i++)
		{
			if (i%2==0) g.setColor(Color.WHITE);
			else g.setColor(new Color(200,200,200));
			g.fillRect(0,i*ROWHEIGHT,width(),ROWHEIGHT);
		}
	}

	public int width()
	{
		return 10+ROWHEIGHT+10+(FIELDWIDTH+10)*4;
	}

	public int height()
	{
		return (BREAKPOINT_NUMBER+2)*ROWHEIGHT;
	}

	public void set()
	{
		String newequation="";
		for (int i=0; i<BREAKPOINT_NUMBER; i++)
		{
			if (!activeBox[i].isSelected()) continue;
			newequation+="( ";

			if (comparisonBox[i].getSelectedValue()!=null && (comparisonBox[i].getSelectedValue()).equals("changed"))
			{
				if (typeBox[i].getSelectedValue().equals("flag") && entityBox[i].getSelectedValue()!=null)
				{
					boolean v = readFlag((String)entityBox[i].getSelectedValue());
					comparisonBox[i].setSelectedIndex(v?2:1);
				}
				else if (typeBox[i].getSelectedValue().equals("register") && entityBox[i].getSelectedValue()!=null)
				{
					int v = readRegister((String)entityBox[i].getSelectedValue());
					comparisonBox[i].setSelectedIndex(2);
					numberField[i].setText(Integer.toHexString(v));
				}
				else if (typeBox[i].getSelectedValue().equals("memory") && !entityField[i].getText().equals(""))
				{
					int v = 0xff&computer.physicalMemory.getByte(Integer.parseInt(entityField[i].getText(),16));
					comparisonBox[i].setSelectedIndex(2);
					numberField[i].setText(Integer.toHexString(v));
				}
				else if (typeBox[i].getSelectedValue().equals("port") && !entityField[i].getText().equals(""))
				{
					int v = 0xff&computer.processor.ioports.ioPortReadByte(Integer.parseInt(entityField[i].getText(),16));
					comparisonBox[i].setSelectedIndex(2);
					numberField[i].setText(Integer.toHexString(v));
				}
			}

			if(typeBox[i].getSelectedValue().equals("register"))
			{
				if(entityBox[i].getSelectedValue()==null||comparisonBox[i].getSelectedValue()==null||numberField[i].getText().equals(""))
				{
					activeBox[i].setSelected(false);
					return;
				}
				newequation+="register "+(String)entityBox[i].getSelectedValue()+" ";
				newequation+=comparisonBox[i].getSelectedValue()+" ";
				newequation+=numberField[i].getText()+" ";
			}
			else if(typeBox[i].getSelectedValue().equals("flag"))
			{
				if(entityBox[i].getSelectedValue()==null||comparisonBox[i].getSelectedValue()==null)
				{
					activeBox[i].setSelected(false);
					return;
				}
				newequation+="flag "+(String)entityBox[i].getSelectedValue()+" ";
				newequation+=comparisonBox[i].getSelectedValue()+" ";
			}
			else if(typeBox[i].getSelectedValue().equals("memory"))
			{
				if(entityField[i].getText().equals("")||comparisonBox[i].getSelectedValue()==null||numberField[i].getText().equals(""))
				{
					activeBox[i].setSelected(false);
					return;
				}
				newequation+="memory[ "+entityField[i].getText()+" ] ";
				newequation+=comparisonBox[i].getSelectedValue()+" ";
				newequation+=numberField[i].getText()+" ";
			}
			else if(typeBox[i].getSelectedValue().equals("port"))
			{
				if(entityField[i].getText().equals("")||comparisonBox[i].getSelectedValue()==null||numberField[i].getText().equals(""))
				{
					activeBox[i].setSelected(false);
					return;
				}
				newequation+="port[ "+entityField[i].getText()+" ] ";
				newequation+=comparisonBox[i].getSelectedValue()+" ";
				newequation+=numberField[i].getText()+" ";
			}
			else if(typeBox[i].getSelectedValue().equals("interrupt"))
			{
				if(entityField[i].getText().equals(""))
				{
					activeBox[i].setSelected(false);
					return;
				}
				newequation+="interrupt "+entityField[i].getText()+" ";
			}
			else if(typeBox[i].getSelectedValue().equals("inst count"))
			{
				if(comparisonBox[i].getSelectedValue()==null||numberField[i].getText().equals(""))
				{
					activeBox[i].setSelected(false);
					return;
				}
				newequation+="instruction_count "+comparisonBox[i].getSelectedValue()+" ";
				newequation+=numberField[i].getText()+" ";
			}
			else if(typeBox[i].getSelectedValue().equals("instruction"))
			{
				if(comparisonBox[i].getSelectedValue()==null||numberField[i].getText().equals(""))
				{
					activeBox[i].setSelected(false);
					return;
				}
				newequation+="instruction_address "+comparisonBox[i].getSelectedValue()+" ";
				newequation+=numberField[i].getText()+" ";
			}

			newequation+=") ";
		}
		newequation+=". ";
		equation=newequation;
		setStatusLabel("Breakpoint: "+equation);
	}

	public boolean atBreakpoint()
	{
		if (equation.equals("")) return false;

		parsedEquation=equation;

		String element=getNextEquationElement();
		while(!element.equals("."))
		{
			element=getNextEquationElement();
			if (element.equals("register"))
			{
				String register=getNextEquationElement();
				String comparison=getNextEquationElement();
				String number=getNextEquationElement();
				int number1=readRegister(register);
				int number2=(int)(Long.parseLong(number,16)&0xffffffff);
//				int number2=Integer.parseInt(number,16);
				if (doComparison(comparison,number1,number2)) return true;
				element=getNextEquationElement();
			}
			else if (element.equals("flag"))
			{
				String flag=getNextEquationElement();
				String comparison=getNextEquationElement();
				boolean number=readFlag(flag);
				if (comparison.equals("set")&&number) return true;
				if (comparison.equals("clear")&&!number) return true;
				element=getNextEquationElement();
			}
			else if (element.equals("memory["))
			{
				String address=getNextEquationElement();
				getNextEquationElement(); // ] 
				String comparison=getNextEquationElement();
				String number=getNextEquationElement();
				int number1=0xff&computer.physicalMemory.getByte((int)Long.parseLong(address,16));
				int number2=0xff&(int)Long.parseLong(number,16);
				if (doComparison(comparison,number1,number2)) return true;
				element=getNextEquationElement();
			}
			else if (element.equals("port["))
			{
				String address=getNextEquationElement();
				getNextEquationElement(); // ] 
				String comparison=getNextEquationElement();
				String number=getNextEquationElement();
				int number1=0xff&computer.processor.ioports.ioPortReadByte(Integer.parseInt(address,16));
				int number2=0xff&(int)Long.parseLong(number,16);
				if (doComparison(comparison,number1,number2)) return true;
				element=getNextEquationElement();
			}
			else if (element.equals("instruction_count"))
			{
				String comparison=getNextEquationElement();
				String number=getNextEquationElement();
				int number1=computer.icount;
				int number2=Integer.parseInt(number);
				if (doComparison(comparison,number1,number2)) return true;
				element=getNextEquationElement();
			}
			else if (element.equals("instruction_address"))
			{
				String comparison=getNextEquationElement();
				String number=getNextEquationElement();
				int number1=computer.processor.cs.address(computer.processor.eip.getValue());
				int number2=Integer.parseInt(number,16);
				if (doComparison(comparison,number1,number2)) return true;
				element=getNextEquationElement();
			}
			else if (element.equals("interrupt"))
			{
				String number=getNextEquationElement();
				int number1=computer.processor.lastInterrupt;
				int number2=Integer.parseInt(number,16);
				if (number1==number2) return true;
				element=getNextEquationElement();
			}
			element=getNextEquationElement();	
		}
		return false;
	}

	private String parsedEquation="";

	private String getNextEquationElement()
	{
		int i=parsedEquation.indexOf(' ');
		String element=parsedEquation.substring(0,i);
		if (!element.equals("."))
			parsedEquation=parsedEquation.substring(i+1,parsedEquation.length());
		return element;
	}

	private boolean doComparison(String comparison, int number1, int number2)
	{
		if (comparison.equals("==")) return number1==number2;
		else if (comparison.equals("!=")) return number1!=number2;
		else if (comparison.equals(">")) return number1>number2;
		else if (comparison.equals("<")) return number1<number2;
		else if (comparison.equals(">=")) return number1>=number2;
		else if (comparison.equals("<=")) return number1<=number2;
		else if (comparison.equals("changed")) return number1!=number2;

		return false;
	}

	private int readRegister(String register)
	{
		if (register.equals("EIP")) return computer.processor.eip.getValue();
		else if (register.equals("EAX")) return computer.processor.eax.getValue();
		else if (register.equals("EBX")) return computer.processor.ebx.getValue();
		else if (register.equals("ECX")) return computer.processor.ecx.getValue();
		else if (register.equals("EDX")) return computer.processor.edx.getValue();
		else if (register.equals("ESI")) return computer.processor.esi.getValue();
		else if (register.equals("EDI")) return computer.processor.edi.getValue();
		else if (register.equals("ESP")) return computer.processor.esp.getValue();
		else if (register.equals("EBP")) return computer.processor.ebp.getValue();
		else if (register.equals("CR0")) return computer.processor.cr0.getValue();
		else if (register.equals("CR2")) return computer.processor.cr2.getValue();
		else if (register.equals("CR3")) return computer.processor.cr3.getValue();
		else if (register.equals("CS")) return computer.processor.cs.getValue();
		else if (register.equals("SS")) return computer.processor.ss.getValue();
		else if (register.equals("DS")) return computer.processor.ds.getValue();
		else if (register.equals("ES")) return computer.processor.es.getValue();
		else if (register.equals("FS")) return computer.processor.fs.getValue();
		else if (register.equals("GS")) return computer.processor.gs.getValue();
		else if (register.equals("IDTR")) return computer.processor.idtr.getValue();
		else if (register.equals("GDTR")) return computer.processor.gdtr.getValue();
		else if (register.equals("LDTR")) return computer.processor.ldtr.getValue();
		else if (register.equals("TSS")) return computer.processor.tss.getValue();
		else return 0;
	}

	private boolean readFlag(String flag)
	{
		if (flag.equals("carry")) return computer.processor.carry.read();
		else if (flag.equals("auxcarry")) return computer.processor.auxiliaryCarry.read();
		else if (flag.equals("sign")) return computer.processor.sign.read();
		else if (flag.equals("parity")) return computer.processor.parity.read();
		else if (flag.equals("zero")) return computer.processor.zero.read();
		else if (flag.equals("overflow")) return computer.processor.overflow.read();
		else if (flag.equals("direction")) return computer.processor.direction.read();
		else if (flag.equals("interrupt")) return computer.processor.interruptEnable.read();
		else return false;
	}

	public void clear()
	{
		for (int i=0; i<BREAKPOINT_NUMBER; i++)
			activeBox[i].setSelected(false);
		equation="";
		setStatusLabel("No breakpoint set");
	}

	public class BreakpointListSelectionListener implements ListSelectionListener
	{
		int row;
		public BreakpointListSelectionListener(int row)
		{
			this.row=row;
		}
		public void valueChanged(ListSelectionEvent e)
		{
			activeBox[row].setSelected(true);
			int selection=typeBox[row].getSelectedIndex();
			if(selection==-1) return;
			entityBox[row].setListData(ENTITIES[selection]);
			comparisonBox[row].setListData(COMPARISONS[selection]);

			listpane[row][1].setVisible(ENTITYLISTPRESENT[selection]);
			entityField[row].setVisible(ENTITYFIELDPRESENT[selection]);
			listpane[row][2].setVisible(COMPARISONLISTPRESENT[selection]);
			numberField[row].setVisible(NUMBERFIELDPRESENT[selection]);
			
			entityBox[row].setSelectedIndex(0);
			comparisonBox[row].setSelectedIndex(0);
			numberField[row].setText("0");
			entityField[row].setText("0");
		}
	}

	public class ButtonListener implements ActionListener
	{
		public void actionPerformed(ActionEvent e)
		{
			if (e.getActionCommand().equals("Set"))
				set();
			else if (e.getActionCommand().equals("Clear"))
				clear();
			else if (e.getActionCommand().equals("Equation"))
				editEquation();
			else if (e.getActionCommand().equals("Close"))
				close();
		}
	}
}
