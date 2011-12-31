//Michael Black
//12/2011
//Handles a database of past operations

package simulator;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;

import javax.xml.stream.events.StartDocument;

public class Trace extends AbstractGUI
{
	public static final int MAXTRACESIZE=1000;
	public static final String[] REGISTERNAMES=new String[]{"EIP","EAX","EBX","ECX","EDX","ESI","EDI","ESP","EBP","CR0","CR2","CR3","CR4"};
	public static final String[] SEGREGNAMES=new String[]{"CS","SS","DS","ES","FS","GS","IDTR","GDTR","LDTR","TSS"};
	public static final String[] FLAGNAMES=new String[]{"CARRY","PARITY","AUXCARRY","ZERO","SIGN","TRAP","INTERRUPTENABLE","DIRECTION","OVERFLOW","IOPRIVILEGE0","IOPRIVILEGE1","NESTEDTASK","ALIGNMENTCHECK","IDFLAG"};
	public static final int REGISTERCOUNT=REGISTERNAMES.length;
	public static final int SEGREGCOUNT=SEGREGNAMES.length;
	
	ArrayList<TraceEntry> tracebase;
	TraceEntry currentEntry=null;
	int dumpcount=0;
	
	public static final int W=500,H=400,LINEHEIGHT=18;
	
	public Trace(Computer computer)
	{
		super(computer,"Trace",W,H,true,true,true,false);
		tracebase=new ArrayList<TraceEntry>();
		try{new BufferedWriter(new FileWriter("dump.txt",false)).close();}catch(Exception e){}
		refresh();
	}
	
	public void closeGUI()
	{
		dumpTraceToFile();
		computer.trace=null;
	}
	
	public int height()
	{
		return LINEHEIGHT*MAXTRACESIZE;
	}
	
	public void mouseClick(MouseEvent e)
	{
		int x=e.getX();
		int y=e.getY();
		if (x<0 || x>=width())
			return;
		if (y<0 || y>=height())
			return;

		int row=tracebase.size()-y/LINEHEIGHT-1;
		if (row<0) return;
		
		if (e.getButton()==MouseEvent.BUTTON3)
		{
			new MemoryBlockGUI(computer,MemoryBlockGUI.CODE,tracebase.get(row).instruction_address);
			dumpTrace();
		}
		else
		{
			String label="";
			TraceEntry entry=tracebase.get(row);
			for (int r=0; r<entry.registers.length; r++)
				label+=REGISTERNAMES[r]+": "+Integer.toHexString(entry.registers[r])+" ";
			for (int r=0; r<entry.segreg_values.length; r++)
				label+=SEGREGNAMES[r]+": "+Integer.toHexString(entry.segreg_values[r])+" "+Integer.toHexString(entry.segreg_bases[r])+" "+Integer.toHexString(entry.segreg_limits[r])+" ";
			label+="Flags: "+Integer.toBinaryString(entry.flags);

			setStatusLabel(label);
			dumpTrace(tracebase.get(row));
		}
	}
	
	public void doPaint(Graphics g)
	{
		int j=0;
		for (int i=tracebase.size()-1; i>=0; i--)
		{
			TraceEntry entry=null;
			try{entry=tracebase.get(i);}catch(IndexOutOfBoundsException e){ continue; }
			if (entry==null) continue;
				
			if (i%2==0)
				g.setColor(new Color(200,200,255));
			else
				g.setColor(Color.WHITE);
			g.fillRect(0, j*LINEHEIGHT, canvasX, LINEHEIGHT);
			g.setColor(Color.BLACK);
			g.drawString(""+entry.instruction_count, 10, (j+1)*LINEHEIGHT-3);
			g.drawString(Integer.toHexString(entry.instruction_address), 90, (j+1)*LINEHEIGHT-3);
			g.drawString(""+entry.instruction_name, 150, (j+1)*LINEHEIGHT-3);
			j++;
			
		}
	}
	
	public class TraceEntry
	{
		int instruction_count;
		int instruction_address;
		String instruction_name;
		ArrayList<Processor.GUICODE> processorCode;
		ArrayList<String> processorCodeName;
		ArrayList<String> processorCodeValue;
		ArrayList<String> instructionBytes;
		int[] registers;
		int flags;
		int[] segreg_values;
		int[] segreg_bases;
		int[] segreg_limits;
		
