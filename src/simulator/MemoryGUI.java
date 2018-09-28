package simulator;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

public class MemoryGUI extends AbstractGUI {
    private static final int BLOCKSIZE = 0x100;        //256 bytes/block
    //	private static final int TOTAL_BLOCKS=(int)(0x100000000l/BLOCKSIZE);
    private static final int TOTAL_BLOCKS = (int) (PhysicalMemory.TOTAL_RAM_SIZE / BLOCKSIZE);

    public int lastCodeRead = -1, lastCodeWrite = -1, lastDataRead = -1, lastDataWrite = -1, lastStackRead = -1, lastStackWrite = -1, lastExtraRead = -1, lastExtraWrite = -1, lastInterruptRead = -1, lastInterruptWrite = -1;
    public boolean memoryRead = false, memoryWrite = false, romRead = false;

    public MemoryBlockGUI codeFrame = null, stackFrame = null, dataFrame = null, interruptFrame = null, defaultFrame = null;

    public static final int BLOCKWIDTH = 10;
    public static final int BLOCKHEIGHT = 6;

    public MemoryOverlay overlay;


    public MemoryGUI(Computer computer) {
        super(computer, "Memory", 600, 500, true, true, true, false);
        overlay = new SegmentOverlay();
        refresh();
    }

    public void closeGUI() {
        computer.memoryGUI = null;
    }

    public int width() {
        return canvasX - 20;
    }

    public int height() {
        int numBlocks = TOTAL_BLOCKS;
        return BLOCKHEIGHT * (numBlocks / (width() / BLOCKWIDTH) + 1);
    }

    public void doPaint(Graphics g) {
        int xblocks = width() / BLOCKWIDTH;
        int yblocks = height() / BLOCKHEIGHT;
        for (int y = 0; y < yblocks; y++) {
            int visibleStart = yblocks * scrollPane.getVerticalScrollBar().getValue() / scrollPane.getVerticalScrollBar().getMaximum();
            int visibleEnd = visibleStart + scrollPane.getVerticalScrollBar().getVisibleAmount() * yblocks / scrollPane.getVerticalScrollBar().getMaximum();

            if (y < visibleStart - 5 || y > visibleEnd + 5)
                continue;

            if (y * xblocks >= TOTAL_BLOCKS)
                break;

            for (int x = 0; x < xblocks; x++) {
                //don't depict non-existent blocks
                int blockNumber = y * xblocks + x;
                if (blockNumber >= TOTAL_BLOCKS)
                    break;

                g.setColor(overlay.addressColor(blockNumber * BLOCKSIZE));

                g.fillRect(x * BLOCKWIDTH, y * BLOCKHEIGHT, BLOCKWIDTH - 1, BLOCKHEIGHT - 1);
            }
        }
    }

    public void mouseMove(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        if (x < 0 || x >= width())
            return;
        if (y < 0 || y >= height())
            return;
        int blockx = x / BLOCKWIDTH;
        int blocky = y / BLOCKHEIGHT;
        int block = blocky * (width() / BLOCKWIDTH) + blockx;
        if (block >= TOTAL_BLOCKS)
            return;
        repaint();
        String label = overlay.addressLabel(block * BLOCKSIZE);
        label = "Address " + Integer.toHexString(block * BLOCKSIZE) + ": " + label;
        setStatusLabel(label);
    }

    public void mouseExit(MouseEvent e) {
        setStatusLabel("");
    }

