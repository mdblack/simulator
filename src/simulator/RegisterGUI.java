package simulator;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.ChangeListener;

import java.awt.event.*;

public class RegisterGUI extends AbstractGUI
{
	public static final int NAMEWIDTH=70,FIELDWIDTH=70,COMMENTWIDTH=300,ROWHEIGHT=20;

	public static final int EIP=0,EAX=1,EBX=2,ECX=3,EDX=4,ESP=5,EBP=6,ESI=7,EDI=8,CS=9,SS=10,DS=11,ES=12,FS=13,GS=14,CR0=15,CR2=16,CR3=17,IDTR=18,GDTR=19,LDTR=20,TSS=21;
	public static final int C=0,P=1,AC=2,Z=3,S=4,T=5,I=6,D=7,O=8;

	public static final String[] register_name = new String[] {
"EIP","EAX","EBX","ECX","EDX","ESP","EBP","ESI","EDI","CS","SS","DS","ES","FS","GS","CR0","CR2","CR3","IDTR","GDTR","LDTR","TSS"};
	public static final String[] flag_name = new String[] {
"carry","parity","aux carry","zero","sign","trap","interrupt","direction","overflow"};

	private JTextField[] regField;
	private JLabel[] regComment,regName,flagName;
	private JCheckBox[] flagBox;
	private JLabel inst;
	private JTextField intText;
	
	private String[] oldValues;

