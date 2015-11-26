package simulator;

import java.util.ArrayList;
import java.util.Hashtable;

import simulator.DatapathBuilder.Bus;


public class ToArduino 
{
	String xml;		//incoming xml code
	String[] xmlParts;	//xml now split into entries
	int numblocks;	//number of blocks
	ArrayList<Integer>[] inputs;
	int nextIOPin=3;	//assign arduino pins starting at 3

	Hashtable<Integer,Integer> busEquivalency;	//old bus number : source number
	Hashtable<Integer,Integer> busMasks;		//old bus number : bits

	StringBuilder headerCode=new StringBuilder();	//global vars
	StringBuilder setupCode=new StringBuilder();	//setup()
	StringBuilder loopCode=new StringBuilder();		//loop()
	
	//the call point for this module
	//pass it a datapath xml it sets up the datastructure
	//then call getC to get the Arduino equivalent code
	public ToArduino(String xml)
	{
		this.xml=xml;
		busEquivalency=new Hashtable<Integer,Integer>();
		busMasks=new Hashtable<Integer,Integer>();
		datapathXMLParse();
		numblocks=highestBlockNumber()+1;
		inputs=new ArrayList[numblocks];
		for(int i=0; i<numblocks; i++)
			inputs[i]=new ArrayList<Integer>();
		for (int i=1; i<=highestBlockNumber(); i++)
			constructBus(i);
		reduceBus();
		for (int i=1; i<=highestBlockNumber(); i++)
			constructBlock(i);
		clockRegs();
		setupCode.append("//Clock is pin 2\npinMode(2,INPUT);\n");
	}
	
	//pass it a complete Arduino C program with incomplete lookup table definitions
	//it outputs a corrected program with defined arrays
	private String correctMemorySyntax(String c_in)
	{
		int first,last=0;
		if((first=c_in.indexOf("int memoryC"))==-1) return c_in;
		
//if(0==0) return c_in;		
		//find the max memoryC line and the longest
		int max=0,longest=0;
		int finger=0;
		String replace="";
		
		while(c_in.indexOf("int memoryC[",finger)!=-1)
		{
			finger=c_in.indexOf("int memoryC[",finger);
			last=c_in.indexOf("};",finger)+"};".length();
//System.out.println(finger+" "+last);
			String addToReplace=c_in.substring(c_in.indexOf("{",finger),last-1);
			int size=addToReplace.split(",").length-1;
			if(size>longest) longest=size;
			replace+=addToReplace+",\n";
			max++;
			finger=last;
		}
		replace="int memoryC["+max+"]["+longest+"]={\n"+replace+"};\n";
		
		c_in=c_in.substring(0,first)+replace+c_in.substring(last,c_in.length());
		return c_in;
	}

	//reduces the size of the table[]
	//input is Arduino program with each table index == the datapath id
	//output is renumbered to make the table as small as possible
	private String reduceC(String c_in)
	{
		//extract table size
		int t1=c_in.indexOf("int table[")+"int table[".length();
		int tablesize=Integer.parseInt(c_in.substring(t1,c_in.indexOf("]",t1)));
		Hashtable<Integer,Integer> tablemapping=new Hashtable<Integer,Integer>();
		Hashtable<Integer,Integer> registermapping=new Hashtable<Integer,Integer>();
		Hashtable<Integer,Integer> memorymapping=new Hashtable<Integer,Integer>();
		int finger=c_in.indexOf("loop()");
		int remapCounter=0;
		while(c_in.indexOf("table[",finger)>=0)
		{
			finger=c_in.indexOf("table[",finger)+"table[".length();
			int index=Integer.parseInt(c_in.substring(finger, c_in.indexOf("]",finger)));
			if(!tablemapping.containsKey(index))
				tablemapping.put(index, remapCounter++);
		}
		finger=c_in.indexOf("loop()");
		int registerRemapCounter=0;
		while(c_in.indexOf("registerInput[",finger)>=0)
		{
			finger=c_in.indexOf("registerInput[",finger)+"registerInput[".length();
			int index=Integer.parseInt(c_in.substring(finger, c_in.indexOf("]",finger)));
			if(!registermapping.containsKey(index))
				registermapping.put(index, registerRemapCounter++);
		}
		finger=c_in.indexOf("loop()");
		int memoryRemapCounter=0;
		while(c_in.indexOf("memory[",finger)>=0)
		{
			finger=c_in.indexOf("memory[",finger)+"memory[".length();
			int index=Integer.parseInt(c_in.substring(finger, c_in.indexOf("]",finger)));
			if(!memorymapping.containsKey(index))
				memorymapping.put(index, memoryRemapCounter++);
		}
		Object[] dests=tablemapping.keySet().toArray();
		for(int i=0; i<dests.length; i++)
		{
			c_in=c_in.replaceAll("table\\["+dests[i]+"\\]", "tableC["+tablemapping.get(dests[i])+"]");
//			System.out.println("replacing "+"table["+dests[i]+"]"+" with "+"table["+tablemapping.get(dests[i])+"]");
		}
		dests=registermapping.keySet().toArray();
		for(int i=0; i<dests.length; i++)
			c_in=c_in.replaceAll("registerInput\\["+dests[i]+"\\]", "registerInputC["+registermapping.get(dests[i])+"]");
		dests=memorymapping.keySet().toArray();
		for(int i=0; i<dests.length; i++)
			c_in=c_in.replaceAll("memory\\["+dests[i], "memoryC["+memorymapping.get(dests[i]));
		c_in=c_in.replaceAll("int table\\["+tablesize+"\\]", "int tableC["+remapCounter+"]");
		c_in=c_in.replaceAll("int registerInput\\["+tablesize+"\\]", "int registerInputC["+registerRemapCounter+"]");
		c_in=c_in.replaceAll("int memory\\["+tablesize+"\\]", "int memoryC["+memoryRemapCounter+"]");
			
		
		return c_in;
	}
	
