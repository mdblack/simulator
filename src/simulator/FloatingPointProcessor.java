/*FloatingPointProcessor.java
 * much of this comes from Java PC
 * 
	//Original files:
	//  FpuState.java
	//  FpuState64.java
	 * 
	//author Jeff Tseng
 * 
 * */

package simulator;

import simulator.Processor.GUICODE;
import simulator.Processor.MICROCODE;

public class FloatingPointProcessor 
{
	private Processor processor;
	public FloatingPointProcessor(Processor processor)
	{
		this.processor=processor;
 	}

    public void decodeFloat(int opcode)
	{
		int modrm = (0xff & processor.fetchQueue.readByte());
		int sib=0;
		int displacement=0;
		processor.fetchQueue.advance(1);
		
		if (processor.isCode(MICROCODE.PREFIX_ADDRESS_32BIT))
		{
			if (Processor.sibTable[modrm]==1)
			{
				sib=(0xff&processor.fetchQueue.readByte());
				processor.fetchQueue.advance(1);
			}
		}
		//displacement
		if ((modrm & 0xc0)!=0xc0)
		{
			int hasdisplacement=0;
			if(processor.isCode(MICROCODE.PREFIX_ADDRESS_32BIT))
			{
				switch(modrm&0xc0)
				{
				case 0:
					switch(modrm&7)
					{
					case 4:
						if ((sib&0x7)==0x5)
							hasdisplacement=4;
						else
							hasdisplacement=0;
						break;
					case 5:
						hasdisplacement=4;
						break;
					}
					break;
				case 0x40:
					hasdisplacement=1; break;
				case 0x80:
					hasdisplacement=4; break;
				}
			}
			else
			{
				switch(modrm&0xc0)
				{
				case 0:
					if ((modrm&7)==6)
						hasdisplacement=2;
					else
						hasdisplacement=0;
					break;
				case 0x40:
					hasdisplacement=1; break;
				case 0x80:
					hasdisplacement=2; break;
				}		
			}
			if (hasdisplacement==1)
			{
				displacement=(0xff & processor.fetchQueue.readByte());
				processor.fetchQueue.advance(1);
			}
			else if (hasdisplacement==2)
			{
				displacement=(0xff & processor.fetchQueue.readByte());
				processor.fetchQueue.advance(1);
				displacement=(displacement&0xff)|(0xff00 & (processor.fetchQueue.readByte()<<8));
				processor.fetchQueue.advance(1);
			}
			else if (hasdisplacement==4)
			{
				displacement=(0xff & processor.fetchQueue.readByte());
				processor.fetchQueue.advance(1);
				displacement=(displacement&0xff)|(0xff00 & (processor.fetchQueue.readByte()<<8));
				processor.fetchQueue.advance(1);
				displacement=(displacement&0xffff)|(0xff0000 & (processor.fetchQueue.readByte()<<16));
				processor.fetchQueue.advance(1);
				displacement=(displacement&0xffffff)|(0xff000000 & (processor.fetchQueue.readByte()<<24));
				processor.fetchQueue.advance(1);
			}
		}
		
		//decode input operands
	
		switch(opcode)
		{
	    case 0xd8:
	        processor.pushCode(MICROCODE.FWAIT);
	        if ((modrm & 0xc0) != 0xc0) 
	        {
	            switch (modrm & 0x38) 
	            {
	            case 0x28:
	            case 0x38:
	                processor.decode_memory(modrm, sib, displacement);
	                processor.pushCode(MICROCODE.FLOAD0_MEM_SINGLE);
	                processor.pushCode(MICROCODE.FLOAD1_ST0);                     
	                break;
	            default:
	                processor.pushCode(MICROCODE.FLOAD0_ST0); 
	                processor.decode_memory(modrm, sib, displacement);
	                processor.pushCode(MICROCODE.FLOAD1_MEM_SINGLE);
	                break;
	            }
	        } 
	        else 
	        {
	            switch (modrm & 0xf8) 
	            {
	            case 0xe8:
	            case 0xf8:
	                processor.pushCode(MICROCODE.FLOAD0_STN);
	                processor.pushCode(modrm & 0x07);
	                processor.pushCode(MICROCODE.FLOAD1_ST0);
	                break;
	            default:
	                processor.pushCode(MICROCODE.FLOAD0_ST0);
	                processor.pushCode(MICROCODE.FLOAD1_STN);
	                processor.pushCode(modrm & 0x07);
	                break;
	            }
	        }
	        break;            
	
    	case 0xd9:
	        if ((modrm & 0xc0) != 0xc0) 
	        {
	            switch (modrm & 0x38) 
	            {
	            case 0x00:
	                processor.pushCode(MICROCODE.FWAIT);
	                processor.decode_memory(modrm, sib, displacement);
	                processor.pushCode(MICROCODE.FLOAD0_MEM_SINGLE);
	                break;
	            case 0x10:
	            case 0x18: 
	                processor.pushCode(MICROCODE.FWAIT); 
	                processor.pushCode(MICROCODE.FLOAD0_ST0); break;
	            case 0x20:
	                processor.pushCode(MICROCODE.FWAIT);
	                processor.decode_memory(modrm, sib, displacement); 
	                break;
	            case 0x28:
	                processor.pushCode(MICROCODE.FWAIT);
	                processor.decode_memory(modrm, sib, displacement);
	                processor.pushCode(MICROCODE.LOAD0_MEM_WORD);
	                break;
	            case 0x30: processor.decode_memory(modrm, sib, displacement); break;
	            case 0x38: processor.pushCode(MICROCODE.LOAD0_FPUCW); break;
	            }
	        } 
	        else 
	        {
	            processor.pushCode(MICROCODE.FWAIT);
	            switch (modrm & 0xf8) 
	            {
	            case 0xc0:
	                processor.pushCode(MICROCODE.FLOAD0_STN);
	                processor.pushCode(modrm & 0x07);
	                break;
	            case 0xc8:
	                processor.pushCode(MICROCODE.FLOAD0_ST0);
	                processor.pushCode(MICROCODE.FLOAD1_STN);
	                processor.pushCode(modrm & 0x07);
	                break;
	            }
	            switch (modrm) 
	            {
	            case 0xd0:
	            case 0xf6:
	            case 0xf7: break;
	            case 0xe0: 
	            case 0xe1: 
	            case 0xe5:
	            case 0xf0:
	            case 0xf2:
	            case 0xf4:
	            case 0xfa:
	            case 0xfb:
	            case 0xfc:
	            case 0xfe:
	            case 0xff: processor.pushCode(MICROCODE.FLOAD0_ST0); break;
	            case 0xf1:
	            case 0xf3:
	            case 0xf5:
	            case 0xf8:
	            case 0xf9:
	            case 0xfd:
	                processor.pushCode(MICROCODE.FLOAD0_ST0);
	                processor.pushCode(MICROCODE.FLOAD1_STN);
	                processor.pushCode(1);
	                break;
	            case 0xe4:
	                processor.pushCode(MICROCODE.FLOAD0_ST0);
	                processor.pushCode(MICROCODE.FLOAD1_POS0);
	                break;
	            case 0xe8: processor.pushCode(MICROCODE.FLOAD0_1); break;
	            case 0xe9: processor.pushCode(MICROCODE.FLOAD0_L2TEN); break;
	            case 0xea: processor.pushCode(MICROCODE.FLOAD0_L2E); break;
	            case 0xeb: processor.pushCode(MICROCODE.FLOAD0_PI); break;
	            case 0xec: processor.pushCode(MICROCODE.FLOAD0_LOG2); break;
	            case 0xed: processor.pushCode(MICROCODE.FLOAD0_LN2); break;
	            case 0xee: processor.pushCode(MICROCODE.FLOAD0_POS0); break;
	            }
	        }
	        break;            
	
	    case 0xda:
	        processor.pushCode(MICROCODE.FWAIT);
	        if ((modrm & 0xc0) != 0xc0) 
	        {
	            switch (modrm & 0x38) 
	            {
	            case 0x28:
	            case 0x38:
	                processor.decode_memory(modrm, sib, displacement);
	                processor.pushCode(MICROCODE.LOAD0_MEM_DOUBLE);
	                processor.pushCode(MICROCODE.FLOAD0_REG0);
	                processor.pushCode(MICROCODE.FLOAD1_ST0);
	                break;
	            default:
	                processor.pushCode(MICROCODE.FLOAD0_ST0); 
	                processor.decode_memory(modrm, sib, displacement);
	                processor.pushCode(MICROCODE.LOAD0_MEM_DOUBLE);
	                processor.pushCode(MICROCODE.FLOAD1_REG0);
	                break;
	            }
	        } 
	        else 
	        {
	            switch (modrm & 0xf8) 
	            {
	            case 0xc0: 
	            case 0xc8: 
	            case 0xd0: 
	            case 0xd8: 
	                processor.pushCode(MICROCODE.FLOAD0_STN);
	                processor.pushCode(modrm & 0x07);
	                break;
	            }
	            switch (modrm) 
	            {
	            case 0xe9:
	                processor.pushCode(MICROCODE.FLOAD0_ST0);
	                processor.pushCode(MICROCODE.FLOAD1_STN);
	                processor.pushCode(1);
	                break;
	            }
	        }
	        break;            
	
	    case 0xdb:
	        if ((modrm & 0xc0) != 0xc0) 
	        {
	            processor.pushCode(MICROCODE.FWAIT);
	            switch (modrm & 0x38) 
	            {
	            case 0x00:
	                processor.decode_memory(modrm, sib, displacement);
	                processor.pushCode(MICROCODE.LOAD0_MEM_DOUBLE);
	                processor.pushCode(MICROCODE.FLOAD0_REG0);
	                break;
	            case 0x08:
	            case 0x10:
	            case 0x18:
	            case 0x38: processor.pushCode(MICROCODE.FLOAD0_ST0); break;
	            case 0x28:
	                processor.decode_memory(modrm, sib, displacement);
	                processor.pushCode(MICROCODE.FLOAD0_MEM_EXTENDED);
	                break;
	            }
	        } 
	        else 
	        {
	            switch (modrm) 
	            {
	            case 0xe2: 
	            case 0xe3: break;
	            default: processor.pushCode(MICROCODE.FWAIT); break;
	            }
	            switch (modrm & 0xf8) 
	            {
	            case 0xc0: 
	            case 0xc8: 
	            case 0xd0: 
	            case 0xd8: 
	                processor.pushCode(MICROCODE.FLOAD0_STN);
	                processor.pushCode(modrm & 0x07);
	                break;
	            case 0xe8:
	            case 0xf0:
	                processor.pushCode(MICROCODE.FLOAD0_ST0);
	                processor.pushCode(MICROCODE.FLOAD1_STN);
	                processor.pushCode(modrm & 0x07);
	                break;
	            }
	        }
	        break;            
	
	    case 0xdc:
	        processor.pushCode(MICROCODE.FWAIT);
	        if ((modrm & 0xc0) != 0xc0) 
	        {
	            switch (modrm & 0x38) 
	            {
	            case 0x28:
	            case 0x38:
	                processor.decode_memory(modrm, sib, displacement);
	                processor.pushCode(MICROCODE.FLOAD0_MEM_DOUBLE);
	                processor.pushCode(MICROCODE.FLOAD1_ST0);                     
	                break;
	            default:
	                processor.pushCode(MICROCODE.FLOAD0_ST0); 
	                processor.decode_memory(modrm, sib, displacement);
	                processor.pushCode(MICROCODE.FLOAD1_MEM_DOUBLE);
	                break;
	            }
	        } 
	        else 
	        {
	            switch (modrm & 0xf8) 
	            {
	            case 0xe8:
	            case 0xf8:
	                processor.pushCode(MICROCODE.FLOAD0_STN);
	                processor.pushCode(modrm & 0x07);
	                processor.pushCode(MICROCODE.FLOAD1_ST0);
	                break;
	            default:
	                processor.pushCode(MICROCODE.FLOAD0_ST0);
	                processor.pushCode(MICROCODE.FLOAD1_STN);
	                processor.pushCode(modrm & 0x07);
	                break;
	            }
	        }
	        break;            
	
	    case 0xdd:
	        if ((modrm & 0xc0) != 0xc0) 
	        {
	            switch (modrm & 0x38) 
	            {
	            case 0x00:
	                processor.pushCode(MICROCODE.FWAIT);
	                processor.decode_memory(modrm, sib, displacement);
	                processor.pushCode(MICROCODE.FLOAD0_MEM_DOUBLE);
	                break;
	            case 0x08:
	            case 0x10:
	            case 0x18:
	                processor.pushCode(MICROCODE.FWAIT);
	                processor.pushCode(MICROCODE.FLOAD0_ST0); 
	                break;
	            case 0x20:
	                processor.pushCode(MICROCODE.FWAIT);
	                processor.decode_memory(modrm, sib, displacement); 
	                break;
	            case 0x30: processor.decode_memory(modrm, sib, displacement); break;
	            case 0x38: processor.pushCode(MICROCODE.LOAD0_FPUSW); break;
	            }
	        } 
	        else 
	        {
	            processor.pushCode(MICROCODE.FWAIT);
	            switch (modrm & 0xf8) 
	            {
	            case 0xc0: 
	                processor.pushCode(MICROCODE.LOAD0_ID);
	                processor.pushCode(modrm & 0x07);
	                break;
	            case 0xd0: 
	            case 0xd8: processor.pushCode(MICROCODE.FLOAD0_ST0); break;
	            case 0xe0:
	            case 0xe8:
	                processor.pushCode(MICROCODE.FLOAD0_ST0);
	                processor.pushCode(MICROCODE.FLOAD1_STN);
	                processor.pushCode(modrm & 0x07);
	                break;
	            }
	        }
	        break;            
	
	    case 0xde:
	        processor.pushCode(MICROCODE.FWAIT);
	        if ((modrm & 0xc0) != 0xc0) 
	        {
	            switch (modrm & 0x38) 
	            {
	            case 0x28:
	            case 0x38:
	                processor.decode_memory(modrm, sib, displacement);
	                processor.pushCode(MICROCODE.LOAD0_MEM_WORD);
	                processor.pushCode(MICROCODE.FLOAD0_REG0);
	                processor.pushCode(MICROCODE.FLOAD1_ST0);
	                break;
	            case 0x30:
	                processor.pushCode(MICROCODE.FLOAD0_ST0); 
	                processor.decode_memory(modrm, sib, displacement);
	                processor.pushCode(MICROCODE.LOAD0_MEM_QUAD);
	                processor.pushCode(MICROCODE.FLOAD1_REG0L);
	                break;
	            default:
	                processor.pushCode(MICROCODE.FLOAD0_ST0); 
	                processor.decode_memory(modrm, sib, displacement);
	                processor.pushCode(MICROCODE.LOAD0_MEM_WORD);
	                processor.pushCode(MICROCODE.FLOAD1_REG0);
	                break;
	            }
	        } 
	        else 
	        {
	            switch (modrm & 0xf8) {
	            case 0xc0: 
	            case 0xc8: 
	            case 0xe0: 
	            case 0xf0: 
	                processor.pushCode(MICROCODE.FLOAD0_ST0);
	                processor.pushCode(MICROCODE.FLOAD1_STN);
	                processor.pushCode(modrm & 0x07);
	                break;
	            case 0xe8: 
	            case 0xf8: 
	                processor.pushCode(MICROCODE.FLOAD1_ST0);
	                processor.pushCode(MICROCODE.FLOAD0_STN);
	                processor.pushCode(modrm & 0x07);
	                break;
	            }
	            switch (modrm) 
	            {
	            case 0xd9:
	                processor.pushCode(MICROCODE.FLOAD0_ST0);
	                processor.pushCode(MICROCODE.FLOAD1_STN);
	                processor.pushCode(1);
	                break;
	            }
	        }
	        break;            
	
	    case 0xdf:
	        if ((modrm & 0xc0) != 0xc0) 
	        {
	            processor.pushCode(MICROCODE.FWAIT);
	            switch (modrm & 0x38) 
	            {
	            case 0x00:
	                processor.decode_memory(modrm, sib, displacement);
	                processor.pushCode(MICROCODE.LOAD0_MEM_WORD);
	                processor.pushCode(MICROCODE.FLOAD0_REG0);
	                break;
	            case 0x28:
	                processor.decode_memory(modrm, sib, displacement);
	                processor.pushCode(MICROCODE.LOAD0_MEM_QUAD);
	                processor.pushCode(MICROCODE.FLOAD0_REG0L);
	                break;
	            case 0x08:
	            case 0x10:
	            case 0x18:
	            case 0x38: 
	                processor.pushCode(MICROCODE.FLOAD0_ST0); 
	                break;
	            case 0x30:
	                processor.pushCode(MICROCODE.FLOAD0_ST0); 
	                processor.decode_memory(modrm, sib, displacement);
	                break;
	            case 0x20:
	                processor.decode_memory(modrm, sib, displacement);
	                break;
	            }
	        }
	        else 
	        {
	            switch (modrm) 
	            {
	            case 0xe0: processor.pushCode(MICROCODE.LOAD0_FPUSW); break;
	            default: processor.pushCode(MICROCODE.FWAIT); break;
	            }
	            switch (modrm & 0xf8) {
	            case 0xe8:
	            case 0xf0:
	                processor.pushCode(MICROCODE.FLOAD0_ST0);
	                processor.pushCode(MICROCODE.FLOAD1_STN);
	                processor.pushCode(modrm & 0x07);
	                break;
	            }
	        }
	        break;            
		}
		
		//decode operation
		if (opcode==0xd8)
		{
			switch(modrm&0x38)
			{
			case 0x00: processor.pushCode(MICROCODE.OP_FADD); break;
			case 0x08: processor.pushCode(MICROCODE.OP_FMUL); break;
			case 0x10: processor.pushCode(MICROCODE.OP_FCOM); break;
			case 0x18: processor.pushCode(MICROCODE.OP_FCOM); break;
			case 0x20: processor.pushCode(MICROCODE.OP_FSUB); break;
			case 0x28: processor.pushCode(MICROCODE.OP_FSUB); break;
			case 0x30: processor.pushCode(MICROCODE.OP_FDIV); break;
			case 0x38: processor.pushCode(MICROCODE.OP_FDIV); break;
			}
		}
		else if (opcode==0xd9)
		{
			if ((modrm & 0xc0) != 0xc0)
			{
				switch (modrm & 0x38) 
				{
					case 0x00: processor.pushCode(MICROCODE.OP_FPUSH); break;
					case 0x20:
						if (processor.isCode(MICROCODE.PREFIX_OPCODE_32BIT))
							processor.pushCode(MICROCODE.OP_FLDENV_28);
						else
							processor.pushCode(MICROCODE.OP_FLDENV_14);
						break;
					case 0x30:
						if (processor.isCode(MICROCODE.PREFIX_OPCODE_32BIT))
							processor.pushCode(MICROCODE.OP_FSTENV_28);
						else
							processor.pushCode(MICROCODE.OP_FSTENV_14);
						break;
				}
			}
			else
			{
				switch (modrm & 0xf8) 
				{
					case 0xc0: processor.pushCode(MICROCODE.OP_FPUSH); break;
				}                
				switch (modrm) 
				{
			        case 0xd0: break;
			        case 0xe0: processor.pushCode(MICROCODE.OP_FCHS); break;
			        case 0xe1: processor.pushCode(MICROCODE.OP_FABS); break;
			        case 0xe4: processor.pushCode(MICROCODE.OP_FCOM); break;
			        case 0xe5: processor.pushCode(MICROCODE.OP_FXAM); break;
			        case 0xe8: processor.pushCode(MICROCODE.OP_FPUSH); break;
			        case 0xe9: processor.pushCode(MICROCODE.OP_FPUSH); break;
			        case 0xea: processor.pushCode(MICROCODE.OP_FPUSH); break;
			        case 0xeb: processor.pushCode(MICROCODE.OP_FPUSH); break;
			        case 0xec: processor.pushCode(MICROCODE.OP_FPUSH); break;
			        case 0xed: processor.pushCode(MICROCODE.OP_FPUSH); break;
			        case 0xee: processor.pushCode(MICROCODE.OP_FPUSH); break;
			        case 0xf0: processor.pushCode(MICROCODE.OP_F2XM1); break;
			        case 0xf1: processor.pushCode(MICROCODE.OP_FYL2X); break;
			        case 0xf2: processor.pushCode(MICROCODE.OP_FPTAN); break;
			        case 0xf3: processor.pushCode(MICROCODE.OP_FPATAN); break;
			        case 0xf4: processor.pushCode(MICROCODE.OP_FXTRACT); break;
			        case 0xf5: processor.pushCode(MICROCODE.OP_FPREM1); break;
			        case 0xf6: processor.pushCode(MICROCODE.OP_FDECSTP); break;
			        case 0xf7: processor.pushCode(MICROCODE.OP_FINCSTP); break;
			        case 0xf8: processor.pushCode(MICROCODE.OP_FPREM); break;
			        case 0xf9: processor.pushCode(MICROCODE.OP_FYL2XP1); break;
			        case 0xfa: processor.pushCode(MICROCODE.OP_FSQRT); break;
			        case 0xfb: processor.pushCode(MICROCODE.OP_FSINCOS); break;
			        case 0xfc: processor.pushCode(MICROCODE.OP_FRNDINT); break;
			        case 0xfd: processor.pushCode(MICROCODE.OP_FSCALE); break;
			        case 0xfe: processor.pushCode(MICROCODE.OP_FSIN); break;
			        case 0xff: processor.pushCode(MICROCODE.OP_FCOS); break;
				}
			}
		}
		else if (opcode==0xda)
		{
		    if ((modrm & 0xc0) != 0xc0)
		    {
		        switch (modrm & 0x38) 
		        {
		        case 0x00: processor.pushCode(MICROCODE.OP_FADD); break;
		        case 0x08: processor.pushCode(MICROCODE.OP_FMUL); break;
		        case 0x10:
		        case 0x18: processor.pushCode(MICROCODE.OP_FCOM); break;
		        case 0x20:
		        case 0x28: processor.pushCode(MICROCODE.OP_FSUB); break;
		        case 0x30:
		        case 0x38: processor.pushCode(MICROCODE.OP_FDIV); break;
		        }
		    }
		    else
		    {
		        switch (modrm & 0xf8) 
		        {
		        case 0xc0: processor.pushCode(MICROCODE.OP_FCMOVB); break;
		        case 0xc8: processor.pushCode(MICROCODE.OP_FCMOVE); break;
		        case 0xd0: processor.pushCode(MICROCODE.OP_FCMOVBE); break;
		        case 0xd8: processor.pushCode(MICROCODE.OP_FCMOVU); break;
		        }                
		        switch (modrm) 
		        {
		        case 0xe9: processor.pushCode(MICROCODE.OP_FUCOM); break;
		        }
		    }			
		}

		else if (opcode==0xdb)
		{
		    if ((modrm & 0xc0) != 0xc0)
		    {
		        switch (modrm & 0x38) 
		        {
		        case 0x00: processor.pushCode(MICROCODE.OP_FPUSH); break;
		        case 0x08: processor.pushCode(MICROCODE.OP_FCHOP); break;
		        case 0x10:
		        case 0x18: processor.pushCode(MICROCODE.OP_FRNDINT); break;
		        case 0x28: processor.pushCode(MICROCODE.OP_FPUSH); break;
		        case 0x38: break;
		        }
		    }
		    else
		    {
		        switch (modrm & 0xf8) 
		        {
		        case 0xc0: processor.pushCode(MICROCODE.OP_FCMOVNB); break;
		        case 0xc8: processor.pushCode(MICROCODE.OP_FCMOVNE); break;
		        case 0xd0: processor.pushCode(MICROCODE.OP_FCMOVNBE); break;
		        case 0xd8: processor.pushCode(MICROCODE.OP_FCMOVNU); break;
		        case 0xe8: processor.pushCode(MICROCODE.OP_FUCOMI); break;
		        case 0xf0: processor.pushCode(MICROCODE.OP_FCOMI); break;
		        }                
		        switch (modrm) 
		        {
		        case 0xe2: processor.pushCode(MICROCODE.OP_FCLEX); break;
		        case 0xe3: processor.pushCode(MICROCODE.OP_FINIT); break;
		        case 0xe4: break;
		        }
		    }			
		}
		else if (opcode==0xdc)
		{
		    switch (modrm & 0x38) 
		    {
		    case 0x00: processor.pushCode(MICROCODE.OP_FADD); break;
		    case 0x08: processor.pushCode(MICROCODE.OP_FMUL); break;
		    case 0x10: 
		    case 0x18: processor.pushCode(MICROCODE.OP_FCOM); break;
		    case 0x20:
		    case 0x28: processor.pushCode(MICROCODE.OP_FSUB); break;
		    case 0x30:
		    case 0x38: processor.pushCode(MICROCODE.OP_FDIV); break;
		    }			
		}
		else if (opcode==0xdd)
		{
		    if ((modrm & 0xc0) != 0xc0)
		    {
		        switch (modrm & 0x38) 
		        {
		        case 0x00: processor.pushCode(MICROCODE.OP_FPUSH); break;
		        case 0x08: processor.pushCode(MICROCODE.OP_FCHOP); break;
		        case 0x10: 
		        case 0x18:
		        case 0x38: break;
		        case 0x20: 
		            if (processor.isCode(MICROCODE.PREFIX_OPCODE_32BIT))
		                processor.pushCode(MICROCODE.OP_FRSTOR_108); 
		            else
		                processor.pushCode(MICROCODE.OP_FRSTOR_94); 
		            break;
		        case 0x30: 
		            if (processor.isCode(MICROCODE.PREFIX_OPCODE_32BIT))
		                processor.pushCode(MICROCODE.OP_FSAVE_108); 
		            else
		                processor.pushCode(MICROCODE.OP_FSAVE_94); 
		            break;
		        }
		    }
		    else
		    {
		        switch (modrm & 0xf8) 
		        {
		        case 0xc0: processor.pushCode(MICROCODE.OP_FFREE); break;
		        case 0xd0:
		        case 0xd8: break;
		        case 0xe0:
		        case 0xe8: processor.pushCode(MICROCODE.OP_FUCOM); break;
		        }
		    }			
		}
		else if (opcode==0xde)
		{
		    switch (modrm) 
		    {
		    case 0xd9: processor.pushCode(MICROCODE.OP_FCOM); break;
		    default:
		        switch (modrm & 0x38) 
		        {
		        case 0x00: processor.pushCode(MICROCODE.OP_FADD); break;
		        case 0x08: processor.pushCode(MICROCODE.OP_FMUL); break;
		        case 0x10: 
		        case 0x18: processor.pushCode(MICROCODE.OP_FCOM); break;
		        case 0x20:
		        case 0x28: processor.pushCode(MICROCODE.OP_FSUB); break;
		        case 0x30:
		        case 0x38: processor.pushCode(MICROCODE.OP_FDIV); break;
		        }
		    }			
		}
		else if (opcode==0xdf)
		{
		    if ((modrm & 0xc0) != 0xc0)
		    {
		        switch (modrm & 0x38) 
		        {
		        case 0x00: processor.pushCode(MICROCODE.OP_FPUSH); break;
		        case 0x08: processor.pushCode(MICROCODE.OP_FCHOP); break;
		        case 0x10: 
		        case 0x18: 
		        case 0x38: processor.pushCode(MICROCODE.OP_FRNDINT); break;
		        case 0x20: processor.pushCode(MICROCODE.OP_FBCD2F); break;
		        case 0x28: processor.pushCode(MICROCODE.OP_FPUSH); break;
		        case 0x30: processor.pushCode(MICROCODE.OP_FF2BCD); break;
		        }
		    }
		    else
		    {
		        switch (modrm & 0xf8) 
		        {
		        case 0xe8: processor.pushCode(MICROCODE.OP_FUCOMI); break;
		        case 0xf0: processor.pushCode(MICROCODE.OP_FCOMI); break;
		        }
		    }			
		}
		
		pushInstruction(processor.code[processor.codeLength-1]);
		
		//decode output operands
	
		switch(opcode)
		{
	    case 0xd8:
	        switch (modrm & 0x38) 
	        {
	        case 0x00:
	        case 0x08:
	        case 0x20:
	        case 0x28:
	        case 0x30:
	        case 0x38: 
	            processor.pushCode(MICROCODE.FSTORE0_ST0); 
	            processor.pushCode(MICROCODE.OP_FCHECK0);
	            break;
	        case 0x10: break;
	        case 0x18: processor.pushCode(MICROCODE.OP_FPOP); break;
	        }
	        break;            
	
	    case 0xd9:
	        if ((modrm & 0xc0) != 0xc0)
	        {
	            switch (modrm & 0x38) 
	            {
	            case 0x00:
	            case 0x20:
	            case 0x30: 
	                break;
	            case 0x10:
	                processor.decode_memory(modrm, sib, displacement);
	                processor.pushCode(MICROCODE.FSTORE0_MEM_SINGLE);
	                break;
	            case 0x18:
	                processor.decode_memory(modrm, sib, displacement);
	                processor.pushCode(MICROCODE.FSTORE0_MEM_SINGLE);
	                processor.pushCode(MICROCODE.OP_FPOP);
	                break;
	            case 0x28: processor.pushCode(MICROCODE.STORE0_FPUCW); break;
	            case 0x38:
	                processor.decode_memory(modrm, sib, displacement);
	                processor.pushCode(MICROCODE.STORE0_MEM_WORD);
	                break;
	            }
	        }
	        else
	        {
	            switch (modrm & 0xf8) 
	            {
	            case 0xc0: break;
	            case 0xc8:
	                processor.pushCode(MICROCODE.FSTORE0_STN);
	                processor.pushCode(modrm & 0x07);
	                processor.pushCode(MICROCODE.FSTORE1_ST0);
	                break;
	            }
	            switch (modrm) 
	            {
	            case 0xd0:
	            case 0xe4:
	            case 0xe5:
	            case 0xe8:
	            case 0xe9:
	            case 0xea:
	            case 0xeb:
	            case 0xec:
	            case 0xed:
	            case 0xee:
	            case 0xf6:
	            case 0xf7: break;
	            case 0xe0:
	            case 0xe1:
	            case 0xfe:
	            case 0xff: processor.pushCode(MICROCODE.FSTORE0_ST0); break;
	            case 0xf0:
	            case 0xf5:
	            case 0xf8:
	            case 0xfa:
	            case 0xfc:
	            case 0xfd:
	                processor.pushCode(MICROCODE.FSTORE0_ST0); 
	                processor.pushCode(MICROCODE.OP_FCHECK0); 
	                break;
	            case 0xf2:
	                processor.pushCode(MICROCODE.FSTORE0_ST0);
	                processor.pushCode(MICROCODE.FLOAD0_1);
	                processor.pushCode(MICROCODE.OP_FPUSH);
	                break;
	            case 0xf1:
	            case 0xf3:
	                processor.pushCode(MICROCODE.OP_FPOP);
	                processor.pushCode(MICROCODE.FSTORE0_ST0);
	                break;
	            case 0xf9:
	                processor.pushCode(MICROCODE.OP_FPOP);
	                processor.pushCode(MICROCODE.FSTORE0_ST0);
	                processor.pushCode(MICROCODE.OP_FCHECK0); 
	                break;
	            case 0xf4:
	                processor.pushCode(MICROCODE.FSTORE1_ST0);
	                processor.pushCode(MICROCODE.OP_FPUSH);
	                break;
	            case 0xfb:
	                processor.pushCode(MICROCODE.FSTORE1_ST0);
	                processor.pushCode(MICROCODE.OP_FPUSH);
	                processor.pushCode(MICROCODE.OP_FCHECK0); 
	                processor.pushCode(MICROCODE.OP_FCHECK1); 
	                break;
	            }
	        }
	        break;            
	
	    case 0xda:
	        if ((modrm & 0xc0) != 0xc0) 
	        {
	            switch (modrm & 0x38) 
	            {
	            case 0x00:
	            case 0x08:
	            case 0x20:
	            case 0x28:
	            case 0x30:
	            case 0x38:
	                processor.pushCode(MICROCODE.FSTORE0_ST0); 
	                processor.pushCode(MICROCODE.OP_FCHECK0); 
	                break;
	            case 0x10: break;
	            case 0x18: processor.pushCode(MICROCODE.OP_FPOP); break;
	            }
	        } 
	        else 
	        {
	            switch (modrm) 
	            {
	            case 0xe9:
	                processor.pushCode(MICROCODE.OP_FPOP);
	                processor.pushCode(MICROCODE.OP_FPOP);
	                break;
	            }
	        }
	        break;            
	
	    case 0xdb:
	        if ((modrm & 0xc0) != 0xc0) 
	        {
	            switch (modrm & 0x38) 
	            {
	            case 0x00:
	            case 0x28: break;
	            case 0x10:
	                processor.decode_memory(modrm, sib, displacement);
	                processor.pushCode(MICROCODE.STORE0_MEM_DOUBLE);
	                break;
	            case 0x08:
	            case 0x18:
	                processor.decode_memory(modrm, sib, displacement);
	                processor.pushCode(MICROCODE.STORE0_MEM_DOUBLE);
	                processor.pushCode(MICROCODE.OP_FPOP);
	                break;
	            case 0x38:
	                processor.decode_memory(modrm, sib, displacement);
	                processor.pushCode(MICROCODE.FSTORE0_MEM_EXTENDED); 
	                processor.pushCode(MICROCODE.OP_FPOP); 
	                break;
	            }
	        } 
	        break;            
	
	    case 0xdc:
	        if ((modrm & 0xc0) != 0xc0) 
	        {
	            switch (modrm & 0x38) 
	            {
	            case 0x00:
	            case 0x08:
	            case 0x20:
	            case 0x28:
	            case 0x30:
	            case 0x38: 
	                processor.pushCode(MICROCODE.FSTORE0_ST0); 
	                processor.pushCode(MICROCODE.OP_FCHECK0); 
	                break;
	            case 0x10: break;
	            case 0x18: processor.pushCode(MICROCODE.OP_FPOP); break;
	            }
	        }
	        else
	        {
	            switch (modrm & 0xf8) 
	            {
	            case 0xc0: 
	            case 0xc8: 
	            case 0xe0: 
	            case 0xe8: 
	            case 0xf0: 
	            case 0xf8: 
	                processor.pushCode(MICROCODE.FSTORE0_STN); 
	                processor.pushCode(modrm & 0x07);
	                processor.pushCode(MICROCODE.OP_FCHECK0); 
	                break;
	            }
	        }
	        break;            
	
	    case 0xdd:
	        if ((modrm & 0xc0) != 0xc0) 
	        {
	            switch (modrm & 0x38) 
	            {
	            case 0x00:
	            case 0x20:
	            case 0x30: break;
	            case 0x08:
	                processor.decode_memory(modrm, sib, displacement);
	                processor.pushCode(MICROCODE.STORE0_MEM_QUAD);
	                processor.pushCode(MICROCODE.OP_FPOP);
	                break;
	            case 0x10:
	                processor.decode_memory(modrm, sib, displacement);
	                processor.pushCode(MICROCODE.FSTORE0_MEM_DOUBLE);
	                break;
	            case 0x18:
	                processor.decode_memory(modrm, sib, displacement);
	                processor.pushCode(MICROCODE.FSTORE0_MEM_DOUBLE);
	                processor.pushCode(MICROCODE.OP_FPOP);
	                break;
	            case 0x38:
	                processor.decode_memory(modrm, sib, displacement);
	                processor.pushCode(MICROCODE.STORE0_MEM_WORD); 
	                break;
	            }
	        } 
	        else 
	        {
	            switch (modrm & 0xf8) 
	            {
	            case 0xc0:
	            case 0xe0: break;
	            case 0xd0:
	                processor.pushCode(MICROCODE.FSTORE0_STN);
	                processor.pushCode(modrm & 0x07);
	                break;
	            case 0xd8:
	                processor.pushCode(MICROCODE.FSTORE0_STN);
	                processor.pushCode(modrm & 0x07);
	                processor.pushCode(MICROCODE.OP_FPOP);
	                break;
	            case 0xe8: processor.pushCode(MICROCODE.OP_FPOP); break;
	            }
	        }
	        break;            
	
	    case 0xde:
	        if ((modrm & 0xc0) != 0xc0) 
	        {
	            switch (modrm & 0x38) 
	            {
	            case 0x00:
	            case 0x08:
	            case 0x20:
	            case 0x28:
	            case 0x30:
	            case 0x38: 
	                processor.pushCode(MICROCODE.FSTORE0_ST0); 
	                processor.pushCode(MICROCODE.OP_FCHECK0); 
	                break;
	            case 0x10: break;
	            case 0x18: processor.pushCode(MICROCODE.OP_FPOP); break;
	            }
	        } 
	        else 
	        {
	            switch (modrm & 0xf8) 
	            {
	            case 0xc0:
	            case 0xc8:
	            case 0xe0:
	            case 0xe8:
	            case 0xf0:
	            case 0xf8:
	                processor.pushCode(MICROCODE.FSTORE0_STN);
	                processor.pushCode(modrm & 0x07);
	                processor.pushCode(MICROCODE.OP_FPOP);
	                processor.pushCode(MICROCODE.OP_FCHECK0); 
	                break;
	            case 0xd0:
	            case 0xd8: break;
	            }
	            switch (modrm) 
	            {
	            case 0xd9: 
	                processor.pushCode(MICROCODE.OP_FPOP);
	                processor.pushCode(MICROCODE.OP_FPOP);
	                break;
	            }
	        }
	        break;            
	
	    case 0xdf:
	        if ((modrm & 0xc0) != 0xc0) 
	        {
	            switch (modrm & 0x38) 
	            {
	            case 0x00:
	            case 0x20:
	            case 0x28:
	            case 0x30: break;
	            case 0x08: 
	            case 0x18:
	                processor.decode_memory(modrm, sib, displacement);
	                processor.pushCode(MICROCODE.STORE0_MEM_WORD);
	                processor.pushCode(MICROCODE.OP_FPOP);
	                break;
	            case 0x10:
	                processor.decode_memory(modrm, sib, displacement);
	                processor.pushCode(MICROCODE.STORE0_MEM_WORD);
	                break;
	            case 0x38:
	                processor.decode_memory(modrm, sib, displacement);
	                processor.pushCode(MICROCODE.STORE0_MEM_QUAD); 
	                processor.pushCode(MICROCODE.OP_FPOP);
	                break;
	            }
	        } 
	        else 
	        {
	            switch (modrm & 0xf8) 
	            {
	            case 0xe8:
	            case 0xf0: processor.pushCode(MICROCODE.OP_FPOP); break;
	            }
	            switch (modrm) 
	            {
	            case 0xe0: processor.pushCode(MICROCODE.STORE0_AX); break;
	            }
	        }
	        break;            

		}
	}
	
