package simulator;

import java.applet.*;
import java.awt.*;
import javax.swing.*;

public class ComputerApplet extends JApplet implements Runnable {
    JPanel panel;
    Computer computer;
    Thread appletThread;
    boolean suspended = false;
    final Computer.Lock appletLock = new Computer.Lock();

    public void init() {
        System.out.println("applet initialized");
        appletThread = new Thread(this);
        appletThread.start();
    }

    public void start() {
        System.out.println("applet start called");
        if (suspended) {
            suspended = false;
            try {
                System.out.println("applet resumed");
                appletLock.lockResume();
            } catch (NullPointerException e) {
                System.out.println(e);
            }
        }
    }

    public void stop() {
        System.out.println("applet paused");
        suspended = true;
    }

    public void run() {
        System.out.println("applet run called");
        panel = new JPanel();
        getContentPane().add(panel);
        computer = new Computer(this);
    }
}