	//eliminate buses from the output
	//all buses are iteratively tracked back to the source
	//all components are then reconnected to their true source
	private void reduceBus()
	{
		
		Object[] dests=busEquivalency.keySet().toArray();
		int[] sources=new int[dests.length];
		for(int i=0; i<dests.length; i++)
			sources[i]=busEquivalency.get(dests[i]);
		//now go through and reassign
		//repeat until converges
		for(int k=0; k<dests.length; k++)
		{
			//consider every dest
			for(int i=0; i<dests.length; i++)
			{
				int dest=(Integer)dests[i];
				//look at all the entries
				for(int j=0; j<sources.length; j++)
				{
					//if the dest occurs, replace it with its own source
					if(sources[j]==dest)
					{
						sources[j]=sources[i];
					}
				}
			}
		}
		//now every block dest should link to a block
		busEquivalency.clear();
		for(int blockdest=0; blockdest<dests.length; blockdest++)
		{
			busEquivalency.put((Integer)(dests[blockdest]), sources[blockdest]);
//			loopCode.append("table["+(Integer)(dests[blockdest])+"]=table["+sources[blockdest]+"] & "+busMasks.get((Integer)dests[blockdest])+";\n");
		}
	}

	//main external call point
	//generates and returns the C code
	public String getC() {
		StringBuilder c=new StringBuilder();
		c.append(getCopyright());
		c.append("int table["+numblocks+"];\n");
		c.append("int lastclock=0;\n");
		c.append("int registerInput["+numblocks+"];\n\n");
		c.append(headerCode.toString()+"\n");
		c.append("void setup()\n{\n");
		c.append(setupCode.toString());
		c.append("}\n\nvoid loop()\n{\n");
		c.append("int clock=digitalRead(2);\nint doClock=0;\nif(clock==1&&lastclock==0) doClock=1;\nlastclock=clock;\n");
		c.append(loopCode.toString());
		c.append("}\n");
		String cstr= c.toString();
		cstr=reduceC(cstr);
		cstr=correctMemorySyntax(cstr);
		return cstr;
	}
	
	private String getCopyright()
	{
		return "/* Autogenerated from Emumaker86\n * Michael Black, 2015\n*/\n\n";
	}
	
