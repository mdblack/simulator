package simulator;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

public class RegisterGUI extends AbstractGUI
{
	public static final int NAMEWIDTH=70,FIELDWIDTH=70,COMMENTWIDTH=300,ROWHEIGHT=20;

	public static final int EIP=0,EAX=1,EBX=2,ECX=3,EDX=4,ESP=5,EBP=6,ESI=7,EDI=8,CS=9,SS=10,DS=11,ES=12,FS=13,GS=14,CR0=15,CR2=16,CR3=17,IDTR=18,GDTR=19,LDTR=20,TSS=21;
	public static final int C=0,P=1,AC=2,Z=3,S=3,T=4,I=5,D=6,O=7;

	public static final String[] register_name = new String[] {
"EIP","EAX","EBX","ECX","EDX","ESP","EBP","ESI","EDI","CS","SS","DS","ES","FS","GS","CR0","CR2","CR3","IDTR","GDTR","LDTR","TSS"};
	public static final String[] flag_name = new String[] {
"carry","parity","aux carry","zero","sign","trap","interrupt","direction","overflow"};

	private JTextField[] regField;
	private JLabel[] regComment,regName,flagName;
	private JCheckBox[] flagBox;
	private JLabel inst;

	public RegisterGUI(Computer computer)
	{
		super(computer,"Registers",600,600,false,true,true,false);

		regField=new JTextField[22];
		regName=new JLabel[22];
		regComment=new JLabel[22];
		flagBox=new JCheckBox[9];
		flagName=new JLabel[9];

		for (int i=0; i<22; i++)
		{
			regName[i]=new JLabel();
			regField[i]=new JTextField();
			regComment[i]=new JLabel();

			regName[i].setText(register_name[i]);
			regComment[i].setText("");
		}
		for (int i=0; i<9; i++)
		{
			flagBox[i]=new JCheckBox();
			flagName[i]=new JLabel();
			flagBox[i].setSelected(false);
			flagName[i].setText(flag_name[i]);
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

	public void writeRegisters()
	{
		computer.processor.eip.setValue((int)Long.parseLong(regField[EIP].getText(),16));
		computer.processor.eax.setValue((int)Long.parseLong(regField[EAX].getText(),16));
		computer.processor.ebx.setValue((int)Long.parseLong(regField[EBX].getText(),16));
		computer.processor.ecx.setValue((int)Long.parseLong(regField[ECX].getText(),16));
		computer.processor.edx.setValue((int)Long.parseLong(regField[EDX].getText(),16));
		computer.processor.esi.setValue((int)Long.parseLong(regField[ESI].getText(),16));
		computer.processor.edi.setValue((int)Long.parseLong(regField[EDI].getText(),16));
		computer.processor.esp.setValue((int)Long.parseLong(regField[ESP].getText(),16));
		computer.processor.ebp.setValue((int)Long.parseLong(regField[EBP].getText(),16));
		computer.processor.cr0.setValue((int)Long.parseLong(regField[CR0].getText(),16));
		computer.processor.cr2.setValue((int)Long.parseLong(regField[CR2].getText(),16));
		computer.processor.cr3.setValue((int)Long.parseLong(regField[CR3].getText(),16));
		computer.processor.cs.setDescriptorValue((int)Long.parseLong(regField[CS].getText(),16));
		computer.processor.ss.setDescriptorValue((int)Long.parseLong(regField[SS].getText(),16));
		computer.processor.ds.setDescriptorValue((int)Long.parseLong(regField[DS].getText(),16));
		computer.processor.es.setDescriptorValue((int)Long.parseLong(regField[ES].getText(),16));
		computer.processor.fs.setDescriptorValue((int)Long.parseLong(regField[FS].getText(),16));
		computer.processor.gs.setDescriptorValue((int)Long.parseLong(regField[GS].getText(),16));
		if (computer.processor.idtr!=null) computer.processor.idtr.setDescriptorValue((int)Long.parseLong(regField[IDTR].getText(),16));
		if (computer.processor.gdtr!=null) computer.processor.gdtr.setDescriptorValue((int)Long.parseLong(regField[GDTR].getText(),16));
		if (computer.processor.ldtr!=null) computer.processor.ldtr.setDescriptorValue((int)Long.parseLong(regField[LDTR].getText(),16));
		if (computer.processor.tss!=null) computer.processor.tss.setDescriptorValue((int)Long.parseLong(regField[TSS].getText(),16));

		computer.processor.carry.set(flagBox[C].isSelected());
		computer.processor.parity.set(flagBox[P].isSelected());
		computer.processor.auxiliaryCarry.set(flagBox[AC].isSelected());
		computer.processor.zero.set(flagBox[Z].isSelected());
		computer.processor.sign.set(flagBox[S].isSelected());
		computer.processor.trap.set(flagBox[T].isSelected());
		computer.processor.interruptEnable.set(flagBox[I].isSelected());
		computer.processor.direction.set(flagBox[D].isSelected());
		computer.processor.overflow.set(flagBox[O].isSelected());
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
		change.setBounds(canvasX/2-75,ROWHEIGHT*26,150,ROWHEIGHT);
		change.addActionListener(new ButtonListener());
		guicomponent.add(change);

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
			writeRegisters();
		}
	}
}