	private void pushInstruction(MICROCODE microcode)
	{
	String name="";
	switch(microcode)
	{
	case OP_FPOP: name="fpop"; break; 
	case OP_FPUSH: name="fpush"; break; 
	case OP_FADD: name="fadd"; break; 
	case OP_FMUL: name="fmul"; break; 
	case OP_FCOM: name="fcom"; break; 
	case OP_FUCOM: name="fucom"; break; 
	case OP_FCOMI: name="fcomi"; break; 
	case OP_FUCOMI: name="fucomi"; break; 
	case OP_FSUB: name="fsub"; break; 
	case OP_FDIV: name="fdiv"; break; 
	case OP_FCHS: name="fchs"; break; 
	case OP_FABS: name="fabs"; break; 
	case OP_FXAM: name="fxam"; break; 
	case OP_F2XM1: name="f2xm1"; break; 
	case OP_FYL2X: name="fyl2x"; break; 
	case OP_FPTAN: name="fptan"; break; 
	case OP_FPATAN: name="fpatan"; break; 
	case OP_FXTRACT: name="fxtract"; break; 
	case OP_FPREM1: name="fprem1"; break; 
	case OP_FDECSTP: name="fdecstp"; break; 
	case OP_FINCSTP: name="fincstp"; break; 
	case OP_FPREM: name="fprem"; break; 
	case OP_FYL2XP1: name="fyl2xp1"; break; 
	case OP_FSQRT: name="fsqrt"; break; 
	case OP_FSINCOD: name="fsincod"; break; 
	case OP_FRNDINT: name="frndint"; break; 
	case OP_FSCALE: name="fscale"; break; 
	case OP_FSIN: name="fsin"; break; 
	case OP_FCOS: name="fcos"; break; 
	case OP_FRSTOR_94: name="frstor_94"; break; 
	case OP_FRSTOR_108: name="frstor_108"; break; 
	case OP_FSAVE_94: name="fsave_94"; break; 
	case OP_FSAVE_108: name="fsave_108"; break; 
	case OP_FFREE: name="ffree"; break; 
	case OP_FBCD2F: name="fbcd2f"; break; 
	case OP_FF2BCD: name="ff2bcd"; break; 
	case OP_FLDENV_14: name="fldenv_14"; break; 
	case OP_FLDENV_28: name="fldenv_28"; break; 
	case OP_FSTENV_14: name="fstenv_14"; break; 
	case OP_FSTENV_28: name="fstenv_28"; break; 
	case OP_FCMOVB: name="fcmovb"; break; 
	case OP_FCMOVE: name="fcmove"; break; 
	case OP_FCMOVBE: name="fcmovbe"; break; 
	case OP_FCMOVU: name="fcmovu"; break; 
	case OP_FCMOVNB: name="fmovnb"; break; 
	case OP_FCMOVNE: name="fcmovne"; break; 
	case OP_FCMOVNBE: name="fcmovnbe"; break; 
	case OP_FCMOVNU: name="fcmovnu"; break; 
	case OP_FCHOP: name="fchop"; break; 
	case OP_FCLEX: name="fclex"; break; 
	case OP_FINIT: name="finit"; break; 
	case OP_FCHECK0: name="fcheck0"; break; 
	case OP_FCHECK1: name="fcheck1"; break; 
	case OP_FXSAVE: name="fxsave"; break; 
	case OP_FSINCOS: name="fsincos"; break; 
	}
	if(processor.processorGUICode!=null) processor.processorGUICode.push(GUICODE.DECODE_INSTRUCTION, name);		
	}
	
