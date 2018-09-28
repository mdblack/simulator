package simulator;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

public class ProcessorGUI extends AbstractGUI {
    private static final int REGISTERHEIGHT = 200;
    private static final int BLOCK1HEIGHT = 400;
    private static final int BLOCK2HEIGHT = 500;
    private static final int RXSIZE = 30, RYSIZE = 20;
    private static final int BXSIZE = 50, BYSIZE = 40;
    private static final int FONTSIZE = 8;
    private static final int BUS1HEIGHT = 130;
    private static final int BUS2HEIGHT = 300;
    private static final int BUS3HEIGHT = 600;
    private static final int FLAGX = 710, FLAGY = 200;
    private static final int FLAGSIZE = 12;

    private static final Color INACTIVECOLOR = new Color(200, 200, 255);
    private static final Color READCOLOR = new Color(200, 255, 200);
    private static final Color WRITECOLOR = new Color(255, 200, 200);
    private static final Color VALUECOLOR = new Color(0, 0, 255);
    private static final Color WRITETEXTCOLOR = new Color(50, 0, 0);
    private static final Color READTEXTCOLOR = new Color(0, 50, 0);

    public static final int EIP = 0, ESP = 1, CS = 2, SS = 3, DS = 4, ES = 5, FS = 6, GS = 7, EAX = 8, EBX = 9, ECX = 10, EDX = 11, EBP = 12, ESI = 13, EDI = 14, CR0 = 15, CR2 = 16, CR3 = 17, IDTR = 18, GDTR = 19, LDTR = 20, TSS = 21, FLAGS = 22;
    public static final String[] register_name = new String[]{
            "EIP", "ESP", "CS", "SS", "DS", "ES", "FS", "GS", "EAX", "EBX", "ECX", "EDX", "EBP", "ESI", "EDI", "CR0", "CR2", "CR3", "IDTR", "GDTR", "LDTR", "TSS", "FLAGS"};

    public static final int REG0 = 0, REG1 = 1, ADDR = 2, SEG = 3, MEMORY = 4, PORTS = 5, ALU = 6, DISPALU = 7, IPALU = 8, SPALU = 9;
    public static final String[] block_name = new String[]{
            "reg0", "reg1", "addr", "seg", "memory", "ports", "ALU", "dispALU", "ipALU", "spALU"};

    public static final int BUSMAIN = 0, BUSEIPOUT = 1, BUSESPOUT = 2, BUSCSOUT = 3, BUSSSOUT = 4, BUSDSOUT = 5, BUSESOUT = 6, BUSFSOUT = 7, BUSGSOUT = 8, BUSEAXOUT = 9, BUSEBXOUT = 10, BUSECXOUT = 11, BUSEDXOUT = 12, BUSEBPOUT = 13, BUSESIOUT = 14, BUSEDIOUT = 15, BUSCR0OUT = 16, BUSCR2OUT = 17, BUSCR3OUT = 18, BUSIDTROUT = 19, BUSGDTROUT = 20, BUSLDTROUT = 21, BUSTSSOUT = 22, BUSFLAGSOUT = 23, BUSEIPIN = 24, BUSESPIN = 25, BUSCSIN = 26, BUSSSIN = 27, BUSDSIN = 28, BUSESIN = 29, BUSFSIN = 30, BUSGSIN = 31, BUSEAXIN = 32, BUSEBXIN = 33, BUSECXIN = 34, BUSEDXIN = 35, BUSEBPIN = 36, BUSESIIN = 37, BUSEDIIN = 38, BUSCR0IN = 39, BUSCR2IN = 40, BUSCR3IN = 41, BUSIDTRIN = 42, BUSGDTRIN = 43, BUSLDTRIN = 44, BUSTSSIN = 45, BUSFLAGSIN = 46, BUSREG0OUT = 47, BUSREG1OUT = 48, BUSADDROUT = 49, BUSSEGOUT = 50, BUSMEMORYOUT = 51, BUSPORTSOUT = 52, BUSREG0IN = 53, BUSREG1IN = 54, BUSADDRIN = 55, BUSSEGIN = 56, BUSMEMORYIN = 57, BUSPORTSIN = 58, BUSPORTADDRESS = 59, BUSIPALU = 60, BUSSPALU = 61, BUSADDRALU = 62, BUSMAINALUIN0 = 63, BUSMAINALUIN1 = 64, BUSMAINALUOUT0 = 65, BUSMAINALUOUT1 = 66, BUSDISPLACEMENT = 67, BUSIMMEDIATE = 68, BUSIPMEMORY = 69, BUSINSTRUCTIONIN = 70, BUSIPALU1 = 71, BUSSPALU1 = 72, BUSSPMEMORY = 73;

    RegisterBubble[] registerBubble;
    InternalBlock[] internalBlock;
    Bus[] bus;
    Flag[] flags;
    InstructionBlock instructionBlock;

    boolean cumulativeAddress = false;

    public ProcessorGUI(Computer computer) {
        super(computer, "Processor", 900, 740, true, true, true, true);

        makeBlocks();

        refresh();
    }

    public void closeGUI() {
        computer.processorGUI = null;
    }