	public RegisterGUI(Computer computer)
	{
		super(computer,"Registers",600,600,false,true,true,false);

		regField=new JTextField[22];
		regName=new JLabel[22];
		regComment=new JLabel[22];
		flagBox=new JCheckBox[9];
		flagName=new JLabel[9];
		oldValues=new String[22];

		for (int i=0; i<22; i++)
		{
			regName[i]=new JLabel();
			regField[i]=new JTextField();
			regComment[i]=new JLabel();

			regName[i].setText(register_name[i]);
			regComment[i].setText("");
			final int r=i;
			regField[i].addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){writeRegister(r);}});
		}
		for (int i=0; i<9; i++)
		{
			flagBox[i]=new JCheckBox();
			flagName[i]=new JLabel();
			flagBox[i].setSelected(false);
			flagName[i].setText(flag_name[i]);
			final int r=i;
			flagBox[i].addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){writeFlag(r);}});
		}
		inst=new JLabel();

		refresh();
	}
	
	public void closeGUI()
	{
		computer.registerGUI=null;
	}

	public int width()
	{
		return 10+NAMEWIDTH+10+FIELDWIDTH+10+COMMENTWIDTH+100;
	}

	public int height()
	{
		return ROWHEIGHT*(22+2+4);
	}

	public void writeRegister(int i)
	{
		switch(i)
		{
		case(EIP): computer.processor.eip.setValue((int)Long.parseLong(regField[EIP].getText(),16)); break;
		case(EAX): computer.processor.eax.setValue((int)Long.parseLong(regField[EAX].getText(),16)); break;
		case(EBX): computer.processor.ebx.setValue((int)Long.parseLong(regField[EBX].getText(),16)); break;
		case(ECX): computer.processor.ecx.setValue((int)Long.parseLong(regField[ECX].getText(),16)); break;
		case(EDX): computer.processor.edx.setValue((int)Long.parseLong(regField[EDX].getText(),16)); break;
		case(ESI): computer.processor.esi.setValue((int)Long.parseLong(regField[ESI].getText(),16)); break;
		case(EDI): computer.processor.edi.setValue((int)Long.parseLong(regField[EDI].getText(),16)); break;
		case(ESP): computer.processor.esp.setValue((int)Long.parseLong(regField[ESP].getText(),16)); break;
		case(EBP): computer.processor.ebp.setValue((int)Long.parseLong(regField[EBP].getText(),16)); break;
		case(CR0): computer.processor.setCR0((int)Long.parseLong(regField[CR0].getText(),16)); break;
		case(CR2): computer.processor.setCR2((int)Long.parseLong(regField[CR2].getText(),16)); break;
		case(CR3): computer.processor.setCR3((int)Long.parseLong(regField[CR3].getText(),16)); break;
		case(CS): computer.processor.cs.setDescriptorValue((int)Long.parseLong(regField[CS].getText(),16)); break;
		case(SS): computer.processor.ss.setDescriptorValue((int)Long.parseLong(regField[SS].getText(),16)); break;
		case(DS): computer.processor.ds.setDescriptorValue((int)Long.parseLong(regField[DS].getText(),16)); break;
		case(ES): computer.processor.es.setDescriptorValue((int)Long.parseLong(regField[ES].getText(),16)); break;
		case(FS): computer.processor.fs.setDescriptorValue((int)Long.parseLong(regField[FS].getText(),16)); break;
		case(GS): computer.processor.gs.setDescriptorValue((int)Long.parseLong(regField[GS].getText(),16)); break;
		case(IDTR): if (computer.processor.idtr!=null) computer.processor.idtr.setDescriptorValue((int)Long.parseLong(regField[IDTR].getText(),16)); break;
		case(GDTR): if (computer.processor.gdtr!=null) computer.processor.gdtr.setDescriptorValue((int)Long.parseLong(regField[GDTR].getText(),16)); break;
		case(LDTR): if (computer.processor.ldtr!=null) computer.processor.ldtr.setDescriptorValue((int)Long.parseLong(regField[LDTR].getText(),16)); break;
		case(TSS): if (computer.processor.tss!=null) computer.processor.tss.setDescriptorValue((int)Long.parseLong(regField[TSS].getText(),16)); break;
		}
	}
	public void writeFlag(int i)
	{
		switch(i)
		{
		case(C): computer.processor.carry.set(flagBox[C].isSelected()); break;
		case(P): computer.processor.parity.set(flagBox[P].isSelected()); break;
		case(AC): computer.processor.auxiliaryCarry.set(flagBox[AC].isSelected()); break;
		case(Z): computer.processor.zero.set(flagBox[Z].isSelected()); break;
		case(S): computer.processor.sign.set(flagBox[S].isSelected()); break;
		case(T): computer.processor.trap.set(flagBox[T].isSelected()); break;
		case(I): computer.processor.interruptEnable.set(flagBox[I].isSelected()); break;
		case(D): computer.processor.direction.set(flagBox[D].isSelected()); break;
		case(O): computer.processor.overflow.set(flagBox[O].isSelected()); break;
		}
	}

	public void readRegisters()
	{
		if (!computer.updateGUIOnPlay && !computer.debugMode) return;

		regField[EIP].setText(""+Integer.toHexString(computer.processor.eip.getValue()));
		regField[EAX].setText(""+Integer.toHexString(computer.processor.eax.getValue()));
		regField[EBX].setText(""+Integer.toHexString(computer.processor.ebx.getValue()));
		regField[ECX].setText(""+Integer.toHexString(computer.processor.ecx.getValue()));
		regField[EDX].setText(""+Integer.toHexString(computer.processor.edx.getValue()));
		regField[ESP].setText(""+Integer.toHexString(computer.processor.esp.getValue()));
		regField[EBP].setText(""+Integer.toHexString(computer.processor.ebp.getValue()));
		regField[ESI].setText(""+Integer.toHexString(computer.processor.esi.getValue()));
		regField[EDI].setText(""+Integer.toHexString(computer.processor.edi.getValue()));
		regField[CR0].setText(""+Integer.toHexString(computer.processor.cr0.getValue()));
		regField[CR2].setText(""+Integer.toHexString(computer.processor.cr2.getValue()));
		regField[CR3].setText(""+Integer.toHexString(computer.processor.cr3.getValue()));
		regField[CS].setText(""+Integer.toHexString(computer.processor.cs.getValue()));
		regField[SS].setText(""+Integer.toHexString(computer.processor.ss.getValue()));
		regField[DS].setText(""+Integer.toHexString(computer.processor.ds.getValue()));
		regField[ES].setText(""+Integer.toHexString(computer.processor.es.getValue()));
		regField[FS].setText(""+Integer.toHexString(computer.processor.fs.getValue()));
		regField[GS].setText(""+Integer.toHexString(computer.processor.gs.getValue()));
		if (computer.processor.idtr!=null) regField[IDTR].setText(""+Integer.toHexString(computer.processor.idtr.getValue()));
		if (computer.processor.gdtr!=null) regField[GDTR].setText(""+Integer.toHexString(computer.processor.gdtr.getValue()));
		if (computer.processor.tss!=null) regField[TSS].setText(""+Integer.toHexString(computer.processor.tss.getValue()));
		if (computer.processor.ldtr!=null) regField[LDTR].setText(""+Integer.toHexString(computer.processor.ldtr.getValue()));
		
		for (int r=0; r<regField.length; r++)
			oldValues[r]=regField[r].getText();

		int csbase=computer.processor.cs.getBase();
		int cslimit=computer.processor.cs.getLimit();
		regComment[CS].setText("Base: "+Integer.toHexString(csbase)+", Limit: "+Integer.toHexString(cslimit));
		int ssbase=computer.processor.ss.getBase();
		int sslimit=computer.processor.ss.getLimit();
		regComment[SS].setText("Base: "+Integer.toHexString(ssbase)+", Limit: "+Integer.toHexString(sslimit));
		regComment[DS].setText("Base: "+Integer.toHexString(computer.processor.ds.getBase())+", Limit: "+Integer.toHexString(computer.processor.ds.getLimit()));
		regComment[ES].setText("Base: "+Integer.toHexString(computer.processor.es.getBase())+", Limit: "+Integer.toHexString(computer.processor.es.getLimit()));
		regComment[FS].setText("Base: "+Integer.toHexString(computer.processor.fs.getBase())+", Limit: "+Integer.toHexString(computer.processor.fs.getLimit()));
		regComment[GS].setText("Base: "+Integer.toHexString(computer.processor.gs.getBase())+", Limit: "+Integer.toHexString(computer.processor.gs.getLimit()));
		if (computer.processor.idtr!=null) regComment[IDTR].setText("Base: "+Integer.toHexString(computer.processor.idtr.getBase())+", Limit: "+Integer.toHexString(computer.processor.idtr.getLimit()));
		if (computer.processor.ldtr!=null) regComment[LDTR].setText("Base: "+Integer.toHexString(computer.processor.ldtr.getBase())+", Limit: "+Integer.toHexString(computer.processor.ldtr.getLimit()));
		if (computer.processor.tss!=null) regComment[TSS].setText("Base: "+Integer.toHexString(computer.processor.tss.getBase())+", Limit: "+Integer.toHexString(computer.processor.tss.getLimit()));
		if (computer.processor.gdtr!=null) regComment[GDTR].setText("Base: "+Integer.toHexString(computer.processor.gdtr.getBase())+", Limit: "+Integer.toHexString(computer.processor.gdtr.getLimit()));

		regComment[EIP].setText("Next instruction: "+Integer.toHexString(csbase+computer.processor.eip.getValue()));
		regComment[ESP].setText("Top of the stack: "+Integer.toHexString(ssbase+computer.processor.esp.getValue()));
		if ((computer.processor.cr0.getValue()&1)==0)
			regComment[CR0].setText("Real Mode");
		else
			regComment[CR0].setText("Protected Mode");

		flagBox[C].setSelected(computer.processor.carry.read());
		flagBox[P].setSelected(computer.processor.parity.read());
		flagBox[AC].setSelected(computer.processor.auxiliaryCarry.read());
		flagBox[Z].setSelected(computer.processor.zero.read());
		flagBox[S].setSelected(computer.processor.sign.read());
		flagBox[T].setSelected(computer.processor.trap.read());
		flagBox[I].setSelected(computer.processor.interruptEnable.read());
		flagBox[D].setSelected(computer.processor.direction.read());
		flagBox[O].setSelected(computer.processor.overflow.read());

		if(computer.processor.processorGUICode!=null) inst.setText(computer.processor.processorGUICode.constructName());

		repaint();
	}

	public void constructGUI(AbstractGUI.GUIComponent guicomponent)
	{
		inst.setBounds(10,1,COMMENTWIDTH,ROWHEIGHT-2);
		guicomponent.add(inst);

		for (int i=0; i<22; i++)
		{
			regName[i].setBounds(10,(i+1)*ROWHEIGHT+1,NAMEWIDTH,ROWHEIGHT-2);
			guicomponent.add(regName[i]);
			regField[i].setBounds(10+NAMEWIDTH+10,(i+1)*ROWHEIGHT+1,FIELDWIDTH,ROWHEIGHT-2);
			guicomponent.add(regField[i]);
			if (i==EIP || i==CS || i==SS || i==DS || i==ES || i==FS || i==GS || i==ESP || i==CR0)
			{
				regComment[i].setBounds(10+NAMEWIDTH+10+FIELDWIDTH+10,(i+1)*ROWHEIGHT+1,COMMENTWIDTH,ROWHEIGHT-2);
				guicomponent.add(regComment[i]);
			}
		}
		for (int i=0; i<9; i++)
		{
			flagName[i].setBounds(10+(NAMEWIDTH+10+ROWHEIGHT+10)*(i%5)+ROWHEIGHT+10,ROWHEIGHT*(23+(i<5?0:1))+1,NAMEWIDTH,ROWHEIGHT-2);
			guicomponent.add(flagName[i]);
			flagBox[i].setBounds(10+(NAMEWIDTH+10+ROWHEIGHT+10)*(i%5),ROWHEIGHT*(23+(i<5?0:1))+1,ROWHEIGHT-2,ROWHEIGHT-2);
			guicomponent.add(flagBox[i]);
		}
		JButton change = new JButton("Change values");
		change.setBounds(canvasX/2-90,ROWHEIGHT*26,180,ROWHEIGHT);
		change.addActionListener(new ButtonListener());
		guicomponent.add(change);


		JButton intr=new JButton("Interrupt");
		intr.setBounds(canvasX/4-90,ROWHEIGHT*28,180,ROWHEIGHT);
		intr.addActionListener(new ButtonListener());
		guicomponent.add(intr);

		intr=new JButton("Push Regs");
		intr.setBounds(canvasX/4-90,ROWHEIGHT*29,180,ROWHEIGHT);
		intr.addActionListener(new ButtonListener());
		guicomponent.add(intr);
		
		intText=new JTextField("0");
		intText.setBounds(2*canvasX/4-20,ROWHEIGHT*28,40,ROWHEIGHT);
		guicomponent.add(intText);

		JButton intrr=new JButton("Interrupt Return");
		intrr.setBounds(3*canvasX/4-90,ROWHEIGHT*28,180,ROWHEIGHT);
		intrr.addActionListener(new ButtonListener());
		guicomponent.add(intrr);

		intrr=new JButton("Pop Regs");
		intrr.setBounds(3*canvasX/4-90,ROWHEIGHT*29,180,ROWHEIGHT);
		intrr.addActionListener(new ButtonListener());
		guicomponent.add(intrr);

		
		readRegisters();
	}

	public void doPaint(Graphics g)
	{
		for (int i=0; i<22; i++)
		{
			if (i%2==0) g.setColor(new Color(200,200,200));
			else g.setColor(new Color(255,255,255));
			g.fillRect(0,ROWHEIGHT+i*ROWHEIGHT,canvasX,ROWHEIGHT);
		}
	}

	public class ButtonListener implements ActionListener
	{
		public void actionPerformed(ActionEvent e)
		{
			if (e.getActionCommand().equals("Change values"))
			{
				for (int i=0; i<oldValues.length; i++)
				{
					if (!oldValues[i].equals(regField[i].getText()))
						writeRegister(i);
				}
				for (int i=0; i<flagBox.length; i++)
					writeFlag(i);
				readRegisters();
			}
			else if (e.getActionCommand().equals("Interrupt"))
			{
				int interrupt=Integer.parseInt(intText.getText(),16);
				computer.processor.handleInterrupt(interrupt);
				readRegisters();
			}
			else if (e.getActionCommand().equals("Interrupt Return"))
			{
				computer.processor.iret(!computer.processor.isModeReal(),!computer.processor.isModeReal());
				readRegisters();
			}
			else if (e.getActionCommand().equals("Push Regs"))
			{
				computer.processor.pushad();
				readRegisters();
			}
			else if (e.getActionCommand().equals("Pop Regs"))
			{
				computer.processor.popad();
				readRegisters();
			}
		}
	}
}