    private static final double L2TEN = Math.log(10)/Math.log(2);
    private static final double L2E = 1/Math.log(2);
    private static final double LOG2 = Math.log(2)/Math.log(10);
    private static final double LN2 = Math.log(2);
    private static final double POS0 = Double.longBitsToDouble(0x0l);

    //these are passed back to processor execute
	public int reg0, reg1, reg2, addr, displacement;
	public boolean condition;
	public Processor.Segment seg;
	public long reg0l;
	
	//these are used in executeFloat
    double freg0 = 0.0, freg1 = 0.0;
	
    public void newExecuteFloat()
    {
    	freg0=0.1; freg1=0.0;
    }
    
	public boolean executeFloat(MICROCODE microcode, int reg0, int reg1, int reg2, int addr0, long reg0l, Processor.Segment seg0, boolean condition, int displacement)
	{
		this.reg0=reg0; this.reg1=reg1; this.reg2=reg2; this.addr=addr0; this.reg0l=reg0l; this.seg=seg0; this.condition=condition; this.displacement=displacement;
		
		switch(microcode)
		{
		case FWAIT:
			checkExceptions();
			break;
		case FLOAD0_ST0: 
			freg0 = ST(0);
			validateOperand(freg0);
			break;
    case FLOAD0_STN:
        freg0 = ST(processor.getiCode());
        validateOperand(freg0);
        break;
    case FLOAD0_MEM_SINGLE: {
        //     0x7f800001 thru 0x7fbfffff // SNaN Singalling
        //     0x7fc00000 thru 0x7fffffff // QNaN Quiet
        //     0xff800001 thru 0xffbfffff // SNaN Signalling
        //     0xffc00000 thru 0xffffffff // QNaN Quiet
        int n = seg0.loadDoubleWord(addr0);
        freg0 = Float.intBitsToFloat(n);
        if ((Double.isNaN(freg0)) && ((n & (1 << 22)) == 0))
            setInvalidOperation();
        validateOperand(freg0);               
    }   break;
    case FLOAD0_MEM_DOUBLE: {
        long n = seg0.loadQuadWord(addr0);
        freg0 = Double.longBitsToDouble(n);
        if ((Double.isNaN(freg0)) && ((n & (0x01l << 51)) == 0))
            setInvalidOperation();
        validateOperand(freg0);               
    }   break;
    case FLOAD0_MEM_EXTENDED:{
        byte[] b = new byte[10];
        for (int i=0; i<10; i++)
            b[i] = seg0.loadByte(addr0 + i);
        freg0 = extendedToDouble(b);
        if ((Double.isNaN(freg0)) && ((Double.doubleToLongBits(freg0) & (0x01l << 51)) == 0))
            setInvalidOperation();
        validateOperand(freg0);}  
        break;
    case FLOAD0_REG0:
        freg0 = (double) reg0; 
        validateOperand(freg0);
        break;
    case FLOAD0_REG0L:
        freg0 = (double) reg0l; 
        validateOperand(freg0);
        break;
    case FLOAD0_1:
        freg0 = 1.0; 
//         validateOperand(freg0);
        break;
    case FLOAD0_L2TEN:
        freg0 = L2TEN; 
//         validateOperand(freg0);
        break;
    case FLOAD0_L2E:
        freg0 = L2E; 
//         validateOperand(freg0);
        break;
    case FLOAD0_PI:
        freg0 = Math.PI; 
//         validateOperand(freg0);
        break;
    case FLOAD0_LOG2:
        freg0 = LOG2; 
//         validateOperand(freg0);
        break;
    case FLOAD0_LN2:
        freg0 = LN2; 
//         validateOperand(freg0);
        break;
    case FLOAD0_POS0:
        freg0 = POS0; 
//         validateOperand(freg0);
        break;
    case FLOAD1_POS0:
        freg1 = POS0;
        break;
    case OP_FCLEX:
        checkExceptions();
        clearExceptions();
        break;
    case FLOAD1_ST0:
        freg1 = ST(0); 
        validateOperand(freg1);
        break;
    case FLOAD1_STN:
        freg1 = ST(processor.getiCode()); 
        validateOperand(freg1);
        break;
    case FLOAD1_MEM_SINGLE: {
        int n = seg0.loadDoubleWord(addr0);
        freg1 = Float.intBitsToFloat(n);
        if ((Double.isNaN(freg1)) && ((n & (1 << 22)) == 0))
            setInvalidOperation();
        validateOperand(freg1);
    }   break;
    case FLOAD1_MEM_DOUBLE: {
        long n = seg0.loadQuadWord(addr0);
        freg1 = Double.longBitsToDouble(n);
        if ((Double.isNaN(freg1)) && ((n & (0x01l << 51)) == 0))
            setInvalidOperation();
        validateOperand(freg1);
    }   break;
    case FLOAD1_REG0:
        freg1 = (double) reg0; 
        validateOperand(freg1);
        break;
    case FLOAD1_REG0L:
        freg1 = (double) reg0l;
        validateOperand(freg1);
        break;

    case FSTORE0_ST0: setST(0, freg0); break;
    case FSTORE0_STN: setST(processor.getiCode(), freg0); break;
    case FSTORE0_MEM_SINGLE: {
        int n = Float.floatToRawIntBits((float) freg0);
        seg0.storeDoubleWord(addr0, n); 
    }   break;
    case FSTORE0_MEM_DOUBLE: {
        long n = Double.doubleToRawLongBits(freg0);
        seg0.storeQuadWord(addr0, n); 
    }   break;
    case FSTORE0_REG0: reg0 = (int) freg0; break;
    case FSTORE0_MEM_EXTENDED:{
        byte[] b = doubleToExtended(freg0, false);
        for (int i=0; i<10; i++)
            seg0.storeByte(addr0+i, b[i]);}
        break;
    
    case FSTORE1_ST0: setST(0, freg1); break;
    
    case FSTORE1_STN: setST(processor.getiCode(), freg1); break;
    case FSTORE1_MEM_SINGLE: {
        int n = Float.floatToRawIntBits((float) freg1);
        seg0.storeDoubleWord(addr0, n); 
    }   break;
    case FSTORE1_MEM_DOUBLE: {
        long n = Double.doubleToRawLongBits(freg1);
        seg0.storeQuadWord(addr0, n); 
    }   break;
    case FSTORE1_REG0: reg0 = (int) freg1; break;
    
    case STORE0_FPUCW: setControl(reg0); break;
    case LOAD0_FPUCW: reg0 = getControl(); break;

    case STORE0_FPUSW: setStatus(reg0); break;
    case LOAD0_FPUSW: reg0 = getStatus(); break;

    case OP_FCOM: {
        int newcode = 0xd;
        if (Double.isNaN(freg0) || Double.isNaN(freg1))
            setInvalidOperation();
        else {
            if (freg0 > freg1) newcode = 0;
            else if (freg0 < freg1) newcode = 1;
            else newcode = 8;
        }
        conditionCode &= 2;
        conditionCode |= newcode;
    } break;
    case OP_FCOMI: {
        int newcode = 0xd;
        if (Double.isNaN(freg0) || Double.isNaN(freg1))
            setInvalidOperation();
        else {
            if (freg0 > freg1) newcode = 0;
            else if (freg0 < freg1) newcode = 1;
            else newcode = 8;
        }
        conditionCode &= 2;
        conditionCode |= newcode;
    } break;
    case OP_FUCOM: {
        int newcode = 0xd;
        if (!(Double.isNaN(freg0) || Double.isNaN(freg1)))
        {
            if (freg0 > freg1) newcode = 0;
            else if (freg0 < freg1) newcode = 1;
            else newcode = 8;
        }
        conditionCode &= 2;
        conditionCode |= newcode;
    } break;
    case OP_FUCOMI:
        int newcode = 0xd;
        if (!(Double.isNaN(freg0) || Double.isNaN(freg1)))
        {
            if (freg0 > freg1) newcode = 0;
            else if (freg0 < freg1) newcode = 1;
            else newcode = 8;
        }
        conditionCode &= 2;
        conditionCode |= newcode;
        break;
    case OP_FPOP: pop(); break;
    case OP_FPUSH: push(freg0); break;
    
    case OP_FCHS: freg0 = -freg0; break;
    case OP_FABS: freg0 = Math.abs(freg0); break;
    case OP_FXAM:
        int result = specialTagCode(ST(0));
        conditionCode = result; //wrong
        break;
    case OP_F2XM1: //2^x -1
        setST(0,Math.pow(2.0,ST(0))-1);
        break;
    case OP_FADD: {
        if ((freg0 == Double.NEGATIVE_INFINITY && freg1 == Double.POSITIVE_INFINITY) || (freg0 == Double.POSITIVE_INFINITY && freg1 == Double.NEGATIVE_INFINITY))
            setInvalidOperation();
        freg0 = freg0 + freg1;
    } break;

    case OP_FMUL: {
        if ((Double.isInfinite(freg0) && (freg1 == 0.0)) || (Double.isInfinite(freg1) && (freg0 == 0.0))) 
            setInvalidOperation();
        freg0 = freg0 * freg1;
    } break;

    case OP_FSUB: {
        if ((freg0 == Double.NEGATIVE_INFINITY && freg1 == Double.NEGATIVE_INFINITY) || (freg0 == Double.POSITIVE_INFINITY && freg1 == Double.POSITIVE_INFINITY)) 
            setInvalidOperation();
        freg0 = freg0 - freg1;
    } break;
    case OP_FDIV: {
        if (((freg0 == 0.0) && (freg1 == 0.0)) || (Double.isInfinite(freg0) && Double.isInfinite(freg1)))
            setInvalidOperation();
        if ((freg1 == 0.0) && !Double.isNaN(freg0) && !Double.isInfinite(freg0))
            setZeroDivide();
        freg0 = freg0 / freg1;
    } break;


case OP_FSQRT: {
if (freg0 < 0)
setInvalidOperation();
freg0 = Math.sqrt(freg0);
} break;

case OP_FSIN: {
if (Double.isInfinite(freg0))
setInvalidOperation();
if ((freg0 > Long.MAX_VALUE) || (freg0 < Long.MIN_VALUE))
conditionCode |= 4; // set C2
else
freg0 = Math.sin(freg0);
} break;

case OP_FCOS: {
if (Double.isInfinite(freg0))
setInvalidOperation();
if ((freg0 > Long.MAX_VALUE) || (freg0 < Long.MIN_VALUE))
conditionCode |= 4; // set C2
else
freg0 = Math.cos(freg0);
} break;
    case OP_FFREE:
        {
            setTagEmpty(reg0);
        } break;
    case OP_FBCD2F: {
        long n = 0;
        long decade = 1;
        for (int i = 0; i < 9; i++) 
        {
            byte b = seg0.loadByte(addr0 + i);
            n += (b & 0xf) * decade; 
            decade *= 10;
            n += ((b >> 4) & 0xf) * decade; 
            decade *= 10;
        }
        byte sign = seg0.loadByte(addr0 + 9);
        double m = (double)n;
        if (sign < 0)
            m *= -1.0;
        freg0 = m;
    } break;

    case OP_FF2BCD: {
        long n = (long)Math.abs(freg0);
        long decade = 1;
        for (int i = 0; i < 9; i++) 
        {
            int val = (int) ((n % (decade * 10)) / decade);
            byte b = (byte) val;
            decade *= 10;
            val = (int) ((n % (decade * 10)) / decade);
            b |= (val << 4);
            seg0.storeByte(addr0 + i, b);
        }
        seg0.storeByte(addr0 + 9,  (freg0 < 0) ? (byte)0x80 : (byte)0x00);
    } break;

    case OP_FSTENV_14:
    	seg0.storeWord(addr0, (short) getControl());
        seg0.storeWord(addr0 + 2, (short) getStatus());
        seg0.storeWord(addr0 + 4, (short) getTagWord());
        seg0.storeWord(addr0 + 6, (short) 0 /* getIP()  offset*/);
        seg0.storeWord(addr0 + 8, (short) 0 /* (selector & 0xFFFF)*/);
        seg0.storeWord(addr0 + 10, (short) 0 /* operand pntr offset*/);
        seg0.storeWord(addr0 + 12, (short) 0 /* operand pntr selector & 0xFFFF*/);
    break;
    case OP_FLDENV_14:
        setControl(seg0.loadWord(addr0));
        setStatus(seg0.loadWord(addr0 + 2));
        setTagWord(seg0.loadWord(addr0 + 4));
     break;
    case OP_FSTENV_28:
        seg0.storeDoubleWord(addr0, getControl() & 0xffff);
        seg0.storeDoubleWord(addr0 + 4, getStatus() & 0xffff);
        seg0.storeDoubleWord(addr0 + 8, getTagWord() & 0xffff);
        seg0.storeDoubleWord(addr0 + 12, 0 /* getIP() */);
        seg0.storeDoubleWord(addr0 + 16, 0 /* ((opcode  << 16) & 0x7FF ) + (selector & 0xFFFF)*/);
        seg0.storeDoubleWord(addr0 + 20, 0 /* operand pntr offset*/);
        seg0.storeDoubleWord(addr0 + 24, 0 /* operand pntr selector & 0xFFFF*/);
    break;
    case OP_FLDENV_28:
       setControl(seg0.loadDoubleWord(addr0));
        setStatus(seg0.loadDoubleWord(addr0 + 4));
        setTagWord(seg0.loadDoubleWord(addr0 + 8));
    break;
    case OP_FPATAN: freg0 = Math.atan2(freg1, freg0); break;
    case OP_FPREM: {
        int d = Math.getExponent(freg0) - Math.getExponent(freg1);
        if (d < 64)
        {
            // full remainder
            conditionCode &= ~4; // clear C2
            freg0 = freg0 % freg1;
            // compute least significant bits -> C0 C3 C1
            long i = (long)Math.rint(freg0 / freg1);
            conditionCode &= 4;
            if ((i & 1) != 0) conditionCode |= 2;
            if ((i & 2) != 0) conditionCode |= 8;
            if ((i & 4) != 0) conditionCode |= 1;
        }
        else
        {
            // partial remainder
            conditionCode |= 4; // set C2
            int n = 63; // implementation dependent in manual
            double f = Math.pow(2.0, (double)(d - n));
            double z = (freg0 / freg1) / f;
            double qq = (z < 0) ? Math.ceil(z) : Math.floor(z);
            freg0 = freg0 - (freg1 * qq * f);
        }
    } break;
    case OP_FPREM1: {
        int d = Math.getExponent(freg0) - Math.getExponent(freg1);
        if (d < 64)
        {
            // full remainder
            conditionCode &= ~4; // clear C2
            double z = Math.IEEEremainder(freg0, freg1);
            // compute least significant bits -> C0 C3 C1
            long i = (long)Math.rint(freg0 / freg1);
            conditionCode &= 4;
            if ((i & 1) != 0) conditionCode |= 2;
            if ((i & 2) != 0) conditionCode |= 8;
            if ((i & 4) != 0) conditionCode |= 1;
            setST(0, z);
        }
        else
        {
            // partial remainder
            conditionCode |= 4; // set C2
            int n = 63; // implementation dependent in manual
            double f = Math.pow(2.0, (double)(d - n));
            double z = (freg0 / freg1) / f;
            double qq = (z < 0) ? Math.ceil(z) : Math.floor(z);
            freg0 = freg0 - (freg1 * qq * f);
        }
    } break;

    case OP_FPTAN: {
        if ((freg0 > Math.pow(2.0, 63.0)) || (freg0 < -1.0*Math.pow(2.0, 63.0))) {
            if (Double.isInfinite(freg0))
                setInvalidOperation();
            conditionCode |= 4;
        } else 
        {
            conditionCode &= ~4;
            freg0 = Math.tan(freg0);
        }
    } break;
    case OP_FSCALE: freg0 = Math.scalb(freg0, (int) freg1); break;
     case OP_FSINCOS: {
         freg1 = Math.sin(freg0);
         freg0 = Math.cos(freg0);
     } break;
    case OP_FXTRACT: {
        int e = Math.getExponent(freg0);
        freg1 = (double) e;
        freg0 = Math.scalb(freg0, -e);
    } break;
     case OP_FYL2X: {
         if (freg0 < 0)
             setInvalidOperation();
         else if  (Double.isInfinite(freg0))
         {
             if (freg1 == 0)
                setInvalidOperation();
             else if (freg1 > 0)
                 freg1 = freg0;
             else
                 freg1 = -freg0;
         }
         else if ((freg0 == 1) && (Double.isInfinite(freg1)))
             setInvalidOperation();
         else if (freg0 == 0)
         {
             if (freg1 == 0)
                setInvalidOperation();
             else if (!Double.isInfinite(freg1))
                 setZeroDivide();
             else
                 freg1 = -freg1;
         }
         else if (Double.isInfinite(freg1))
         {
             if (freg0 < 1)
                 freg1 = -freg1;
         }
         else
            freg1 = freg1 * Math.log(freg0)/LN2;
         freg0 = freg1;
     } break;
     case OP_FYL2XP1: {
         if (freg0 == 0)
         {
             if (Double.isInfinite(freg1))
                 setInvalidOperation();
             else freg1 = 0;
         }
         else if (Double.isInfinite(freg1))
         {
            if (freg0 < 0)
                freg1 = -freg1;
         }
         else
            freg1 = freg1 * Math.log(freg0 + 1.0)/LN2;
         freg0 = freg1;
     } break;
        

    case OP_FRNDINT: {
        if (Double.isInfinite(freg0)) 
            break; // preserve infinities
    
        switch(getRoundingControl())
        {
        case FPU_ROUNDING_CONTROL_EVEN:
            freg0 = Math.rint(freg0);
            break;
        case FPU_ROUNDING_CONTROL_DOWN:
            freg0 = Math.floor(freg0);
            break;
        case FPU_ROUNDING_CONTROL_UP:
            freg0 = Math.ceil(freg0);
            break;
        case FPU_ROUNDING_CONTROL_TRUNCATE:
            freg0 = Math.signum(freg0) * Math.floor(Math.abs(freg0));
            break;
        default:
            throw new IllegalStateException("Invalid rounding control value");
        }
        reg0 = (int)freg0;
        reg0l = (long)freg0;
    } break;

    case OP_FCHECK0: checkResult(freg0); break;
    case OP_FCHECK1: checkResult(freg1); break;

    case OP_FINIT: init(); break;

//   case CPL_CHECK: if (cpu.getCPL() != 0) throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);//ProcessorException.GENERAL_PROTECTION_0;
//    break;
    case OP_FSAVE_94: {
        seg0.storeWord(addr0, (short) getControl());
        seg0.storeWord(addr0 + 2, (short) getStatus());
        seg0.storeWord(addr0 + 4, (short) getTagWord());
        seg0.storeWord(addr0 + 6, (short) 0 /* getIP()  offset*/);
        seg0.storeWord(addr0 + 8, (short) 0 /* (selector & 0xFFFF)*/);
        seg0.storeWord(addr0 + 10, (short) 0 /* operand pntr offset*/);
        seg0.storeWord(addr0 + 12, (short) 0 /* operand pntr selector & 0xFFFF*/);

        for (int i = 0; i < 8; i++) {
            byte[] extended = doubleToExtended(ST(i), false /* this is WRONG!!!!!!! */);
            for (int j = 0; j < 10; j++)
                seg0.storeByte(addr0 + 14 + j + (10 * i), extended[j]);
        }
        init();
     } break;
     case OP_FRSTOR_94: {
        setControl(seg0.loadWord(addr0));
        setStatus(seg0.loadWord(addr0 + 2));
        setTagWord(seg0.loadWord(addr0 + 4));
     } break;
     case OP_FSAVE_108: {
         seg0.storeDoubleWord(addr0, getControl() & 0xffff);
         seg0.storeDoubleWord(addr0 + 4, getStatus() & 0xffff);
         seg0.storeDoubleWord(addr0 + 8, getTagWord() & 0xffff);
         seg0.storeDoubleWord(addr0 + 12, 0 /* getIP() */);
         seg0.storeDoubleWord(addr0 + 16, 0 /* opcode + selector*/);
         seg0.storeDoubleWord(addr0 + 20, 0 /* operand pntr */);
         seg0.storeDoubleWord(addr0 + 24, 0 /* more operand pntr */);

         for (int i = 0; i < 8; i++) {
             byte[] extended = doubleToExtended(ST(i), false /* this is WRONG!!!!!!! */);
             for (int j = 0; j < 10; j++)
                 seg0.storeByte(addr0 + 28 + j + (10 * i), extended[j]);
         }
         init();
     } break;
     case OP_FRSTOR_108: {
         setControl(seg0.loadDoubleWord(addr0));
         setStatus(seg0.loadDoubleWord(addr0 + 4));
         setTagWord(seg0.loadDoubleWord(addr0 + 8));
		
     }		
     break;
     default:
    	 return false;
		}
    	 return true;
	}

	
	