    public void makeBlocks() {
        flags = new Flag[6];
        flags[0] = new Flag("Carry", computer.processor.carry, FLAGX, FLAGY);
        flags[1] = new Flag("Zero", computer.processor.zero, FLAGX + 35, FLAGY);
        flags[2] = new Flag("Parity", computer.processor.parity, FLAGX + 70, FLAGY);
        flags[3] = new Flag("Sign", computer.processor.sign, FLAGX, FLAGY + 30);
        flags[4] = new Flag("Overflow", computer.processor.overflow, FLAGX + 35, FLAGY + 30);
        flags[5] = new Flag("Auxiliary Carry", computer.processor.auxiliaryCarry, FLAGX + 70, FLAGY + 30);

        registerBubble = new RegisterBubble[23];
        internalBlock = new InternalBlock[10];
        registerBubble[0] = new RegisterBubble(0, 100, REGISTERHEIGHT);    //ip
        registerBubble[1] = new RegisterBubble(1, 200, REGISTERHEIGHT);    //sp
        for (int i = 2; i < 23; i++)
            registerBubble[i] = new RegisterBubble(i, 250 + (RXSIZE + 10) * i / 2, REGISTERHEIGHT - (i % 2) * (RYSIZE + 10));    //other regs
        internalBlock[0] = new InternalBlock(0, false, 550, BLOCK1HEIGHT);    //reg0
        internalBlock[1] = new InternalBlock(1, false, 650, BLOCK1HEIGHT);    //reg1
        internalBlock[2] = new InternalBlock(2, false, 350, BLOCK1HEIGHT);    //addr
        internalBlock[3] = new InternalBlock(3, false, 150, BLOCK1HEIGHT);    //seg
        internalBlock[4] = new InternalBlock(4, false, 50, BLOCK2HEIGHT);    //memory
        internalBlock[5] = new InternalBlock(5, false, 400, BLOCK2HEIGHT);    //ports
        internalBlock[6] = new InternalBlock(6, true, 600, BLOCK2HEIGHT);    //alu
        internalBlock[7] = new InternalBlock(7, true, 250, (BLOCK1HEIGHT + BLOCK2HEIGHT) / 2);    //dispALU
        internalBlock[8] = new InternalBlock(8, true, 50, REGISTERHEIGHT + 50);    //ipALU
        internalBlock[9] = new InternalBlock(9, true, 150, REGISTERHEIGHT + 50);    //spALU
        instructionBlock = new InstructionBlock(680, 80);

        bus = new Bus[74];
        //main bus
        bus[0] = new Bus(new int[]{internalBlock[8].arrowInX1(), BUS1HEIGHT, 850, BUS1HEIGHT, 50, BUS2HEIGHT, 850, BUS2HEIGHT, 50, BUS3HEIGHT, 850, BUS3HEIGHT, 850, BUS3HEIGHT, 850, BUS1HEIGHT});
        for (int i = 0; i < 23; i++) {
            //register outputs
            bus[i + 1] = new Bus(new int[]{registerBubble[i].arrowOutX(), registerBubble[i].arrowOutY(), registerBubble[i].arrowOutX(), BUS2HEIGHT});
            //register inputs
            bus[i + 1 + 23] = new Bus(new int[]{registerBubble[i].arrowInX(), BUS1HEIGHT, registerBubble[i].arrowInX(), registerBubble[i].arrowInY()});
        }
        for (int i = 0; i < 6; i++) {
            //internal block outputs
            if (i != 2 && i != 3)
                bus[i + 1 + 23 + 23] = new Bus(new int[]{internalBlock[i].arrowOutX(), internalBlock[i].arrowOutY(), internalBlock[i].arrowOutX(), BUS3HEIGHT});
            else
                bus[i + 1 + 23 + 23] = new Bus(new int[]{internalBlock[i].arrowOutX(), internalBlock[i].arrowOutY(), internalBlock[i].arrowOutX(), internalBlock[4].arrowAddressY(), internalBlock[i].arrowOutX(), internalBlock[4].arrowAddressY(), internalBlock[4].arrowAddressX(), internalBlock[4].arrowAddressY()});
            //internal block inputs
            bus[i + 1 + 23 + 23 + 6] = new Bus(new int[]{internalBlock[i].arrowInX(), BUS2HEIGHT, internalBlock[i].arrowInX(), internalBlock[i].arrowInY()});
        }
        int ax, ay, bx, by, cx, cy, dx, dy, ex, ey, fx, fy;
        //port address
        bus[1 + 23 + 23 + 6 + 6] = new Bus(new int[]{internalBlock[0].arrowOutX(), internalBlock[0].arrowOutY(), internalBlock[0].arrowOutX(), internalBlock[4].arrowAddressY(), internalBlock[0].arrowOutX(), internalBlock[4].arrowAddressY(), internalBlock[5].arrowAddressX(), internalBlock[5].arrowAddressY()});
        //ip alu
        ax = internalBlock[8].arrowOutX();
        ay = internalBlock[8].arrowOutY();
        bx = ax;
        by = ay + 10;
        cx = ax - BXSIZE / 2 - 10;
        cy = by;
        dx = cx;
        dy = registerBubble[0].arrowInY() - 10;
        ex = registerBubble[0].arrowInX();
        ey = dy;
        fx = ex;
        fy = registerBubble[0].arrowInY();
        bus[1 + 23 + 23 + 6 + 6 + 1] = new Bus(new int[]{registerBubble[0].arrowOutX(), registerBubble[0].arrowOutY(), registerBubble[0].arrowOutX(), (registerBubble[0].arrowOutY() + internalBlock[8].arrowInY()) / 2, registerBubble[0].arrowOutX(), (registerBubble[0].arrowOutY() + internalBlock[8].arrowInY()) / 2, internalBlock[8].arrowInX2(), (registerBubble[0].arrowOutY() + internalBlock[8].arrowInY()) / 2, internalBlock[8].arrowInX2(), (registerBubble[0].arrowOutY() + internalBlock[8].arrowInY()) / 2, internalBlock[8].arrowInX2(), internalBlock[8].arrowInY(), ax, ay, bx, by, bx, by, cx, cy, cx, cy, dx, dy, dx, dy, ex, ey, ex, ey, fx, fy});
        //sp alu
        ax = internalBlock[9].arrowOutX();
        ay = internalBlock[9].arrowOutY();
        bx = ax;
        by = ay + 10;
        cx = ax - BXSIZE / 2 - 8;
        cy = by;
        dx = cx;
        dy = registerBubble[1].arrowInY() - 10;
        ex = registerBubble[1].arrowInX();
        ey = dy;
        fx = ex;
        fy = registerBubble[1].arrowInY();
        bus[1 + 23 + 23 + 6 + 6 + 2] = new Bus(new int[]{registerBubble[1].arrowOutX(), registerBubble[1].arrowOutY(), registerBubble[1].arrowOutX(), (registerBubble[1].arrowOutY() + internalBlock[9].arrowInY()) / 2, registerBubble[1].arrowOutX(), (registerBubble[1].arrowOutY() + internalBlock[9].arrowInY()) / 2, internalBlock[9].arrowInX2(), (registerBubble[1].arrowOutY() + internalBlock[9].arrowInY()) / 2, internalBlock[9].arrowInX2(), (registerBubble[1].arrowOutY() + internalBlock[9].arrowInY()) / 2, internalBlock[9].arrowInX2(), internalBlock[9].arrowInY(), ax, ay, bx, by, bx, by, cx, cy, cx, cy, dx, dy, dx, dy, ex, ey, ex, ey, fx, fy});
        //addr alu
        ax = internalBlock[7].arrowOutX();
        ay = internalBlock[7].arrowOutY();
        bx = ax;
        by = ay + 10;
        cx = ax + (BXSIZE - 10);
        cy = by;
        dx = cx;
        dy = internalBlock[2].arrowInY() - 10;
        ex = internalBlock[2].arrowInX();
        ey = dy;
        fx = ex;
        fy = internalBlock[2].arrowInY();
        bus[1 + 23 + 23 + 6 + 6 + 3] = new Bus(new int[]{internalBlock[2].arrowOutX(), internalBlock[2].arrowOutY(), internalBlock[2].arrowOutX(), (internalBlock[2].arrowOutY() + internalBlock[7].arrowInY()) / 2, internalBlock[2].arrowOutX(), (internalBlock[2].arrowOutY() + internalBlock[7].arrowInY()) / 2, internalBlock[7].arrowInX2(), (internalBlock[2].arrowOutY() + internalBlock[7].arrowInY()) / 2, internalBlock[7].arrowInX2(), (internalBlock[2].arrowOutY() + internalBlock[7].arrowInY()) / 2, internalBlock[7].arrowInX2(), internalBlock[7].arrowInY(), ax, ay, bx, by, bx, by, cx, cy, cx, cy, dx, dy, dx, dy, ex, ey, ex, ey, fx, fy, internalBlock[7].arrowInX1(), BUS2HEIGHT, internalBlock[7].arrowInX1(), internalBlock[7].arrowInY()});
        //main alu in 0
        bus[1 + 23 + 23 + 6 + 6 + 4] = new Bus(new int[]{internalBlock[0].arrowOutX(), internalBlock[0].arrowOutY(), internalBlock[0].arrowOutX(), (internalBlock[0].arrowOutY() + internalBlock[6].arrowInY()) / 2, internalBlock[0].arrowOutX(), (internalBlock[0].arrowOutY() + internalBlock[6].arrowInY()) / 2, internalBlock[6].arrowInX1(), (internalBlock[0].arrowOutY() + internalBlock[6].arrowInY()) / 2, internalBlock[6].arrowInX1(), (internalBlock[0].arrowOutY() + internalBlock[6].arrowInY()) / 2, internalBlock[6].arrowInX1(), internalBlock[6].arrowInY()});
        //main alu in 1
        bus[1 + 23 + 23 + 6 + 6 + 5] = new Bus(new int[]{internalBlock[1].arrowOutX(), internalBlock[1].arrowOutY(), internalBlock[1].arrowOutX(), (internalBlock[1].arrowOutY() + internalBlock[6].arrowInY()) / 2, internalBlock[1].arrowOutX(), (internalBlock[1].arrowOutY() + internalBlock[6].arrowInY()) / 2, internalBlock[6].arrowInX2(), (internalBlock[1].arrowOutY() + internalBlock[6].arrowInY()) / 2, internalBlock[6].arrowInX2(), (internalBlock[1].arrowOutY() + internalBlock[6].arrowInY()) / 2, internalBlock[6].arrowInX2(), internalBlock[6].arrowInY()});
        //main alu out 0
        ax = internalBlock[6].arrowOutX1();
        ay = internalBlock[6].arrowOutY();
        bx = ax;
        by = ay + 10;
        cx = ax - (BXSIZE * 2 - 10);
        cy = by;
        dx = cx;
        dy = internalBlock[0].arrowInY() - 10;
        ex = internalBlock[0].arrowInX();
        ey = dy;
        fx = ex;
        fy = internalBlock[0].arrowInY();
        bus[1 + 23 + 23 + 6 + 6 + 6] = new Bus(new int[]{ax, ay, bx, by, bx, by, cx, cy, cx, cy, dx, dy, dx, dy, ex, ey, ex, ey, fx, fy});
        //main alu out 1
        ax = internalBlock[6].arrowOutX2();
        ay = internalBlock[6].arrowOutY();
        bx = ax;
        by = ay + 10;
        cx = ax + (BXSIZE * 2 - 10);
        cy = by;
        dx = cx;
        dy = internalBlock[1].arrowInY() - 10;
        ex = internalBlock[1].arrowInX();
        ey = dy;
        fx = ex;
        fy = internalBlock[1].arrowInY();
        bus[1 + 23 + 23 + 6 + 6 + 7] = new Bus(new int[]{ax, ay, bx, by, bx, by, cx, cy, cx, cy, dx, dy, dx, dy, ex, ey, ex, ey, fx, fy});
        //displacement line
        bus[1 + 23 + 23 + 6 + 6 + 8] = new Bus(new int[]{instructionBlock.arrowOutXdisp(), instructionBlock.arrowOutY(), instructionBlock.arrowOutXdisp(), BUS1HEIGHT});
        //immediate line
        bus[1 + 23 + 23 + 6 + 6 + 9] = new Bus(new int[]{instructionBlock.arrowOutXimm(), instructionBlock.arrowOutY(), instructionBlock.arrowOutXimm(), BUS1HEIGHT});
        //ip memory address line
        bus[1 + 23 + 23 + 6 + 6 + 10] = new Bus(new int[]{registerBubble[0].arrowOutX(), registerBubble[0].arrowOutY(), registerBubble[0].arrowOutX(), internalBlock[4].arrowAddressY(), registerBubble[0].arrowOutX(), internalBlock[4].arrowAddressY(), internalBlock[4].arrowAddressX(), internalBlock[4].arrowAddressY()});
        //memory to instruction register line
        ax = 850;
        ay = BUS1HEIGHT;
        bx = ax;
        by = instructionBlock.arrowInY() - 10;
        cx = instructionBlock.arrowInX();
        cy = by;
        dx = cx;
        dy = instructionBlock.arrowInY();
        bus[1 + 23 + 23 + 6 + 6 + 11] = new Bus(new int[]{ax, ay, bx, by, bx, by, cx, cy, cx, cy, dx, dy});
        //ip alu addition line
        bus[1 + 23 + 23 + 6 + 6 + 12] = new Bus(new int[]{internalBlock[8].arrowInX1(), BUS1HEIGHT, internalBlock[8].arrowInX1(), internalBlock[8].arrowInY()});
        //sp alu addition line
        bus[1 + 23 + 23 + 6 + 6 + 13] = new Bus(new int[]{internalBlock[9].arrowInX1(), BUS1HEIGHT, internalBlock[9].arrowInX1(), internalBlock[9].arrowInY()});
        //sp memory address line
        bus[1 + 23 + 23 + 6 + 6 + 14] = new Bus(new int[]{registerBubble[1].arrowOutX(), registerBubble[1].arrowOutY(), registerBubble[1].arrowOutX(), internalBlock[4].arrowAddressY(), registerBubble[1].arrowOutX(), internalBlock[4].arrowAddressY(), internalBlock[4].arrowAddressX(), internalBlock[4].arrowAddressY()});
    }

