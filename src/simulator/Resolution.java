package simulator;

import java.awt.Dimension;
import java.awt.Toolkit;

public class Resolution {
	public int screenWidth;
	public int screenHeight;
	
	public int desktopWindowWidth;
	public int desktopWindowHeight;
	
	public int buttonWidth = 100;
	public int buttonHeight = 30;
	public int statusHeight = 30;
	public int desktopPanelHeight;
	public int desktopPanelWidth;
	
	public int newComponentHeight;
	public int newComponentWidth;
	
	private int fontSize = 14;
	private double scalingFactor = 1.6;

	public Resolution() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        screenWidth = (int) screenSize.getWidth();
        screenHeight = (int) screenSize.getHeight();
        
        setDesktopDimensions((int)(screenWidth * 0.7), (int)(screenHeight * 0.8));
	}
	
	public void setDesktopDimensions(int newWidth, int newHeight) {
        desktopWindowWidth = newWidth;
        desktopWindowHeight = newHeight;
        
        desktopPanelWidth = desktopWindowWidth;
        
        // Calculate the panel size, leaving room for 3 status heights.
        desktopPanelHeight = desktopWindowHeight - (buttonHeight*3);
    	
	    	newComponentHeight = (int)(desktopPanelHeight * 0.8);
	    	newComponentWidth = (int)(desktopPanelWidth * 0.9);
	}
	
	public double getScalingFactor() {
		return scalingFactor;
	}
	
	public int getFontSize() {
		return fontSize;
	}
	
	public int getDatapathToolComponentWidth() {
		return 150;
	}
	
	public int getButtonHeight() {
		return 45;
	}
	public int getButtonHeightAndSpace() {
		return 50;
	}
	public int getDatapathModificationComponentWidth() {
		return 150;
	}
}