    // stack depth (common to all x87 FPU's)
    public final static int STACK_DEPTH = 8;

    public static final int FPU_PRECISION_CONTROL_SINGLE = 0;
    public static final int FPU_PRECISION_CONTROL_DOUBLE = 2;
    public static final int FPU_PRECISION_CONTROL_EXTENDED = 3;

    public static final int FPU_ROUNDING_CONTROL_EVEN = 0;
    public static final int FPU_ROUNDING_CONTROL_DOWN = 1;
    public static final int FPU_ROUNDING_CONTROL_UP = 2;
    public static final int FPU_ROUNDING_CONTROL_TRUNCATE = 3;

    public static final int FPU_TAG_VALID = 0;
    public static final int FPU_TAG_ZERO = 1;
    public static final int FPU_TAG_SPECIAL = 2;
    public static final int FPU_TAG_EMPTY = 3;

    public final static int FPU_SPECIAL_TAG_NONE = 0;
    public final static int FPU_SPECIAL_TAG_NAN = 1;
    public final static int FPU_SPECIAL_TAG_UNSUPPORTED = 2;
    public final static int FPU_SPECIAL_TAG_INFINITY = 3;
    public final static int FPU_SPECIAL_TAG_DENORMAL = 4;
    public final static int FPU_SPECIAL_TAG_SNAN = 5;

