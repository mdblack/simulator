/*
IOPorts.java


Handles communication between the processor and the IO devices
*/
package simulator;

public class IOPorts {
    IODevice[] ioport;
    String[] ioportDeviceName;
    String[] ioportPortName;
    String[][] ioportBitName;

    NullDevice nulldevice;
    Computer computer;

    public IOPorts(Computer computer) {
        this.computer = computer;

        //initialize all 65536 IO ports
        ioport = new IODevice[65536];
        ioportDeviceName = new String[65536];
        ioportPortName = new String[65536];
        ioportBitName = new String[65536][8];
        nulldevice = new NullDevice();
        for (int i = 0; i < 65536; i++) {
            ioport[i] = nulldevice;
            ioportDeviceName[i] = "Unconnected port";
        }
    }

    public int ioPortReadByte(int address) {
        byte b = ioport[address].ioPortReadByte(address);
        if (computer.processor.processorGUICode != null)
            computer.processor.processorGUICode.push(Processor.GUICODE.PORT_READ, address, b);
        if (computer.ioGUI != null)
            computer.ioGUI.readPort(address, b & 0xff);
//		System.out.printf("Read %x from port %x\n",b,address);
        return b;
    }

    public int ioPortReadWord(int address) {
        return ioport[address].ioPortReadWord(address);
        //               return (ioPortReadByte(address)&0xff) | ((ioPortReadByte(address+1)<<8)&0xff00);
    }

    public int ioPortReadLong(int address) {
        return ioport[address].ioPortReadLong(address);
//                return (ioPortReadWord(address)&0xffff) | ((ioPortReadWord(address+2)<<16)&0xffff0000);
    }

    public void ioPortWriteByte(int address, int data) {
//		System.out.printf("Write to port %x\n",address);
        ioport[address].ioPortWriteByte(address, (byte) data);
        if (computer.processor.processorGUICode != null)
            computer.processor.processorGUICode.push(Processor.GUICODE.PORT_WRITE, address, data);
        if (computer.ioGUI != null)
            computer.ioGUI.writePort(address, data & 0xff);
    }

    public void ioPortWriteWord(int address, int data) {
        ioport[address].ioPortWriteWord(address, (short) data);
//                ioPortWriteByte(address, data&0xff);
//                ioPortWriteByte(address+1, (data>>8)&0xff);
    }

    public void ioPortWriteLong(int address, int data) {
        ioport[address].ioPortWriteLong(address, data);
//                ioPortWriteWord(address, data&0xffff);
//                ioPortWriteWord(address+2, (data>>16)&0xffff);
    }

    public void requestPorts(IODevice device, int[] ports, String deviceName, String[] portName, String[][] bitName) {
        for (int i = 0; i < ports.length; i++) {
            if (ioport[ports[i]] == nulldevice) {
                ioport[ports[i]] = device;
                ioportDeviceName[ports[i]] = deviceName;
                if (portName != null)
                    ioportPortName[ports[i]] = portName[i];
                else
                    ioportPortName[ports[i]] = "";
                if (bitName != null && bitName[i].length != 0) {
                    ioportBitName[ports[i]] = bitName[i];
                } else {
                    for (int j = 0; j < 8; j++)
                        ioportBitName[ports[i]][j] = "";
                }
            }
        }
        if (computer.ioGUI != null)
            computer.ioGUI.refresh();
    }

    public void requestPorts(IODevice device, int[] ports) {
        requestPorts(device, ports, "", null, null);
    }

    public void requestPorts(IODevice device, int[] ports, String deviceName) {
        requestPorts(device, ports, deviceName, null, null);
    }

    public void requestPorts(IODevice device, int[] ports, String deviceName, String[] portName) {
        requestPorts(device, ports, deviceName, portName, null);
    }

    public void detachPorts(int[] ports) {
        for (int i = 0; i < ports.length; i++)
            ioport[ports[i]] = nulldevice;

        if (computer.ioGUI != null)
            computer.ioGUI.refresh();
    }


    public static class NullDevice extends IODevice {
        public byte ioPortReadByte(int address) {
//			System.out.printf("Read from undefined port %x\n",address);
            return (byte) 0xff;
        }

        public void ioPortWriteByte(int address, byte data) {
//			System.out.printf("Write to undefined port %x\n",address);
        }
    }
}
