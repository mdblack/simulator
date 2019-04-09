package simulator;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;

public class TimerGUI extends AbstractGUI {
    public static final int BUTTONWIDTH = 45;
    public static final int BUTTONHEIGHT = 20;
    public static final int INFOHEIGHT = 16;
    public static final int CLOCKX = 10;
    public final int CLOCKY = BUTTONHEIGHT + INFOHEIGHT + 30;
    public int CLOCKW = 0;
    public int[] endTime, currentTime;
    public int currentTimer = 0;
    public boolean[] active;
    public boolean oneshot = false, square = false, interruptonterminal = false, rategen = false;


    public TimerGUI(Computer computer) {
        super(computer, "Timer", 280, 340, true, true, true, false);
        CLOCKW = canvasY - BUTTONROWSIZE - STATUSSIZE - CLOCKY - 10;
        active = new boolean[3];
        endTime = new int[3];
        currentTime = new int[3];
        setTimer(0);
        refresh();
    }

    public void closeGUI() {
        computer.timerGUI = null;
    }

    public void startTimer(int currentTimer, int endTime) {
        currentTime[currentTimer] = 0;
        this.endTime[currentTimer] = endTime;
        active[currentTimer] = true;
        repaint();
        setStatusLabel("Starting timer " + currentTimer);
    }

    public void endTimer(int currentTimer) {
        active[currentTimer] = false;
        repaint();
    }

    public void interrupt(int currentTimer, int irq, int irqlevel) {
        setStatusLabel("Interrupt " + Integer.toHexString(irq) + " from timer " + currentTimer + ": " + irqlevel);
    }

    public void clockTick() {
        if (active[0]) currentTime[0] = (currentTime[0] + 1) % endTime[0];
        if (active[1]) currentTime[1] = (currentTime[1] + 1) % endTime[1];
        if (active[2]) currentTime[2] = (currentTime[2] + 1) % endTime[2];
        if (active[currentTimer]) repaint();
    }

    public void updateTimer(int currentTimer) {
        if (this.currentTimer == currentTimer)
            setTimer(currentTimer);
    }

    public void setTimer(int currentTimer) {
        this.currentTimer = currentTimer;
        int mode = computer.timer.channels[currentTimer].mode;
        if (mode == Timer.Channel.MODE_INTERRUPT_ON_TERMINAL_COUNT) {
            square = false;
            oneshot = true;
            interruptonterminal = true;
            rategen = false;
        } else if (mode == Timer.Channel.MODE_HARDWARE_RETRIGGERABLE_ONE_SHOT) {
            square = false;
            oneshot = true;
            interruptonterminal = false;
            rategen = false;
        } else if (mode == Timer.Channel.MODE_RATE_GENERATOR) {
            square = false;
            oneshot = false;
            interruptonterminal = false;
            rategen = true;
        } else if (mode == Timer.Channel.MODE_SQUARE_WAVE) {
            square = true;
            oneshot = false;
            interruptonterminal = false;
            rategen = false;
        } else if (mode == Timer.Channel.MODE_SOFTWARE_TRIGGERED_STROBE || mode == Timer.Channel.MODE_HARDWARE_TRIGGERED_STROBE) {
            square = false;
            oneshot = false;
            interruptonterminal = false;
            rategen = false;
        }
        repaint();
    }

    public void doPaint(Graphics g) {
        g.setColor(Color.BLACK);
        g.setFont(new Font("Dialog", Font.BOLD, INFOHEIGHT - 4));
        g.drawString("Channel:", 10, 10 + BUTTONHEIGHT - 4);
        String info = "Timer channel " + currentTimer + ": ";
        if (!active[currentTimer])
            info += "inactive";
        else
            info += "" + (endTime[currentTimer] - currentTime[currentTimer]) + " cycles left";
        g.drawString(info, 10, 20 + BUTTONHEIGHT + INFOHEIGHT - 2);

        g.setColor(Color.BLACK);
        g.drawOval(CLOCKX, CLOCKY, CLOCKW, CLOCKW);

        g.setColor(new Color(150, 150, 150));
//			int startAngle=(int)(-360*((double)(transitionTime-startTime))/(endTime-startTime)+90+360)%360;
        int startAngle = 90;
        int endAngle = 90;
        if (square)
            startAngle = 270;
        else if (rategen)
            startAngle = 449;
        else if (oneshot)
            startAngle = 91;
        if (!interruptonterminal)
            for (int i = 0; i < CLOCKW / 2; i++)
                g.drawArc(i + CLOCKX, i + CLOCKY, CLOCKW - 2 * i, CLOCKW - 2 * i, startAngle, (endAngle - startAngle));
        if (oneshot) {
            g.setColor(Color.RED);
            g.drawLine(CLOCKX + CLOCKW / 2, CLOCKY + CLOCKW / 2, CLOCKX + CLOCKW / 2, CLOCKY);
        }
        if (active[currentTimer]) {
            g.setColor(Color.GREEN);
//				double timeAngle = (double)(currentTime-startTime)*(2*Math.PI)/(endTime-startTime);
            double timeAngle = (double) (currentTime[currentTimer]) * (2 * Math.PI) / (endTime[currentTimer]);
            g.drawLine(CLOCKW / 2 + CLOCKX, CLOCKW / 2 + CLOCKY, (int) (CLOCKW / 2 + CLOCKX + (CLOCKW / 2 - 10) * Math.sin(timeAngle)), (int) (CLOCKW / 2 + CLOCKY - (CLOCKW / 2 - 10) * Math.cos(timeAngle)));
        }
    }

    public void constructGUI(GUIComponent guiComponent) {
        JButton c1 = new JButton("0");
        c1.setBounds(75, 10, BUTTONWIDTH, BUTTONHEIGHT);
        c1.addActionListener(new TimerButtonListener());
        guiComponent.add(c1);
        JButton c2 = new JButton("1");
        c2.setBounds(75 + BUTTONWIDTH + 10, 10, BUTTONWIDTH, BUTTONHEIGHT);
        c2.addActionListener(new TimerButtonListener());
        guiComponent.add(c2);
        JButton c3 = new JButton("2");
        c3.setBounds(75 + BUTTONWIDTH * 2 + 20, 10, BUTTONWIDTH, BUTTONHEIGHT);
        c3.addActionListener(new TimerButtonListener());
        guiComponent.add(c3);
    }

    private class TimerButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals("0"))
                setTimer(0);
            else if (e.getActionCommand().equals("1"))
                setTimer(1);
            else if (e.getActionCommand().equals("2"))
                setTimer(2);
        }
    }
}