    public final static double UNDERFLOW_THRESHOLD = Math.pow(2.0, -1022.0);

	
    double[] data;
    int[] tag;
    int[] specialTag;

    // status word

    private int statusWord;

	
    public boolean getInvalidOperation() { return ((statusWord & 0x01) != 0); }
    public boolean getDenormalizedOperand() { return ((statusWord&0x02) != 0); }
    public boolean getZeroDivide() { return ((statusWord & 0x04) != 0); }
    public boolean getOverflow() { return ((statusWord & 0x08) != 0); }
    public boolean getUnderflow() { return ((statusWord & 0x10) != 0); }
    public boolean getPrecision() { return ((statusWord & 0x20) != 0); }
    public boolean getStackFault() { return ((statusWord & 0x40) != 0); }

    public void setInvalidOperation() { statusWord |= 0x01;}
    public void setDenormalizedOperand() { statusWord |= 0x02;}
    public void setZeroDivide() { statusWord |= 0x04;}
    public void setOverflow() { statusWord |= 0x08;}
    public void setUnderflow() {statusWord |= 0x10;}
    public void setPrecision() { statusWord |= 0x20;}
    public void setStackFault() { statusWord |= 0x40;}

    public boolean getBusy() { return getErrorSummaryStatus(); }

    public boolean getErrorSummaryStatus()
    {
        // (note stack fault is a subset of invalid operation)
        return (((statusWord & 0x3f) & ~maskWord) != 0);
    }

