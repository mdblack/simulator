package simulator;

import java.util.ArrayList;
import java.util.Hashtable;

import javax.swing.JOptionPane;

/*
 * questions:
make a fetch stage:
1. data width
2. address width
make a decode stage:
3. # opcodes
4. # general purpose registers
rep: create inst type
	5. place fields:
		source reg 1
		source reg 2
		dest reg
		disp (spec bits)
		imm (spec bits)
make an execute stage:
rep: for each opcode
	6. choose inst name
	7. choose inst type


data bus size:
f	inst memory
f	inst register
d	inst bits
d?	immextend
d?	general purpose regs
e	data memory
e	i/o ports
d?	immediate
d?	value1
d?	value2
-	stack address
-	stack
e	aluinput1
e	aluinput2
e	alu
e	writeback value
e	==
e	signsplit
e	c equalszero


# opcodes:
d	opcode
d	opcode control

# regs:
d?	destReg
d?	sourceReg1
d?	sourceReg2
d?	regSelect
-	c registerIndex

imm size:
d?
disp size:
d?	
address size:
d?	shift left
d?	disp sign extend
d?	shift left
d?	add, add
d?	displacedPC
d?	displaced value
f	program counter
f	pcinputselect
f	pcadder
e	memory select
e	port select
e	next pc select
e	nextpc
	
f	c pcincrement
d?	c displacementExtend

 */

public class ProcessorBuilder extends AbstractGUI {
    String[] fparts = new String[]{"instruction memory", "instruction register", "program counter", "pcInputSelect", "pcadder"};
    int[] fpartswidth = new int[]{0, 0, 1, 1, 1};

