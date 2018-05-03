package simulator;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

public class DiskGUI extends AbstractGUI {
    private int cylinders, heads, sectors;
    private String name;
    private Disk disk;

    private int currentTrack;
    private int currentSector;
    private int currentHead;
    private int currentOperation = -1;        //1=read,2=write,0=show last operation,-1=show nothing

    public boolean isFloppy;
    public int id;
    private static final int W = 600, H = 500;

    private final int DISKSIZE = H - MARGIN * 2;

    private static final Color COLOR_BACKGROUND = new Color(220, 220, 220);
    private static final Color COLOR_BORDER = Color.BLACK;
    private static final Color COLOR_INNER_CIRCLE = new Color(50, 50, 50);
    ;
    private static final Color COLOR_TRACK_HIGHLIGHT = Color.WHITE;
    private static final Color COLOR_READ = Color.GREEN;
    private static final Color COLOR_WRITE = Color.RED;
    private static final Color COLOR_LASTOP = Color.BLACK;

    public DiskGUI(Computer computer, int id) {
        super(computer, "", W, H, true, true, true, false);
        this.isFloppy = id < 2;
        this.id = id;
    }

    public void closeGUI() {
        computer.diskGUI[id] = null;
    }

    public void read(int sector) {
        if (sectors * heads == 0) return;
        currentTrack = (sector / (sectors * heads));
        currentHead = ((sector / sectors) % heads);
        currentSector = sector % sectors;
        currentOperation = 1;
        repaint();
    }

    public void write(int sector) {
        if (sectors * heads == 0) return;
        currentTrack = (sector / (sectors * heads));
        currentHead = ((sector / sectors) % heads);
        currentSector = sector % sectors;
        currentOperation = 2;
        repaint();
    }

    public void redraw(String name, int cylinders, int heads, int sectors, Disk disk) {
        this.cylinders = cylinders;
        this.heads = heads;
        this.sectors = sectors;
        this.name = name;
        this.disk = disk;

        refresh();
        setTitle(name);
    }

    private void drawLegend(Graphics g) {
        int top_y = MARGIN;
        int fontSize = W / 50 + 2;
        int w = fontSize * 12;
        int top_x = W - w - MARGIN;
        g.setFont(new Font("Dialog", Font.BOLD, fontSize));
        g.setColor(Color.WHITE);
        g.fillRect(top_x, top_y, w, fontSize * 5 + 4);
        g.setColor(Color.BLACK);
        g.drawRect(top_x, top_y, w, fontSize * 5 + 4);
        g.drawString(name, top_x, top_y + fontSize * 1);
        g.drawLine(top_x, top_y + fontSize + 1, top_x + w, top_y + fontSize + 1);
        g.setFont(new Font("Dialog", 0, fontSize));
        g.drawString("Cylinders " + cylinders, top_x + 2, top_y + fontSize * 2 + 3);
        g.drawString("Heads " + heads, top_x + 2, top_y + fontSize * 3 + 3);
        g.drawString("Sectors " + sectors, top_x + 2, top_y + fontSize * 4 + 3);
        g.drawString("Total bytes " + cylinders * sectors * heads * 512, top_x + 2, top_y + fontSize * 5 + 3);
    }

    private void drawStatus(Graphics g) {
        String text = "";
        if (currentOperation == 0)
            text += "Last access: ";
        else if (currentOperation == 1)
            text += "Reading from ";
        else if (currentOperation == 2)
            text += "Writing to ";
        else
            return;
        text += "track " + Integer.toHexString(currentTrack) + " head " + Integer.toHexString(currentHead) + " sector " + Integer.toHexString(currentSector);
        setStatusLabel(text);
    }

    private void drawDisk(Graphics g) {
        int circle_size = DISKSIZE;
        int circle_start = MARGIN;
        int inner_circle_size = 20;
        int inner_circle_start = circle_start + circle_size / 2 - inner_circle_size / 2;

        //highlight the current track
        g.setColor(COLOR_TRACK_HIGHLIGHT);
        int width = (circle_size - inner_circle_size) / cylinders / 2;
        if (width == 0) width = 1;
        int start = circle_start + currentTrack * (inner_circle_start - circle_start) / cylinders;
        int size = circle_size - currentTrack * (circle_size - inner_circle_size) / cylinders;
        for (int i = 0; i < width; i++) {
            g.drawOval(start, start, size, size);
            start++;
            size -= 2;
        }

        //highlight the current sector
        switch (currentOperation) {
            case 0:
                g.setColor(COLOR_LASTOP);
                break;
            case 1:
                g.setColor(COLOR_READ);
                break;
            case 2:
                g.setColor(COLOR_WRITE);
                break;
            default:
                g.setColor(COLOR_TRACK_HIGHLIGHT);
                break;
        }
        int swidth = (circle_size - inner_circle_size) / cylinders / 2;
        if (swidth == 0) swidth = 1;
        int sstart = circle_start + currentTrack * (inner_circle_start - circle_start) / cylinders;
        int ssize = circle_size - currentTrack * (circle_size - inner_circle_size) / cylinders;
        int startangle = -(int) (currentSector * 360 / sectors - 90);
        int anglesize = -360 / sectors;
        if (anglesize == 0) anglesize = -1;

        for (int i = 0; i < swidth; i++) {
            g.drawArc(sstart, sstart, ssize, ssize, startangle, anglesize);
            sstart++;
            ssize -= 2;
        }

        //draw the outer circle and inner circle
        g.setColor(COLOR_INNER_CIRCLE);
        g.fillOval(inner_circle_start, inner_circle_start, inner_circle_size, inner_circle_size);
        g.setColor(COLOR_BORDER);
        g.drawOval(circle_start, circle_start, circle_size, circle_size);
        g.drawOval(inner_circle_start, inner_circle_start, inner_circle_size, inner_circle_size);

        //draw the tracks only if there's enough space
        g.setColor(COLOR_BORDER);
        if (cylinders * 8 < circle_size - inner_circle_size) {
            for (int c = 0; c < cylinders; c++) {
                int cylstart = circle_start + c * (inner_circle_start - circle_start) / cylinders;
                int cylsize = circle_size - c * (circle_size - inner_circle_size) / cylinders;
                g.drawOval(cylstart, cylstart, cylsize, cylsize);
            }
        }

        //draw the sectors only if there's enough space
        g.setColor(COLOR_BORDER);
        int center = circle_start + circle_size / 2;
        if (sectors * 8 < circle_size - inner_circle_size) {
            for (int s = 0; s < sectors; s++) {
                double angle = s * 2 * Math.PI / sectors;
                int xend = (int) (center + Math.sin(angle) * circle_size / 2);
                int yend = (int) (center - Math.cos(angle) * circle_size / 2);
                g.drawLine(center, center, xend, yend);
            }
        }
    }

    public void doPaint(Graphics g) {
        g.setColor(COLOR_BACKGROUND);
        g.fillRect(0, 0, W, H);

        drawDisk(g);
        drawLegend(g);
        drawStatus(g);
    }
}