    public void checkExceptions() throws Processor.Processor_Exception
    {
 //       if (getErrorSummaryStatus())
//	    cpu.reportFPUException();
    }

    public void clearExceptions() { statusWord = 0; }

    // control word

    private int maskWord;
    private int precisionControl;
    private int roundingControl;

    public boolean getInvalidOperationMask() { return ((maskWord & 1) != 0); }
    public boolean getDenormalizedOperandMask() { return ((maskWord & 2) != 0);}
    public boolean getZeroDivideMask() { return ((maskWord & 4) != 0); }
    public boolean getOverflowMask() { return ((maskWord & 8) != 0); }
    public boolean getUnderflowMask() { return ((maskWord & 0x10) != 0); }
    public boolean getPrecisionMask() { return ((maskWord & 0x20) != 0); }
    public int getPrecisionControl() { return precisionControl; }
    public int getRoundingControl() { return roundingControl; }

    public void setInvalidOperationMask(boolean value)
    { 
        if (value) maskWord |= 1;
        else maskWord &= ~1;
    }

    public void setDenormalizedOperandMask(boolean value)
    { 
        if (value) maskWord |= 2;
        else maskWord &= ~2;
    }

    public void setZeroDivideMask(boolean value)
    { 
        if (value) maskWord |= 4;
        else maskWord &= ~4;
    }

