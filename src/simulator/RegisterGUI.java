package simulator;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.ChangeListener;

import simulator.BreakpointGUI.BreakpointListSelectionListener;

import java.awt.event.*;

public class RegisterGUI extends AbstractGUI {
    public static final int NAMEWIDTH = 70, FIELDWIDTH = 70, COMMENTWIDTH = 300, ROWHEIGHT = 21;

    public static final int EIP = 0, EAX = 1, EBX = 2, ECX = 3, EDX = 4, ESP = 5, EBP = 6, ESI = 7, EDI = 8, CS = 9, SS = 10, DS = 11, ES = 12, FS = 13, GS = 14, CR0 = 15, CR2 = 16, CR3 = 17, IDTR = 18, GDTR = 19, LDTR = 20, TSS = 21;
    public static final int AH = 0, AL = 1, BH = 2, BL = 3, CH = 4, CL = 5, DH = 6, DL = 7;
    public static final int C = 0, P = 1, AC = 2, Z = 3, S = 4, T = 5, I = 6, D = 7, O = 8;

    public static final String[] register_name = new String[]{
            "EIP", "EAX", "EBX", "ECX", "EDX", "ESP", "EBP", "ESI", "EDI", "CS", "SS", "DS", "ES", "FS", "GS", "CR0", "CR2", "CR3", "IDTR", "GDTR", "LDTR", "TSS"};
    public static final String[] flag_name = new String[]{
            "carry", "parity", "aux carry", "zero", "sign", "trap", "interrupt", "direction", "overflow"};

    private JTextField[] regField;
    private JLabel[] regComment, regName, flagName, subRegName;
    private JCheckBox[] flagBox;
    private JLabel inst;
    private JTextField intText;
    private JTextField addrField;
    private JList typeBox;
    private JCheckBox stepOverInterrupts;

    private JTextField[] subRegField;
    public static final String[] sub_register_name = new String[]{"AH", "AL", "BH", "BL", "CH", "CL", "DH", "DL"};

    private String[] oldValues;
    private int valuemode = 0;        //0=hex, 1=binary, 2=decimal

    //example method, to be deleted
    public void makeDescriptor(Computer computer) {
        int base = 0x8000;
        int limit = 0xfff;
        long d = this.makeDescriptorEntry(base, limit, 0, true);
        System.out.println(Long.toHexString(d));
    }