    public void applyCode(Processor.GUICODE code, String name, String value) {
        boolean codefound = true;
        String tempvalue = "";

        for (int i = 0; i < registerBubble.length; i++)
            registerBubble[i].reset();
        for (int i = 0; i < internalBlock.length; i++)
            internalBlock[i].reset();
        for (int i = 0; i < bus.length; i++)
            bus[i].reset();
        instructionBlock.reset();

        switch (code) {
            case EXECUTE_HALT:
                setStatusLabel("Processor Halted");
                break;

            case EXECUTE_FLAG:
                registerBubble[FLAGS].write(value);
                setStatusLabel("WRITEBACK: Setting Flags");
                break;

            case EXECUTE_INTERRUPT_RETURN:
            case EXECUTE_RETURN:
                registerBubble[ESP].read();
                bus[BUSSPMEMORY].access();
                bus[BUSSPALU].access();
                registerBubble[ESP].write(value);
                bus[BUSMEMORYOUT].access();
                bus[BUSMAIN].access();
                bus[BUSEIPIN].access();
                registerBubble[REG0].write(value);
                setStatusLabel("EXECUTE: Pop IP from Stack");
                break;

            case EXECUTE_POP:
                registerBubble[ESP].read();
                bus[BUSSPMEMORY].access();
                bus[BUSSPALU].access();
                registerBubble[ESP].write(value);
                bus[BUSMEMORYOUT].access();
                bus[BUSMAIN].access();
                bus[BUSREG0IN].access();
                internalBlock[REG0].write(value);
                setStatusLabel("EXECUTE: Pop from Stack");
                break;

            case EXECUTE_INTERRUPT:
            case EXECUTE_CALL_STACK:
                registerBubble[ESP].read();
                bus[BUSSPMEMORY].access();
                bus[BUSSPALU].access();
                registerBubble[ESP].write(value);
                registerBubble[EIP].read();
                bus[BUSEIPOUT].access();
                bus[BUSMAIN].access();
                bus[BUSMEMORYIN].access();
                internalBlock[MEMORY].write(value);
                setStatusLabel("EXECUTE: Push IP on Stack");
                break;

            case EXECUTE_PUSH:
                registerBubble[ESP].read();
                bus[BUSSPMEMORY].access();
                bus[BUSSPALU].access();
                registerBubble[ESP].write(value);
                internalBlock[REG0].read();
                bus[BUSREG0OUT].access();
                bus[BUSMAIN].access();
                bus[BUSMEMORYIN].access();
                internalBlock[MEMORY].write(value);
                setStatusLabel("EXECUTE: Push to Stack");
                break;

            case EXECUTE_JUMP_ABSOLUTE:
                internalBlock[REG0].read();
                bus[BUSREG0OUT].access();
                bus[BUSMAIN].access();
                bus[BUSEIPIN].access();
                registerBubble[EIP].write(value);
                setStatusLabel("EXECUTE: Absolute jump");
                break;

            case EXECUTE_JUMP:
                internalBlock[REG0].read();
                bus[BUSREG0OUT].access();
                bus[BUSMAIN].access();
                bus[BUSIPALU1].access();
                registerBubble[EIP].write(value);
                bus[BUSIPALU].access();
                setStatusLabel("EXECUTE: Relative jump");
                break;

            case EXECUTE_CONDITION:
                registerBubble[FLAGS].read();
                setStatusLabel("DECODE: Checking flag condition");
                break;

            case EXECUTE_TRANSFER:
                internalBlock[REG1].read();
                bus[BUSREG1OUT].access();
                bus[BUSMAIN].access();
                bus[BUSREG0IN].access();
                internalBlock[REG0].write(value);
                setStatusLabel("EXECUTE: Moving between registers");
                break;

            case EXECUTE_ARITHMETIC_1_1:
                internalBlock[REG0].read();
                bus[BUSMAINALUIN0].access();
                bus[BUSMAINALUOUT0].access();
                internalBlock[REG0].write(value);
                setStatusLabel("EXECUTE: Performing arithmetic");
                break;

            case EXECUTE_ARITHMETIC_2_1:
                internalBlock[REG0].read();
                internalBlock[REG1].read();
                bus[BUSMAINALUIN0].access();
                bus[BUSMAINALUIN1].access();
                bus[BUSMAINALUOUT0].access();
                internalBlock[REG0].write(value);
                setStatusLabel("EXECUTE: Performing arithmetic");
                break;

            case EXECUTE_PORT_READ:
                internalBlock[REG0].read();
                bus[BUSPORTADDRESS].access();
                bus[BUSMAIN].access();
                internalBlock[PORTS].read();
                bus[BUSPORTSOUT].access();
                bus[BUSREG1IN].access();
                internalBlock[REG1].write(value);
                setStatusLabel("EXECUTE: Reading from I/O port");
                break;

            case EXECUTE_PORT_WRITE:
                internalBlock[REG0].read();
                bus[BUSPORTADDRESS].access();
                internalBlock[REG1].read();
                bus[BUSREG1OUT].access();
                bus[BUSMAIN].access();
                bus[BUSPORTSIN].access();
                internalBlock[PORTS].write(value);
                setStatusLabel("EXECUTE: Writing to I/O port");
                break;

            case DECODE_SEGMENT_ADDRESS:
                if (name.equals("cs")) {
                    registerBubble[CS].read();
                    bus[BUSCSOUT].access();
                    value = registerBubble[CS].getValue();
                }
                if (name.equals("ss")) {
                    registerBubble[SS].read();
                    bus[BUSSSOUT].access();
                    value = registerBubble[SS].getValue();
                }
                if (name.equals("ds")) {
                    registerBubble[DS].read();
                    bus[BUSDSOUT].access();
                    value = registerBubble[DS].getValue();
                }
                if (name.equals("es")) {
                    registerBubble[ES].read();
                    bus[BUSESOUT].access();
                    value = registerBubble[ES].getValue();
                }
                if (name.equals("fs")) {
                    registerBubble[FS].read();
                    bus[BUSFSOUT].access();
                    value = registerBubble[FS].getValue();
                }
                if (name.equals("gs")) {
                    registerBubble[GS].read();
                    bus[BUSGSOUT].access();
                    value = registerBubble[GS].getValue();
                }
                bus[BUSMAIN].access();
                bus[BUSSEGIN].access();
                internalBlock[SEG].write(value);
                cumulativeAddress = false;
                setStatusLabel("DECODE: Getting segment");
                break;

            case DECODE_MEMORY_ADDRESS:
                if (name.equals("ax")) {
                    registerBubble[EAX].read();
                    bus[BUSEAXOUT].access();
                    tempvalue = registerBubble[EAX].getValue();
                }
                if (name.equals("bx")) {
                    registerBubble[EBX].read();
                    bus[BUSEBXOUT].access();
                    tempvalue = registerBubble[EBX].getValue();
                }
                if (name.equals("cx")) {
                    registerBubble[ECX].read();
                    bus[BUSECXOUT].access();
                    tempvalue = registerBubble[ECX].getValue();
                }
                if (name.equals("dx")) {
                    registerBubble[EDX].read();
                    bus[BUSEDXOUT].access();
                    tempvalue = registerBubble[EDX].getValue();
                }
                if (name.equals("si")) {
                    registerBubble[ESI].read();
                    bus[BUSESIOUT].access();
                    tempvalue = registerBubble[ESI].getValue();
                }
                if (name.equals("di")) {
                    registerBubble[EDI].read();
                    bus[BUSEDIOUT].access();
                    tempvalue = registerBubble[EDI].getValue();
                }
                if (name.equals("sp")) {
                    registerBubble[ESP].read();
                    bus[BUSESPOUT].access();
                    tempvalue = registerBubble[ESP].getValue();
                }
                if (name.equals("bp")) {
                    registerBubble[EBP].read();
                    bus[BUSEBPOUT].access();
                    tempvalue = registerBubble[EBP].getValue();
                }
                if (name.equals("displacement")) {
                    instructionBlock.readDisp();
                    bus[BUSDISPLACEMENT].access();
                    tempvalue = value;
                }
                bus[BUSMAIN].access();
                bus[BUSSEGIN].access();
                if (!cumulativeAddress)
                    internalBlock[ADDR].write(tempvalue);
                else {
                    bus[BUSADDRALU].access();
                    internalBlock[ADDR].write(internalBlock[ADDR].value + "+" + tempvalue);
                }
                cumulativeAddress = true;
                setStatusLabel("DECODE: Calculating address");
                break;

            case DECODE_INPUT_OPERAND_0:
                if (name.equals("ax") || name.equals("eax") || name.equals("ah") || name.equals("al")) {
                    registerBubble[EAX].read();
                    bus[BUSEAXOUT].access();
                } else if (name.equals("bx") || name.equals("ebx") || name.equals("bh") || name.equals("bl")) {
                    registerBubble[EBX].read();
                    bus[BUSEBXOUT].access();
                } else if (name.equals("cx") || name.equals("ecx") || name.equals("ch") || name.equals("cl")) {
                    registerBubble[ECX].read();
                    bus[BUSECXOUT].access();
                } else if (name.equals("dx") || name.equals("edx") || name.equals("dh") || name.equals("dl")) {
                    registerBubble[EDX].read();
                    bus[BUSEDXOUT].access();
                } else if (name.equals("si") || name.equals("esi")) {
                    registerBubble[ESI].read();
                    bus[BUSESIOUT].access();
                } else if (name.equals("di") || name.equals("edi")) {
                    registerBubble[EDI].read();
                    bus[BUSEDIOUT].access();
                } else if (name.equals("sp") || name.equals("esp")) {
                    registerBubble[ESP].read();
                    bus[BUSESPOUT].access();
                } else if (name.equals("bp") || name.equals("ebp")) {
                    registerBubble[EBP].read();
                    bus[BUSEBPOUT].access();
                } else if (name.equals("cs")) {
                    registerBubble[CS].read();
                    bus[BUSCSOUT].access();
                } else if (name.equals("ss")) {
                    registerBubble[SS].read();
                    bus[BUSSSOUT].access();
                } else if (name.equals("ds")) {
                    registerBubble[DS].read();
                    bus[BUSDSOUT].access();
                } else if (name.equals("es")) {
                    registerBubble[ES].read();
                    bus[BUSESOUT].access();
                } else if (name.equals("fs")) {
                    registerBubble[FS].read();
                    bus[BUSFSOUT].access();
                } else if (name.equals("gs")) {
                    registerBubble[GS].read();
                    bus[BUSGSOUT].access();
                } else if (name.equals("cr0")) {
                    registerBubble[CR0].read();
                    bus[BUSCR0OUT].access();
                } else if (name.equals("cr2")) {
                    registerBubble[CR2].read();
                    bus[BUSCR2OUT].access();
                } else if (name.equals("cr3")) {
                    registerBubble[CR3].read();
                    bus[BUSCR3OUT].access();
                } else if (name.equals("immediate")) {
                    instructionBlock.readImm();
                    bus[BUSIMMEDIATE].access();
                } else if (name.charAt(0) == 'm') {
                    internalBlock[SEG].read();
                    bus[BUSSEGOUT].access();
                    internalBlock[ADDR].read();
                    bus[BUSADDROUT].access();
                    internalBlock[MEMORY].read();
                    bus[BUSMEMORYOUT].access();
                }

                bus[BUSMAIN].access();
                bus[BUSREG0IN].access();
                internalBlock[REG0].write(value);
                setStatusLabel("DECODE: Reading " + name);
                break;

            case DECODE_INPUT_OPERAND_1:
                if (name.equals("ax") || name.equals("eax") || name.equals("ah") || name.equals("al")) {
                    registerBubble[EAX].read();
                    bus[BUSEAXOUT].access();
                } else if (name.equals("bx") || name.equals("ebx") || name.equals("bh") || name.equals("bl")) {
                    registerBubble[EBX].read();
                    bus[BUSEBXOUT].access();
                } else if (name.equals("cx") || name.equals("ecx") || name.equals("ch") || name.equals("cl")) {
                    registerBubble[ECX].read();
                    bus[BUSECXOUT].access();
                } else if (name.equals("dx") || name.equals("edx") || name.equals("dh") || name.equals("dl")) {
                    registerBubble[EDX].read();
                    bus[BUSEDXOUT].access();
                } else if (name.equals("si") || name.equals("esi")) {
                    registerBubble[ESI].read();
                    bus[BUSESIOUT].access();
                } else if (name.equals("di") || name.equals("edi")) {
                    registerBubble[EDI].read();
                    bus[BUSEDIOUT].access();
                } else if (name.equals("sp") || name.equals("esp")) {
                    registerBubble[ESP].read();
                    bus[BUSESPOUT].access();
                } else if (name.equals("bp") || name.equals("ebp")) {
                    registerBubble[EBP].read();
                    bus[BUSEBPOUT].access();
                } else if (name.equals("cs")) {
                    registerBubble[CS].read();
                    bus[BUSCSOUT].access();
                } else if (name.equals("ss")) {
                    registerBubble[SS].read();
                    bus[BUSSSOUT].access();
                } else if (name.equals("ds")) {
                    registerBubble[DS].read();
                    bus[BUSDSOUT].access();
                } else if (name.equals("es")) {
                    registerBubble[ES].read();
                    bus[BUSESOUT].access();
                } else if (name.equals("fs")) {
                    registerBubble[FS].read();
                    bus[BUSFSOUT].access();
                } else if (name.equals("gs")) {
                    registerBubble[GS].read();
                    bus[BUSGSOUT].access();
                } else if (name.equals("cr0")) {
                    registerBubble[CR0].read();
                    bus[BUSCR0OUT].access();
                } else if (name.equals("cr2")) {
                    registerBubble[CR2].read();
                    bus[BUSCR2OUT].access();
                } else if (name.equals("cr3")) {
                    registerBubble[CR3].read();
                    bus[BUSCR3OUT].access();
                } else if (name.equals("immediate")) {
                    instructionBlock.readImm();
                    bus[BUSIMMEDIATE].access();
                } else if (name.charAt(0) == 'm') {
                    internalBlock[SEG].read();
                    bus[BUSSEGOUT].access();
                    internalBlock[ADDR].read();
                    bus[BUSADDROUT].access();
                    internalBlock[MEMORY].read();
                    bus[BUSMEMORYOUT].access();
                }

                bus[BUSMAIN].access();
                bus[BUSREG1IN].access();
                internalBlock[REG1].write(value);
                setStatusLabel("DECODE: Reading " + name);
                break;

            case DECODE_OUTPUT_OPERAND_0:
                if (name.equals("ax") || name.equals("eax") || name.equals("ah") || name.equals("al")) {
                    registerBubble[EAX].write(value);
                    bus[BUSEAXIN].access();
                } else if (name.equals("bx") || name.equals("ebx") || name.equals("bh") || name.equals("bl")) {
                    registerBubble[EBX].write(value);
                    bus[BUSEBXIN].access();
                } else if (name.equals("cx") || name.equals("ecx") || name.equals("ch") || name.equals("cl")) {
                    registerBubble[ECX].write(value);
                    bus[BUSECXIN].access();
                } else if (name.equals("dx") || name.equals("edx") || name.equals("dh") || name.equals("dl")) {
                    registerBubble[EDX].write(value);
                    bus[BUSEDXIN].access();
                } else if (name.equals("si") || name.equals("esi")) {
                    registerBubble[ESI].write(value);
                    bus[BUSESIIN].access();
                } else if (name.equals("di") || name.equals("edi")) {
                    registerBubble[EDI].write(value);
                    bus[BUSEDIIN].access();
                } else if (name.equals("sp") || name.equals("esp")) {
                    registerBubble[ESP].write(value);
                    bus[BUSESPIN].access();
                } else if (name.equals("bp") || name.equals("ebp")) {
                    registerBubble[EBP].write(value);
                    bus[BUSEBPIN].access();
                } else if (name.equals("cs")) {
                    registerBubble[CS].write(value);
                    bus[BUSCSIN].access();
                } else if (name.equals("ss")) {
                    registerBubble[SS].write(value);
                    bus[BUSSSIN].access();
                } else if (name.equals("ds")) {
                    registerBubble[DS].write(value);
                    bus[BUSDSIN].access();
                } else if (name.equals("es")) {
                    registerBubble[ES].write(value);
                    bus[BUSESIN].access();
                } else if (name.equals("fs")) {
                    registerBubble[FS].write(value);
                    bus[BUSFSIN].access();
                } else if (name.equals("gs")) {
                    registerBubble[GS].write(value);
                    bus[BUSGSIN].access();
                } else if (name.equals("cr0")) {
                    registerBubble[CR0].write(value);
                    bus[BUSCR0IN].access();
                } else if (name.equals("cr2")) {
                    registerBubble[CR2].write(value);
                    bus[BUSCR2IN].access();
                } else if (name.equals("cr3")) {
                    registerBubble[CR3].write(value);
                    bus[BUSCR3IN].access();
                } else if (name.charAt(0) == 'm') {
                    internalBlock[SEG].read();
                    bus[BUSSEGOUT].access();
                    internalBlock[ADDR].read();
                    bus[BUSADDROUT].access();
                    internalBlock[MEMORY].write(value);
                    bus[BUSMEMORYIN].access();
                }

                bus[BUSMAIN].access();
                bus[BUSREG0OUT].access();
                internalBlock[REG0].read();
                setStatusLabel("WRITEBACK: Writing to " + name);
                break;

            case DECODE_OUTPUT_OPERAND_1:
                if (name.equals("ax") || name.equals("eax") || name.equals("ah") || name.equals("al")) {
                    registerBubble[EAX].write(value);
                    bus[BUSEAXIN].access();
                } else if (name.equals("bx") || name.equals("ebx") || name.equals("bh") || name.equals("bl")) {
                    registerBubble[EBX].write(value);
                    bus[BUSEBXIN].access();
                } else if (name.equals("cx") || name.equals("ecx") || name.equals("ch") || name.equals("cl")) {
                    registerBubble[ECX].write(value);
                    bus[BUSECXIN].access();
                } else if (name.equals("dx") || name.equals("edx") || name.equals("dh") || name.equals("dl")) {
                    registerBubble[EDX].write(value);
                    bus[BUSEDXIN].access();
                } else if (name.equals("si") || name.equals("esi")) {
                    registerBubble[ESI].write(value);
                    bus[BUSESIIN].access();
                } else if (name.equals("di") || name.equals("edi")) {
                    registerBubble[EDI].write(value);
                    bus[BUSEDIIN].access();
                } else if (name.equals("sp") || name.equals("esp")) {
                    registerBubble[ESP].write(value);
                    bus[BUSESPIN].access();
                } else if (name.equals("bp") || name.equals("ebp")) {
                    registerBubble[EBP].write(value);
                    bus[BUSEBPIN].access();
                } else if (name.equals("cs")) {
                    registerBubble[CS].write(value);
                    bus[BUSCSIN].access();
                } else if (name.equals("ss")) {
                    registerBubble[SS].write(value);
                    bus[BUSSSIN].access();
                } else if (name.equals("ds")) {
                    registerBubble[DS].write(value);
                    bus[BUSDSIN].access();
                } else if (name.equals("es")) {
                    registerBubble[ES].write(value);
                    bus[BUSESIN].access();
                } else if (name.equals("fs")) {
                    registerBubble[FS].write(value);
                    bus[BUSFSIN].access();
                } else if (name.equals("gs")) {
                    registerBubble[GS].write(value);
                    bus[BUSGSIN].access();
                } else if (name.equals("cr0")) {
                    registerBubble[CR0].write(value);
                    bus[BUSCR0IN].access();
                } else if (name.equals("cr2")) {
                    registerBubble[CR2].write(value);
                    bus[BUSCR2IN].access();
                } else if (name.equals("cr3")) {
                    registerBubble[CR3].write(value);
                    bus[BUSCR3IN].access();
                } else if (name.charAt(0) == 'm') {
                    internalBlock[SEG].read();
                    bus[BUSSEGOUT].access();
                    internalBlock[ADDR].read();
                    bus[BUSADDROUT].access();
                    internalBlock[MEMORY].write(value);
                    bus[BUSMEMORYIN].access();
                }

                bus[BUSMAIN].access();
                bus[BUSREG1OUT].access();
                internalBlock[REG1].read();
                setStatusLabel("WRITEBACK: Writing to " + name);
                break;

            case FETCH:
                instructionBlock.fetch();
                codefound = false;
                break;
            case DECODE_PREFIX:
                registerBubble[EIP].read();
                bus[BUSIPMEMORY].access();
                internalBlock[MEMORY].read();
                bus[BUSMEMORYOUT].access();
                bus[BUSMAIN].access();
                bus[BUSINSTRUCTIONIN].access();
                instructionBlock.writePrefix(name);
                setStatusLabel("FETCH: Reading Prefix " + name);
                break;
            case DECODE_OPCODE:
                registerBubble[EIP].read();
                bus[BUSIPMEMORY].access();
                internalBlock[MEMORY].read();
                bus[BUSMEMORYOUT].access();
                bus[BUSMAIN].access();
                bus[BUSINSTRUCTIONIN].access();
                instructionBlock.writeOpcode(name);
                setStatusLabel("FETCH: Reading Opcode " + name);
                break;
            case DECODE_INSTRUCTION:
                instructionBlock.writeInstruction(name);
                codefound = false;
                break;
            case DECODE_MODRM:
                registerBubble[EIP].read();
                bus[BUSIPMEMORY].access();
                internalBlock[MEMORY].read();
                bus[BUSMEMORYOUT].access();
                bus[BUSMAIN].access();
                bus[BUSINSTRUCTIONIN].access();
                instructionBlock.writeMODRM(name);
                setStatusLabel("FETCH: Reading ModR/M Byte " + name);
                break;
            case DECODE_SIB:
                registerBubble[EIP].read();
                bus[BUSIPMEMORY].access();
                internalBlock[MEMORY].read();
                bus[BUSMEMORYOUT].access();
                bus[BUSMAIN].access();
                bus[BUSINSTRUCTIONIN].access();
                instructionBlock.writeSIB(name);
                setStatusLabel("FETCH: Reading SIB Byte " + name);
                break;
            case DECODE_DISPLACEMENT:
                registerBubble[EIP].read();
                bus[BUSIPMEMORY].access();
                internalBlock[MEMORY].read();
                bus[BUSMEMORYOUT].access();
                bus[BUSMAIN].access();
                bus[BUSINSTRUCTIONIN].access();
                instructionBlock.writeDISP(name);
                setStatusLabel("FETCH: Reading address displacement " + name);
                break;
            case DECODE_IMMEDIATE:
                registerBubble[EIP].read();
                bus[BUSIPMEMORY].access();
                internalBlock[MEMORY].read();
                bus[BUSMEMORYOUT].access();
                bus[BUSMAIN].access();
                bus[BUSINSTRUCTIONIN].access();
                instructionBlock.writeIMM(name);
                setStatusLabel("FETCH: Reading immediate " + name);
                break;
            default:
                codefound = false;
                break;
        }
        if (codefound) {
            repaint();
//			if (computer.debugMode && computer.applet==null)
            if (computer.debugMode) {
                synchronized (computer.stepLock) {
                    try {
                        computer.stepLock.wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
    }

    public int width() {
        return 900;
    }

    public int height() {
        return 740;
    }

    public void doPaint(Graphics g) {
        g.setColor(new Color(0xfb, 0xf1, 0xb5));
        g.fillRect(0, 0, width(), height());
        for (int i = 0; i < 23; i++)
            registerBubble[i].draw(g);
        for (int i = 0; i < 10; i++)
            internalBlock[i].draw(g);
        for (int i = 0; i < bus.length; i++)
            bus[i].draw(g, false);
        for (int i = 0; i < bus.length; i++)
            bus[i].draw(g, true);
        for (int i = 0; i < flags.length; i++)
            flags[i].draw(g);
        instructionBlock.draw(g);
    }

    public void mouseMove(MouseEvent e) {
        for (int i = 0; i < 23; i++) {
            if (registerBubble[i].isMouse(e.getX(), e.getY()))
                setStatusLabel("Register " + register_name[registerBubble[i].register] + ": " + registerBubble[i].getValue());
        }
    }

    private class Bus {
        int[] points;
        boolean active;

        public Bus(int[] points) {
            this.points = points;
            active = false;
        }

        public void draw(Graphics g, boolean drawactive) {
            if (!active)
                g.setColor(new Color(200, 200, 200));
            else
                g.setColor(Color.RED);
            if (active || !drawactive) {
                int i = 0;
                while (i < points.length) {
                    g.drawLine(points[i], points[i + 1], points[i + 2], points[i + 3]);
                    i += 4;
                }
            }
        }

        public void reset() {
            active = false;
        }

        public void access() {
            active = true;
        }
    }

    private class InstructionBlock {
        int x, y;
        boolean writeOp, writeModrm, writeSib, writeDisp, writeImm, writePfix;
        boolean dispread;
        boolean immread;
        String opvalue = "", modrmvalue = "", sibvalue = "", dispvalue = "", immvalue = "";
        boolean hasprefix, hasop, hasmodrm, hassib, hasdisp, hasimm;
        String opcodename = "";
        String prefixname = "";

        public InstructionBlock(int x, int y) {
            this.x = x;
            this.y = y;
            dispread = false;
            immread = false;
        }

        public void reset() {
            writeOp = false;
            writeModrm = false;
            writeSib = false;
            writeDisp = false;
            writeImm = false;
            writePfix = false;
            dispread = false;
            immread = false;
        }

        public void fetch() {
            hasprefix = false;
            hasop = false;
            hasmodrm = false;
            hassib = false;
            hasdisp = false;
            hasimm = false;
        }

        public void writeOpcode(String opvalue) {
            writeOp = true;
            this.opvalue = opvalue;
            this.opcodename = "";
            hasop = true;
        }

        public void writeInstruction(String opcodename) {
            this.opcodename = opcodename;
        }

        public void writePrefix(String prefixname) {
            if (hasprefix)
                this.prefixname += (" " + prefixname);
            else
                this.prefixname = prefixname;
            writePfix = true;
            hasprefix = true;
        }

        public void writeMODRM(String modrm) {
            writeModrm = true;
            this.modrmvalue = modrm;
            hasmodrm = true;
        }

        public void writeSIB(String sib) {
            writeSib = true;
            this.sibvalue = sib;
            hassib = true;
        }

        public void writeIMM(String immvalue) {
            writeImm = true;
            this.immvalue = immvalue;
            hasimm = true;
        }

        public void writeDISP(String dispvalue) {
            writeDisp = true;
            this.dispvalue = dispvalue;
            hasdisp = true;
        }

        public void readDisp() {
            dispread = true;
        }

        public void readImm() {
            immread = true;
        }

        public void draw(Graphics g) {
            g.setColor(Color.BLACK);
            g.drawRect(x - 150, y - BYSIZE / 2, 300, BYSIZE);

            g.setColor(INACTIVECOLOR);
            g.fillRect(x - 149, y - BYSIZE / 2 + 1, 299, BYSIZE - 1);

            g.setColor(Color.BLACK);
            g.drawLine(x - 100, y - BYSIZE / 2, x - 100, y + BYSIZE / 2);
            g.drawLine(x - 50, y - BYSIZE / 2, x - 50, y + BYSIZE / 2);
            g.drawLine(x - 0, y - BYSIZE / 2, x - 0, y + BYSIZE / 2);
            g.drawLine(x + 50, y - BYSIZE / 2, x + 50, y + BYSIZE / 2);
            g.drawLine(x + 100, y - BYSIZE / 2, x + 100, y + BYSIZE / 2);

            if (writePfix) {
                g.setColor(WRITECOLOR);
                g.fillRect(x - 149, y - BYSIZE / 2 + 1, 49, BYSIZE - 1);
            } else if (writeOp) {
                g.setColor(WRITECOLOR);
                g.fillRect(x - 99, y - BYSIZE / 2 + 1, 49, BYSIZE - 1);
            } else if (writeModrm) {
                g.setColor(WRITECOLOR);
                g.fillRect(x - 49, y - BYSIZE / 2 + 1, 49, BYSIZE - 1);
            } else if (writeSib) {
                g.setColor(WRITECOLOR);
                g.fillRect(x + 1, y - BYSIZE / 2 + 1, 49, BYSIZE - 1);
            } else if (writeDisp) {
                g.setColor(WRITECOLOR);
                g.fillRect(x + 51, y - BYSIZE / 2 + 1, 49, BYSIZE - 1);
            } else if (writeImm) {
                g.setColor(WRITECOLOR);
                g.fillRect(x + 101, y - BYSIZE / 2 + 1, 49, BYSIZE - 1);
            } else {
                if (dispread) {
                    g.setColor(READCOLOR);
                    g.fillRect(x + 51, y - BYSIZE / 2 + 1, 49, BYSIZE - 1);
                }
                if (immread) {
                    g.setColor(READCOLOR);
                    g.fillRect(x + 101, y - BYSIZE / 2 + 1, 49, BYSIZE - 1);
                }
            }

            g.setColor(Color.BLACK);
            g.drawString("prefix", x - 150 + 2, y - BYSIZE / 4);
            g.drawString("opcode", x - 100 + 2, y - BYSIZE / 4);
            g.drawString("modr/m", x - 50 + 2, y - BYSIZE / 4);
            g.drawString("sib", x + 2, y - BYSIZE / 4);
            g.drawString("displace", x + 50 + 2, y - BYSIZE / 4);
            g.drawString("immediate", x + 100 + 2, y - BYSIZE / 4);

            g.setColor(VALUECOLOR);
            if (hasprefix) {
                int tempy = y;
                String temp = prefixname;
                while (!temp.equals("")) {
                    String temp2 = "";
                    int i;
                    for (i = 0; i < temp.length(); i++) {
                        if (temp.charAt(i) == ' ') break;
                        temp2 = temp2 + temp.charAt(i);
                    }
                    if (i + 1 >= temp.length())
                        temp = "";
                    else
                        temp = temp.substring(i + 1, temp.length());
                    g.drawString(temp2, x - 150 + 2, tempy);
                    tempy += BYSIZE / 4;
                }
            }
            if (hasop) g.drawString(opvalue, x - 100 + 2, y);
            if (hasop) g.drawString(opcodename, x - 100 + 2, y + BYSIZE / 4);
            if (hasmodrm) g.drawString(modrmvalue, x - 50 + 2, y + BYSIZE / 4);
            if (hassib) g.drawString(sibvalue, x + 2, y + BYSIZE / 4);
            if (hasdisp) g.drawString(dispvalue, x + 50 + 2, y + BYSIZE / 4);
            if (hasimm) g.drawString(immvalue, x + 100 + 2, y + BYSIZE / 4);

            if (writePfix || writeOp || writeModrm || writeSib || writeDisp || writeImm) {
                g.setColor(Color.RED);
                g.drawLine(arrowInX(), arrowInY() - 1, arrowInX(), arrowInY() - 1);
                g.drawLine(arrowInX() - 1, arrowInY() - 2, arrowInX() + 1, arrowInY() - 2);
                g.drawLine(arrowInX() - 2, arrowInY() - 3, arrowInX() + 2, arrowInY() - 3);
            }
        }

        public int arrowInX() {
            return x;
        }

        public int arrowInY() {
            return y - BYSIZE / 2 - 1;
        }

        public int arrowOutY() {
            return y + BYSIZE / 2 + 1;
        }

        public int arrowOutXdisp() {
            return x + 75;
        }

        public int arrowOutXimm() {
            return x + 125;
        }
    }

    private class InternalBlock {
        int id, x, y;
        boolean active;
        boolean write;
        boolean isALU;
        String value = "";

        public InternalBlock(int id, boolean isALU, int x, int y) {
            this.id = id;
            this.isALU = isALU;
            this.x = x;
            this.y = y;
            active = false;
            write = false;
        }

        public void reset() {
            active = false;
            write = false;
        }

        public void read() {
            active = true;
        }

        public void write(String value) {
            active = true;
            write = true;
            this.value = value;
        }

        public void draw(Graphics g) {
            if (!isALU) {
                g.setColor(Color.BLACK);
                g.drawRect(x - BXSIZE / 2, y - BYSIZE / 2, BXSIZE, BYSIZE);
                if (!active)
                    g.setColor(INACTIVECOLOR);
                else if (!write)
                    g.setColor(Color.GREEN);
                else
                    g.setColor(Color.RED);
                g.fillRect(x - BXSIZE / 2 + 1, y - BYSIZE / 2 + 1, BXSIZE - 1, BYSIZE - 1);

                if (write) {
                    g.setColor(Color.RED);
                    g.drawLine(arrowInX(), arrowInY() - 1, arrowInX(), arrowInY() - 1);
                    g.drawLine(arrowInX() - 1, arrowInY() - 2, arrowInX() + 1, arrowInY() - 2);
                    g.drawLine(arrowInX() - 2, arrowInY() - 3, arrowInX() + 2, arrowInY() - 3);
                }
                if ((id == 4 || id == 5) && active) {
                    g.setColor(Color.RED);
                    g.drawLine(arrowAddressX() + 1, arrowAddressY(), arrowAddressX() + 1, arrowAddressY());
                    g.drawLine(arrowAddressX() + 2, arrowAddressY() - 1, arrowAddressX() + 2, arrowAddressY() + 1);
                    g.drawLine(arrowAddressX() + 3, arrowAddressY() - 2, arrowAddressX() + 3, arrowAddressY() + 2);
                }

            } else {
                g.setColor(Color.BLACK);
                g.drawLine(x - BXSIZE / 2, y - BYSIZE / 2, x - BXSIZE / 2 + BXSIZE / 5, y + BYSIZE / 2);    //left
                g.drawLine(x + BXSIZE / 2, y - BYSIZE / 2, x + BXSIZE / 2 - BXSIZE / 5, y + BYSIZE / 2);    //right
                g.drawLine(x - BXSIZE / 2 + BXSIZE / 5, y + BYSIZE / 2, x + BXSIZE / 2 - BXSIZE / 5, y + BYSIZE / 2);    //bottom
                g.drawLine(x - BXSIZE / 8, y - BYSIZE / 2, x, y - BYSIZE / 3);        //left notch
                g.drawLine(x + BXSIZE / 8, y - BYSIZE / 2, x, y - BYSIZE / 3);        //right notch
                g.drawLine(x - BXSIZE / 2, y - BYSIZE / 2, x - BXSIZE / 8, y - BYSIZE / 2);    //left top
                g.drawLine(x + BXSIZE / 2, y - BYSIZE / 2, x + BXSIZE / 8, y - BYSIZE / 2);    //right top
            }
            g.setColor(Color.BLACK);
            g.setFont(new Font("Dialog", Font.PLAIN, FONTSIZE));
            g.drawString(block_name[id], x - 12, y - BYSIZE / 4);

            if (!isALU && id != 4 && id != 5) {
                g.setColor(VALUECOLOR);
                g.drawString(value, x - BXSIZE / 2 + 2, y + BYSIZE / 4);
            }
        }

        public int arrowInY() {
            return y - BYSIZE / 2 - 1;
        }

        public int arrowOutY() {
            return y + BYSIZE / 2 + 1;
        }

        public int arrowInX() {
            return x;
        }

        public int arrowOutX() {
            return x;
        }

        public int arrowInX1() {
            return x - BXSIZE / 3;
        }

        public int arrowInX2() {
            return x + BXSIZE / 3;
        }

        public int arrowOutX1() {
            return x - BXSIZE / 3;
        }

        public int arrowOutX2() {
            return x + BXSIZE / 3;
        }

        public int arrowAddressX() {
            return x + BXSIZE / 2 + 1;
        }

        public int arrowAddressY() {
            return y;
        }
    }

    private class RegisterBubble {
        int register, x, y;
        boolean active;
        boolean write;
        String value = "";

        public RegisterBubble(int register, int x, int y) {
            this.register = register;
            this.x = x;
            this.y = y;
            active = false;
            write = false;

            if (register == EAX) value = Integer.toHexString(computer.processor.eax.getValue());
            if (register == EBX) value = Integer.toHexString(computer.processor.ebx.getValue());
            if (register == ECX) value = Integer.toHexString(computer.processor.ecx.getValue());
            if (register == EDX) value = Integer.toHexString(computer.processor.edx.getValue());
            if (register == ESI) value = Integer.toHexString(computer.processor.esi.getValue());
            if (register == EDI) value = Integer.toHexString(computer.processor.edi.getValue());
            if (register == EBP) value = Integer.toHexString(computer.processor.ebp.getValue());
            if (register == ESP) value = Integer.toHexString(computer.processor.esp.getValue());
            if (register == EIP) value = Integer.toHexString(computer.processor.eip.getValue());
            if (register == CR0) value = Integer.toHexString(computer.processor.cr0.getValue());
            if (register == CR2) value = Integer.toHexString(computer.processor.cr2.getValue());
            if (register == CR3) value = Integer.toHexString(computer.processor.cr3.getValue());
            if (register == CS) value = Integer.toHexString(computer.processor.cs.getValue());
            if (register == SS) value = Integer.toHexString(computer.processor.ss.getValue());
            if (register == DS) value = Integer.toHexString(computer.processor.ds.getValue());
            if (register == ES) value = Integer.toHexString(computer.processor.es.getValue());
            if (register == FS) value = Integer.toHexString(computer.processor.fs.getValue());
            if (register == GS) value = Integer.toHexString(computer.processor.gs.getValue());
            if (register == IDTR && computer.processor.idtr != null)
                value = Integer.toHexString(computer.processor.idtr.getValue());
            if (register == GDTR && computer.processor.idtr != null)
                value = Integer.toHexString(computer.processor.gdtr.getValue());
            if (register == LDTR && computer.processor.idtr != null)
                value = Integer.toHexString(computer.processor.ldtr.getValue());
            if (register == TSS && computer.processor.idtr != null)
                value = Integer.toHexString(computer.processor.tss.getValue());
            if (register == FLAGS) value = Integer.toHexString(computer.processor.getFlags());
        }

        public String getValue() {
            if (register == EAX) value = Integer.toHexString(computer.processor.eax.getValue());
            if (register == EBX) value = Integer.toHexString(computer.processor.ebx.getValue());
            if (register == ECX) value = Integer.toHexString(computer.processor.ecx.getValue());
            if (register == EDX) value = Integer.toHexString(computer.processor.edx.getValue());
            if (register == ESI) value = Integer.toHexString(computer.processor.esi.getValue());
            if (register == EDI) value = Integer.toHexString(computer.processor.edi.getValue());
            if (register == EBP) value = Integer.toHexString(computer.processor.ebp.getValue());
            if (register == ESP) value = Integer.toHexString(computer.processor.esp.getValue());
            if (register == EIP) value = Integer.toHexString(computer.processor.eip.getValue());
            if (register == CR0) value = Integer.toHexString(computer.processor.cr0.getValue());
            if (register == CR2) value = Integer.toHexString(computer.processor.cr2.getValue());
            if (register == CR3) value = Integer.toHexString(computer.processor.cr3.getValue());
            if (register == CS) value = Integer.toHexString(computer.processor.cs.getValue());
            if (register == SS) value = Integer.toHexString(computer.processor.ss.getValue());
            if (register == DS) value = Integer.toHexString(computer.processor.ds.getValue());
            if (register == ES) value = Integer.toHexString(computer.processor.es.getValue());
            if (register == FS) value = Integer.toHexString(computer.processor.fs.getValue());
            if (register == GS) value = Integer.toHexString(computer.processor.gs.getValue());
            if (register == IDTR && computer.processor.idtr != null)
                value = Integer.toHexString(computer.processor.idtr.getValue());
            if (register == GDTR && computer.processor.idtr != null)
                value = Integer.toHexString(computer.processor.gdtr.getValue());
            if (register == LDTR && computer.processor.idtr != null)
                value = Integer.toHexString(computer.processor.ldtr.getValue());
            if (register == TSS && computer.processor.idtr != null)
                value = Integer.toHexString(computer.processor.tss.getValue());
            if (register == FLAGS) value = Integer.toHexString(computer.processor.getFlags());
            return value;
        }

        public void reset() {
            active = false;
            write = false;
        }

        public void read() {
            active = true;
        }

        public void write(String value) {
            active = true;
            write = true;
            this.value = value;
        }

        public boolean isMouse(int x, int y) {
            if (x >= this.x - RXSIZE / 2 && x < this.x + RXSIZE / 2 && y >= this.y - RYSIZE / 2 && y < this.y + RYSIZE / 2)
                return true;
            return false;
        }

        public void draw(Graphics g) {
            g.setColor(Color.BLACK);
            g.drawOval(x - RXSIZE / 2, y - RYSIZE / 2, RXSIZE, RYSIZE);
            if (!active)
                g.setColor(INACTIVECOLOR);
            else if (!write)
                g.setColor(READCOLOR);
            else
                g.setColor(WRITECOLOR);
            g.fillOval(x - RXSIZE / 2 + 1, y - RYSIZE / 2 + 1, RXSIZE - 2, RYSIZE - 2);
            g.setColor(Color.BLACK);
            g.setFont(new Font("Dialog", Font.PLAIN, FONTSIZE));
            g.drawString(register_name[register], x - 12, y + RYSIZE / 4);

            if (active) {
                if (write) {
                    g.setColor(Color.RED);
                    g.drawLine(arrowInX(), arrowInY() - 1, arrowInX(), arrowInY() - 1);
                    g.drawLine(arrowInX() - 1, arrowInY() - 2, arrowInX() + 1, arrowInY() - 2);
                    g.drawLine(arrowInX() - 2, arrowInY() - 3, arrowInX() + 2, arrowInY() - 3);

                    g.setColor(WRITETEXTCOLOR);
                    g.drawString(value, x + 1, y - RYSIZE / 2 - 1);
                } else {
                    g.setColor(READTEXTCOLOR);
                    g.drawString(value, x + 1, y + RYSIZE / 2 + FONTSIZE + 1);
                }
            }
        }

        public int arrowInY() {
            return y - RYSIZE / 2 - 1;
        }

        public int arrowOutY() {
            return y + RYSIZE / 2 + 1;
        }

        public int arrowInX() {
            return x;
        }

        public int arrowOutX() {
            return x;
        }
    }

    private class Flag {

        String name;
        Processor.Flag flag;
        int x, y;

        public Flag(String name, Processor.Flag flag, int x, int y) {
            this.name = name;
            this.flag = flag;
            this.x = x;
            this.y = y;
        }

        public void draw(Graphics g) {
            boolean state = flag.read();

            if (state) {
                g.setColor(Color.BLACK);
                g.drawLine(x, y, x, y - FLAGSIZE);
                g.setColor(Color.RED);
                g.drawLine(x, y - FLAGSIZE, x + FLAGSIZE / 2, y - FLAGSIZE + FLAGSIZE / 6);
                g.drawLine(x, y - FLAGSIZE + FLAGSIZE / 3, x + FLAGSIZE / 2, y - FLAGSIZE + FLAGSIZE / 6);
            } else {
                g.setColor(Color.BLACK);
                g.drawLine(x, y, x + FLAGSIZE, y);
                g.setColor(Color.RED);
                g.drawLine(x + FLAGSIZE, y, x + FLAGSIZE - FLAGSIZE / 6, y + FLAGSIZE / 2);
                g.drawLine(x + FLAGSIZE - FLAGSIZE / 3, y, x + FLAGSIZE - FLAGSIZE / 6, y + FLAGSIZE / 2);
            }
            g.setColor(Color.BLACK);
            g.setFont(new Font("Dialog", Font.PLAIN, FONTSIZE));
            g.drawString(name, x + 5, y - 1);
        }
    }
}