    public void mouseClick(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        if (x < 0 || x >= width())
            return;
        if (y < 0 || y >= height())
            return;

        if (e.getButton() == MouseEvent.BUTTON3) {
            statusEdit("View address: ", -1, false);
            return;
        }

        int blockx = x / BLOCKWIDTH;
        int blocky = y / BLOCKHEIGHT;
        int block = blocky * (width() / BLOCKWIDTH) + blockx;
        if (block >= TOTAL_BLOCKS)
            return;

        if (overlay.addressType(block * BLOCKSIZE) == 4)
            codeFrame = new MemoryBlockGUI(computer, MemoryBlockGUI.CODE, computer.processor.cs.physicalAddress(computer.processor.eip.getValue()));
        else if (overlay.addressType(block * BLOCKSIZE) == 3)
            codeFrame = new MemoryBlockGUI(computer, MemoryBlockGUI.CODE, block * BLOCKSIZE);
        else if (overlay.addressType(block * BLOCKSIZE) == 8)
            stackFrame = new MemoryBlockGUI(computer, MemoryBlockGUI.STACK, computer.processor.ss.physicalAddress(computer.processor.esp.getValue()));
        else if (overlay.addressType(block * BLOCKSIZE) == 7)
            stackFrame = new MemoryBlockGUI(computer, MemoryBlockGUI.STACK, block * BLOCKSIZE);
        else if (overlay.addressType(block * BLOCKSIZE) == 6 || overlay.addressType(block * BLOCKSIZE) == 5)
            dataFrame = new MemoryBlockGUI(computer, MemoryBlockGUI.DATA, block * BLOCKSIZE);
        else if (overlay.addressType(block * BLOCKSIZE) == 10 || overlay.addressType(block * BLOCKSIZE) == 9)
            dataFrame = new MemoryBlockGUI(computer, MemoryBlockGUI.DATA, block * BLOCKSIZE);
        else
            defaultFrame = new MemoryBlockGUI(computer, MemoryBlockGUI.NOTHING, block * BLOCKSIZE);
    }

    public void statusEdited(String keys) {
        keys = keys.toLowerCase();
        if (keys.equals("i"))
            codeFrame = new MemoryBlockGUI(computer, MemoryBlockGUI.CODE, computer.processor.cs.physicalAddress(computer.processor.eip.getValue()));
        else if (keys.equals("s"))
            stackFrame = new MemoryBlockGUI(computer, MemoryBlockGUI.STACK, computer.processor.ss.physicalAddress(computer.processor.esp.getValue()));
        else if (keys.charAt(0) == 'i')
            defaultFrame = new MemoryBlockGUI(computer, MemoryBlockGUI.CODE, Integer.parseInt(keys.substring(1, keys.length()), 16));
        else if (keys.charAt(0) == 's')
            defaultFrame = new MemoryBlockGUI(computer, MemoryBlockGUI.STACK, Integer.parseInt(keys.substring(1, keys.length()), 16));
        else if (keys.charAt(0) == 'd')
            defaultFrame = new MemoryBlockGUI(computer, MemoryBlockGUI.DATA, Integer.parseInt(keys.substring(1, keys.length()), 16));
        else if (keys.charAt(0) == 'v')
            defaultFrame = new MemoryBlockGUI(computer, MemoryBlockGUI.DATA, computer.linearMemory.virtualAddressLookup(Integer.parseInt("0" + keys.substring(1, keys.length()), 16)));
        else
            defaultFrame = new MemoryBlockGUI(computer, MemoryBlockGUI.NOTHING, Integer.parseInt(keys, 16));
    }

    public abstract class MemoryOverlay {
        protected Color[] color;
        protected String[] label;

        public Color addressColor(int address) {
            return color[addressType(address)];
        }

        public String addressLabel(int address) {
            return label[addressType(address)];
        }

        protected abstract int addressType(int address);
    }

