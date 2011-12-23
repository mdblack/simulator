//Michael Black
//12/2011
//Handles a database of past operations

package simulator;

import java.util.ArrayList;

public class Trace 
{
	public static final int MAXTRACESIZE=1000;
	public static final String[] REGISTERNAMES=new String[]{"EIP","EAX","EBX","ECX","EDX","ESI","EDI","ESP","EBP","CR0","CR2","CR3","CR4"};
	public static final String[] SEGREGNAMES=new String[]{"CS","SS","DS","ES","FS","GS","IDTR","GDTR","LDTR","TSS"};
	public static final String[] FLAGNAMES=new String[]{"CARRY","PARITY","AUXCARRY","ZERO","SIGN","TRAP","INTERRUPTENABLE","DIRECTION","OVERFLOW","IOPRIVILEGE0","IOPRIVILEGE1","NESTEDTASK","ALIGNMENTCHECK","IDFLAG"};
	public static final int REGISTERCOUNT=REGISTERNAMES.length;
	public static final int SEGREGCOUNT=SEGREGNAMES.length;
	
	Computer computer;
	ArrayList<TraceEntry> tracebase;
	TraceEntry currentEntry=null;
	
	public Trace(Computer computer)
	{
		this.computer=computer;
		tracebase=new ArrayList<TraceEntry>();
	}
	
	public class TraceEntry
	{
		int instruction_count;
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
		tracebase.add(currentEntry);
		
		if (tracebase.size()>MAXTRACESIZE)
			tracebase.remove(0);
	}
	
	public void postInstructionByte(byte instbyte)
	{
		if (currentEntry==null) return;
		currentEntry.instructionBytes.add(Integer.toHexString(instbyte&0xff));
	}
	
	public void printTraceEntry(int icount)
	{ if (0==0) return;
		for (TraceEntry entry:tracebase)
		{
			if (entry==null) continue;
			if (entry.instruction_count!=icount) continue;
			
			System.out.print("InstructionNumber: "+entry.instruction_count+" ");
			System.out.print("Instruction: "+entry.instruction_name+" ");
			for (int r=0; r<entry.registers.length; r++)
				System.out.print(REGISTERNAMES[r]+": "+Integer.toHexString(entry.registers[r])+" ");
			for (int r=0; r<entry.segreg_values.length; r++)
				System.out.print(SEGREGNAMES[r]+": "+Integer.toHexString(entry.segreg_values[r])+" "+Integer.toHexString(entry.segreg_bases[r])+" "+Integer.toHexString(entry.segreg_limits[r])+" ");

			System.out.print(FLAGNAMES[0]+": "+((entry.flags&1)>>0)+" ");
			System.out.print(FLAGNAMES[1]+": "+((entry.flags&4)>>2)+" ");
			System.out.print(FLAGNAMES[2]+": "+((entry.flags&0x10)>>4)+" ");
			System.out.print(FLAGNAMES[3]+": "+((entry.flags&0x40)>>6)+" ");
			System.out.print(FLAGNAMES[4]+": "+((entry.flags&0x80)>>7)+" ");
			System.out.print(FLAGNAMES[5]+": "+((entry.flags&0x100)>>8)+" ");
			System.out.print(FLAGNAMES[6]+": "+((entry.flags&0x200)>>9)+" ");
			System.out.print(FLAGNAMES[7]+": "+((entry.flags&0x400)>>10)+" ");
			System.out.print(FLAGNAMES[8]+": "+((entry.flags&0x800)>>11)+" ");
			System.out.print(FLAGNAMES[9]+": "+((entry.flags&0x1000)>>12)+" ");
			System.out.print(FLAGNAMES[10]+": "+((entry.flags&0x2000)>>13)+" ");
			System.out.print(FLAGNAMES[11]+": "+((entry.flags&0x4000)>>14)+" ");
			System.out.print(FLAGNAMES[12]+": "+((entry.flags&0x40000)>>18)+" ");
			System.out.print(FLAGNAMES[13]+": "+((entry.flags&0x200000)>>21)+" ");
			
			System.out.print("Instruction bytes: ");
			for (String b:entry.instructionBytes)
				System.out.print(b+" ");
			System.out.print("Instruction codes: ");
			for (int i=0; i<entry.processorCodeName.size() && i<10; i++)
				System.out.print(entry.processorCodeName.get(i)+" ");
			for (int i=0; i<entry.processorCodeValue.size() && i<10; i++)
				System.out.print(entry.processorCodeValue.get(i)+" ");
			
			System.out.println();
		}
	}
}