		public TraceEntry(int instruction_count)
		{
			this.instruction_count=instruction_count;
			registers=new int[REGISTERCOUNT];
			segreg_values=new int[SEGREGCOUNT];
			segreg_bases=new int[SEGREGCOUNT];
			segreg_limits=new int[SEGREGCOUNT];
			instructionBytes=new ArrayList<String>();
		}
	}
	
	public void addProcessorCode(Processor.ProcessorGUICode processorGUICode)
	{
		if (currentEntry==null) return;
		currentEntry.processorCode=processorGUICode.code;
		currentEntry.processorCodeName=processorGUICode.name;
		currentEntry.processorCodeValue=processorGUICode.value;
		currentEntry.instruction_name=processorGUICode.constructName();
	}

	public void addRegisters(Processor processor)
	{
		if (currentEntry==null) return;
		currentEntry.instruction_address=processor.cs.address(processor.eip.getValue());
		currentEntry.registers[0]=processor.eip.getValue();
		currentEntry.registers[1]=processor.eax.getValue();
		currentEntry.registers[2]=processor.ebx.getValue();
		currentEntry.registers[3]=processor.ecx.getValue();
		currentEntry.registers[4]=processor.edx.getValue();
		currentEntry.registers[5]=processor.esi.getValue();
		currentEntry.registers[6]=processor.edi.getValue();
		currentEntry.registers[7]=processor.esp.getValue();
		currentEntry.registers[8]=processor.ebp.getValue();
		currentEntry.registers[9]=processor.cr0.getValue();
		currentEntry.registers[10]=processor.cr2.getValue();
		currentEntry.registers[11]=processor.cr3.getValue();
		currentEntry.registers[12]=processor.cr4.getValue();
		currentEntry.flags=processor.getFlags();
		currentEntry.segreg_values[0]=processor.cs.getValue();
		currentEntry.segreg_bases[0]=processor.cs.getBase();
		currentEntry.segreg_limits[0]=processor.cs.getLimit();
		currentEntry.segreg_values[1]=processor.ss.getValue();
		currentEntry.segreg_bases[1]=processor.ss.getBase();
		currentEntry.segreg_limits[1]=processor.ss.getLimit();
		currentEntry.segreg_values[2]=processor.ds.getValue();
		currentEntry.segreg_bases[2]=processor.ds.getBase();
		currentEntry.segreg_limits[2]=processor.ds.getLimit();
		currentEntry.segreg_values[3]=processor.es.getValue();
		currentEntry.segreg_bases[3]=processor.es.getBase();
		currentEntry.segreg_limits[3]=processor.es.getLimit();
		currentEntry.segreg_values[4]=processor.fs.getValue();
		currentEntry.segreg_bases[4]=processor.fs.getBase();
		currentEntry.segreg_limits[4]=processor.fs.getLimit();
		currentEntry.segreg_values[5]=processor.gs.getValue();
		currentEntry.segreg_bases[5]=processor.gs.getBase();
		currentEntry.segreg_limits[5]=processor.gs.getLimit();
		currentEntry.segreg_values[6]=processor.idtr.getValue();
		currentEntry.segreg_bases[6]=processor.idtr.getBase();
		currentEntry.segreg_limits[6]=processor.idtr.getLimit();
		currentEntry.segreg_values[7]=processor.gdtr.getValue();
		currentEntry.segreg_bases[7]=processor.gdtr.getBase();
		currentEntry.segreg_limits[7]=processor.gdtr.getLimit();
		currentEntry.segreg_values[8]=processor.ldtr.getValue();
		currentEntry.segreg_bases[8]=processor.ldtr.getBase();
		currentEntry.segreg_limits[8]=processor.ldtr.getLimit();
		currentEntry.segreg_values[9]=processor.tss.getValue();
		currentEntry.segreg_bases[9]=processor.tss.getBase();
		currentEntry.segreg_limits[9]=processor.tss.getLimit();
	}
	
	public void newTraceEntry(int instructionCount)
	{
		currentEntry=new TraceEntry(instructionCount);
	}
	public void closeTraceEntry()
	{
		if (currentEntry==null) return;
		if (currentEntry.instruction_name==null) return;
		if (currentEntry.instruction_name.equals("null")) return;
		
		dumpcount++;
		if (dumpcount==MAXTRACESIZE)
		{
			dumpcount=0;
			dumpTraceToFile();
		}
		
		tracebase.add(currentEntry);
		
		if (tracebase.size()>MAXTRACESIZE)
			tracebase.remove(0);
		
		repaint();
	}
	
