package simulator;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.io.*;

public class MemoryTransferGUI extends AbstractGUI
{
	private static final int MARGIN=10,ROWSIZE=20,TEXTWIDTH=200,LABELWIDTH=190;
	private static final int WINDOWWIDTH=MARGIN*3+TEXTWIDTH+LABELWIDTH+LABELWIDTH;

	private JTextField imagefield,afield,efield;
	private JButton choosebutton;
	private JButton loadbutton,dumpbutton;
	private boolean doimport;

	public MemoryTransferGUI(Computer computer)
	{
		super(computer,"Memory Transfer",WINDOWWIDTH,10+ROWSIZE*7,false,false,false,true);
		refresh();
	}
	public void closeGUI()
	{
		computer.memoryTransferGUI=null;
	}
	public void constructGUI(GUIComponent guicomponent)
	{
		JLabel l;
		l=new JLabel("Memory Image File: ");
		imagefield=new JTextField();
		imagefield.setText(computer.bootgui.bootImageName);
		l.setBounds(MARGIN,10,LABELWIDTH,ROWSIZE);
		imagefield.setBounds(MARGIN+LABELWIDTH,10,TEXTWIDTH,ROWSIZE);
		guicomponent.add(l);
		guicomponent.add(imagefield);
		choosebutton=new JButton("Choose");
		choosebutton.setBounds(MARGIN+LABELWIDTH+TEXTWIDTH+10,10,LABELWIDTH,ROWSIZE);
		choosebutton.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { chooseFile(); } } );
		guicomponent.add(choosebutton);

		l=new JLabel("Starting Address");
		afield=new JTextField();
		l.setBounds(MARGIN,10+ROWSIZE*2,LABELWIDTH,ROWSIZE);
		afield.setBounds(MARGIN+LABELWIDTH,10+ROWSIZE*2,TEXTWIDTH,ROWSIZE);
		afield.setText("0");
		guicomponent.add(l);
		guicomponent.add(afield);
		l=new JLabel("Ending Address");
		efield=new JTextField();
		l.setBounds(MARGIN,10+ROWSIZE*4,LABELWIDTH,ROWSIZE);
		efield.setBounds(MARGIN+LABELWIDTH,10+ROWSIZE*4,TEXTWIDTH,ROWSIZE);
		efield.setText("0");
		guicomponent.add(l);
		guicomponent.add(efield);


		loadbutton=new JButton("Load");
		loadbutton.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doLoad(); } } );

		dumpbutton=new JButton("Dump");
		dumpbutton.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doDump(); } } );

		loadbutton.setBounds(WINDOWWIDTH/4-TEXTWIDTH/4,10+ROWSIZE*6,TEXTWIDTH/2,ROWSIZE);
		guicomponent.add(loadbutton);
		dumpbutton.setBounds(2*WINDOWWIDTH/4-TEXTWIDTH/4,10+ROWSIZE*6,TEXTWIDTH/2,ROWSIZE);
		guicomponent.add(dumpbutton);
		JButton cancelbutton=new JButton("Cancel");
		cancelbutton.setBounds(3*WINDOWWIDTH/4-TEXTWIDTH/4,10+ROWSIZE*6,TEXTWIDTH/2,ROWSIZE);
		cancelbutton.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doClose(); } } );
		guicomponent.add(cancelbutton);
	}

	public void doClose()
	{
		setVisible(false);
		if (computer.computerGUI.singleFrame)
			computer.computerGUI.removeComponent(computer.memoryTransferGUI);
	}

	public void chooseFile()
	{
		JFileChooser fc = new JFileChooser();
		fc.setCurrentDirectory(new File("."));
		fc.showOpenDialog(null);
		if (fc.getSelectedFile()!=null && fc.getSelectedFile().getAbsolutePath()!=null)
			imagefield.setText(fc.getSelectedFile().getAbsolutePath());
	}

	public static byte[] readExternalFile(String name)
	{
		byte[] buffer=null;
		try
		{
			RandomAccessFile f = new RandomAccessFile(name,"r");
			int file_size=(int)f.length();

			buffer=new byte[file_size];

			for(int i=0; i<file_size; i++)
				buffer[i]=(byte)f.read();

			f.close();
		}
		catch(IOException e)
		{
		}
		return buffer;
	}

	public void writeExternalFile(String name, byte[] buffer)
	{
		if (name==null || buffer==null) return;
		try
		{
			FileOutputStream f = new FileOutputStream(name);
			int file_size=buffer.length;

			for(int i=0; i<file_size; i++)
				f.write(buffer[i]&0xff);

			f.close();
		}
		catch(IOException e)
		{
		}
	}

	public void doDump()
	{
		int a=Integer.parseInt(afield.getText(),16);
		int e=Integer.parseInt(efield.getText(),16);
		if (e<a) return;
		byte[] buffer=new byte[(e-a+1)*3];
		int bptr=0;
		for (int i=a; i<=e; i++)
		{
			byte b=computer.physicalMemory.getByte(i);
			byte b1=(byte)((b>>>4)&0xf);
			if (b1<=9)
				buffer[bptr++]=(byte)(b1+0x30);
			else
				buffer[bptr++]=(byte)(b1+0x41-0xa);
			byte b2=(byte)(b&0xf);
			if (b2<=9)
				buffer[bptr++]=(byte)(b2+0x30);
			else
				buffer[bptr++]=(byte)(b2+0x41-0xa);
			buffer[bptr++]=(byte)0x20;
		}
		writeExternalFile(imagefield.getText(),buffer);
		doClose();
	}

	public void doLoad()
	{
		MemoryTransferGUI.load(imagefield.getText(),Integer.parseInt(afield.getText(),16),computer);
		doClose();
	}
	public static void load(String imagefile, int a, Computer computer)
	{
		byte[] buffer=readExternalFile(imagefile);
		int i=0;
		byte b=0;
		boolean byteready=false;
		while(i<buffer.length)
		{
			if (buffer[i]>=0x30&&buffer[i]<=0x39)
			{
				b=(byte)((b<<4)|(buffer[i]-0x30));
				byteready=true;
			}
			else if (buffer[i]>=0x41&&buffer[i]<=0x46)
			{
				b=(byte)((b<<4)|(buffer[i]-0x41+0xa));
				byteready=true;
			}
			else if (buffer[i]>=0x61&&buffer[i]<=0x66)
			{
				b=(byte)((b<<4)|(buffer[i]-0x61+0xa));
				byteready=true;
			}
			else if (byteready)
			{
				computer.physicalMemory.setByte(a,b);
				a++;
				b=0;
				byteready=false;
			}
			i++;
		}
	}
}
