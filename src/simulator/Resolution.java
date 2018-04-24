package simulator;

import java.awt.Dimension;
import java.awt.Toolkit;

import javax.swing.JComponent;
import javax.swing.UIManager;

public class Resolution {
	
	private final int BUTTON_COMPONENT_WIDTH = 100;
	private final int BUTTON_HEIGHT = 20;
	private final int STATUS_HEIGHT = 30;
	private final int DEFAULT_STATUS_BAR_THICKNESS = 30;
	private final int DEFAULT_SCROLL_BAR_THICKNESS = 15;
	
	double multiplier;

	Desktop desktop;
	Monitor monitor;
	Datapath datapath;
	
	public class AbstractWindow {
		public int width;
		public int height;	
		public InnerPane pane;
		public int fontSize = 10;
		
		public int getFontSize() {
			return (int)(fontSize * multiplier);
		}
	
		public int getScrollbarWidth() {
			return (int) UIManager.get("ScrollBar.width") + 4;
		}
		
		public int getButtonHeight() {
			return (int)(BUTTON_HEIGHT * multiplier);
		}
		
		public int getButtonHeightAndSpace() {
			return (int)(BUTTON_HEIGHT * multiplier + 5);
		}
		
		public int getStatusBarThickness() {
			return (int)(DEFAULT_STATUS_BAR_THICKNESS * multiplier);
		}
		
		public int getScrollbarThickness() {
			return (int)(DEFAULT_SCROLL_BAR_THICKNESS * multiplier);
		}
	}
	public class InnerPane extends AbstractWindow{
		public int preferredWidth;
		public int preferredHeight;
	}

	public class InnerWindow extends AbstractWindow{
		public InnerWindow(int width, int height) {
			this.width = width;
			this.height = height;
		}
		public InnerWindow() {
		}
		public InnerWindow(int width) {
			this(width, 0);
		}
	}
	
	public class Monitor extends AbstractWindow {
		public Monitor() {
	        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	        width = (int) screenSize.getWidth();
	        height = (int) screenSize.getHeight();
		}
	}
	
	public class Desktop extends AbstractWindow{
		public Desktop(int screenWidth, int screenHeight) {
			width = (int)(screenWidth * 0.7);
			height = (int)(screenHeight * 0.8);
			pane = new InnerPane();
			
			setInnerPane(width, height);
		}
		
		public void setInnerPane(int basedOnWidth, int basedOnHeight) {
	        pane.width = basedOnWidth;
	        pane.height = basedOnHeight - (int)(BUTTON_HEIGHT*3*multiplier);
	        pane.preferredWidth = (int)(pane.width * 0.9);
	        pane.preferredHeight = (int)(pane.height * 0.8);
	    }
		
		public void setScrollbars() {
	        UIManager.put("ScrollBar.width", getScrollbarThickness());
		}
	}
	
	public class Datapath extends AbstractWindow {
		InnerWindow toolComponent;
		InnerWindow modificationComponent;
		private double scalingFactor = 0.6;
		
		public Datapath() {
			width = desktop.pane.preferredWidth;
			height = desktop.pane.preferredHeight;
			
			toolComponent = new InnerWindow((int)(100 * multiplier));
			modificationComponent = new InnerWindow((int)(100 * multiplier));
		}
		
		public double getScalingFactor() {
			return scalingFactor + multiplier;
		}
	}
	
	//////////////////////////
	
	public Resolution() {
        monitor = new Monitor();
        multiplier = monitor.width / 1440.0;
        
        desktop = new Desktop(monitor.width, monitor.height);
        datapath = new Datapath();
 	}
}