	public void postInstructionByte(byte instbyte)
	{
		if (currentEntry==null) return;
		currentEntry.instructionBytes.add(Integer.toHexString(instbyte&0xff));
	}
	
	public void dumpTrace()
	{
		for (TraceEntry entry:tracebase)
			dumpTrace(entry);
	}
	
	public synchronized void dumpTraceToFile()
	{
		BufferedWriter bw=null;
		try
		{
			bw=new BufferedWriter(new FileWriter("dump.txt",true));
			bw.append("Count: "+tracebase.get(0).instruction_count+"\n");
			for (TraceEntry entry:tracebase)
				bw.append(dumpTraceToStringShort(entry)+"\n");
			bw.flush();
		}
		catch(Exception e)
		{
			System.out.println("Couldn't dump to dump.txt");
			e.printStackTrace();
		}
		finally
		{
			if (bw!=null) try { bw.close(); } catch(Exception e){}
		}
	}
	
	public void dumpTrace(TraceEntry entry)
	{
		System.out.println(dumpTraceToString(entry));
	}
	
	public String dumpTraceToStringShort(TraceEntry entry)
	{
		if (entry==null) return "";
		String dump="";
		for (int r=0; r<entry.registers.length; r++)
			dump+=(Integer.toHexString(entry.registers[r])+" ");
		for (int r=0; r<entry.segreg_values.length; r++)
			dump+=(Integer.toHexString(entry.segreg_values[r])+" ");
		dump+=Integer.toBinaryString(entry.flags)+" ";
		for (String b:entry.instructionBytes)
			dump+=(b+" ");
		return dump;
	}
	
	public String dumpTraceToString(TraceEntry entry)
	{ 
//		for (TraceEntry entry:tracebase)
//		{
			if (entry==null) return "";
//			if (entry.instruction_count!=icount) continue;
			
			String dump="";
			
			dump+=entry.instruction_count+" ";
			dump+=entry.instruction_name+" ";
			for (int r=0; r<entry.registers.length; r++)
				dump+=(REGISTERNAMES[r]+": "+Integer.toHexString(entry.registers[r])+" ");
			for (int r=0; r<entry.segreg_values.length; r++)
				dump+=(SEGREGNAMES[r]+": "+Integer.toHexString(entry.segreg_values[r])+" "+Integer.toHexString(entry.segreg_bases[r])+" "+Integer.toHexString(entry.segreg_limits[r])+" ");
//			dump+="Flags: "+Integer.toBinaryString(entry.flags)+" ";

			dump+=(FLAGNAMES[0]+": "+((entry.flags&1)>>0)+" ");
			dump+=(FLAGNAMES[1]+": "+((entry.flags&4)>>2)+" ");
			dump+=(FLAGNAMES[2]+": "+((entry.flags&0x10)>>4)+" ");
			dump+=(FLAGNAMES[3]+": "+((entry.flags&0x40)>>6)+" ");
			dump+=(FLAGNAMES[4]+": "+((entry.flags&0x80)>>7)+" ");
			dump+=(FLAGNAMES[5]+": "+((entry.flags&0x100)>>8)+" ");
			dump+=(FLAGNAMES[6]+": "+((entry.flags&0x200)>>9)+" ");
			dump+=(FLAGNAMES[7]+": "+((entry.flags&0x400)>>10)+" ");
			dump+=(FLAGNAMES[8]+": "+((entry.flags&0x800)>>11)+" ");
			dump+=(FLAGNAMES[9]+": "+((entry.flags&0x1000)>>12)+" ");
			dump+=(FLAGNAMES[10]+": "+((entry.flags&0x2000)>>13)+" ");
			dump+=(FLAGNAMES[11]+": "+((entry.flags&0x4000)>>14)+" ");
			dump+=(FLAGNAMES[12]+": "+((entry.flags&0x40000)>>18)+" ");
			dump+=(FLAGNAMES[13]+": "+((entry.flags&0x200000)>>21)+" ");

			dump+="Instruction bytes: ";
			for (String b:entry.instructionBytes)
				dump+=(b+" ");
/*			dump+=("Instruction codes: ");
			for (int i=0; i<entry.processorCodeName.size() && i<10; i++)
				dump+=(entry.processorCodeName.get(i)+" ");
			for (int i=0; i<entry.processorCodeValue.size() && i<10; i++)
				dump+=(entry.processorCodeValue.get(i)+" ");*/
			
			return dump;
//		}
	}
}
