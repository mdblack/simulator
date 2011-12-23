package simulator;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

public class MakeDiskGUI extends AbstractGUI
{
	private static final int MARGIN=10,ROWSIZE=20,TEXTWIDTH=300;

	private JTextField namefield;
	private JList templatelist;
	private JScrollPane templatePane;
	private JButton makebutton;

	private String[] templates=null,templatenames=null;

	public MakeDiskGUI(Computer computer)
	{
		super(computer,"Construct Disk Image",MARGIN*3+TEXTWIDTH,(ROWSIZE+10)*7,false,false,false,true);

		try
		{
			String[] templist=new String[1000];
			int templength=0;

			Scanner namescan=new Scanner(computer.getClass().getResource("/resource/templates.ini").openConnection().getInputStream());
			while(namescan.hasNext())
				templist[templength++]=namescan.next();
			templates=new String[templength];
			for (int i=0; i<templength; i++)
				templates[i]=templist[i];
		}
		catch(IOException e)
		{
			System.out.println("Cannot open templates.ini");
			return;
		}

		templatenames=new String[templates.length];
		for (int i=0; i<templates.length; i++)
		{
			String temp=templates[i];
			temp=temp.substring(temp.indexOf("_")+1,temp.length());
			if(temp.charAt(0)=='f')
			{
				templatenames[i]="3 1/2 floppy - ";
				temp=temp.substring(temp.indexOf("_")+1,temp.length());
			}
			else
			{
				int c = Integer.parseInt(temp.substring(0,temp.indexOf("_")));
				temp=temp.substring(temp.indexOf("_")+1,temp.length());
				int h = Integer.parseInt(temp.substring(0,temp.indexOf("_")));
				temp=temp.substring(temp.indexOf("_")+1,temp.length());
				int s = Integer.parseInt(temp.substring(0,temp.indexOf("_")));
				temp=temp.substring(temp.indexOf("_")+1,temp.length());
				int size=c*h*s*512 / 1048576;
				templatenames[i]=""+size+" MB hard disk - ";
			}
			templatenames[i]+=temp.substring(0,temp.indexOf("."));
		}
		refresh();
	}
	public void doClose()
	{
		computer.makeDiskGUI=null;
	}

	public void constructGUI(GUIComponent guicomponent)
	{
		JLabel l;
		l=new JLabel("Choose a template:");
		l.setBounds(MARGIN,10,TEXTWIDTH,ROWSIZE);
		guicomponent.add(l);
		templatelist=new JList(templatenames);

		templatelist.addListSelectionListener(new ListSelectionListener() { 
			public void valueChanged(ListSelectionEvent e) 
			{
				String temp=templates[templatelist.getSelectedIndex()];
				temp=temp.substring(temp.indexOf("_")+1,temp.length());
				namefield.setText(temp);
			}
		});

		templatePane=new JScrollPane(templatelist);
		templatePane.setBounds(MARGIN,10+ROWSIZE+10,TEXTWIDTH,ROWSIZE);
		guicomponent.add(templatePane);
		l=new JLabel("Choose a filename for the image:");
		l.setBounds(MARGIN,10+(ROWSIZE+10)*2,TEXTWIDTH,ROWSIZE);
		guicomponent.add(l);
		namefield=new JTextField();
		namefield.setBounds(MARGIN,10+(ROWSIZE+10)*3,TEXTWIDTH,ROWSIZE);
		guicomponent.add(namefield);

		makebutton=new JButton("Construct image");
                makebutton.setBounds((MARGIN*3+TEXTWIDTH)/4-TEXTWIDTH/4,10+(ROWSIZE+10)*4,TEXTWIDTH/2,ROWSIZE);
		makebutton.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { constructImage(); } } );
		guicomponent.add(makebutton);

		JButton cancelbutton=new JButton("Cancel");
		cancelbutton.setBounds(3*(MARGIN*3+TEXTWIDTH)/4-TEXTWIDTH/4,10+(ROWSIZE+10)*4,TEXTWIDTH/2,ROWSIZE);
		cancelbutton.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) 
			{
				frame.setVisible(false);
				if (computer.computerGUI.singleFrame)
					computer.computerGUI.removeComponent(computer.makeDiskGUI);
			} } );
		guicomponent.add(cancelbutton);
	}

	private void constructImage()
	{
		String tname=templates[templatelist.getSelectedIndex()];
//		tname="simulator/resource/"+tname;
		String outname=namefield.getText();
		String temp=tname.substring(tname.indexOf("_")+1,tname.length());
		int size;
		if (temp.charAt(0)=='f')
			size=2880*512;
		else
		{
			int c = Integer.parseInt(temp.substring(0,temp.indexOf("_")));
			temp=temp.substring(temp.indexOf("_")+1,temp.length());
			int h = Integer.parseInt(temp.substring(0,temp.indexOf("_")));
			temp=temp.substring(temp.indexOf("_")+1,temp.length());
			int s = Integer.parseInt(temp.substring(0,temp.indexOf("_")));
			temp=temp.substring(temp.indexOf("_")+1,temp.length());
			size=c*h*s*512;
		}

		byte[] image=new byte[size];
		
		try
		{
			InputStream f=computer.getClass().getResourceAsStream("/resource/"+tname);

			int i=0,j;
			while((j=f.read())!=-1)
				image[i++]=(byte)j;

			f.close();
		}
		catch(IOException e)
		{
			System.out.println("Error reading file: "+e);
		}

		try
		{
			FileOutputStream f = new FileOutputStream(outname);

			for(int i=0; i<size; i++)
				f.write(image[i]&0xff);

			f.close();
		}
		catch(IOException e)
		{
			System.out.println("Error making file: "+e);
		}


		frame.setVisible(false);
		if (computer.computerGUI.singleFrame)
			computer.computerGUI.removeComponent(computer.makeDiskGUI);
	}

}
