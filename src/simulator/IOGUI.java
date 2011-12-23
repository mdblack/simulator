package simulator;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;

public class IOGUI extends AbstractGUI
{
	public int portLastRead=-1;
	public int portLastWrite=-1;
	static final int PORTHEIGHT=20;
	static final int NUMBERWIDTH=PORTHEIGHT*3;
	static final int NAMEWIDTH=PORTHEIGHT*5;
	static final int INFOWIDTH=PORTHEIGHT*5;
	static final int FIELDWIDTH=PORTHEIGHT*1;
	static final int BUTTONWIDTH=PORTHEIGHT*4;

	JTextField[][] bit;
	JButton[] readButton, writeButton;

	int numPorts;
	int[] portY;

	public IOGUI(Computer computer)
	{
		super(computer,"IO Ports",600,340,true,true,true,false);
		refresh();
	}
	
	public void closeGUI()
	{
		computer.ioGUI=null;
	}

	public void readPort(int port, int value)
	{
		portLastRead=port;
		if (computer.ioports.ioportDeviceName[port].equals(""))
			setStatusLabel("Read from port "+Integer.toHexString(port)+": "+Integer.toHexString(value));
		else
			setStatusLabel("Read from "+computer.ioports.ioportDeviceName[port]+" "+computer.ioports.ioportPortName[port]+" ("+Integer.toHexString(port)+"): "+Integer.toHexString(value));
	}

	public void writePort(int port, int value)
	{
		portLastWrite=port;
		if (computer.ioports.ioportDeviceName[port].equals(""))
			setStatusLabel("Write to port "+Integer.toHexString(port)+": "+Integer.toHexString(value));
		else
			setStatusLabel("Write to "+computer.ioports.ioportDeviceName[port]+" "+computer.ioports.ioportPortName[port]+" ("+Integer.toHexString(port)+"): "+Integer.toHexString(value));
	}

	public void constructGUI(AbstractGUI.GUIComponent guicomponent)
	{
		numPorts=0;
		portY=new int[65536];
		int y=0;
		for (int i=0; i<65536; i++)
		{
			if (computer.ioports.ioport[i]==computer.ioports.nulldevice)
				portY[i]=-1;
			else
			{
				portY[i]=y;
				y+=PORTHEIGHT;
				numPorts++;
			}
		}
		bit=new JTextField[numPorts][8];
		readButton=new JButton[numPorts];
		writeButton=new JButton[numPorts];
		for (int i=0; i<numPorts; i++)
		{
			for (int j=0; j<8; j++)
			{
				bit[i][j]=new JTextField("",1);
				bit[i][j].setBounds(NUMBERWIDTH+NAMEWIDTH+INFOWIDTH+j*FIELDWIDTH+1,i*PORTHEIGHT+1,FIELDWIDTH-2,PORTHEIGHT-2);
				guicomponent.add(bit[i][j]);
			}
			readButton[i]=new JButton("read");
			readButton[i].setBounds(NUMBERWIDTH+NAMEWIDTH+INFOWIDTH+FIELDWIDTH*8+1,i*PORTHEIGHT+1,BUTTONWIDTH-2,PORTHEIGHT-2);
			readButton[i].addActionListener(new PortButtonListener(i));
			guicomponent.add(readButton[i]);
			writeButton[i]=new JButton("write");
			writeButton[i].setBounds(NUMBERWIDTH+NAMEWIDTH+INFOWIDTH+FIELDWIDTH*8+1+BUTTONWIDTH,i*PORTHEIGHT+1,BUTTONWIDTH-2,PORTHEIGHT-2);
			writeButton[i].addActionListener(new PortButtonListener(i));
			guicomponent.add(writeButton[i]);
		}
	}

	public void mouseMove(MouseEvent e)
	{
		if (e.getX()<NUMBERWIDTH+NAMEWIDTH+INFOWIDTH || e.getX()>=NUMBERWIDTH+NAMEWIDTH+INFOWIDTH+FIELDWIDTH*8) return;
		if (e.getY()<0 || e.getY()>=numPorts*PORTHEIGHT) return;
		int b=(e.getX()-(NUMBERWIDTH+NAMEWIDTH+INFOWIDTH))/FIELDWIDTH;
		int p=(e.getY()/PORTHEIGHT);
		int port;
		for (port=0; port<65536; port++)
			if (portY[port]==p*PORTHEIGHT)
				break;
		setStatusLabel(computer.ioports.ioportBitName[port][b]);
	}

	public int width()
	{
		return NUMBERWIDTH+NAMEWIDTH+INFOWIDTH+FIELDWIDTH*8+BUTTONWIDTH*2;
	}

	public int height()
	{
		return PORTHEIGHT*numPorts;
	}

	public void doPaint(Graphics g)
	{
		for (int p=0; p<numPorts; p++)
		{
			int port;
			for (port=0; port<65536; port++)
				if (portY[port]==p*PORTHEIGHT)
					break;

			if (port==portLastRead)
				g.setColor(new Color(150,255,150));
			else if (port==portLastWrite)
				g.setColor(new Color(255,150,150));
			else if (p%2==0)
				g.setColor(new Color(200,200,200));
			else
				g.setColor(Color.WHITE);

			g.fillRect(0,p*PORTHEIGHT,width(),PORTHEIGHT);

			g.setColor(Color.BLACK);
			g.setFont(new Font("Dialog",Font.BOLD,PORTHEIGHT-8));
			g.drawString(Integer.toHexString(port),3,p*PORTHEIGHT+PORTHEIGHT-3);

			g.drawString(computer.ioports.ioportDeviceName[port],NUMBERWIDTH,p*PORTHEIGHT+PORTHEIGHT-3);
			g.drawString(computer.ioports.ioportPortName[port],NUMBERWIDTH+NAMEWIDTH,p*PORTHEIGHT+PORTHEIGHT-3);
		}
	}

	private class PortButtonListener implements ActionListener
	{
		int port=0;
		int p;
		public PortButtonListener(int p)
		{
			super();
			this.p=p;
			for (int i=0; i<65536; i++)
			{
				if (portY[i]==p*PORTHEIGHT)
				{
					port=i;
					break;
				}
			}
		}
		public void actionPerformed(ActionEvent e)
		{
			if (e.getActionCommand().equals("read"))
			{
				int b = computer.ioports.ioPortReadByte(port);
				for (int i=7; i>=0; i--)
				{
					bit[p][i].setText(""+(b&1));
					b=(b>>>1);
				}
			}
			else if (e.getActionCommand().equals("write"))
			{
				//audit all the bit fields
				boolean mustRead=false;
				for (int i=0; i<8; i++)
				{
					String t = bit[p][i].getText();
					if (t.length()>1)
					{
						t=""+t.charAt(0);
						bit[p][i].setText(t);
					}
					if (!t.equals("0")&&!t.equals("1"))
					{
						bit[p][i].setText("");
						mustRead=true;
					}
				}
				int b=0;
				if(mustRead)
					b=computer.ioports.ioPortReadByte(port);
				for (int i=0; i<8; i++)
				{
					if (bit[p][i].getText().equals(""))
						continue;
					int j=Integer.parseInt(bit[p][i].getText())&1;
					j=j<<(7-i);
					b=b|j;
				}
				computer.ioports.ioPortWriteByte(port,b&0xff);
			}
		}
	}
}
