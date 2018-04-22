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
    	desktopPanelHeight = desktopWindowHeight - buttonWidth - statusHeight;
	}

}