    public class SegmentOverlay extends MemoryOverlay {
        public SegmentOverlay() {
            label = new String[]
                    {
                            "not present",
                            "unallocated base memory",
                            "unallocated extended memory",
                            "code segment",
                            "current instruction",
                            "data segment",
                            "last data access",
                            "stack segment",
                            "top of the stack",
                            "extra segment",
                            "last extra access",
                            "interrupt table",
                            "BIOS ROM",
                            "VGA Video RAM",
                            "CGA/Text Video RAM",
                            "VGA ROM",
                    };
            color = new Color[]
                    {
                            new Color(0, 0, 0),
                            new Color(200, 200, 200),
                            new Color(200, 200, 200),
                            new Color(150, 150, 255),
                            new Color(0, 0, 200),
                            new Color(150, 255, 150),
                            new Color(0, 200, 0),
                            new Color(255, 150, 150),
                            new Color(200, 0, 0),
                            new Color(150, 255, 150),
                            new Color(0, 200, 0),
                            new Color(150, 50, 150),
                            new Color(100, 100, 100),
                            new Color(150, 50, 150),
                            new Color(150, 50, 150),
                            new Color(100, 100, 100),
                    };
        }

        public int addressType(int address) {
            //these are in display priority order
            if (computer.processor.cs.address(computer.processor.eip.getValue()) / BLOCKSIZE == address / BLOCKSIZE)
                return 4;    //current IP
            if (computer.processor.ss.address(computer.processor.esp.getValue()) / BLOCKSIZE == address / BLOCKSIZE)
                return 8;    //current SP
            if (lastDataRead / BLOCKSIZE == address / BLOCKSIZE || lastDataWrite / BLOCKSIZE == address / BLOCKSIZE)
                return 6;    //last data access
            if (lastExtraRead / BLOCKSIZE == address / BLOCKSIZE || lastExtraWrite / BLOCKSIZE == address / BLOCKSIZE)
                return 10;    //last extra access
            if (address >= 0xc0000 && address < 0xd0000)
                return 15;    //vga rom
            if (address >= 0xf0000 && address < 0x100000)
                return 12;    //bios
            if (address >= 0xa0000 && address < 0xb0000)
                return 13;    //vga ram
            if (address >= 0xb0000 && address < 0xc0000)
                return 14;    //text video
            if (address >= computer.processor.cs.getBase() && address < computer.processor.cs.getBase() + computer.processor.cs.getLimit())
                return 3;    //code segment
            if (address >= computer.processor.ss.getBase() && address < computer.processor.ss.getBase() + computer.processor.ss.getLimit())
                return 7;    //stack segment
            if (address >= computer.processor.ds.getBase() && address < computer.processor.ds.getBase() + computer.processor.ds.getLimit())
                return 5;    //data segment
            if (address >= computer.processor.es.getBase() && address < computer.processor.es.getBase() + computer.processor.es.getLimit())
                return 9;    //extra segment
            if (address >= computer.processor.fs.getBase() && address < computer.processor.fs.getBase() + computer.processor.es.getLimit())
                return 9;    //extra segment
            if (address >= computer.processor.gs.getBase() && address < computer.processor.gs.getBase() + computer.processor.es.getLimit())
                return 9;    //extra segment
            if (address / BLOCKSIZE == computer.processor.idtr.getBase() / BLOCKSIZE)
                return 11;    //interrupt table
            if (address >= 0 && address < 0xa0000)
                return 1;    //base ram
            if (address >= 0x100000)
                return 2;    //extended ram
            return 0;        //hole
        }

    }

    public class TypeOverlay extends MemoryOverlay {
        public TypeOverlay() {
            label = new String[]
                    {
                            "Base RAM",
                            "VGA Video RAM",
                            "CGA/Text Video RAM",
                            "VGA ROM",
                            "BIOS ROM",
                            "Extended RAM",
                            "not present",
                    };

            color = new Color[]
                    {
                            new Color(0, 150, 0),
                            new Color(0, 0, 150),
                            new Color(0, 0, 250),
                            new Color(150, 0, 0),
                            new Color(250, 0, 0),
                            new Color(0, 250, 0),
                            new Color(0, 0, 0),
                    };
        }

        protected int addressType(int address) {
            if (address >= 0 && address < 0x80000)
                return 0;
            if (address >= 0xa0000 && address < 0xb0000)
                return 1;
            if (address >= 0xb0000 && address < 0xc0000)
                return 2;
            if (address >= 0xc0000 && address < 0xd0000)
                return 3;
            if (address >= 0xf0000 && address < 0x100000)
                return 4;
            if (address >= 0x100000)
                return 5;
            return 6;
        }
    }