    public ProcessorBuilder(Computer computer) {
        super(computer, "Processor Wizard", 200, 200, false, false, false, false);
        if (JOptionPane.showConfirmDialog(this, "This wizard will help you create your own CPU", "Processor Builder", 2) != 0) {
            closeGUI();
            return;
        }
        if (computer.datapathBuilder == null)
            computer.datapathBuilder = new DatapathBuilder(computer);

        computer.datapathBuilder.doload("template.xml", computer.datapathBuilder.defaultModule);
        boolean[] remove = new boolean[computer.datapathBuilder.defaultModule.blocks.size()];
        for (int i = 0; i < remove.length; i++)
            remove[i] = true;

        if (JOptionPane.showConfirmDialog(this, "Shall we create a fetch stage?", "Fetch", 2) != 0) {
            finish(remove);
            return;
        }

//		JOptionPane.showMessageDialog(this, "Let's start with an instruction register that will hold the machine code of the current instruction.","Instruction Register",0);
        String[] bitoptions = new String[]{"1", "4", "8", "16", "32", "64"};
        int bits = Integer.parseInt(bitoptions[JOptionPane.showOptionDialog(this, "How many bits wide should each instruction be?", "Instruction width", JOptionPane.OK_OPTION, JOptionPane.QUESTION_MESSAGE, null, bitoptions, "8")]);
        String[] pcoptions = new String[]{"1", "16", "256", "64k", "4M"};
        int[] pcbitoptions = new int[]{1, 4, 8, 16, 32};
        int pcbits = pcbitoptions[JOptionPane.showOptionDialog(this, "How many bytes of instruction memory should be addressable?", "Instruction Memory Size", JOptionPane.OK_OPTION, JOptionPane.QUESTION_MESSAGE, null, pcoptions, "256")];

        for (int i = 0; i < remove.length; i++) {
            if (computer.datapathBuilder.defaultModule.blocks.get(i).name.equals("instruction register")) {
                computer.datapathBuilder.defaultModule.blocks.get(i).bits = bits;
                remove[i] = false;
            } else if (computer.datapathBuilder.defaultModule.blocks.get(i).name.equals("instruction memory")) {
                computer.datapathBuilder.defaultModule.blocks.get(i).bits = bits;
                remove[i] = false;
            } else if (computer.datapathBuilder.defaultModule.blocks.get(i).name.equals("program counter")) {
                computer.datapathBuilder.defaultModule.blocks.get(i).bits = pcbits;
                remove[i] = false;
            } else if (computer.datapathBuilder.defaultModule.blocks.get(i).name.equals("pcInputSelect")) {
                computer.datapathBuilder.defaultModule.blocks.get(i).bits = pcbits;
                remove[i] = false;
            } else if (computer.datapathBuilder.defaultModule.blocks.get(i).name.equals("pcadder")) {
                computer.datapathBuilder.defaultModule.blocks.get(i).bits = pcbits;
                remove[i] = false;
            } else if (computer.datapathBuilder.defaultModule.blocks.get(i).description.equals("pcIncrement")) {
                computer.datapathBuilder.defaultModule.blocks.get(i).bits = pcbits;
                computer.datapathBuilder.defaultModule.blocks.get(i).name = "" + ((int) Math.ceil(bits / 8.0));
                remove[i] = false;
            }
        }
        if (JOptionPane.showConfirmDialog(this, "Shall we create a decode stage?", "Decode", 2) != 0) {
            finish(remove);
            return;
        }
//		int opcodes=Integer.parseInt(JOptionPane.showInputDialog("How many instruction opcodes would you like?"));
//		int opcodebits=(int)Math.ceil(Math.log(opcodes)/Math.log(2));

        String[] regoptions = new String[]{"1", "2", "4", "8", "16", "32", "64"};
        int[] regbitoptions = new int[]{0, 1, 2, 3, 4, 5, 6};
        int regbits = regbitoptions[JOptionPane.showOptionDialog(this, "How many general purpose registers would you like?", "Registers", JOptionPane.OK_OPTION, JOptionPane.QUESTION_MESSAGE, null, regoptions, "8")];

//		JOptionPane.showMessageDialog(null, "Out of an instruction word of "+bits+"bits, you need "+opcodebits+" bits for the opcode, leaving you with "+(bits-opcodebits)+" for the operands.  You will need "+regbits+" bits for each register operand.");
        JOptionPane.showMessageDialog(null, "You will now be prompted to enter your assembly instruction set.  Enter a unique opcode name, followed by 0 to 3 operand fields, all separated by spaces");
        JOptionPane.showMessageDialog(null, "The following are operand field options: rs, rt, rd, m[rs], m[rt], m[rd], m[rs+disp], m[rt+disp], m[rd+disp], disp, imm");

        ArrayList<String> ilist = new ArrayList<String>();
        while (true) {
            String i = (JOptionPane.showInputDialog("Enter an instruction, or leave the line blank to stop"));
            if (i.equals(""))
                break;
            ilist.add(i);
        }
        int opcodebits = (int) Math.ceil(Math.log((double) ilist.size()) / Math.log(2.0));
        int dispbits = bits;
        int rsbits = 0;
        int rtbits = 0;
        int rdbits = 0;
        int immbits = dispbits;
        for (int i = 0; i < ilist.size(); i++) {
            String inst = ilist.get(i);
            boolean rs = false, rt = false, rd = false;
            boolean disp = false;
            boolean imm = false;
            for (String o : inst.split(" ")) {
                if (o.equals("rs") || o.equals("[rs]") || o.equals("[rs+disp]")) rs = true;
                if (o.equals("rt") || o.equals("[rt]") || o.equals("[rt+disp]")) rt = true;
                if (o.equals("rd") || o.equals("[rd]") || o.equals("[rd+disp]")) rd = true;
                if (o.equals("[rs+disp]") || o.equals("[rt+disp]") || o.equals("[rd+disp]") || o.equals("disp"))
                    disp = true;
                if (o.equals("imm")) imm = true;
            }
            if (rs) rsbits = regbits;
            if (rt) rtbits = regbits;
            if (rd) rdbits = regbits;
        }
        for (int i = 0; i < ilist.size(); i++) {
            String inst = ilist.get(i);
            boolean rs = false, rt = false, rd = false;
            boolean disp = false;
            boolean imm = false;
            for (String o : inst.split(" ")) {
                if (o.equals("[rs+disp]") || o.equals("[rt+disp]") || o.equals("[rd+disp]") || o.equals("disp"))
                    disp = true;
                if (o.equals("imm")) imm = true;
            }
            int totalregs = rsbits + rtbits + rdbits;
            if (totalregs > bits - opcodebits) {
                JOptionPane.showMessageDialog(null, "Instruction " + inst + " can't be implemented");
                ilist.set(i, "");
                continue;
            }
            if (disp && bits - opcodebits - totalregs < dispbits)
                dispbits = bits - opcodebits - totalregs;
            if (imm && bits - opcodebits - totalregs < immbits)
                immbits = bits - opcodebits - totalregs;
        }
        if (immbits == bits) immbits = 0;
        if (dispbits == bits) dispbits = 0;
        int ophigh = -1, oplow = -1, rshigh = -1, rslow = -1, rdhigh = -1, rdlow = -1, rthigh = -1, rtlow = -1, immhigh = -1, immlow = -1, disphigh = -1, displow = -1;
        String encoding = "|opcode " + opcodebits + "|";
        ophigh = bits - 1;
        oplow = bits - opcodebits;
        int ptr = oplow - 1;
        if (rsbits > 0) {
            encoding += "rs " + rsbits + "|";
            rshigh = bits - opcodebits - 1;
            rslow = bits - opcodebits - rsbits;
            ptr = rslow - 1;
        }
        if (rtbits > 0) {
            encoding += "rt " + rtbits + "|";
            rthigh = ptr;
            rtlow = ptr - rtbits + 1;
            ptr = rtlow - 1;
        }
        if (rdbits > 0) {
            encoding += "rd " + rdbits + "|";
            rdhigh = ptr;
            rdlow = ptr - rdbits + 1;
            ptr = rdlow - 1;
        }
        if (dispbits > 0) {
            encoding += "disp " + dispbits + " ";
            disphigh = ptr;
            displow = ptr - dispbits + 1;
        }
        if (immbits > 0) {
            encoding += "imm " + immbits + " ";
            immhigh = ptr;
            immlow = ptr - immbits + 1;
        }

        JOptionPane.showMessageDialog(null, "Encoding: " + encoding);

        for (int i = 0; i < remove.length; i++) {
            if (computer.datapathBuilder.defaultModule.blocks.get(i).name.equals("instruction bits")) {
                computer.datapathBuilder.defaultModule.blocks.get(i).bits = bits;

                if (opcodebits != 0)
                    computer.datapathBuilder.defaultModule.blocks.get(i).bus.put(35, "" + ophigh + ":" + oplow);
                if (immbits != 0)
                    computer.datapathBuilder.defaultModule.blocks.get(i).bus.put(90, "" + immhigh + ":" + immlow);
                if (dispbits != 0)
                    computer.datapathBuilder.defaultModule.blocks.get(i).bus.put(272, "" + disphigh + ":" + displow);
                if (rdbits != 0)
                    computer.datapathBuilder.defaultModule.blocks.get(i).bus.put(38, "" + rdhigh + ":" + rdlow);
                if (rsbits != 0)
                    computer.datapathBuilder.defaultModule.blocks.get(i).bus.put(39, "" + rshigh + ":" + rslow);
                if (rtbits != 0)
                    computer.datapathBuilder.defaultModule.blocks.get(i).bus.put(42, "" + rthigh + ":" + rtlow);
                remove[i] = false;
            } else if (computer.datapathBuilder.defaultModule.blocks.get(i).name.equals("general purpose registers") && (rsbits != 0 || rtbits != 0 || rdbits != 0)) {
                computer.datapathBuilder.defaultModule.blocks.get(i).bits = bits;
                remove[i] = false;
            } else if (computer.datapathBuilder.defaultModule.blocks.get(i).name.equals("value1") && (rsbits != 0)) {
                computer.datapathBuilder.defaultModule.blocks.get(i).bits = bits;
                remove[i] = false;
            } else if (computer.datapathBuilder.defaultModule.blocks.get(i).name.equals("value2") && (rtbits != 0)) {
                computer.datapathBuilder.defaultModule.blocks.get(i).bits = bits;
                remove[i] = false;
            } else if (computer.datapathBuilder.defaultModule.blocks.get(i).name.equals("immediate") && (immbits != 0)) {
                computer.datapathBuilder.defaultModule.blocks.get(i).bits = bits;
                remove[i] = false;
            } else if (computer.datapathBuilder.defaultModule.blocks.get(i).name.equals("immediateExtend") && (immbits != 0)) {
                computer.datapathBuilder.defaultModule.blocks.get(i).bits = bits;
                remove[i] = false;
            } else if (computer.datapathBuilder.defaultModule.blocks.get(i).name.equals("opcode") && (opcodebits != 0)) {
                computer.datapathBuilder.defaultModule.blocks.get(i).bits = opcodebits;
                remove[i] = false;
            } else if (computer.datapathBuilder.defaultModule.blocks.get(i).name.equals("destReg") && (rdbits != 0)) {
                computer.datapathBuilder.defaultModule.blocks.get(i).bits = regbits;
                remove[i] = false;
            } else if (computer.datapathBuilder.defaultModule.blocks.get(i).name.equals("sourceReg1") && (rsbits != 0)) {
                computer.datapathBuilder.defaultModule.blocks.get(i).bits = regbits;
                remove[i] = false;
            } else if (computer.datapathBuilder.defaultModule.blocks.get(i).name.equals("sourceReg2") && (rtbits != 0)) {
                computer.datapathBuilder.defaultModule.blocks.get(i).bits = regbits;
                remove[i] = false;
            } else if (computer.datapathBuilder.defaultModule.blocks.get(i).name.equals("regSelect") && (rdbits != 0 || rsbits != 0 || rtbits != 0)) {
                computer.datapathBuilder.defaultModule.blocks.get(i).bits = regbits;
                remove[i] = false;
            } else if (computer.datapathBuilder.defaultModule.blocks.get(i).name.equals("displacedPC") && (dispbits != 0)) {
                computer.datapathBuilder.defaultModule.blocks.get(i).bits = pcbits;
                remove[i] = false;
            } else if (computer.datapathBuilder.defaultModule.blocks.get(i).name.equals("displaced value") && (dispbits != 0)) {
                computer.datapathBuilder.defaultModule.blocks.get(i).bits = pcbits;
                remove[i] = false;
            } else if (computer.datapathBuilder.defaultModule.blocks.get(i).name.equals("displacementExtend") && (dispbits != 0)) {
                computer.datapathBuilder.defaultModule.blocks.get(i).bits = pcbits;
                remove[i] = false;
            } else if (computer.datapathBuilder.defaultModule.blocks.get(i).name.equals("add") && (dispbits != 0)) {
                computer.datapathBuilder.defaultModule.blocks.get(i).bits = pcbits;
                remove[i] = false;
            } else if (computer.datapathBuilder.defaultModule.blocks.get(i).name.equals("shift-left") && (dispbits != 0)) {
                computer.datapathBuilder.defaultModule.blocks.get(i).bits = pcbits;
                remove[i] = false;
            } else if (computer.datapathBuilder.defaultModule.blocks.get(i).description.equals("displacementShift") && (dispbits != 0)) {
                computer.datapathBuilder.defaultModule.blocks.get(i).name = "" + (int) (Math.ceil((bits / 8.0)) - 1);
                computer.datapathBuilder.defaultModule.blocks.get(i).bits = pcbits;
                remove[i] = false;
            }
        }

        finish(remove);
    }

