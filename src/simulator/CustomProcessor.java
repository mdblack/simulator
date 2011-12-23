package simulator;

public class CustomProcessor extends IODevice
{
	public boolean active=false;
	private Computer computer;
	
	public CustomProcessor(Computer computer)
	{
		this.computer=computer;
		if (computer.datapathBuilder==null || computer.controlBuilder==null) return;
		computer.datapathBuilder.resetAll();

		computer.ioports.requestPorts(this,new int[]{0xff},"Switch Processor",new String[]{""});

		active=true;

		currentState=new String[computer.controlBuilder.controlPaths.size()];
		nextState=new String[computer.controlBuilder.controlPaths.size()];
		setFirstState();
		computer.datapathBuilder.propagateAll();
		doAllPaths();
		computer.datapathBuilder.propagateAll();

		computer.datapathBuilder.repaint();
	}
	public void doCycle()
	{
		if (!active) return;
		computer.controlBuilder.resetHighlights();
		computer.datapathBuilder.resetHighlights();
		computer.datapathBuilder.propagateAll();
		doAllPaths();
		computer.datapathBuilder.propagateAll();
		assembleAllNextStates();
		computer.datapathBuilder.clockAll();
		computer.datapathBuilder.propagateAll();
		doAllPaths();
		computer.datapathBuilder.propagateAll();
		currentState=nextState;
		nextState=new String[computer.controlBuilder.controlPaths.size()];
		computer.controlBuilder.repaint();
		computer.datapathBuilder.repaint();
	}
	public String[] currentState;
	public String[] nextState;

	public void setFirstState()
	{
		for (int i=0; i<computer.controlBuilder.controlPaths.size(); i++)
		{
			currentState[i]=((ControlBuilder.ControlState)(((ControlBuilder.ControlPath)(computer.controlBuilder.controlPaths.elementAt(i))).controlStates.elementAt(0))).name;
		}
	}
	public void doAllPaths()
	{
		computer.datapathBuilder.resetClocks();
		for (int i=0; i<computer.controlBuilder.controlPaths.size(); i++)
		{
			for (int j=0; j<((ControlBuilder.ControlPath)(computer.controlBuilder.controlPaths.elementAt(i))).controlStates.size(); j++)
			{
				if(((ControlBuilder.ControlState)(((ControlBuilder.ControlPath)(computer.controlBuilder.controlPaths.elementAt(i))).controlStates.elementAt(j))).name.equals(currentState[i]))
					doState(((ControlBuilder.ControlState)(((ControlBuilder.ControlPath)(computer.controlBuilder.controlPaths.elementAt(i))).controlStates.elementAt(j))),i);
			}
		}
	}
	public void assembleAllNextStates()
	{
		for (int i=0; i<computer.controlBuilder.controlPaths.size(); i++)
		{
			for (int j=0; j<((ControlBuilder.ControlPath)(computer.controlBuilder.controlPaths.elementAt(i))).controlStates.size(); j++)
			{
				if(((ControlBuilder.ControlState)(((ControlBuilder.ControlPath)(computer.controlBuilder.controlPaths.elementAt(i))).controlStates.elementAt(j))).name.equals(currentState[i]))
					assembleNextState(((ControlBuilder.ControlState)(((ControlBuilder.ControlPath)(computer.controlBuilder.controlPaths.elementAt(i))).controlStates.elementAt(j))),i);
			}
		}
	}
	public void doState(ControlBuilder.ControlState state, int path)
	{
		computer.controlBuilder.highlightedRows[state.row]=true;
		computer.controlBuilder.repaint();

		for (int i=0; i<state.controlInstructions.size(); i++)
		{
			if (((ControlBuilder.ControlInstruction)state.controlInstructions.elementAt(i)).isActive())
			{
				computer.controlBuilder.highlightedRows[((ControlBuilder.ControlInstruction)state.controlInstructions.elementAt(i)).row]=true;
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
				computer.controlBuilder.highlightedRows[((ControlBuilder.NextState)state.nextStates.elementAt(i)).row]=true;
				nextState[path]=((ControlBuilder.NextState)state.nextStates.elementAt(i)).state;
				break;
			}
		}
	}
	public byte ioPortReadByte(int address)
	{
		return active? (byte)1:0;
	}
	public void ioPortWriteByte(int address, byte data)
	{
		if (data==0)
			active=false;
		else
			active=true;
	}
}