    public void setOverflowMask(boolean value)
    { 
        if (value) maskWord |= 8;
        else maskWord &= ~8;
    }

    public void setUnderflowMask(boolean value)
    { 
        if (value) maskWord |= 0x10;
        else maskWord &= ~0x10;
    }

    public void setPrecisionMask(boolean value)
    { 
        if (value) maskWord |= 0x20;
        else maskWord &= ~0x20;
    }

    public void setAllMasks(boolean value)
    {
        if (value) maskWord |= 0x3f;
        else maskWord = 0;
    }

    public void setPrecisionControl(int value)
    { 
        precisionControl = FPU_PRECISION_CONTROL_DOUBLE;
    }

    public void setRoundingControl(int value)
    { 
        roundingControl = FPU_ROUNDING_CONTROL_EVEN;
    }

	public void reset()
	{
		data = new double[STACK_DEPTH];
	    tag = new int[STACK_DEPTH];
	    specialTag = new int[STACK_DEPTH];
	    init();
	}

    public int conditionCode; // 4 bits
    public int top; // top of stack pointer (3 bits)
    public boolean infinityControl; // legacy:  not really used anymore
    public long lastIP; // last instruction pointer
    public long lastData; // last data (operand) pointer
    public int lastOpcode; // 11 bits

    public void init()
    {
        // tag word (and non-x87 special tags)
        for (int i = 0; i < tag.length; ++i)
            tag[i] = FPU_TAG_EMPTY;
        for (int i = 0; i < specialTag.length; ++i)
            specialTag[i] = FPU_SPECIAL_TAG_NONE;
        // status word
        clearExceptions();
        conditionCode = 0;
        top = 0;
        // control word
        setAllMasks(true);
        infinityControl = false;
        setPrecisionControl(FPU_PRECISION_CONTROL_DOUBLE); // 64 bits default
            // (x87 uses 80-bit precision as default!)
        setRoundingControl(FPU_ROUNDING_CONTROL_EVEN); // default
        lastIP = lastData = lastOpcode = 0;
    }
	
    public double ST(int index) throws Processor.Processor_Exception
    {
        int i = ((top + index) & 0x7);
        if (tag[i] == FPU_TAG_EMPTY)
        {
            // an attempt to read an empty register is technically
            // a "stack underflow"
            setInvalidOperation();
            setStackFault();
            conditionCode &= ~2; // C1 cleared to indicate stack underflow
            checkExceptions();
        }
        else if (specialTag[i] == FPU_SPECIAL_TAG_SNAN)
        {
            setInvalidOperation();
            checkExceptions();
            return Double.NaN; // QNaN if masked
        }
        return data[i];
    }
    public int tagCode(double x)
    {
        if (x == 0.0) return FPU_TAG_ZERO;
        else if (Double.isNaN(x)||Double.isInfinite(x)) return FPU_TAG_SPECIAL;
        else return FPU_TAG_VALID;
    }

    public static boolean isDenormal(double x)
    {
        long n = Double.doubleToRawLongBits(x);
        int exponent = (int)((n >> 52) & 0x7ff);
        if (exponent != 0) return false;
        long fraction = (n & ~(0xfffL << 52));
        if (fraction == 0L) return false;
        return true;
    }