    public void codeRead(int address) {
        lastCodeRead = address;
        if (address >= 0xf0000 && address <= 0xfffff)
            romRead = true;
        else
            memoryRead = true;
        repaint();
        if (codeFrame != null) {
            codeFrame.setStatusLabel("Reading from " + Integer.toHexString(address));
            codeFrame.update(address);
        }
    }

    public void codeWrite(int address) {
        lastCodeWrite = address;
        memoryWrite = true;
        repaint();
        if (codeFrame != null) {
            codeFrame.update(address);
            codeFrame.setStatusLabel("Writing to " + Integer.toHexString(address));
        }
    }

    public void stackRead(int address) {
        lastStackRead = address;
        if (address >= 0xf0000 && address <= 0xfffff)
            romRead = true;
        else
            memoryRead = true;
        repaint();
        if (stackFrame != null) {
            stackFrame.setStatusLabel("Reading from " + Integer.toHexString(address));
            stackFrame.update(address);
        }
    }

    public void stackWrite(int address) {
        lastStackWrite = address;
        memoryWrite = true;
        repaint();
        if (stackFrame != null) {
            stackFrame.update(address);
            stackFrame.setStatusLabel("Writing to " + Integer.toHexString(address));
        }
    }

    public void stackWrite(int address, String information, int size) {
        lastStackWrite = address;
        memoryWrite = true;
        repaint();
        if (stackFrame != null) {
            stackFrame.update(address, information, size);
            stackFrame.setStatusLabel("Writing to " + Integer.toHexString(address));
        }
    }

    public void dataRead(int address) {
        lastDataRead = address;
        if (address >= 0xf0000 && address <= 0xfffff)
            romRead = true;
        else
            memoryRead = true;
        repaint();
        if (dataFrame != null) {
            dataFrame.setStatusLabel("Reading from " + Integer.toHexString(address));
            dataFrame.update(address);
        }
    }

    public void dataWrite(int address) {
        lastDataWrite = address;
        memoryWrite = true;
        repaint();
        if (dataFrame != null) {
            dataFrame.update(address);
            dataFrame.setStatusLabel("Writing to " + Integer.toHexString(address));
        }
    }

    public void extraRead(int address) {
        lastExtraRead = address;
        if (address >= 0xf0000 && address <= 0xfffff)
            romRead = true;
        else
            memoryRead = true;
        repaint();
        if (dataFrame != null) {
            dataFrame.setStatusLabel("Reading from " + Integer.toHexString(address));
            dataFrame.update(address);
        }
    }

    public void extraWrite(int address) {
        lastExtraWrite = address;
        memoryWrite = true;
        repaint();
        if (dataFrame != null) {
            dataFrame.update(address);
            dataFrame.setStatusLabel("Writing to " + Integer.toHexString(address));
        }
    }

    public void interruptRead(int address) {
        lastInterruptRead = address;
        if (address >= 0xf0000 && address <= 0xfffff)
            romRead = true;
        else
            memoryRead = true;
        repaint();
        if (interruptFrame != null) {
            interruptFrame.setStatusLabel("Reading from " + Integer.toHexString(address));
            interruptFrame.update(address);
        }
    }

    public void interruptWrite(int address) {
        lastInterruptWrite = address;
        memoryWrite = true;
        repaint();
        if (interruptFrame != null) {
            interruptFrame.update(address);
            interruptFrame.setStatusLabel("Writing to " + Integer.toHexString(address));
        }
    }

    public void updateIP(int address) {
        repaint();
        if (codeFrame != null) {
            codeFrame.lastIP = codeFrame.IP;
            codeFrame.IP = address;
            codeFrame.setStatusLabel("IP = " + Integer.toHexString(address));
            codeFrame.update(codeFrame.lastIP);
        }
    }
}