    public void finish(boolean[] remove) {
        for (int i = 0; i < remove.length; i++)
            if (remove[i])
                computer.datapathBuilder.defaultModule.blocks.get(i).selected = true;
        computer.datapathBuilder.delete();

        int buses = computer.datapathBuilder.defaultModule.buses.size();
        for (int i = 0; i < buses; i++)
            for (int j = 0; j < computer.datapathBuilder.defaultModule.buses.size(); j++)
                computer.datapathBuilder.defaultModule.buses.get(j).fix();
        for (int i = 0; i < buses; i++) {
            for (int j = 0; j < computer.datapathBuilder.defaultModule.buses.size(); j++) {
                if (!computer.datapathBuilder.defaultModule.buses.get(j).verify())
                    computer.datapathBuilder.defaultModule.buses.get(j).selected = true;
            }
            computer.datapathBuilder.delete();
        }

        computer.datapathBuilder.unselectAll();
        closeGUI();
    }

    private void makeFetch(Computer computer, int ibits, int pcbits) {


        computer.datapathBuilder.doload("fetchstage.xml", computer.datapathBuilder.defaultModule);
        computer.datapathBuilder.defaultModule.getBlock("instruction register").bits = ibits;
        computer.datapathBuilder.defaultModule.getBlock("instruction memory").bits = ibits;
        computer.datapathBuilder.defaultModule.getBlock("program counter").bits = pcbits;
        computer.datapathBuilder.defaultModule.getBlock("pcadder").bits = pcbits;
        computer.datapathBuilder.defaultModule.getBlock("pcInputSelect").bits = pcbits;
        for (int i = computer.datapathBuilder.defaultModule.blocknumber; i >= 0; i--)
            if (computer.datapathBuilder.defaultModule.getBlock(i) != null && computer.datapathBuilder.defaultModule.getBlock(i).type.equals("constant")) {
                computer.datapathBuilder.defaultModule.getBlock(i).bits = pcbits;
                if (ibits >= 8)
                    computer.datapathBuilder.defaultModule.getBlock(i).name = "" + (ibits / 8);
                break;
            }
        computer.datapathBuilder.defaultModule.fixbuses();
        computer.datapathBuilder.repaint();

    }

    private Hashtable<String, Integer> searchDatapath(Computer computer) {
        String[] clist = new String[]{"instruction memory", "program counter", "instruction register"};
        Hashtable<String, Integer> components = new Hashtable<String, Integer>();
        for (String s : clist) {
            DatapathBuilder.Block b = computer.datapathBuilder.defaultModule.getBlock(s);
            if (b == null)
                components.put(s, -1);
            else
                components.put(s, b.number);
        }
        return components;
    }

    public void closeGUI() {
    }

}
