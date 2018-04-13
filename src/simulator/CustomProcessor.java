package simulator;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

public class CustomProcessor extends IODevice
{
	public Computer computer;
	public CustomProcessorModule defaultModule;
	public boolean interruptRaised=false;
	public long program_counter=0;
	public long opcode=0;
	
	public CustomProcessor(Computer computer)
	{
		this.computer=computer;
//		if (computer.datapathBuilder==null) return;
		
		if (computer.datapathBuilder==null)
		{
			computer.datapathBuilder=new DatapathBuilder(computer);
			computer.datapathBuilder.doload();
			computer.controlBuilder=new ControlBuilder(computer,computer.datapathBuilder.defaultModule);
			computer.controlBuilder.clear();
			computer.controlBuilder.doload();
		}
		
		if (computer.controlBuilder==null)
		{
			computer.controlBuilder=new ControlBuilder(computer,computer.datapathBuilder.defaultModule);
		}
		
		defaultModule=new CustomProcessorModule(computer,computer.datapathBuilder.defaultModule,computer.controlBuilder.defaultControl);
		
		computer.datapathBuilder.resetAll();

		computer.ioports.requestPorts(this,new int[]{0xff},"Switch Processor",new String[]{""});

		defaultModule.active=true;
		defaultModule.updateGUIs=true;
		
		while (computer.datapathBuilder.modules.size() > 0)
			computer.datapathBuilder.modules.remove(0);

		computer.datapathBuilder.modules.add(defaultModule);

		computer.datapathBuilder.repaint();
	}
	public void raiseInterrupt()
	{
		interruptRaised=true;
	}
	public void clearInterrupt()
	{
		interruptRaised=false;
	}
	public byte ioPortReadByte(int address)
	{
		return defaultModule.active? (byte)1:0;
	}
	public void ioPortWriteByte(int address, byte data)
	{
		if (data==0)
			defaultModule.active=false;
		else
			defaultModule.active=true;
	}
	public void doCycle()
	{
		for (CustomProcessorModule module: computer.datapathBuilder.modules)
			module.doCycle();
	}
	
	public static class CustomProcessorModule
	{
		public boolean active=false;
		public boolean updateGUIs=false;
		public DatapathBuilder.DatapathModule datapath;
		public ControlBuilder.ControlModule control;
		public Computer computer;
		public CustomProcessorModule(Computer computer, DatapathBuilder.DatapathModule datapath,ControlBuilder.ControlModule control)
		{
			this.computer=computer; this.datapath=datapath; this.control=control;
			initialize();
		}
		public CustomProcessorModule(Computer computer, String datapathFilename, String controlFilename)
		{
			this.computer=computer;
			datapath=computer.datapathBuilder.loadDatapathModule(datapathFilename);
			control=ControlBuilder.loadControlModule(computer,controlFilename,datapath);
			initialize();
		}
		public void initialize()
		{
			currentState=new String[control.controlPaths.size()];
			nextState=new String[control.controlPaths.size()];
			setFirstState();
			datapath.propagateAll();
			doAllPaths();
			datapath.propagateAll();			
		}
		public void getHardwareInputs()
		{
			if (computer.customProcessor!=null && datapath.getBlock("interrupt")!=null && datapath.getBlock("interrupt").type.equals("input pin"))
				datapath.getBlock("interrupt").setValue(computer.customProcessor.interruptRaised?1:0);
//			if (datapath.getBlock("interrupt number")!=null && datapath.getBlock("interrupt number").type.equals("input pin") && computer.interruptController!=null)
//				datapath.getBlock("interrupt number").setValue(computer.interruptController.cpuGetInterrupt());
		}
		public void putHardwareOutputs()
		{
			if (computer.customProcessor!=null && datapath.getBlock("program counter")!=null && datapath.getBlock("program counter").type.equals("register"))
				computer.customProcessor.program_counter=datapath.getBlock("program counter").getValue();
			if (computer.customProcessor!=null && datapath.getBlock("PC")!=null && datapath.getBlock("PC").type.equals("register"))
				computer.customProcessor.program_counter=datapath.getBlock("PC").getValue();
			if (computer.customProcessor!=null && datapath.getBlock("raise interrupt")!=null && datapath.getBlock("raise interrupt").type.equals("output pin") && computer.interruptController!=null)
				computer.interruptController.setIRQ(0, (int)datapath.getBlock("raise interrupt").getValue());
			if (computer.customProcessor!=null && datapath.getBlock("halt")!=null && datapath.getBlock("halt").type.equals("output pin"))
				if (datapath.getBlock("halt").getValue()!=0)
						computer.computerGUI.pause();
		}
		public void doCycle()
		{
			if (!active) return;
			control.resetHighlights();
			datapath.resetHighlights();
			getHardwareInputs();
			datapath.propagateAll();
			doAllPaths();
			datapath.propagateAll();
			assembleAllNextStates();
			datapath.clockAll();
			datapath.propagateAll();
			doAllPaths();
			datapath.propagateAll();
			currentState=nextState;
			nextState=new String[control.controlPaths.size()];
			putHardwareOutputs();
			if (updateGUIs)
			{
				computer.controlBuilder.repaint();
				computer.datapathBuilder.repaint();
			}
		}
		public String[] currentState;
		public String[] nextState;

