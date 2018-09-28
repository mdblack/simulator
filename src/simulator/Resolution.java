package simulator;

import java.awt.Dimension;
import java.awt.Toolkit;

import javax.swing.JComponent;
import javax.swing.UIManager;

public class Resolution {
    public final boolean USE_SCALING_FETURE = false;

    Desktop desktop;
    Monitor monitor;
    Datapath datapath;

    public class InnerPane extends AbstractWindow {
        public int preferredWidth;
        public int preferredHeight;
    }

    public class InnerWindow extends AbstractWindow {
        public InnerWindow(int width, int height) {
            super(width, height);
        }

        public InnerWindow() {
            this(0, 0);
        }

        public InnerWindow(int width) {
            this(width, 0);
        }
    }

    public class Monitor extends AbstractWindow {
        private final double STANDARD_WIDTH = 1440.0;
        private final double STANDARD_HEIGHT = 900.0;

        public Monitor() {
            Dimension screenSize;
            if (USE_SCALING_FETURE)
                screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            else
                screenSize = new Dimension((int) STANDARD_WIDTH, (int) STANDARD_HEIGHT);

            width = (int) screenSize.getWidth();
            height = (int) screenSize.getHeight();

            multiplier = width / STANDARD_WIDTH;
        }
    }

    public class Desktop extends AbstractWindow {
        public Desktop() {
            width = (int) (monitor.width * 0.7);
            height = (int) (monitor.height * 0.8);
            pane = new InnerPane();

            setInnerPane(width, height);
        }

        public void setInnerPane(int basedOnWidth, int basedOnHeight) {
            pane.width = basedOnWidth;
            pane.height = basedOnHeight - (int) (BUTTON_HEIGHT * 3 * multiplier);
            pane.preferredWidth = (int) (pane.width * 0.9);
            pane.preferredHeight = (int) (pane.height * 0.8);
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

            toolComponent = new InnerWindow((int) (BUTTON_COMPONENT_WIDTH * multiplier));
            modificationComponent = new InnerWindow((int) (BUTTON_COMPONENT_WIDTH * multiplier));
        }

        public double getScalingFactor() {
            return scalingFactor + multiplier;
        }
    }


    //////////////////////////

    public Resolution() {
        monitor = new Monitor();

        System.out.println("Width: " + monitor.width + " Height: " + monitor.height);
        desktop = new Desktop();
        datapath = new Datapath();

    }
}