    public static boolean isSNaN(long n)
    {
        // have to determine this based on 64-bit bit pattern,
        // since reassignment might cause Java to rationalize it to infinity
        int exponent = (int)((n >> 52) & 0x7ff);
        if (exponent != 0x7ff) return false;
        long fraction = (n & ~(0xfffL << 52));
        if ((fraction & (1L << 51)) != 0) return false;
        return (fraction != 0L);
    }

    // SNaN's aren't generated internally by x87.  Instead, they are
    // detected when they are read in from memory.  So if you push()
    // from memory, find out before whether it's an SNaN, then push(),
    // then set the tag word accordingly.
    public static int specialTagCode(double x)
    {
        // decode special:  NaN, unsupported, infinity, or denormal
        if (Double.isNaN(x)) return FPU_SPECIAL_TAG_NAN; // QNaN by default
        else if (Double.isInfinite(x)) return FPU_SPECIAL_TAG_INFINITY;
        //else if (Math.abs(x) < UNDERFLOW_THRESHOLD)
        else if (isDenormal(x))
            return FPU_SPECIAL_TAG_DENORMAL;
        else return FPU_SPECIAL_TAG_NONE;
    }

    public void push(double x) throws Processor.Processor_Exception
    {
        if (--top < 0) top = STACK_DEPTH - 1;
        if (tag[top] != FPU_TAG_EMPTY)
        {
            setInvalidOperation();
            setStackFault();
            conditionCode |= 2; // C1 set to indicate stack overflow
            checkExceptions();
            // if IE is masked, then we just continue and overwrite
        }
        data[top] = x;
        tag[top] = tagCode(x);
        specialTag[top] = specialTagCode(x);
    }
    public double pop() throws Processor.Processor_Exception
    {
        if (tag[top] == FPU_TAG_EMPTY)
        {
            setInvalidOperation();
            setStackFault();
            conditionCode &= ~2; // C1 cleared to indicate stack underflow
            checkExceptions();
        }
        else if (specialTag[top] == FPU_SPECIAL_TAG_SNAN)
        {
            setInvalidOperation();
            checkExceptions();
            return Double.NaN; // QNaN if masked
        }
        double x = data[top];
        tag[top] = FPU_TAG_EMPTY;
        if (++top >= STACK_DEPTH) top = 0;
        return x;
    }
    public int getTag(int index)
    {
        int i = ((top + index) & 0x7);
        return tag[i];
    }

    public int getSpecialTag(int index)
    {
        int i = ((top + index) & 0x7);
        return specialTag[i];
    }

    public void setTagEmpty(int index)
    {
        // used by FFREE
        int i = ((top + index) & 0x7);
        tag[i] = FPU_TAG_EMPTY;
    }

    public void setST(int index, double value)
    {
        int i = ((top + index) & 0x7);
        data[i] = value;
        tag[i] = tagCode(value);
        specialTag[i] = specialTagCode(value);
    }
    public int getStatus()
    {
        int w = statusWord;
        if (getErrorSummaryStatus()) w |= 0x80;
        if (getBusy()) w |= 0x8000;
        w |= (top << 11);
        w |= ((conditionCode & 0x7) << 8);
        w |= ((conditionCode & 0x8) << 11);
        return w;
    }

    public void setStatus(int w)
    {
        statusWord &= ~0x7f;
        statusWord |= (w & 0x7f);
        top = ((w >> 11) & 0x7);
        conditionCode = ((w >> 8) & 0x7);
        conditionCode |= ((w >>> 14) & 1);
    }

    public int getControl()
    {
        int w = maskWord;
        w |= ((precisionControl & 0x3) << 8);
        w |= ((roundingControl & 0x3) << 10);
        if (infinityControl) w |= 0x1000;
        return w;
    }

    public void setControl(int w)
    {
        maskWord &= ~0x3f;
        maskWord |= (w & 0x3f);
        infinityControl = ((w & 0x1000) != 0);
        setPrecisionControl((w >> 8) & 3);
        setRoundingControl((w >> 10) & 3);
    }

    public int getTagWord()
    {
        int w = 0;
        for (int i = STACK_DEPTH - 1; i >= 0; --i)
            w = ((w << 2) | (tag[i] & 0x3));
        return w;
    }

    public void setTagWord(int w)
    {
        for (int i = 0; i < tag.length; ++i)
        {
            int t = (w & 0x3);
            if (t == FPU_TAG_EMPTY)
            {
                tag[i] = FPU_TAG_EMPTY;
            }
            else
            {
                tag[i] = tagCode(data[i]);
                if (specialTag[i] != FPU_SPECIAL_TAG_SNAN)
                    specialTag[i] = specialTagCode(data[i]);
                // SNaN is sticky, and Java doesn't preserve the bit pattern.
            }
            w >>= 2;
        }
    }

 
    public static byte[] doubleToExtended(double x, boolean isSignalNaN)
    {
        byte[] b = new byte[10];
        long fraction = 0;
        int iexp = 0;
        // other special forms?
        if (isSignalNaN)
        {
            fraction = 0xc000000000000000L; // is this right?
        }
        else
        {
            long n = Double.doubleToRawLongBits(x);
            fraction = (n & ~(0xfff << 52));
            iexp = ((int)(n >> 52) & 0x7ff);
            boolean sgn = ((n & (1 << 63)) != 0);
            // insert implicit 1
            fraction |= (1 << 52);
            fraction <<= 11;
            // re-bias exponent
            iexp += (16383 - 1023);
            if (sgn) iexp |= 0x8000;
        }     
        for (int i = 0; i < 8; ++i)
        {
            b[i] = (byte)fraction;
            fraction >>>= 8;
        }
        b[8] = (byte)iexp;
        b[9] = (byte)(iexp >>> 8);
        return b;
    }

    public static int specialTagCode(byte[] b)
    {
        long fraction = 0;
        for (int i = 7; i >= 0; --i)
        {
            long w = ((long)b[i] & 0xff);
            fraction |= w;
            fraction <<= 8;
        }
        int iexp = (((int)b[8] & 0xff) | (((int)b[9] & 0x7f) << 8));
        boolean sgn = ((b[9] & 0x80) != 0);
        boolean integ = ((b[7] & 0x80) != 0); // explicit integer bit

        if (iexp == 0)
        {
            if (integ)
            {
                // "pseudo-denormals" - treated like a normal denormal
                return FPU_SPECIAL_TAG_DENORMAL;
            }
            else
            {
                // normal denormals
                return FPU_SPECIAL_TAG_DENORMAL;
            }
        }
        else if (iexp == 0x7fff)
        {
            if (fraction == 0L)
            {
                // "pseudo-infinity"
                return FPU_SPECIAL_TAG_UNSUPPORTED;
            }
            else if (integ)
            {
                if ((fraction << 1) == 0)
                {
                    // infinity
                    return FPU_SPECIAL_TAG_INFINITY;
                }
                else
                {
                    // NaN's
                    if ((fraction >>> 62) == 0) return FPU_SPECIAL_TAG_SNAN;
                    else return FPU_SPECIAL_TAG_NAN;
                }
            }
            else
            {
                // pseudo-NaN
                return FPU_SPECIAL_TAG_UNSUPPORTED;
            }
        }
        else
        {
            if (integ)
            {
                // normal float
                return FPU_SPECIAL_TAG_NONE;
            }
            else
            {
                // "unnormal"
                return FPU_SPECIAL_TAG_UNSUPPORTED;
            }
        }
    }


    public static double extendedToDouble(byte[] b)
    {
        long fraction = 0;
        for (int i = 7; i >= 0; --i)
        {
            long w = ((long)b[i] & 0xff);
            fraction |= w;
            fraction <<= 8;
        }
        int iexp = (((int)b[8] & 0xff) | (((int)b[9] & 0x7f) << 8));
        boolean sgn = ((b[9] & 0x80) != 0);
        boolean integ = ((b[7] & 0x80) != 0); // explicit integer bit

        if (iexp == 0)
        {
            if (integ)
            {
                // "pseudo-denormals" - treat exponent as value 1 and
                // mantissa as the same
                // (http://www.ragestorm.net/downloads/387intel.txt)
                iexp = 1;
            }
            // now treat as a normal denormal (from denormal).
            // actually, given that min unbiased exponent is -16383 for
            // extended, and only -1023 for double, a denormalized
            // extended is pretty much zero in double!
            return 0.0;
        }
        else if (iexp == 0x7fff)
        {
            if (fraction == 0L)
            {
                // "pseudo-infinity":  if #IA masked, return QNaN
                // more technically, sign bit should be set to indicate
                // "QNaN floating-point indefinite"
                return Double.NaN;
            }
            else if (integ)
            {
                if ((fraction << 1) == 0)
                {
                    return (sgn) ? Double.NEGATIVE_INFINITY :
                                   Double.POSITIVE_INFINITY;
                }
                else
                {
                    // a conventional NaN
                    return Double.NaN;
                }
            }
            else
            {
                // pseudo-NaN
                return Double.NaN;
            }
        }
        else
        {
            if (integ)
            {
                // normal float:  decode
                iexp += 1023 - 16383; // rebias for double format
                fraction >>>= 11; // truncate rounding (is this the right way?)
                if (iexp > 0x7ff)
                {
                    // too big an exponent
                    return (sgn) ? Double.NEGATIVE_INFINITY :
                                   Double.POSITIVE_INFINITY;
                }
                else if (iexp < 0)
                {
                    // denormal (from normal)
                    fraction >>>= (- iexp);
                    iexp = 0;
                }
                fraction &= ~(0xfffL << 52); // this cuts off explicit 1
                fraction |= (((long)iexp & 0x7ff) << 52);
                if (sgn) fraction |= (1 << 63);
                return Double.longBitsToDouble(fraction);
            }
            else
            {
                // "unnormal":  if #IA masked, return QNaN FP indefinite
                return Double.NaN;
            }
        }
    }
    private void validateOperand(double x) throws Processor.Processor_Exception
    {
        long n = Double.doubleToRawLongBits(x);
        if (((n >> 52) & 0x7ff) == 0 && ((n & 0xfffffffffffffL) != 0)) {
            setDenormalizedOperand();
        }
    }
    private void checkResult(double x) throws Processor.Processor_Exception
    {
        if (Double.isInfinite(x)) {
            setOverflow();
        }
    }    

}
