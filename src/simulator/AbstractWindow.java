package simulator;

import javax.swing.*;

import simulator.Resolution.InnerPane;

public class AbstractWindow {
	protected final int BUTTON_COMPONENT_WIDTH = 100;
	protected final int BUTTON_HEIGHT = 25;
	protected final int STATUS_HEIGHT = 30;
	protected final int DEFAULT_STATUS_BAR_THICKNESS = 45;
	protected final int DEFAULT_SCROLL_BAR_THICKNESS = 15;
	protected final int SCROLL_BAR_PADDING = 4;
	
	static double multiplier;

	public int width;
	public int height;	
	public InnerPane pane;
	private int fontSize = 10;

	public AbstractWindow(int width, int height) {
		this.width = width;
		this.height = height;
	}
	public AbstractWindow() {
		this(0, 0);
	}
	
	public int getFontSize() {
		return (int)(fontSize * multiplier);
	}

	public int getScrollbarWidth() {
		return (int) UIManager.get("ScrollBar.width") + SCROLL_BAR_PADDING;
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