	//go through the datapath XML and put together an arraylist of components
	private void datapathXMLParse()
	{
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
	private String[] extractBlock(int number)
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
	private String extractField(String[] block, String field)
	{
		for (int i=0; i<block.length; i++)
		{
			if (block[i].equals(field))
				return block[i+1];
		}
		return null;
	}

	//return the number of the highest-numbered block
	private int highestBlockNumber()
	{
		int number=0;
		for(int i=0; i<xmlParts.length; i++)
		{
			if (xmlParts[i].equals("<number>") && Integer.parseInt(xmlParts[i+1])>number)
				number=Integer.parseInt(xmlParts[i+1]);
		}
		return number;
	}
	
	//produce C code to pass registerInputs to table entries on the rising edge of doClock
	private void clockRegs()
	{
		loopCode.append("if(doClock) {\n");
		for (int i=1; i<=highestBlockNumber(); i++)
		{
			String[] block=extractBlock(i);
			if(block==null) continue;
			String type=block[0].substring(1,block[0].length()-1);
			if(type.equals("register"))
			{
				String a;
				if((a=extractField(block,"<enable>"))!=null)
					loopCode.append("if("+tablestring(Integer.parseInt(a))+"!=0) ");
				loopCode.append("table["+i+"]=registerInput["+i+"];\n");
			}
			else if (type.equals("register file") || type.equals("memory"))
			{
				String a=extractField(block,"<address>");
				loopCode.append("memory["+i+"]["+tablestring(Integer.parseInt(a))+"]=registerInput["+i+"];\n");
			}
		}
		loopCode.append("}\n");
	}
	

	//given a bus number, create C code for it, and mark its output blocks
	private void constructBus(int number)
	{
		//first get its parts
		String[] block=extractBlock(number);
		if (block==null) return;
		String type=block[0].substring(1,block[0].length()-1);
System.out.println("encountered block "+number+" of type "+type);
		if(!type.equals("bus")) return;
		int bits=0;
		if (extractField(block,"<bits>")!=null)
			bits=Integer.parseInt(extractField(block,"<bits>"));
		int mask=(int)(Math.pow(2, bits)-1);
		//get the bits field
		int entry=0,exit=0;

		//get the location coordinates, entry and exit buses
		if (extractField(block,"<entry>")!=null)
		{
			entry=Integer.parseInt(extractField(block,"<entry>"));
		}
		if (extractField(block,"<exit>")!=null)
		{
			exit=Integer.parseInt(extractField(block,"<exit>"));
		}
		
		//splitter sourced buses are special cases.  we need to keep them in the C code.
		if(entry!=0 && getType(entry).equals("splitter"))
		{
			System.out.println("splitterbus: "+number+" "+entry);
			//find the splitter line going to this bus
			String fieldtag="<line "+number+">";
			System.out.println(fieldtag);
			String field=extractField(extractBlock(entry),fieldtag);
			if(field==null)
			{
				loopCode.append("table["+number+"]=table["+entry+"] & "+mask+";\n");
System.out.println(loopCode);
			}
			else
			{
			String[] bitrange=field.split(":");
			int high=Integer.parseInt(bitrange[0]);
			int low=Integer.parseInt(bitrange[1]);
			int bitmask=(int)Math.pow(2, high-low+1)-1;
			loopCode.append("table["+number+"]=(table["+entry+"]>>"+low+")&"+bitmask+";\n");
System.out.println(loopCode);
//busEquivalency.put(number, entry);
			}
		}
		
//		loopCode.append("table["+number+"]=table["+entry+"] & "+mask+";\n");
		else
		{
			busEquivalency.put(number, entry);
		}
		busMasks.put(number, mask);
		if(exit!=0 && Integer.parseInt(extractField(block,"<xcoordinate>"))==Integer.parseInt(extractField(block,"<xcoordinate2>")))
		{
//			inputs[exit].add(number);
			// add it to block inputs[exit]
			//if the block has other inputs, arrange them from smallest to largest xcoordinate
			
			int myxcoor=Integer.parseInt(extractField(block,"<xcoordinate>"));
			int x;
			for(x=0; x<inputs[exit].size(); x++)
			{
				int b=inputs[exit].get(x);
				int otherxcoor=Integer.parseInt(extractField(extractBlock(b),"<xcoordinate>"));
				if(myxcoor<otherxcoor) break;
			}
			inputs[exit].add(x,number);
		}
	}
	
	private String tablestring(int number, int i)
	{
		return tablestring(inputs[number].get(i));
	}
	private String tablestring(int number)
	{
		//note: buses coming directly from splitters have no equivalency entry
		return "(table["+busEquivalency.get(number)+"]&"+busMasks.get(number)+") ";
	}
	private String getType(int number)
	{
		String[] block=extractBlock(number);
		String type=block[0].substring(1,block[0].length()-1);
		return type;
	}
	
	//generate C code for each type of component
	private void constructBlock(int number)
	{
		//first get its parts
		String[] block=extractBlock(number);
		if (block==null) return;
		String type=block[0].substring(1,block[0].length()-1);
		if(type.equals("bus")) return;
		int bits=0;
		if (extractField(block,"<bits>")!=null)
			bits=Integer.parseInt(extractField(block,"<bits>"));
		String name="";
		if (extractField(block,"<name>")!=null)
			name=extractField(block,"<name>");
		String a=extractField(block,"<address>");

		String v="";
		if (type.equals("adder")||type.equals("combinational-adder"))
		{
			v="table["+number+"]=0xffffffff&(";
			for (int i=0; i<inputs[number].size(); i++)
			{
//				v+="table["+inputs[number].get(i)+"] ";
				v+=tablestring(number,i);
				if(i<inputs[number].size()-1) v+=" + ";
			}
			v+=");\n";
		}
		else if (type.equals("joiner"))
		{
			v="table["+number+"]=(";
			int b=0;
			for(int i=inputs[number].size()-1; i>=0; i--)
			{
				v+="("+tablestring(number,i)+"<<"+b+")";
				b+=(int)(Math.log(busMasks.get(inputs[number].get(i))+1)/Math.log(2));
				if(i>0) v+=" | ";
			}
			v+=");\n";
		}
		else if (type.equals("combinational-and"))
		{
			v="table["+number+"]=(";
			for (int i=0; i<inputs[number].size(); i++)
			{
//				v+="table["+inputs[number].get(i)+"] ";
				v+=tablestring(number,i);
				if(i<inputs[number].size()-1) v+=" & ";
			}
			v+=");\n";
		}
		else if (type.equals("combinational-or"))
		{
			v="table["+number+"]=(";
			for (int i=0; i<inputs[number].size(); i++)
			{
//				v+="table["+inputs[number].get(i)+"] ";
				v+=tablestring(number,i);
				if(i<inputs[number].size()-1) v+=" | ";
			}
			v+=");\n";
		}
		else if (type.equals("combinational-nand"))
		{
			v="table["+number+"]=~(";
			for (int i=0; i<inputs[number].size(); i++)
			{
//				v+="table["+inputs[number].get(i)+"] ";
				v+=tablestring(number,i);
				if(i<inputs[number].size()-1) v+=" & ";
			}
			v+=");\n";
		}
		else if (type.equals("combinational-nor"))
		{
			v="table["+number+"]=~(";
			for (int i=0; i<inputs[number].size(); i++)
			{
//				v+="table["+inputs[number].get(i)+"] ";
				v+=tablestring(number,i);
				if(i<inputs[number].size()-1) v+=" | ";
			}
			v+=");\n";
		}
		else if (type.equals("combinational-xor"))
		{
			v="table["+number+"]=(";
			for (int i=0; i<inputs[number].size(); i++)
			{
//				v+="table["+inputs[number].get(i)+"] ";
				v+=tablestring(number,i);
				if(i<inputs[number].size()-1) v+=" ^ ";
			}
			v+=");\n";
		}
		else if (type.equals("combinational-not"))
		{
//			v="table["+number+"]=~table["+inputs[number].get(0)+"];\n";
			v="table["+number+"]=~"+tablestring(number,0)+";\n";
		}
		else if (type.equals("combinational-negate"))
		{
//			v="table["+number+"]=-table["+inputs[number].get(0)+"];\n";
			v="table["+number+"]=-"+tablestring(number,0)+";\n";
		}
		else if (type.equals("combinational-increment"))
		{
//			v="table["+number+"]=(table["+inputs[number].get(0)+"]+1)&0xffffffff;\n";
			v="table["+number+"]=("+tablestring(number,0)+"+1)&0xffffffff;\n";
		}
		else if (type.equals("combinational-decrement"))
		{
//			v="table["+number+"]=(table["+inputs[number].get(0)+"]-1)&0xffffffff;\n";
			v="table["+number+"]=("+tablestring(number,0)+"-1)&0xffffffff;\n";
		}
		else if (type.equals("combinational-less-than"))
		{
//			v="table["+number+"]=table["+inputs[number].get(0)+"] < table["+inputs[number].get(1)+"]? 1l:0;\n";
			v="table["+number+"]="+tablestring(number,0)+" < "+tablestring(number,1)+"? 1l:0;\n";
		}
		else if (type.equals("combinational-equal-to"))
		{
//			v="table["+number+"]=table["+inputs[number].get(0)+"] == table["+inputs[number].get(1)+"]? 1l:0;\n";
			v="table["+number+"]="+tablestring(number,0)+" == "+tablestring(number,1)+"? 1l:0;\n";
		}
		else if (type.equals("combinational-shift-right"))
		{
			if(inputs[number].size()==2)
//				v="table["+number+"]=table["+inputs[number].get(0)+"] >> table["+inputs[number].get(1)+"];\n";
				v="table["+number+"]="+tablestring(number,0)+" >> "+tablestring(number,1)+";\n";
			else
//				v="table["+number+"]=table["+inputs[number].get(0)+"] >> 1;\n";
				v="table["+number+"]="+tablestring(number,0)+" >> 1;\n";
		}
		else if (type.equals("combinational-shift-left"))
		{
			if(inputs[number].size()==2)
				//v="table["+number+"]=table["+inputs[number].get(0)+"] << table["+inputs[number].get(1)+"];\n";
				v="table["+number+"]="+tablestring(number,0)+" << "+tablestring(number,1)+";\n";
			else
				//v="table["+number+"]=table["+inputs[number].get(0)+"] << 1;\n";
				v="table["+number+"]="+tablestring(number,0)+" << 1;\n";
		}
		else if (type.equals("input pin"))
		{
			setupCode.append("// connected to input pin "+name+"\n");
			v="table["+number+"]=";
			for(int b=0; b<bits; b++)
			{
				v+="(digitalRead("+nextIOPin+")<<"+b+")";
				if(b<bits-1)
					v+="|";
				setupCode.append("pinMode("+nextIOPin+",INPUT);\n");
				nextIOPin++;
			}
			v+=";\n";
		}
		else if (type.equals("output pin"))
		{
			setupCode.append("// connected to output pin "+name+"\n");
			if(name.equals("light") && bits==1)
			{
				v+="digitalWrite("+13+","+tablestring(number,0)+"==0? LOW:HIGH);\n";
				setupCode.append("pinMode("+13+",OUTPUT);\n");				
			}
			else
			{
			for(int b=0; b<bits; b++)
			{
				v+="digitalWrite("+nextIOPin+",(("+tablestring(number,0)+">>"+b+")&1)==0? LOW:HIGH);\n";
				setupCode.append("pinMode("+nextIOPin+",OUTPUT);\n");
				nextIOPin++;
			}
			}
		}
		else if (type.equals("constant"))
		{
			v="table["+number+"]=0x"+Integer.parseInt(name,16)+";\n";
		}
		else if ((type.equals("multiplexor")&&a!=null)||type.equals("data_multiplexor"))
		{
			v="";
			for (int i=0; i<inputs[number].size(); i++)
			{
				v+="if ("+tablestring(Integer.parseInt(a))+"=="+i+") ";
				v+="table["+number+"]="+tablestring(number,i)+";\n";
			}
		}
		else if (type.equals("register"))
		{
			v="registerInput["+number+"]="+tablestring(number,0)+";\n";
		}
		else if (type.equals("register file"))
		{
//			headerCode.append("int memory["+number+"]["+(busMasks.get(Integer.parseInt(a))+1)+"]={};\n");
			headerCode.append("int memory["+number+"][]={");
			for(int i=0; i<busMasks.get(Integer.parseInt(a))+1; i++)
			{
				headerCode.append("0,");
			}
			headerCode.append("};\n");
			v="registerInput["+number+"]="+tablestring(number,0)+";\n";
			v+="table["+number+"]=memory["+number+"]["+tablestring(Integer.parseInt(a))+"];\n";
		}
		else if (type.equals("memory"))
		{
//			headerCode.append("int memory["+number+"]["+(busMasks.get(Integer.parseInt(a))+1)+"]={};\n");

			headerCode.append("int memory["+number+"][]={");
			for(int i=0; i<busMasks.get(Integer.parseInt(a))+1; i++)
			{
				headerCode.append("0,");
			}
			headerCode.append("};\n");
			
			
			v="registerInput["+number+"]="+tablestring(number,0)+";\n";
			v+="table["+number+"]=memory["+number+"]["+tablestring(Integer.parseInt(a))+"];\n";
		}
		else if (type.equals("lookup table"))
		{
			headerCode.append("int memory["+number+"][]={");
			for(int i=0; i<busMasks.get(Integer.parseInt(a))+1; i++)
			{
				String val=extractField(block,"<value "+i+">");
				if(val==null)
					headerCode.append("0,");
				else
					headerCode.append("0x"+val+",");
			}
			headerCode.append("};\n");
			v="table["+number+"]=memory["+number+"]["+tablestring(Integer.parseInt(a))+"];\n";
		}
		else if (type.equals("splitter"))
		{
			v="table["+number+"]="+tablestring(number,0)+";\n";			
		}
		else
		{
			System.out.println("Error: unhandled device "+type);
		}

		
		loopCode.append(v);
	}
}
