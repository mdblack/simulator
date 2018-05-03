package simulator;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class SerialGUI extends AbstractGUI {
    public static final int MAXHISTORY = 20;
    public static final int XSIZE = 300, YSIZE = 160 + MAXHISTORY * 20;

    private boolean transmitting = false, receiving = false;
    private int[] lasttransmit = new int[12];
    private int[] lastreceive = new int[12];

    private byte[] receivehistory = new byte[MAXHISTORY];
    private int receivehistorysize = 0;
    private byte[] transmithistory = new byte[MAXHISTORY];
    private int transmithistorysize = 0;

    private int edittype;

    public SerialGUI(Computer computer) {
        super(computer, "Serial Port", XSIZE, YSIZE, true, true, true, false);
        refresh();
    }

    public void closeGUI() {
        computer.serialGUI = null;
    }

    public void constructGUI(GUIComponent g) {
        JButton b = new JButton("Connect");
        b.setBounds(330, 25, 100, 30);
        b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            }
        });
        g.add(b);
    }

    public void doPaint(Graphics g) {
        //set background color

        g.setColor(new Color(0xfb, 0xf1, 0xb5));
        g.fillRect(0, 0, width(), height());

        //draw DB9 connector

        g.setColor(Color.BLACK);
        g.drawLine(20, 20, 170, 20);
        g.drawLine(50, 70, 140, 70);
        g.drawLine(20, 20, 50, 70);
        g.drawLine(170, 20, 140, 70);

        g.drawOval(51 - 4, 37 - 4, 8, 8);
        g.setColor(Color.GREEN);
        if (receiving)
            g.fillOval(72 - 4, 37 - 4, 8, 8);
        else
            g.drawOval(72 - 4, 37 - 4, 8, 8);
        g.setColor(Color.RED);
        if (transmitting)
            g.fillOval(93 - 4, 37 - 4, 8, 8);
        else
            g.drawOval(93 - 4, 37 - 4, 8, 8);
        g.setColor(Color.BLACK);
        g.drawOval(114 - 4, 37 - 4, 8, 8);
        g.fillOval(135 - 4, 37 - 4, 8, 8);

        g.drawOval(62 - 4, 53 - 4, 8, 8);
        g.drawOval(84 - 4, 53 - 4, 8, 8);
        g.drawOval(106 - 4, 53 - 4, 8, 8);
        g.drawOval(128 - 4, 53 - 4, 8, 8);

        //draw parameter box
        g.setColor(Color.WHITE);
        g.fillRect(200, 20, 100, 50);
        g.setColor(Color.BLACK);
        g.drawRect(200, 20, 100, 50);
        g.setFont(new Font("Dialog", Font.PLAIN, 10));
        g.drawString("Baud: " + computer.serialport.baud, 205, 20 + 15);
        g.drawString("Stop bits: " + computer.serialport.stopbits, 205, 20 + 30);
        g.drawString("Parity: " + (computer.serialport.parity == 0 ? "none" : (computer.serialport.parity == 1 ? "odd" : "even")), 205, 20 + 45);

        //draw last transmitted, last received
        g.setColor(new Color(255, 255, 255));
        g.fillRect(0, 90, width(), 20);
        g.setColor(new Color(255, 220, 220));
        g.fillRect(0, 110, width(), 20);
        g.setColor(new Color(220, 255, 220));
        g.fillRect(0, 130, width(), 20);
        g.setColor(Color.BLACK);
        g.drawString("start", 70, 105);
        for (int i = 0; i < 8; i++)
            g.drawString("" + i, 100 + i * 30, 105);
        if (computer.serialport.parity != 0) {
            g.drawString("parity", 100 + 8 * 30, 105);
            g.drawString("stop", 100 + 9 * 30, 105);
            if (computer.serialport.stopbits == 2)
                g.drawString("stop", 100 + 10 * 30, 105);
        } else {
            g.drawString("stop", 100 + 8 * 30, 105);
            if (computer.serialport.stopbits == 2)
                g.drawString("stop", 100 + 9 * 30, 105);
        }
        g.drawString("Transmitted: ", 0, 125);
        g.drawString("Received: ", 0, 145);
        for (int i = 0; i < 12; i++) {
            g.setColor(new Color(100, 0, 0));
            if (lasttransmit[i] == 1)
                g.drawString("1", 70 + i * 30, 125);
            else if (lasttransmit[i] == -1)
                g.drawString("0", 70 + i * 30, 125);
            g.setColor(new Color(0, 100, 0));
            if (lastreceive[i] == 1)
                g.drawString("1", 70 + i * 30, 145);
            else if (lastreceive[i] == -1)
                g.drawString("0", 70 + i * 30, 145);
        }

        //draw history
        for (int i = 0; i < MAXHISTORY + 1; i++) {
            if (i % 2 == 0)
                g.setColor(new Color(255, 230, 230));
            else
                g.setColor(new Color(225, 200, 200));
            g.fillRect(0, 160 + i * 20, width() / 2, 20);
            g.setColor(new Color(50, 0, 0));
            if (i == 0)
                g.drawString("Transmit History", 10, 160 + i * 20 + 15);
            else if (i - 1 < transmithistorysize)
                g.drawString(Integer.toHexString(0xff & transmithistory[i - 1]), 10, 160 + i * 20 + 15);
            if (i % 2 == 0)
                g.setColor(new Color(230, 255, 230));
            else
                g.setColor(new Color(200, 225, 200));
            g.fillRect(width() / 2, 160 + i * 20, width() / 2, 20);
            g.setColor(new Color(0, 50, 0));
            if (i == 0)
                g.drawString("Receive History", 10 + width() / 2, 160 + i * 20 + 15);
            else if (i - 1 < receivehistorysize)
                g.drawString(Integer.toHexString(0xff & receivehistory[i - 1]), 10 + width() / 2, 160 + i * 20 + 15);
        }

    }

    /*	public int width()
        {
            return XSIZE;
        }*/
    public int height() {
        return YSIZE;
    }

    public void transmit(byte b) {
        transmitting = true;
        lasttransmit = new int[12];
        repaint();
        //start bit
        lasttransmit[0] = -1;
        repaint();
        //data bits
        for (int i = 0; i < 8; i++) {
            if ((b >>> i) % 2 == 1)
                lasttransmit[i + 1] = 1;
            else
                lasttransmit[i + 1] = -1;
            repaint();
        }
        //parity bit
        int p = 0;
        for (int i = 0; i < 8; i++)
            p = p + ((b >>> i) % 2);
        if (computer.serialport.parity == 1) {
            if (p % 2 == 0)
                lasttransmit[9] = 1;
            else
                lasttransmit[9] = -1;
        } else if (computer.serialport.parity == 2) {
            if (p % 2 == 1)
                lasttransmit[9] = 1;
            else
                lasttransmit[9] = -1;
        }
        repaint();
        //stop bit(s)
        if (computer.serialport.parity == 0) {
            lasttransmit[9] = -1;
            repaint();
            if (computer.serialport.stopbits == 2) {
                lasttransmit[10] = -1;
                repaint();
            }
        } else {
            lasttransmit[10] = -1;
            repaint();
            if (computer.serialport.stopbits == 2) {
                lasttransmit[11] = -1;
                repaint();
            }
        }
        transmitting = false;

        transmithistorysize++;
        if (transmithistorysize >= MAXHISTORY)
            transmithistorysize = MAXHISTORY - 1;
        for (int i = transmithistorysize; i > 0; i--)
            transmithistory[i] = transmithistory[i - 1];
        transmithistory[0] = b;

        repaint();
    }

    public void receive(byte b) {
        receiving = true;
        lastreceive = new int[12];
        repaint();
        //start bit
        lastreceive[0] = -1;
        repaint();
        //data bits
        for (int i = 0; i < 8; i++) {
            if ((b >>> i) % 2 == 1)
                lastreceive[i + 1] = 1;
            else
                lastreceive[i + 1] = -1;
            repaint();
        }
        //parity bit
        int p = 0;
        for (int i = 0; i < 8; i++)
            p = p + ((b >>> i) % 2);
        if (computer.serialport.parity == 1) {
            if (p % 2 == 0)
                lastreceive[9] = 1;
            else
                lastreceive[9] = -1;
        } else if (computer.serialport.parity == 2) {
            if (p % 2 == 1)
                lastreceive[9] = 1;
            else
                lastreceive[9] = -1;
        }
        repaint();
        //stop bit(s)
        if (computer.serialport.parity == 0) {
            lastreceive[9] = -1;
            repaint();
            if (computer.serialport.stopbits == 2) {
                lastreceive[10] = -1;
                repaint();
            }
        } else {
            lastreceive[10] = -1;
            repaint();
            if (computer.serialport.stopbits == 2) {
                lastreceive[11] = -1;
                repaint();
            }
        }
        receiving = false;

        receivehistorysize++;
        if (receivehistorysize >= MAXHISTORY)
            receivehistorysize = MAXHISTORY - 1;
        for (int i = receivehistorysize; i > 0; i--)
            receivehistory[i] = receivehistory[i - 1];
        receivehistory[0] = b;

        repaint();
    }

    public void mouseMove(MouseEvent e) {
        if (e.getX() >= 51 - 4 && e.getX() < 51 + 4 && e.getY() > 37 - 4 && e.getY() < 37 + 4)
            setStatusLabel("Pin 1: Data Carrier Detect (DCD)");
        else if (e.getX() >= 72 - 4 && e.getX() < 72 + 4 && e.getY() > 37 - 4 && e.getY() < 37 + 4)
            setStatusLabel("Pin 2: Received Data (RxD)");
        else if (e.getX() >= 93 - 4 && e.getX() < 93 + 4 && e.getY() > 37 - 4 && e.getY() < 37 + 4)
            setStatusLabel("Pin 3: Transmitted Data (TxD)");
        else if (e.getX() >= 114 - 4 && e.getX() < 114 + 4 && e.getY() > 37 - 4 && e.getY() < 37 + 4)
            setStatusLabel("Pin 4: Data Terminal Ready (DTR)");
        else if (e.getX() >= 135 - 4 && e.getX() < 135 + 4 && e.getY() > 37 - 4 && e.getY() < 37 + 4)
            setStatusLabel("Pin 5: Ground (GND)");
        else if (e.getX() >= 62 - 4 && e.getX() < 62 + 4 && e.getY() > 53 - 4 && e.getY() < 53 + 4)
            setStatusLabel("Pin 6: Data Set Ready (DSR)");
        else if (e.getX() >= 84 - 4 && e.getX() < 84 + 4 && e.getY() > 53 - 4 && e.getY() < 53 + 4)
            setStatusLabel("Pin 7: Request To Send (RTS)");
        else if (e.getX() >= 106 - 4 && e.getX() < 106 + 4 && e.getY() > 53 - 4 && e.getY() < 53 + 4)
            setStatusLabel("Pin 8: Clear To Send (CTS)");
        else if (e.getX() >= 128 - 4 && e.getX() < 128 + 4 && e.getY() > 53 - 4 && e.getY() < 53 + 4)
            setStatusLabel("Pin 9: Ring Indicator (RI)");
        else if (e.getY() >= 110 && e.getY() < 130)
            setStatusLabel("Click to transmit a byte");
        else if (e.getY() >= 130 && e.getY() < 150)
            setStatusLabel("Click to receive a byte");
        else if (e.getX() >= 200 && e.getX() < 300 && e.getY() >= 20 && e.getY() < 20 + 15)
            setStatusLabel("Click to set baud rate");
        else if (e.getX() >= 200 && e.getX() < 300 && e.getY() >= 20 + 15 && e.getY() < 20 + 30)
            setStatusLabel("Click to set stop bits");
        else if (e.getX() >= 200 && e.getX() < 300 && e.getY() >= 20 + 30 && e.getY() < 20 + 45)
            setStatusLabel("Click to set parity");
        else
            setStatusLabel("");
    }

    public void mouseClick(MouseEvent e) {
        if (e.getY() >= 110 && e.getY() < 130) {
            statusEdit("Enter the byte to send in hex: ", 2, true);
            edittype = 1;
        } else if (e.getY() >= 130 && e.getY() < 150) {
            statusEdit("Enter the byte to receive in hex: ", 2, true);
            edittype = 2;
        } else if (e.getX() >= 200 && e.getX() < 300 && e.getY() >= 20 && e.getY() < 20 + 15) {
            statusEdit("New baud rate: ", -1, false);
            edittype = 3;
        } else if (e.getX() >= 200 && e.getX() < 300 && e.getY() >= 20 + 15 && e.getY() < 20 + 30) {
            statusEdit("How many stop bits (1 or 2): ", 1, false);
            edittype = 4;
        } else if (e.getX() >= 200 && e.getX() < 300 && e.getY() >= 20 + 30 && e.getY() < 20 + 45) {
            statusEdit("New parity: (n, o, or e): ", 1, false);
            edittype = 5;
        }
    }

    public void statusEdited(String keys) {
        if (keys.equals(""))
            return;
        keys = keys.toLowerCase();
        if (edittype == 1) {
            computer.serialport.send((byte) Integer.parseInt(keys, 16));
        } else if (edittype == 2) {
            computer.serialport.receive((byte) Integer.parseInt(keys, 16));
        } else if (edittype == 3) {
            computer.serialport.baud = Integer.parseInt(keys);
            repaint();
        } else if (edittype == 4) {
            computer.serialport.stopbits = Integer.parseInt(keys);
            repaint();
        } else if (edittype == 5) {
            if (keys.charAt(0) == 'o')
                computer.serialport.parity = 1;
            else if (keys.charAt(0) == 'e')
                computer.serialport.parity = 2;
            else if (keys.charAt(0) == 'n')
                computer.serialport.parity = 0;
            repaint();
        }
    }
}