		public void setFirstState()
		{
			for (int i=0; i<control.controlPaths.size(); i++)
			{
				currentState[i]=((ControlBuilder.ControlState)(((ControlBuilder.ControlPath)(control.controlPaths.elementAt(i))).controlStates.elementAt(0))).name;
			}
		}
		public void doAllPaths()
		{
			datapath.resetClocks();
			for (int i=0; i<control.controlPaths.size(); i++)
			{
				for (int j=0; j<((ControlBuilder.ControlPath)(control.controlPaths.elementAt(i))).controlStates.size(); j++)
				{
					if(((ControlBuilder.ControlState)(((ControlBuilder.ControlPath)(control.controlPaths.elementAt(i))).controlStates.elementAt(j))).name.equals(currentState[i]))
					{
						doState(((ControlBuilder.ControlState)(((ControlBuilder.ControlPath)(control.controlPaths.elementAt(i))).controlStates.elementAt(j))),i);
					}
				}
			}
		}
		public void assembleAllNextStates()
		{
			for (int i=0; i<control.controlPaths.size(); i++)
			{
				for (int j=0; j<((ControlBuilder.ControlPath)(control.controlPaths.elementAt(i))).controlStates.size(); j++)
				{
					if(((ControlBuilder.ControlState)(((ControlBuilder.ControlPath)(control.controlPaths.elementAt(i))).controlStates.elementAt(j))).name.equals(currentState[i]))
						assembleNextState(((ControlBuilder.ControlState)(((ControlBuilder.ControlPath)(control.controlPaths.elementAt(i))).controlStates.elementAt(j))),i);
				}
			}
		}
		public void doState(ControlBuilder.ControlState state, int path)
		{
//			if (state.row==0) return;		//not sure why this is here?
			if (state.row<control.highlightedRows.length)
				control.highlightedRows[state.row]=true;
			if (updateGUIs)
			{
				computer.controlBuilder.repaint();
				computer.controlBuilder.setStatusLabel("Current state: "+state.name);
			}
			
			for (int i=0; i<state.controlInstructions.size(); i++)
			{
				if (((ControlBuilder.ControlInstruction)state.controlInstructions.elementAt(i)).isActive())
				{
					control.highlightedRows[((ControlBuilder.ControlInstruction)state.controlInstructions.elementAt(i)).row]=true;
					((ControlBuilder.ControlInstruction)state.controlInstructions.elementAt(i)).doInstruction();
					break;
				}
			}
		}
		public void assembleNextState(ControlBuilder.ControlState state, int path)
		{
			for (int i=0; i<state.nextStates.size(); i++)
			{
				if (((ControlBuilder.NextState)state.nextStates.elementAt(i)).isActive())
				{
					control.highlightedRows[((ControlBuilder.NextState)state.nextStates.elementAt(i)).row]=true;
					nextState[path]=((ControlBuilder.NextState)state.nextStates.elementAt(i)).name;
					break;
				}
			}
		}
	}

	//construct a module file containing:
	// full paths of the datapath and control xml files
	// a list of input pins, their orderings, in-module numbers, and names
	// a list of output pins, etc.
	public static void createModule(String name, String cname, String mname, String xml) 
	{
		String module="<module>\n";
		module+="<datapath>"+name+"</datapath>\n";
		module+="<control>"+cname+"</control>\n";
		
		//look through xml for <input pin> and <output pin> declarations and copy them to module
		int ptr=0;
		while(true)
		{
			ptr=xml.indexOf("<input pin>",ptr);
			if (ptr==-1) break;
			module+=xml.substring(ptr,xml.indexOf("</input pin>",ptr)+"</input pin>".length())+"\n";
			ptr++;
		}
		ptr=0;
		while(true)
		{
			ptr=xml.indexOf("<output pin>",ptr);
			if (ptr==-1) break;
			module+=xml.substring(ptr,xml.indexOf("</output pin>",ptr)+"</output pin>".length())+"\n";
			ptr++;
		}
		
		module+="</module>\n";

		PrintWriter pw;
		try {
			pw = new PrintWriter(mname);
			pw.print(module);
			pw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