    public RegisterGUI(Computer computer) {
        super(computer, "Registers", 600, 700, false, true, true, false);

        makeDescriptor(computer);

        regField = new JTextField[22];
        subRegField = new JTextField[8];
        regName = new JLabel[22];
        subRegName = new JLabel[8];
        regComment = new JLabel[22];
        flagBox = new JCheckBox[9];
        flagName = new JLabel[9];
        oldValues = new String[22 + 8];

        for (int i = 0; i < 22; i++) {
            regName[i] = new JLabel();
            regField[i] = new JTextField();
            regComment[i] = new JLabel();

            regName[i].setText(register_name[i]);
            regComment[i].setText("");
            final int r = i;
            regField[i].addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    writeRegister(r);
                }
            });
        }
        for (int i = 0; i < 8; i++) {
            subRegName[i] = new JLabel(sub_register_name[i]);
            subRegField[i] = new JTextField();
            final int r = i;
            subRegField[i].addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    writeSubRegister(r);
                }
            });
        }
        for (int i = 0; i < 9; i++) {
            flagBox[i] = new JCheckBox();
            flagName[i] = new JLabel();
            flagBox[i].setSelected(false);
            flagName[i].setText(flag_name[i]);
            final int r = i;
            flagBox[i].addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    writeFlag(r);
                }
            });
        }
        inst = new JLabel();
        stepOverInterrupts = new JCheckBox();
        stepOverInterrupts.setSelected(true);

        refresh();
    }

    public void closeGUI() {
        computer.registerGUI = null;
    }

    public int width() {
        return 10 + NAMEWIDTH + 10 + FIELDWIDTH + 10 + COMMENTWIDTH + 100;
    }

    public int height() {
        return ROWHEIGHT * (22 + 2 + 4 + 8 + 1);
    }

    public void writeSubRegister(int i) {
        switch (i) {
            case (AH):
                computer.processor.eax.setUpper8Value(stringToValue(subRegField[AH].getText()));
            case (AL):
                computer.processor.eax.setLower8Value(stringToValue(subRegField[AL].getText()));
            case (BH):
                computer.processor.ebx.setUpper8Value(stringToValue(subRegField[BH].getText()));
            case (BL):
                computer.processor.ebx.setLower8Value(stringToValue(subRegField[BL].getText()));
            case (CH):
                computer.processor.ecx.setUpper8Value(stringToValue(subRegField[CH].getText()));
            case (CL):
                computer.processor.ecx.setLower8Value(stringToValue(subRegField[CL].getText()));
            case (DH):
                computer.processor.edx.setUpper8Value(stringToValue(subRegField[DH].getText()));
            case (DL):
                computer.processor.edx.setLower8Value(stringToValue(subRegField[DL].getText()));
        }
    }

    public void writeRegister(int i) {
        switch (i) {
            case (EIP):
                computer.processor.eip.setValue(stringToValue(regField[EIP].getText()));
                break;
            case (EAX):
                computer.processor.eax.setValue(stringToValue(regField[EAX].getText()));
                break;
            case (EBX):
                computer.processor.ebx.setValue(stringToValue(regField[EBX].getText()));
                break;
            case (ECX):
                computer.processor.ecx.setValue(stringToValue(regField[ECX].getText()));
                break;
            case (EDX):
                computer.processor.edx.setValue(stringToValue(regField[EDX].getText()));
                break;
            case (ESI):
                computer.processor.esi.setValue(stringToValue(regField[ESI].getText()));
                break;
            case (EDI):
                computer.processor.edi.setValue(stringToValue(regField[EDI].getText()));
                break;
            case (ESP):
                computer.processor.esp.setValue(stringToValue(regField[ESP].getText()));
                break;
            case (EBP):
                computer.processor.ebp.setValue(stringToValue(regField[EBP].getText()));
                break;
            case (CR0):
                computer.processor.setCR0(stringToValue(regField[CR0].getText()));
                break;
            case (CR2):
                computer.processor.setCR2(stringToValue(regField[CR2].getText()));
                break;
            case (CR3):
                computer.processor.setCR3(stringToValue(regField[CR3].getText()));
                break;
            case (CS):
                if (computer.processor.isModeReal()) {
                    computer.processor.cs.setValue(stringToValue(regField[CS].getText()));
                    break;
                } else {
                    computer.processor.cs.setProtectedValue((stringToValue(regField[CS].getText())));
                    break;
                }
            case (SS):
                if (computer.processor.isModeReal()) {
                    computer.processor.ss.setValue(stringToValue(regField[SS].getText()));
                    break;
                } else {
                    computer.processor.ss.setProtectedValue((stringToValue(regField[SS].getText())));
                    break;
                }
            case (DS):
                if (computer.processor.isModeReal()) {
                    computer.processor.ds.setValue(stringToValue(regField[DS].getText()));
                    break;
                } else {
                    computer.processor.ds.setProtectedValue((stringToValue(regField[DS].getText())));
                    break;
                }
            case (ES):
                if (computer.processor.isModeReal()) {
                    computer.processor.es.setValue(stringToValue(regField[ES].getText()));
                    break;
                } else {
                    computer.processor.es.setProtectedValue((stringToValue(regField[ES].getText())));
                    break;
                }
            case (FS):
                if (computer.processor.isModeReal()) {
                    computer.processor.fs.setValue(stringToValue(regField[FS].getText()));
                    break;
                } else {
                    computer.processor.fs.setProtectedValue((stringToValue(regField[FS].getText())));
                    break;
                }
            case (GS):
                if (computer.processor.isModeReal()) {
                    computer.processor.gs.setValue(stringToValue(regField[GS].getText()));
                    break;
                } else {
                    computer.processor.gs.setProtectedValue((stringToValue(regField[GS].getText())));
                    break;
                }
            case (IDTR):
                if (computer.processor.idtr != null)
                    computer.processor.idtr.setDescriptorValue(stringToValue(regField[IDTR].getText()));
                break;
            case (GDTR):
                if (computer.processor.gdtr != null)
                    computer.processor.gdtr.setDescriptorValue(stringToValue(regField[GDTR].getText()));
                break;
            case (LDTR):
                if (computer.processor.ldtr != null)
                    computer.processor.ldtr.setDescriptorValue(stringToValue(regField[LDTR].getText()));
                break;
            case (TSS):
                if (computer.processor.tss != null)
                    computer.processor.tss.setDescriptorValue(stringToValue(regField[TSS].getText()));
                break;
        }
    }

    public void writeFlag(int i) {
        switch (i) {
            case (C):
                computer.processor.carry.set(flagBox[C].isSelected());
                break;
            case (P):
                computer.processor.parity.set(flagBox[P].isSelected());
                break;
            case (AC):
                computer.processor.auxiliaryCarry.set(flagBox[AC].isSelected());
                break;
            case (Z):
                computer.processor.zero.set(flagBox[Z].isSelected());
                break;
            case (S):
                computer.processor.sign.set(flagBox[S].isSelected());
                break;
            case (T):
                computer.processor.trap.set(flagBox[T].isSelected());
                break;
            case (I):
                computer.processor.interruptEnable.set(flagBox[I].isSelected());
                break;
            case (D):
                computer.processor.direction.set(flagBox[D].isSelected());
                break;
            case (O):
                computer.processor.overflow.set(flagBox[O].isSelected());
                break;
        }
    }

    private String valueToString(int value) {
        if (valuemode == 0)
            return Integer.toHexString(value);
        else if (valuemode == 1)
            return Integer.toBinaryString(value);
        else if (valuemode == 2)
            return Integer.toString(value);
        else
            return "" + toascii(value >> 24) + toascii(value >> 16) + toascii(value >> 8) + toascii(value);
    }

    private char toascii(int v) {
        v = v & 0xff;
        char w = (char) v;
        if (!Character.isLetterOrDigit(w) || v >= 0x80)
            w = '.';
        return w;
    }

    private String value8ToString(int value) {
        value = value & 0xff;
        if (valuemode == 0)
            return Integer.toHexString(value);
        else if (valuemode == 1)
            return Integer.toBinaryString(value);
        else if (valuemode == 2)
            return Integer.toString(value);
        else
            return "" + toascii(value);
    }

    private int stringToValue(String value) {
        if (valuemode == 0)
            return (int) Long.parseLong(value, 16);
        else if (valuemode == 1)
            return (int) Long.parseLong(value, 2);
        else if (valuemode == 2)
            return (int) Long.parseLong(value, 10);
        else {
            int v = 0;
            for (int i = 0; i < value.length(); i++) {
                v = v << 8;
                v += (int) value.charAt(i);
            }
            return v;
        }
    }

    public void readRegisters() {
        readRegisters(true);
    }

    public void readRegisters(boolean breakOnCSchanged) {
        if (!computer.updateGUIOnPlay && !computer.debugMode) return;

        //set a breakpoint if cs changed.  was probably an interrupt.
        if (breakOnCSchanged && !regField[CS].getText().equals("") && stringToValue(regField[CS].getText()) != computer.processor.cs.getValue()) {
            if (computer.breakpointGUI == null) {
                computer.breakpointGUI = new BreakpointGUI(computer, "( register CS == " + Long.toHexString(stringToValue(regField[CS].getText())) + " ) . ");
                computer.breakpointGUI.toBack();
                if (stepOverInterrupts.isSelected()) {
                    computer.updateGUIOnPlay = false;
                    computer.computerGUI.play();
                }
            }
        }

        subRegField[AH].setText("" + value8ToString(computer.processor.eax.getUpper8Value()));
        subRegField[AL].setText("" + value8ToString(computer.processor.eax.getLower8Value()));
        subRegField[BH].setText("" + value8ToString(computer.processor.ebx.getUpper8Value()));
        subRegField[BL].setText("" + value8ToString(computer.processor.ebx.getLower8Value()));
        subRegField[CH].setText("" + value8ToString(computer.processor.ecx.getUpper8Value()));
        subRegField[CL].setText("" + value8ToString(computer.processor.ecx.getLower8Value()));
        subRegField[DH].setText("" + value8ToString(computer.processor.edx.getUpper8Value()));
        subRegField[DL].setText("" + value8ToString(computer.processor.edx.getLower8Value()));
        regField[EIP].setText("" + valueToString(computer.processor.eip.getValue()));
        regField[EAX].setText("" + valueToString(computer.processor.eax.getValue()));
        regField[EBX].setText("" + valueToString(computer.processor.ebx.getValue()));
        regField[ECX].setText("" + valueToString(computer.processor.ecx.getValue()));
        regField[EDX].setText("" + valueToString(computer.processor.edx.getValue()));
        regField[ESP].setText("" + valueToString(computer.processor.esp.getValue()));
        regField[EBP].setText("" + valueToString(computer.processor.ebp.getValue()));
        regField[ESI].setText("" + valueToString(computer.processor.esi.getValue()));
        regField[EDI].setText("" + valueToString(computer.processor.edi.getValue()));
        regField[CR0].setText("" + valueToString(computer.processor.cr0.getValue()));
        regField[CR2].setText("" + valueToString(computer.processor.cr2.getValue()));
        regField[CR3].setText("" + valueToString(computer.processor.cr3.getValue()));
        regField[CS].setText("" + valueToString(computer.processor.cs.getValue()));
        regField[SS].setText("" + valueToString(computer.processor.ss.getValue()));
        regField[DS].setText("" + valueToString(computer.processor.ds.getValue()));
        regField[ES].setText("" + valueToString(computer.processor.es.getValue()));
        regField[FS].setText("" + valueToString(computer.processor.fs.getValue()));
        regField[GS].setText("" + valueToString(computer.processor.gs.getValue()));
        if (computer.processor.idtr != null)
            regField[IDTR].setText("" + valueToString(computer.processor.idtr.getValue()));
        if (computer.processor.gdtr != null)
            regField[GDTR].setText("" + valueToString(computer.processor.gdtr.getValue()));
        if (computer.processor.tss != null)
            regField[TSS].setText("" + valueToString(computer.processor.tss.getValue()));
        if (computer.processor.ldtr != null)
            regField[LDTR].setText("" + valueToString(computer.processor.ldtr.getValue()));

        for (int r = 0; r < regField.length; r++)
            oldValues[r] = regField[r].getText();
        for (int r = 0; r < subRegField.length; r++)
            oldValues[r + regField.length] = subRegField[r].getText();

        int csbase = computer.processor.cs.getBase();
        int cslimit = computer.processor.cs.getLimit();
        regComment[CS].setText("Base: " + valueToString(csbase) + ", Limit: " + valueToString(cslimit));
        int ssbase = computer.processor.ss.getBase();
        int sslimit = computer.processor.ss.getLimit();
        int dsbase = computer.processor.ds.getBase();
        int esbase = computer.processor.es.getBase();
        regComment[SS].setText("Base: " + valueToString(ssbase) + ", Limit: " + valueToString(sslimit));
        regComment[DS].setText("Base: " + valueToString(computer.processor.ds.getBase()) + ", Limit: " + valueToString(computer.processor.ds.getLimit()));
        regComment[ES].setText("Base: " + valueToString(computer.processor.es.getBase()) + ", Limit: " + valueToString(computer.processor.es.getLimit()));
        regComment[FS].setText("Base: " + valueToString(computer.processor.fs.getBase()) + ", Limit: " + valueToString(computer.processor.fs.getLimit()));
        regComment[GS].setText("Base: " + valueToString(computer.processor.gs.getBase()) + ", Limit: " + valueToString(computer.processor.gs.getLimit()));
        if (computer.processor.idtr != null)
            regComment[IDTR].setText("Base: " + valueToString(computer.processor.idtr.getBase()) + ", Limit: " + valueToString(computer.processor.idtr.getLimit()));
        if (computer.processor.ldtr != null)
            regComment[LDTR].setText("Base: " + valueToString(computer.processor.ldtr.getBase()) + ", Limit: " + valueToString(computer.processor.ldtr.getLimit()));
        if (computer.processor.tss != null)
            regComment[TSS].setText("Base: " + valueToString(computer.processor.tss.getBase()) + ", Limit: " + valueToString(computer.processor.tss.getLimit()));
        if (computer.processor.gdtr != null)
            regComment[GDTR].setText("Base: " + valueToString(computer.processor.gdtr.getBase()) + ", Limit: " + valueToString(computer.processor.gdtr.getLimit()));

        if (computer.processor.isModeReal()) {
            regComment[EIP].setText("Next instruction: " + valueToString(csbase + computer.processor.eip.getValue()));
            regComment[ESP].setText("Top of the stack: " + valueToString(ssbase + computer.processor.esp.getValue()));
            regComment[EDX].setText("Points to data at: " + valueToString(dsbase + computer.processor.edx.getValue()));
            regComment[ESI].setText("Points to data at: " + valueToString(dsbase + computer.processor.esi.getValue()));
            regComment[EDI].setText("Points to data at: " + valueToString(esbase + computer.processor.edi.getValue()));
        } else {
            regComment[EIP].setText("Next instruction: " + valueToString(computer.processor.cs.physicalAddress(computer.processor.eip.getValue())));
            regComment[ESP].setText("Top of the stack: " + valueToString(computer.processor.ss.physicalAddress(computer.processor.esp.getValue())));
            regComment[EDX].setText("Points to data at: " + valueToString(computer.processor.ds.physicalAddress(computer.processor.edx.getValue())));
            regComment[ESI].setText("Points to data at: " + valueToString(computer.processor.ds.physicalAddress(computer.processor.esi.getValue())));
            regComment[EDI].setText("Points to data at: " + valueToString(computer.processor.ds.physicalAddress(computer.processor.edi.getValue())));
        }
        if ((computer.processor.cr0.getValue() & 1) == 0)
            regComment[CR0].setText("Real Mode");
        else {
            regComment[CR0].setText("Protected Mode");
            if ((computer.processor.cr0.getValue() & 0x80000000) != 0)
                regComment[CR0].setText("Protected Mode with Paging");
        }

        flagBox[C].setSelected(computer.processor.carry.read());
        flagBox[P].setSelected(computer.processor.parity.read());
        flagBox[AC].setSelected(computer.processor.auxiliaryCarry.read());
        flagBox[Z].setSelected(computer.processor.zero.read());
        flagBox[S].setSelected(computer.processor.sign.read());
        flagBox[T].setSelected(computer.processor.trap.read());
        flagBox[I].setSelected(computer.processor.interruptEnable.read());
        flagBox[D].setSelected(computer.processor.direction.read());
        flagBox[O].setSelected(computer.processor.overflow.read());

        if (computer.processor.processorGUICode != null)
            inst.setText(computer.processor.processorGUICode.constructName());

        repaint();
    }

    public void constructGUI(AbstractGUI.GUIComponent guicomponent) {
        inst.setBounds(10, 1, COMMENTWIDTH, ROWHEIGHT - 2);
        guicomponent.add(inst);

        for (int i = 0; i < 22; i++) {
            regName[i].setBounds(10, (i + 1) * ROWHEIGHT + 1, NAMEWIDTH, ROWHEIGHT - 2);
            guicomponent.add(regName[i]);
            regField[i].setBounds(10 + NAMEWIDTH + 10, (i + 1) * ROWHEIGHT + 1, FIELDWIDTH, ROWHEIGHT - 2);
            guicomponent.add(regField[i]);
            if (i == EIP || i == CS || i == SS || i == DS || i == ES || i == FS || i == GS || i == ESP || i == CR0 || i == ESI || i == EDI || i == EDX) {
                regComment[i].setBounds(10 + NAMEWIDTH + 10 + FIELDWIDTH + 10, (i + 1) * ROWHEIGHT + 1, COMMENTWIDTH, ROWHEIGHT - 2);
                if (i == EDX)
                    regComment[i].setBounds(10 + NAMEWIDTH + 10 + FIELDWIDTH + 10 + NAMEWIDTH + 10 + FIELDWIDTH + 10 + NAMEWIDTH + 10 + FIELDWIDTH + 10, (i + 1) * ROWHEIGHT + 1, COMMENTWIDTH, ROWHEIGHT - 2);
                guicomponent.add(regComment[i]);
                final int j = i;
                regComment[i].addMouseListener(new MouseListener() {
                    public void mouseClicked(MouseEvent arg0) {
                        if (j == EIP || j == ESP || j == ESI || j == EDI) {
                            if (computer.memoryGUI == null)
                                computer.memoryGUI = new MemoryGUI(computer);
                        }
                        if (j == EIP)
                            computer.memoryGUI.codeFrame = new MemoryBlockGUI(computer, MemoryBlockGUI.CODE, computer.processor.cs.physicalAddress(computer.processor.eip.getValue()));
                        else if (j == ESP)
                            computer.memoryGUI.stackFrame = new MemoryBlockGUI(computer, MemoryBlockGUI.STACK, computer.processor.ss.physicalAddress(computer.processor.esp.getValue()));
                        else if (j == EDX)
                            computer.memoryGUI.dataFrame = new MemoryBlockGUI(computer, MemoryBlockGUI.DATA, computer.processor.ds.physicalAddress(computer.processor.edx.getValue()));
                        else if (j == ESI)
                            computer.memoryGUI.dataFrame = new MemoryBlockGUI(computer, MemoryBlockGUI.DATA, computer.processor.ds.physicalAddress(computer.processor.esi.getValue()));
                        else if (j == EDI)
                            computer.memoryGUI.defaultFrame = new MemoryBlockGUI(computer, MemoryBlockGUI.DATA, computer.processor.es.physicalAddress(computer.processor.edi.getValue()));
                    }

                    public void mouseEntered(MouseEvent arg0) {
                    }

                    public void mouseExited(MouseEvent arg0) {
                    }

                    public void mousePressed(MouseEvent arg0) {
                    }

                    public void mouseReleased(MouseEvent arg0) {
                    }
                });
            }
        }
        for (int i = 0; i < 8; i += 2) {
            subRegName[i].setBounds(10 + NAMEWIDTH + 10 + FIELDWIDTH + 10, (i / 2 + 2) * ROWHEIGHT + 1, NAMEWIDTH, ROWHEIGHT - 2);
            guicomponent.add(subRegName[i]);
            subRegField[i].setBounds(10 + NAMEWIDTH + 10 + FIELDWIDTH + 10 + NAMEWIDTH + 10, (i / 2 + 2) * ROWHEIGHT + 1, FIELDWIDTH, ROWHEIGHT - 2);
            guicomponent.add(subRegField[i]);
            subRegName[i + 1].setBounds(10 + NAMEWIDTH + 10 + FIELDWIDTH + 10 + NAMEWIDTH + 10 + FIELDWIDTH + 10, (i / 2 + 2) * ROWHEIGHT + 1, NAMEWIDTH, ROWHEIGHT - 2);
            guicomponent.add(subRegName[i + 1]);
            subRegField[i + 1].setBounds(10 + NAMEWIDTH + 10 + FIELDWIDTH + 10 + NAMEWIDTH + 10 + FIELDWIDTH + 10 + NAMEWIDTH + 10, (i / 2 + 2) * ROWHEIGHT + 1, FIELDWIDTH, ROWHEIGHT - 2);
            guicomponent.add(subRegField[i + 1]);
        }
        for (int i = 0; i < 9; i++) {
            flagName[i].setBounds(10 + (NAMEWIDTH + 10 + ROWHEIGHT + 10) * (i % 5) + ROWHEIGHT + 10, ROWHEIGHT * (23 + (i < 5 ? 0 : 1)) + 1, NAMEWIDTH, ROWHEIGHT - 2);
            guicomponent.add(flagName[i]);
            flagBox[i].setBounds(10 + (NAMEWIDTH + 10 + ROWHEIGHT + 10) * (i % 5), ROWHEIGHT * (23 + (i < 5 ? 0 : 1)) + 1, ROWHEIGHT - 2, ROWHEIGHT - 2);
            guicomponent.add(flagBox[i]);
        }
        JButton change = new JButton("Change values");
        change.setBounds(canvasX / 3 - 90, ROWHEIGHT * 25, 180, ROWHEIGHT - 1);
        change.addActionListener(new ButtonListener());
        guicomponent.add(change);

        JButton base = new JButton("Switch base");
        base.setBounds(2 * canvasX / 3 - 90, ROWHEIGHT * 25, 180, ROWHEIGHT - 1);
        base.addActionListener(new ButtonListener());
        guicomponent.add(base);

        JButton intr = new JButton("Interrupt");
        intr.setBounds(canvasX / 4 - 90, ROWHEIGHT * 27, 180, ROWHEIGHT - 1);
        intr.addActionListener(new ButtonListener());
        guicomponent.add(intr);

        intText = new JTextField("0");
        intText.setBounds(2 * canvasX / 4 - 20, ROWHEIGHT * 27, 40, ROWHEIGHT - 1);
        guicomponent.add(intText);

        JButton intrr = new JButton("Interrupt Return");
        intrr.setBounds(3 * canvasX / 4 - 90, ROWHEIGHT * 27, 180, ROWHEIGHT - 1);
        intrr.addActionListener(new ButtonListener());
        guicomponent.add(intrr);

        intr = new JButton("Look up address");
        intr.setBounds(10, ROWHEIGHT * 28, 180, ROWHEIGHT - 1);
        intr.addActionListener(new ButtonListener());
        guicomponent.add(intr);

        typeBox = new JList(new String[]{"Abs", "CS", "SS", "DS", "ES", "FS", "GS", "IDTR", "GDTR", "LDTR", "TSS"});
        typeBox.setSelectedIndex(0);
        JScrollPane listpane = new JScrollPane(typeBox);
        listpane.setBounds(190, ROWHEIGHT * 28, FIELDWIDTH, ROWHEIGHT);
        guicomponent.add(listpane);

        addrField = new JTextField("0");
        addrField.setBounds(190 + FIELDWIDTH + 10, ROWHEIGHT * 28, FIELDWIDTH, ROWHEIGHT);
        guicomponent.add(addrField);

        JLabel l = new JLabel("Step over interrupts?");
        l.setBounds(10 + ROWHEIGHT + ROWHEIGHT, ROWHEIGHT * 29, 200, ROWHEIGHT);
        stepOverInterrupts.setBounds(10, ROWHEIGHT * 29, ROWHEIGHT - 2, ROWHEIGHT - 2);
        guicomponent.add(l);
        guicomponent.add(stepOverInterrupts);

        JButton exittodos = new JButton("Return to DOS");
        exittodos.setBounds(200 + ROWHEIGHT * 2 + 10, ROWHEIGHT * 29, 180, ROWHEIGHT - 1);
        exittodos.addActionListener(new ButtonListener());
//		guicomponent.add(exittodos);		

        readRegisters(false);
    }

    public void doPaint(Graphics g) {
        for (int i = 0; i < 22; i++) {
            if (i % 2 == 0) g.setColor(new Color(200, 200, 200));
            else g.setColor(new Color(255, 255, 255));
            g.fillRect(0, ROWHEIGHT + i * ROWHEIGHT, canvasX, ROWHEIGHT);
        }
    }

    public long makeDescriptorEntry(int base, int limit, int privilegeLevel, boolean is32) {
        //make a gdt entry
        long descriptor = 0;
        descriptor |= limit & 0xffffl;
        descriptor |= (base & 0xffffffl) << 16;
        descriptor |= (limit & 0xf0000l) << 48;
        descriptor |= (base & 0xffffffl) << 16;
        descriptor |= (base & 0xff000000l) << 56;
        descriptor |= (is32 ? 1l : 0) << 54;    //32 bit size
        descriptor |= (1l << 47);    //is present
        descriptor |= (((long) privilegeLevel) << 45);    //lowest privilege level
        descriptor |= (12 << 40);    //other access settings: r/w data segment
        base = (int) ((0xffffffl & (descriptor >> 16)) | ((descriptor >> 32) & 0xffffffffff000000l));

        System.out.println("Base=" + Integer.toHexString(base) + " limit=" + Integer.toHexString(limit) + " descriptor=" + Long.toHexString(descriptor));
        return descriptor;
    }

    public class ButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals("Change values")) {
                for (int i = 0; i < regField.length; i++) {
                    if (!oldValues[i].equals(regField[i].getText()))
                        writeRegister(i);
                }
                for (int i = 0; i < subRegField.length; i++) {
                    if (!oldValues[i + regField.length].equals(subRegField[i].getText()))
                        writeSubRegister(i);
                }
                for (int i = 0; i < flagBox.length; i++)
                    writeFlag(i);
                readRegisters(false);
            } else if (e.getActionCommand().equals("Switch base")) {
                valuemode = (valuemode + 1) % 4;
                readRegisters(false);
            } else if (e.getActionCommand().equals("Interrupt")) {
                int interrupt = stringToValue(intText.getText());
                computer.processor.handleInterrupt(interrupt);
                readRegisters(false);
            } else if (e.getActionCommand().equals("Return to DOS")) {
                int interrupt = 0x21;
                computer.processor.eax.setValue(0);
                computer.processor.handleInterrupt(interrupt);
                readRegisters(false);
                computer.updateGUIOnPlay = false;
                computer.computerGUI.play();
            } else if (e.getActionCommand().equals("Interrupt Return")) {
                computer.processor.iret(!computer.processor.isModeReal(), !computer.processor.isModeReal());
                readRegisters(false);
            } else if (e.getActionCommand().equals("Push Regs")) {
                computer.processor.pushad();
                readRegisters(false);
            } else if (e.getActionCommand().equals("Pop Regs")) {
                computer.processor.popad();
                readRegisters(false);
            } else if (e.getActionCommand().equals("Look up address")) {
                if (computer.memoryGUI == null)
                    computer.memoryGUI = new MemoryGUI(computer);
                int address = stringToValue(addrField.getText());
                switch (typeBox.getSelectedIndex()) {
                    case 0:
                        if (!computer.processor.isModeReal())
                            addrField.setText(valueToString(computer.linearMemory.virtualAddressLookup(address)));
                        break;
                    case 1:
                        addrField.setText(valueToString(computer.processor.cs.physicalAddress(address)));
                        computer.memoryGUI.codeFrame = new MemoryBlockGUI(computer, MemoryBlockGUI.CODE, computer.processor.cs.physicalAddress(address));
                        break;
                    case 2:
                        addrField.setText(valueToString(computer.processor.ss.physicalAddress(address)));
                        break;
                    case 3:
                        addrField.setText(valueToString(computer.processor.ds.physicalAddress(address)));
                        break;
                    case 4:
                        addrField.setText(valueToString(computer.processor.es.physicalAddress(address)));
                        break;
                    case 5:
                        addrField.setText(valueToString(computer.processor.fs.physicalAddress(address)));
                        break;
                    case 6:
                        addrField.setText(valueToString(computer.processor.gs.physicalAddress(address)));
                        break;
                    case 7:
                        addrField.setText(valueToString(computer.processor.idtr.physicalAddress(address)));
                        break;
                    case 8:
                        addrField.setText(valueToString(computer.processor.gdtr.physicalAddress(address)));
                        break;
                    case 9:
                        addrField.setText(valueToString(computer.processor.ldtr.physicalAddress(address)));
                        break;
                    case 10:
                        addrField.setText(valueToString(computer.processor.tss.physicalAddress(address)));
                        break;
                }

                if (typeBox.getSelectedIndex() != 1)
                    computer.memoryGUI.dataFrame = new MemoryBlockGUI(computer, MemoryBlockGUI.DATA, stringToValue(addrField.getText()));
            }
        }
    }
}
