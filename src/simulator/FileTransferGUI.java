package simulator;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.io.*;

//this class is used for:
//- moving files to a FAT file system
//- moving files from a FAT file system
//- compiling and running ASM or C programs
//- moving batches of files

//TODO:
//- editing or creating text files


public class FileTransferGUI extends AbstractGUI
{
	private static final int MARGIN=10,ROWSIZE=20,TEXTWIDTH=700;

	private JTextField infield,outfield;
	private JTextArea inarea;
	private JButton choosebutton;
	private boolean doimport;
	private int type;

	private JComboBox imageList;
	private JComboBox fileList;
//	private JScrollPane filePane;
	private FATDecoder fatdecoder;

	public FileTransferGUI(Computer computer, int type)
	{
		super(computer, new String[]{"Import File","Export File","Edit File"}[type], 700, 500 ,false,false,false,true);
		this.doimport=(type==0);
		this.type=type;
		
		refresh();
	}
	public void closeGUI()
	{
		computer.fileTransferGUI=null;
	}
	
	public void constructGUI(GUIComponent guicomponent)
	{
		JLabel l;
		Dimension dim=new Dimension(TEXTWIDTH,ROWSIZE);
		JPanel masterpanel=new JPanel();
		masterpanel.setLayout(new BoxLayout(masterpanel,BoxLayout.PAGE_AXIS));
		guicomponent.add(masterpanel);
		masterpanel.setBounds(0,0,TEXTWIDTH,getHeight()-50);
		masterpanel.setBackground(Color.WHITE);

		imageList=new JComboBox();
		for(int i=0; i<computer.bootgui.diskImage.length; i++)
			if (!computer.bootgui.diskImage.equals(""))
			{
				imageList.addItem(computer.bootgui.diskImage[i]);
			}
		imageList.setSelectedItem(computer.bootgui.bootImageName);
		imageList.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) 
			{ 
				fatdecoder=new FATDecoder((String)imageList.getSelectedItem());
				if (fileList!=null)
				{
					fileList.removeAllItems();
					for (int i=0; i<fatdecoder.filelist.length; i++)
						fileList.addItem(fatdecoder.filelist[i]);
				}
			}
		});
		imageList.setMaximumSize(dim);
		imageList.setMinimumSize(dim);
		imageList.setAlignmentX(MARGIN);
		fatdecoder=new FATDecoder(computer.bootgui.bootImageName);
		
		fileList=new JComboBox();
		for (int i=0; i<fatdecoder.filelist.length; i++)
			fileList.addItem(fatdecoder.filelist[i]);
		fileList.setMaximumSize(dim);
		fileList.setAlignmentX(MARGIN);
		
		fileList.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) 
			{ 
				infield.setText((String)fileList.getSelectedItem()); 
				if (outfield!=null) 
					outfield.setText(infield.getText());
			}
		});

		infield=new JTextField();
		infield.setMaximumSize(dim);
		infield.setAlignmentX(MARGIN);
		outfield=new JTextField();
		outfield.setMaximumSize(dim);
		outfield.setAlignmentX(MARGIN);
		choosebutton=new JButton("Choose");
		choosebutton.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { chooseFile(); } } );
		choosebutton.setAlignmentX(MARGIN);
		
		inarea=new JTextArea();
		JScrollPane inareapane=new JScrollPane(inarea);
		inareapane.setAlignmentX(MARGIN);

		JButton editbutton=new JButton("Edit");
		editbutton.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doEdit(); } } );
		editbutton.setToolTipText("Open the file in the editor.  Only works for ASCII files.");
		JButton deletebutton=new JButton("Delete");
		deletebutton.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doDelete(); } } );
		deletebutton.setToolTipText("Delete the file from the disk image.");
		JButton savebutton=new JButton("Save");
		savebutton.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doSave(); } } );
		savebutton.setToolTipText("Save the editor contents to the disk image.");
		JButton exportbutton=new JButton("Export");
		exportbutton.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doExport(); } } );
		exportbutton.setToolTipText("Copy the file to your hard drive.");
		JButton compilebutton=new JButton("Compile");
		compilebutton.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doCompile(); } } );
		compilebutton.setToolTipText("Calls TASM and TLINK to create an executable file.");
		JButton runbutton=new JButton("Run");
		runbutton.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doRun(); } } );
		runbutton.setToolTipText("Run the program as an executable");
		JButton runbreakbutton=new JButton("Run and break");
		runbreakbutton.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doBreak(); doRun(); } } );
		runbreakbutton.setToolTipText("Run the program and break when AX=1234h");
		JButton stopbutton=new JButton("Return to DOS");
		stopbutton.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doStop(); } } );
		stopbutton.setToolTipText("Terminates program with an interrupt 21h call");

		JButton transferbutton=new JButton("Transfer");
		transferbutton.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { doTransfer(); } } );

		JButton cancelbutton=new JButton("Cancel");
		cancelbutton.setToolTipText("Close this window.");
		cancelbutton.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) 
			{
				setVisible(false);
				computer.computerGUI.removeComponent(computer.fileTransferGUI);
			} } );
		
		

		if (type<=1)
		{
			l=new JLabel("Disk Image: "); l.setAlignmentX(MARGIN); l.setMaximumSize(dim); masterpanel.add(l);
			masterpanel.add(imageList);
			masterpanel.add(new Box.Filler(dim, dim, dim));
			if (doimport)
			{
				l=new JLabel("File on your computer: "); l.setAlignmentX(MARGIN); l.setMaximumSize(dim); masterpanel.add(l);
				masterpanel.add(choosebutton);
				masterpanel.add(outfield);
				l=new JLabel("File on image: "); l.setAlignmentX(MARGIN); l.setMaximumSize(dim); masterpanel.add(l);
				masterpanel.add(fileList);
				masterpanel.add(infield);
				masterpanel.add(new Box.Filler(dim, dim, dim));
			}
			else
			{
				l=new JLabel("File on image: "); l.setAlignmentX(MARGIN); l.setMaximumSize(dim); masterpanel.add(l);
				masterpanel.add(fileList);
				masterpanel.add(infield);
				masterpanel.add(new Box.Filler(dim, dim, dim));
				l=new JLabel("File on your computer: "); l.setAlignmentX(MARGIN); l.setMaximumSize(dim); masterpanel.add(l);
				masterpanel.add(choosebutton);
				masterpanel.add(outfield);				
			}
			masterpanel.add(new Box.Filler(dim, dim, dim));
			JPanel bpanel=new JPanel();
			bpanel.add(transferbutton);
			bpanel.add(cancelbutton);
			masterpanel.add(bpanel);
		}
		else
		{
			l=new JLabel("Disk Image: "); l.setAlignmentX(MARGIN); l.setMaximumSize(dim); masterpanel.add(l);
			masterpanel.add(imageList);
			masterpanel.add(new Box.Filler(dim, dim, dim));
			l=new JLabel("File on image: "); l.setAlignmentX(MARGIN); l.setMaximumSize(dim); masterpanel.add(l);
			masterpanel.add(fileList);
			masterpanel.add(infield);
			masterpanel.add(new Box.Filler(dim, dim, dim));
			masterpanel.add(inareapane);
			masterpanel.add(new Box.Filler(dim, dim, dim));
			JPanel bpanel=new JPanel();
			bpanel.add(editbutton);
			bpanel.add(savebutton);
			bpanel.add(exportbutton);
			bpanel.add(compilebutton);
			bpanel.add(runbutton);
			bpanel.add(runbreakbutton);
			bpanel.add(stopbutton);
			bpanel.add(deletebutton);
			bpanel.add(cancelbutton);
			masterpanel.add(bpanel);			
		}		
	}

	private void refreshdisk()
	{
		fatdecoder=new FATDecoder((String)imageList.getSelectedItem());
		if (fileList!=null)
		{
			fileList.removeAllItems();
			for (int i=0; i<fatdecoder.filelist.length; i++)
				fileList.addItem(fatdecoder.filelist[i]);
		}		
	}
	
	public void chooseFile()
	{
		JFileChooser fc = new JFileChooser();
		fc.setCurrentDirectory(new File("."));
		fc.setMultiSelectionEnabled(true);
		if (doimport) fc.showOpenDialog(null);
		else fc.showSaveDialog(null);
		if (fc.getSelectedFile()==null)
			return;
		
		String ofd="",ifd="";
		for (int i=0; i<fc.getSelectedFiles().length; i++)
		{
			ofd+=fc.getSelectedFiles()[i].getAbsolutePath()+" ";
			ifd+=fc.getSelectedFiles()[i].getName()+" ";
		}
		outfield.setText(ofd);
		if (doimport)
			infield.setText(ifd);
		
//		outfield.setText(fc.getSelectedFile().getAbsolutePath());
//		if (doimport) infield.setText(fc.getSelectedFile().getName());
	}


	public void doTransfer()
	{
		String[] infields=infield.getText().split(" ");
		String[] outfields=outfield.getText().split(" ");
		for (int i=0; i<infields.length; i++)
		{
			fatdecoder.decodeDiskParameters();
			if(doimport)
				fatdecoder.writeInternalFile(infields[i],fatdecoder.readExternalFile(outfields[i]));
			else
				fatdecoder.writeExternalFile(outfields[i],fatdecoder.readInternalFile(infields[i]));
		}

		setVisible(false);
		if (computer.computerGUI.singleFrame)
			computer.computerGUI.removeComponent(computer.fileTransferGUI);
	}
	
	public void doExport()
	{
		if (infield.getText().equals("") || JOptionPane.showConfirmDialog(null, ""+infield.getText()+" will be saved in your home directory\nIf it already exists, it will be overwritten.","Are you sure?",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)
			return;
		fatdecoder.decodeDiskParameters();
		fatdecoder.writeExternalFile(infield.getText(),fatdecoder.readInternalFile(infield.getText()));
	}

	public void doDelete()
	{
		if (infield.getText().equals("") || JOptionPane.showConfirmDialog(null, "Click YES to delete "+infield.getText(),"Are you sure?",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)
			return;
		String[] infields=infield.getText().split(" ");
		for (int i=0; i<infields.length; i++)
		{
			fatdecoder.decodeDiskParameters();
			fatdecoder.deleteFile(infields[i]);
		}
		refreshdisk();
	}

	public void doRun()
	{
		if (infield.getText().equals("")) return;
		String[] infields=infield.getText().split(" ");
		for (int i=0; i<infields.length; i++)
		{
			if (computer.keyboardGUI!=null)
			{
				computer.keyboardGUI.issueScript(infields[i].substring(0,infields[i].length()-4));
			}
		}
		if (computer.debugMode)
		{
			computer.computerGUI.play();
		}
	}

	public void doCompile()
	{
		if (infield.getText().equals("")) return;
		String[] infields=infield.getText().split(" ");
		for (int i=0; i<infields.length; i++)
		{
			String script="";
			if (infields[i].indexOf(".OBJ")!=-1)
				script="tlink "+infields[i]+"\n";
			else if (infields[i].indexOf(".C")!=-1)
				script="tcc "+infields[i]+"\n";
			else if (infields[i].indexOf(".ASM")!=-1)
			{
				fatdecoder.decodeDiskParameters();
				fatdecoder.deleteFile(infields[i].substring(0,infields[i].length()-4)+".OBJ");
				fatdecoder.deleteFile(infields[i].substring(0,infields[i].length()-4)+".EXE");
				script="tasm "+infields[i]+"\n";
				script+="tlink "+infields[i].substring(0,infields[i].length()-4)+"\n";
//				infield.setText(infields[i].substring(0,infields[i].length()-4)+".EXE");
			}
			if (computer.keyboardGUI!=null) 
			{
				if (computer.debugMode)
				{
					computer.updateGUIOnPlay=false;
					computer.computerGUI.play();
				}
				computer.keyboardGUI.issueScript(script);
			}
		}
	}
	public void doEdit()
	{
		if (infield.getText().equals("")) return;
		byte[] file=fatdecoder.readInternalFile(infield.getText());
		String s="";
		for (int i=0; i<file.length; i++)
			s+=(char)file[i];
		inarea.setText(s);
	}
	public void doStop()
	{
		if (JOptionPane.showConfirmDialog(null, "This will cause your program to exit.\nIf a A:> prompt does not appear you may need to restart the simulator.\nDon't press this if you are currently at a DOS prompt.","Are you sure?",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)
			return;
		if (computer.breakpointGUI!=null)
			computer.breakpointGUI.clear();
		int interrupt=0x21;
		computer.processor.eax.setValue(0);
		computer.processor.handleInterrupt(interrupt);
		computer.updateGUIOnPlay=false;
		computer.computerGUI.play();		
	}
	public void doSave()
	{
		if (infield.getText().equals("")) return;
		if (JOptionPane.showConfirmDialog(null, "This will overwrite "+infield.getText(),"Are you sure?",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION)
			fatdecoder.writeInternalFile(infield.getText(), inarea.getText().getBytes());
	}
	public void doBreak()
	{
		computer.updateGUIOnPlay=false;
		if (computer.breakpointGUI==null)
		{
			computer.breakpointGUI=new BreakpointGUI(computer,"( register EAX == 1234 ) . ");
			computer.breakpointGUI.toBack();
		}
		else
			computer.breakpointGUI.setEquation("( register EAX == 1234 ) . ");
		if (computer.registerGUI==null)
			computer.registerGUI=new RegisterGUI(computer);
/*		if (computer.memoryGUI==null) 
			computer.memoryGUI=new MemoryGUI(computer);
		computer.memoryGUI.codeFrame=new MemoryBlockGUI(computer,MemoryBlockGUI.CODE,computer.processor.cs.address(computer.processor.eip.getValue()));
		*/
	}
}
