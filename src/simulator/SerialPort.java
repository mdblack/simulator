package simulator;

public class SerialPort extends IODevice {
    private InterruptController irqDevice;
    private byte statusRegister;
    private byte controlRegister;
    private byte interruptRegister;
    private byte interruptEnableRegister;
    private byte scratchRegister;
    private short divider;
    private boolean thrIPending;
    private byte receiveBuffer;
    private int irq = 4;

    public int baud = 9600;
    public int stopbits = 1;
    public int parity = 0;

    private static final byte UART_LCR_DLAB = (byte) 0x80;    //divisor latch bit
    private static final byte UART_LSR_TEMT = 0x40;    //transmitter empty
    private static final byte UART_LSR_THRE = 0x20;    //transmit hold register empty
    private static final byte UART_LSR_DR = 1;    //receiver data empty
    private static final byte UART_LSR_BI = 0x10;    //break interrupt
    private static final byte UART_IIR_NO_INT = 1;    //no interrupts pending
    private static final byte UART_IIR_RDI = 4;    //receive data interrupt
    private static final byte UART_IIR_THRI = 2;    //transmitter hold interrupt
    private static final byte UART_IER_RDI = 1;    //enable receiver interrupt
    private static final byte UART_IER_THRI = 2;    //enable transmitter hold interrupt
    private Computer computer;

    public SerialPort(Computer computer) {
        this.computer = computer;
        this.irqDevice = computer.interruptController;

        statusRegister = UART_LSR_TEMT | UART_LSR_THRE;
        interruptRegister = UART_IIR_NO_INT;

        computer.ioports.requestPorts(this, new int[]{0x3f8, 0x3f9, 0x3fa, 0x3fb, 0x3fc, 0x3fd, 0x3fe, 0x3ff}, "Serial Port 0", new String[]{"Low 8 bits", "High 8 bits", "", "Control", "", "Status", "", "Scratch"});
    }

    public byte ioPortReadByte(int address) {
        switch (address & 7) {
            case 0:
                if (0 != (controlRegister & UART_LCR_DLAB))
                    return (byte) divider;
                statusRegister = (byte) (statusRegister & ~(UART_LSR_DR | UART_LSR_BI));
                byte ret = receiveBuffer;
                updateIRQ();
                return ret;

            case 1:
                if (0 != (controlRegister & UART_LCR_DLAB))
                    return (byte) (divider >>> 8);
                return interruptEnableRegister;

            case 3:
                return controlRegister;

            case 5:
                return statusRegister;

            case 7:
                return scratchRegister;
        }
        return 0;
    }

    public void ioPortWriteByte(int address, byte data) {
        switch (address & 7) {
            case 0:
                if (0 == (controlRegister & UART_LCR_DLAB)) {
                    thrIPending = false;
                    statusRegister = (byte) (statusRegister & ~UART_LSR_THRE);
                    updateIRQ();
                    send(data);
                    thrIPending = true;
                    statusRegister = (byte) (statusRegister | UART_LSR_THRE | UART_LSR_TEMT);
                    updateIRQ();
                } else
                    divider = (short) ((divider & 0xff00) | data);
                break;

            case 1:
                if (0 != (controlRegister & UART_LCR_DLAB))
                    divider = (short) ((divider & 0x00ff) | (data << 8));
                else {
                    interruptEnableRegister = (byte) (data & 0x0f);
                    if (0 != (statusRegister & UART_LSR_THRE))
                        thrIPending = true;
                    updateIRQ();
                }
                break;

            case 3:
                controlRegister = (byte) data;
                break;

            case 7:
                scratchRegister = (byte) data;
                break;
        }
    }

    public void receive(byte data) {
        receiveBuffer = data;
        if (data == 0)
            statusRegister = (byte) (statusRegister | UART_LSR_DR);
        else
            statusRegister = (byte) (statusRegister | UART_LSR_BI | UART_LSR_DR);
        updateIRQ();

        if (computer.serialGUI != null)
            computer.serialGUI.receive(data);
    }

    public void send(byte data) {
        if (computer.serialGUI != null)
            computer.serialGUI.transmit(data);
    }

    private void updateIRQ() {
        if ((0 != (statusRegister & UART_LSR_DR)) && (0 != (interruptEnableRegister & UART_IER_RDI)))
            interruptRegister = UART_IIR_RDI;
        else if (thrIPending && (0 != (interruptEnableRegister & UART_IER_THRI)))
            interruptRegister = UART_IIR_THRI;
        else
            interruptRegister = UART_IIR_NO_INT;
        if (interruptRegister != UART_IIR_NO_INT)
            irqDevice.setIRQ(irq, 1);
        else
            irqDevice.setIRQ(irq, 0);
    }
}

